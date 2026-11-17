Compared on the 28 bugs that every setting finished.

### Effectiveness

| Setting | Bugs detected (FDR) | Class line cov. (D4J) | Class branch cov. (D4J) | Major mutation | PIT mutation (class) | PIT mutation (method) | Method line cov. | Method branch cov. | Our mutants killed |
|---|---|---|---|---|---|---|---|---|---|
| full | 16/28 (57%) | 45% | 36% | 82% | 34% | 86% | 96% | 89% | 82% |
| evosuite | 12/28 (43%) | 79% | 74% | 59% | 68% | 74% | 80% | 72% | - |

### Test quality and cost

| Setting | 1st try: syntax ok | 1st try: compile | 1st try: run w/o error | 1st try: pass | 1st try: oracle correctness | Valid tests / bug | Assertions / test | False alarms | Bug-encoding tests | AI calls / bug | Tokens / bug | Seconds / bug |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| full | 100% | 77% | 68% | 61% | 96% | 19.3 | 1.9 | 0 | 0 | 8.4 | 42348.5 | 122.2 |
| evosuite | - | - | - | - | - | 35.6 | 1.7 | 0 | 0 | 0.0 | - | 240.4 |

