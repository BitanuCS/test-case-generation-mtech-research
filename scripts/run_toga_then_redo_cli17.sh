#!/bin/bash
# After the TOGA dev40 run: redo Cli-17, which crashed in TOGA (no assertion candidates) before toga.py was patched.
cd "$(dirname "$0")/.."; source env.sh
while pgrep -f "scripts/run_toga\.py" >/dev/null; do sleep 60; done
echo "[$(date)] redo Cli-17 with patched toga.py"
mv results/toga/Cli-17 /Users/bitu/mtech-tools/toga_out/Cli-17-first-try
python3 -u scripts/run_toga.py bugs/dev40.txt
echo "[$(date)] DONE"
