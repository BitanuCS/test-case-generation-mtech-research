package com.google.javascript.rhino.jstype;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import com.google.javascript.rhino.jstype.RecordTypeBuilder.RecordProperty;

public class RecordType_CandorTest extends BaseJSTypeTestCase {

  public void testIsSubtypeMissingPropertyReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(stringType, null)
    ));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  public void testIsSubtypeDeclaredPropertyNotInvariantReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    
    // typeA with declared property 'a' of type string
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    // typeB with property 'a' of type object
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(objectType, null)));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  public void testIsSubtypeDeclaredPropertyInvariantReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  public void testIsSubtypeInferredPropertyNotSubtypeReturnsFalse() {
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    
    // Inferred property can be simulated or built where typeA has object and typeB expects string
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(objectType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  public void testIsSubtypeInferredPropertyIsSubtypeReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(objectType, null)));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  public void testCheckRecordEquivalenceDifferentKeySetsReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("b", new RecordProperty(stringType, null)));

    assertFalse(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  public void testCheckRecordEquivalenceSameKeySetsEquivalentPropsReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertTrue(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  public void testCheckRecordEquivalenceSameKeySetsDifferentPropsReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(numberType, null)));

    assertFalse(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  public void testGetGreatestSubtypeRecordConflictingPropertyReturnsNoType() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(numberType, null)));

    JSType result = typeA.getGreatestSubtypeHelper(typeB);
    assertEquals(registry.getNativeObjectType(JSTypeNative.NO_TYPE), result);
  }

  public void testGetGreatestSubtypeRecordCompatiblePropertiesMergesSuccessfully() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = registry.createRecordType(ImmutableMap.of("b", new RecordProperty(numberType, null)));

    JSType result = typeA.getGreatestSubtypeHelper(typeB);
    assertNotNull(result);
    assertTrue(result.isRecordType());
    RecordType recordResult = result.toMaybeRecordType();
    assertTrue(recordResult.hasProperty("a"));
    assertTrue(recordResult.hasProperty("b"));
  }

  public void testGetGreatestSubtypeNonRecordObjectEmptyIntersectionReturnsNoObjectType() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = registry.createRecordType(ImmutableMap.of("a", new RecordProperty(stringType, null)));
    JSType noObjectType = registry.getNativeType(JSTypeNative.NO_OBJECT_TYPE);

    JSType result = typeA.getGreatestSubtypeHelper(noObjectType);
    assertEquals(registry.getNativeType(JSTypeNative.NO_OBJECT_TYPE), result);
  }

  public void testIsSubtypeMultiplePropertiesAllValidReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);

    RecordType typeA = registry.createRecordType(ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(numberType, null)
    ));
    RecordType typeB = registry.createRecordType(ImmutableMap.of(
        "a", new RecordProperty(objectType, null),
        "b", new RecordProperty(numberType, null)
    ));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  public void testIsSubtypeMultiplePropertiesOneInvalidReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType booleanType = getNativeType(JSTypeNative.BOOLEAN_TYPE);

    RecordType typeA = registry.createRecordType(ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(booleanType, null)
    ));
    RecordType typeB = registry.createRecordType(ImmutableMap.of(
        "a", new RecordProperty(objectType, null),
        "b", new RecordProperty(numberType, null)
    ));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  public void testCheckRecordEquivalenceEmptyRecordsReturnsTrue() {
    RecordType typeA = registry.createRecordType(ImmutableMap.of());
    RecordType typeB = registry.createRecordType(ImmutableMap.of());

    assertTrue(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }
}
