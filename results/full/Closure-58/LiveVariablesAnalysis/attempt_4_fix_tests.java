package com.google.javascript.jscomp;

import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LiveVariablesAnalysis_CandorTest {

  private Scope scope;
  private LiveVariablesAnalysis analysis;
  private Method computeGenKillMethod;

  @Before
  public void setUp() throws Exception {
    Compiler compiler = new Compiler();
    Node root = new Node(Token.BLOCK);
    // Create function root with parameter list (lp)
    Node fn = new Node(Token.FUNCTION, Node.newString(Token.NAME, "f"), new Node(Token.LP));
    root.addChildToBack(fn);

    scope = new Scope(fn, compiler);
    
    // Declare some test variables in scope
    scope.declare("x", Node.newString(Token.NAME, "x"), null, null);
    scope.declare("y", Node.newString(Token.NAME, "y"), null, null);

    ControlFlowGraph<Node> cfg = new ControlFlowGraph<>(root, false, false);
    analysis = new LiveVariablesAnalysis(cfg, scope, compiler);

    computeGenKillMethod = LiveVariablesAnalysis.class.getDeclaredMethod(
        "computeGenKill", Node.class, BitSet.class, BitSet.class, boolean.class);
    computeGenKillMethod.setAccessible(true);
  }

  private void invokeComputeGenKill(Node n, BitSet gen, BitSet kill, boolean conditional) throws Exception {
    computeGenKillMethod.invoke(analysis, n, gen, kill, conditional);
  }

  @Test
  public void scriptBlockFunctionNodesReturnEarly() throws Exception {
    Node block = new Node(Token.BLOCK);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(block, gen, kill, false);

    assertTrue(gen.isEmpty());
    assertTrue(kill.isEmpty());
  }

  @Test
  public void whileNodeRecursesCondition() throws Exception {
    Node nameX = Node.newString(Token.NAME, "x");
    Node whileNode = new Node(Token.WHILE, nameX, new Node(Token.BLOCK));
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(whileNode, gen, kill, false);

    // 'x' is a local declared variable, so it should be added to gen via condition recursion
    assertTrue(gen.get(scope.getVar("x").index));
    assertTrue(kill.isEmpty());
  }

  @Test
  public void nameNodeAddsToGen() throws Exception {
    Node nameX = Node.newString(Token.NAME, "x");
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(nameX, gen, kill, false);

    assertTrue(gen.get(scope.getVar("x").index));
    assertTrue(kill.isEmpty());
  }

  @Test
  public void argumentsNameTriggersMarkAllParametersEscaped() throws Exception {
    Node argsName = Node.newString(Token.NAME, "arguments");
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(argsName, gen, kill, false);

    // Should call markAllParametersEscaped successfully without error
    assertTrue(gen.isEmpty());
    assertTrue(kill.isEmpty());
  }

  @Test
  public void simpleAssignmentKillsAndGensRhs() throws Exception {
    Node lhs = Node.newString(Token.NAME, "x");
    Node rhs = Node.newString(Token.NAME, "y");
    Node assign = new Node(Token.ASSIGN, lhs, rhs);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(assign, gen, kill, false);

    // 'x' killed, 'y' genned
    assertTrue(kill.get(scope.getVar("x").index));
    assertTrue(gen.get(scope.getVar("y").index));
  }

  @Test
  public void compoundAssignmentAddsLhsToGenAndKill() throws Exception {
    Node lhs = Node.newString(Token.NAME, "x");
    Node rhs = Node.newNumber(1.0);
    Node assignAdd = new Node(Token.ASSIGN_ADD, lhs, rhs);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(assignAdd, gen, kill, false);

    // 'x' killed and genned because compound assignment reads 'x'
    assertTrue(kill.get(scope.getVar("x").index));
    assertTrue(gen.get(scope.getVar("x").index));
  }

  @Test
  public void conditionalAssignmentDoesNotKill() throws Exception {
    Node lhs = Node.newString(Token.NAME, "x");
    Node rhs = Node.newNumber(1.0);
    Node assign = new Node(Token.ASSIGN, lhs, rhs);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(assign, gen, kill, true);

    // conditional = true means 'x' is NOT killed
    assertTrue(kill.isEmpty());
  }

  @Test
  public void varDeclarationWithoutConditionalKillsAndGensInitializer() throws Exception {
    Node nameX = Node.newString(Token.NAME, "x");
    Node nameY = Node.newString(Token.NAME, "y");
    nameX.addChildToBack(nameY);
    Node varNode = new Node(Token.VAR, nameX);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(varNode, gen, kill, false);

    // 'y' genned (initializer), 'x' killed (conditional is false)
    assertTrue(gen.get(scope.getVar("y").index));
    assertTrue(kill.get(scope.getVar("x").index));
  }

  @Test
  public void varDeclarationWithConditionalDoesNotKill() throws Exception {
    Node nameX = Node.newString(Token.NAME, "x");
    Node nameY = Node.newString(Token.NAME, "y");
    nameX.addChildToBack(nameY);
    Node varNode = new Node(Token.VAR, nameX);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(varNode, gen, kill, true);

    // 'y' genned, but 'x' NOT killed because conditional is true
    assertTrue(gen.get(scope.getVar("y").index));
    assertTrue(kill.isEmpty());
  }

  @Test
  public void andOrShortCircuitSetsConditionalTrueForRhs() throws Exception {
    Node lhs = Node.newString(Token.NAME, "x");
    Node rhs = Node.newString(Token.NAME, "y");
    Node andNode = new Node(Token.AND, lhs, rhs);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(andNode, gen, kill, false);

    // Both 'x' and 'y' should be added to gen
    assertTrue(gen.get(scope.getVar("x").index));
    assertTrue(gen.get(scope.getVar("y").index));
  }

  @Test
  public void hookTernaryOperatorForcesConditionalOnBranches() throws Exception {
    Node cond = Node.newString(Token.NAME, "x");
    Node thenBranch = Node.newString(Token.NAME, "y");
    Node elseBranch = Node.newString(Token.NAME, "x");
    Node hookNode = new Node(Token.HOOK, cond, thenBranch);
    hookNode.addChildToBack(elseBranch);

    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(hookNode, gen, kill, false);

    assertTrue(gen.get(scope.getVar("x").index));
    assertTrue(gen.get(scope.getVar("y").index));
  }

  @Test
  public void standardForLoopConditionAndBody() throws Exception {
    Node cond = Node.newString(Token.NAME, "x");
    Node forNode = new Node(Token.FOR, cond, new Node(Token.BLOCK));
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(forNode, gen, kill, false);

    assertTrue(gen.get(scope.getVar("x").index));
  }

  @Test
  public void defaultTreeNodeTraversal() throws Exception {
    Node nameX = Node.newString(Token.NAME, "x");
    Node exprResult = new Node(Token.EXPR_RESULT, nameX);
    BitSet gen = new BitSet();
    BitSet kill = new BitSet();

    invokeComputeGenKill(exprResult, gen, kill, false);

    assertTrue(gen.get(scope.getVar("x").index));
  }
}
