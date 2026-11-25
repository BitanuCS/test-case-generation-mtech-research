"""Baseline: CAT-LM (ASE 2023; Rao, Jain, Alon, Le Goues, Hellendoorn) -- a 2.7B GPT-NeoX model
pretrained on aligned code and test files (huggingface.co/nikitharao/catlm).

The model is too big for this Mac (8 GB RAM), so generation runs on Google Colab (free T4 GPU):

  1. python3 scripts/run_catlm.py prepare  bugs/dev40.txt      -> results/catlm/prompts.jsonl
  2. upload prompts.jsonl to colab/catlm_generate.ipynb, run it   -> generations.jsonl
     (put the downloaded file at results/catlm/generations.jsonl)
  3. python3 scripts/run_catlm.py evaluate bugs/dev40.txt      -> results/catlm/<bug>/report.json

Prompt = CAT-LM's training layout (code file, then <|codetestpair|>, then the test file), as in
the paper's "first test" setting (test file with imports and class header but no test yet):
    <code of the class under test, fixed version>
    <|codetestpair|>
    package p; <JUnit 4 imports + the class's imports>
    public class <Class>Test {
        @Test
        public void test
The model writes the rest of one test method. Paper settings: 10 samples per prompt, temperature
0.2 (top_k 50, top_p 0.95 from the authors' main_sample.py), max 512 new tokens. Each sample is
cut at the end of its first method.
If the class file does not fit in CAT-LM's 8192-token window, the notebook uses a short version:
package, imports, class header, fields and the methods the fix changed (the SAME focal methods as
our framework and A3Test). No developer tests are shown (no tool sees them).
Each distinct generated test goes into its own class (one broken test cannot remove the others).
Tests that do not compile alone are dropped up front (= what fix_test_suite.pl would do; see
run_mltests.compiles_alone), then the usual protocol: fix_test_suite.pl on the fixed version,
run_bug_detection.pl on the buggy version, Defects4J coverage + Major.
"""
import argparse
import csv
import json
import re
import shutil
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "scripts"))

from our_framework import context, d4j, standard_metrics  # noqa: E402
from our_framework import java_parse as jp  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402
from run_mltests import add_throws, compiles_alone, count_tests, sh  # noqa: E402

OUT = RESULTS_DIR / "catlm"
BIN = d4j.D4J_BIN.parent
SEP = "<|codetestpair|>"
EXTRA_IMPORTS = ["import org.junit.Test;", "import static org.junit.Assert.*;",
                 "import static org.hamcrest.CoreMatchers.*;"]


def modified_classes(b):
    return [c.strip() for c in re.split(r"[;\n]", b.export("classes.modified")) if c.strip()]


def short_code(cls, ctx):
    """Class head + fields + the focal methods (used when the whole file is too long)."""
    parts = [f"package {ctx.package};", "", *cls.imports, "", cls.declaration + " {", ""]
    parts += ["    " + f for f in cls.fields] + [""]
    for m in ctx.focal_methods:
        parts += [m.full_source(), ""]
    return "\n".join(parts + ["}"])


def test_header(ctx, cls):
    imports = list(dict.fromkeys(EXTRA_IMPORTS + [i for i in cls.imports]))
    body = "\n".join([f"package {ctx.package};", "", *imports, "",
                      f"public class {ctx.class_name}Test {{", "", "    @Test", "    public void test"])
    return imports, body


def prepare(bugs):
    OUT.mkdir(parents=True, exist_ok=True)
    rows = []
    for bug in bugs:
        project, bid = bug.rsplit("-", 1)
        b = d4j.Bug(project, int(bid))
        b.checkout("f")
        for fqn in modified_classes(b):
            ctx = context.build(b, fqn, "f")
            row = {"bug": bug, "class": fqn}
            if ctx.skip_reason or not ctx.focal_methods:
                row["skip_reason"] = ctx.skip_reason or "no focal method"
                rows.append(row)
                continue
            cls = jp.read_class(ctx.class_src, ctx.class_name)
            imports, header = test_header(ctx, cls)
            row.update(package=ctx.package, class_name=ctx.class_name, imports=imports,
                       focal_methods=[m.name for m in ctx.focal_methods],
                       prompt_full=f"{ctx.class_src.rstrip()}\n{SEP}\n{header}",
                       prompt_short=f"{short_code(cls, ctx)}\n{SEP}\n{header}")
            rows.append(row)
        print(f"{bug}: {sum(1 for r in rows if r['bug'] == bug and 'prompt_full' in r)} prompt(s)", flush=True)
    with open(OUT / "prompts.jsonl", "w") as f:
        f.writelines(json.dumps(r) + "\n" for r in rows)
    print(f"{sum('prompt_full' in r for r in rows)} prompts -> {OUT / 'prompts.jsonl'}")


def first_method(generated):
    """'public void test' + generated text, cut where the method's braces close (strings and
    comments skipped). None if the method never closes within the 512 tokens."""
    text = "public void test" + generated
    depth, opened, i, n = 0, False, 0, len(text)
    while i < n:
        c = text[i]
        if text.startswith("//", i):
            i = text.find("\n", i)
            i = n if i < 0 else i
            continue
        if text.startswith("/*", i):
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        if c in "\"'":
            j = i + 1
            while j < n and text[j] != c:
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c == "{":
            depth, opened = depth + 1, True
        elif c == "}":
            depth -= 1
            if opened and depth == 0:
                return "@Test\n    " + text[:i + 1]
        i += 1
    return None


