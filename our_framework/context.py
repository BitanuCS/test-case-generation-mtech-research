"""Context Agent (no AI needed): collects the information the AI needs
about the code under test.

For a bug we test the class(es) the developer's fix changed ("focal class")
and, inside it, the methods the fix touched ("focal methods").

What the AI gets to see:
  - package, imports, class declaration and Javadoc of the focal class
  - fields and constructor signatures (how to create objects)
  - signatures of the other non-private methods in the class
  - full code + Javadoc of each focal method
  - full code of short same-class helper methods the focal method calls
  - the *setup part* of the existing developer test class (imports,
    fields, setUp) for style -- NEVER its test methods, because those
    include the test that reveals the bug (that would be cheating).
"""
import difflib
import re
from dataclasses import dataclass, field
from pathlib import Path

from . import java_parse as jp

MAX_HELPER_LINES = 40
MAX_CONTEXT_CHARS = 45000   # keep prompts within free-tier limits


@dataclass
class FocalContext:
    bug: str
    class_fqn: str            # e.g. org.apache.commons.lang3.math.NumberUtils
    package: str
    class_name: str
    junit: int                # 3 or 4
    focal_methods: list       # list[jp.Method]
    text: str                 # the context, rendered for the AI
    class_src: str = ""       # full source of the class under test
    focal_text: str = ""      # short version: only the method(s) under test (for the panel)
    existing_test_names: list = field(default_factory=list)
    skip_reason: str = ""     # set when there is nothing sensible to test


def _changed_lines(this_src: str, other_src: str):
    """Line numbers (1-based, in this_src) that differ between the two versions."""
    sm = difflib.SequenceMatcher(None, other_src.splitlines(), this_src.splitlines(), autojunk=False)
    lines = set()
    for tag, _i1, _i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue
        if j2 > j1:
            lines.update(range(j1 + 1, j2 + 1))
        else:                       # pure deletion: blame the neighbouring line
            lines.add(max(j1, 1))
    return sorted(lines)


def _test_setup_only(test_src: str):
    """Existing test class with all its test methods (and license header) removed."""
    src = jp.remove_methods(test_src, jp.test_method_names(test_src))
    pkg = src.find("package ")
    if pkg > 0:
        src = src[pkg:]
    out, blank = [], False
    for ln in src.splitlines():
        if not ln.strip() or ln.strip().startswith("//"):
            if not blank:
                out.append("")
            blank = True
        else:
            out.append(ln.rstrip())
            blank = False
    return "\n".join(out)


def _junit_version(test_src: str):
    return 4 if "org.junit.Test" in test_src or "org.junit.*" in test_src else 3


