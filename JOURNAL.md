# Project Journal: what we did, what went wrong, what to try next

This file keeps track of **everything**. If our results turn out weak, this is where we look for **why** and **what to try next**.
Update it after every run.

---

## 1. Summary so far (17 Nov 2026)

- **Built:** a multi-agent framework that writes JUnit tests for Defects4J bugs. It has 8 agents: Context, Planner, Tester, Fixer, 3-AI panel, Curator, Mutant agent, Killer. Everything runs on free AI models.
- **Fresh start, 16 Nov:** old results were zipped (`old_results_backup.zip`), the junk was cleaned up, and the 40-bug development set was made.
- **Current result (all 40 dev bugs, 17 Nov):** ours **20/40 (50%)**, EvoSuite 13/40 (32%). Together 23/40 (58%).
- **Target to beat:** AdverTest's published **66.6%** and HITS's **61.4%**.

---

## 1b. Fairness rules (must follow)

- **The AI never sees the buggy code, the fix, or the developers' bug-revealing tests** in standard mode. It sees only the fixed code. The buggy vs. fixed comparison is used only to choose *which method* to test, as AdverTest and HITS do; say this in the thesis.
- **The 40 bugs in bugs/dev40.txt are the DEVELOPMENT set.** We studied their bugs, fixes and developer tests to find weaknesses, so they **can't** be used for the final claims.
- **Improvements must be general rules** (e.g. "always try extreme values"), never rules written for one specific bug.
- **Final results come from unseen bugs:** the 198-bug main set minus the 40 development bugs (158 bugs). EvoSuite, AdverTest and single-shot run on the same unseen bugs.

## 2. Timeline

| Date | What happened |
|---|---|
| early Nov | Plan written (plan.md, phases.md). Defects4J v3, Java 11, JaCoCo, PIT, EvoSuite installed. |
| ~10 Nov | First pilot on 50 bugs (Chart, Lang, Math): ours 31/50, single-shot 24/50, EvoSuite 20/50. These runs mixed several Gemini models. |
| ~10–15 Nov | Tried more free models: Mistral (no free API), Kaggle (needs phone verification), Groq (works, small limits). Settled on one main model, **gemini-3.5-flash-lite**, for fairness. |
| 15–16 Nov | Big study started (198 bugs and all settings). We found broken EvoSuite and PIT results and stopped it. |
| 16 Nov | Fixed 6 problems (section 3), did a fresh start, and started the 40-bug run. The quota ran out after 29 bugs. |
| 17 Nov | EvoSuite finished all 40 (13/40). Our last 12 bugs ran after the 13:30 IST quota reset: 4/12 detected. Math-12 failed once on a network drop and was re-run (missed). Final 20/40. About 110 Gemini calls used for these 12 bugs. 40-bug comparison made. |

---

## 3. Problems found and fixed

| # | Problem | Cause | Fix | File |
|---|---|---|---|---|
| 1 | EvoSuite failed on every bug in ~9 s and **overwrote good results** | Helper processes left behind after a crash blocked EvoSuite's connection | Clear leftover processes; a crash means "retry later"; never overwrite a good result | scripts/run_evosuite.py |
| 2 | EvoSuite failed again at night | Its two processes talk over the Wi-Fi address, which breaks when Wi-Fi sleeps | Force them to talk inside the Mac (127.0.0.1) | scripts/run_evosuite.py |
| 3 | PIT failed on all Cli bugs and some Closure bugs | The projects' old JUnit (3.8, or an old 4.x) came before JUnit 4.12 on the classpath | Put JUnit 4.12 first and drop JUnit 3 | our_framework/pit.py |
| 4 | Cli-7 skipped ("no method") | The fix changed only a **field** | Test the methods that use the changed field | our_framework/context.py |
| 5 | Math-12 skipped | The fix changed only the **class header** (`implements Serializable`) | Test the public methods, plus a note about what the header promises | our_framework/context.py |
| 6 | Closure-169 crashed | The fix **adds a whole new class**, which doesn't exist in the buggy version | Skip that class with a clear reason | our_framework/context.py |
| 7 | Tests never reached the buggy method (Cli-38: 0% coverage) | The AI was told about surviving mutants, but **not which lines no test runs** | **Coverage feedback:** each challenge round shows the uncovered lines. Cli-38 is now detected. | our_framework/pipeline.py, coverage.py |
| 8 | Major mutation looked "missing" for tiny classes | A 3-line class has nothing to mutate | Recorded as "0 mutants" | our_framework/standard_metrics.py |
| 8b | Buggy-method coverage was blank for constructors (Chart-12) | JaCoCo gives a constructor's first line as a field-initializer line, outside the constructor | Count focal coverage line by line inside the method range. Older reports with constructors can be re-measured from the saved tests. | our_framework/coverage.py |
| 10 | All tests of Chart-13 thrown away | The clean-up gave up when compile errors sat in a helper class, not directly in a test method | Remove wrong `@Override` labels automatically; when an error sits inside a test, delete the enclosing test, not the innermost method | our_framework/pipeline.py |
| 9 | Earlier: running EvoSuite at the same time as our framework crashed it (8 GB RAM) | Not enough memory | Run them one after the other, never together | scripts |

