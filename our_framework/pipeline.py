"""The full framework for one Defects4J bug.

  1. Understand  (Context Agent, no AI)
  2. Plan        (Planner)
  3. Write       (Tester)
  4. Fix         (Fixer, + Oracle Panel and Curator in realistic mode)
  5. Clean up    (no AI: drop tests that don't compile, fail, or are flaky)
  6. Challenge   (Mutant agent + rule-based mutants -> Killer writes more tests)
  7. Evaluate    (run the final tests on the other version; coverage; mutation score)

Two modes:

  standard   (the usual Defects4J protocol, used by EvoSuite/HITS/AdverTest studies)
             Tests are written and checked on the FIXED code, which is assumed
             correct. Then they run on the BUGGY code: any failure = bug found.

  realistic  (closer to real life, the setting CANDOR's oracle panel is made for)
             Tests are written on the BUGGY code, without knowing it is buggy.
             When a test fails, the Oracle Panel votes: is the test wrong, or
             is the code wrong? Tests the panel trusts are kept as "bug
             reports". Then the tests run on the FIXED code: a bug report that
             passes there is a real bug found; one that still fails is a false
             alarm.

Everything is saved under results/<setting>/<bug>/<FocalClass>/.
"""
import json
import re
import shutil
import time
from dataclasses import asdict, dataclass

from . import agents, context, coverage, d4j, llm, mutation, pit, runner, standard_metrics
from . import java_parse as jp
from .config import MODELS, RESULTS_DIR


@dataclass
class Settings:
    mode: str = "standard"      # "standard" or "realistic"
    context: bool = True        # False: AI sees only the method under test
    planner: bool = True        # False: no separate planning step
    fix_rounds: int = 3         # AI repair rounds (0 = no Fixer)
    panel: bool = True          # realistic mode: let the Oracle Panel judge failing tests
    mutation_rounds: int = 2    # rounds of "write tests for surviving mutants" (0 = off)
    rule_mutants: int = 25      # how many rule-based mutants to sample
    ai_mutants: int = 5         # how many AI mutants to ask for
    coverage: bool = True
    standard_metrics: bool = True   # Defects4J coverage + Major mutation score (same as other tools)
    pit: bool = True                # PIT mutation score (as CANDOR / TestAgent / MutGen)
    repeat: int = 0             # 0 = first run; 2, 3 = repeated runs (fresh AI answers, not cached)


# The experiments of the study. "full" is our framework; the others remove
# one part at a time (ablation) or are simple baselines.
PRESETS = {
    "full":               Settings(),
    "realistic":          Settings(mode="realistic"),
    "no-context":         Settings(context=False),
    "no-planner":         Settings(planner=False),
    "no-fix":             Settings(fix_rounds=0),
    "no-mutation":        Settings(mutation_rounds=0),
    "realistic-no-panel": Settings(mode="realistic", panel=False),
    "single-shot":        Settings(planner=False, fix_rounds=0, panel=False, mutation_rounds=0),
    # Repeated runs of "full" to show how much results vary between runs
    "full-repeat2":       Settings(repeat=2),
    "full-repeat3":       Settings(repeat=3),
}


def _log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


class _Saver:
    def __init__(self, folder):
        self.folder = folder
        folder.mkdir(parents=True, exist_ok=True)

    def text(self, name, content):
        (self.folder / name).write_text(content, encoding="utf-8")

    def json(self, name, obj):
        self.text(name, json.dumps(obj, indent=2, default=str))


ASSERT_RE = re.compile(r"\b(assert\w*|fail)\s*\(|expected\s*=\s*[\w.]+\.class")
ASSERTION_FAILURES = ("java.lang.AssertionError", "org.junit.ComparisonFailure",
                      "junit.framework.AssertionFailedError", "junit.framework.ComparisonFailure",
                      "org.opentest4j.AssertionFailedError")


def _count_oracles(code):
    """Number of oracles (assert..., fail(...), @Test(expected=...)) in test code."""
    return len(ASSERT_RE.findall(code))


def _need_panel(answered):
    """A vote with fewer than 2 panel members is not a real vote: pause the
    run (it is retried later, when the free quotas have reset)."""
    if answered < 2:
        raise llm.QuotaExhausted("fewer than 2 panel members could answer")


