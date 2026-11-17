#!/bin/bash
# One job at a time (8 GB RAM): wait for running jobs, then finish our framework, then AdverTest.
# Safe to re-run: finished bugs are skipped. Stops cleanly when the free Gemini quota is used up.
cd "$(dirname "$0")/.."
source env.sh
while pgrep -f "run_evosuite.py|run_advertest.py|run_batch.py" >/dev/null; do sleep 60; done
echo "[$(date '+%F %H:%M')] START framework (full), remaining bugs of bugs/dev40.txt"
python3 -u scripts/run_batch.py bugs/dev40.txt --setting full
echo "[$(date '+%F %H:%M')] START AdverTest on bugs/dev40.txt"
python3 -u scripts/run_advertest.py bugs/dev40.txt
python3 -u scripts/summarize.py bugs/dev40.txt
echo "[$(date '+%F %H:%M')] ALL DONE"
