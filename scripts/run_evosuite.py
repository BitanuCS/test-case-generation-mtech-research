"""Baseline: EvoSuite (classic search-based test generator, free, no AI).

  python3 scripts/run_evosuite.py bugs/pilot.txt [--budget 120]

Uses Defects4J's own scripts, exactly like published studies:
  gen_tests.pl          - EvoSuite writes tests for the fixed version's
                          modified classes (search budget in seconds)
  run_bug_detection.pl  - runs them on the buggy version
Results go to results/evosuite/<bug>/report.json, so summarize.py includes
them. (Only bug detection is recorded for EvoSuite.)
Already finished bugs are skipped.
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


def _evo_env():
    # EvoSuite's two processes talk over RMI. By default they use the Mac's Wi-Fi
    # address, which breaks when Wi-Fi sleeps or changes (e.g. at night) and then
    # every run fails with "no such object in table". Force the local loopback.
    env = d4j._env()
    env["JAVA_TOOL_OPTIONS"] = "-Djava.rmi.server.hostname=127.0.0.1 -Djava.net.preferIPv4Stack=true"
    return env


def run(cmd, log):
    with open(log, "a") as f:
        return subprocess.run(cmd, env=_evo_env(), stdout=f, stderr=subprocess.STDOUT, timeout=3600).returncode


# EvoSuite talks to its helper process over RMI. A helper left behind by an
# earlier crash keeps the port busy, and then every new run fails at once with
# "Error in starting clients" / "no such object in table".
CRASH_SIGNS = ("Error in starting clients", "NoSuchObjectException", "Connection refused to host")


def kill_leftover_evosuite():
    subprocess.run(["pkill", "-f", "evosuite-current.jar"], capture_output=True)
    subprocess.run(["pkill", "-f", "org.evosuite"], capture_output=True)
    time.sleep(1)


def crashed(log):
    text = log.read_text(errors="replace") if log.exists() else ""
    return any(sign in text for sign in CRASH_SIGNS)


def one(bug, budget):
    """Returns the report, or None if EvoSuite crashed (nothing is saved, so it is retried later)."""
    kill_leftover_evosuite()
    project, bid = bug.rsplit("-", 1)
    work = TOOLS_DIR / "evosuite_out" / bug
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    run([str(BIN / "gen_tests.pl"), "-g", "evosuite", "-p", project, "-v", f"{bid}f", "-n", "1",
         "-o", str(work / "suites"), "-b", str(budget)], log)
    suite_dir = work / "suites" / project / "evosuite" / "1"
    report = {"bug": bug, "setting": "evosuite", "budget_s": budget, "detected": False,
              "llm_calls": {"fresh": 0, "cached": 0}, "classes": []}
    if list(suite_dir.glob("*.tar.bz2")):
        run([str(BIN / "run_bug_detection.pl"), "-p", project, "-d", str(suite_dir),
             "-o", str(work / "detect")], log)
        det = work / "detect" / "bug_detection"
        if det.exists():
            rows = list(csv.DictReader(det.open()))
            report["detection_rows"] = rows
            # "Broken" suites (don't compile on the buggy version) show "-": not detected
            report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0
                                     for r in rows)
    elif crashed(log):
        return None                            # tool crash, not a result: never overwrite anything
    else:
        report["error"] = "EvoSuite produced no test suite"
        report["standard_metrics"] = None      # nothing to measure; counts as done (not detected)
    if list(suite_dir.glob("*.tar.bz2")):
        # same standard measurements as our framework (coverage + Major mutation score)
        report["standard_metrics"] = standard_metrics.measure_archive(project, suite_dir, work / "std")
        out_keep = RESULTS_DIR / "evosuite" / bug
        out_keep.mkdir(parents=True, exist_ok=True)
        for a in suite_dir.glob("*.tar.bz2"):            # keep the tests for later checks
            shutil.copy(a, out_keep / "evosuite_tests.tar.bz2")
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error",
                          "detected": report["detected"]}]
    out = RESULTS_DIR / "evosuite" / bug
    out.mkdir(parents=True, exist_ok=True)
    old = out / "report.json"
    if "error" in report and old.exists() and "error" not in json.loads(old.read_text()):
        return json.loads(old.read_text())     # never replace a good result with a failed one
    (out / "report.json").write_text(json.dumps(report, indent=2))
    shutil.rmtree(work / "suites", ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    ap.add_argument("--budget", type=int, default=120, help="EvoSuite search time per class (s)")
    ap.add_argument("--max-minutes", type=float, default=None, help="stop starting new bugs after this time")
    ap.add_argument("--redo-without-metrics", action="store_true",
                    help="also redo finished bugs that have no standard metrics yet")
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()

    def needs(b):
        rep = RESULTS_DIR / "evosuite" / b / "report.json"
        if not rep.exists():
            return True
        return args.redo_without_metrics and "standard_metrics" not in json.loads(rep.read_text())
    todo = [b for b in bugs if needs(b)]
    print(f"{len(bugs)} bugs, {len(todo)} to go")
    t_start = time.time()
    for b in todo:
        if args.max_minutes and time.time() - t_start > args.max_minutes * 60:
            print(f"Time limit of {args.max_minutes} minutes reached; continuing next time.")
            break
        r = one(b, args.budget)
        if r is None or "error" in r:
            # EvoSuite sometimes crashes (e.g. low memory): try once more before recording a failure
            r = one(b, args.budget)
        if r is None:
            print(f"{b}: EvoSuite crashed twice; nothing saved, will retry next time", flush=True)
            continue
        print(f"{b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s)"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
