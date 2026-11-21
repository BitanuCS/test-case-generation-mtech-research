"""Baselines: A3Test (2023, PLBART) and its CodeT5 baseline -- trained models that write one
JUnit test per focal method.

  python3 scripts/run_mltests.py bugs/dev40.txt --tool a3test
  python3 scripts/run_mltests.py bugs/dev40.txt --tool codet5

For every class the fix modified, the SAME focal methods as our framework (context.build on
the fixed version) are turned into the Methods2Test "src_fm_fc_ms_ff" input the models were
trained on:  ClassName { <focal method> <constructor and method signatures> <fields> }
(non-private members, 'public' dropped, as in Methods2Test / A3Test's Defects4j inputs).
The model writes one test per focal method (greedy decoding, like the authors' scripts);
A3Test's postProcessingScript.py rules are applied (add @Test, "test" name prefix, cut an
unbalanced test at its last ';' and close it). Each test is placed in its own JUnit 4 class
in the focal class's package, with the focal class's imports (so one broken test cannot
break the others), and "throws Exception" is added when missing (harness, declared).
Then the usual protocol: fix_test_suite.pl (drops tests that do not compile or fail on the
fixed version), run_bug_detection.pl on the buggy version, Defects4J coverage + Major.
Results: results/<tool>/<bug>/report.json (+ <tool>_tests.tar.bz2). Finished bugs are skipped.
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

from our_framework import context, d4j, standard_metrics  # noqa: E402
from our_framework import java_parse as jp  # noqa: E402
from our_framework.config import TOOLS_DIR  # noqa: E402
from our_framework.pipeline import RESULTS_DIR  # noqa: E402

PY = TOOLS_DIR / "toga" / ".venv" / "bin" / "python"     # has torch + transformers
BIN = d4j.D4J_BIN.parent
LABEL = {"a3test": "A3", "codet5": "CT5"}


def flat(s):
    return " ".join(s.split())


def drop_public(sig):
    words = [w for w in flat(sig).split(" ") if w != "public" and not w.startswith("@")]
    return " ".join(words)


def src_fm_fc_ms_ff(cls, focal):
    """Methods2Test input: class name, focal method, signatures, fields."""
    sigs = [drop_public(m.signature) + ";" for m in cls.methods if not m.is_private]
    fields = [drop_public(f.rstrip(";").split("=")[0]) + ";" for f in cls.fields
              if "private" not in f.split("=")[0].split()]
    return f"{cls.name} {{ {flat(focal.source)} {' '.join(sigs)} {' '.join(fields)} }}"


def balanced(text):
    pairs, stack = dict(zip("({[", ")}]")), []
    for ch in text:
        if ch in pairs:
            stack.append(pairs[ch])
        elif ch in ")}]":
            if not stack or ch != stack.pop():
                return False
    return not stack


def post_process(gen):
    """A3Test's postProcessingScript.py, same rules."""
    words = gen.strip().split(" ")
    if len(words) <= 1 or "void" not in words:
        return None
    if words[0] != "@Test":
        words.insert(0, "@Test")
    i = words.index("void")
    if i + 1 < len(words) and not words[i + 1].startswith("test"):
        words[i + 1] = "test" + words[i + 1]
    if not balanced(" ".join(words)):
        last = next((k for k in range(len(words) - 1, -1, -1) if words[k].endswith(";")), None)
        if last is None:
            return None
        words = words[:last + 1] + ["}"]
    return " ".join(words)


def add_throws(test):
    head, brace, rest = test.partition("{")
    if "throws" not in head:
        head = head.rstrip() + " throws Exception "
    return head + brace + rest


