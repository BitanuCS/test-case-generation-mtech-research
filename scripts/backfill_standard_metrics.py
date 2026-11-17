"""Add the standard Defects4J measurements (coverage + Major mutation score)
to finished bugs that don't have them yet. Uses the saved final tests: no AI.

  python3 scripts/backfill_standard_metrics.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import standard_metrics  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402

todo = []
for rep in sorted(RESULTS_DIR.glob("*/*/report.json")):
    if rep.parent.parent.name in ("evosuite",) or rep.parent.parent.is_symlink():
        continue
    r = json.loads(rep.read_text())
    if r.get("standard_metrics") or r.get("incomplete") or "classes" not in r:
        continue
    ok = [c for c in r["classes"] if c.get("status") == "ok"]
    if ok:
        todo.append((rep, r, ok))
print(f"{len(todo)} finished bugs need standard metrics")
for rep, r, ok in todo:
    gen = "f" if ok[0].get("mode", "standard") == "standard" else "b"
    dirs = [rep.parent / c["class"].rsplit(".", 1)[-1] for c in ok]
    reports = [t for c in ok for t in c.get("bug_reports", [])]
    r["standard_metrics"] = standard_metrics.measure_results(r["bug"], gen, dirs, reports)
    rep.write_text(json.dumps(r, indent=2, default=str))
    print(rep.parent.parent.name, r["bug"], json.dumps(r["standard_metrics"])[:150], flush=True)
