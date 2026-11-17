package com.google.javascript.rhino.jstype;

import com.google.common.collect.ImmutableList;
import com.google.javascript.rhino.Node;
import junit.framework.TestCase;
import static com.google.javascript.rhino.jstype.JSTypeNative.OBJECT_TYPE;

/**
 * JUnit 3 test class for FunctionType methods under test.
 */
public class FunctionType_CandorTest extends TestCase {

  private JSTypeRegistry registry;
  private FunctionType ordinaryFn1;
  private FunctionType ordinaryFn2;
  private FunctionType ordinaryFn3;
  private FunctionType constructorFn1;
  private FunctionType constructorFn2;
  private FunctionType interfaceFnFoo1;
  private FunctionType interfaceFnFoo2;
  private FunctionType interfaceFnBar;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registry = new JSTypeRegistry(null);

    Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
    ArrowType arrow1 = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
    ArrowType arrow2 = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.STRING_TYPE), false);
    ArrowType arrow3 = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);

    ObjectType typeOfThis1 = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);

    ordinaryFn1 = new FunctionType(registry, "fn1", null, arrow1, typeOfThis1, null, false, false);
    ordinaryFn2 = new FunctionType(registry, "fn2", null, arrow2, typeOfThis1, null, false, false);
    ordinaryFn3 = new FunctionType(registry, "fn3", null, arrow3, typeOfThis1, null, false, false);

    constructorFn1 = new FunctionType(registry, "Ctor1", null, arrow1, typeOfThis1, null, true, false);
    constructorFn2 = new FunctionType(registry, "Ctor2", null, arrow1, typeOfThis1, null, true, false);

    interfaceFnFoo1 = FunctionType.forInterface(registry, "Foo", null);
    interfaceFnFoo2 = FunctionType.forInterface(registry, "Foo", null);
    interfaceFnBar = FunctionType.forInterface(registry, "Bar", null);
  }

  public void testTryMergeWithDifferentParametersReturnsNull() {
    Node differentParamsNode = new Node(com.google.javascript.rhino.Token.LP);
    differentParamsNode.addChildToBack(Node.newString(com.google.javascript.rhino.Token.NAME, "a"));
    ArrowType differentArrow = new ArrowType(registry, differentParamsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
    FunctionType diffFn = new FunctionType(registry, "diff", null, differentArrow, ordinaryFn1.getTypeOfThis(), null, false, false);

    assertFalse(ordinaryFn1.hasEqualCallType(diffFn));
  }

  public void testConstructorsEqualWhenSameReference() {
    assertTrue(constructorFn1.checkFunctionEquivalenceHelper(constructorFn1, EquivalenceMethod.IDENTITY));
  }

  public void testConstructorsNotEqualWhenDifferentReferences() {
    assertFalse(constructorFn1.checkFunctionEquivalenceHelper(constructorFn2, EquivalenceMethod.IDENTITY));
  }

  public void testConstructorComparedToNonConstructorReturnsFalse() {
    assertFalse(constructorFn1.checkFunctionEquivalenceHelper(ordinaryFn1, EquivalenceMethod.IDENTITY));
  }

  public void testInterfacesEqualWhenNamesMatch() {
    assertTrue(interfaceFnFoo1.checkFunctionEquivalenceHelper(interfaceFnFoo2, EquivalenceMethod.IDENTITY));
  }

  public void testInterfacesNotEqualWhenNamesDiffer() {
    assertFalse(interfaceFnFoo1.checkFunctionEquivalenceHelper(interfaceFnBar, EquivalenceMethod.IDENTITY));
  }

  public void testInterfaceComparedToNonInterfaceReturnsFalse() {
    assertFalse(interfaceFnFoo1.checkFunctionEquivalenceHelper(ordinaryFn1, EquivalenceMethod.IDENTITY));
  }

  public void testNonInterfaceComparedToInterfaceReturnsFalse() {
    assertFalse(ordinaryFn1.checkFunctionEquivalenceHelper(interfaceFnFoo1, EquivalenceMethod.IDENTITY));
  }

  public void testOrdinaryFunctionsEquivalentSignatures() {
    assertTrue(ordinaryFn1.checkFunctionEquivalenceHelper(ordinaryFn3, EquivalenceMethod.IDENTITY));
  }

  public void testOrdinaryFunctionsDifferentArrowType() {
    assertFalse(ordinaryFn1.checkFunctionEquivalenceHelper(ordinaryFn2, EquivalenceMethod.IDENTITY));
  }

  public void testHasEqualCallTypeTrue() {
    assertTrue(ordinaryFn1.hasEqualCallType(ordinaryFn3));
  }

  public void testHasEqualCallTypeFalse() {
    assertFalse(ordinaryFn1.hasEqualCallType(ordinaryFn2));
  }
}
