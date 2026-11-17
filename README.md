# CANDOR-D4J: multi-agent AI test generation for real bugs

This framework uses several AI "agents" that work together to write JUnit tests able to catch **real bugs** from the Defects4J dataset. It combines:
- CANDOR's idea of several AIs **voting** on the correct expected results;
- AdverTest's **fake-bug challenge** (mutants);
- two new parts: a **Context Agent** that gives the AI the information it needs about the project, and a **compile-and-fix loop**.

The simple explanation of the idea is in [plan.md](plan.md), and the time plan is in [phases.md](phases.md).

## How it works (one bug)

| Step | Who | What happens |
|---|---|---|
| 1. Understand | Context Agent (no AI) | Finds the method(s) the bug is in, then collects the class info, Javadoc and helper methods. The developers' own test methods are hidden (no cheating). |
| 2. Plan | Planner (AI) | Lists 10–20 test situations: normal, edge and error cases. |
| 3. Write | Tester (AI) | Writes the JUnit test class. |
| 4. Fix | Fixer (AI) + Oracle Panel (3 AIs) + Curator | Compiles and runs the tests, and repairs errors. In *realistic* mode the panel votes on each failing test: is the test wrong, or the code? |
| 5. Clean up | no AI | Removes tests that still don't compile, fail, or are flaky (each test runs 3 times). |
| 6. Challenge | Mutant Agent (AI) + rule-based mutants | Puts fake bugs into the code. If the tests miss them, the Tester writes new tests to catch them (2 rounds). |
| 7. Evaluate | no AI | Runs the tests on the other version of the code. Also measures coverage (JaCoCo) and mutation score. |

### Two modes
- **standard**, the usual Defects4J protocol used by EvoSuite, HITS and AdverTest:
  - tests are written on the **fixed** code, which is assumed correct;
  - if a test fails on the **buggy** code, the bug is found.
- **realistic**, which is closer to real life:
  - tests are written on the **buggy** code, without knowing it is buggy;
  - when a test fails, the panel decides whether it is a bug report or a wrong test;
  - a bug report that passes on the fixed code is a real bug found; if it fails there too, it's a false alarm.

## Setup (already done on this Mac)
- Java 11 (Homebrew `openjdk@11`), Perl libraries, Subversion and wget.
- Defects4J, the Perl libraries and JaCoCo live in `/Users/bitu/mtech-tools`, because Defects4J breaks on folder names with spaces.
- Python packages: `pip3 install -r requirements.txt`.
- AI keys go in `.env` (see `.env.example`). Only free tiers are used:
  - **Gemini 3.5 flash-lite** does every big job (one model, for a fair comparison);
  - the oracle panel is flash-lite plus two **Groq** models (gpt-oss and Qwen).

Check everything with:
```
python3 scripts/check_setup.py
```

## Running

```bash
# one bug
python3 scripts/run_bug.py Lang-1
python3 scripts/run_bug.py --setting realistic Lang-1

# many bugs (resumable: run the same command again each day)
python3 scripts/run_batch.py bugs/pilot.txt                 # 50 pilot bugs, full framework
python3 scripts/run_batch.py bugs/main.txt                  # 198 bugs over all 17 projects
python3 scripts/run_batch.py bugs/pilot.txt --setting single-shot

# baseline without AI
python3 scripts/run_evosuite.py bugs/pilot.txt

# results table
python3 scripts/summarize.py bugs/pilot.txt
```

### Settings (for the comparison and the remove-one-step study)

| Setting | What it is |
|---|---|
| `full` | The whole framework, standard mode |
| `realistic` | The whole framework, realistic mode, with the oracle panel |
| `realistic-no-panel` | Realistic mode, but trusts the code instead of the panel |
| `no-context` | The AI sees only the method under test |
| `no-planner` | No planning step |
| `no-fix` | No AI repair (broken tests are just removed) |
| `no-mutation` | No fake-bug challenge |
| `single-shot` | One prompt, no planner, fixer, panel or challenge (a simple LLM baseline) |
| `evosuite` | EvoSuite, via `run_evosuite.py` |

## Free-tier limits

| Provider | Limit (as measured Nov 2026) |
|---|---|
| Gemini | about 20 requests per day per model |
| Groq | 1,000 requests per day, but only 8,000 tokens per minute |
| OpenRouter | 50 free requests per day |

- One bug needs about 6–12 AI calls, so about **10–15 bugs per day** fit in the free quota.
- `run_batch.py` stops by itself when the quota is used up, and continues the next day.
- Every AI answer is cached in `cache/llm_cache.sqlite`, so re-running never costs extra.

## Where things are

| Path | What |
|---|---|
| `our_framework/context.py` | Context Agent |
| `our_framework/agents.py` | All AI prompts: Planner, Tester, Fixer, Panel judge, Mutant agent, Killer |
| `our_framework/pipeline.py` | The steps, the two modes and the settings |
| `our_framework/mutation.py` | Rule-based and AI mutants, and running tests on them |
| `our_framework/runner.py` | Compiles and runs tests against a bug checkout |
| `our_framework/coverage.py` | JaCoCo line and branch coverage |
| `our_framework/llm.py`, `config.py` | Free AI providers, model lists and cache |
| `results/<setting>/<bug>/` | Everything for one run: the context, plan, every test version, final tests, mutants and `report.json` |
