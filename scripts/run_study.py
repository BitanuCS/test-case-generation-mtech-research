"""Run the WHOLE study, unattended, within the free AI limits.

  python3 scripts/run_study.py

Works through the jobs below in order. When the free daily quota runs out,
it waits and tries again every hour (Gemini's quota resets once a day), so
it finishes by itself over a few days. Finished bugs are always skipped, so
it is safe to stop and start again at any time.

The Mac must stay awake (plugged in, lid open) while it runs.
"""
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from our_framework import pipeline  # noqa: E402

# (bug list, setting) in order of importance
JOBS = [
    ("bugs/main.txt", "full"),                 # main comparison: 198 bugs
    ("bugs/pilot.txt", "full"),
    ("bugs/pilot.txt", "single-shot"),
    ("bugs/pilot.txt", "no-mutation"),
    ("bugs/pilot.txt", "no-context"),
    ("bugs/pilot.txt", "no-fix"),
    ("bugs/pilot.txt", "no-planner"),
    ("bugs/main.txt", "single-shot"),          # simple AI baseline on all 198
    ("bugs/pilot.txt", "realistic-no-panel"),
    ("bugs/pilot.txt", "realistic"),           # slowest: many panel votes
    ("bugs/pilot.txt", "full-repeat2"),        # repeated runs: how stable are the results?
    ("bugs/pilot.txt", "full-repeat3"),
]
WAIT_SECONDS = 3600


def remaining(bug_file, setting):
    bugs = (ROOT / bug_file).read_text().split()
    return [b for b in bugs if not pipeline.report_path(setting, b).exists()]


def log(msg):
    print(f"[{datetime.now():%Y-%m-%d %H:%M}] {msg}", flush=True)


def evosuite_remaining():
    import json
    out = []
    for b in (ROOT / "bugs" / "all.txt").read_text().split():
        rep = pipeline.RESULTS_DIR / "evosuite" / b / "report.json"
        if not rep.exists() or "standard_metrics" not in json.loads(rep.read_text()):
            out.append(b)
    return out


def run_evosuite(minutes):
    """EvoSuite needs no AI: run it while the AI quota is paused (never in parallel:
    with 8 GB RAM, running both at once made EvoSuite crash)."""
    log(f"--- EvoSuite baseline: {len(evosuite_remaining())} bugs left (for up to {minutes} min)")
    subprocess.run([sys.executable, "-u", str(ROOT / "scripts" / "run_evosuite.py"), "bugs/all.txt",
                    "--redo-without-metrics", "--max-minutes", str(minutes)], cwd=ROOT)


def main():
    log(f"Results folder: {pipeline.RESULTS_DIR}")
    while True:
        # fill in standard metrics for any finished bug that lacks them (no AI needed)
        subprocess.run([sys.executable, "-u", str(ROOT / "scripts" / "backfill_standard_metrics.py")], cwd=ROOT)
        todo = [(f, s) for f, s in JOBS if remaining(f, s)]
        if not todo:
            if evosuite_remaining():
                run_evosuite(600)
                continue
            log("STUDY COMPLETE - every job is done.")
            return
        for bug_file, setting in todo:
            left = len(remaining(bug_file, setting))
            log(f"--- {setting} on {bug_file}: {left} bugs left")
            subprocess.run([sys.executable, "-u", str(ROOT / "scripts" / "run_batch.py"),
                            bug_file, "--setting", setting], cwd=ROOT)
            if remaining(bug_file, setting):
                break      # stopped early (quota or network): wait, then continue
        if any(remaining(f, s) for f, s in JOBS):
            if evosuite_remaining():
                run_evosuite(WAIT_SECONDS // 60)      # use the waiting time for EvoSuite
            else:
                log(f"Pausing {WAIT_SECONDS // 60} minutes (free quota or network), then continuing.")
                time.sleep(WAIT_SECONDS)


if __name__ == "__main__":
    main()
