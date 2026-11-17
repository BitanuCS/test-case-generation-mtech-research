"""The AI agents.

  Planner      - lists test scenarios                       (main model)
  Tester       - writes the JUnit test class                (main model)
  Fixer        - repairs compile errors / wrong tests       (main model)
  Panel judge  - one panel member's vote on failing tests   (panel models)
  Mutant agent - invents realistic fake bugs                (main model)
  Killer       - writes extra tests for surviving mutants   (main model)

The Curator (vote counting) is plain code in pipeline.py.
"""
import json
import re

from . import llm

SYSTEM = (
    "You are an expert Java developer who writes thorough, correct JUnit tests. "
    "You only use classes, methods and constructors that really exist in the code you are shown "
    "or in the standard Java library. Never invent APIs."
)


def _extract_json(text):
    m = re.search(r"```(?:json)?\s*(\{.*?\}|\[.*?\])\s*```", text, re.S)
    raw = m.group(1) if m else text[text.find("{"): text.rfind("}") + 1]
    return json.loads(raw)


def extract_java(text):
    """Take the (last, longest) ```java code block from an answer."""
    blocks = re.findall(r"```(?:java)?\s*\n(.*?)```", text, re.S)
    if not blocks:
        # No closed block: drop an unclosed opening fence if there is one.
        extract_java.last = "unclosed_block" if "```" in text else "no_block"
        return re.sub(r"^.*?```(?:java)?\s*\n", "", text, count=1, flags=re.S).strip() + "\n"
    extract_java.last = "code_block"
    return max(blocks, key=len).strip() + "\n"


extract_java.last = None   # how the last answer's code was found (for extractability stats)


def fix_header(code, package, class_name):
    """Make sure the package and class name are exactly what we expect."""
    if re.search(r"^\s*package\s+[\w.]+\s*;", code, re.M):
        code = re.sub(r"^\s*package\s+[\w.]+\s*;", f"package {package};", code, count=1, flags=re.M)
    else:
        code = f"package {package};\n\n" + code
    code = re.sub(r"^(public\s+)?(final\s+)?class\s+\w+", f"public class {class_name}",
                  code, count=1, flags=re.M)
    return code


# ---------------------------------------------------------------- Planner
def plan(ctx):
    names = ", ".join(m.name for m in ctx.focal_methods)
    prompt = f"""Below is a Java class. Your job: plan unit tests for the method(s) under test: {names}.

{ctx.text}

Think about every important behaviour described in the Javadoc and implemented in the code:
normal inputs, boundary values (empty, zero, min/max, limits of int/long, length limits),
special formats, invalid inputs and the exceptions they cause, and every major branch.

Return ONLY a JSON object in a ```json block, like this:
{{"scenarios": [
  {{"method": "<method under test>", "name": "<short camelCase name>",
    "input": "<the concrete input values>",
    "expected": "<what should happen: return value or exception, and WHY (Javadoc/code)>"}}
]}}
Give 10 to 20 scenarios, the most bug-revealing ones first."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    try:
        return _extract_json(answer)["scenarios"]
    except (ValueError, KeyError):
        return [{"method": names, "name": "fromPlannerText", "input": "", "expected": answer[:4000]}]


# ---------------------------------------------------------------- Tester
def write_tests(ctx, scenarios, test_class, spec_first=False):
    """scenarios=None means "no Planner": the Tester decides what to test itself.

    spec_first=True (realistic mode): the code may be buggy, so expected
    values must come from the documentation, not from tracing the code.
    """
    junit_rules = (
        "Use JUnit 4: import org.junit.Test; import static org.junit.Assert.*; annotate each test with @Test. "
        "For expected exceptions use @Test(expected = X.class) or try { ...; fail(); } catch (X e) {}."
        if ctx.junit == 4 else
        "Use JUnit 3: extend junit.framework.TestCase, test method names start with 'test'."
    )
    prompt = f"""Write a complete JUnit test class for the method(s) under test in this Java code.

{ctx.text}

{_plan_text(scenarios)}

Rules:
- Package: {ctx.package}   Class name: {test_class}
- {junit_rules}
- Only call methods/constructors that exist in the code above. Add all needed imports.
- Each test method checks ONE scenario and has precise assertions (exact values, exact types,
  exact exception classes). Use assertEquals with a delta for floating point.
- {ORACLE_RULE_SPEC if spec_first else ORACLE_RULE_TRACE}
- No mocking frameworks, no reflection on private members, no Thread.sleep, no file or network access.

