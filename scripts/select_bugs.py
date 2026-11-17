"""Choose which Defects4J bugs to use (fixed random seed, so it is repeatable).

  python3 scripts/select_bugs.py

Writes:
  bugs/pilot.txt  - 50 bugs from Lang, Math and Chart (Phase 5 pilot)
  bugs/main.txt   - ~200 bugs spread over all 17 projects (Phase 6), up to
                    12 per project. Includes the pilot bugs from those projects
                    where possible, so pilot results can be reused.
"""
import random
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import d4j  # noqa: E402
from our_framework.config import PROJECT_ROOT  # noqa: E402

PER_PROJECT = 12
PILOT = {"Lang": 17, "Math": 17, "Chart": 16}


def active_bugs(project):
    out = subprocess.run([str(d4j.D4J_BIN), "bids", "-p", project], env=d4j._env(),
                         capture_output=True, text=True, check=True).stdout
    return [int(x) for x in out.split()]


def main():
    projects = subprocess.run([str(d4j.D4J_BIN), "pids"], env=d4j._env(),
                              capture_output=True, text=True, check=True).stdout.split()
    rng = random.Random(42)
    pilot, main_set = [], []
    for p in sorted(projects):
        ids = active_bugs(p)
        rng.shuffle(ids)
        if p in PILOT:
            pilot += [f"{p}-{i}" for i in sorted(ids[:PILOT[p]])]
        main_set += [f"{p}-{i}" for i in sorted(ids[:PER_PROJECT])]
    out = PROJECT_ROOT / "bugs"
    out.mkdir(exist_ok=True)
    (out / "pilot.txt").write_text("\n".join(pilot) + "\n")
    (out / "main.txt").write_text("\n".join(main_set) + "\n")
    print(f"pilot: {len(pilot)} bugs -> bugs/pilot.txt")
    print(f"main:  {len(main_set)} bugs -> bugs/main.txt")


if __name__ == "__main__":
    main()
