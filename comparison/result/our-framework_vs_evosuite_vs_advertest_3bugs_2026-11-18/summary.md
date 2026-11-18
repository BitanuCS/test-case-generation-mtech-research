# Our framework vs EvoSuite vs AdverTest: 3 bugs

Made 2026-11-18 by `comparison/comparison.py full evosuite advertest --bugs bugs/dev40.txt`.

Same 3 Defects4J bugs for every framework; same Defects4J scripts judge all of them.

| Measure | Our framework | EvoSuite | AdverTest |
|---|---|---|---|
| Bugs detected | 1/3 (33%) | 1/3 (33%) | 1/3 (33%) |
| Major mutation score (on 2 bugs) | 76% | 66% | 77% |
| PIT mutation, class | 42% | 74% | 61% |
| PIT mutation, buggy method (on 2 bugs) | 87% | 83% | 87% |
| Line coverage, whole class (on 2 bugs) | 89% | 90% | 85% |
| Branch coverage, whole class (on 2 bugs) | 82% | 90% | 64% |
| Line coverage, buggy method (on 2 bugs) | 100% | 96% | 100% |
| Branch coverage, buggy method (on 2 bugs) | 95% | 100% | 84% |
| 1st try: Syntax OK | 100% | - | - |
| 1st try: Compiles | 69% | - | - |
| 1st try: Runs without error | 67% | - | - |
| 1st try: Passes | 61% | - | - |
| 1st try: Oracle correctness | 96% | - | - |
| Valid tests per bug | 16.7 | 18.0 | 51.0 |
| Assertions per test | 1.9 | 0.9 | 2.0 |
| AI calls per bug | 9.0 | 0.0 | 98.0 |
| Tokens per bug | 45514.0 | - | 208147.0 |
| Seconds per bug | 119.7 | 108.3 | 2847.0 |

## Fix loop: Our framework (3 test classes)

Each class's suite after up to k Fixer answers (last answer carried forward). Every saved AI answer was re-run on the fixed version; no AI calls.

| Step | Needed this step | Syntax OK | Compiles | Runs without error | Passes | Oracle correctness |
|---|---|---|---|---|---|---|
| 1st answer | 3/3 | 100% | 69% | 67% | 61% | 96% |
| after 1 fix | 3/3 | 100% | 100% | 77% | 74% | 98% |
| after 2 fixes | 2/3 | 100% | 100% | 97% | 94% | 98% |
| after 3 fixes | 2/3 | 100% | 100% | 100% | 97% | 98% |

## Mutant challenge rounds: Our framework

| Round | Mutation score (rule mutants) | Mutation score (all, incl. AI mutants) | Buggy-method line coverage |
|---|---|---|---|
| start | 57% | 47% | 100% |
| 1 | 57% | 47% | 100% |
| 2 | 57% | 47% | 100% |

## Coverage is not detection

- **Our framework:** 1 bugs missed although the buggy method had 100% line coverage: Lang-4
- **EvoSuite:** 0 bugs missed although the buggy method had 100% line coverage: -
- **AdverTest:** 1 bugs missed although the buggy method had 100% line coverage: Lang-4

## Which bugs

- **Caught by at least one (1):** Lang-9
- **Caught by none (2):** Lang-13, Lang-4
- **Only Our framework (0):** -
- **Only EvoSuite (0):** -
- **Only AdverTest (0):** -

## The 3 bugs compared

Lang-4, Lang-9, Lang-13
