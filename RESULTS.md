# Results

## Current run: 40-bug development set (fresh start, 16 Nov 2026)

- **Bugs:** [bugs/dev40.txt](bugs/dev40.txt), 40 bugs from all 17 Defects4J projects.
- **Model:** Gemini 3.5 flash-lite for every main job. The panel is flash-lite plus Groq gpt-oss-120b and Qwen3.8-27b. Free tiers only.
- **Code version:** after the fixes of 16 Nov (see [JOURNAL.md](JOURNAL.md)): EvoSuite crash, PIT on Cli/Closure, field-only and header-only fixes, new classes, and coverage feedback.

### Our framework (standard mode): all 40 done (17 Nov 2026)

| | Bugs |
|---|---|
| **Detected (20)** | Lang-9, Lang-19, Lang-23, Math-34, Cli-17, Cli-38, Csv-1, Csv-4, Gson-2, Gson-6, JxPath-1, Chart-10, Time-1, Compress-12, JacksonCore-2, JacksonCore-8, Collections-1, JacksonDatabind-24, Mockito-2, Mockito-9 |
| **Missed (20)** | Lang-4, Lang-13, Math-2, Math-12, Math-42, Cli-7, Codec-1, Codec-5, JxPath-3, Chart-12, Chart-13, Time-5, Compress-5, Jsoup-7, Jsoup-18, JacksonXml-1, Closure-45, Closure-58, Closure-169, JacksonDatabind-8 |

**20 / 40 = 50% detected.** The last 12 bugs were harder: only 4 of them were detected. Closure-169 alone took 12 minutes and 50 AI calls.

- **Cli-38** was missed before the coverage-feedback fix (0% coverage of the buggy method). It is **now detected**.
- First try (averages): 100% syntax OK, 71% compile, 54% pass, 97% oracle correctness.
- Fix loop: pass rate 53% → 72% → 79% → 84% after 1, 2 and 3 Fixer answers; compile 69% → 94%.
- About 9 AI calls and 44k tokens per bug, and about 2.3 minutes per bug.

### Our framework vs EvoSuite on all 40 bugs

Graphs and tables: [comparison/result/our-framework_vs_evosuite_40bugs_2026-11-17/](comparison/result/our-framework_vs_evosuite_40bugs_2026-11-17/). "On N bugs" = averages only over bugs where both tools have the value.

| | Ours | EvoSuite |
|---|---|---|
| **Bugs detected** | **20/40 (50%)** | 13/40 (32%) |
| Major mutation score (on 26 bugs) | **86%** | 66% |
| PIT mutation, buggy method (on 30 bugs) | **85%** | 70% |
| PIT mutation, whole class (on 31 bugs) | 33% | **65%** |
| Line coverage, buggy method (on 33 bugs) | **92%** | 69% |
| Line coverage, whole class (on 26 bugs) | 45% | **72%** |
| Seconds per bug | **136** | 255 |

- **Only ours (10):** Chart-10, Cli-17, Cli-38, Collections-1, Compress-12, Csv-1, Gson-6, JacksonDatabind-24, Math-34, Mockito-9.
- **Only EvoSuite (3):** JxPath-3, Math-42, Time-5.
- **Both (10)**; **neither (17)**.
- **Together: 23/40 (58%).** The two tools find different bugs.
- **Per bug, Major mutation:** ours higher on 21 of 26 bugs, EvoSuite higher on 2.
- **Our class coverage is lower** because we only test the buggy method(s), while EvoSuite tests the whole class.
- **Coverage is not detection:** we missed 6 bugs even though our tests ran every line of the buggy method (Cli-7, Jsoup-18, Lang-4, Math-2, Math-12, Time-5). The weak part there is the checks (oracles), not reaching the code.

### Still to run on the 40 bugs
- single-shot AI: the simple baseline;
- ablations: no-context, no-planner, no-fix, no-mutation;
- realistic mode, with and without the panel;
- AdverTest on the same bugs;
- repeated runs (full-repeat2, full-repeat3).

## Older results (before the 16 Nov fixes; for reference only)

These are stored in `old_results_backup.zip`. They mixed Gemini models, and the EvoSuite part was partly broken, so **don't use them in the thesis**.

- **Pilot, 50 bugs from Chart, Lang and Math:** ours 31/50 (62%), single-shot 24/50 (48%), EvoSuite 20/50 (40%).
- **Realistic mode on Lang-1:** the AI copied the bug into a test, and the panel unanimously trusted a false alarm. This is the "misguidance" problem, which arXiv 2607.22883 also reports.

## Related work and their published numbers

| Framework | Benchmark | Their result | Comparable to ours? |
|---|---|---|---|
| **AdverTest** | Defects4J (50 bugs, 129 methods), GrowingBugs | **66.6%** bugs detected (DeepSeek V3.2) | yes; we will also run it on our bugs |
| **HITS** | Defects4J | 61.4% detected | different bugs and model |
| **EvoSuite** (in the AdverTest paper) | Defects4J | 40.8% detected | yes, and we run it ourselves |
| **BenjiTest** | Defects4J | 46.1% detected (zero-shot 10.5%) | different setup |
| **TestART** | Defects4J | 78.6% pass rate | no bug-detection number |
| **TestAgent** | 1,454 methods (5 Defects4J projects) | 92% line coverage, 84% mutation score | no real-bug detection |
| **CAT** | Defects4J | +18% line and +22% branch coverage over PANTA | coverage only |
| **CANDOR** | HumanEvalJava, LeetCodeJava | high oracle correctness; beats TOGLL by more than 21 points | different dataset (small single methods) |
| **Synthetic Bug Feedback** | Defects4J, method-level | beats coverage- and mutation-based baselines | no numbers found yet |
| **AssertMate** | Defects4J | more bugs detected than ChatAssert | assertions only |

Full list and notes: [plan.md](plan.md), section 8.

## Known limits
- **Free Gemini quota:** about 220 calls per day in practice, which is about 29 bugs per day.
- **The Mac must stay awake:** plugged in, lid open.
- **Bugs whose fix adds a whole new class** (part of Closure-169) can't be detected with tests written on the fixed version. That class is skipped, with the reason recorded.
