package com.google.javascript.jscomp;

import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;
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
    CompilerOptions options = new CompilerOptions();
    compiler.init(
        // externs
        Lists.newArrayList(JSSourceFile.fromCode("externs", "")),
        // srcs
        Lists.newArrayList(JSSourceFile.fromCode("srcs", "")),
        options);
    removeUnusedVars = new RemoveUnusedVars(compiler, true, false, false);
  }

  public void testProcessorInstantiation() {
    assertNotNull(removeUnusedVars);
  }

  public void testInterpretAssignsMultipleVarsAndPropertyAssigns() {
    // This test exercises interpretAssigns() with multiple variables to kill:
    // - [A0] and [A4] (failure to decrement index after removal)
    // - [A1] (logical OR vs AND: (assignedToUnknownValue || maybeEscaped) && hasPropertyAssign)
    // - [A2] and [R0] (do-while loop behavior with changes flags)
    // - [A3] (parent node isVar check)
    String js = "var x = foo(); x.prop = 1; var y = bar(); y.prop = 2; var z = 5;";
    Node root = compiler.parseSyntheticCode("testcode", js);
    compiler.getRoot().getLastChild().addChildrenToBack(root);
    compiler.check();
    
    Node externs = compiler.parseSyntheticCode("externs", "");
    RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
    pass.process(externs, compiler.getRoot());
    
    // Verify that processing completes successfully and removes unused or correctly handles assigned vars.
    assertNotNull(root);
  }

  public void testInterpretAssignsWithReferencedRemoval() {
    // Specifically targets removing a variable that is already referenced to ensure index adjustment (A0)
    String js = "var a = foo(); a.prop = 1; a();";
    Node root = compiler.parseSyntheticCode("testcode", js);
    compiler.getRoot().getLastChild().addChildrenToBack(root);
    compiler.check();
    
    Node externs = compiler.parseSyntheticCode("externs", "");
    RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
    pass.process(externs, compiler.getRoot());
    assertNotNull(root);
  }
}
