"""Extra measurements for EvoSuite, from its SAVED tests (no re-generation, no AI).

  python3 scripts/evosuite_extras.py bugs/dev40.txt

Adds to results/evosuite/<bug>/report.json, per modified class, the same numbers
our framework reports:
  - coverage of the buggy method(s) and the class (JaCoCo)
  - PIT mutation score (class and buggy method)
  - number of tests and assertions
EvoSuite's tests use `separateClassLoader = true`, which hides them from coverage
and mutation tools; as is usual in studies, it is switched off before measuring.
"""
import json
import re
import shutil
import sys
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from our_framework import context, coverage, d4j, pit, runner  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR, _count_oracles  # noqa: E402

EVO_RT = TOOLS_DIR / "defects4j" / "framework" / "lib" / "test_generation" / "runtime" / "evosuite-rt.jar"
TAG = "evosuite"


def measure(bug_name):
    rep_file = RESULTS_DIR / "evosuite" / bug_name / "report.json"
    arch = RESULTS_DIR / "evosuite" / bug_name / "evosuite_tests.tar.bz2"
    if not rep_file.exists() or not arch.exists():
        return None
    report = json.loads(rep_file.read_text())
    project, bid = bug_name.rsplit("-", 1)
    bug = d4j.Bug(project, int(bid))
    bug.checkout("f")
    bug.checkout("b")                       # context.build compares both versions
    out = runner.build_dir(bug, "f", TAG)
    shutil.rmtree(out, ignore_errors=True)
    src = out / "src"
    src.mkdir(parents=True)
    with tarfile.open(arch) as t:
        t.extractall(src)
    for junk in src.rglob("._*"):            # macOS metadata files inside the archive
        junk.unlink()
    files = list(src.rglob("*.java"))
    for f in files:                          # let JaCoCo/PIT see the classes under test
        f.write_text(f.read_text(errors="replace").replace("separateClassLoader = true",
                                                           "separateClassLoader = false"))
    cp = runner.project_classpath(bug, "f") + ":" + str(runner.JUNIT_JAR) + ":" + str(EVO_RT)
    (out / "classes").mkdir()
    import subprocess
    proc = subprocess.run([runner.JAVAC, "-nowarn", "-encoding", "UTF-8", "-proc:none", "-cp", cp,
                           "-d", str(out / "classes"), *map(str, files)],
                          capture_output=True, text=True, timeout=900)
    classes = []
    for test_file in [f for f in files if f.name.endswith("_ESTest.java")]:
        code = test_file.read_text(errors="replace")
        pkg = re.search(r"^package\s+([\w.]+);", code, re.M).group(1)
        test_class = test_file.stem
        class_fqn = f"{pkg}.{test_class[:-len('_ESTest')]}"
        entry = {"class": class_fqn, "status": "ok", "detected": report.get("detected"),
                 "final_tests": len(re.findall(r"@Test", code)), "final_oracles": _count_oracles(code)}
        if proc.returncode != 0:
            entry.update(status="error", error="EvoSuite tests do not compile: " + proc.stderr[-500:])
            classes.append(entry)
            continue
        try:
            ctx = context.build(bug, class_fqn, "f")
            entry["focal_methods"] = [m.name for m in ctx.focal_methods]
            entry["coverage"] = coverage.measure(bug, "f", class_fqn, ctx.focal_methods, pkg, test_class,
                                                 tag=TAG, extra_cp=[EVO_RT])
            for _ in range(2):                # EvoSuite tests are sometimes flaky under PIT: retry once
                entry["pit"] = pit.measure(bug, "f", class_fqn, ctx.focal_methods, pkg, test_class,
                                           out / "classes", extra_cp=[EVO_RT])
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
        rep = RESULTS_DIR / "evosuite" / b / "report.json"
        if rep.exists() and json.loads(rep.read_text()).get("extras_measured"):
            continue
        cls = measure(b)
        if cls is None:
            print(f"{b}: no saved EvoSuite tests", flush=True)
            continue
        for c in cls:
            cov = ((c.get("coverage") or {}).get("focal") or {})
            p = ((c.get("pit") or {}).get("class") or {}).get("score")
            print(f"{b} {c['class'].rsplit('.', 1)[-1]}: {c['final_tests']} tests, method line cov "
                  f"{cov.get('line')}, PIT {p} {c.get('error', '') or c.get('extras_error', '')}"[:200], flush=True)
