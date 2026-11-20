"""Baseline: Randoop (classic feedback-directed random test generator, free, no AI).

  python3 scripts/run_randoop.py bugs/dev40.txt [--budget 120]

Same protocol as run_evosuite.py, using Defects4J's own scripts:
  gen_tests.pl -g randoop  - Randoop writes regression tests for the fixed version's
                             modified classes (time budget in seconds, same as EvoSuite)
  run_bug_detection.pl     - runs them on the buggy version
plus the same standard measurements (Defects4J coverage + Major mutation).
Results: results/randoop/<bug>/report.json (+ randoop_tests.tar.bz2). Finished bugs are skipped.
"""
import argparse
import csv
import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import d4j, standard_metrics  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402

BIN = d4j.D4J_BIN.parent
SEED = 1                      # fixed seed, so the run can be repeated


XMX = None                    # e.g. "3g": more Java memory, set by --xmx (retry of out-of-memory crashes)


def run(cmd, log, timeout=3600):
    env = d4j._env()
    if XMX:
        env["JAVA_TOOL_OPTIONS"] = f"-Xmx{XMX}"
    with open(log, "a") as f:
        try:
            return subprocess.run(cmd, env=env, stdout=f, stderr=subprocess.STDOUT,
                                  timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            f.write(f"\nTIMEOUT after {timeout}s: {' '.join(cmd)}\n")
            return -1


def one(bug, budget):
    project, bid = bug.rsplit("-", 1)
    work = TOOLS_DIR / "randoop_out" / bug           # no spaces: Defects4J scripts break on them
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    run([str(BIN / "gen_tests.pl"), "-g", "randoop", "-p", project, "-v", f"{bid}f", "-n", "1",
         "-o", str(work / "suites"), "-b", str(budget), "-s", str(SEED)], log)
    suite_dir = work / "suites" / project / "randoop" / "1"
    report = {"bug": bug, "setting": "randoop", "budget_s": budget, "seed": SEED, "java_xmx": XMX, "detected": False,
              "llm_calls": {"fresh": 0, "cached": 0}, "classes": []}
    archives = list(suite_dir.glob("*.tar.bz2"))
    if archives:
        # drop tests that fail on the fixed version (flaky / environment-dependent), as for every tool
        run([str(BIN.parent / "util" / "fix_test_suite.pl"), "-p", project, "-d", str(suite_dir),
             "-v", f"{bid}f"], log)
    archives = list(suite_dir.glob("*.tar.bz2"))
    if archives:
        run([str(BIN / "run_bug_detection.pl"), "-p", project, "-d", str(suite_dir),
             "-o", str(work / "detect")], log)
        det = work / "detect" / "bug_detection"
        if det.exists():
            rows = list(csv.DictReader(det.open()))
            report["detection_rows"] = rows
            report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0
                                     for r in rows)
        else:
            report["error"] = "bug detection did not run (see log)"
        report["standard_metrics"] = standard_metrics.measure_archive(project, suite_dir, work / "std")
        out_keep = RESULTS_DIR / "randoop" / bug
        out_keep.mkdir(parents=True, exist_ok=True)
        shutil.copy(archives[0], out_keep / "randoop_tests.tar.bz2")
    else:
        report["error"] = "Randoop produced no test suite (or none survived on the fixed version)"
        report["standard_metrics"] = None
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error",
                          "detected": report["detected"]}]
    out = RESULTS_DIR / "randoop" / bug
    out.mkdir(parents=True, exist_ok=True)
    (out / "report.json").write_text(json.dumps(report, indent=2))
    shutil.copy(log, out / "log.txt")
    shutil.rmtree(work, ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    ap.add_argument("--budget", type=int, default=120, help="Randoop time per bug (s); same as EvoSuite")
    ap.add_argument("--xmx", default=None, help="Java max memory, e.g. 3g")
    ap.add_argument("--retry-oom", action="store_true",
                    help="redo bugs whose Randoop run crashed with OutOfMemoryError")
    args = ap.parse_args()
    XMX = args.xmx
    bugs = Path(args.bug_file).read_text().split()
    if args.retry_oom:
        for b in bugs:
            lg = RESULTS_DIR / "randoop" / b / "log.txt"
            if lg.exists() and "OutOfMemoryError" in lg.read_text(errors="replace"):
                print(f"{b}: out of memory before, redo", flush=True)
                shutil.move(str(lg.parent), str(TOOLS_DIR / "randoop_out" / f"{b}-oom-first-try"))
    todo = [b for b in bugs if not (RESULTS_DIR / "randoop" / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [randoop]", flush=True)
    for b in todo:
        r = one(b, args.budget)
        print(f"{b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s)"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
