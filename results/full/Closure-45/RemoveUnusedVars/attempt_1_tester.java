package com.google.javascript.jscomp;

import com.google.javascript.rhino.IR;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import junit.framework.TestCase;

/**
 * JUnit test class for RemoveUnusedVars internal logic (Assign, interpretAssigns, etc.)
 * targeting test scenarios from the test plan.
 */
public class RemoveUnusedVars_CandorTest extends TestCase {

  public void testAssignWithValidAssignmentOp() {
    Node assignNode = IR.assign(IR.name("x"), IR.number(5));
    Node nameNode = IR.name("x");
    boolean isPropertyAssign = false;

    RemoveUnusedVars.Assign assign = new RemoveUnusedVars.Assign(assignNode, nameNode, isPropertyAssign);
    assertNotNull(assign);
  }

  public void testAssignThrowsExceptionOnNonAssignmentOp() {
    Node notAssignNode = IR.add(IR.number(5), IR.number(3));
    Node nameNode = IR.name("x");
    boolean isPropertyAssign = false;

    try {
      new RemoveUnusedVars.Assign(notAssignNode, nameNode, isPropertyAssign);
      fail("Expected IllegalStateException due to non-assignment operator");
    } catch (IllegalStateException e) {
      // Expected
    }
  }

  public void testAssignWithAliasedExpressionResultUsed() {
    // Construct an assignment whose expression result is used (e.g. inside an expression)
    Node innerAssign = IR.assign(IR.name("y"), IR.number(5));
    Node assignNode = IR.assign(IR.name("x"), innerAssign);
    // Mark parent so expression result is used
    Node exprResult = IR.exprResult(assignNode);

    Node nameNode = IR.name("x");
    boolean isPropertyAssign = false;

    RemoveUnusedVars.Assign assign = new RemoveUnusedVars.Assign(assignNode, nameNode, isPropertyAssign);
    assertNotNull(assign);
  }

  public void testAssignWithSideEffectFirstChild() {
    // getObj().foo = 5
    Node getProp = IR.getprop(IR.call(IR.name("getObj")), IR.string("foo"));
    Node assignNode = IR.assign(getProp, IR.number(5));
    Node nameNode = IR.name("obj");
    boolean isPropertyAssign = true;

    RemoveUnusedVars.Assign assign = new RemoveUnusedVars.Assign(assignNode, nameNode, isPropertyAssign);
    assertNotNull(assign);
  }

  public void testAssignWithSideEffectLastChild() {
    // x = sideEffectFunction()
    Node callNode = IR.call(IR.name("sideEffectFunction"));
    Node assignNode = IR.assign(IR.name("x"), callNode);
    Node nameNode = IR.name("x");
    boolean isPropertyAssign = false;

    RemoveUnusedVars.Assign assign = new RemoveUnusedVars.Assign(assignNode, nameNode, isPropertyAssign);
    assertNotNull(assign);
  }
}
