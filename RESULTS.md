# Results

## Current run: 40-bug development set (fresh start, 16 Nov 2026)

- **Bugs:** [bugs/dev40.txt](bugs/dev40.txt), 40 bugs from all 17 Defects4J projects.
- **Model:** Gemini 3.5 flash-lite for every main job. The panel is flash-lite plus Groq gpt-oss-120b and Qwen3.8-27b. Free tiers only.
- **Code version:** after the fixes of 16 Nov (see [JOURNAL.md](JOURNAL.md)): EvoSuite crash, PIT on Cli/Closure, field-only and header-only fixes, new classes, and coverage feedback.

### Our framework (standard mode): 28 of 40 done, so far

| | Bugs |
|---|---|
| **Detected (16)** | Lang-9, Lang-19, Lang-23, Math-34, Cli-17, Cli-38, Csv-1, Csv-4, Gson-2, Gson-6, JxPath-1, Chart-10, Time-1, Compress-12, JacksonCore-2, JacksonCore-8 |
| **Missed (12)** | Lang-4, Lang-13, Math-2, Math-42, Cli-7, Codec-1, Codec-5, JxPath-3, Chart-12, Chart-13, Time-5, Compress-5 |
| **Still to run (12)** | Math-12 (re-run after the fix), Jsoup-7, Jsoup-18, Collections-1, JacksonXml-1, Closure-45, Closure-58, Closure-169, JacksonDatabind-8, JacksonDatabind-24, Mockito-2, Mockito-9 |

**So far: 16 / 28 = 57% detected.** The free Gemini quota ran out after 29 bugs; the rest run on the next day.

- **Cli-38** was missed before the coverage-feedback fix (0% coverage of the buggy method). It is **now detected**.
- First try (averages): 100% syntax OK, 79% compile, 66% pass, 94% oracle correctness.
- About 7 AI calls and 30k tokens per bug, and about 2 minutes per bug.

### EvoSuite on the same bugs: finished (all 40, no crashes)

**EvoSuite on all 40: 13/40 (33%).** On the 28 bugs both tools have finished:

| | Ours | EvoSuite |
|---|---|---|
| **Bugs detected** | **16/28 (57%)** | 12/28 (43%) |
| Class line coverage (Defects4J) | 45% | **79%** |
| Class branch coverage | 36% | **74%** |
| Major mutation score | **82%** | 59% |
| Buggy-method line and branch coverage (JaCoCo) | 96% / 89% | not measured yet |
| Seconds per bug | 122 | 240 |

- **Only ours:** Math-34, Cli-17, Cli-38, Csv-1, Gson-6, Chart-10, Compress-12.
- **Only EvoSuite:** Math-42, JxPath-3, Time-5.
- **Both missed:** Lang-4, Lang-13, Math-2, Cli-7, Codec-1, Codec-5, Chart-12, Chart-13, Compress-5.
- **Together: 19/28 (68%).** The two tools find different bugs.
- **Our class coverage is lower** because we only test the buggy method(s), while EvoSuite tests the whole class. On the buggy method itself we reach 96% of lines.

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
