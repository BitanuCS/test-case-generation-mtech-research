"""PIT mutation score (the tool CANDOR, TestAgent and MutGen use).

Run with PIT's default mutators on the class under test, using our compiled
test class. Reports the score for the whole class and for the method(s)
under test only (mutants whose line is inside those methods).
"""
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

from . import d4j, runner
from .config import TOOLS_DIR

PIT_JARS = sorted((TOOLS_DIR / "pitest").glob("*.jar"))


def measure(bug, version, class_fqn, focal_methods, package, test_class, test_classes_dir, timeout=1800,
            extra_cp=()):
    """test_classes_dir: folder with the compiled test class (must all pass)."""
    if not PIT_JARS:
        return {"error": "PIT jars missing"}
    out = runner.build_dir(bug, version, "pit_report")
    shutil.rmtree(out, ignore_errors=True)
    # JUnit 4.12 must come BEFORE the project's own jars: some projects (e.g. Cli)
    # ship JUnit 3.8, and then PIT finds no JUnit 4 and refuses to run.
    class_path = ",".join([str(test_classes_dir), str(runner.JUNIT_JAR), *map(str, extra_cp)]
                          + [c for c in runner.project_classpath(bug, version).split(":")
                             if "junit-3" not in Path(c).name])
    src_dir = bug.workdir(version) / bug.export("dir.src.classes", version)
    cmd = [runner.JAVA, "-cp", ":".join(map(str, PIT_JARS)),
           "org.pitest.mutationtest.commandline.MutationCoverageReport",
           "--reportDir", str(out), "--targetClasses", class_fqn,
           "--targetTests", f"{package}.{test_class}", "--classPath", class_path,
           "--sourceDirs", str(src_dir), "--outputFormats", "XML",
           "--timestampedReports=false", "--threads", "2",
           # PIT's own libraries must not mix with the project's (e.g. Lang IS commons-lang3)
           "--includeLaunchClasspath=false"]
    try:
        proc = subprocess.run(cmd, cwd=bug.workdir(version), env=d4j._env(),
                              capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return {"error": "PIT timeout"}
    xml_file = out / "mutations.xml"
    if not xml_file.exists():
        return {"error": (proc.stdout + proc.stderr)[-800:]}
    muts = ET.parse(xml_file).getroot().findall("mutation")

    def score(ms):
        killed = sum(m.get("detected") == "true" for m in ms)
        return {"generated": len(ms), "killed": killed,
                "score": round(killed / len(ms), 3) if ms else None}

    focal = [m for m in muts
             if any(f.start_line <= int(m.findtext("lineNumber") or 0) <= f.end_line for f in focal_methods)]
    return {"class": score(muts), "focal": score(focal)}
