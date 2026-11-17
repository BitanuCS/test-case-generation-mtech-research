"""Thin wrapper around the `defects4j` command-line tool.

A Defects4J bug is named like "Lang-1": project Lang, bug 1.
Each bug has two versions:
  - buggy ("1b"): the code with the real bug
  - fixed ("1f"): the code after the developer fixed it
A good test PASSES on the fixed version and FAILS on the buggy version.
"""
import os
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

from .config import TOOLS_DIR, WORK_DIR

D4J_BIN = TOOLS_DIR / "defects4j" / "framework" / "bin" / "defects4j"


def _env():
    env = os.environ.copy()
    java_home = "/opt/homebrew/opt/openjdk@11"
    env["JAVA_HOME"] = java_home
    env["PATH"] = f"{java_home}/bin:{D4J_BIN.parent}:{env['PATH']}"
    perl_lib = str(TOOLS_DIR / "perl5" / "lib" / "perl5")
    env["PERL5LIB"] = perl_lib + (":" + env["PERL5LIB"] if env.get("PERL5LIB") else "")
    env["TZ"] = "America/Los_Angeles"
    return env


@dataclass
class Result:
    ok: bool
    stdout: str   # the actual answer (e.g. property value, failing tests)
    stderr: str   # progress messages ("Running ant ... OK") and errors

    @property
    def output(self):
        return self.stdout + self.stderr


def run(args, cwd=None, timeout=1800):
    """Run `defects4j <args>`."""
    proc = subprocess.run(
        [str(D4J_BIN), *args],
        cwd=cwd,
        env=_env(),
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    return Result(proc.returncode == 0, proc.stdout, proc.stderr)


@dataclass
class Bug:
    project: str   # e.g. "Lang"
    bug_id: int    # e.g. 1

    @property
    def name(self):
        return f"{self.project}-{self.bug_id}"

    def workdir(self, version):
        """version is "b" (buggy) or "f" (fixed)."""
        return WORK_DIR / f"{self.name}{version}"

    def checkout(self, version, fresh=False):
        wd = self.workdir(version)
        if wd.exists():
            # a checkout interrupted halfway has no .defects4j.config: redo it
            if not fresh and (wd / ".defects4j.config").exists():
                return wd
            shutil.rmtree(wd)
        wd.parent.mkdir(parents=True, exist_ok=True)
        res = run(["checkout", "-p", self.project, "-v", f"{self.bug_id}{version}", "-w", str(wd)])
        if not res.ok:
            raise RuntimeError(f"checkout {self.name}{version} failed:\n{res.output}")
        return wd

    def export(self, prop, version="f"):
        """Read a Defects4J property, e.g. "classes.modified", "dir.src.classes".

        Multi-valued properties (like tests.trigger) come back one per line.
        """
        res = run(["export", "-p", prop], cwd=self.workdir(version))
        if not res.ok:
            raise RuntimeError(f"export {prop} failed:\n{res.output}")
        return res.stdout.strip()

    def compile(self, version="f"):
        return run(["compile"], cwd=self.workdir(version))

    def test(self, version="f", test_name=None):
        """Run tests. test_name like "org.foo.BarTest::testX" or "org.foo.BarTest"."""
        args = ["test"] + (["-t", test_name] if test_name else [])
        return run(args, cwd=self.workdir(version))


def failing_tests(output):
    """Parse the 'Failing tests: N' block printed by `defects4j test`."""
    return [ln.strip()[2:] for ln in output.splitlines() if ln.strip().startswith("- ")]