---

## 4. What is still missing (to do)

### Runs
- [x] Finish the last 12 bugs of the 40-bug set: done 17 Nov, 4/12 detected, 20/40 overall. **Note: Gemini's daily quota resets at midnight Pacific = 13:30 IST, not at our midnight.**
- [x] EvoSuite on the 40 bugs: done on 17 Nov, 13/40 detected, no crashes.
- [ ] **Single-shot AI** baseline on the 40.
- [ ] **Ablations** on the 40: no-context, no-planner, no-fix, no-mutation.
- [ ] **Realistic mode** on the 40, with and without the panel.
- [x] **AdverTest set up** (17 Nov):
  - its code is in `/Users/bitu/mtech-tools/advertest` and runs unchanged, except that it uses gemini-3.5-flash-lite instead of DeepSeek;
  - it has its own patched copy of Defects4J (`defects4j-advertest`), so ours is untouched;
  - it has a `timeout` replacement for macOS, and its own Python environment (`.venv`, old langchain).
  - Driver: `scripts/run_advertest.py`. It records AdverTest's own verdict **and** our standard verdict: Defects4J `fix_test_suite.pl`, then `run_bug_detection.pl`, the same as for EvoSuite, plus the same coverage and Major metrics.
  - ⚠️ AdverTest's own rule counts "tests don't compile on the buggy version" as **detected**, and it hides AI errors as "missed". That's why we also apply our own rule.
- [ ] **AdverTest** on the same 40 bugs. **Not started.** The setup is tested up to real AI calls (Lang-9: 4 calls OK, then stopped on request; partial result deleted). Start with `python3 scripts/run_advertest.py bugs/dev40.txt`, or `scripts/run_next.sh` to run our remaining bugs first, then AdverTest.
- [ ] Repeated runs (3×) for stability and statistics.
- [ ] Then scale up to the 198-bug main set.

### Measurements, after the runs (no AI needed)
- [ ] **EvoSuite extras** from its saved tests (`results/evosuite/<bug>/evosuite_tests.tar.bz2`): PIT score, buggy-method coverage (JaCoCo), number of tests and assertions. Run this after EvoSuite finishes, never at the same time (memory).
- [ ] Statistical tests (Wilcoxon, A12).
- [ ] Test smells (tsDetect) and readability.
- [ ] Similarity to the developers' tests (memorisation check).
- [ ] A hand check of some surviving mutants (are they really equivalent?).

### Optional
- [ ] Randoop baseline (built into Defects4J).
- [ ] Our framework on CANDOR's dataset (HumanEvalJava).
- [ ] Newer bugs the AI hasn't seen (GrowingBugs), as a contamination check.

---

## 5. If results are not good enough: ideas to improve

Ranked by **expected gain per effort**. Each idea comes with how we would test it.

