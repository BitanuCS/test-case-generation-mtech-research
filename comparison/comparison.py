"""Compare test-generation frameworks on the SAME bugs, and save graphs + tables.

  python3 comparison/comparison.py full evosuite
  python3 comparison/comparison.py full evosuite advertest --bugs bugs/dev40.txt
  python3 comparison/comparison.py full single-shot no-context no-planner no-fix no-mutation

Each name is a results folder in results/ (a framework or a setting of ours):
  full = our framework, evosuite, advertest, single-shot, no-context, ...

Only bugs that EVERY compared framework has finished are used, so the
comparison is fair. ALL comparison graphs are made by this one script.
Every run writes a NEW folder named after what was compared, how many bugs and
the date (India time), so earlier comparisons are never overwritten:

  comparison/result/<a>_vs_<b>[_vs_...]_<N>bugs_<date>/
      01_bugs_detected.png     share of bugs detected (main result)
      02_key_measures.png      detection, mutation and coverage, side by side
      03_overlap.png           which bugs each one caught (2 frameworks: only A / both / only B / neither)
      04_per_project.png       bugs detected per Defects4J project
      05_per_bug.png           every bug: detected or not, per framework
      06_time_and_cost.png     seconds per bug (and AI calls per bug, if any)
      07_first_try.png         quality of the AI's first answer (AI frameworks only)
      08_fix_loop.png          how the fix loop improves the AI's tests, round by round
                               (every saved AI answer re-run on the fixed version; cached in
                               results/<framework>/attempt_quality.csv; no AI calls)
      09_mutation_rounds.png   mutant challenge: mutation score and coverage per round (AdverTest Fig. 2)
      10_coverage_vs_detection.png  buggy-method coverage per bug, detected or missed
      11_paired_per_bug.png    two frameworks only: per-bug scores, one against the other
      summary.md               all numbers in tables, plus the lists of bugs
      per_bug.csv              one row per bug and framework
"""
import argparse
import csv
import re
import sys
from datetime import datetime
from zoneinfo import ZoneInfo
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
# folder dates in India time (env.sh sets TZ to Los Angeles for Defects4J)
TODAY = datetime.now(ZoneInfo("Asia/Kolkata")).date().isoformat()
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "scripts"))

from summarize import _mean, load_rows  # noqa: E402  (same numbers as the results tables)

OUT_ROOT = Path(__file__).resolve().parent / "result"

# Display names, and ONE fixed colour per framework (a framework keeps its colour in every graph).
NAMES = {
    "full": "Our framework", "evosuite": "EvoSuite", "advertest": "AdverTest", "single-shot": "Single-shot AI",
    "no-context": "No context", "no-planner": "No planner", "no-fix": "No fix loop", "no-mutation": "No mutation",
    "realistic": "Realistic mode", "realistic-no-panel": "Realistic, no panel", "randoop": "Randoop",
    "full-repeat2": "Our framework (run 2)", "full-repeat3": "Our framework (run 3)",
}
COLORS = {  # validated categorical palette (colour-blind safe in adjacent pairs)
    "full": "#2a78d6", "evosuite": "#eb6834", "advertest": "#1baf7a", "single-shot": "#eda100",
    "no-context": "#e87ba4", "no-planner": "#008300", "no-fix": "#6250d6", "no-mutation": "#e34948",
    "realistic": "#1baf7a", "realistic-no-panel": "#e87ba4", "randoop": "#6250d6",
}
EXTRA = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300", "#6250d6", "#e34948"]
INK, MUTED, GRID = "#1b2230", "#5b6577", "#e6e9ef"
NEUTRAL_BOTH, NEUTRAL_NONE = "#52514e", "#c9ced8"

# percentage measures: (key in summarize rows, label)
PCT = [
    ("detected", "Bugs detected"),
    ("major_mutation", "Major mutation score"),
    ("pit_class", "PIT mutation, class"),
    ("pit_method", "PIT mutation, buggy method"),
    ("d4j_line_cov", "Line coverage, whole class"),
    ("d4j_branch_cov", "Branch coverage, whole class"),
    ("line_cov", "Line coverage, buggy method"),
    ("branch_cov", "Branch coverage, buggy method"),
]
FIRST = [("first_syntax_rate", "Syntax OK"), ("first_compile_rate", "Compiles"),
         ("first_execution_rate", "Runs without error"), ("first_pass_rate", "Passes"),
         ("first_oracle_correctness", "Oracle correctness")]


def name(s):
    return NAMES.get(s, s)


def color(s, i):
    return COLORS.get(s, EXTRA[i % len(EXTRA)])


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", name(s).lower()).strip("-")


