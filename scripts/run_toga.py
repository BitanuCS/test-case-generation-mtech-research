"""Baseline: TOGA (ICSE 2022, neural test-oracle generation; github.com/microsoft/toga).

  python3 scripts/run_toga.py bugs/dev40.txt

TOGA does not write test inputs itself: like in its paper, it takes EvoSuite's tests, removes
their oracles, and its two trained CodeBERT models predict a new oracle for every test prefix
(an expected exception, or one assertion picked from a candidate list). We use the SAME EvoSuite
tests that we evaluate as the EvoSuite baseline (results/evosuite/<bug>/evosuite_tests.tar.bz2),
so EvoSuite vs TOGA isolates the effect of TOGA's oracles.
Then the usual protocol: Defects4J fix_test_suite.pl drops tests that fail on the fixed version
(counted as TOGA false alarms), run_bug_detection.pl runs the rest on the buggy version, plus
Defects4J coverage + Major mutation.
Results: results/toga/<bug>/report.json (+ toga_tests.tar.bz2). Finished bugs are skipped.
"""
import argparse
import csv
import json
import re
import shutil
import subprocess
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from our_framework import d4j, standard_metrics  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402

TOGA = TOOLS_DIR / "toga"
TOGA_PY = TOGA / ".venv" / "bin" / "python"
BIN = d4j.D4J_BIN.parent
OUT = RESULTS_DIR / "toga"

# ---- test building: TOGA's eval/rq3/gen_tests_from_metadata.py (MIT licence) ----------------
fail_catch_extract_re = re.compile(r'try\s*{(.*;).*fail\(.*\)\s*;\s*}\s*catch', re.DOTALL)
assert_re = re.compile(r"assert\w*\s*\((.*)\)")


def get_prefix(test):
    open_curly = test.find('{')
    if 'throws ' not in test[:open_curly]:
        test = test[:open_curly] + ' throws Exception ' + test[open_curly:]
    test = test.replace('// Undeclared exception!', '')
    m_try_catch = fail_catch_extract_re.search(test)
    m_assert = assert_re.search(test)
    if m_try_catch:
        return test[0:m_try_catch.span()[0]] + " " + m_try_catch.group(1).strip()
    if m_assert:
        return test[0:m_assert.span()[0]]
    return test[0:test.rfind('}')]


def bool_assert_to_equals(assertion):
    bool_str = 'true' if 'assertTrue' in assertion else 'false'
    return 'assertEquals(' + bool_str + ', ' + assertion[assertion.find('(') + 1:assertion.rfind(')')] + ')'


def insert_assertion(method, assertion):
    return "\n".join(method.strip().split("\n") + ["      " + assertion + ';'] + ["}"])


def insert_try_catch(method):
    c = method.find('{')
    return method[:c] + '{\n\ttry ' + method[c:] + '\n\t\tfail(\"Expecting exception\"); } catch (Exception e) { }\n\t}'


def build_test(row):
    test = '@Test(timeout = 4000)\n' + get_prefix(row["test_prefix"])
    if str(row["except_pred"]) in ("1", "True"):
        return insert_try_catch(test)
    if row["assert_pred"]:
        a = row["assert_pred"]
        if 'assertEquals' in row["test_prefix"] and ('assertTrue' in a or 'assertFalse' in a):
            a = bool_assert_to_equals(a)
        return insert_assertion(test, a)
    return test + "\n    }"
# -----------------------------------------------------------------------------------------------


def sh(cmd, log, cwd=None, timeout=7200):
    with open(log, "a") as f:
        f.write(f"\n$ {' '.join(map(str, cmd))}\n")
        f.flush()
        try:
            env = d4j._env()
            env.update(HF_HUB_OFFLINE="1", HF_HUB_DISABLE_IMPLICIT_TOKEN="1",   # models are local; no login
                       PATH=f"{TOGA_PY.parent}:{env['PATH']}")         # TOGA's scripts call plain `python`
            return subprocess.run(list(map(str, cmd)), cwd=cwd, env=env, stdout=f,
                                  stderr=subprocess.STDOUT, timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            f.write(f"\nTIMEOUT after {timeout}s\n")
            return -1


def write_suite(evo_src, preds, dst_src):
    """One TOGA test per class (like TOGA's own evaluation, which runs every test separately), so
    one test that does not compile cannot remove the others. Each class keeps the EvoSuite file's
    imports and runner and extends the same EvoSuite scaffolding class."""
    shutil.copytree(evo_src, dst_src)
    originals = {}
    for f in dst_src.rglob("*_ESTest.java"):
        text = f.read_text(errors="replace")
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M).group(1)
        originals[f"{pkg}.{f.stem}"] = (f, text[:text.find("@Test")])
    n = 0
    for k, row in enumerate(preds):
        cls, name = row["test_name"].split("::")
        if cls not in originals:
            continue
        f, header = originals[cls]
        new_cls = f"{f.stem}_T{k}"
        header = re.sub(rf"\bclass\s+{f.stem}\b", f"class {new_cls}", header, count=1)
        (f.parent / f"{new_cls}.java").write_text(header + build_test(row) + "\n}\n")
        n += 1
    for f, _ in originals.values():
        f.unlink()                       # the original EvoSuite oracles are not part of TOGA's suite
    return n


ACTIVE_TEST = re.compile(r"^\s*@Test\b", re.M)


