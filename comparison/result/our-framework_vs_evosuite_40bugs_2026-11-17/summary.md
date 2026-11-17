# Our framework vs EvoSuite: 40 bugs

Made 2026-11-17 by `comparison/comparison.py full evosuite --bugs bugs/dev40.txt`.

Same 40 Defects4J bugs for every framework; same Defects4J scripts judge all of them.

| Measure | Our framework | EvoSuite |
|---|---|---|
| Bugs detected | 20/40 (50%) | 13/40 (32%) |
| Major mutation score (on 26 bugs) | 86% | 66% |
| PIT mutation, class (on 31 bugs) | 33% | 65% |
| PIT mutation, buggy method (on 30 bugs) | 85% | 70% |
| Line coverage, whole class (on 26 bugs) | 45% | 72% |
| Branch coverage, whole class (on 25 bugs) | 34% | 66% |
| Line coverage, buggy method (on 33 bugs) | 92% | 69% |
| Branch coverage, buggy method (on 28 bugs) | 85% | 65% |
| 1st try: Syntax OK | 100% | - |
| 1st try: Compiles | 71% | - |
| 1st try: Runs without error | 58% | - |
| 1st try: Passes | 54% | - |
| 1st try: Oracle correctness | 97% | - |
| Valid tests per bug | 17.7 | 37.9 |
| Assertions per test | 1.8 | 1.5 |
| AI calls per bug | 8.7 | 0.0 |
| Tokens per bug | 44437.3 | - |
| Seconds per bug | 135.7 | 255.4 |

## Fix loop: Our framework (49 test classes)

Each class's suite after up to k Fixer answers (last answer carried forward). Every saved AI answer was re-run on the fixed version; no AI calls.

| Step | Needed this step | Syntax OK | Compiles | Runs without error | Passes | Oracle correctness |
|---|---|---|---|---|---|---|
| 1st answer | 49/49 | 100% | 69% | 57% | 53% | 97% |
| after 1 fix | 35/49 | 100% | 87% | 74% | 72% | 99% |
| after 2 fixes | 20/49 | 100% | 90% | 80% | 79% | 99% |
| after 3 fixes | 17/49 | 100% | 94% | 85% | 84% | 100% |

## Mutant challenge rounds: Our framework

| Round | Mutation score (rule mutants) | Mutation score (all, incl. AI mutants) | Buggy-method line coverage |
|---|---|---|---|
| start | 67% | 65% | 84% |
| 1 | 80% | 77% | 90% |
| 2 | 81% | 78% | 91% |

## Coverage is not detection

- **Our framework:** 6 bugs missed although the buggy method had 100% line coverage: Cli-7, Jsoup-18, Lang-4, Math-2, Math-12, Time-5
- **EvoSuite:** 9 bugs missed although the buggy method had 100% line coverage: Chart-10, Chart-12, Chart-13, Cli-17, Cli-38, Codec-5, Math-2, Math-12, Math-34

## Per bug: Our framework vs EvoSuite

| Measure | Bugs | Our framework higher | equal | EvoSuite higher |
|---|---|---|---|---|
| Major mutation score | 26 | 21 | 3 | 2 |
| PIT score, buggy method | 30 | 15 | 10 | 5 |
| Line coverage, buggy method | 33 | 13 | 17 | 3 |

## Which bugs

- **Only our framework (10):** Chart-10, Cli-17, Cli-38, Collections-1, Compress-12, Csv-1, Gson-6, JacksonDatabind-24, Math-34, Mockito-9
- **Both (10):** Csv-4, Gson-2, JacksonCore-2, JacksonCore-8, JxPath-1, Lang-19, Lang-23, Lang-9, Mockito-2, Time-1
- **Only EvoSuite (3):** JxPath-3, Math-42, Time-5
- **Neither (17):** Chart-12, Chart-13, Cli-7, Closure-169, Closure-45, Closure-58, Codec-1, Codec-5, Compress-5, JacksonDatabind-8, JacksonXml-1, Jsoup-18, Jsoup-7, Lang-13, Lang-4, Math-12, Math-2

## The 40 bugs compared

Chart-10, Chart-12, Chart-13, Cli-7, Cli-17, Cli-38, Closure-45, Closure-58, Closure-169, Codec-1, Codec-5, Collections-1, Compress-5, Compress-12, Csv-1, Csv-4, Gson-2, Gson-6, JacksonCore-2, JacksonCore-8, JacksonDatabind-8, JacksonDatabind-24, JacksonXml-1, Jsoup-7, Jsoup-18, JxPath-1, JxPath-3, Lang-4, Lang-9, Lang-13, Lang-19, Lang-23, Math-2, Math-12, Math-34, Math-42, Mockito-2, Mockito-9, Time-1, Time-5