def style(ax, xgrid=True):
    for side in ("top", "right", "left"):
        ax.spines[side].set_visible(False)
    ax.spines["bottom"].set_color(GRID)
    ax.tick_params(colors=MUTED, length=0, labelsize=9)
    if xgrid:
        ax.xaxis.grid(True, color=GRID, linewidth=1)
    ax.set_axisbelow(True)


def save(fig, path, title, subtitle=None):
    """Title (and subtitle) in a fixed band at the top of the figure, plot below it."""
    h = fig.get_figheight()
    band = (0.85 if subtitle else 0.55) / h          # inches reserved for the title lines
    fig.subplots_adjust(top=1 - band)
    fig.text(0.01, 1 - 0.08 / h, title, ha="left", va="top", fontsize=13, fontweight="bold", color=INK)
    if subtitle:
        fig.text(0.01, 1 - 0.42 / h, subtitle, ha="left", va="top", fontsize=9, color=MUTED)
    fig.savefig(path, dpi=160, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def legend(ax, fws, loc="lower right"):
    from matplotlib.patches import Patch
    ax.legend(handles=[Patch(color=color(s, i), label=name(s)) for i, s in enumerate(fws)],
              loc=loc, frameon=False, fontsize=9, labelcolor=INK)


def avg(rows, key):
    if key == "detected":
        return sum(r["detected"] for r in rows) / len(rows) if rows else None
    return _mean([r.get(key) for r in rows])


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("frameworks", nargs="+", help="results folders to compare, e.g. full evosuite advertest")
    ap.add_argument("--bugs", default=None, help="only these bugs (a bug list file, e.g. bugs/dev40.txt)")
    ap.add_argument("--name", default=None, help="custom name for the output folder")
    ap.add_argument("--no-replay", action="store_true",
                    help="don't replay saved AI answers that are not measured yet (fix-loop graph uses only cached ones)")
    args = ap.parse_args()
    fws = args.frameworks
    if len(fws) < 2:
        sys.exit("Give at least two frameworks to compare.")

    only = set(Path(args.bugs).read_text().split()) if args.bugs else None
    rows = [r for r in load_rows(only) if r["setting"] in fws]
    by = {s: {r["bug"]: r for r in rows if r["setting"] == s} for s in fws}
    for s in fws:
        if not by[s]:
            sys.exit(f"No finished results for '{s}' (folder results/{s}/).")
    common = sorted(set.intersection(*(set(v) for v in by.values())),
                    key=lambda b: (b.rsplit("-", 1)[0], int(b.rsplit("-", 1)[1])))
    if not common:
        sys.exit("These frameworks have no finished bug in common yet.")
    N = len(common)
    R = {s: [by[s][b] for b in common] for s in fws}

    folder = args.name or ("_vs_".join(slug(s) for s in fws) + f"_{N}bugs_{TODAY}")
    out = OUT_ROOT / folder
    out.mkdir(parents=True, exist_ok=True)
    note = f"Same {N} Defects4J bugs for every framework; same Defects4J scripts judge all of them."

    # ---------- 01 bugs detected ----------
    fig, ax = plt.subplots(figsize=(7.5, 0.55 * len(fws) + 1.2))
    for i, s in enumerate(fws):
        d = sum(r["detected"] for r in R[s])
        ax.barh(i, 100 * d / N, height=0.55, color=color(s, i))
        ax.text(100 * d / N + 1, i, f"{d}/{N}  ({100 * d / N:.0f}%)", va="center", fontsize=10,
                color=INK, fontweight="bold")
    ax.set_yticks(range(len(fws)), [name(s) for s in fws], color=INK, fontsize=10)
    ax.invert_yaxis()
    ax.set_xlim(0, 100)
    ax.xaxis.set_major_formatter(lambda v, _: f"{v:.0f}%")
    style(ax)
    save(fig, out / "01_bugs_detected.png", "Bugs detected (fault detection rate)", note)

    # ---------- 02 key measures ----------
    def paired(k):
        """bugs where every framework has this measure (so averages compare like with like)"""
        return [b for b in common if all(by[s][b].get(k) is not None for s in fws)] if k != "detected" else common
    measures, missing = [], []
    for k, lab in PCT:
        pb = paired(k)
        if not pb:
            missing.append(lab)
            continue
        measures.append((k, lab if len(pb) == N else f"{lab} (on {len(pb)} bugs)"))
    RK = {k: {s: [by[s][b] for b in paired(k)] for s in fws} for k, _ in measures}
    h = 0.8 / len(fws)
    fig, ax = plt.subplots(figsize=(8.5, 0.5 * len(measures) * len(fws) + 1.4))
    for i, s in enumerate(fws):
        for j, (k, _) in enumerate(measures):
            v = 100 * avg(RK[k][s], k)
            y = j + i * h
            ax.barh(y, v, height=h * 0.9, color=color(s, i))
            ax.text(v + 1, y, f"{v:.0f}%", va="center", fontsize=8.5, color=INK)
    ax.set_yticks([j + h * (len(fws) - 1) / 2 for j in range(len(measures))], [lab for _, lab in measures],
                  color=INK, fontsize=10)
    ax.invert_yaxis()
    ax.set_xlim(0, 108)
    ax.xaxis.set_major_formatter(lambda v, _: f"{v:.0f}%" if v <= 100 else "")
    style(ax)
    legend(ax, fws)
    save(fig, out / "02_key_measures.png", "Key measures", note
         + (f"  Not shown (not measured for every framework): {', '.join(missing)}." if missing else ""))

    # ---------- 03 overlap ----------
    det = {s: {b for b in common if by[s][b]["detected"]} for s in fws}
    overlap_lines = []
    if len(fws) == 2:
        a, b = fws
        only = lambda f: "Only " + (name(f)[0].lower() + name(f)[1:] if name(f).startswith("Our") else name(f))
        parts = [(only(a), det[a] - det[b], color(a, 0)), ("Both", det[a] & det[b], NEUTRAL_BOTH),
                 (only(b), det[b] - det[a], color(b, 1)),
                 ("Neither", set(common) - det[a] - det[b], NEUTRAL_NONE)]
        fig, ax = plt.subplots(figsize=(9, 1.9))
        fig.subplots_adjust(left=0.01, right=0.99)
        x = 0
        for lab, bugs, c in parts:
            n = len(bugs)
            if n:
                ax.barh(0, n, left=x, height=0.5, color=c, edgecolor="white", linewidth=2)
                ax.text(x + n / 2, 0, str(n), ha="center", va="center", fontsize=11, fontweight="bold",
                        color="white" if c != NEUTRAL_NONE else INK)
            x += n
            overlap_lines.append((lab, sorted(bugs)))
        ax.set_xlim(0, N)
        ax.set_yticks([])
        style(ax, xgrid=False)
        ax.spines["bottom"].set_visible(False)
        ax.set_xticks([])
        from matplotlib.patches import Patch
        ax.legend(handles=[Patch(color=c, label=f"{lab}: {len(bugs)}") for lab, bugs, c in parts],
                  loc="upper center", bbox_to_anchor=(0.5, -0.05), ncol=4, frameon=False, fontsize=9,
                  labelcolor=INK)
        union = len(det[a] | det[b])
        save(fig, out / "03_overlap.png", "Which bugs each one caught",
             f"Caught by at least one: {union}/{N} ({100 * union / N:.0f}%).")
    else:
        union = set().union(*det.values())
        overlap_lines.append(("Caught by at least one", sorted(union)))
        overlap_lines.append(("Caught by none", sorted(set(common) - union)))
        for s in fws:
            others = set().union(*(det[o] for o in fws if o != s))
            overlap_lines.append((f"Only {name(s)}", sorted(det[s] - others)))

    # ---------- 04 per project ----------
    projects = sorted({b.rsplit("-", 1)[0] for b in common})
    h = 0.8 / len(fws)
    fig, ax = plt.subplots(figsize=(8, 0.45 * len(projects) * len(fws) + 1.4))
    maxn = 0
    for j, p in enumerate(projects):
        pb = [b for b in common if b.rsplit("-", 1)[0] == p]
        maxn = max(maxn, len(pb))
        for i, s in enumerate(fws):
            d = sum(by[s][b]["detected"] for b in pb)
            y = j + i * h
            ax.barh(y, d, height=h * 0.9, color=color(s, i))
            ax.text(d + 0.05, y, str(d), va="center", fontsize=8.5, color=INK)
    ax.set_yticks([j + h * (len(fws) - 1) / 2 for j in range(len(projects))],
                  [f"{p} ({sum(1 for b in common if b.rsplit('-', 1)[0] == p)})" for p in projects],
                  color=INK, fontsize=9.5)
    ax.invert_yaxis()
    ax.set_xlim(0, maxn + 0.6)
    ax.xaxis.set_major_locator(matplotlib.ticker.MaxNLocator(integer=True))
    style(ax)
    legend(ax, fws)
    save(fig, out / "04_per_project.png", "Bugs detected per project",
         "Number in brackets = bugs of that project in the comparison.")

    # ---------- 05 per bug ----------
    fig, ax = plt.subplots(figsize=(1.4 * len(fws) + 2.2, 0.24 * N + 1.2))
    for i, s in enumerate(fws):
        for j, b in enumerate(common):
            hit = by[s][b]["detected"]
            ax.add_patch(plt.Rectangle((i + 0.06, j + 0.08), 0.88, 0.84,
                                       color=color(s, i) if hit else "#f1f3f6", linewidth=0))
            ax.text(i + 0.5, j + 0.5, "✓" if hit else "–", ha="center", va="center", fontsize=8,
                    color="white" if hit else MUTED)
    ax.set_xlim(0, len(fws))
    ax.set_ylim(N, 0)
    ax.set_xticks([i + 0.5 for i in range(len(fws))], [name(s) for s in fws], color=INK, fontsize=9)
    ax.xaxis.tick_top()
    ax.set_yticks([j + 0.5 for j in range(N)], common, fontsize=8, color=INK)
    style(ax, xgrid=False)
    ax.spines["bottom"].set_visible(False)
    save(fig, out / "05_per_bug.png", "Every bug", "Coloured ✓ = detected; – = missed.")

    # ---------- 06 time and cost ----------
    has_ai = [s for s in fws if (avg(R[s], "ai_calls") or 0) > 0]
    ncols = 2 if has_ai else 1
    fig, axes = plt.subplots(1, ncols, figsize=(4.2 * ncols + 1, 0.5 * len(fws) + 1.4), squeeze=False)
    for col, (key, lab) in enumerate([("seconds", "Seconds per bug"), ("ai_calls", "AI calls per bug")][:ncols]):
        ax = axes[0][col]
        vals = [avg(R[s], key) or 0 for s in fws]
        for i, (s, v) in enumerate(zip(fws, vals)):
            ax.barh(i, v, height=0.55, color=color(s, i))
            ax.text(v, i, f"  {v:.0f}" if key == "seconds" else f"  {v:.1f}", va="center", fontsize=9, color=INK)
        ax.set_yticks(range(len(fws)), [name(s) for s in fws] if col == 0 else [""] * len(fws),
                      color=INK, fontsize=9.5)
        ax.invert_yaxis()
        ax.set_title(lab, loc="left", fontsize=10, color=INK)
        ax.set_xlim(0, max(vals) * 1.25 or 1)
        style(ax)
    save(fig, out / "06_time_and_cost.png", "Time and cost", "Averages over the compared bugs.")

    # ---------- 07 first try (AI frameworks only) ----------
    ai = [s for s in fws if avg(R[s], "first_compile_rate") is not None]
    if ai:
        h = 0.8 / len(ai)
        fig, ax = plt.subplots(figsize=(8, 0.5 * len(FIRST) * len(ai) + 1.4))
        for i, s in enumerate(ai):
            ci = fws.index(s)
            for j, (k, _) in enumerate(FIRST):
                v = 100 * (avg(R[s], k) or 0)
                y = j + i * h
                ax.barh(y, v, height=h * 0.9, color=color(s, ci))
                ax.text(v + 1, y, f"{v:.0f}%", va="center", fontsize=8.5, color=INK)
        ax.set_yticks([j + h * (len(ai) - 1) / 2 for j in range(len(FIRST))], [lab for _, lab in FIRST],
                      color=INK, fontsize=10)
        ax.invert_yaxis()
        ax.set_xlim(0, 108)
        ax.xaxis.set_major_formatter(lambda v, _: f"{v:.0f}%" if v <= 100 else "")
        style(ax)
        legend(ax, ai)
        save(fig, out / "07_first_try.png", "Quality of the AI's first answer (before any repair)",
             "Only frameworks that use an AI.")

    # ---------- 08-11: fix loop, mutation rounds, coverage vs detection, paired scores ----------
    md_extra = more_graphs(fws, common, by, out, replay=not args.no_replay)

    # ---------- tables ----------
    with open(out / "per_bug.csv", "w", newline="") as f:
        cols = list(rows[0])
        w = csv.DictWriter(f, fieldnames=cols)
        w.writeheader()
        for s in fws:
            for b in common:
                w.writerow(by[s][b])

    def fmt(v, pct=True):
        return "-" if v is None else (f"{100 * v:.0f}%" if pct else f"{v:.1f}")

    md = [f"# {' vs '.join(name(s) for s in fws)}: {N} bugs", "",
          f"Made {TODAY} by `comparison/comparison.py {' '.join(fws)}"
          + (f" --bugs {args.bugs}" if args.bugs else "") + "`.", "", note, "",
          "| Measure | " + " | ".join(name(s) for s in fws) + " |",
          "|---|" + "---|" * len(fws)]
    md.append("| Bugs detected | " + " | ".join(
        f"{sum(r['detected'] for r in R[s])}/{N} ({100 * sum(r['detected'] for r in R[s]) / N:.0f}%)"
        for s in fws) + " |")
    for k, lab in PCT[1:]:
        pb = paired(k)
        extra = "" if len(pb) == N else f" (on {len(pb)} bugs)"
        md.append(f"| {lab}{extra} | " + " | ".join(fmt(avg([by[s][b] for b in pb], k)) if pb else "-"
                                                    for s in fws) + " |")
    for k, lab in FIRST:
        md.append(f"| 1st try: {lab} | " + " | ".join(fmt(avg(R[s], k)) for s in fws) + " |")
    for k, lab in [("final_tests", "Valid tests per bug"), ("oracles_per_test", "Assertions per test"),
                   ("ai_calls", "AI calls per bug"), ("tokens", "Tokens per bug"), ("seconds", "Seconds per bug")]:
        md.append(f"| {lab} | " + " | ".join(fmt(avg(R[s], k), pct=False) for s in fws) + " |")
    md += md_extra
    md += ["", "## Which bugs", ""]
    md += [f"- **{lab} ({len(bugs)}):** {', '.join(bugs) or '-'}" for lab, bugs in overlap_lines]
    md += ["", f"## The {N} bugs compared", "", ", ".join(common), ""]
    (out / "summary.md").write_text("\n".join(md))

    print(f"Compared {', '.join(name(s) for s in fws)} on {N} common bugs.")
    for s in fws:
        d = sum(r["detected"] for r in R[s])
        print(f"  {name(s):<22} {d}/{N} detected ({100 * d / N:.0f}%)")
    print(f"Saved graphs and tables in: {out}")


# =====================================================================================
# Fix loop: quality of EVERY saved AI answer (replayed on the fixed version, no AI calls)
# =====================================================================================
ATTEMPT = re.compile(r"attempt_(\d+)_(\w+)\.java$")
RATE_KEYS = ["syntax_rate", "compile_rate", "execution_rate", "pass_rate", "oracle_correctness"]
RATE_LABELS = dict(FIRST) | {"syntax_rate": "Syntax OK", "compile_rate": "Compiles",
                             "execution_rate": "Runs without error", "pass_rate": "Passes",
                             "oracle_correctness": "Oracle correctness"}


def results_dir():
    from our_framework.pipeline import RESULTS_DIR
    return RESULTS_DIR


def replay_answers(setting, bugs, replay=True):
    """Rows (bug, class, attempt, kind, rates) for every saved AI answer of `setting`.
    Cached in results/<setting>/attempt_quality.csv; only bugs not in the cache are replayed.
    Each answer is compiled and run on the FIXED version with the same measurements as the
    1st-answer numbers (pipeline._first_attempt_stats)."""
    base = results_dir() / setting
    cache = base / "attempt_quality.csv"
    rows = []
    if cache.exists():
        with open(cache) as f:
            for r in csv.DictReader(f):
                r["attempt"] = int(r["attempt"])
                for k in RATE_KEYS:
                    r[k] = float(r[k]) if r[k] not in ("", "None") else None
                rows.append(r)
    done = {r["bug"] for r in rows}
    todo = [b for b in bugs if b not in done and any((base / b).glob("*/attempt_*.java"))]
    if todo and replay:
        from our_framework import agents, context, d4j, pipeline
        import json

        class _NoSave:
            def text(self, *a): pass
            def json(self, *a): pass

        print(f"Replaying saved AI answers of {len(todo)} bug(s) for '{setting}' (no AI calls)...", flush=True)
        for b in todo:
            rep = json.loads((base / b / "report.json").read_text())
            project, bid = b.rsplit("-", 1)
            bug = d4j.Bug(project, int(bid))
            bug.checkout("f")
            bug.checkout("b")
            for class_dir in sorted(p for p in (base / b).iterdir() if p.is_dir()):
                cls = next((c for c in rep.get("classes", []) if c.get("class", "").endswith("." + class_dir.name)), None)
                files = sorted((f for f in class_dir.glob("attempt_*.java") if ATTEMPT.search(f.name)),
                               key=lambda f: int(ATTEMPT.search(f.name).group(1)))
                if cls is None or not files:
                    continue
                run = pipeline.ClassRun(bug, cls["class"], pipeline.PRESETS.get(setting, pipeline.PRESETS["full"]),
                                        _NoSave())
                run.ctx = context.build(bug, cls["class"], "f")
                run.test_class = f"{run.ctx.class_name}_CandorTest"
                for f in files:
                    n, kind = ATTEMPT.search(f.name).groups()
                    agents.extract_java.last = "saved"
                    st = run._first_attempt_stats(f.read_text(errors="replace"))
                    rows.append({"bug": b, "class": class_dir.name, "attempt": int(n), "kind": kind,
                                 **{k: st.get(k) for k in RATE_KEYS}})
            print(f"  {b} done", flush=True)
        with open(cache, "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=["bug", "class", "attempt", "kind"] + RATE_KEYS, extrasaction="ignore")
            w.writeheader()
            w.writerows(rows)
    return [r for r in rows if r["bug"] in set(bugs)]


def fix_loop_steps(rows):
    """Step k = each class's suite after up to k Fixer answers (initial fix loop only,
    i.e. before the first mutant-killer answer); the last answer is carried forward so
    every class counts at every step (no selection bias)."""
    by_class = {}
    for r in rows:
        by_class.setdefault((r["bug"], r["class"]), []).append(r)
    loops = []
    for rs in by_class.values():
        rs.sort(key=lambda r: r["attempt"])
        seq = []
        for r in rs:
            if r["kind"] == "mutant_killers":
                break
            if r["kind"] in ("tester", "fix_compile", "fix_tests"):
                seq.append(r)
        if seq and seq[0]["kind"] == "tester":
            loops.append(seq)
    if not loops:
        return [], 0
    steps = []
    for k in range(max(len(s) for s in loops)):
        cur = [s[min(k, len(s) - 1)] for s in loops]
        steps.append({"step": "1st answer" if k == 0 else f"after {k} fix{'es' if k > 1 else ''}",
                      "classes": len(cur), "needed": len(cur) if k == 0 else sum(1 for s in loops if k < len(s)),
                      **{key: _mean([c[key] for c in cur]) for key in RATE_KEYS}})
    return steps, len(loops)


def mutation_rounds(setting, bugs):
    """Per challenge round: our rule-mutant score, all-mutant score and buggy-method line coverage,
    averaged over classes (last round carried forward)."""
    import json
    series = []
    for b in bugs:
        rep = results_dir() / setting / b / "report.json"
        if not rep.exists():
            continue
        for c in json.loads(rep.read_text()).get("classes", []):
            rounds = c.get("mutation") or []
            if c.get("status") == "ok" and rounds:
                series.append(rounds)
    if not series:
        return []
    out = []
    for k in range(max(len(s) for s in series)):
        cur = [s[min(k, len(s) - 1)] for s in series]
        out.append({"round": k,
                    "rule": _mean([(c.get("rule") or {}).get("score") for c in cur]),
                    "all": _mean([(c.get("all") or {}).get("score") for c in cur]),
                    "coverage": _mean([(c.get("coverage") or {}).get("line") for c in cur]),
                    "classes": len(cur)})
    return out


def more_graphs(fws, common, by, out, replay=True):
    """08 fix loop, 09 mutation rounds, 10 coverage vs detection, 11 paired per-bug scores.
    Returns extra summary.md lines."""
    md = []
    pct = lambda v: "-" if v is None else f"{100 * v:.0f}%"  # noqa: E731

    # ---------- 08 fix loop (AI frameworks with saved answers) ----------
    loops = {}
    for s in fws:
        if any((results_dir() / s / b).glob("*/attempt_*.java") for b in common):
            steps, ncls = fix_loop_steps(replay_answers(s, common, replay))
            if steps:
                loops[s] = (steps, ncls)
    if loops:
        fig, ax = plt.subplots(figsize=(8.5, 4.4))
        if len(loops) == 1:
            s, (steps, ncls) = next(iter(loops.items()))
            lines = [("compile_rate", "#2a78d6"), ("pass_rate", "#eb6834"), ("oracle_correctness", "#1baf7a")]
            sub = f"{name(s)}: {ncls} test classes; each saved AI answer re-run on the fixed version (no AI calls)."
        else:
            lines = [("pass_rate", None)]
            sub = "Passing tests after each fix round; each saved AI answer re-run on the fixed version."
        ends, starts = [], []                      # (y, text, x) end/start labels, spread apart below
        for s, (steps, ncls) in loops.items():
            xs = list(range(len(steps)))
            for key, c in lines:
                ys = [100 * (st[key] or 0) for st in steps]
                col = c or color(s, fws.index(s))
                lab = RATE_LABELS[key] if len(loops) == 1 else name(s)
                ax.plot(xs, ys, color=col, linewidth=2, marker="o", markersize=7, markeredgecolor="white",
                        markeredgewidth=2)
                ends.append([ys[-1], f"{lab} {ys[-1]:.0f}%", xs[-1] + 0.1])
                starts.append([ys[0], f"{ys[0]:.0f}%", xs[0] - 0.1])
        for group, ha in ((ends, "left"), (starts, "right")):
            group.sort(key=lambda g: -g[0])            # top to bottom, at least 4.5 points apart
            for i in range(1, len(group)):
                group[i][0] = min(group[i][0], group[i - 1][0] - 4.5)
            for y, txt, x in group:
                ax.text(x, y, txt, va="center", ha=ha, fontsize=9, color=INK)
        longest = max(len(v[0]) for v in loops.values())
        ref = max(loops.values(), key=lambda v: len(v[0]))[0]
        ax.set_xticks(range(longest), [st["step"] for st in ref], fontsize=9, color=INK)
        ax.set_xlim(-0.6, longest - 1 + 1.9)
        ax.set_ylim(0, 105)
        ax.yaxis.set_major_formatter(lambda v, _: f"{v:.0f}%")
        style(ax, xgrid=False)
        ax.yaxis.grid(True, color=GRID)
        save(fig, out / "08_fix_loop.png", "How the fix loop improves the AI's tests", sub)
        for s, (steps, ncls) in loops.items():
            md += ["", f"## Fix loop: {name(s)} ({ncls} test classes)", "",
                   "Each class's suite after up to k Fixer answers (last answer carried forward). "
                   "Every saved AI answer was re-run on the fixed version; no AI calls.", "",
                   "| Step | Needed this step | " + " | ".join(RATE_LABELS[k] for k in RATE_KEYS) + " |",
                   "|---|---|" + "---|" * len(RATE_KEYS)]
            md += [f"| {st['step']} | {st['needed']}/{st['classes']} | " + " | ".join(pct(st[k]) for k in RATE_KEYS)
                   + " |" for st in steps]

    # ---------- 09 mutation challenge rounds (our mutants; AdverTest Fig. 2 style) ----------
    mr = {s: mutation_rounds(s, common) for s in fws}
    mr = {s: v for s, v in mr.items() if v}
    if mr:
        fig, axes = plt.subplots(1, 2, figsize=(10, 4.2))
        for s, rounds in mr.items():
            xs = [r["round"] for r in rounds]
            for ax, key, lab in ((axes[0], "rule", "Mutation score (rule mutants)"),
                                 (axes[1], "coverage", "Line coverage, buggy method")):
                ys = [100 * (r[key] or 0) for r in rounds]
                ax.plot(xs, ys, color=color(s, fws.index(s)), linewidth=2, marker="o", markersize=7,
                        markeredgecolor="white", markeredgewidth=2, label=name(s))
                for x, y in zip(xs, ys):
                    ax.text(x, y + 2.5, f"{y:.0f}%", ha="center", fontsize=8.5, color=INK)
                ax.set_title(lab, loc="left", fontsize=10, color=INK)
                ax.set_xticks(xs, ["start" if x == 0 else f"round {x}" for x in xs], fontsize=9, color=INK)
                ax.set_ylim(0, 108)
                ax.yaxis.set_major_formatter(lambda v, _: f"{v:.0f}%" if v <= 100 else "")
                style(ax, xgrid=False)
                ax.yaxis.grid(True, color=GRID)
        if len(mr) > 1:
            axes[1].legend(loc="lower right", frameon=False, fontsize=9)
        n_cls = max(r[0]["classes"] for r in mr.values())
        save(fig, out / "09_mutation_rounds.png", "Mutant challenge: before and after each round",
             f"{n_cls} test classes; same 25 rule-based mutants per class in every round (as AdverTest's Fig. 2).")
        for s, rounds in mr.items():
            md += ["", f"## Mutant challenge rounds: {name(s)}", "",
                   "| Round | Mutation score (rule mutants) | Mutation score (all, incl. AI mutants) | Buggy-method line coverage |",
                   "|---|---|---|---|"]
            md += [f"| {'start' if r['round'] == 0 else r['round']} | {pct(r['rule'])} | {pct(r['all'])} | "
                   f"{pct(r['coverage'])} |" for r in rounds]

    # ---------- 10 coverage of the buggy method vs detection ----------
    have = [s for s in fws if any(by[s][b].get("line_cov") is not None for b in common)]
    if have:
        fig, ax = plt.subplots(figsize=(8.5, 0.9 * len(have) + 1.6))
        import random
        rnd = random.Random(1)                         # fixed jitter so the graph is reproducible
        cov_lines = []
        for i, s in enumerate(have):
            c = color(s, fws.index(s))
            for b in common:
                v = by[s][b].get("line_cov")
                if v is None:
                    continue
                y = i + rnd.uniform(-0.22, 0.22)
                hit = by[s][b]["detected"]
                ax.scatter(100 * v, y, s=46, color=c if hit else "white", edgecolors=c, linewidths=1.6, zorder=3)
            full_missed = [b for b in common if (by[s][b].get("line_cov") or 0) >= 0.99 and not by[s][b]["detected"]]
            cov_lines.append(f"- **{name(s)}:** {len(full_missed)} bugs missed although the buggy method had "
                             f"100% line coverage: {', '.join(full_missed) or '-'}")
        ax.set_yticks(range(len(have)), [name(s) for s in have], color=INK, fontsize=10)
        ax.set_ylim(len(have) - 0.5, -0.5)
        ax.set_xlim(-3, 103)
        ax.xaxis.set_major_formatter(lambda v, _: f"{v:.0f}%")
        style(ax)
        from matplotlib.lines import Line2D
        ax.legend(handles=[Line2D([], [], marker="o", ls="", color=MUTED, markersize=7, label="bug detected"),
                           Line2D([], [], marker="o", ls="", markerfacecolor="white", markeredgecolor=MUTED,
                                  markersize=7, label="bug missed")],
                  loc="upper left", bbox_to_anchor=(0, -0.12), ncol=2, frameon=False, fontsize=9)
        save(fig, out / "10_coverage_vs_detection.png", "Line coverage of the buggy method vs bug detected",
             "One dot per bug. Many bugs are missed even at 100% coverage: running the code is not enough.")
        md += ["", "## Coverage is not detection", ""] + cov_lines

    # ---------- 11 paired per-bug scores (exactly two frameworks) ----------
    if len(fws) == 2:
        a, b2 = fws
        panels = [(k, lab) for k, lab in [("major_mutation", "Major mutation score"),
                                          ("pit_method", "PIT score, buggy method"),
                                          ("line_cov", "Line coverage, buggy method")]
                  if any(by[a][b].get(k) is not None and by[b2][b].get(k) is not None for b in common)]
        if panels:
            fig, axes = plt.subplots(1, len(panels), figsize=(4.2 * len(panels), 4.6), squeeze=False)
            md += ["", f"## Per bug: {name(a)} vs {name(b2)}", "",
                   f"| Measure | Bugs | {name(a)} higher | equal | {name(b2)} higher |", "|---|---|---|---|---|"]
            for ax, (k, lab) in zip(axes[0], panels):
                pts = [(100 * by[a][b][k], 100 * by[b2][b][k], b) for b in common
                       if by[a][b].get(k) is not None and by[b2][b].get(k) is not None]
                ax.plot([0, 100], [0, 100], color=GRID, linewidth=1.5, zorder=1)
                for x, y, b in pts:
                    c = color(a, 0) if x > y + 0.5 else (color(b2, 1) if y > x + 0.5 else MUTED)
                    ax.scatter(x, y, s=40, color=c, edgecolors="white", linewidths=1.2, zorder=3)
                hi_a = sum(1 for x, y, _ in pts if x > y + 0.5)
                hi_b = sum(1 for x, y, _ in pts if y > x + 0.5)
                ax.set_title(f"{lab}\n{name(a)} higher on {hi_a}, {name(b2)} on {hi_b} of {len(pts)}",
                             loc="left", fontsize=9.5, color=INK)
                ax.set_xlim(-3, 103)
                ax.set_ylim(-3, 103)
                ax.set_aspect("equal")
                ax.set_xlabel(name(a), color=INK, fontsize=9)
                ax.set_ylabel(name(b2), color=INK, fontsize=9)
                style(ax)
                ax.yaxis.grid(True, color=GRID)
                ax.spines["left"].set_visible(True)
                ax.spines["left"].set_color(GRID)
                md.append(f"| {lab} | {len(pts)} | {hi_a} | {len(pts) - hi_a - hi_b} | {hi_b} |")
            save(fig, out / "11_paired_per_bug.png", f"Per bug: {name(a)} vs {name(b2)}",
                 f"One dot per bug. Below the diagonal = {name(a)} higher; above = {name(b2)} higher.")
    return md


if __name__ == "__main__":
    main()
