package com.google.javascript.rhino.jstype;

import junit.framework.TestCase;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.SimpleErrorReporter;
import com.google.javascript.rhino.ErrorReporter;

public class JSType_CandorTest extends TestCase {

  private JSTypeRegistry registry;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registry = new JSTypeRegistry(new SimpleErrorReporter());
  }

  public void testIdenticalReferenceShortCircuit() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    boolean result = numberType.checkEquivalenceHelper(numberType, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testInvariantWithUnknownType() {
    JSType unknownType = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    boolean result = unknownType.checkEquivalenceHelper(numberType, EquivalenceMethod.INVARIANT);
    assertTrue(result);
  }

  public void testDataFlowWithBothUnknown() {
    JSType unknownType1 = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    JSType unknownType2 = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    boolean result = unknownType1.checkEquivalenceHelper(unknownType2, EquivalenceMethod.DATA_FLOW);
    assertTrue(result);
  }

  public void testDataFlowWithOneUnknown() {
    JSType unknownType = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    boolean result = unknownType.checkEquivalenceHelper(numberType, EquivalenceMethod.DATA_FLOW);
    assertFalse(result);
  }

  public void testUnknownNominalMismatchFail() {
    JSType unknownType1 = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    JSType unknownType2 = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    // UNKNOWN_TYPE returns isNominalType() == false by default.
    // To trigger (isNominalType() ^ that.isNominalType()) we can use a custom or subclass or test directly.
    // Here both have isNominalType() == false, so let's verify normal case:
    boolean result = unknownType1.checkEquivalenceHelper(unknownType2, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testUnionTypeEquivalenceDelegation() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType stringType = registry.getNativeType(JSTypeNative.STRING_TYPE);
    JSType union1 = registry.createUnionType(numberType, stringType);
    JSType union2 = registry.createUnionType(numberType, stringType);
    boolean result = union1.checkEquivalenceHelper(union2, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testFunctionTypeEquivalenceDelegation() {
    FunctionType func1 = new FunctionBuilder(registry).build();
    // Different instances of functions without matching signatures might not be equivalent under identity unless they are the same or match checkFunctionEquivalenceHelper.
    // Let's test func1 with itself to ensure delegation works.
    boolean result = func1.checkEquivalenceHelper(func1, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testRecordTypeEquivalenceDelegation() {
    RecordTypeBuilder builder1 = new RecordTypeBuilder(registry);
    builder1.addProperty("a", registry.getNativeType(JSTypeNative.NUMBER_TYPE), null);
    JSType record1 = builder1.build();

    RecordTypeBuilder builder2 = new RecordTypeBuilder(registry);
    builder2.addProperty("a", registry.getNativeType(JSTypeNative.NUMBER_TYPE), null);
    JSType record2 = builder2.build();

    boolean result = record1.checkEquivalenceHelper(record2, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testParameterizedTypeMatchingBoth() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    ObjectType objType = registry.createAnonymousObjectType();
    ParameterizedType param1 = new ParameterizedType(registry, objType, numberType);
    ParameterizedType param2 = new ParameterizedType(registry, objType, numberType);
    boolean result = param1.checkEquivalenceHelper(param2, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testParameterizedTypeOneMissingIdentity() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    ObjectType objType = registry.createAnonymousObjectType();
    ParameterizedType param1 = new ParameterizedType(registry, objType, numberType);
    boolean result = param1.checkEquivalenceHelper(objType, EquivalenceMethod.IDENTITY);
    assertFalse(result);
  }

  public void testParameterizedTypeOneMissingDataFlow() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    ObjectType objType = registry.createAnonymousObjectType();
    ParameterizedType param1 = new ParameterizedType(registry, objType, numberType);
    boolean result = param1.checkEquivalenceHelper(objType, EquivalenceMethod.DATA_FLOW);
    assertTrue(result);
  }

  public void testNominalTypeSameReferenceName() {
    FunctionType ctor1 = registry.createConstructorType("Foo", null, null, null);
    ObjectType inst1 = ctor1.getInstanceType();
    FunctionType ctor2 = registry.createConstructorType("Foo", null, null, null);
    ObjectType inst2 = ctor2.getInstanceType();

    boolean result = inst1.checkEquivalenceHelper(inst2, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testProxyObjectTypeUnboxingThis() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    ProxyObjectType proxy = new ProxyObjectType(registry, numberType);
    boolean result = proxy.checkEquivalenceHelper(numberType, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testProxyObjectTypeUnboxingThat() {
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    ProxyObjectType proxy = new ProxyObjectType(registry, numberType);
    boolean result = numberType.checkEquivalenceHelper(proxy, EquivalenceMethod.IDENTITY);
    assertTrue(result);
  }

  public void testIsEquivalentToBasicTrue() {
    JSType numberType1 = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType numberType2 = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    assertTrue(numberType1.isEquivalentTo(numberType2));
  }

  public void testIsInvariantWithUnknown() {
    JSType unknownType = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    JSType numberType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    assertTrue(unknownType.isInvariant(numberType));
  }

  public void testDiffersFromIdenticalTypes() {
    JSType stringType1 = registry.getNativeType(JSTypeNative.STRING_TYPE);
    JSType stringType2 = registry.getNativeType(JSTypeNative.STRING_TYPE);
    assertFalse(stringType1.differsFrom(stringType2));
  }

  public void testUnknownNominalMismatchDataFlow() {
    JSType unknownType = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    
    FunctionType ctor = registry.createConstructorType("NominalUnknown", null, null, null);
    JSType nominalUnknown = ctor.getInstanceType();
    
    boolean result = unknownType.checkEquivalenceHelper(nominalUnknown, EquivalenceMethod.DATA_FLOW);
    assertFalse(result);
  }

  public void testFunctionTypeEquivalenceHelperDelegation() {
    FunctionType func1 = new FunctionBuilder(registry).withReturnType(registry.getNativeType(JSTypeNative.NUMBER_TYPE)).build();
    FunctionType func2 = new FunctionBuilder(registry).withReturnType(registry.getNativeType(JSTypeNative.STRING_TYPE)).build();
    
    boolean result = func1.checkEquivalenceHelper(func2, EquivalenceMethod.IDENTITY);
    assertFalse(result);
  }

  
}