Return ONLY the full Java file in one ```java block."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    return fix_header(extract_java(answer), ctx.package, test_class)


ORACLE_RULE_TRACE = "Decide expected values from the Javadoc and by carefully tracing the code."
ORACLE_RULE_SPEC = ("The code under test MAY CONTAIN BUGS. Decide expected values from the Javadoc, the method "
                    "name and the obvious intent -- NOT by tracing what the code currently does. A test that "
                    "fails because the code is wrong is exactly what we want.")


def _plan_text(scenarios):
    if scenarios is None:
        return "Write 10 to 20 test methods covering normal, boundary and error cases."
    return ("Test plan (implement one test method per scenario):\n```json\n"
            + json.dumps(scenarios, indent=1) + "\n```")


# ---------------------------------------------------------------- Fixer
def fix_compile(ctx, code, compile_output, test_class):
    prompt = f"""This JUnit test class does not compile.

Code under test:
{ctx.text}

Test class:
```java
{code}
```

Compiler errors:
```
{compile_output[:6000]}
```

Fix ALL errors. Keep every test method that can be fixed; delete a test method only if it
cannot work with the real API. Do not change what the tests check unless needed to compile.
Return ONLY the full corrected Java file in one ```java block."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    return fix_header(extract_java(answer), ctx.package, test_class)


def fix_failures(ctx, code, failures, test_class):
    """failures: {method: message+trace} from running on the FIXED (assumed correct) version."""
    fail_text = "\n\n".join(f"{name}:\n{msg}" for name, msg in failures.items())
    prompt = f"""This JUnit test class compiles, but some tests fail when run against the code under test.
The code under test is the reference behaviour: it is correct. So each failure means the TEST is wrong
(wrong expected value, wrong exception type, wrong setup).

Code under test:
{ctx.text}

Test class:
```java
{code}
```

Failing tests:
```
{fail_text[:6000]}
```

For each failing test, trace the code under test carefully and correct the test so it matches the real
behaviour. Do not weaken assertions (no removing asserts, no catching Throwable). If a test cannot be
made correct, delete it. Leave passing tests unchanged.
Return ONLY the full corrected Java file in one ```java block."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    return fix_header(extract_java(answer), ctx.package, test_class)


# ---------------------------------------------------------------- Panel judge
VERDICTS = ("TEST_CORRECT", "CODE_CORRECT", "UNSURE")


def judge_failures(ctx, failing, models):
    """One panel member's opinion on each failing test.

    failing: {test_name: (test_method_source, failure_message)}
    Returns {test_name: verdict} with verdict in VERDICTS.

    The member sees the documentation and code of the method under test, but
    is told that the code may contain bugs: the documentation and the obvious
    intent of the method decide what is correct.
    """
    cases = "\n\n".join(
        f"### {name}\n```java\n{src.strip()}\n```\nResult when run: {msg.splitlines()[0][:400]}"
        for name, (src, msg) in failing.items()
    )
    prompt = f"""You are reviewing unit tests for a Java method. The tests below FAIL when run.
For each one decide who is right:
  TEST_CORRECT - the test expects the behaviour the documentation/intent requires; the CODE has a bug.
  CODE_CORRECT - the code behaves as documented; the TEST's expectation is wrong.
  UNSURE       - the documentation does not make it clear.
Important: the code may contain a bug. Judge by the Javadoc, the method name and common sense about
what the method is supposed to do -- not simply by what the code currently does.

Method(s) under test:
```java
{ctx.focal_text[:9000]}
```

Failing tests:
{cases}

Answer ONLY with a JSON object in a ```json block:
{{"verdicts": [{{"test": "<test name>", "verdict": "TEST_CORRECT|CODE_CORRECT|UNSURE", "reason": "<one sentence>"}}]}}"""
    answer = llm.chat([{"role": "user", "content": prompt}], models=models)
    out = {}
    try:
        for v in _extract_json(answer).get("verdicts", []):
            verdict = str(v.get("verdict", "")).upper()
            if v.get("test") in failing and verdict in VERDICTS:
                out[v["test"]] = verdict
    except (ValueError, AttributeError):
        pass
    return out


# ---------------------------------------------------------------- Mutant agent
def write_mutants(ctx, test_code, n=5):
    """Realistic fake bugs that the current tests probably do not catch."""
    prompt = f"""You are a mutation-testing expert. Below is a Java method under test and its current tests.
