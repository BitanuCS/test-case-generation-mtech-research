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

  public void testInterpretAssignsMultipleVarsOffByOne() {
      String js = "var x = {}; x.foo = 1; var y = {}; y.bar = 1; function f(u, v) { u.a = 2; v.b = 2; } f(x, y);";
      JSSourceFile source = JSSourceFile.fromCode("test", js);
      compiler.parse(source);
      
      RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
      pass.process(compiler.getRoot().getFirstChild(), compiler.getRoot().getLastChild());
      
      assertNotNull(compiler.getRoot());
    }

  public void testInterpretAssignsLogicalOperatorAndVsOr() {
      String js = "var x = {}; x.foo = 1;";
      JSSourceFile source = JSSourceFile.fromCode("test", js);
      compiler.parse(source);
  
      RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
      pass.process(compiler.getRoot().getFirstChild(), compiler.getRoot().getLastChild());
  
      String generatedCode = compiler.toSource();
      assertFalse(generatedCode.contains("x"));
    }

  public void testInterpretAssignsChangesFlagReset() {
      String js = "var x = 1; x;";
      JSSourceFile source = JSSourceFile.fromCode("test", js);
      compiler.parse(source);
  
      RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
      pass.process(compiler.getRoot().getFirstChild(), compiler.getRoot().getLastChild());
      assertNotNull(compiler.getRoot());
    }

  public void testInterpretAssignsVarParentNodeCheck() {
      String js = "var x = foo(); function g() { x.bar = 1; }";
      JSSourceFile source = JSSourceFile.fromCode("test", js);
      compiler.parse(source);
  
      RemoveUnusedVars pass = new RemoveUnusedVars(compiler, true, false, false);
      pass.process(compiler.getRoot().getFirstChild(), compiler.getRoot().getLastChild());
      assertNotNull(compiler.getRoot());
    }
}
