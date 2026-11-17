# Research Plan (Simple Version)

## 1. What is this about?

Programmers write **tests**: small pieces of code that check that the main code works correctly. Writing tests by hand takes a lot of time, so researchers are using **AI (large language models)** to write tests automatically.

Some researchers use a **team of AI agents**. Each agent has one job, like a team of people working together.

## 2. The two frameworks we are comparing

### CANDOR (gets very good results)
- Uses a team of **8 AI agents**: one plans, one writes tests, one checks them, a group discusses the right answers, and so on.
- It was tested on **small, simple coding problems** (HumanEvalJava and LeetCodeJava). Each problem is a single, separate function, like an exam question.
- Its results are **very good**.

### AdverTest (results look weaker)
- Uses only **2 AI agents**:
  - one writes tests;
  - the other adds small fake bugs to the code to see if the tests catch them.
- It was tested on **Defects4J**, a collection of **real bugs from real, large software projects**.
- Its results look **lower** than CANDOR's.

## 3. What is the problem?

The comparison is not fair: CANDOR was tested on easy, small problems, and AdverTest on hard, real-world projects.

**Real projects are much harder because:**
1. **The code is connected.** One piece of code depends on many other pieces, and the AI only sees a small part, so it gets confused.
2. **The AI makes things up.** It often uses names that don't exist in the project, so the test does not even run.
3. **The AI copies the bug.** If it reads buggy code to decide the "correct answer", the test agrees with the bug and never catches it.
4. **The AI may have seen the answers.** These projects are public, so the AI may have memorized them during training.

**Nobody has shown that a strong team like CANDOR works on real projects like Defects4J.** The CANDOR authors say so themselves: it is left for future work.

## 4. What are we solving?

**Goal:** build a new framework that gets **CANDOR-level results on real projects (Defects4J)**, and does better than AdverTest.

## 5. How will we solve it? (Approach)

We take the **best ideas from both** and add the missing pieces:

| Idea | Taken from | Why it helps |
|---|---|---|
| Give the AI enough information about the surrounding code | **New** (our addition) | Fixes problem 1: the AI understands the project better |
| Run the test, and if it fails, show the AI the error so it can fix the test | **New** (our addition) | Fixes problem 2: fewer broken tests |
| Several AIs discuss and vote on the correct answer, based on the **description** of the code rather than the code itself | **CANDOR** | Fixes problem 3: the AI doesn't copy the bug |
| One agent adds fake bugs and the tests must catch them; repeat a few times | **AdverTest** | Makes the tests stronger and sharper |
| Also test on newer bugs the AI has likely not seen | **New** (our addition) | Fixes problem 4: proves results are real, not memorized |

### The steps, in simple words
1. **Understand:** collect the important information about the code to be tested.
2. **Plan:** decide which situations to test: normal, edge and error cases.
3. **Write:** the AI writes the tests.
4. **Fix:** run the tests, and if they are broken, let the AI repair them.
5. **Agree on answers:** several AIs vote on what the correct result should be.
6. **Challenge:** add fake bugs. If the tests miss them, improve the tests, and repeat.

## 6. How will we know it works?

- **Main check:** count how many **real bugs** in Defects4J our tests catch.
- **Compare** against CANDOR, AdverTest and other known tools on the same bugs.
- **Remove one step at a time** to see how much each step helps.
- **Start small:** first try on about 50 bugs, then run on the full dataset.

## 7. Related work we must know: TestAgent (2026)

**TestAgent** (Zhang et al., "Multi-Agent LLM Collaboration for Unit Test Generation via Human-Testing-Inspired Workflows", arXiv 2607.09101, July 2026; also an FSE 2026 demo) is very close to our idea.

**What TestAgent already does**, so we can NOT claim these as new:
- A team of AI agents: a Planner, a Generator and a Reviewer.
- Gives the AI information about the whole project, through a "knowledge graph".
- Runs the tests and repairs errors.
- Uses the mutation score (from the PIT tool) as feedback.
- Uses code from Defects4J projects (Lang, Chart and others: 1,454 methods).

**What TestAgent does NOT do**, which is where our work is new:

