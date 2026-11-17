# Project Phases

## Status (10 Nov 2026)

| Phase | Status |
|---|---|
| 1. Setup | ✅ Done |
| 2. Understand, Write, Fix | ✅ Done |
| 3. AIs vote on answers (realistic mode) | ✅ Built; full pilot run queued |
| 4. Fake-bug challenge | ✅ Done (mutation score about 83%) |
| 5. Pilot on 50 bugs | ✅ Full framework 31/50 (62%), EvoSuite 20/50 (40%). Remove-one-step runs in progress. |
| 6. Full run (198 bugs) and comparison | ⏳ EvoSuite running; our framework next |

See [RESULTS.md](RESULTS.md) for the numbers.

**Goal:** build a multi-agent AI framework that writes tests able to catch real bugs in Defects4J, and compare it with other frameworks.

**Total time:** about 10–12 weeks. A first working version is ready in about 3–4 weeks.

**Budget:** ₹0. Everything uses free tools and free AI services.

---

## Our setup (free)

| Thing | What we use | Cost |
|---|---|---|
| Computer | Your Mac: M1, 8 GB RAM, about 15 GB free space | Free |
| Bug dataset | Defects4J, installed directly on the Mac without Docker to save memory | Free |
| Main AI model | **Google Gemini Flash** (free tier of the Gemini API) | Free |
| Extra AI models, for voting | **Groq** free tier (Llama/Qwen models) and **OpenRouter** free models (e.g. DeepSeek) | Free |
| Coding language | Python for the framework; the tests themselves are Java | Free |
| Testing tools | JUnit (to run tests), PIT (to create fake bugs), JaCoCo (to measure how much code is tested) | Free |

**Limits of free AI:**
- Free services allow only a limited number of requests per day.
- So we **save every AI answer** and never ask the same question twice.
- We also use a **smaller set of bugs** (about 200 instead of 800).

**Don't run the AI on your own Mac.** Running models locally needs more than 8 GB RAM.

**Ask your university:** if DTU has a GPU server or lab machine, it would make the full runs much easier.

---

## Phase 1: Setup (Week 1–2)

**Goal:** get Defects4J working, and get one AI answer back.

- Free up disk space; we need about 10 GB.
- Install Java 11, Perl and Defects4J.
- Load one bug (e.g. Lang bug 1), compile it, and run its tests.
- Create free accounts on Gemini, Groq and OpenRouter, and get API keys.
- Write a small script that sends one question to the AI and saves the answer.

**Done when:** we can load a bug, run its tests, and talk to the AI from Python.

---

## Phase 2: Basic pipeline: Understand, then Write, then Fix (Week 2–4)

**Goal:** the AI writes tests that actually compile and run.

- **Understand agent:** collects the important information about the code to be tested.
- **Planner agent:** decides which situations to test: normal, edge and error cases.
- **Tester agent:** writes the test code.
- **Fixer agent:** runs the test, shows any error to the AI, and lets it repair the test (up to 3 tries).
- Try it on a few bugs from the Lang project.

**Done when:** most AI-written tests compile and run on a few bugs.

**First working version is ready here.**

---

## Phase 3: Agreeing on the correct answers (Week 5–6)

**Goal:** make the tests check for the *right* results, not the buggy results.

- 2–3 different AI models each say what the correct output should be.
- They decide based on the **description** of the code (comments and documentation) rather than the buggy code itself.
- A **Curator agent** collects their answers and takes a vote.
- If they disagree, the answer is marked "unsure".

**Done when:** tests start catching real bugs, not just running.

---

## Phase 4: Fake-bug challenge (Week 6–7)

**Goal:** make the tests stronger.

- A **Mutant agent** adds small fake bugs into the code.
- The PIT tool adds more fake bugs automatically.
- If our tests miss a fake bug, it goes back to the Tester to write better tests.
- Repeat 2–3 rounds, or stop early when there is no more improvement.

**Done when:** tests catch more fake bugs after each round.

---

## Phase 5: Pilot run (Week 8)

**Goal:** test the full framework on a small set before the big run.

- Run on about 50 bugs from the Lang, Math and Chart projects.
- Find what breaks, then fix prompts and code.
- Check how many free AI requests one bug uses, so we can plan the full run.

**Done when:** the framework runs on 50 bugs without crashing, and we know the bug-catching rate.

---

## Phase 6: Full run and comparison (Week 9–12)

**Goal:** get final results and compare with other frameworks.

- Run on about 200 bugs, the same kind of subset AdverTest used.
- Run, or take the reported results of, other tools: **EvoSuite**, **AdverTest**, **HITS** and **TestART**.
- **Remove one step at a time** to show how much each step helps:
  - without the Understand agent;
  - without the Fixer;
  - with only 1 AI model instead of voting;
  - without the fake-bug challenge.
- Make tables and charts of the results.

**Done when:** we have final numbers showing how our framework compares with the others.

---

## Summary

| Phase | What | Weeks |
|---|---|---|
| 1 | Setup | 1–2 |
| 2 | Understand, Write, Fix | 2–4 |
| 3 | AIs vote on answers | 5–6 |
| 4 | Fake-bug challenge | 6–7 |
| 5 | Pilot on 50 bugs | 8 |
| 6 | Full run and comparison | 9–12 |
