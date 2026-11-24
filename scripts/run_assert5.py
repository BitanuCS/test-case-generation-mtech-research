"""Baseline: AsserT5 (AST 2025; Primbs, Fein, Fraser) -- CodeT5-large fine-tuned to write test assertions.

  python3 scripts/run_assert5.py bugs/dev40.txt

Like TOGA, AsserT5 writes only the oracle, not the test inputs. Its own Defects4J study also used
EvoSuite tests with the assertions removed. We use exactly TOGA's inputs, so TOGA vs AsserT5
differs only in the oracle model:
 - the SAME EvoSuite tests as the EvoSuite baseline (results/evosuite/<bug>/evosuite_tests.tar.bz2);
 - the SAME test prefixes and focal methods (scripts/toga_prepare.py: every EvoSuite test is cut
   at each assertion, the focal method is the last called method of the class under test);
 - AsserT5 (scripts/assert5_predict.py) writes one assertion at the end of each prefix.
Declared difference from the AsserT5 paper: there, the placeholder was put where EvoSuite's
assertion FAILED on the buggy version (only bug-revealing tests were used). Here the buggy version
is not looked at, as for every other tool. AsserT5 cannot predict expected exceptions (its own
runner skips them), so for try/fail/catch prefixes it also just writes an assertion.
Then the usual protocol: fix_test_suite.pl (drops tests that fail or do not compile on the fixed
version = false alarms), run_bug_detection.pl on the buggy version, Defects4J coverage + Major.
One test per class (as for TOGA), so one broken test cannot remove the others.
Results: results/assert5/<bug>/report.json (+ assert5_tests.tar.bz2). Finished bugs are skipped.
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

from our_framework import d4j, standard_metrics  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402
from run_toga import TOGA_PY, count_tests, get_prefix, insert_assertion, sh  # noqa: E402

BIN = d4j.D4J_BIN.parent
OUT = RESULTS_DIR / "assert5"


def write_suite(evo_src, rows, dst_src):
    """One test per class, keeping the EvoSuite file's imports, runner and scaffolding."""
    shutil.copytree(evo_src, dst_src)
    originals = {}
    for f in dst_src.rglob("*_ESTest.java"):
        text = f.read_text(errors="replace")
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M).group(1)
        originals[f"{pkg}.{f.stem}"] = (f, text[:text.find("@Test")])
    n = 0
    for k, r in enumerate(rows):
        if not r.get("prediction"):
            continue
        cls = r["test_name"].split("::")[0]
        if cls not in originals:
            continue
        f, header = originals[cls]
        new_cls = f"{f.stem}_A5_{k}"
        header = re.sub(rf"\bclass\s+{f.stem}\b", f"class {new_cls}", header, count=1)
        test = insert_assertion(r["prefix_test"], r["prediction"].rstrip(";").strip())
        (f.parent / f"{new_cls}.java").write_text(header + test + "\n}\n")
        n += 1
    for f, _ in originals.values():
        f.unlink()                       # EvoSuite's own oracles are not part of AsserT5's suite
    return n