| # | Idea | Why it might help | Source | How to test |
|---|---|---|---|---|
| 1 | **Missed-bug analysis first** | Find **why** each bug was missed before changing anything | — | For each missed bug, record: was the buggy method reached? was the buggy line run? did the assertion check the right thing? did the tests compile on the buggy version? |
| 2 | **Panel discussion instead of majority vote** | CANDOR showed that discussion beats voting for oracle correctness | CANDOR | Realistic mode, panel vote vs. discussion |
| 3 | **Mutants based on real bug patterns** | Fake bugs that look like real ones (off-by-one, missing null check, wrong boundary) train the tests better | Synthetic Bug Feedback (2607.11573) | full vs. full + pattern mutants |
| 4 | **Specification-first prompting in realistic mode** | Stops the AI from copying the bug | Misguidance paper (2607.22883) | Realistic mode: code shown vs. AI-written spec shown |
| 5 | **More challenge rounds, or more mutants on missed bugs** | Some bugs need one more push | AdverTest | mutation_rounds 2 → 4 on the missed bugs only |
| 6 | **Several attempts (sampling)** | Different tries find different bugs | pass@k idea | Run 3 times and count "detected in any run" |
| 7 | **Better context for deep code** | Bugs inside long call chains need more information | CAT, TestAgent | Add caller and callee snippets to the context sheet |
| 8 | **Combine with EvoSuite (hybrid)** | They catch different bugs (pilot: together 70%) | Hybrid SBST+LLM papers | Union of both suites |
| 9 | **Boundary and exception focus in the Planner** | Many Defects4J bugs are edge cases | Defects4J bug studies | Force at least N edge and exception scenarios |
| 10 | **Stronger model only for hard bugs** | flash-lite is small; a stronger free model may help on the misses | — | Re-run missed bugs with a stronger model and report it **separately** (fairness) |

---

## 6. Observations (things we noticed)

