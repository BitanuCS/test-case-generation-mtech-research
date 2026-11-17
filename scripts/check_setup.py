"""Phase 1 check: is everything working?

  1. Defects4J: check out Lang-1 (fixed and buggy), compile, run the
     developer's bug-revealing test on both versions.
  2. AI: send one question to each provider that has a key in .env.

Run:  python3 scripts/check_setup.py
"""
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import d4j, llm  # noqa: E402
from our_framework.config import MODELS, PROVIDERS  # noqa: E402


def check_defects4j():
    print("== Defects4J ==")
    bug = d4j.Bug("Lang", 1)
    for v in ("f", "b"):
        wd = bug.checkout(v)
        print(f"checked out {bug.name}{v} -> {wd}")
        res = bug.compile(v)
        print(f"  compile {v}: {'OK' if res.ok else 'FAILED'}")
        if not res.ok:
            print(res.output[-2000:])
            return False

    trigger = bug.export("tests.trigger").splitlines()[0]
    print(f"  bug-revealing test: {trigger}")
    on_fixed = bug.test("f", trigger)
    on_buggy = bug.test("b", trigger)
    fixed_fail = d4j.failing_tests(on_fixed.stdout)
    buggy_fail = d4j.failing_tests(on_buggy.stdout)
    print(f"  on fixed version: {len(fixed_fail)} failing (expect 0)")
    print(f"  on buggy version: {len(buggy_fail)} failing (expect >= 1)")
    return not fixed_fail and buggy_fail


def check_llm():
    print("\n== AI providers ==")
    any_ok = False
    used = list(dict.fromkeys([*MODELS["main"], *[m for member in MODELS["panel"] for m in member]]))
    for name, model in used:
        key_env = PROVIDERS[name]["key_env"]
        if not os.environ.get(key_env):
            print(f"  {name}/{model}: skipped (no {key_env} in .env)")
            continue
        try:
            answer = llm.chat(
                [{"role": "user", "content": "Reply with exactly: OK"}],
                models=[(name, model)],
            )
            print(f"  {name} ({model}): {answer.strip()[:50]}")
            any_ok = True
        except Exception as e:  # noqa: BLE001 - report and keep checking others
            print(f"  {name}: ERROR {e}")
    return any_ok


if __name__ == "__main__":
    d4j_ok = check_defects4j()
    llm_ok = check_llm()
    print("\nDefects4J:", "READY" if d4j_ok else "NOT READY")
    print("AI:       ", "READY" if llm_ok else "NOT READY")
