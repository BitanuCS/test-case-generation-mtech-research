"""Make the results table for all settings (and EvoSuite, if run).

  python3 scripts/summarize.py                 # all bugs that have results
  python3 scripts/summarize.py bugs/pilot.txt  # only these bugs

Writes results/summary.csv (one row per bug and setting) and
results/summary.md (one row per setting), and prints the table.

When comparing settings, only bugs that EVERY setting finished are counted,
so the comparison is fair.
"""
import csv
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework.pipeline import RESULTS_DIR  # noqa: E402


def _mean(xs):
    xs = [x for x in xs if x is not None]
    return round(sum(xs) / len(xs), 3) if xs else None


def load_rows(only=None):
    rows = []
    for rep in sorted(RESULTS_DIR.glob("*/*/report.json")):
        setting = rep.parent.parent.name
        if setting.startswith("_"):
            continue
        r = json.loads(rep.read_text())
        if r.get("incomplete") or (only and r["bug"] not in only):
            continue
        classes = [c for c in r.get("classes", []) if c.get("status") == "ok"]
        cov_line = [((c.get("coverage") or {}).get("focal") or {}).get("line") for c in classes]
        cov_branch = [((c.get("coverage") or {}).get("focal") or {}).get("branch") for c in classes]
        mut = [((c.get("mutation") or [{}])[-1] or {}).get("rule", {}).get("score") for c in classes]
        rows.append({
            "setting": setting,
            "bug": r["bug"],
            "detected": int(bool(r.get("detected"))),
            "status": ",".join(c.get("status", "?") for c in r.get("classes", [])),
            "generated_tests": sum(c.get("generated_tests", 0) for c in r.get("classes", [])),
            "first_compiles": _mean([int(c["first_compiles"]) for c in r.get("classes", [])
                                     if "first_compiles" in c]),
            "final_tests": sum(c.get("final_tests", 0) for c in classes),
            "false_alarms": sum(len(c.get("false_alarms", [])) for c in classes),
            "bug_encoding_tests": sum(len(c.get("bug_encoding_tests", [])) for c in classes),
            "line_cov": _mean(cov_line),
            "branch_cov": _mean(cov_branch),
            "mutation_score": _mean(mut),
            "ai_calls": r.get("llm_calls", {}).get("fresh", 0) + r.get("llm_calls", {}).get("cached", 0),
            "tokens": (r.get("llm_calls", {}).get("tokens_in", 0) + r.get("llm_calls", {}).get("tokens_out", 0))
                      if "tokens_in" in r.get("llm_calls", {}) else None,
            "seconds": r.get("seconds"),
            # standard Defects4J measurements (same tools for every framework)
            "d4j_line_cov": ((r.get("standard_metrics") or {}).get("coverage") or {}).get("line"),
            "d4j_branch_cov": ((r.get("standard_metrics") or {}).get("coverage") or {}).get("branch"),
            "major_mutation": ((r.get("standard_metrics") or {}).get("major_mutation") or {}).get("score"),
            # PIT (as CANDOR / TestAgent / MutGen)
            "pit_class": _mean([((c.get("pit") or {}).get("class") or {}).get("score") for c in classes]),
            "pit_method": _mean([((c.get("pit") or {}).get("focal") or {}).get("score") for c in classes]),
            # quality of the Tester's FIRST answer (TestAgent / CANDOR / EMSE prompt study)
            **{f"first_{k}": _mean([(c.get("first_attempt") or {}).get(k) for c in r.get("classes", [])])
               for k in ("syntax_rate", "compile_rate", "execution_rate", "pass_rate", "oracle_correctness")},
            "oracles_per_test": _mean([c.get("final_oracles", 0) / c["final_tests"] for c in classes
                                       if c.get("final_tests")]),
        })
    return rows


def main():
    only = None
    if len(sys.argv) > 1:
        only = set(Path(sys.argv[1]).read_text().split())
    rows = load_rows(only)
    if not rows:
        sys.exit("No results yet.")

    with open(RESULTS_DIR / "summary.csv", "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)

    settings = sorted({r["setting"] for r in rows}, key=lambda s: (s != "full", s))
    by = {s: {r["bug"]: r for r in rows if r["setting"] == s} for s in settings}
    common = set.intersection(*(set(v) for v in by.values()))

    lines = [f"Compared on the {len(common)} bugs that every setting finished.", ""]

    def table(title, cols):
        lines.extend([f"### {title}", "", "| Setting | " + " | ".join(c[0] for c in cols) + " |",
                      "|" + "|".join(["---"] * (len(cols) + 1)) + "|"])
        for s in settings:
            rs = [by[s][b] for b in common]
            if rs:
                lines.append(f"| {s} | " + " | ".join(c[1](rs) for c in cols) + " |")
        lines.append("")

    m = lambda rs, k: _mean([r[k] for r in rs])  # noqa: E731
    pct = lambda k: (lambda rs: _fmt(m(rs, k)))  # noqa: E731
    num = lambda k: (lambda rs: "-" if m(rs, k) is None else f"{m(rs, k):.1f}")  # noqa: E731
    table("Effectiveness", [
        ("Bugs detected (FDR)", lambda rs: f"{sum(r['detected'] for r in rs)}/{len(rs)} "
                                           f"({100 * sum(r['detected'] for r in rs) / len(rs):.0f}%)"),
        ("Class line cov. (D4J)", pct("d4j_line_cov")), ("Class branch cov. (D4J)", pct("d4j_branch_cov")),
        ("Major mutation", pct("major_mutation")), ("PIT mutation (class)", pct("pit_class")),
        ("PIT mutation (method)", pct("pit_method")), ("Method line cov.", pct("line_cov")),
        ("Method branch cov.", pct("branch_cov")), ("Our mutants killed", pct("mutation_score")),
    ])
    table("Test quality and cost", [
        ("1st try: syntax ok", pct("first_syntax_rate")), ("1st try: compile", pct("first_compile_rate")),
        ("1st try: run w/o error", pct("first_execution_rate")), ("1st try: pass", pct("first_pass_rate")),
        ("1st try: oracle correctness", pct("first_oracle_correctness")),
        ("Valid tests / bug", num("final_tests")), ("Assertions / test", num("oracles_per_test")),
        ("False alarms", lambda rs: str(sum(r["false_alarms"] for r in rs))),
        ("Bug-encoding tests", lambda rs: str(sum(r["bug_encoding_tests"] for r in rs))),
        ("AI calls / bug", num("ai_calls")), ("Tokens / bug", num("tokens")), ("Seconds / bug", num("seconds")),
    ])
    text = "\n".join(lines)
    (RESULTS_DIR / "summary.md").write_text(text + "\n")
    print(text)
    print(f"\nPer-bug rows: {RESULTS_DIR / 'summary.csv'}")


def _fmt(x):
    return "-" if x is None else f"{100 * x:.0f}%"


if __name__ == "__main__":
    main()