def sh(cmd, log, cwd=None, timeout=7200):
    with open(log, "a") as f:
        f.write(f"\n$ {' '.join(map(str, cmd))}\n")
        f.flush()
        try:
            env = d4j._env()
            env.update(HF_HUB_OFFLINE="1", HF_HUB_DISABLE_IMPLICIT_TOKEN="1")   # models are local; no login
            return subprocess.run(list(map(str, cmd)), cwd=cwd, env=env, stdout=f,
                                  stderr=subprocess.STDOUT, timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            f.write(f"\nTIMEOUT after {timeout}s\n")
            return -1


def count_tests(arch):
    """(tests still real, assertions commented out); see run_toga.count_tests."""
    n = flaky = 0
    with tarfile.open(arch) as t:
        for m in t.getmembers():
            if m.name.endswith(".java") and not Path(m.name).name.startswith("._"):
                text = t.extractfile(m).read().decode(errors="replace")
                n += len(re.findall(r"^\s*@Test\b", text, re.M)) - text.count("// Defects4J: flaky method")
                flaky += text.count("// Defects4J: flaky assertion")
    return n, flaky


def one(bug, tool):
    project, bid = bug.rsplit("-", 1)
    out = RESULTS_DIR / tool / bug
    report = {"bug": bug, "setting": tool, "detected": False, "llm_calls": {"fresh": 0, "cached": 0}}
    work = TOOLS_DIR / f"{tool}_out" / bug
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    log = work / "log.txt"
    t = time.time()
    b = d4j.Bug(project, int(bid))
    b.checkout("f")
    b.checkout("b")
    inputs, classes = [], []
    for fqn in [c for c in b.export("classes.modified").split(";") if c]:
        ctx = context.build(b, fqn, "f")
        entry = {"class": fqn, "focal_methods": [m.name for m in ctx.focal_methods]}
        if ctx.skip_reason or not ctx.focal_methods:
            entry["skip_reason"] = ctx.skip_reason or "no focal method"
            classes.append(entry)
            continue
        cls = jp.read_class(ctx.class_src, ctx.class_name)
        for k, m in enumerate(ctx.focal_methods):
            inputs.append({"id": f"{fqn}#{m.name}#{k}", "class": fqn, "package": ctx.package,
                           "class_name": ctx.class_name, "imports": cls.imports,
                           "src_fm_fc_ms_ff": src_fm_fc_ms_ff(cls, m)})
        classes.append(entry)
    report["classes_info"] = classes
    if not inputs:
        report["error"] = "no focal method to test"
        return finish(report, out, work, log, t)
    with open(work / "inputs.jsonl", "w") as f:
        f.writelines(json.dumps(r) + "\n" for r in inputs)
    sh([PY, ROOT / "scripts" / "ml_testgen.py", tool, work / "inputs.jsonl", work / "outputs.jsonl"], log)
    if not (work / "outputs.jsonl").exists():
        report["error"] = "model produced no output (see log)"
        return finish(report, out, work, log, t)
    gens = [json.loads(line) for line in open(work / "outputs.jsonl")]
    src = work / "src"
    written = 0
    for k, g in enumerate(gens):
        test = post_process(g["generated"])
        g["test"] = test
        if test is None:
            continue
        name = f"{g['class_name']}_{LABEL[tool]}{k}_Test"
        body = "\n".join([f"package {g['package']};", "", *g["imports"],
                          "import org.junit.Test;", "import static org.junit.Assert.*;",
                          "import static org.hamcrest.CoreMatchers.*;", "",   # the models also write assertThat(x, is(..))
                          f"public class {name} {{", "", add_throws(test), "}", ""])
        p = src / Path(*g["package"].split(".")) / f"{name}.java"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(body)
        written += 1
    out.mkdir(parents=True, exist_ok=True)
    (out / "generated.jsonl").write_text("".join(json.dumps(g) + "\n" for g in gens))
    report["tests"] = {"focal_methods": len(inputs), "written": written}
    report["gen_seconds"] = round(sum(g.get("gen_seconds", 0) for g in gens), 1)
    if not written:
        report["error"] = "no generated test survived post-processing"
        return finish(report, out, work, log, t)
    suite = work / "suite"
    suite.mkdir()
    arch = suite / f"{project}-{bid}f-{tool}.1.tar.bz2"
    with tarfile.open(arch, "w:bz2") as tar:
        for p in src.iterdir():
            tar.add(p, arcname=p.name)
    shutil.copy(arch, out / f"{tool}_tests.tar.bz2")
    sh([BIN.parent / "util" / "fix_test_suite.pl", "-p", project, "-d", suite, "-v", f"{bid}f"], log)
    kept, flaky = count_tests(arch) if arch.exists() else (0, 0)
    report["tests"].update(kept_on_fixed=kept, removed_on_fixed=written - kept, assertions_commented_out=flaky)
    if arch.exists():
        shutil.copy(arch, out / f"{tool}_tests_kept.tar.bz2")    # the suite after fix_test_suite
    if not kept:
        report["error"] = "no generated test compiles and passes on the fixed version"
        return finish(report, out, work, log, t)
    sh([BIN / "run_bug_detection.pl", "-p", project, "-d", suite, "-o", work / "detect"], log)
    det = work / "detect" / "bug_detection"
    if det.exists():
        rows = list(csv.DictReader(det.open()))
        report["detection_rows"] = rows
        report["detected"] = any((r.get("num_trigger") or "0").isdigit() and int(r["num_trigger"]) > 0 for r in rows)
    else:
        report["error"] = "bug detection did not run (see log)"
    report["standard_metrics"] = standard_metrics.measure_archive(project, suite, work / "std")
    return finish(report, out, work, log, t)


def finish(report, out, work, log, t):
    report["seconds"] = round(time.time() - t, 1)
    report["classes"] = [{"status": "ok" if "error" not in report else "error", "detected": report["detected"]}]
    out.mkdir(parents=True, exist_ok=True)
    (out / "report.json").write_text(json.dumps(report, indent=2))
    if log.exists():
        shutil.copy(log, out / "log.txt")
    shutil.rmtree(work, ignore_errors=True)
    return report


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("bug_file")
    ap.add_argument("--tool", choices=["a3test", "codet5"], required=True)
    args = ap.parse_args()
    bugs = Path(args.bug_file).read_text().split()
    todo = [b for b in bugs if not (RESULTS_DIR / args.tool / b / "report.json").exists()]
    print(f"{len(bugs)} bugs, {len(todo)} to go  [{args.tool}]", flush=True)
    for b in todo:
        r = one(b, args.tool)
        print(f"{b}: {'DETECTED' if r['detected'] else 'missed'} ({r['seconds']}s) {r.get('tests', '')}"
              + (f"  [{r['error']}]" if "error" in r else ""), flush=True)
