#!/bin/bash
# Trial run on the 40-bug development set: our framework, then EvoSuite, then the tables.
# Safe to re-run: finished bugs are skipped.
cd "$(dirname "$0")/.."
source env.sh
echo "[$(date '+%F %H:%M')] START framework (full) on bugs/dev40.txt"
python3 -u scripts/run_batch.py bugs/dev40.txt --setting full
echo "[$(date '+%F %H:%M')] START EvoSuite on bugs/dev40.txt"
python3 -u scripts/run_evosuite.py bugs/dev40.txt
python3 -u scripts/summarize.py bugs/dev40.txt
echo "[$(date '+%F %H:%M')] ALL DONE"
