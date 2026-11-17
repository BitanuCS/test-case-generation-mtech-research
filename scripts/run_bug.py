"""Run the framework on one or more Defects4J bugs.

  python3 scripts/run_bug.py Lang-1
  python3 scripts/run_bug.py --setting realistic Lang-1 Math-2

Settings: full (default), realistic, no-context, no-planner, no-fix,
no-mutation, realistic-no-panel, single-shot.
Results go to results/<setting>/<bug>/.
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import llm, pipeline  # noqa: E402

if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("bugs", nargs="+")
    ap.add_argument("--setting", default="full", choices=list(pipeline.PRESETS))
    args = ap.parse_args()
    for name in args.bugs:
        try:
            s = pipeline.run_bug(name, args.setting)
            calls = s["llm_calls"]
            print(f"\n{name}: {'DETECTED' if s['detected'] else 'missed'}  {s['seconds']}s  "
                  f"AI calls: {calls['fresh']} new, {calls['cached']} cached  {calls['by_model']}")
        except llm.QuotaExhausted as e:
            sys.exit(f"Stopped: free AI quota used up for today. Run again tomorrow.\n{e}")
