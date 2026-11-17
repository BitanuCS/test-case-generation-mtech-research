"""Standard measurements with Defects4J's own tools, the same for every tool.

  coverage  - run_coverage.pl : line and branch coverage of the modified class(es)
  mutation  - run_mutation.pl : Major mutation score of the modified class(es)

These are what published studies (EvoSuite, AdverTest, HITS, TestAgent, ...)
report, so our numbers can be compared directly with theirs and with EvoSuite
run on the same bugs.

Works from a test-suite archive in Defects4J's format:
  <dir>/<Project>-<id><b|f>-<tool>.1.tar.bz2   (Java test files under their package folders)
"""
import csv
import shutil
import subprocess
import tarfile
from pathlib import Path

from . import d4j
from . import java_parse as jp
from .config import WORK_DIR

BIN = d4j.D4J_BIN.parent


def _run(script, project, suite_dir, out_dir, tmp_dir, log, bin_dir=None):
    with open(log, "a") as f:
        subprocess.run([str((bin_dir or BIN) / script), "-p", project, "-d", str(suite_dir), "-o", str(out_dir),
                        "-t", str(tmp_dir)], env=d4j._env(), stdout=f, stderr=subprocess.STDOUT,
                       timeout=3600)


def _read(path):
    if not path.exists():
        return None
    rows = list(csv.DictReader(path.open()))
    return rows[-1] if rows else None


def _num(x):
    try:
        return int(x)
    except (TypeError, ValueError):
        return None


def measure_archive(project, suite_dir, work, bin_dir=None):
    """Run coverage + mutation on every archive in suite_dir. Returns a dict."""
    work.mkdir(parents=True, exist_ok=True)
    tmp = work / "tmp"
    tmp.mkdir(exist_ok=True)
    log = work / "log.txt"
    _run("run_coverage.pl", project, suite_dir, work / "cov", tmp, log, bin_dir)
    _run("run_mutation.pl", project, suite_dir, work / "mut", tmp, log, bin_dir)
    cov, mut = _read(work / "cov" / "coverage"), _read(work / "mut" / "mutation")
    out = {}
    if cov:
        lt, lc = _num(cov.get("lines_total")), _num(cov.get("lines_covered"))
        bt, bc = _num(cov.get("branches_total")), _num(cov.get("branches_covered"))
        out["coverage"] = {"lines_total": lt, "lines_covered": lc, "branches_total": bt,
                           "branches_covered": bc,
                           "line": round(lc / lt, 3) if lt and lc is not None else None,
                           "branch": round(bc / bt, 3) if bt and bc is not None else None}
    if mut:
        g, k = _num(mut.get("mut_generated")), _num(mut.get("mut_killed"))
        cv = _num(mut.get("mut_covered"))
        out["major_mutation"] = {"generated": g, "covered": cv, "killed": k,
                                 "score": round(k / g, 3) if g and k is not None else None}
    elif log.exists() and "No mutants generated" in log.read_text(errors="replace"):
        # tiny class (e.g. 3 lines): Major has nothing to mutate -- a real result, not an error
        out["major_mutation"] = {"generated": 0, "covered": 0, "killed": 0, "score": None}
    if not out:
        out["error"] = log.read_text()[-1500:] if log.exists() else "no output"
    return out


def measure_results(bug_name, version, class_dirs, bug_reports=()):
    """Measure our saved final tests for one bug.

    class_dirs: result folders that contain final.java (one per focal class)
    bug_reports: test names to leave out (realistic mode: they fail on purpose)
    """
    project, bid = bug_name.rsplit("-", 1)
    work = WORK_DIR / "_std" / bug_name
    shutil.rmtree(work, ignore_errors=True)
    src = work / "src"
    added = 0
    for folder in class_dirs:
        final = Path(folder) / "final.java"
        if not final.exists():
            continue
        code = final.read_text(encoding="utf-8")
        drop = set(bug_reports) & set(jp.test_method_names(code))
        if drop:
            code = jp.remove_methods(code, drop)
        if not jp.test_method_names(code):
            continue
        pkg = jp.read_class(code).package
        name = jp.read_class(code).name
        dest = src / Path(*pkg.split(".")) / f"{name}.java"
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(code, encoding="utf-8")
        added += 1
    if not added:
        return {"error": "no final tests to measure"}
    suite_dir = work / "suite"
    suite_dir.mkdir()
    with tarfile.open(suite_dir / f"{project}-{bid}{version}-candor.1.tar.bz2", "w:bz2") as tar:
        for f in sorted(src.rglob("*.java")):
            tar.add(f, arcname=str(f.relative_to(src)))
    result = measure_archive(project, suite_dir, work / "out")
    shutil.rmtree(work, ignore_errors=True)
    return result
