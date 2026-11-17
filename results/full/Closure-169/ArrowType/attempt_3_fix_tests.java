package com.google.javascript.rhino.jstype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSTypeRegistry;
import org.junit.Test;

public class ArrowType_CandorTest {

  // Helper method to create a parameter Node with an optional JSType attached
  private static Node createParamNode(JSType type) {
    Node node = new Node(0);
    if (type != null) {
      node.setJSType(type);
    }
    return node;
  }

  // Helper method to build a parameter list Node (where the first child contains the actual params)
  private static Node createParameterList(Node... paramNodes) {
    Node parent = new Node(0);
    Node last = parent;
    for (Node p : paramNodes) {
      last.addChildToBack(p);
      last = p;
    }
    return parent;
  }

  @Test
  public void bothReturnTypesAndParametersEqual() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeA));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.checkArrowEquivalenceHelper(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void returnTypesNotEqual() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType returnTypeB = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeA));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeB);

    assertFalse(thisArrow.checkArrowEquivalenceHelper(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void bothParameterListsEmpty() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList();
    Node thatParams = createParameterList();

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void thisHasParamWithNullTypeOtherHasParamWithNonNullType() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeB = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(null));
    Node thatParams = createParameterList(createParamNode(paramTypeB));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void thisHasParamWithNonNullTypeOtherHasParamWithNullType() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(null));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertFalse(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void bothParamsNonNullButEquivalenceFails() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);
    JSType paramTypeB = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeB));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertFalse(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void bothParamsNonNullAndEquivalenceSucceeds() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeA));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void differentParameterListLengthsThisLonger() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA), createParamNode(null));
    Node thatParams = createParameterList(createParamNode(paramTypeA));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void differentParameterListLengthsThatLonger() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeA), createParamNode(null));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void multipleParametersAllMatch() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);
    JSType paramTypeB = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA), createParamNode(paramTypeB));
    Node thatParams = createParameterList(createParamNode(paramTypeA), createParamNode(paramTypeB));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertTrue(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void multipleParametersMismatchOnSecond() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);
    JSType paramTypeB = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA), createParamNode(paramTypeB));
    Node thatParams = createParameterList(createParamNode(paramTypeA), createParamNode(null));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertFalse(thisArrow.hasEqualParameters(thatArrow, EquivalenceMethod.IDENTITY));
  }

  @Test
  public void returnTypesEqualButParametersDiffer() {
    JSTypeRegistry registry = null;
    JSType returnTypeA = new DummyJSType(registry);
    JSType paramTypeA = new DummyJSType(registry);
    JSType paramTypeB = new DummyJSType(registry);

    Node thisParams = createParameterList(createParamNode(paramTypeA));
    Node thatParams = createParameterList(createParamNode(paramTypeB));

    ArrowType thisArrow = new ArrowType(registry, thisParams, returnTypeA);
    ArrowType thatArrow = new ArrowType(registry, thatParams, returnTypeA);

    assertFalse(thisArrow.checkArrowEquivalenceHelper(thatArrow, EquivalenceMethod.IDENTITY));
  }

  /**
   * Minimal concrete subclass of JSType to satisfy JSType references without complex initialization.
   */
  private static class DummyJSType extends JSType {
    private static final long serialVersionUID = 1L;

    DummyJSType(JSTypeRegistry registry) {
      super(registry);
    }

    @Override
    public boolean checkEquivalenceHelper(JSType that, EquivalenceMethod eqMethod) {
      return this == that;
    }

    @Override
    public boolean isSubtype(JSType other) {
      return false;
    }

    @Override
    public JSType getLeastSupertype(JSType that) {
      return this;
    }

    @Override
    public JSType getGreatestSubtype(JSType that) {
      return this;
    }

    @Override
    public TernaryValue testForEquality(JSType that) {
      return TernaryValue.UNKNOWN;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return null;
    }

    @Override
    public BooleanLiteralSet getPossibleToBooleanOutcomes() {
      return BooleanLiteralSet.BOTH;
    }

    @Override
    JSType resolveInternal(com.google.javascript.rhino.ErrorReporter t, StaticScope<JSType> scope) {
      return this;
    }

    @Override
    String toStringHelper(boolean forAnnotations) {
      return "DummyJSType";
    }

    @Override
    public boolean hasAnyTemplateInternal() {
      return false;
    }
  }
}