Invent {n} REALISTIC bugs a developer could plausibly make in the method under test (off-by-one,
wrong boundary, missing special case, wrong operator, wrong variable, swapped arguments, ...),
especially bugs the current tests would NOT detect. Each bug must still compile.

Method(s) under test:
```java
{ctx.focal_text[:12000]}
```

Current tests:
```java
{test_code[:12000]}
```

Describe each bug as an exact text replacement inside the method: "original" must be copied EXACTLY
(same spaces) from the method above and be unique in it; "mutated" is the buggy replacement.
Answer ONLY with a JSON object in a ```json block:
{{"mutants": [{{"description": "<the bug>", "original": "<exact snippet>", "mutated": "<buggy snippet>"}}]}}"""
    answer = llm.chat([{"role": "user", "content": prompt}])
    try:
        return [m for m in _extract_json(answer).get("mutants", []) if isinstance(m, dict)]
    except (ValueError, AttributeError):
        return []


# ---------------------------------------------------------------- Killer (Tester again)
def kill_mutants(ctx, code, survivors_text, test_class):
    """Ask for NEW test methods that detect the surviving fake bugs."""
    prompt = f"""Our JUnit tests for the code below do not detect these small changes ("mutants") to the
method under test. A good test suite should FAIL on each mutant and PASS on the original code.

Code under test:
{ctx.text}

Current test class:
```java
{code}
```

Surviving mutants:
{survivors_text}

Write NEW test methods (do not repeat existing ones) that pass on the original code and fail on these
mutants. Base expected values on the documentation and the original code. Return a complete Java class
named {test_class} in package {ctx.package} containing ONLY the new test methods (plus any imports they
need), in one ```java block."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    return fix_header(extract_java(answer), ctx.package, test_class)


# ---------------------------------------------------------------- Panel review of passing tests
REVIEW = ("AGREE", "DISAGREE", "UNSURE")


def review_tests(ctx, cases, models):
    """One panel member checks tests that PASS on the (possibly buggy) code.

    cases: {test_name: test_method_source}
    Returns {test_name: (verdict, reason)}; DISAGREE means the test expects
    something different from the documented behaviour, i.e. the test (and
    probably the code) is wrong.
    """
    tests = "\n\n".join(f"### {n}\n```java\n{src.strip()}\n```" for n, src in cases.items())
    prompt = f"""You are reviewing unit tests for a Java method. These tests currently PASS, but the code under
test MAY CONTAIN A BUG, and the tests may have been written by copying the buggy behaviour.
For each test, check its expected results against the Javadoc, the method name and the obvious intent:
  AGREE    - the expectations are what the documented/intended behaviour requires.
  DISAGREE - at least one expectation contradicts the documented/intended behaviour. Say what the correct
             expectation is.
  UNSURE   - the documentation does not decide it.
Only answer DISAGREE when you are confident.

Method(s) under test:
```java
{ctx.focal_text[:9000]}
```

Tests:
{tests}

Answer ONLY with a JSON object in a ```json block:
{{"reviews": [{{"test": "<test name>", "verdict": "AGREE|DISAGREE|UNSURE", "reason": "<one sentence; for DISAGREE, the correct expectation>"}}]}}"""
    answer = llm.chat([{"role": "user", "content": prompt}], models=models)
    out = {}
    try:
        for v in _extract_json(answer).get("reviews", []):
            verdict = str(v.get("verdict", "")).upper()
            if v.get("test") in cases and verdict in REVIEW:
                out[v["test"]] = (verdict, str(v.get("reason", ""))[:400])
    except (ValueError, AttributeError):
        pass
    return out


def fix_to_spec(ctx, code, items, test_class):
    """Rewrite the listed tests so they expect the DOCUMENTED behaviour.

    items: {test_name: [reasons from the panel]}
    """
    notes = "\n".join(f"- {n}: " + " | ".join(r for r in rs if r) for n, rs in items.items())
    prompt = f"""A review panel found that some tests in this class expect behaviour that contradicts the
documentation of the method under test. The code under test may be buggy, so these tests must be rewritten to
expect the DOCUMENTED behaviour -- even if that makes them fail on the current code.

Code under test:
{ctx.text}

Test class:
```java
{code}
```

Tests to rewrite, with the panel's notes:
{notes}

Rewrite ONLY these test methods (keep their names); leave all other tests unchanged.
Return ONLY the full Java file in one ```java block."""
    answer = llm.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
    return fix_header(extract_java(answer), ctx.package, test_class)