def one(bug):
    project, bid = bug.rsplit("-", 1)
    evo_arch = RESULTS_DIR / "evosuite" / bug / "evosuite_tests.tar.bz2"
    report = {"bug": bug, "setting": "assert5", "detected": False, "llm_calls": {"fresh": 0, "cached": 0},
              "model": "se2p/AsserT5 1/raw (CodeT5-large), greedy",
              "input_tests": "EvoSuite tests of results/evosuite, TOGA's prefixes (scripts/toga_prepare.py)"}
    work = TOOLS_DIR / "assert5_out" / bug
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    if not evo_arch.exists():
        report["error"] = "no EvoSuite tests for this bug (AsserT5 needs test prefixes)"
        return finish(report, work, log, t)
    evo_src = work / "evo"
    evo_src.mkdir()
    with tarfile.open(evo_arch) as tar:
        tar.extractall(evo_src, filter="data")
    for junk in evo_src.rglob("._*"):
        junk.unlink()
    b = d4j.Bug(project, int(bid))
    b.checkout("f")
    src_dir = b.workdir("f") / b.export("dir.src.classes")
    sh([TOGA_PY, ROOT / "scripts" / "toga_prepare.py", evo_src, src_dir, project, bid, work], log)
    if not (work / "inputs.csv").exists():
        report["error"] = "could not extract test prefixes (see log)"
        return finish(report, work, log, t)
    inputs = list(csv.DictReader((work / "inputs.csv").open()))
    meta = list(csv.DictReader((work / "meta.csv").open()))
    rows = []
    for inp, m in zip(inputs, meta):
        prefix_test = "@Test(timeout = 4000)\n" + get_prefix(inp["test_prefix"])
        rows.append({"test_name": m["test_name"], "prefix_test": prefix_test,
                     "test_method": prefix_test + "\n}", "focal_method": inp["focal_method"]})
    if not rows:
        report["error"] = "no test prefixes"
        return finish(report, work, log, t)
    with open(work / "a5_in.jsonl", "w") as f:
        f.writelines(json.dumps(r) + "\n" for r in rows)
    t_gen = time.time()
    sh([TOGA_PY, ROOT / "scripts" / "assert5_predict.py", work / "a5_in.jsonl", work / "a5_out.jsonl"], log)
    report["gen_seconds"] = round(time.time() - t_gen, 1)
    if not (work / "a5_out.jsonl").exists():
        report["error"] = "AsserT5 produced no predictions (see log)"
        return finish(report, work, log, t)
    rows = [json.loads(line) for line in open(work / "a5_out.jsonl")]
    report["oracles"] = {"prefixes": len(rows), "with_focal_method": sum(bool(r["focal_method"]) for r in rows),
                         "predicted": sum(bool(r.get("prediction")) for r in rows)}
    suite = work / "suite"
    suite.mkdir()
    n_written = write_suite(evo_src, rows, work / "a5_src")
    OUT.joinpath(bug).mkdir(parents=True, exist_ok=True)
    if not n_written:
        report["error"] = "no AsserT5 test written"
        return finish(report, work, log, t)
    arch = suite / f"{project}-{bid}f-assert5.1.tar.bz2"
    with tarfile.open(arch, "w:bz2") as tar:
        for p in (work / "a5_src").iterdir():
            tar.add(p, arcname=p.name)
    shutil.copy(arch, OUT / bug / "assert5_tests.tar.bz2")
    rc = sh([BIN.parent / "util" / "fix_test_suite.pl", "-p", project, "-d", suite, "-v", f"{bid}f"],
            log, timeout=3600)
    if rc == -1:
        report["error"] = "fix_test_suite.pl timed out"
        return finish(report, work, log, t)
    kept, flaky_asserts = count_tests(arch) if arch.exists() else (0, 0)
    report["tests"] = {"written": n_written, "kept_on_fixed": kept, "false_alarms_removed": n_written - kept,
                       "assertions_commented_out": flaky_asserts}
    if arch.exists():
        shutil.copy(arch, OUT / bug / "assert5_tests_kept.tar.bz2")
    if not kept:
        report["error"] = "no AsserT5 test survived on the fixed version"
        return finish(report, work, log, t)
    sh([BIN / "run_bug_detection.pl", "-p", project, "-d", suite, "-o", work / "detect"], log)
    det = work / "detect" / "bug_detection"
    if det.exists():
        rows_d = list(csv.DictReader(det.open()))
        report["detection_rows"] = rows_d
        report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0
                                 for r in rows_d)
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
    if (work / "a5_out.jsonl").exists():
        shutil.copy(work / "a5_out.jsonl", out / "predictions.jsonl")
    shutil.rmtree(work, ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()
    todo = [b for b in bugs if not (OUT / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [assert5]", flush=True)
    for b in todo:
        r = one(b)
        print(f"{b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s) {r.get('tests', '')}"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
