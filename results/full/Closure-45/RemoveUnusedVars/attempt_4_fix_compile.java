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

  public void testProcessRun() {
    Node root = new Node(Token.SCRIPT);
    removeUnusedVars.process(root, root);
  }
}
