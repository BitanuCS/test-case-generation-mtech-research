"""Line and branch coverage of the class / methods under test (JaCoCo)."""
import subprocess
import xml.etree.ElementTree as ET

from . import runner
from .config import TOOLS_DIR

JACOCO_CLI = TOOLS_DIR / "jacoco" / "org.jacoco.cli-0.8.12-nodeps.jar"


def _counters(elem):
    out = {}
    for c in elem.findall("counter"):
        missed, covered = int(c.get("missed")), int(c.get("covered"))
        total = missed + covered
        out[c.get("type").lower()] = round(covered / total, 3) if total else None
    return out


def measure(bug, version, class_fqn, focal_methods, package, test_class, tag="suite", extra_cp=()):
    """Run the (already compiled) test class with JaCoCo and report coverage.

    Returns {"class": {"line": .., "branch": ..}, "focal": {"line": .., "branch": ..}}
    """
    work = runner.build_dir(bug, version, "coverage")
    work.mkdir(parents=True, exist_ok=True)
    exec_file = work / "jacoco.exec"
    runner.run_test(bug, version, package, test_class, coverage_file=exec_file, timeout=300,
                    tag=tag, extra_cp=extra_cp)
    if not exec_file.exists():
        return None

    bin_dir = bug.workdir(version) / bug.export("dir.bin.classes", version)
    rel = class_fqn.replace(".", "/")
    class_files = [p for p in (bin_dir / rel).parent.glob(rel.rsplit("/", 1)[-1] + "*.class")
                   if p.stem == rel.rsplit("/", 1)[-1] or p.stem.startswith(rel.rsplit("/", 1)[-1] + "$")]
    if not class_files:
        return None
    xml_file = work / "report.xml"
    cmd = [runner.JAVA, "-jar", str(JACOCO_CLI), "report", str(exec_file), "--xml", str(xml_file)]
    for f in class_files:
        cmd += ["--classfiles", str(f)]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
    if proc.returncode != 0 or not xml_file.exists():
        return None

    root = ET.parse(xml_file).getroot()
    result = {"class": None, "focal": None}
    focal = {"line": [0, 0], "branch": [0, 0]}   # [covered, total]
    for cls in root.iter("class"):
        if cls.get("name") != rel:
            continue
        result["class"] = _counters(cls)
        for meth in cls.findall("method"):
            name = meth.get("name")
            line = int(meth.get("line") or 0)   # first code line of the method
            if not any(m.start_line <= line <= m.end_line and
                       (m.name == name or (m.kind == "constructor" and name == "<init>"))
                       for m in focal_methods):
                continue
            for c in meth.findall("counter"):
                t = c.get("type").lower()
                if t in focal:
                    focal[t][0] += int(c.get("covered"))
                    focal[t][1] += int(c.get("missed")) + int(c.get("covered"))
    # Focal coverage from the per-LINE data of the source file: lines inside the method(s)
    # under test. (Matching JaCoCo's <method> entries by their first line fails for
    # constructors, whose first line is often a field initializer outside the constructor.)
    src_name = rel.rsplit("/", 1)[-1] + ".java"
    pkg_name = rel.rsplit("/", 1)[0] if "/" in rel else ""
    lines = {"line": [0, 0], "branch": [0, 0]}
    missed = []
    for pkg in root.iter("package"):
        if pkg.get("name") != pkg_name:
            continue
        for sf in pkg.findall("sourcefile"):
            if sf.get("name") != src_name:
                continue
            for ln in sf.findall("line"):
                nr = int(ln.get("nr"))
                if not any(m.start_line <= nr <= m.end_line for m in focal_methods):
                    continue
                mi, ci = int(ln.get("mi", 0)), int(ln.get("ci", 0))
                mb, cb = int(ln.get("mb", 0)), int(ln.get("cb", 0))
                if mi + ci:
                    lines["line"][1] += 1
                    lines["line"][0] += 1 if ci else 0
                    if not ci:
                        missed.append(nr)
                lines["branch"][0] += cb
                lines["branch"][1] += mb + cb
    if lines["line"][1]:
        focal = lines
    result["focal"] = {t: (round(cov / tot, 3) if tot else None) for t, (cov, tot) in focal.items()}
    # line numbers inside the method(s) under test that no test ran (fed back to the AI)
    result["focal_missed_lines"] = sorted(set(missed))
    return result
