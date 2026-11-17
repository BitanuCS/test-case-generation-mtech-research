# What we measure, and why (from the Google Sheet and the papers in `Research/`)

Every measurement below is taken **during the main run**, so nothing has to be run again. Measurements that need no AI can also be recomputed later from the saved test files.

## Measured for every bug, during the run

| Measurement | Used by (papers) | How we measure it | Where it is saved |
|---|---|---|---|
| **Fault detection rate (FDR)**: tests pass on the fixed code and fail on the buggy code | AdverTest (main metric), HITS, EvoSuite studies | We run the final tests on both versions | `report.json → detected` |
| **Line and branch coverage of the class** | AdverTest (Cobertura), CAT, HITS, TestAgent | Defects4J `run_coverage.pl` (Cobertura, the same as AdverTest) | `standard_metrics.coverage` |
| **Line and branch coverage of the buggy method(s)** | CANDOR, TestAgent, MutGen (JaCoCo) | JaCoCo | `classes[].coverage.focal` |
| **Mutation score, PIT** (whole class and buggy method) | CANDOR, TestAgent, MutGen | PIT 1.16.1 with default mutators | `classes[].pit` |
| **Mutation score, Major** | Defects4J standard; for comparing with EvoSuite | Defects4J `run_mutation.pl` | `standard_metrics.major_mutation` |
| Mutation score and coverage **after every round** of the fake-bug challenge | AdverTest (Fig. 2) | Our rule-based and AI mutants, plus JaCoCo | `classes[].mutation[]` |
| **Syntax correctness rate** of the first answer | TestAgent, EMSE prompt study | Java parser | `first_attempt.syntax_rate` |
| **Compilation success rate** of the first answer | TestAgent, CAT, EMSE prompt study | javac, per test method | `first_attempt.compile_rate` |
| **Execution success rate** (runs without a runtime error) | TestAgent | Run on the fixed code | `first_attempt.execution_rate` |
| **Pass rate** of the first answer | TestForge (pass@1), TestPilot | Run on the fixed code | `first_attempt.pass_rate` |
| **Oracle correctness** (correct assertions / all assertions) | CANDOR | Failing assertions on the correct (fixed) code | `first_attempt.oracle_correctness` |
| **Code extractability**: did the AI return a proper code block? | EMSE prompt study (MSR/CSR) | Checked when the code is extracted | `first_attempt.output_format` |
| Number of tests and assertions | TestAgent, EMSE | Counted | `final_tests`, `final_oracles` |
| **Flaky tests** (pass in some runs, fail in others; each test runs 3 times) | Chu et al. survey | Repeated runs | `removed` (reason `flaky`) |
| Instruction coverage | ChatGPT-vs-SBST, Hybrid SBST+LLM | JaCoCo | `classes[].coverage.class.instruction` |
| **False alarms** and **bug-copying tests** (realistic mode) | CANDOR (correctness on faulty code), TestAgent (precision) | Run on both versions | `false_alarms`, `bug_encoding_tests` |
| AI mutants: usable / did not compile / killed / survived | AdverTest, Meta ACH | Compile and run each mutant | `mutants.json` |
| **Cost: tokens in and out**, AI calls | AdverTest, TestAgent, TestForge, MutGen | From the API response | `llm_calls` |
| **Time** per bug | AdverTest, TestAgent, MutGen | Clock | `seconds` |
| **Repeated runs** (3 runs) | CANDOR (3 runs + Wilcoxon test) | Settings `full`, `full-repeat2`, `full-repeat3` | `results/full-repeat*` |
| EvoSuite baseline: FDR, coverage, Major mutation | AdverTest, TestAgent, CAT, ChatGPT-vs-SBST | Defects4J scripts; EvoSuite's tests are now kept | `results/evosuite/` |

## Computed at the end, from the saved tests (no AI and no re-run needed)

| Measurement | Used by | Tool |
|---|---|---|
| **Test smells** (Assertion Roulette, Magic Number Test, …) | EMSE prompt study, Chu et al. survey | tsDetect |
| **Readability** | EMSE prompt study, ChatGPT-vs-SBST, TestAgent | Readability model or LLM judge |
| **Statistical tests** (Wilcoxon signed-rank, Vargha–Delaney A12) | CANDOR, CAT, MutGen | Python (scipy) |
| Money cost at public API prices | AdverTest, TestForge | Tokens × price |
| **Tests with no real assertion** ("non-trivial assertions") | TestPilot | Parse the saved tests |
| **Similarity to the developers' own tests** (memorisation / data-leak check) | TestPilot | Compare with Defects4J's test files |
| **Equivalent mutants**: a hand-checked sample of surviving AI mutants | Meta ACH, AdverTest | Manual check |
| Randoop baseline (optional) | AI-Powered Multi-LLM paper, Enriching test inputs | Defects4J `gen_tests.pl -g randoop` |

## Not possible or not applicable for us

| Measurement | Used by | Why not |
|---|---|---|
| Human / developer acceptance studies | Meta ACH, TestAgent (user study) | Needs real developers |
| BLEU / CodeBLEU | ChatAssert, test-repair papers | Compares text with a reference test; not meaningful for bug finding |
| Python / JavaScript benchmarks | TestPilot, Nessie, TestForge | Our study is Java + Defects4J |
| GrowingBugs (newer bugs, data-leak check) | AdverTest | Not installed; a possible extra later |
| Acc@k, exact match against reference assertions | ChatAssert | Compares text with a reference; we judge by running tests instead |

Papers checked: all 27 testing-related PDFs in `Research/` (two other PDFs are about vision-language models for walking assistants, and one test-repair PDF is a duplicate).