class ClassRun:
    """Generates and evaluates tests for one focal class of one bug."""

    def __init__(self, bug, class_fqn, s: Settings, save: _Saver):
        self.bug, self.class_fqn, self.s, self.save = bug, class_fqn, s, save
        self.gen = "f" if s.mode == "standard" else "b"     # version the tests are written on
        self.other = "b" if self.gen == "f" else "f"        # version used for evaluation
        self.ctx = None
        self.test_class = None
        self.bug_reports = set()     # realistic mode: failing tests the panel trusts
        self.panel_log = []
        self.attempt = 0

    # ---------------------------------------------------------- helpers
    def _compile(self, code, version=None):
        return runner.compile_test(self.bug, version or self.gen, self.ctx.package, self.test_class, code)

    def _run(self, version=None):
        return runner.run_test(self.bug, version or self.gen, self.ctx.package, self.test_class)

    def _save_attempt(self, code, why):
        self.attempt += 1
        self.save.text(f"attempt_{self.attempt}_{why}.java", code)

    # ---------------------------------------------------------- First-attempt quality
    def _first_attempt_stats(self, code):
        """Quality of the Tester's FIRST answer, before any repair, measured on the
        FIXED (correct) program -- the metrics TestAgent, CANDOR and the EMSE prompt
        study report: syntax / compile / execution / pass rates and oracle correctness."""
        names = jp.test_method_names(code)
        st = {"tests": len(names), "oracles": _count_oracles(code),
              "output_format": agents.extract_java.last}
        tree = jp.parse(code).root_node
        st["syntax_ok"] = not tree.has_error
        try:
            st["syntax_ok_tests"] = sum(1 for m in jp.read_class(code).methods
                                        if m.name in names and not m.node.has_error)
        except ValueError:
            st["syntax_ok_tests"] = 0
        pkg, tc = self.ctx.package, self.test_class
        comp = runner.compile_test(self.bug, "f", pkg, tc, code, tag="first")
        st["compiles"] = comp.ok
        bad = set()
        if not comp.ok:
            bad = {jp.method_at_line(code, ln) for ln, _ in comp.errors}
            bad = set(names) if (None in bad or not bad) else bad & set(names)
        st["compile_ok_tests"] = len(names) - len(bad)
        runnable = jp.remove_methods(code, bad) if bad and bad != set(names) else code
        st.update(passed=0, assertion_failures=0, error_failures=0, timeout=False)
        if st["compile_ok_tests"] and (comp.ok or runner.compile_test(self.bug, "f", pkg, tc, runnable,
                                                                       tag="first").ok):
            res = runner.run_test(self.bug, "f", pkg, tc, tag="first")
            st["timeout"] = res.timeout
            st["passed"] = len(res.passed)
            st["assertion_failures"] = sum(m.startswith(ASSERTION_FAILURES) for m in res.failed.values())
            st["error_failures"] = len(res.failed) - st["assertion_failures"]
        n = max(len(names), 1)
        st["syntax_rate"] = round(st["syntax_ok_tests"] / n, 3)
        st["compile_rate"] = round(st["compile_ok_tests"] / n, 3)
        st["execution_rate"] = round((st["passed"] + st["assertion_failures"]) / n, 3)  # ran without runtime error
        st["pass_rate"] = round(st["passed"] / n, 3)
        # each failing assertion is one wrong oracle (CANDOR: correct oracles / all oracles)
        st["oracle_correctness"] = (round(1 - st["assertion_failures"] / st["oracles"], 3)
                                    if st["oracles"] else None)
        return st

    # ---------------------------------------------------------- Oracle panel + Curator
    def _panel(self, code, failing):
        """Votes from every panel member; the Curator takes the majority."""
        cases = {n: (jp.method_source(code, n), msg) for n, msg in failing.items()}
        names = list(cases)
        votes = {n: [] for n in names}
        for i in range(0, len(names), 6):                 # small batches fit free limits
            chunk = {n: cases[n] for n in names[i:i + 6]}
            answered = 0
            for member in MODELS["panel"]:
                try:
                    for n, v in agents.judge_failures(self.ctx, chunk, member).items():
                        votes[n].append(v)
                    answered += 1
                except llm.LLMError as e:
                    _log(f"    panel member {member[0][1]} unavailable: {str(e)[:80]}")
            _need_panel(answered)
        decisions = {}
        for n, vs in votes.items():
            best = max(agents.VERDICTS, key=vs.count) if vs else "UNSURE"
            decisions[n] = best if vs.count(best) >= 2 and vs.count(best) > len(vs) / 2 else "UNSURE"
        self.panel_log.append({"votes": votes, "decisions": decisions})
        return decisions

    def _spec_review(self, code, report):
        """Realistic mode: the panel checks the tests that PASS on the code under
        test, because they may have copied the bug. Tests the majority thinks are
        wrong are rewritten to the documented behaviour; if they then fail,
        they become bug reports."""
        cases = {n: jp.method_source(code, n) for n in jp.test_method_names(code) if n not in self.bug_reports}
        names = list(cases)
        votes = {n: [] for n in names}
        for i in range(0, len(names), 6):
            chunk = {n: cases[n] for n in names[i:i + 6]}
            answered = 0
            for member in MODELS["panel"]:
                try:
                    for n, (v, why) in agents.review_tests(self.ctx, chunk, member).items():
                        votes[n].append((v, why))
                    answered += 1
                except llm.LLMError as e:
                    _log(f"    panel member {member[0][1]} unavailable: {str(e)[:80]}")
            _need_panel(answered)
        wrong = {}
        for n, vs in votes.items():
            dis = [why for v, why in vs if v == "DISAGREE"]
            if len(dis) >= 2 and len(dis) > len(vs) / 2:
                wrong[n] = dis
        self.panel_log.append({"review_votes": votes, "rewrite": sorted(wrong)})
        _log(f"  panel review: {len(names)} passing tests checked, {len(wrong)} judged wrong")
        report["panel_review_wrong"] = sorted(wrong)
        if not wrong:
            return code
        new = agents.fix_to_spec(self.ctx, code, wrong, self.test_class)
        self._save_attempt(new, "fix_to_spec")
        comp = self._compile(new)
        if not comp.ok:
            new = agents.fix_compile(self.ctx, new, comp.raw, self.test_class)
            if not self._compile(new).ok:
                _log("  rewritten tests do not compile; keeping previous suite")
                return code
        res = self._run()
        rewritten = set(wrong) & set(jp.test_method_names(new))
        self.bug_reports |= rewritten & set(res.failed)
        _log(f"  after rewrite: {len(rewritten & set(res.failed))} now fail -> bug reports")
        cleaned = self._clean_up(new, report["removed"])
        return cleaned if cleaned is not None else code

    # ---------------------------------------------------------- Fix loop
    def _fix_loop(self, code, history):
        for r in range(1, self.s.fix_rounds + 1):
            comp = self._compile(code)
            if not comp.ok:
                _log(f"  fix {r}: {len(comp.errors)} compile error(s) -> Fixer")
                history.append({"round": r, "compile_errors": len(comp.errors)})
                code = agents.fix_compile(self.ctx, code, comp.raw, self.test_class)
                self._save_attempt(code, "fix_compile")
                continue
            res = self._run()
            if res.timeout:
                history.append({"round": r, "timeout": True})
                _log(f"  fix {r}: tests hang (timeout)")
                break
            failing = {n: m for n, m in res.failed.items() if n not in self.bug_reports}
            history.append({"round": r, "passed": len(res.passed), "failed": len(failing),
                            "bug_reports": len(self.bug_reports)})
            if not failing:
                _log(f"  fix {r}: all good ({len(res.passed)} pass"
                     + (f", {len(self.bug_reports)} bug reports)" if self.bug_reports else ")"))
                break
            if self.s.mode == "realistic" and self.s.panel:
                decisions = self._panel(code, failing)
                trusted = {n for n, d in decisions.items() if d == "TEST_CORRECT"}
                unsure = {n for n, d in decisions.items() if d == "UNSURE"}
                to_fix = {n: failing[n] for n, d in decisions.items() if d == "CODE_CORRECT"}
                self.bug_reports |= trusted
                _log(f"  fix {r}: {len(failing)} fail -> panel: {len(trusted)} bug reports, "
                     f"{len(to_fix)} wrong tests, {len(unsure)} unsure (dropped)")
                if unsure:
                    code = jp.remove_methods(code, unsure)
                if not to_fix:
                    continue
            else:
                to_fix = failing
                _log(f"  fix {r}: {len(res.passed)} pass, {len(failing)} fail -> Fixer")
            code = agents.fix_failures(self.ctx, code, to_fix, self.test_class)
            self.bug_reports &= set(jp.test_method_names(code))
            self._save_attempt(code, "fix_tests")
        return code

    # ---------------------------------------------------------- Clean up (no AI)
    def _clean_up(self, code, removed, flaky_runs=3):
        for _ in range(10):
            comp = self._compile(code)
            if not comp.ok:
                # 1. harmless mechanical fix: a wrong @Override label (changes nothing the test does)
                lines = code.splitlines()
                fixed = False
                for ln, msg in comp.errors:
                    if "does not override or implement a method" in msg:
                        for k in (ln - 1, ln - 2):          # the label is on the error line or just above
                            if 0 <= k < len(lines) and lines[k].strip().startswith("@Override"):
                                lines[k] = lines[k].replace("@Override", "", 1)
                                fixed = True
                                break
                if fixed:
                    code = "\n".join(lines) + "\n"
                    continue
                # 2. drop the test methods that contain an error (also errors inside helper
                #    classes written within a test: use the enclosing TEST method, not the innermost one)
                tests = set(jp.test_method_names(code))
                bad = {m.name for ln, _ in comp.errors for m in jp.methods_at_lines(code, [ln])} & tests
                if not bad:
                    return None          # error outside test methods: give up
                removed += [(m, "compile") for m in bad]
                code = jp.remove_methods(code, bad)
                self.bug_reports -= bad
                continue
            failed, passed = set(), set()
            for _ in range(flaky_runs):
                res = self._run()
                if res.timeout:
                    return None
                failed |= set(res.failed)
                passed |= set(res.passed)
            # Normal tests must always pass; bug reports must always fail.
            bad = (failed - self.bug_reports) | (self.bug_reports & passed)
            if not bad:
                return code if jp.test_method_names(code) else None
            # "flaky" = passed in some runs and failed in others (counted separately, as in the literature)
            removed += [(m, "flaky" if (m in failed and m in passed) else
                         ("fails" if m not in self.bug_reports else "flaky_report")) for m in bad]
            code = jp.remove_methods(code, bad)
            self.bug_reports -= bad
        return None

    def _validate(self, code, history, removed):
        code = self._fix_loop(code, history)
        return self._clean_up(code, removed)

    # ---------------------------------------------------------- Mutation challenge
    def _mutants(self, code):
        rules = mutation.rule_mutants(self.ctx.class_src, self.ctx.focal_methods,
                                      self.s.rule_mutants, seed=self.bug.name)
        props = agents.write_mutants(self.ctx, code, self.s.ai_mutants) if self.s.ai_mutants else []
        ais = mutation.ai_mutants(self.ctx.class_src, self.ctx.focal_methods, props)
        self.save.json("ai_mutant_proposals.json", props)
        _log(f"  mutant agent: {len(ais)} of {len(props)} AI mutants usable")
        return rules + ais

    def _measure(self, code, mutants):
        for m in mutants:
            m.status, m.killed_by = "new", []
        passing = set(jp.test_method_names(code)) - self.bug_reports
        self._compile(code)
        mutation.run_mutants(self.bug, self.gen, self.class_fqn, self.ctx.class_src, mutants,
                             self.ctx.package, self.test_class, passing)
        # "rule" = rule-based mutants only: the same set for every setting, so scores are comparable
        out = {"all": mutation.score(mutants),
               "rule": mutation.score([m for m in mutants if m.kind == "rule"]),
               "tests": len(jp.test_method_names(code))}
        if self.s.coverage:            # coverage after every round (as AdverTest's Figure 2)
            self._compile(code)
            cov = coverage.measure(self.bug, self.gen, self.class_fqn, self.ctx.focal_methods,
                                   self.ctx.package, self.test_class)
            out["coverage"] = (cov or {}).get("focal")
            self._missed_lines = (cov or {}).get("focal_missed_lines", [])
        return out

    def _uncovered_text(self):
        """The lines of the method(s) under test that no test runs yet (coverage feedback)."""
        missed = getattr(self, "_missed_lines", [])
        if not missed:
            return ""
        src = self.ctx.class_src.splitlines()
        shown = [f"  line {n}: {src[n - 1].strip()}" for n in missed[:25] if n <= len(src)]
        names = ", ".join(sorted({m.name for m in self.ctx.focal_methods}))
        return (f"\n\nCOVERAGE PROBLEM: the current tests never execute these lines of the method(s) "
                f"under test ({names}). Write tests whose inputs and setup make the code reach them:\n"
                + "\n".join(shown))

    def _challenge(self, code, report):
        mutants = self._mutants(code)
        if not mutants:
            return code, None
        rounds = [self._measure(code, mutants)]
        _log(f"  mutants: {rounds[0]}")
        for r in range(1, self.s.mutation_rounds + 1):
            survivors = [m for m in mutants if m.status == "survived"]
            uncovered = self._uncovered_text()
            if not survivors and not uncovered:
                break
            survivors.sort(key=lambda m: m.kind != "ai")          # AI mutants first
            text = "\n\n".join(mutation.describe(m, self.ctx.class_src) for m in survivors[:6])
            text = (text or "(none)") + uncovered
            extra = agents.kill_mutants(self.ctx, code, text, self.test_class)
            merged = jp.merge_methods(code, extra)
            if merged == code:
                break
            self._save_attempt(merged, "mutant_killers")
            new = self._validate(merged, report["fix_rounds"], report["removed"])
            if new is None:
                _log("  mutant-killing tests could not be validated; keeping previous suite")
                break
            code = new
            rounds.append(self._measure(code, mutants))
            _log(f"  challenge {r}: {rounds[-1]}")
        self.save.json("mutants.json", [
            {k: v for k, v in asdict(m).items() if k not in ("start", "end")} for m in mutants])
        return code, rounds

    # ---------------------------------------------------------- Evaluate
    def _evaluate(self, code):
        comp = self._compile(code, self.other)
        if not comp.ok:
            return {"other_version_compiles": False, "detected": False, "detecting_tests": [],
                    "compile_output": comp.raw[-2000:]}
        res = self._run(self.other)
        failed = set(res.failed)
        names = set(jp.test_method_names(code))
        out = {"other_version_compiles": True, "timeout": res.timeout}
        if self.s.mode == "standard":
            # every final test passes on fixed; failing on buggy = bug found
            out["detecting_tests"] = sorted(failed)
        else:
            # bug reports fail on buggy; passing on fixed = real bug, failing = false alarm
            out["detecting_tests"] = sorted(self.bug_reports - failed)
            out["false_alarms"] = sorted(self.bug_reports & failed)
            out["bug_encoding_tests"] = sorted((names - self.bug_reports) & failed)
        out["detected"] = bool(out["detecting_tests"]) and not res.timeout
        out["failures_on_other"] = res.failed
        return out

    # ---------------------------------------------------------- Main
    def run(self):
        s, bug = self.s, self.bug
        report = {"bug": bug.name, "class": self.class_fqn, "mode": s.mode,
                  "fix_rounds": [], "removed": []}

        # 1. Understand
        self.ctx = context.build(bug, self.class_fqn, self.gen, full=s.context)
        self.save.text("context.txt", self.ctx.text)
        report["focal_methods"] = [m.name for m in self.ctx.focal_methods]
        if not self.ctx.focal_methods:
            _log(f"{self.class_fqn}: no method to test - skipped ({self.ctx.skip_reason})")
            report["status"] = "no_focal_method"
            report["skip_reason"] = self.ctx.skip_reason
            return report
        _log(f"{self.class_fqn}: testing {report['focal_methods']} (on {'fixed' if self.gen == 'f' else 'buggy'})")
        self.test_class = f"{self.ctx.class_name}_CandorTest"

        # 2. Plan
        scenarios = agents.plan(self.ctx) if s.planner else None
        if scenarios is not None:
            self.save.json("plan.json", scenarios)
            _log(f"  planner: {len(scenarios)} scenarios")

        # 3. Write
        code = agents.write_tests(self.ctx, scenarios, self.test_class,
                                  spec_first=(s.mode == "realistic" and s.panel))
        self._save_attempt(code, "tester")
        report["generated_tests"] = len(jp.test_method_names(code))
        _log(f"  tester: {report['generated_tests']} test methods")
        report["first_compiles"] = self._compile(code).ok
        report["first_attempt"] = self._first_attempt_stats(code)

        # 4 + 5. Fix and clean up
        code = self._validate(code, report["fix_rounds"], report["removed"])
        if code is None:
            _log("  no usable tests left")
            report["status"] = "no_valid_tests"
            return report
        report["valid_tests_before_challenge"] = len(jp.test_method_names(code))

        # 6. Challenge with mutants
        if s.mutation_rounds > 0:
            code, rounds = self._challenge(code, report)
        else:
            # No challenge, but still measure the mutation score (rule mutants only, no AI cost)
            muts = mutation.rule_mutants(self.ctx.class_src, self.ctx.focal_methods,
                                         s.rule_mutants, seed=bug.name)
            rounds = [self._measure(code, muts)] if muts else None
        report["mutation"] = rounds
        # 6b. Realistic mode: the panel double-checks the tests that pass
        if s.mode == "realistic" and s.panel:
            code = self._spec_review(code, report)

        self.save.text("final.java", code)
        report["final_tests"] = len(jp.test_method_names(code))
        report["bug_reports"] = sorted(self.bug_reports)
        report["panel"] = self.panel_log

        # 7. Evaluate
        if s.coverage:
            self._compile(code)
            report["coverage"] = coverage.measure(bug, self.gen, self.class_fqn, self.ctx.focal_methods,
                                                  self.ctx.package, self.test_class)
        if s.pit:
            pit_code = jp.remove_methods(code, self.bug_reports) if self.bug_reports else code
            comp = runner.compile_test(bug, self.gen, self.ctx.package, self.test_class, pit_code, tag="pit")
            report["pit"] = (pit.measure(bug, self.gen, self.class_fqn, self.ctx.focal_methods, self.ctx.package,
                                         self.test_class, runner.build_dir(bug, self.gen, "pit") / "classes")
                             if comp.ok and jp.test_method_names(pit_code) else {"error": "no runnable tests"})
            _log(f"  PIT: {json.dumps(report['pit'])[:160]}")
        report["final_oracles"] = _count_oracles(code)
        report.update(self._evaluate(code))
        report["status"] = "ok"
        extra = (f", {len(report.get('false_alarms', []))} false alarms" if s.mode == "realistic" else "")
        _log(f"  final: {report['final_tests']} tests, {len(report['detecting_tests'])} detect the bug{extra}"
             f" -> {'BUG DETECTED' if report['detected'] else 'not detected'}")
        return report