def count_tests(arch):
    """(tests still real, assertions commented out). fix_test_suite.pl replaces a failing test by an
    empty dummy marked '// Defects4J: flaky method' and comments a failing assertion out as
    '// Defects4J: flaky assertion -->'; dummies are not counted as kept tests."""
    n = flaky_asserts = 0
    with tarfile.open(arch) as t:
        for m in t.getmembers():
            name = Path(m.name).name
            if name.endswith(".java") and not name.startswith("._") and "scaffolding" not in name:
                text = t.extractfile(m).read().decode(errors="replace")
                n += len(ACTIVE_TEST.findall(text)) - text.count("// Defects4J: flaky method")
                flaky_asserts += text.count("// Defects4J: flaky assertion")
    return n, flaky_asserts


def one(bug):
    project, bid = bug.rsplit("-", 1)
    evo_arch = RESULTS_DIR / "evosuite" / bug / "evosuite_tests.tar.bz2"
    report = {"bug": bug, "setting": "toga", "detected": False, "llm_calls": {"fresh": 0, "cached": 0},
              "input_tests": "EvoSuite tests of results/evosuite (same suite as the EvoSuite baseline)"}
    work = TOOLS_DIR / "toga_out" / bug
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    if not evo_arch.exists():
        report["error"] = "no EvoSuite tests for this bug (TOGA needs EvoSuite's test prefixes)"
        return finish(report, work, log, t)
    evo_src = work / "evo"
    evo_src.mkdir()
    with tarfile.open(evo_arch) as tar:
        tar.extractall(evo_src, filter="data")
    for junk in evo_src.rglob("._*"):
        junk.unlink()
    b = d4j.Bug(project, int(bid))
    b.checkout("f")
    src_dir = b.workdir("f") / b.export("dir.src.classes")
    sh([TOGA_PY, ROOT / "scripts" / "toga_prepare.py", evo_src, src_dir, project, bid, work], log)
    if not (work / "inputs.csv").exists():
        report["error"] = "could not extract test prefixes (see log)"
        return finish(report, work, log, t)
    # TOGA runs from its own folder (relative paths to its models and vocabulary)
    for name in ("oracle_preds.csv", "exception_preds.csv", "assertion_preds.csv"):
        (TOGA / name).unlink(missing_ok=True)
    sh([TOGA_PY, "toga.py", work / "inputs.csv", work / "meta.csv"], log, cwd=TOGA)
    if not (TOGA / "oracle_preds.csv").exists():
        report["error"] = "TOGA produced no predictions (see log)"
        return finish(report, work, log, t)
    shutil.move(TOGA / "oracle_preds.csv", work / "oracle_preds.csv")
    preds = list(csv.DictReader((work / "oracle_preds.csv").open()))
    report["oracles"] = {"prefixes": len(preds),
                         "exception": sum(str(p["except_pred"]) in ("1", "True") for p in preds),
                         "assertion": sum(bool(p["assert_pred"]) and str(p["except_pred"]) not in ("1", "True")
                                          for p in preds)}
    suite = work / "suite"
    suite.mkdir()
    n_written = write_suite(evo_src, preds, work / "toga_src")
    arch = suite / f"{project}-{bid}f-toga.1.tar.bz2"
    with tarfile.open(arch, "w:bz2") as tar:
        for p in (work / "toga_src").iterdir():
            tar.add(p, arcname=p.name)
    OUT.joinpath(bug).mkdir(parents=True, exist_ok=True)
    shutil.copy(arch, OUT / bug / "toga_tests.tar.bz2")
    sh([BIN.parent / "util" / "fix_test_suite.pl", "-p", project, "-d", suite, "-v", f"{bid}f"], log)
    kept, flaky_asserts = count_tests(arch) if arch.exists() else (0, 0)
    report["tests"] = {"written": n_written, "kept_on_fixed": kept, "false_alarms_removed": n_written - kept,
                       "assertions_commented_out": flaky_asserts}
    if arch.exists():
        shutil.copy(arch, OUT / bug / "toga_tests_kept.tar.bz2")    # the suite after fix_test_suite
    if not kept:
        report["error"] = "no TOGA test survived on the fixed version"
        return finish(report, work, log, t)
    sh([BIN / "run_bug_detection.pl", "-p", project, "-d", suite, "-o", work / "detect"], log)
    det = work / "detect" / "bug_detection"
    if det.exists():
        rows = list(csv.DictReader(det.open()))
        report["detection_rows"] = rows
        report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0 for r in rows)
    else:
        report["error"] = "bug detection did not run (see log)"
    report["standard_metrics"] = standard_metrics.measure_archive(project, suite, work / "std")
    return finish(report, work, log, t)


def finish(report, work, log, t):
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error", "detected": report["detected"]}]
    out = OUT / report["bug"]
    out.mkdir(parents=True, exist_ok=True)
    (out / "report.json").write_text(json.dumps(report, indent=2))
    if log.exists():
        shutil.copy(log, out / "log.txt")
    for f in ("inputs.csv", "oracle_preds.csv"):
        if (work / f).exists():
            shutil.copy(work / f, out / f)
    shutil.rmtree(work, ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()
    todo = [b for b in bugs if not (OUT / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [toga]", flush=True)
    for b in todo:
        r = one(b)
        print(f"{b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s) {r.get('tests', '')}"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