def build(bug, class_fqn: str, version: str = "f", full: bool = True) -> FocalContext:
    """Context for testing class_fqn as it is in `version` ("f" fixed or "b" buggy).

    bug: d4j.Bug with both versions checked out (the other version is only
    used to find WHICH methods to test, never shown to the AI).
    full=False gives a minimal context (only the method under test) -- used
    to measure how much the Context Agent helps.
    """
    other = "b" if version == "f" else "f"
    src_dir = bug.export("dir.src.classes", version)
    test_dir = bug.export("dir.src.tests", version)
    rel = Path(*class_fqn.split(".")).with_suffix(".java")
    simple = class_fqn.rsplit(".", 1)[-1]
    this_path, other_path = bug.workdir(version) / src_dir / rel, bug.workdir(other) / src_dir / rel
    if not (this_path.exists() and other_path.exists()):
        # The fix added (or removed) this whole class. Tests for it cannot even
        # compile on the other version, so they can never reveal the bug.
        missing = "buggy" if not (bug.workdir("b") / src_dir / rel).exists() else "fixed"
        return FocalContext(bug=bug.name, class_fqn=class_fqn, package=class_fqn.rsplit(".", 1)[0],
                            class_name=simple, junit=4, focal_methods=[], text="",
                            skip_reason=f"class does not exist in the {missing} version")
    this_src = this_path.read_text(encoding="utf-8", errors="replace")
    other_src = other_path.read_text(encoding="utf-8", errors="replace")

    cls = jp.read_class(this_src, simple)
    all_methods = jp.methods_at_lines(this_src, range(1, this_src.count("\n") + 2))
    changed = _changed_lines(this_src, other_src)
    focal = jp.methods_at_lines(this_src, changed)
    if not focal:
        # e.g. the fix only added code: use the same-named methods of the other version
        names = {m.name for m in jp.methods_at_lines(other_src, _changed_lines(other_src, this_src))}
        focal = [m for m in all_methods if m.name in names]
    if not focal:
        # the fix changed only a field (e.g. `new HashSet()` -> `new LinkedHashSet()`):
        # test the methods that use that field
        lines = this_src.splitlines()
        fields = set()
        for ln in changed:
            m = re.search(r"(\w+)\s*(=|;)", lines[ln - 1]) if ln <= len(lines) else None
            if m and not lines[ln - 1].lstrip().startswith(("import", "package", "//", "*", "/*")):
                fields.add(m.group(1))
        focal = [m for m in all_methods
                 if any(re.search(rf"\b{re.escape(f)}\b", m.source) for f in fields)][:4]
    header_note = ""
    if not focal:
        # the fix changed only the class header (e.g. `implements Serializable`):
        # test the public methods, and what the header promises
        lines = this_src.splitlines()
        start = next((i + 1 for i, ln in enumerate(lines)
                      if re.search(rf"\b(class|interface|enum)\s+{re.escape(simple)}\b", ln)), None)
        if start:
            end = next((i + 1 for i in range(start - 1, len(lines)) if "{" in lines[i]), start)
            if any(start <= ln <= end + 3 for ln in changed):
                focal = [m for m in all_methods if not m.is_private][:6]
                header_note = ("  // NOTE: also test what the class declaration above promises "
                               "(every interface it implements or class it extends, e.g. Serializable "
                               "objects must survive serialization).")
    if not focal:
        return FocalContext(bug=bug.name, class_fqn=class_fqn, package=cls.package, class_name=simple,
                            junit=4, focal_methods=[], text="", class_src=this_src,
                            skip_reason="the fix changed no method or field (e.g. only imports or comments)")
    by_name = {}
    for m in cls.methods:
        by_name.setdefault(m.name, []).append(m)

    # Same-class helpers called by the focal methods.
    focal_names = {m.name for m in focal}
    helpers = []
    for fm in focal:
        for obj, name in fm.calls:
            if obj not in (None, "this", simple) or name in focal_names:
                continue
            for h in by_name.get(name, []):
                if h not in helpers and (h.end_line - h.start_line) < MAX_HELPER_LINES:
                    helpers.append(h)

    # Existing developer test class (setup part only).
    test_path = bug.workdir(version) / test_dir / rel.with_name(simple + "Test.java")
    test_src = test_path.read_text(encoding="utf-8", errors="replace") if test_path.exists() else ""
    junit = _junit_version(test_src) if test_src else 4

    focal_text = "\n".join([
        f"// Class under test: {class_fqn}",
        cls.declaration + " {",
        *[_indent(m.full_source()) for m in focal],
        "}",
    ])
    if not full:
        return FocalContext(bug=bug.name, class_fqn=class_fqn, package=cls.package, class_name=simple,
                            junit=junit, focal_methods=focal,
                            text=f"package {cls.package};\n" + "\n".join(cls.imports) + "\n\n" + focal_text,
                            class_src=this_src, focal_text=focal_text)

    signatures = [f"  {m.signature};" for m in cls.methods
                  if not m.is_private and m.name not in focal_names and m not in helpers]
    parts = [
        f"// ===== Class under test: {class_fqn} =====",
        f"package {cls.package};",
        *cls.imports,
        "",
        cls.javadoc,
        cls.declaration + " {",
        "",
        "  // --- fields ---",
        *[f"  {f}" for f in cls.fields],
        "",
        "  // --- constructors and other methods (signatures only) ---",
        *signatures,
        "",
    ]
    if helpers:
        parts.append("  // --- helper methods called by the method(s) under test ---")
        parts += [_indent(h.full_source()) for h in helpers] + [""]
    if header_note:
        parts.append(header_note)
    parts.append("  // --- METHOD(S) UNDER TEST ---")
    parts += [_indent(m.full_source()) for m in focal]
    parts.append("}")
    if test_src:
        parts += [
            "",
            f"// ===== Existing test class {simple}Test (test methods removed; style/setup reference only) =====",
            _test_setup_only(test_src).strip(),
        ]

    text = "\n".join(p for p in parts if p is not None)
    if len(text) > MAX_CONTEXT_CHARS:      # very big class: drop the least useful parts first
        text = text.replace("\n".join(signatures), "  // (other method signatures omitted: class too big)")
    if len(text) > MAX_CONTEXT_CHARS:
        text = text[:MAX_CONTEXT_CHARS // 3] + "\n  // ... (cut) ...\n" + text[-(2 * MAX_CONTEXT_CHARS // 3):]

    return FocalContext(
        bug=bug.name,
        class_fqn=class_fqn,
        package=cls.package,
        class_name=simple,
        junit=junit,
        focal_methods=focal,
        text=text,
        class_src=this_src,
        focal_text=focal_text,
        existing_test_names=jp.test_method_names(test_src) if test_src else [],
    )


def _indent(code, n=2):
    pad = " " * n
    return "\n".join(pad + ln if ln.strip() else ln for ln in code.splitlines())