| | TestAgent | Ours |
|---|---|---|
| Checks if tests catch **Defects4J's real bugs** (buggy vs fixed) | ❌ It measures coverage and mutation score only. Its "154 bugs" are new crashes found by hand. | ✅ Our main measurement |
| **Several AIs vote** on the correct expected result (CANDOR's panel) | ❌ One model | ✅ 3 AIs from different families |
| **Realistic mode**: tests written on buggy code (does the AI copy the bug?) | ❌ | ✅ |
| **AI-written fake bugs** competing with the tests (as in AdverTest) | ❌ | ✅ |
| Cost | Paid GPT-4o | Free models only |

**What this changes:** "a multi-agent test generator with context, repair and mutation feedback" is no longer new on its own. Our contribution is the parts in the table above. TestAgent goes into the thesis as related work. It would be a baseline too, but its code is not publicly linked in the paper.

## 8. All related frameworks (searched 16 Nov 2026)

We searched arXiv, ISSTA/ICSE/FSE/ASE papers and the AwesomeLLM4UT list. **Many related tools exist, but none combines all three of our ideas on Defects4J real bugs:** the voting panel, fake-bug practice, and the project-information sheet.

### A. Direct competitors: whole-test generators that report Defects4J bug detection
| Framework | Main idea | What is known | Can we run it? |
|---|---|---|---|
| **AdverTest** (ISSTA 2026, arXiv 2602.08146) | test agent vs. mutant agent | 66.6% bugs detected (HITS 61.4%, EvoSuite 40.8%) | ✅ code public, **we should run it** |
| **HITS** (arXiv 2408.11324) | splits a method into slices | 61.4% (from the AdverTest paper) | paper numbers |
| **Synthetic Bug Feedback** (arXiv 2607.11573) | fake bugs made from patterns of **real past bugs** | real-bug detection on Defects4J; closest to our fake-bug step | no code link found |
| **BenjiTest** (SSRN 2026) | agentic generate-and-correct loop | 46.1% detected (zero-shot 10.5%) | paper numbers |
| **ChatUniTest** (arXiv 2305.04764) | generate, check, repair | baseline in AdverTest | ✅ open source |
| **TestART** (arXiv 2408.03095) | generation and repair loop | 78.6% pass rate on Defects4J | paper numbers |
| **TestAgent** (arXiv 2607.09101) | multi-agent with a knowledge graph | coverage and mutation only (see section 7) | no code |

### B. Coverage-focused Java tools (report coverage, not real-bug detection)
CAT (arXiv 2604.22046, call-chain context), PANTA, IntTestGen (arXiv 2605.26851), SymPrompt, TestSpark, CoverUp.

### C. Oracle (expected-value) generation, similar to our voting panel
CANDOR (panel discussion), AssertMate (arXiv 2608.05822: several agents plus an AI judge, on Defects4J), Nexus, TOGLL, ChatAssert, AugmenTest.

### D. Mutation-guided, similar to our fake-bug step
MutGen, Meta's ACH, LLMutantKiller (ISSTA 2026), MuTAP.

### E. Other multi-agent unit-test frameworks
MACO (2026), TestForge, SCATE (supervising coding agents), and the "Human-Testing-Inspired Workflows" paper (= TestAgent).

### F. Two findings from related work that matter for us
1. **CANDOR showed that plain majority voting is worse than a panel discussion**, in which the AIs explain their reasons and the most consistent reasoning wins. Our panel currently uses majority voting, so this is an improvement to try.
2. **The misguidance paper** (arXiv 2607.22883, ISSTA 2026) shows that buggy code tricks the AI into copying the bug, which matches our Lang-1 finding. Its fix is to show the AI an **AI-written specification** instead of the code. We can try this in realistic mode.

### Comparison plan
- **Run ourselves on the same bugs:** EvoSuite (done), AdverTest (to do), single-shot AI (to do), Randoop (optional).
- **Compare by published numbers:** HITS, BenjiTest, CANDOR, CAT, TestAgent, Synthetic Bug Feedback.

## 9. In one line

> CANDOR works well on easy problems but has not been tried on real projects. AdverTest works on real projects but uses only 2 agents. TestAgent uses a team of agents on real projects but never checks real known bugs and has no voting. **We bring CANDOR's voting AIs to real-world bugs (Defects4J), add AdverTest's fake-bug challenge, and study the realistic case where the AI is given buggy code, using only free models.**
