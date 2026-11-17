Compared on the 40 bugs that every setting finished.

### Effectiveness

| Setting | Bugs detected (FDR) | Class line cov. (D4J) | Class branch cov. (D4J) | Major mutation | PIT mutation (class) | PIT mutation (method) | Method line cov. | Method branch cov. | Our mutants killed |
|---|---|---|---|---|---|---|---|---|---|
| full | 20/40 (50%) | 44% | 34% | 84% | 32% | 84% | 92% | 86% | 81% |
| evosuite | 13/40 (32%) | 73% | 67% | 64% | 61% | 62% | 66% | 59% | - |

### Test quality and cost

| Setting | 1st try: syntax ok | 1st try: compile | 1st try: run w/o error | 1st try: pass | 1st try: oracle correctness | Valid tests / bug | Assertions / test | False alarms | Bug-encoding tests | AI calls / bug | Tokens / bug | Seconds / bug |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| full | 100% | 71% | 58% | 54% | 97% | 17.7 | 1.8 | 0 | 0 | 8.7 | 44437.3 | 135.7 |
| evosuite | - | - | - | - | - | 37.9 | 1.5 | 0 | 0 | 0.0 | - | 255.4 |

