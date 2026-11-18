"""Extra measurements for AdverTest, from its SAVED tests (no re-generation, no AI).

  python3 scripts/advertest_extras.py bugs/dev40.txt

Same numbers as for our framework and EvoSuite (see evosuite_extras.py):
  - coverage of the buggy method(s) and the class (JaCoCo)
  - PIT mutation score (class and buggy method)
  - number of tests and assertions
Measured on the tests that Defects4J kept (fix_test_suite.pl drops tests that fail on
the fixed version), i.e. the same tests that were used for bug detection.
AdverTest writes `package gentest.<real package>`; it uses JUnit 4 + Mockito.
"""
import json
import re
import shutil
import subprocess
import sys
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "scripts"))

from our_framework import context, coverage, d4j, pit, runner  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR, _count_oracles  # noqa: E402
import run_advertest as ra  # noqa: E402

LIB = TOOLS_DIR / "advertest" / "lib"
MOCK_CP = [LIB / "mockito-core-4.11.0.jar", LIB / "byte-buddy-1.14.4.jar", LIB / "objenesis-3.2.jar"]
TAG = "advertest"


def kept_tests(bug_name, work):
    """AdverTest's tests after Defects4J removed the ones failing on the fixed version."""
    project, bid = bug_name.rsplit("-", 1)
    shutil.rmtree(work, ignore_errors=True)
    suite = work / "suite"
    suite.mkdir(parents=True)
    arch = suite / f"{project}-{bid}f-advertest.1.tar.bz2"
    ra.repack(RESULTS_DIR / "advertest" / bug_name / "advertest_tests.tar.bz2", arch, work / "unpack")
    ra.sh([str(ra.ADV_UTIL / "fix_test_suite.pl"), "-p", project, "-d", str(suite), "-v", f"{bid}f"],
          work / "fix.log")
    return arch if arch.exists() else None


def measure(bug_name):
    rep_file = RESULTS_DIR / "advertest" / bug_name / "report.json"
    if not rep_file.exists() or not (rep_file.parent / "advertest_tests.tar.bz2").exists():
        return None
    report = json.loads(rep_file.read_text())
    project, bid = bug_name.rsplit("-", 1)
    bug = d4j.Bug(project, int(bid))
    bug.checkout("f")
    bug.checkout("b")                       # context.build compares both versions
    arch = kept_tests(bug_name, TOOLS_DIR / "advertest_out" / f"extras-{bug_name}")
    out = runner.build_dir(bug, "f", TAG)
    shutil.rmtree(out, ignore_errors=True)
    src = out / "src"
    src.mkdir(parents=True)
    if arch is None:
        report["classes"] = [{"status": "error", "detected": False, "final_tests": 0, "final_oracles": 0,
                              "error": "no AdverTest test survived on the fixed version"}]
        report["extras_measured"] = True
        rep_file.write_text(json.dumps(report, indent=2))
        return report["classes"]
    with tarfile.open(arch) as t:
        t.extractall(src, filter="data")
    for junk in src.rglob("._*"):            # macOS metadata files inside the archive
        junk.unlink()
    files = list(src.rglob("*.java"))
    cp = ":".join([runner.project_classpath(bug, "f"), str(runner.JUNIT_JAR), *map(str, MOCK_CP)])
    (out / "classes").mkdir()
    proc = subprocess.run([runner.JAVAC, "-nowarn", "-encoding", "UTF-8", "-proc:none", "-cp", cp,
                           "-d", str(out / "classes"), *map(str, files)],
                          capture_output=True, text=True, timeout=900)
    modified = bug.export("classes.modified").split(";")
    classes = []
    for test_file in files:
        code = test_file.read_text(errors="replace")
        if "@Test" not in code:
            continue
        pkg = re.search(r"^\s*package\s+([\w.]+)\s*;", code, re.M).group(1)
        test_class = test_file.stem
        target = test_class[:-len("Test")] if test_class.endswith("Test") else test_class
        class_fqn = next((c for c in modified if c.rsplit(".", 1)[-1] == target), modified[0])
        entry = {"class": class_fqn, "status": "ok", "detected": report.get("detected"),
                 "final_tests": len(re.findall(r"@Test", code)), "final_oracles": _count_oracles(code)}
        if proc.returncode != 0:
            entry.update(status="error", error="AdverTest tests do not compile: " + proc.stderr[-500:])
            classes.append(entry)
            continue
        try:
            ctx = context.build(bug, class_fqn, "f")
            entry["focal_methods"] = [m.name for m in ctx.focal_methods]
            entry["coverage"] = coverage.measure(bug, "f", class_fqn, ctx.focal_methods, pkg, test_class,
                                                 tag=TAG, extra_cp=MOCK_CP)
            for _ in range(2):                # retry once, as for EvoSuite
                entry["pit"] = pit.measure(bug, "f", class_fqn, ctx.focal_methods, pkg, test_class,
                                           out / "classes", extra_cp=MOCK_CP)
                if "error" not in entry["pit"]:
                    break
        except Exception as e:  # noqa: BLE001
            entry["extras_error"] = str(e)[:500]
        classes.append(entry)
    if classes:
        report["classes"] = classes
        report["extras_measured"] = True
        rep_file.write_text(json.dumps(report, indent=2))
    return classes


if __name__ == "__main__":
    bugs = Path(sys.argv[1]).read_text().split()
    for b in bugs:
        rep = RESULTS_DIR / "advertest" / b / "report.json"
        if not rep.exists() or json.loads(rep.read_text()).get("extras_measured"):
            continue
        cls = measure(b)
        if cls is None:
            print(f"{b}: no saved AdverTest tests", flush=True)
            continue
        for c in cls:
            cov = ((c.get("coverage") or {}).get("focal") or {})
            p = ((c.get("pit") or {}).get("class") or {}).get("score")
            print(f"{b} {c.get('class', '?').rsplit('.', 1)[-1]}: {c['final_tests']} tests, method line cov "
                  f"{cov.get('line')}, PIT {p} {c.get('error', '') or c.get('extras_error', '')}"[:300], flush=True)
