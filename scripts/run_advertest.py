"""Baseline: AdverTest (Chang et al., ISSTA 2026) on our bugs, with the SAME free model.

  python3 scripts/run_advertest.py bugs/dev40.txt

AdverTest's own code (github.com/jmueducn/advertest) lives in
/Users/bitu/mtech-tools/advertest and runs unchanged, except:
  - LLM: gemini-3.5-flash-lite (the model our framework uses), not DeepSeek;
  - its own patched COPY of Defects4J (defects4j-advertest), so our install is untouched;
  - a small `timeout` command for macOS.

Each bug gets two verdicts:
  advertest_verdict  - AdverTest's own rule (its test.py); reported for transparency
  detected           - the SAME rule as for EvoSuite and our framework:
                       Defects4J fix_test_suite.pl removes tests failing on the fixed
                       version, then run_bug_detection.pl runs the rest on the buggy one.
Plus the same standard metrics (Defects4J coverage + Major mutation).
Results: results/advertest/<bug>/report.json (+ the generated tests). Finished bugs are skipped.
Stops when Gemini's daily quota is used up; run again the next day.
"""
import argparse
import csv
import json
import os
import shutil
import subprocess
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from our_framework import d4j, standard_metrics  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402

ADV = TOOLS_DIR / "advertest"
ADV_D4J = TOOLS_DIR / "defects4j-advertest"
ADV_BIN = ADV_D4J / "framework" / "bin"
PY = ADV / ".venv" / "bin" / "python"
OUT = RESULTS_DIR / "advertest"


def env():
    e = d4j._env()
    e["PATH"] = f"{ADV / 'bin'}:{ADV_BIN}:{e['PATH']}"     # AdverTest's defects4j + timeout first
    from dotenv import dotenv_values                     # same .env reading as our framework
    e["GEMINI_API_KEY"] = dotenv_values(ROOT / ".env").get("GEMINI_API_KEY", "")   # never printed
    return e


def sh(cmd, log, timeout=3600):
    with open(log, "a") as f:
        return subprocess.run(cmd, cwd=ADV, env=env(), stdout=f, stderr=subprocess.STDOUT,
                              timeout=timeout).returncode


def checkout(project, bid, log):
    for v, sub in (("f", "fixed"), ("b", "bug")):
        wd = ADV / f"defects4j_{'fixed' if v == 'f' else 'bug'}" / project / f"{project}_{bid}_{sub}"
        if not (wd / ".defects4j.config").exists():
            shutil.rmtree(wd, ignore_errors=True)
            sh([str(ADV_BIN / "defects4j"), "checkout", "-p", project, "-v", f"{bid}{v}", "-w", str(wd)], log)
    return ADV / "defects4j_fixed" / project / f"{project}_{bid}_fixed"


def evaluate(project, bid, fixed_wd, out, log):
    """Same protocol as for EvoSuite: drop tests failing on fixed, then run on buggy."""
    gentest = fixed_wd / "gentest"
    if not gentest.exists() or not list(gentest.rglob("*.java")):
        return {"detected": False, "error": "AdverTest produced no test file"}
    suite = out / "suite"
    shutil.rmtree(suite, ignore_errors=True)
    suite.mkdir(parents=True)
    arch = suite / f"{project}-{bid}f-advertest.1.tar.bz2"
    with tarfile.open(arch, "w:bz2") as tar:              # package folders at the root (Defects4J format)
        for p in gentest.iterdir():
            tar.add(p, arcname=p.name)
    shutil.copy(arch, out / "advertest_tests.tar.bz2")      # keep the raw tests
    sh([str(ADV_BIN / "fix_test_suite.pl"), "-p", project, "-d", str(suite), "-v", f"{bid}f"], log)
    res = {"detected": False}
    if not arch.exists():
        res["error"] = "no test survived on the fixed version"
        return res
    det_dir = out / "detect"
    shutil.rmtree(det_dir, ignore_errors=True)
    sh([str(ADV_BIN / "run_bug_detection.pl"), "-p", project, "-d", str(suite), "-o", str(det_dir)], log)
    det = det_dir / "bug_detection"
    if det.exists():
        rows = list(csv.DictReader(det.open()))
        res["detection_rows"] = rows
        res["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0
                              for r in rows)
    res["standard_metrics"] = standard_metrics.measure_archive(project, suite, out / "std", bin_dir=ADV_BIN)
    shutil.rmtree(out / "std" / "tmp", ignore_errors=True)
    return res


def one(bug):
    project, bid = bug.rsplit("-", 1)
    out = OUT / bug
    shutil.rmtree(out, ignore_errors=True)
    out.mkdir(parents=True)
    log = out / "log.txt"
    t = time.time()
    fixed_wd = checkout(project, bid, log)
    shutil.rmtree(fixed_wd / "gentest", ignore_errors=True)
    stats_file = out / "advertest_stats.json"
    code = sh([str(PY), "-u", "run_one.py", project, bid, str(stats_file)], log, timeout=4 * 3600)
    stats = json.loads(stats_file.read_text()) if stats_file.exists() else {}
    if code == 3 or stats.get("quota_exhausted"):
        shutil.rmtree(out, ignore_errors=True)            # incomplete: redo this bug next time
        return "quota"
    if stats.get("result") == "error" and not stats.get("calls"):
        # crashed before asking the AI: a setup problem, not a result -> nothing saved
        print(f"   setup error, not saved: {stats.get('error', 'no stats (see log)')[:300]}", flush=True)
        (OUT / "_errors").mkdir(parents=True, exist_ok=True)
        shutil.copy(log, OUT / "_errors" / f"{bug}.log")
        shutil.rmtree(out, ignore_errors=True)
        return "setup_error"
    report = {"bug": bug, "setting": "advertest", "model": stats.get("model"),
              "advertest_verdict": stats.get("result"),
              "llm_calls": {"fresh": stats.get("calls", 0), "cached": 0,
                            "tokens_in": stats.get("tokens_in", 0), "tokens_out": stats.get("tokens_out", 0)}}
    report.update(evaluate(project, bid, fixed_wd, out, log))
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error", "detected": report["detected"]}]
    (out / "report.json").write_text(json.dumps(report, indent=2))
    shutil.rmtree(out / "suite", ignore_errors=True)
    for d in ("defects4j_fixed", "defects4j_bug"):         # free disk space
        shutil.rmtree(ADV / d / project / f"{project}_{bid}_{'fixed' if d.endswith('fixed') else 'bug'}",
                      ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    ap.add_argument("--max-bugs", type=int, default=None)
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()
    todo = [b for b in bugs if not (OUT / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [advertest]", flush=True)
    for i, b in enumerate(todo):
        if args.max_bugs and i >= args.max_bugs:
            break
        r = one(b)
        if r == "quota":
            print(f"Free Gemini quota used up at {b}; run the same command again tomorrow.", flush=True)
            break
        if r == "setup_error":
            continue
        print(f"--> {b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s, "
              f"{r['llm_calls']['fresh']} AI calls; AdverTest's own verdict: {r['advertest_verdict']})"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