def report_path(setting_name, bug_name):
    return RESULTS_DIR / setting_name / bug_name / "report.json"


def run_bug(bug_name, setting_name="full", settings=None, cleanup=False):
    """Run one bug with one experiment setting. Raises llm.QuotaExhausted if
    the free AI quota runs out (the bug is then not marked as done)."""
    s = settings or PRESETS[setting_name]
    project, bug_id = bug_name.rsplit("-", 1)
    bug = d4j.Bug(project, int(bug_id))
    t0 = time.time()
    llm.reset_stats()
    llm.RUN_TAG = f"repeat{s.repeat}" if s.repeat else ""
    _log(f"=== {bug.name} [{setting_name}] ===")
    bug.checkout("f")
    bug.checkout("b")

    out_dir = RESULTS_DIR / setting_name / bug.name
    if out_dir.exists():
        shutil.rmtree(out_dir)
    reports = []
    try:
        for class_fqn in bug.export("classes.modified").splitlines():
            save = _Saver(out_dir / class_fqn.rsplit(".", 1)[-1])
            try:
                reports.append(ClassRun(bug, class_fqn, s, save).run())
            except llm.QuotaExhausted:
                raise
            except llm.LLMError as e:
                # AI service or network problem: not the framework's fault -> redo this bug later
                _log(f"  AI/network problem in {class_fqn}: {str(e)[:200]}")
                reports.append({"bug": bug.name, "class": class_fqn, "status": "incomplete",
                                "error": str(e)[:2000]})
            except Exception as e:  # noqa: BLE001 - record and continue with the next class
                _log(f"  ERROR in {class_fqn}: {e}")
                reports.append({"bug": bug.name, "class": class_fqn, "status": "error", "error": str(e)[:2000]})
        std = None
        if s.standard_metrics and any(r.get("status") == "ok" for r in reports):
            _log("  standard metrics (Defects4J coverage + Major mutation)...")
            gen = "f" if s.mode == "standard" else "b"
            reports_bug = [t for r in reports for t in r.get("bug_reports", [])]
            std = standard_metrics.measure_results(
                bug.name, gen, [out_dir / r["class"].rsplit(".", 1)[-1] for r in reports if r.get("status") == "ok"],
                reports_bug)
            _log(f"  standard metrics: {json.dumps(std)[:200]}")
    finally:
        if cleanup:
            for v in ("b", "f"):
                shutil.rmtree(bug.workdir(v), ignore_errors=True)
                shutil.rmtree(runner.build_dir(bug, v, "").parent, ignore_errors=True)
            runner.forget_classpath(bug)

    summary = {
        "bug": bug.name,
        "setting": setting_name,
        "settings": asdict(s),
        "detected": any(r.get("detected") for r in reports),
        "incomplete": any(r.get("status") == "incomplete" for r in reports),
        "standard_metrics": std,
        "seconds": round(time.time() - t0, 1),
        "llm_calls": json.loads(json.dumps(llm.stats)),
        "classes": reports,
    }
    _Saver(out_dir).json("report.json", summary)
    return summary
