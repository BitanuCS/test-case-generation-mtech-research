#!/bin/bash
# Waits until EvoSuite is finished (never run both at once: 8 GB RAM), then runs
# our framework on the unfinished bugs of bugs/dev40.txt. Only our framework: no AdverTest.
cd "$(dirname "$0")/.."
source env.sh
while pgrep -f "scripts/run_evosuite\.py|scripts/evosuite_extras\.py|comparison/comparison\.py" >/dev/null; do sleep 60; done
echo "[$(date '+%F %H:%M')] START framework (full), remaining bugs"
python3 -u scripts/run_batch.py bugs/dev40.txt --setting full
python3 -u scripts/summarize.py bugs/dev40.txt
echo "[$(date '+%F %H:%M')] DONE"
