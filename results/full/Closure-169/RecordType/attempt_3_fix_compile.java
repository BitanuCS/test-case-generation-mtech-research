package com.google.javascript.rhino.jstype;

import com.google.common.collect.ImmutableMap;
import com.google.javascript.rhino.jstype.RecordTypeBuilder.RecordProperty;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecordType_CandorTest {

  private final JSTypeRegistry registry = new JSTypeRegistry(null);

  private JSType getNativeType(JSTypeNative nativeType) {
    return registry.getNativeType(nativeType);
  }

  @Test
  public void testIsSubtypeMissingPropertyReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(stringType, null)
    ));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testIsSubtypeDeclaredPropertyNotInvariantReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(objectType, null)));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testIsSubtypeDeclaredPropertyInvariantReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testIsSubtypeInferredPropertyNotSubtypeReturnsFalse() {
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(objectType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testIsSubtypeInferredPropertyIsSubtypeReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(objectType, null)));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testCheckRecordEquivalenceDifferentKeySetsReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("b", new RecordProperty(stringType, null)));

    assertFalse(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  @Test
  public void testCheckRecordEquivalenceSameKeySetsEquivalentPropsReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));

    assertTrue(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  @Test
  public void testCheckRecordEquivalenceSameKeySetsDifferentPropsReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(numberType, null)));

    assertFalse(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }

  @Test
  public void testGetGreatestSubtypeRecordConflictingPropertyReturnsNoType() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(numberType, null)));

    JSType result = typeA.getGreatestSubtypeHelper(typeB);
    assertEquals(registry.getNativeObjectType(JSTypeNative.NO_TYPE), result);
  }

  @Test
  public void testGetGreatestSubtypeRecordCompatiblePropertiesMergesSuccessfully() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    RecordType typeB = new RecordType(registry, ImmutableMap.of("b", new RecordProperty(numberType, null)));

    JSType result = typeA.getGreatestSubtypeHelper(typeB);
    assertNotNull(result);
    assertTrue(result.isRecordType());
    RecordType recordResult = result.toMaybeRecordType();
    assertTrue(recordResult.hasProperty("a"));
    assertTrue(recordResult.hasProperty("b"));
  }

  @Test
  public void testGetGreatestSubtypeNonRecordObjectEmptyIntersectionReturnsNoObjectType() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));

    JSType result = typeA.getGreatestSubtypeHelper(registry.getNativeType(JSTypeNative.NO_OBJECT_TYPE));
    assertEquals(registry.getNativeType(JSTypeNative.NO_OBJECT_TYPE), result);
  }

  @Test
  public void testIsSubtypeMultiplePropertiesAllValidReturnsTrue() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);

    RecordType typeA = new RecordType(registry, ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(numberType, null)
    ));
    RecordType typeB = new RecordType(registry, ImmutableMap.of(
        "a", new RecordProperty(objectType, null),
        "b", new RecordProperty(numberType, null)
    ));

    assertTrue(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testIsSubtypeMultiplePropertiesOneInvalidReturnsFalse() {
    JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
    JSType objectType = getNativeType(JSTypeNative.OBJECT_TYPE);
    JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType booleanType = getNativeType(JSTypeNative.BOOLEAN_TYPE);

    RecordType typeA = new RecordType(registry, ImmutableMap.of(
        "a", new RecordProperty(stringType, null),
        "b", new RecordProperty(booleanType, null)
    ));
    RecordType typeB = new RecordType(registry, ImmutableMap.of(
        "a", new RecordProperty(objectType, null),
        "b", new RecordProperty(numberType, null)
    ));

    assertFalse(RecordType.isSubtype(typeA, typeB));
  }

  @Test
  public void testCheckRecordEquivalenceEmptyRecordsReturnsTrue() {
    RecordType typeA = new RecordType(registry, ImmutableMap.of());
    RecordType typeB = new RecordType(registry, ImmutableMap.of());

    assertTrue(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
  }
}
