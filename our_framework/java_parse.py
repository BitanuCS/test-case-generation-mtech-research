"""Small helpers for reading Java source code (built on tree-sitter).

Line numbers here are 1-based, like in an editor.
"""
from dataclasses import dataclass, field

import tree_sitter_java as tsj
from tree_sitter import Language, Parser

_parser = Parser(Language(tsj.language()))

TYPE_DECLS = ("class_declaration", "interface_declaration", "enum_declaration")
MEMBER_DECLS = ("method_declaration", "constructor_declaration")


def parse(src: str):
    return _parser.parse(src.encode())


def _text(node):
    return node.text.decode()


def _javadoc(node):
    """The /** ... */ comment right before a declaration, if any."""
    prev = node.prev_sibling
    if prev is not None and prev.type == "block_comment" and _text(prev).startswith("/**"):
        return _text(prev)
    return ""


@dataclass
class Method:
    name: str
    kind: str            # "method" or "constructor"
    signature: str       # modifiers + return type + name + params (+ throws)
    source: str          # full code, without Javadoc
    javadoc: str
    start_line: int
    end_line: int
    is_private: bool
    calls: list = field(default_factory=list)  # names of methods it calls
    node: object = None

    def full_source(self):
        return (self.javadoc + "\n" if self.javadoc else "") + self.source


def _signature(node):
    body = node.child_by_field_name("body")
    end = body.start_byte if body is not None else node.end_byte
    sig = node.text[: end - node.start_byte].decode()
    return " ".join(sig.split()).rstrip(" ;")


def _calls(node):
    """Names of same-class methods called inside `node` (unqualified or this./Class.)."""
    out, stack = [], [node]
    while stack:
        n = stack.pop()
        if n.type == "method_invocation":
            obj = n.child_by_field_name("object")
            name = n.child_by_field_name("name")
            if name is not None and (obj is None or _text(obj) == "this" or _text(obj)[:1].isupper()):
                out.append((_text(obj) if obj is not None else None, _text(name)))
        stack.extend(n.children)
    return out


def _to_method(node):
    mods = next((c for c in node.children if c.type == "modifiers"), None)
    return Method(
        name=_text(node.child_by_field_name("name")),
        kind="constructor" if node.type == "constructor_declaration" else "method",
        signature=_signature(node),
        source=_text(node),
        javadoc=_javadoc(node),
        start_line=node.start_point[0] + 1,
        end_line=node.end_point[0] + 1,
        is_private=mods is not None and "private" in _text(mods).split(),
        calls=_calls(node),
        node=node,
    )


@dataclass
class JavaClass:
    name: str
    package: str
    imports: list
    declaration: str     # e.g. "public class NumberUtils"
    javadoc: str
    fields: list         # field declaration source lines
    methods: list        # list[Method], includes constructors
    src: str


def read_class(src: str, class_name: str = None) -> JavaClass:
    """Parse the top-level type (or the one named class_name) in a Java file."""
    root = parse(src).root_node
    package, imports, decl = "", [], None
    for c in root.children:
        if c.type == "package_declaration":
            package = _text(c).replace("package", "", 1).strip(" ;")
        elif c.type == "import_declaration":
            imports.append(_text(c))
        elif c.type in TYPE_DECLS:
            name = _text(c.child_by_field_name("name"))
            if decl is None or name == class_name:
                decl = c
    if decl is None:
        raise ValueError("no class found")

    body = decl.child_by_field_name("body")
    fields, methods = [], []
    for c in body.children:
        if c.type == "field_declaration":
            fields.append(" ".join(_text(c).split()))
        elif c.type in MEMBER_DECLS:
            methods.append(_to_method(c))

    return JavaClass(
        name=_text(decl.child_by_field_name("name")),
        package=package,
        imports=imports,
        declaration=" ".join(decl.text[: body.start_byte - decl.start_byte].decode().split()),
        javadoc=_javadoc(decl),
        fields=fields,
        methods=methods,
        src=src,
    )


def methods_at_lines(src: str, lines):
    """All methods/constructors (any nesting level) that contain any of `lines`."""
    lines = set(lines)
    found, stack = [], [parse(src).root_node]
    while stack:
        n = stack.pop()
        if n.type in MEMBER_DECLS:
            if any(n.start_point[0] + 1 <= ln <= n.end_point[0] + 1 for ln in lines):
                found.append(_to_method(n))
                continue
        stack.extend(n.children)
    return sorted(found, key=lambda m: m.start_line)


def test_method_names(src: str):
    """Names of methods annotated with @Test (JUnit 4) or named test* (JUnit 3)."""
    names = []
    try:
        methods = read_class(src).methods
    except ValueError:          # not a parsable class (e.g. cut-off AI answer)
        return names
    for m in methods:
        mods = next((c for c in m.node.children if c.type == "modifiers"), None)
        annotated = mods is not None and "@Test" in _text(mods)
        if annotated or (m.kind == "method" and m.name.startswith("test")):
            names.append(m.name)
    return names


def method_at_line(src: str, line: int):
    ms = methods_at_lines(src, [line])
    return ms[-1].name if ms else None


def remove_methods(src: str, names) -> str:
    """Delete the given methods (and their Javadoc) from the source."""
    names = set(names)
    cls = read_class(src)
    spans = []
    for m in cls.methods:
        if m.name in names:
            start = m.node.start_byte
            prev = m.node.prev_sibling
            if prev is not None and prev.type in ("block_comment", "line_comment"):
                start = prev.start_byte
            spans.append((start, m.node.end_byte))
    data = src.encode()
    for s, e in sorted(spans, reverse=True):
        data = data[:s] + data[e:]
    return data.decode()


def method_source(src: str, name: str) -> str:
    try:
        for m in read_class(src).methods:
            if m.name == name:
                return m.source
    except ValueError:
        pass
    return ""


def merge_methods(base: str, extra: str) -> str:
    """Add the methods (and missing imports) of class `extra` into class `base`.

    Methods whose name already exists in base get a numeric suffix.
    """
    try:
        new_cls = read_class(extra)
        base_cls = read_class(base)
    except ValueError:
        return base
    existing = {m.name for m in base_cls.methods}
    chunks = []
    for m in new_cls.methods:
        if m.kind != "method":
            continue
        code = (m.javadoc + "\n" if m.javadoc else "") + m.source
        name = m.name
        if name in existing:
            i = 2
            while f"{name}{i}" in existing:
                i += 1
            code = code.replace(f" {name}(", f" {name}{i}(", 1)
            name = f"{name}{i}"
        existing.add(name)
        chunks.append("\n    " + code.replace("\n", "\n    ") + "\n")
    if not chunks:
        return base
    end = base.rstrip().rfind("}")
    merged = base[:end] + "".join(chunks) + base[end:]
    missing = [imp for imp in new_cls.imports if imp not in base_cls.imports]
    if missing:
        pos = merged.find("\n", merged.find("package ")) + 1
        merged = merged[:pos] + "\n" + "\n".join(missing) + "\n" + merged[pos:]
    return merged
