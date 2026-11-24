"""AsserT5 inference (run with TOGA's venv: torch + transformers + javalang).

  <toga>/.venv/bin/python scripts/assert5_predict.py <inputs.jsonl> <outputs.jsonl>

AsserT5 (Primbs, Fein, Fraser; AST 2025; zenodo 14703162, huggingface se2p/AsserT5) is CodeT5-large
fine-tuned to write ONE assertion for a test whose assertion is masked. We use the authors'
checkpoint "1/raw" (at most 1 assertion per training test, raw tokens: the variant that found the
most Defects4J bugs in their paper, 33 vs 20 for "abstract").

Input format = the authors' AsserT5Subcommand.java (AssertionExtractor), "raw" model:
  "TEST_METHOD: <test tokens> <ASSERTION> ; } FOCAL_METHOD: <focal method tokens>"
 - a dummy assertion is put before the test's last '}' and replaced by the mask <ASSERTION>
   (their code: replaceLast("}", "assertTrue(true);}"), then mask the last assertion);
 - tokens = the Java tokens joined by single spaces, comments dropped (their ANTLR leaf tokens;
   we use javalang's Java tokenizer, same tokens);
 - "(timeout = 4000)" removed from the EvoSuite @Test, as in their assertion_generator.py.
Model settings = their server.py: max 386 input tokens (truncated), max 64 new tokens,
num_beams = prediction_count = 1 (what their Defects4J runner asks for).
The checkpoint is the authors' pickled model; we load it once and keep only the weights
(converted to safetensors next to it) so later runs use ~3 GB instead of 8.8 GB.
"""
import json
import sys
import time
from pathlib import Path

import javalang
import torch
from transformers import AutoTokenizer, T5Config, T5ForConditionalGeneration

A5 = Path("/Users/bitu/mtech-tools/assert5")
CKPT = A5 / "ckpt" / "model-best-1-raw.pt"
WEIGHTS = A5 / "ckpt" / "assert5-1-raw.safetensors"
BASE = A5 / "codet5-large"          # tokenizer + config of Salesforce/codet5-large
MAX_IN, MAX_OUT = 386, 64


def tokens(code):
    return " ".join(t.value for t in javalang.tokenizer.tokenize(code))


def model_input(test_method, focal_method):
    test_method = test_method.replace("(timeout = 4000)", "")
    i = test_method.rfind("}")
    masked = tokens(test_method[:i]) + " <ASSERTION> ; }"
    return f"TEST_METHOD: {masked} FOCAL_METHOD: {tokens(focal_method) if focal_method else ''}".strip()


def load_model():
    if not WEIGHTS.exists():
        from safetensors.torch import save_file
        ck = torch.load(CKPT, map_location="cpu", weights_only=False, mmap=True)
        sd = {k: v.contiguous() for k, v in ck["model"].state_dict().items()}
        del ck
        # T5 ties these weights; safetensors needs them stored once
        for k in ("encoder.embed_tokens.weight", "decoder.embed_tokens.weight"):
            sd.pop(k, None)
        if "lm_head.weight" in sd:                    # tied to shared.weight in the checkpoint
            sd["lm_head.weight"] = sd["lm_head.weight"].clone()
        save_file(sd, str(WEIGHTS))
    from safetensors.torch import load_file
    model = T5ForConditionalGeneration(T5Config.from_pretrained(BASE))
    missing, unexpected = model.load_state_dict(load_file(str(WEIGHTS)), strict=False)
    bad = [k for k in missing if "embed_tokens" not in k]
    if bad or unexpected:
        raise RuntimeError(f"weights do not match: missing {bad[:5]}, unexpected {unexpected[:5]}")
    model.tie_weights()
    return model.eval()


def main(inp, outp):
    rows = [json.loads(line) for line in open(inp)]
    tok = AutoTokenizer.from_pretrained(BASE, model_max_length=MAX_IN)
    model = load_model()
    with open(outp, "w") as f:
        for r in rows:
            t = time.time()
            try:
                text = model_input(r["test_method"], r.get("focal_method", ""))
            except Exception as e:                       # javalang cannot tokenize it
                r.update(model_input=None, prediction=None, error=f"tokenize: {e}")
                f.write(json.dumps(r) + "\n")
                continue
            batch = tok([text], return_tensors="pt", truncation=True, padding=True)
            with torch.no_grad():
                ids = model.generate(batch["input_ids"], attention_mask=batch["attention_mask"],
                                     max_new_tokens=MAX_OUT, num_beams=1, num_return_sequences=1)
            pred = tok.batch_decode(ids, skip_special_tokens=True, clean_up_tokenization_spaces=True)[0]
            r.update(model_input=text, prediction=" ".join(pred.strip().split()),
                     gen_seconds=round(time.time() - t, 2))
            f.write(json.dumps(r) + "\n")
            f.flush()


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
