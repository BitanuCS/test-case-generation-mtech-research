"""Run a trained seq2seq test-generation model (A3Test or its CodeT5 baseline) on prepared inputs.

  <toga>/.venv/bin/python scripts/ml_testgen.py {a3test|codet5} inputs.jsonl outputs.jsonl

Mirrors the authors' scripts in github.com/awsm-research/A3Test:
  A3Test/Test.py     : PLBartForConditionalGeneration + PLBART tokenizer, input cut to 1000
                       characters, greedy decoding, max_length=1000
  CodeT5/codeT5Test.py: T5ForConditionalGeneration + CodeT5 tokenizer, greedy decoding,
                       max_length=512
Models: huggingface.co/SaranyaAakas/A3Test (A3Test_Model = PLBART-base, FinalModelcodeT5 =
CodeT5-small), downloaded to <tools>/a3test/models/.
"""
import json
import sys
import time
from pathlib import Path

import torch
from transformers import (PLBartForConditionalGeneration, PLBartTokenizer, RobertaTokenizer,
                          T5ForConditionalGeneration)

TOOLS = Path("/Users/bitu/mtech-tools/a3test/models")


def load(tool):
    if tool == "a3test":
        model = PLBartForConditionalGeneration.from_pretrained(TOOLS / "a3test")
        tok = PLBartTokenizer.from_pretrained(TOOLS / "tok_plbart")   # = uclanlp/plbart-base, saved locally
        return model, tok, 1000, 1000
    model = T5ForConditionalGeneration.from_pretrained(TOOLS / "codet5")
    tok = RobertaTokenizer.from_pretrained(TOOLS / "tok_codet5")   # = Salesforce/codet5-small, saved locally
    return model, tok, None, 512


def main(tool, inp, outp):
    torch.set_num_threads(4)                      # keep the laptop usable (and cooler)
    model, tok, char_limit, max_len = load(tool)
    model.eval()
    rows = [json.loads(line) for line in open(inp)]
    with open(outp, "w") as out:
        for r in rows:
            text = r["src_fm_fc_ms_ff"]
            if char_limit and len(text) > char_limit:
                text = text[:char_limit]
            ids = tok(text, return_tensors="pt", truncation=True, max_length=1024 if tool == "a3test" else 512).input_ids
            t = time.time()
            with torch.no_grad():
                gen = model.generate(ids, max_length=max_len)
            r["generated"] = tok.decode(gen[0], skip_special_tokens=True)
            r["gen_seconds"] = round(time.time() - t, 1)
            out.write(json.dumps(r) + "\n")
            out.flush()
            print(f"{r['id']}: {r['gen_seconds']}s, {len(r['generated'])} chars", flush=True)


if __name__ == "__main__":
    main(*sys.argv[1:4])
