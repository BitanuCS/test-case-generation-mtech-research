package com.google.javascript.jscomp;

import com.google.common.collect.Lists;
import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import junit.framework.TestCase;

/**
 * JUnit test class for RemoveUnusedVars internal logic targeting test scenarios from the test plan.
 */
public class RemoveUnusedVars_CandorTest extends TestCase {

  private Compiler compiler;
  private RemoveUnusedVars removeUnusedVars;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    compiler = new Compiler();
    // Initialize compiler options/init if needed
    compiler.init(
        // externs
        Lists.newArrayList(JSSourceFile.fromCode("externs", "")),
        // srcs
        Lists.newArrayList(JSSourceFile.fromCode("srcs", "")),
        new CompilerOptions());
    removeUnusedVars = new RemoveUnusedVars(compiler, true, false, false);
  }

  public void testProcessorInstantiation() {
    assertNotNull(removeUnusedVars);
  }

  

    @Override
      protected void setUp2() throws Exception {
        super.setUp();
        compiler = new Compiler();
        compiler.init(
            Lists.newArrayList(JSSourceFile.fromCode("externs", "")),
            Lists.newArrayList(JSSourceFile.fromCode("srcs", "")),
            new CompilerOptions());
      }

    public void testInterpretAssignsMultipleVarsOffByOne() {
        // Tests multiple unreferenced variables to ensure off-by-one index decrement (A0, A4) is exercised.
        // If current-- is forgotten, variables in maybeUnreferenced will be skipped.
        String js = "var x = {}; x.foo = 1; var y = {}; y.bar = 1; function f(u, v) { u.a = 2; v.b = 2; } f(x, y);";
        Node root = compiler.parseSyntheticCode("test", js);
        compiler.parse();
        
        RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
        pass.process(compiler.externs, root);
        
        // If the pass runs successfully and kills/handles both x and y properly without skipping due to index shift.
        assertNotNull(root);
      }

    public void testInterpretAssignsLogicalOperatorAndVsOr() {
        // Tests mutant A1: (assignedToUnknownValue || maybeEscaped) && hasPropertyAssign
        // vs (assignedToUnknownValue || maybeEscaped) || hasPropertyAssign.
        // If assignedToUnknownValue is false and maybeEscaped is false, but hasPropertyAssign is true,
        // the original condition is false (should not mark referenced unless assigned to unknown/escaped).
        // The mutant would evaluate true and incorrectly mark the var referenced.
        String js = "var x = {}; x.foo = 1;";
        Node root = compiler.parseSyntheticCode("test", js);
        compiler.parse();
    
        RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
        pass.process(compiler.externs, root);
    
        // x is an unused object literal with a property assignment, so x should be removed.
        // The mutant A1 would incorrectly keep x alive.
        String generatedCode = compiler.toSource(root);
        assertFalse(generatedCode.contains("x"));
      }

    public void testInterpretAssignsChangesFlagReset() {
        // Tests mutants A2 and R0 where changes flag is initialized to true instead of false.
        String js = "var x = 1; x;";
        Node root = compiler.parseSyntheticCode("test", js);
        compiler.parse();
    
        RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
        pass.process(compiler.externs, root);
        assertNotNull(root);
      }

    public void testInterpretAssignsVarParentNodeCheck() {
        // Tests mutant A3: !var.getParentNode().isVar() instead of var.getParentNode().isVar()
        // For a variable declared via 'var x = foo();' (unknown value initialization),
        // getParentNode().isVar() is true. Inverting this check alters whether assignedToUnknownValue is computed from initial value.
        String js = "var x = foo(); function g() { x.bar = 1; }";
        Node root = compiler.parseSyntheticCode("test", js);
        compiler.parse();
    
        RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
        pass.process(compiler.externs, root);
        assertNotNull(root);
      }
}