def evaluate_bug(bug, prompts, gens):
    project, bid = bug.rsplit("-", 1)
    report = {"bug": bug, "setting": "catlm", "detected": False, "llm_calls": {"fresh": 0, "cached": 0},
              "model": "nikitharao/catlm (2.7B), Colab T4, 10 samples, T=0.2"}
    work = TOOLS_DIR / "catlm_out" / bug
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    report["classes_info"] = [{k: p.get(k) for k in ("class", "focal_methods", "skip_reason") if k in p}
                              for p in prompts]
    b = d4j.Bug(project, int(bid))
    b.checkout("f")
    b.checkout("b")
    src = work / "src"
    seen, written, dropped, unclosed, samples = set(), 0, 0, 0, 0
    kept_tests = []
    for p in prompts:
        for g in gens.get((bug, p["class"]), []):
            for s in g["samples"]:
                samples += 1
                test = first_method(s)
                if test is None:
                    unclosed += 1
                    continue
                key = " ".join(test.split())
                if key in seen:
                    continue
                seen.add(key)
                name = f"{p['class_name']}_CL{len(seen) - 1}_Test"
                body = "\n".join([f"package {p['package']};", "", *p["imports"], "",
                                  f"public class {name} {{", "", "    " + add_throws(test), "}", ""])
                if not compiles_alone(b, p["package"], name, body):
                    dropped += 1
                    continue
                f = src / Path(*p["package"].split(".")) / f"{name}.java"
                f.parent.mkdir(parents=True, exist_ok=True)
                f.write_text(body)
                kept_tests.append({"class": name, "prompt": g.get("prompt_used")})
                written += 1
    distinct = len(seen)
    report["tests"] = {"samples": samples, "unclosed": unclosed, "distinct": distinct,
                       "precompile_dropped": dropped, "written": distinct}
    report["prompt_used"] = sorted({g.get("prompt_used", "?") for p in prompts
                                    for g in gens.get((bug, p["class"]), [])})
    if not written:
        report["tests"].update(kept_on_fixed=0, removed_on_fixed=distinct)
        report["error"] = ("no prompt (nothing to test)" if not any("prompt_full" in p for p in prompts)
                           else "no generated test compiles on the fixed version")
        return finish(report, work, log, t)
    suite = work / "suite"
    suite.mkdir()
    arch = suite / f"{project}-{bid}f-catlm.1.tar.bz2"
    with tarfile.open(arch, "w:bz2") as tar:
        for q in src.iterdir():
            tar.add(q, arcname=q.name)
    OUT.joinpath(bug).mkdir(parents=True, exist_ok=True)
    shutil.copy(arch, OUT / bug / "catlm_tests.tar.bz2")
    rc = sh([BIN.parent / "util" / "fix_test_suite.pl", "-p", project, "-d", suite, "-v", f"{bid}f"],
            log, timeout=3600)
    if rc == -1:
        report["error"] = "fix_test_suite.pl timed out"
        return finish(report, work, log, t)
    kept, flaky = count_tests(arch) if arch.exists() else (0, 0)
    report["tests"].update(kept_on_fixed=kept, removed_on_fixed=distinct - kept, assertions_commented_out=flaky)
    if arch.exists():
        shutil.copy(arch, OUT / bug / "catlm_tests_kept.tar.bz2")
    if not kept:
        report["error"] = "no generated test compiles and passes on the fixed version"
        return finish(report, work, log, t)
    sh([BIN / "run_bug_detection.pl", "-p", project, "-d", suite, "-o", work / "detect"], log)
    det = work / "detect" / "bug_detection"
    if det.exists():
        rows = list(csv.DictReader(det.open()))
        report["detection_rows"] = rows
        report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0 for r in rows)
    else:
        report["error"] = "bug detection did not run (see log)"
    report["standard_metrics"] = standard_metrics.measure_archive(project, suite, work / "std")
    return finish(report, work, log, t)


def finish(report, work, log, t):
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error", "detected": report["detected"]}]
    out = OUT / report["bug"]
    out.mkdir(parents=True, exist_ok=True)
    (out / "report.json").write_text(json.dumps(report, indent=2))
    if log.exists():
        shutil.copy(log, out / "log.txt")
    shutil.rmtree(work, ignore_errors=True)
    return report


def evaluate(bugs, gen_file):
    prompts = [json.loads(line) for line in open(OUT / "prompts.jsonl")]
    gens = {}
    for line in open(gen_file):
        g = json.loads(line)
        gens.setdefault((g["bug"], g["class"]), []).append(g)
    todo = [b for b in bugs if not (OUT / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [catlm]", flush=True)
    for bug in todo:
        ps = [p for p in prompts if p["bug"] == bug]
        if not any((bug, p["class"]) in gens for p in ps if "prompt_full" in p) and any("prompt_full" in p for p in ps):
            print(f"{bug}: no generations in {gen_file}, skipped", flush=True)
            continue
        r = evaluate_bug(bug, ps, gens)
        # per-bug copy of the raw samples, for inspection
        with open(OUT / bug / "generations.jsonl", "w") as f:
            for p in ps:
                f.writelines(json.dumps(g) + "\n" for g in gens.get((bug, p["class"]), []))
        print(f"{bug}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s) {r.get('tests', '')}"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("step", choices=["prepare", "evaluate"])
    ap.add_argument("bug_file")
    ap.add_argument("--gen", default=str(OUT / "generations.jsonl"), help="file downloaded from Colab")
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()
    if args.step == "prepare":
        prepare(bugs)
    else:
        evaluate(bugs, args.gen)
