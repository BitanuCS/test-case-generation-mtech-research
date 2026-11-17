"""Run many bugs, safely, within the free AI limits.

  python3 scripts/run_batch.py bugs/pilot.txt
  python3 scripts/run_batch.py bugs/main.txt --setting realistic
  python3 scripts/run_batch.py bugs/pilot.txt --setting realistic-no-panel --keep-checkouts

- Bugs that already have a report for this setting are skipped, so you can
  simply run the same command again every day until it is done.
- When the free AI quota for today is used up, it stops cleanly.
- Checked-out bugs are deleted after each bug to save disk space
  (unless --keep-checkouts).
"""
import argparse
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import llm, pipeline  # noqa: E402

if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("bug_file")
    ap.add_argument("--setting", default="full", choices=list(pipeline.PRESETS))
    ap.add_argument("--keep-checkouts", action="store_true")
    ap.add_argument("--max-bugs", type=int, default=None, help="stop after this many new bugs")
    args = ap.parse_args()

    bugs = [b.strip() for b in Path(args.bug_file).read_text().split() if b.strip()]
    todo = [b for b in bugs if not pipeline.report_path(args.setting, b).exists()]
    print(f"{len(bugs)} bugs, {len(bugs) - len(todo)} already done, {len(todo)} to go  [{args.setting}]")

    done = failures_in_row = 0
    for name in todo:
        if args.max_bugs is not None and done >= args.max_bugs:
            break
        t = time.time()
        try:
            s = pipeline.run_bug(name, args.setting, cleanup=not args.keep_checkouts)
        except llm.QuotaExhausted:
            # remove the half-finished bug so it is redone next time
            partial = pipeline.report_path(args.setting, name)
            if partial.exists():
                partial.unlink()
            print(f"\nFree AI quota used up for today after {done} new bugs. "
                  f"Run the same command again tomorrow to continue.")
            break
        except Exception as e:  # noqa: BLE001 - e.g. a project that fails to build; record and go on
            print(f"!! {name} crashed: {e}")
            # record it as done-with-error so it is counted (as not detected) and not retried forever
            out = pipeline.report_path(args.setting, name)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps({"bug": name, "setting": args.setting, "detected": False,
                                       "error": str(e)[:2000], "llm_calls": {"fresh": 0, "cached": 0},
                                       "classes": [{"status": "error"}]}, indent=2))
            continue
        if s.get("incomplete"):
            pipeline.report_path(args.setting, name).unlink()
            failures_in_row += 1
            print(f"--> {name}: AI/network problem, will be retried next run\n", flush=True)
            if failures_in_row >= 2:
                print("Two bugs in a row hit AI/network problems: stopping. Check your internet and quota.")
                break
            continue
        failures_in_row = 0
        done += 1
        print(f"--> {name}: {'DETECTED' if s['detected'] else 'missed'} "
              f"({time.time() - t:.0f}s, {s['llm_calls']['fresh']} AI calls)\n", flush=True)

    left = sum(not pipeline.report_path(args.setting, b).exists() for b in bugs)
    print(f"Finished this run: {done} new bugs. Remaining: {left}.")
