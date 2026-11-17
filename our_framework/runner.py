"""Compile and run one generated JUnit test class against a bug checkout.

We call javac / java directly (instead of `defects4j test`) because it is
much faster and gives clean, per-method results. The classpath comes from
Defects4J (`cp.test`), so the test sees exactly the project's classes and
libraries.

`extra_cp` lets us put other classes in front of the project's classes:
that is how mutants (fake bugs) are tested without touching the checkout.
"""
import re
import shutil
import subprocess
from dataclasses import dataclass, field
from pathlib import Path

from . import d4j
from .config import TOOLS_DIR, WORK_DIR

JAVA_HOME = Path("/opt/homebrew/opt/openjdk@11")
JAVAC = str(JAVA_HOME / "bin" / "javac")
JAVA = str(JAVA_HOME / "bin" / "java")
JUNIT_JAR = TOOLS_DIR / "defects4j" / "framework" / "projects" / "lib" / "junit-4.12-hamcrest-1.3.jar"
JACOCO_AGENT = TOOLS_DIR / "jacoco" / "org.jacoco.agent-0.8.12-runtime.jar"
RUNNER_SRC = Path(__file__).parent / "java" / "TestRunner.java"
RUNNER_DIR = WORK_DIR / "_runner"

_cp_cache = {}


@dataclass
class CompileResult:
    ok: bool
    errors: list = field(default_factory=list)  # [(line, message)]
    raw: str = ""


@dataclass
class RunResult:
    passed: list = field(default_factory=list)
    failed: dict = field(default_factory=dict)  # method -> message + short trace
    timeout: bool = False
    raw: str = ""


def _ensure_runner():
    if (RUNNER_DIR / "TestRunner.class").exists():
        return
    RUNNER_DIR.mkdir(parents=True, exist_ok=True)
    subprocess.run([JAVAC, "-cp", str(JUNIT_JAR), "-d", str(RUNNER_DIR), str(RUNNER_SRC)],
                   check=True, capture_output=True)


def project_classpath(bug, version):
    """cp.test of a checkout; compiles the project once if needed."""
    key = (bug.name, version)
    if key not in _cp_cache:
        wd = bug.workdir(version)
        if not (wd / bug.export("dir.bin.classes", version)).exists():
            res = bug.compile(version)
            if not res.ok:
                raise RuntimeError(f"project compile failed for {bug.name}{version}:\n{res.output[-2000:]}")
        _cp_cache[key] = bug.export("cp.test", version)
    return _cp_cache[key]


def forget_classpath(bug):
    for v in ("b", "f"):
        _cp_cache.pop((bug.name, v), None)


def build_dir(bug, version, tag="suite"):
    return WORK_DIR / "_gen" / f"{bug.name}{version}" / tag


def javac(src_file, out_dir, classpath):
    """Compile one .java file. Returns CompileResult with short messages."""
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    proc = subprocess.run(
        [JAVAC, "-nowarn", "-encoding", "UTF-8", "-Xmaxerrs", "30", "-proc:none",
         "-cp", classpath, "-d", str(out_dir), str(src_file)],
        capture_output=True, text=True, timeout=300,
    )
    raw = proc.stdout + proc.stderr
    errors = [(int(m.group(1)), m.group(2).strip())
              for m in re.finditer(r"\.java:(\d+): error: (.*)", raw)]
    raw = raw.replace(str(src_file), src_file.name)   # short, clear messages for the AI
    return CompileResult(proc.returncode == 0, errors, raw)


def compile_test(bug, version, package, class_name, code, tag="suite") -> CompileResult:
    out = build_dir(bug, version, tag)
    src_file = out / "src" / Path(*package.split(".")) / f"{class_name}.java"
    if (out / "src").exists():
        shutil.rmtree(out / "src")
    src_file.parent.mkdir(parents=True, exist_ok=True)
    src_file.write_text(code, encoding="utf-8")
    # some projects' Defects4J classpath has no JUnit 4: always add it for our tests
    return javac(src_file, out / "classes", project_classpath(bug, version) + ":" + str(JUNIT_JAR))


def run_test(bug, version, package, class_name, timeout=120, tag="suite",
             extra_cp=(), coverage_file=None) -> RunResult:
    """Run a class compiled by compile_test()."""
    _ensure_runner()
    cp = ":".join([*map(str, extra_cp), str(build_dir(bug, version, tag) / "classes"), str(RUNNER_DIR),
                   project_classpath(bug, version), str(JUNIT_JAR)])
    cmd = [JAVA, "-Xmx1g", "-cp", cp]
    if coverage_file is not None:
        cmd.insert(1, f"-javaagent:{JACOCO_AGENT}=destfile={coverage_file},append=false")
    cmd += ["TestRunner", f"{package}.{class_name}"]
    try:
        proc = subprocess.run(cmd, cwd=bug.workdir(version), env=d4j._env(),
                              capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return RunResult(timeout=True, raw="TIMEOUT")
    res = RunResult(raw=proc.stdout + proc.stderr)
    current = None
    for ln in proc.stdout.splitlines():
        if ln.startswith("##PASS "):
            res.passed.append(ln[7:].strip())
            current = None
        elif ln.startswith("##FAIL "):
            name, _, msg = ln[7:].partition(" ")
            res.failed[name] = msg
            current = name
        elif current and ln.startswith("    at "):
            res.failed[current] += "\n" + ln
    return res
