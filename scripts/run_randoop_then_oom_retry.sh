#!/bin/bash
# Wait for the running Randoop batch, then redo out-of-memory crashes once with 3 GB of Java memory.
cd "$(dirname "$0")/.."
source env.sh
while pgrep -f "scripts/run_randoop\.py" >/dev/null; do sleep 60; done
echo "[$(TZ=Asia/Kolkata date)] retry out-of-memory bugs with -Xmx3g"
python3 -u scripts/run_randoop.py bugs/dev40.txt --retry-oom --xmx 3g
echo "[$(TZ=Asia/Kolkata date)] DONE"