- **EvoSuite covers more code (91% vs. 70% lines) but catches fewer bugs.** Coverage alone doesn't find bugs; good assertions do.
- **Coverage feedback works:** Cli-38 went from missed (0% coverage) to detected.
- **The free quota is the real speed limit** (about 29 bugs per day), not the Mac.
- **Realistic mode is much harder:** the AI copies the bug, and all 3 AIs can agree on a wrong answer (Lang-1).
- **Missed with 100% coverage: "hidden-condition" bugs** (checked 17 Nov). The tests ran the buggy code but never used the special input that makes the bug appear.
  - **Lang-4:** the bug only shows when the input is a `StringBuilder` or `StringBuffer`, not a `String`. Our 18 tests used only Strings, with 100% line and branch coverage.
  - **Codec-1:** the bug only shows under a Turkish `Locale` (`toLowerCase()` without a locale). None of our 69 tests changed the Locale.
  - **Math-2:** integer overflow only with very large numbers (the developers' test uses 42 million × 50). Our biggest input was 10000.
  - **Codec-5:** the NullPointerException only happens when `decode` is used **through `Base64InputStream`**, which empties the buffer first. Our 18 tests called `decode()` directly.
  - **JxPath-3:** complex setup. The bug needs a badly implemented `AbstractFactory` plugged in with `setFactory()`. Our tests never set a factory, so only 64% of the buggy method's lines were reached.
  - **Chart-12:** weak check (oracle). The constructor doesn't register the plot as a dataset listener. Our 23 tests checked `getDataset()` (the same in both versions) but never `hasListener`. A missing side effect, not a wrong return value.
  - **Time-5:** special input type. The bug needs a `PeriodType` without years, e.g. `PeriodType.months()`. Our 13 tests used only `standard()`, `yearMonthDayTime()` and `time()`, with 100% coverage.
  - **Chart-13:** our tool's fault. All 11 tests were thrown away because 2 wrong `@Override` labels in a helper class never compiled, and the clean-up only looked at the innermost method. Fixed (problem 10 in section 3): 10 of 11 tests are now kept. They still don't catch the bug; the 11th test described exactly the bug's situation but had a wrong expected value.
  - **Fix ideas:**
    - (e) The context should show how other code and tests create helper objects (factories, mocks, custom subclasses), and the Tester may write small fake helper classes.
    - (f) The Tester must also check side effects: listeners or events registered, state of other objects, exceptions.
    - (c) The Planner must always include extreme values: Integer.MAX_VALUE, numbers whose product overflows, empty input, very long input.
    - (d) The context should also show **who calls the method**, e.g. Base64InputStream (CAT's call-chain idea), so the tests cover real usage paths.
    - (a) The Planner must plan for other input types (CharSequence: String, StringBuilder, StringBuffer) and for environment settings (Locale, TimeZone, charset).
    - (b) Add fake-bug rules such as "remove `Locale.ENGLISH`", "remove `.toString()`" and "swap equals/==" (bug-pattern mutants, idea 3 in section 5).
- **Some bugs can't be detected by design.** If the fix adds a whole new class, tests for it can't compile on the buggy code.

---

## 6b. Comparisons (graphs)

**Rule:** every comparison and every graph is made by `comparison/comparison.py`, and each run writes a **new folder** `comparison/result/<a>_vs_<b>_<N>bugs_<date>/`. A new set of frameworks, bug count or date gives a new folder.

`python3 comparison/comparison.py <framework> <framework> ... [--bugs bugs/dev40.txt]` (names = results folders: full, evosuite, advertest, single-shot, no-context, ...). It makes 11 graphs:
1. bugs detected;
2. key measures;
3. overlap;
4. per project;
5. per bug;
6. time and cost;
7. the AI's first answer;
8. the **fix loop** (every saved AI answer replayed on the fixed version, cached in `results/<fw>/attempt_quality.csv`, no AI);
9. **mutant challenge rounds** (AdverTest Fig. 2 style);
10. **coverage vs detection**;
11. **per-bug paired scores** (two frameworks only).

It also writes summary.md and per_bug.csv.

**17 Nov, our framework vs EvoSuite, 28 bugs** (`our-framework_vs_evosuite_28bugs_2026-11-17`):
- **Detected:** ours 16/28 (57%), EvoSuite 12/28 (43%).
- **Fix loop:** passing tests go from 63% (1st answer) to 82%, then 90%, then 96% after 3 fixes. Compiles: 78% → 100%.
- **Mutant challenge:** rule-mutant score goes from 65% at the start to 81% after round 1 and 82% after round 2. Buggy-method coverage goes from 88% to 94% to 96%.
- **Per bug, Major mutation:** ours higher on 19 of 22 bugs, EvoSuite higher on 2.
- **Coverage is not detection:** ours missed 4 bugs at 100% buggy-method coverage; EvoSuite missed 8.

**17 Nov, our framework vs EvoSuite, all 40 bugs** (`our-framework_vs_evosuite_40bugs_2026-11-17`):
- **Detected:** ours 20/40 (50%), EvoSuite 13/40 (32%). Only ours 10, only EvoSuite 3, both 10, neither 17.
- **Fix loop (49 test classes):** passing 53% → 72% → 79% → 84%; compiles 69% → 94%.
- **Mutant challenge:** rule-mutant score 67% → 80% → 81%; buggy-method coverage 84% → 90% → 91%.
- **Per bug, Major mutation:** ours higher on 21 of 26 bugs, EvoSuite higher on 2.
- **Coverage is not detection:** ours missed 6 bugs at 100% buggy-method coverage (Cli-7, Jsoup-18, Lang-4, Math-2, Math-12, Time-5); EvoSuite missed 9.
- **Observation:** the last 12 bugs (Jsoup, Closure, Jackson) were much harder (4/12). Closure-169 used 50 AI calls and still missed. Jsoup-18 finished in 41 s with 3 calls: check why the mutant step was so short.

**AdverTest run started 17 Nov 14:36 IST** (`results/advertest_run.log`).
- Lang-4: 26 AI calls and about 25 minutes (mostly mutant runs). Missed; AdverTest's own verdict also says missed. Class line coverage 100%, Major 71%.
- **Scoring bugs found and fixed (our driver, not AdverTest):**
  1. `fix_test_suite.pl` is in `framework/util`, not `bin`.
  2. The Defects4J scripts break on paths with spaces, so scoring now runs in `mtech-tools/advertest_out/`.
  3. AdverTest uses `package gentest.org...`, so the tests are repacked by their package line.
- **To do after the run:** `python3 scripts/run_advertest.py bugs/dev40.txt --rescore`. Bugs finished by the process started at 15:05 still used the old scoring; re-scoring uses the saved tests and makes no AI calls.
- **17 Nov, 17:40 IST:** Lang-9 detected (96 calls, 53 min). Lang-13 had used 172+ calls: Gemini's ``` code fences cause a compile-fix loop (196 "illegal character '`'" errors). The user chose to let it run until today's quota is used up, then add a fence-stripping step to our Gemini wrapper (AdverTest logic unchanged; a declared adaptation) and re-run every AdverTest bug so all are treated the same. Today's free quota is clearly above 220 calls (about 430 used so far).
- **17 Nov, 20:00 IST:** stopped by the user's choice. Lang-19 had used 29 calls; its partial result was deleted. Done today (before the fence fix): Lang-4 missed (26 calls), Lang-9 detected (96), Lang-13 missed (172). **Next (18 Nov, after 13:30 IST):** add fence stripping to `run_one.py`'s CountingGemini, delete `results/advertest/*`, re-run `scripts/run_advertest.py bugs/dev40.txt`. Keep today's three bugs' call counts as the "before the fix" numbers.
- **Why so many calls:** AdverTest repairs one test method per call, does 5 improve rounds with up to 3 retries each, and Gemini's ``` code fences cause extra compile errors.

**18 Nov, our framework vs EvoSuite vs AdverTest, 3 bugs** (`our-framework_vs_evosuite_vs_advertest_3bugs_2026-11-18`; Lang-4, Lang-9, Lang-13; AdverTest run before the fence fix):
- **Detected:** 1/3 each (all three catch Lang-9 only).
- **Major mutation (2 bugs):** ours 76%, EvoSuite 66%, AdverTest 77%. **PIT, class:** 42% / 74% / 61%. **PIT, buggy method (2 bugs):** 87% / 83% / 87%.
- **Cost:** AI calls per bug 9 / 0 / 98; seconds per bug 120 / 108 / 2847. AdverTest tests per bug: 51.
- AdverTest extras measured by the new `scripts/advertest_extras.py` (same method as evosuite_extras.py; on the tests Defects4J kept; macOS `._*` files removed). Lang-4's AdverTest time was corrected to about 1600 s from its Demonstration.log (the saved 7.4 s was only re-scoring).
- Too few bugs to draw conclusions; this mainly checks that the 3-way pipeline works.

## 6d. Non-LLM and ML baselines (setup, 19 Nov 2026)

The supervisor asked for comparisons with non-LLM and ML-based tools. Chosen: **Randoop (2007), EvoSuite (2011), CodeT5 (2021, A3Test's trained baseline), TOGA (2022), A3Test (2023)**. Candidates still open: AsserT5 (2025, newest), CAT-LM (2023, needs Colab). Left out with reasons: TeCo (test completion, a different task, no trained model), EditAS (same job as TOGA and AsserT5), AthenaTest (model never released; A3Test beats it). **Rule from the user: set up everything, but ask before running anything on bugs.**

| Tool | Script | Setup |
|---|---|---|
| Randoop 4.3.4 | `scripts/run_randoop.py` | Built into Defects4J; same 120 s budget as EvoSuite, seed 1. One trial on Lang-9 (detected, 83% line, Major 78%). |
| TOGA | `scripts/run_toga.py` + `scripts/toga_prepare.py` | Code in `mtech-tools/toga`, models from Zenodo 6210589 (unpacked), own venv (`toga/.venv`: torch, transformers 4.39, javalang, pandas<2.2). Uses the SAME EvoSuite tests as the EvoSuite baseline, splits them at each assertion, and TOGA predicts one oracle per prefix. One test per class (as in TOGA's own evaluation). Checked end to end on a toy example. |
| A3Test | `scripts/run_mltests.py --tool a3test` + `scripts/ml_testgen.py` | PLBART model from huggingface.co/SaranyaAakas/A3Test; tokenizer saved locally. Input = Methods2Test `src_fm_fc_ms_ff` for the SAME focal methods as our framework; greedy decoding; A3Test's post-processing rules. Model load-checked. |
| CodeT5 | `scripts/run_mltests.py --tool codet5` | CodeT5-small trained by the A3Test authors (same Hugging Face repo). Model load-checked. |

**Declared adaptations:** one generated test per class (so one compile error does not remove the others); `throws Exception` added to A3Test/CodeT5 tests when missing; JUnit + Hamcrest CoreMatchers imports added (the models write `assertThat(x, is(..))`). For every tool, `fix_test_suite.pl` removes tests that fail or do not compile on the fixed version (counted as false alarms), then `run_bug_detection.pl` + Defects4J coverage + Major.
**Setup problems fixed:** an expired Hugging Face login on this Mac (now `HF_HUB_DISABLE_IMPLICIT_TOKEN`, offline mode, local tokenizers); a partial CodeT5 download (resumed); new pandas breaking TOGA (pinned <2.2); TOGA's scripts call plain `python` (venv put first on PATH); TOGA needs `roberta-base` and `microsoft/codebert-base` cached.

**Randoop dev40 run (19–20 Nov 2026, about 3 h, 0 AI calls): 8/40 detected.**

- **Detected:** Lang-9, Csv-4, Gson-2, Chart-10, Chart-12, JacksonCore-2, Jsoup-7, Jsoup-18.
- **Tools at a glance:** our framework detects 20/40, EvoSuite 13/40, Randoop 8/40.
- **Caught by Randoop only (not by EvoSuite and not by ours):** Chart-12, Jsoup-7, Jsoup-18.
- **No test suite** from Randoop: Math-34, Closure-45, Closure-169, Mockito-2, Mockito-9. These count as missed.
- **Math-42:** ran out of Java memory on the first try. Rerun with `--retry-oom --xmx 3g` (script `scripts/run_randoop_then_oom_retry.sh`); no memory error the second time, but still missed. The first try's folder is kept in `mtech-tools/randoop_out/Math-42-oom-first-try`.
- **Not done yet:** focal-class JaCoCo coverage and PIT for Randoop, which need a `randoop_extras` script like `evosuite_extras`.

**TOGA dev40 run (started 20 Nov 2026).** Fixes made along the way:

- **Test counting (restarted twice).** `fix_test_suite.pl` does not delete a failing test. It replaces it with an empty dummy test marked `// Defects4J: flaky method`, and comments out a single failing assertion as `// Defects4J: flaky assertion`.
  - Kept tests now = `@Test` lines minus the dummies. Commented-out assertions are counted separately.
  - The fixed suite is saved as `toga_tests_kept.tar.bz2`.
  - The same fix is in `run_mltests.py`.
  - Detection results were not affected.
  - The earlier tries are kept in `mtech-tools/toga_out/*_try_*`.
- **TOGA crash on Cli-17.** That bug's tests have no assertion candidates, and TOGA's assertion step crashed on the empty input (`ValueError: zero-size array`).
  - Patched `toga.py` to skip the assertion model when there are no candidates, so no assertions are predicted, the same result TOGA would give.
  - Also guarded the debug print in `model/{assertions,exceptions}/utils.py`.
  - The originals are kept as `*.orig`. This is a declared fix; the model itself is unchanged.
  - Cli-17 is redone after the run by `scripts/run_toga_then_redo_cli17.sh`.
- **Cli-7:** no EvoSuite tests exist for it, so TOGA has nothing to work with and it counts as missed.
- **Codec-5 mutation took over 1 hour (20 Nov).**
  - Major's `run_mutation.pl` ran over its 1-hour limit, and the time-out crashed the whole run. It also left the ant/java processes running.
  - `standard_metrics._run` now runs each script in its own process group, kills the whole group after the 1-hour limit, and records `"timed_out": [...]`, so that measurement is simply missing.
  - EvoSuite's Codec-5 measurements are also empty.
  - The TOGA run was restarted with 28 bugs to go, Cli-17 included.

**TOGA dev40 result (finished 21 Nov 2026, 23:5x IST): 5/40 detected** — Lang-19, Csv-4, JxPath-1, Time-1, JacksonCore-8.
- All 5 are also detected by EvoSuite (13/40). TOGA reuses EvoSuite's test prefixes and replaces its oracles, so on dev40 it only lost bugs (EvoSuite caught 8 that TOGA missed); it caught none that EvoSuite missed.
- No usable tests (3): Cli-7 (no EvoSuite tests); JacksonDatabind-24 (1 prefix, 0 with a focal method) and Mockito-9 (0 prefixes) — TOGA's own code crashes on empty input (`ZeroDivisionError`, no `exception_preds.csv`). Counted as missed; TOGA's real limitation, not patched.
- Mutation timed out (1 h) on Codec-5 and Closure-169, so those mutation scores are missing.
- Times unreliable (laptop slept): Codec-5, Csv-1, Csv-4, Gson-2, Gson-6, JxPath-1 — do not use them in time comparisons.
- Many tests removed as false alarms on the fixed version (e.g. JacksonCore-2: 650 of 2078; Closure-169: 222 of 787).
- dev40 so far: our framework 20, EvoSuite 13, Randoop 8, TOGA 5.

**A3Test dev40 run (started 22 Nov 2026, ~00:00 IST).**
- Lang-9: the model wrote junk (`Assert.assertEquals( ( ( ( ...`) for the long private `init`, so no test. Model limitation, counted as missed.
- **Math-12 hung ~2 h (awake time; the laptop also slept most of the night).** The model wrote `void test BitsStreamGenerator()` (space in the name). That can never compile, and Defects4J's `rm_broken_tests.pl` cannot match it, so `fix_test_suite.pl` looped forever.
  - A second broken test (`i *= *= *=`) looped the same way. Cause: the models write each test on ONE line, and `rm_broken_tests.pl` cannot cut a broken one-line method out.
  - Fix in `run_mltests.py`: each test is its own class, so each one is compiled alone first (javac 11, the fixed version's `cp.test`, same as our framework). Tests that do not compile are dropped and counted as removed on the fixed version (`precompile_dropped`) — exactly what `fix_test_suite.pl` would do if it could.
  - `sh()` now runs each command in its own process group and kills the whole tree on time-out; `fix_test_suite.pl` has a 1 h limit (recorded as an error if hit).
  - Math-12 after the fix: 5 of 5 tests do not compile, missed (15.7 s).
- **Harness bug found (22 Nov):** `run_mltests.py` split `classes.modified` on `;`, but Defects4J prints one class per line. 5 bugs with several modified classes (Codec-1, JxPath-1, Time-1, JacksonCore-2, Closure-169) got no input ("no focal method"). Fixed (split on `;` or newline) and those 5 redone; first tries kept in `mtech-tools/a3test_out/_split_bug_first_try`. CodeT5 uses the same script, so it is fixed too. Same bug fixed in `advertest_extras.py`; it did not affect AdverTest so far (only Lang-4/9/13 run, one class each).

**A3Test dev40 result (finished 22 Nov 2026, 16:44 IST): 0/40 detected.**
- 81 focal methods → 68 tests after A3Test's post-processing → **6 compile and pass on the fixed version** (Lang-4, Lang-13, Math-2, Chart-13); all 6 also pass on the buggy version.
- 58 tests do not compile alone (checked by hand on a sample: invented fields like `this.parser`, AssertJ/Mockito style calls the project lacks, `'foo'` char literals); 4 more removed by `fix_test_suite.pl`. 3 bugs: the model wrote junk with no `void` method (e.g. Lang-9 `Assert.assertEquals( ( ( ( ...`).
- Matches the A3Test paper's setting: trained on single methods (Methods2Test), it has no project context, so project-level tests mostly do not compile.
- dev40 so far: our framework 20, EvoSuite 13, Randoop 8, TOGA 5, A3Test 0.

**CodeT5 dev40 result (finished 22 Nov 2026, same script and fixes as A3Test): 0/40 detected.**
- 81 focal methods → 59 tests after post-processing → **3 compile and pass on the fixed version** (Lang-23, Cli-7, JacksonCore-2); all 3 also pass on the buggy version.
- 54 do not compile alone, 2 more removed by `fix_test_suite.pl`; 9 bugs: no usable test method in the model output.
- dev40: our framework 20, EvoSuite 13, Randoop 8, TOGA 5, A3Test 0, CodeT5 0.
  - Restarted on 22 Nov ~13:00 IST with 34 bugs to go (the 6 finished bugs are unaffected).

## 6c. GitHub

Public repository: https://github.com/BitanuCS/test-case-generation-mtech-research (first push on 17 Nov 2026). Not uploaded: `.env` (API keys), `cache/`, `Research/` PDFs, `old_results_backup.zip` and logs (see `.gitignore`). To update it: `git add -A && git commit -m "..." && git push`.

## 7. How to resume

```bash
scripts/run_dev40.sh                                  # our framework + EvoSuite on the 40 bugs (skips finished)
python3 scripts/run_evosuite.py bugs/dev40.txt        # EvoSuite only
python3 scripts/summarize.py bugs/dev40.txt           # tables
tail -20 results/dev40.log                            # progress
```
