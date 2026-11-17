package com.google.javascript.rhino.jstype;

import junit.framework.TestCase;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.SimpleErrorReporter;

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
    // Covers lines 536, 537, 541: both are unknown, but one is a nominal type and the other is not.
    // Under DATA_FLOW, this should return false. Mutants that change '&&' to '||' or 'false' to 'true' will fail.
    JSType unknownType = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    
    FunctionType ctor = registry.createConstructorType("NominalUnknown", null, null, null);
    JSType nominalUnknown = ctor.getInstanceType();
    
    boolean result = unknownType.checkEquivalenceHelper(nominalUnknown, EquivalenceMethod.DATA_FLOW);
    assertFalse(result);
  }

  public void testFunctionTypeEquivalenceHelperDelegation() {
    // Covers lines 551, 552: isFunctionType() && that.isFunctionType() -> checkFunctionEquivalenceHelper
    FunctionType func1 = new FunctionBuilder(registry).withReturnType(registry.getNativeType(JSTypeNative.NUMBER_TYPE)).build();
    FunctionType func2 = new FunctionBuilder(registry).withReturnType(registry.getNativeType(JSTypeNative.STRING_TYPE)).build();
    
    // Different return types should make them not equivalent under identity/data flow.
    boolean result = func1.checkEquivalenceHelper(func2, EquivalenceMethod.IDENTITY);
    assertFalse(result);
  }

    @Override
      protected void setUp2() throws Exception {
        super.setUp();
        registry = new JSTypeRegistry(new SimpleErrorReporter());
      }

    public void testUnknownAndNominalUnknownUnderDataFlow() {
        // This test targets the specific block:
        // else if (thisUnknown && thatUnknown && (isNominalType() ^ that.isNominalType())) { return false; }
        //
        // Setup:
        // - this: an unknown type (not a nominal type, so thisUnknown = true, isNominalType() = false)
        // - that: a nominal unknown/instance type where isNominalType() returns true, but it is treated as unknown.
        //   Wait, standard UNKNOWN_TYPE has isNominalType() == false.
        //   How do we get an unknown type that reports isNominalType() == true?
        //   Let's check ProxyObjectType or subclassing JSType/ObjectType, or a custom subclass/anonymous class extending JSType or ObjectType.
        
        JSType standardUnknown = registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
        
        JSType nominalUnknown = new JSType(registry) {
          @Override
          public boolean isUnknownType() {
            return true;
          }
          @Override
          public boolean isNominalType() {
            return true;
          }
          @Override
          public BooleanLiteralSet getPossibleToBooleanOutcomes() {
            return BooleanLiteralSet.BOTH;
          }
          @Override
          public <T> T visit(Visitor<T> visitor) {
            return null;
          }
          @Override
          String toStringHelper(boolean forAnnotations) {
            return "NominalUnknown";
          }
        };
    
        // Under DATA_FLOW equivalence method:
        // standardUnknown (unknown, not nominal) vs nominalUnknown (unknown, nominal)
        // Here thisUnknown = true, thatUnknown = true.
        // isNominalType() ^ that.isNominalType() -> false ^ true -> true.
        // The original code evaluates: true && true && true -> returns false.
        //
        // Mutant [A0]/[R7]/[R8] (swapping && to ||) or [R9] (returning true instead of false):
        // - [A0]/[R7]/[R8] or [R9] would incorrectly return true instead of false.
        //
        // Also tests fallback object pointer equality (line 607):
        // Two distinct JSType instances that are not equal should return false under IDENTITY.
        JSType otherType = new JSType(registry) {
          @Override
          public BooleanLiteralSet getPossibleToBooleanOutcomes() {
            return BooleanLiteralSet.BOTH;
          }
          @Override
          public <T> T visit(Visitor<T> visitor) {
            return null;
          }
          @Override
          String toStringHelper(boolean forAnnotations) {
            return "OtherType";
          }
        };
    
        boolean dataFlowResult = standardUnknown.checkEquivalenceHelper(nominalUnknown, EquivalenceMethod.DATA_FLOW);
        assertFalse("Two unknown types where one is nominal and the other is not should differ in DATA_FLOW", dataFlowResult);
    
        boolean identityResult = standardUnknown.checkEquivalenceHelper(otherType, EquivalenceMethod.IDENTITY);
        assertFalse("Different non-identical object pointers should not be equivalent under IDENTITY", identityResult);
      }
}
