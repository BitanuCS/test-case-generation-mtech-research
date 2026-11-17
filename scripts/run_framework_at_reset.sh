#!/bin/bash
# Gemini's free daily quota resets at midnight Pacific time (13:30 IST).
# Wait until 13:35 IST, then run our framework on the unfinished bugs of bugs/dev40.txt.
cd "$(dirname "$0")/.."
while [ "$(TZ=Asia/Kolkata date +%H%M)" -lt 1335 ]; do sleep 120; done
exec scripts/run_framework_rest.sh
