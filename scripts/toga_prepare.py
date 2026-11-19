"""Make TOGA's model inputs from EvoSuite tests (run with TOGA's own Python, which has javalang).

  <toga>/.venv/bin/python scripts/toga_prepare.py <evosuite_src_dir> <focal_src_dir> <project> <bug> <out_dir>

Same steps as TOGA's eval/extract_tests.py (github.com/microsoft/toga, MIT licence), adapted to
one bug at a time: every EvoSuite test is split at each assertion (one test prefix per oracle),
and the focal method is found with TOGA's own heuristic (the last method of the class under
test called in the prefix). The functions below are copied from that file with small fixes
(split_test used a global variable). Writes inputs.csv and meta.csv as TOGA expects.
"""
import csv
import os
import re
import sys
from copy import copy

import javalang

assert_re = re.compile(r"assert\w*\(.*\)")
test_name_re = re.compile(r"public void (test[0-9]*)\(\)")
extract_package_re = re.compile(r'package\s+(\S+);')
fail_catch_re = re.compile(r"fail\(.*\).*}\s*catch", re.MULTILINE | re.DOTALL)


def get_method_txt(lines, start_line):
    method_def, depth, collected, in_string, escape = '', 0, False, False, False
    line_nums = []
    for i in range(start_line, len(lines)):
        prev_char = ''
        line = lines[i]
        for col, char in enumerate(line):
            next_char = line[col + 1] if col + 1 < len(line) else ''
            if escape:
                escape = False
            elif char == '\\':
                escape = True
            elif char == '/' and prev_char == '/' and not in_string:
                prev_char = ''
                break
            elif not in_string and prev_char == "'" and next_char == "'":
                pass
            elif char == "\"":
                in_string = not in_string
            elif char == '{' and not in_string:
                depth += 1
            elif char == '}' and not in_string:
                depth -= 1
                if depth == 0:
                    method_def += line[:col + 1]
                    line_nums += [i + 1]
                    collected = True
                    break
            prev_char = char
        if collected:
            break
        method_def += line
        line_nums += [i + 1]
    return method_def, line_nums


def get_class_dec(path):
    text = open(path, errors="replace").read()
    lines = open(path, errors="replace").readlines()
    return javalang.parse.parse(text).types[0], lines


def get_classes_with_inherited(full_class_path, src_path):
    ret = []
    while full_class_path and os.path.exists(full_class_path):
        try:
            class_dec, class_lines = get_class_dec(full_class_path)
        except Exception:
            break
        ret.append((class_dec, class_lines))
        full_class_path = None
        imports = {}
        for line in class_lines:
            if line.strip().startswith('import'):
                imported = line.strip().strip(';').split()[-1]
                imports[imported.split('.')[-1]] = imported
        if getattr(class_dec, 'extends', None) and getattr(class_dec.extends, 'name', None):
            ext = class_dec.extends.name
            if ext in imports:
                full_class_path = src_path + '/' + imports[ext].replace('.', '/') + '.java'
    return ret


def extract_all_methods(class_dec, class_lines):
    methods = []
    for m in list(class_dec.constructors) + list(class_dec.methods):
        text, line_nums = get_method_txt(class_lines, m.position.line - 1)
        if text.count("@Test") > 1:
            continue
        methods.append((m, text, line_nums, m.documentation))
    return methods


def extract_focal_methods(tests, all_focal_class_methods):
    focal = []
    for test_txt in tests:
        try:
            parser = javalang.parser.Parser(javalang.tokenizer.tokenize(test_txt))
            test_obj = parser.parse_member_declaration()
        except Exception:
            focal.append(('', ''))
            continue
        fm_names = []
        for _, n in reversed([n for n in test_obj]):
            if isinstance(n, javalang.tree.MethodInvocation):
                if n.member in ('fail', 'verifyException') or 'assert' in n.member:
                    continue
                fm_names.append(n.member)
            if isinstance(n, javalang.tree.ClassCreator):
                fm_names.append(n.type.name)
        found = ('', '')
        for name in fm_names:
            for methods in all_focal_class_methods:
                hit = next(((t, d or '') for dec, t, _, d in methods if dec.name == name), None)
                if hit:
                    found = hit
                    break
            if found[0]:
                break
        focal.append(found)
    return focal


def split_test(test_method, line_nums):
    split_tests, split_lines, rel, rel_nums = [], [], [], []
    for line, no in zip(test_method.split('\n'), line_nums):
        if not line.strip():
            continue
        if 'assert' in line:
            split_tests.append('\n'.join(rel + [line, '}']))
            split_lines.append(copy(rel_nums + [no]))
        else:
            rel.append(line)
            rel_nums.append(no)
    split_tests.append('\n'.join(rel))
    split_lines.append(rel_nums)
    return split_tests, split_lines


def main(evo_dir, src_dir, project, bug, out_dir):
    inputs, meta = [], []
    for root, _, files in os.walk(evo_dir):
        for f in sorted(files):
            if not f.endswith("_ESTest.java") or f.startswith("._"):
                continue
            test_file = os.path.join(root, f)
            class_dec, class_lines = get_class_dec(test_file)
            package = next((m[1] for line in class_lines if (m := extract_package_re.match(line.strip()))), '')
            cut = f[:-len("_ESTest.java")]
            focal_path = os.path.join(src_dir, package.replace('.', '/'), cut + ".java")
            pairs = get_classes_with_inherited(focal_path, src_dir)
            focal_class_methods = [extract_all_methods(d, t) for d, t in pairs]
            split_methods = []
            for _, test_method, line_nums, _ in extract_all_methods(class_dec, class_lines):
                if not test_name_re.search(test_method):
                    continue
                s, _ = split_test(test_method, line_nums)
                split_methods += s
            focal = extract_focal_methods(split_methods, focal_class_methods)
            for test_method, (fm, doc) in zip(split_methods, focal):
                m = assert_re.search(test_method)
                name = test_name_re.search(test_method).group(1)
                inputs.append((fm, test_method, doc))
                meta.append((project, bug, f"{package}.{cut}_ESTest::{name}", 0, 0,
                             bool(fail_catch_re.search(test_method)), m[0] if m else '', ''))
    os.makedirs(out_dir, exist_ok=True)
    with open(f"{out_dir}/inputs.csv", "w") as f1, open(f"{out_dir}/meta.csv", "w") as f2:
        w1, w2 = csv.writer(f1), csv.writer(f2)
        w1.writerow(['focal_method', 'test_prefix', 'docstring'])
        w2.writerow('project,bug_num,test_name,exception_bug,assertion_bug,exception_lbl,assertion_lbl,assert_err'.split(','))
        w1.writerows(inputs)
        w2.writerows(meta)
    print(f"{len(inputs)} test prefixes, {sum(1 for i in inputs if i[0])} with a focal method")


if __name__ == "__main__":
    main(*sys.argv[1:6])
