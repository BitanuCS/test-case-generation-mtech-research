"""Fake bugs ("mutants") for the method(s) under test, and running tests on them.

Two kinds of mutants:
  - rule-based: small mechanical changes, like the PIT tool makes
    (< becomes <=, + becomes -, && becomes ||, 5 becomes 6, ! removed, ...)
  - AI-written: realistic bugs a developer could make (agents.write_mutants)

A mutant is "killed" when at least one of our (normally passing) tests fails
on it. Mutants that survive show where the tests are weak.
"""
import random
import re
from dataclasses import dataclass, field

from . import runner
from . import java_parse as jp

ROR = {"<": ["<=", ">="], "<=": ["<", ">"], ">": [">=", "<="], ">=": [">", "<"],
       "==": ["!="], "!=": ["=="]}
AOR = {"+": ["-"], "-": ["+"], "*": ["/"], "/": ["*"], "%": ["*"]}
LCR = {"&&": ["||"], "||": ["&&"]}


@dataclass
class Mutant:
    mid: str
    kind: str            # "rule" or "ai"
    start: int           # byte offsets in the class source
    end: int
    replacement: str
    description: str
    line: int
    status: str = "new"  # new | stillborn | killed | survived
    killed_by: list = field(default_factory=list)

    def apply(self, src: str) -> str:
        data = src.encode()
        return (data[: self.start] + self.replacement.encode() + data[self.end:]).decode()

    def original(self, src: str) -> str:
        return src.encode()[self.start: self.end].decode()


def _focal_nodes(class_src, focal_methods):
    lines = {m.start_line for m in focal_methods}
    root = jp.parse(class_src).root_node
    found, stack = [], [root]
    while stack:
        n = stack.pop()
        if n.type in jp.MEMBER_DECLS and n.start_point[0] + 1 in lines:
            found.append(n)
            continue
        stack.extend(n.children)
    return found


def rule_mutants(class_src, focal_methods, limit=25, seed="x"):
    out = []
    for method in _focal_nodes(class_src, focal_methods):
        stack = [method]
        while stack:
            n = stack.pop()
            stack.extend(n.children)
            line = n.start_point[0] + 1
            if n.type == "binary_expression":
                op = n.child_by_field_name("operator")
                if op is None:
                    continue
                for table, kind in ((ROR, "relational"), (AOR, "arithmetic"), (LCR, "logical")):
                    for rep in table.get(op.text.decode(), []):
                        out.append((op.start_byte, op.end_byte, rep,
                                    f"{kind}: '{op.text.decode()}' -> '{rep}'", line))
            elif n.type == "decimal_integer_literal":
                txt = n.text.decode().rstrip("lL")
                if txt.isdigit():
                    suffix = n.text.decode()[len(txt):]
                    rep = str(int(txt) + 1) + suffix
                    out.append((n.start_byte, n.end_byte, rep, f"constant: {n.text.decode()} -> {rep}", line))
            elif n.type in ("true", "false"):
                rep = "false" if n.type == "true" else "true"
                out.append((n.start_byte, n.end_byte, rep, f"boolean: {n.type} -> {rep}", line))
            elif n.type == "unary_expression":
                op = n.child_by_field_name("operator")
                operand = n.child_by_field_name("operand")
                if op is not None and op.text == b"!" and operand is not None:
                    out.append((n.start_byte, n.end_byte, operand.text.decode(),
                                "negation removed: '!' deleted", line))
    rng = random.Random(seed)
    rng.shuffle(out)
    out = sorted(out[:limit], key=lambda t: t[0])
    return [Mutant(f"R{i}", "rule", s, e, rep, desc, ln) for i, (s, e, rep, desc, ln) in enumerate(out)]


def ai_mutants(class_src, focal_methods, proposals):
    """proposals: [{"description", "original", "mutated"}] from agents.write_mutants.

    A proposal is used only if its "original" snippet appears exactly once
    inside one focal method.
    """
    out = []
    data = class_src.encode()
    for node in _focal_nodes(class_src, focal_methods):
        body = data[node.start_byte: node.end_byte].decode()
        for p in proposals:
            orig = (p.get("original") or "").strip()
            mutated = (p.get("mutated") or "").strip()
            if not orig or not mutated or " ".join(orig.split()) == " ".join(mutated.split()):
                continue
            # The AI often changes indentation when copying: match ignoring whitespace.
            pattern = r"\s+".join(map(re.escape, orig.split()))
            hits = list(re.finditer(pattern, body))
            if len(hits) != 1:
                continue
            start = node.start_byte + len(body[: hits[0].start()].encode())
            end = node.start_byte + len(body[: hits[0].end()].encode())
            line = data[:start].count(b"\n") + 1
            out.append(Mutant(f"A{len(out)}", "ai", start, end, mutated,
                              p.get("description", "AI mutant"), line))
    return out


def run_mutants(bug, version, class_fqn, class_src, mutants, package, test_class, tests_expected_to_pass):
    """Compile each mutant of the focal class and run our test class on it."""
    rel = class_fqn.replace(".", "/") + ".java"
    cp = runner.project_classpath(bug, version)
    base = runner.build_dir(bug, version, "mutant")
    expected = set(tests_expected_to_pass)
    for m in mutants:
        src_file = base / "src" / rel
        src_file.parent.mkdir(parents=True, exist_ok=True)
        src_file.write_text(m.apply(class_src), encoding="utf-8")
        out = base / "classes"
        comp = runner.javac(src_file, out, cp)
        if not comp.ok:
            m.status = "stillborn"
            continue
        res = runner.run_test(bug, version, package, test_class, timeout=60, extra_cp=[out])
        if res.timeout:
            m.status, m.killed_by = "killed", ["<timeout>"]
            continue
        killers = sorted(set(res.failed) & expected) or sorted(expected - set(res.passed) - set(res.failed))
        m.status = "killed" if killers else "survived"
        m.killed_by = killers
    return mutants


def score(mutants):
    valid = [m for m in mutants if m.status in ("killed", "survived")]
    killed = sum(m.status == "killed" for m in valid)
    return {"total": len(mutants), "valid": len(valid), "killed": killed,
            "score": round(killed / len(valid), 3) if valid else None}


def describe(m, class_src, context_lines=2):
    """A short before/after view of a mutant, for the AI."""
    lines = class_src.splitlines()
    lo, hi = max(0, m.line - 1 - context_lines), min(len(lines), m.line + context_lines)
    before = "\n".join(lines[lo:hi])
    after = "\n".join(m.apply(class_src).splitlines()[lo:hi])
    return f"[{m.mid}] {m.description} (line {m.line})\n--- original\n{before}\n--- mutant\n{after}"
