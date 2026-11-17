# Our framework vs EvoSuite: 28 bugs

Made 2026-11-17 by `comparison/comparison.py full evosuite --bugs bugs/dev40.txt`.

Same 28 Defects4J bugs for every framework; same Defects4J scripts judge all of them.

| Measure | Our framework | EvoSuite |
|---|---|---|
| Bugs detected | 16/28 (57%) | 12/28 (43%) |
| Major mutation score (on 22 bugs) | 84% | 61% |
| PIT mutation, class (on 25 bugs) | 34% | 68% |
| PIT mutation, buggy method (on 24 bugs) | 88% | 75% |
| Line coverage, whole class (on 22 bugs) | 45% | 78% |
| Branch coverage, whole class (on 21 bugs) | 34% | 73% |
| Line coverage, buggy method (on 24 bugs) | 96% | 79% |
| Branch coverage, buggy method (on 20 bugs) | 89% | 74% |
| 1st try: Syntax OK | 100% | - |
| 1st try: Compiles | 77% | - |
| 1st try: Runs without error | 68% | - |
| 1st try: Passes | 61% | - |
| 1st try: Oracle correctness | 96% | - |
| Valid tests per bug | 19.3 | 35.6 |
| Assertions per test | 1.9 | 1.7 |
| AI calls per bug | 8.4 | 0.0 |
| Tokens per bug | 42348.5 | - |
| Seconds per bug | 122.2 | 240.4 |

## Fix loop: Our framework (33 test classes)

Each class's suite after up to k Fixer answers (last answer carried forward). Every saved AI answer was re-run on the fixed version; no AI calls.

| Step | Needed this step | Syntax OK | Compiles | Runs without error | Passes | Oracle correctness |
|---|---|---|---|---|---|---|
| 1st answer | 33/33 | 100% | 78% | 70% | 63% | 96% |
| after 1 fix | 24/33 | 100% | 93% | 84% | 82% | 99% |
| after 2 fixes | 12/33 | 100% | 93% | 90% | 90% | 100% |
| after 3 fixes | 9/33 | 100% | 100% | 96% | 96% | 100% |

## Mutant challenge rounds: Our framework

| Round | Mutation score (rule mutants) | Mutation score (all, incl. AI mutants) | Buggy-method line coverage |
|---|---|---|---|
| start | 65% | 63% | 88% |
| 1 | 81% | 76% | 94% |
| 2 | 82% | 78% | 96% |

## Coverage is not detection

- **Our framework:** 4 bugs missed although the buggy method had 100% line coverage: Cli-7, Lang-4, Math-2, Time-5
- **EvoSuite:** 8 bugs missed although the buggy method had 100% line coverage: Chart-10, Chart-12, Chart-13, Cli-17, Cli-38, Codec-5, Math-2, Math-34

## Per bug: Our framework vs EvoSuite

| Measure | Bugs | Our framework higher | equal | EvoSuite higher |
|---|---|---|---|---|
| Major mutation score | 22 | 19 | 1 | 2 |
| PIT score, buggy method | 24 | 13 | 8 | 3 |
| Line coverage, buggy method | 24 | 8 | 13 | 3 |

## Which bugs

- **Only our framework (7):** Chart-10, Cli-17, Cli-38, Compress-12, Csv-1, Gson-6, Math-34
- **Both (9):** Csv-4, Gson-2, JacksonCore-2, JacksonCore-8, JxPath-1, Lang-19, Lang-23, Lang-9, Time-1
- **Only EvoSuite (3):** JxPath-3, Math-42, Time-5
- **Neither (9):** Chart-12, Chart-13, Cli-7, Codec-1, Codec-5, Compress-5, Lang-13, Lang-4, Math-2

## The 28 bugs compared

Chart-10, Chart-12, Chart-13, Cli-7, Cli-17, Cli-38, Codec-1, Codec-5, Compress-5, Compress-12, Csv-1, Csv-4, Gson-2, Gson-6, JacksonCore-2, JacksonCore-8, JxPath-1, JxPath-3, Lang-4, Lang-9, Lang-13, Lang-19, Lang-23, Math-2, Math-34, Math-42, Time-1, Time-5
