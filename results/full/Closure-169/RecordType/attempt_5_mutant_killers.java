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

    private JSType getNativeType2(JSTypeNative nativeType) {
        return registry.getNativeType(nativeType);
      }

    @Test
      public void testIsSubtypeInferredPropertyNotSubtypeDetectsMutant() {
        // [A1] Mutants on declared vs inferred checks:
        // If property A is *inferred* (has a property node or defined as inferred),
        // it requires propA.isSubtype(propB). If propA is not a subtype of propB,
        // it should return false.
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
    
        // Inferred property can be created by passing a Node or defining as inferred.
        // Using defineProperty or constructor with inferred properties / checking isPropertyTypeDeclared.
        RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(numberType, null)));
        RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
        
        // To ensure it hits the inferred branch (isPropertyTypeDeclared returns false),
        // we define the property as inferred on typeA.
        typeA.defineProperty("a", stringType, true, null);
    
        assertFalse(RecordType.isSubtype(typeA, typeB));
      }

    @Test
      public void testCheckRecordEquivalenceNonSymmetricCheckDetectsMutant() {
        // [A4] Swapped arguments in checkEquivalenceHelper:
        // tests asymmetric equivalence relations if any exist, or ensures correct order of parameters.
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
        RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
    
        assertTrue(typeA.checkRecordEquivalenceHelper(typeB, EquivalenceMethod.DATA_FLOW));
      }

    @Test
      public void testGetGreatestSubtypeSynthesizedTrueDetectsMutant() {
        // [R5] builder.setSynthesized(true) vs false
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
        RecordType typeB = new RecordType(registry, ImmutableMap.of("b", new RecordProperty(stringType, null)));
    
        JSType result = typeA.getGreatestSubtypeHelper(typeB);
        assertNotNull(result);
        assertTrue(result.isRecordType());
        assertTrue(result.toMaybeRecordType().isSynthetic());
      }

    @Test
      public void testGetGreatestSubtypeLogicalAndDetectsMutant() {
        // [R6] thatRecord.hasProperty(property) && !thatRecord.getPropertyType(property)...
        // If thatRecord has property, but it is invariant, should return NO_TYPE.
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        JSType numberType = getNativeType(JSTypeNative.NUMBER_TYPE);
        RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
        RecordType typeB = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(numberType, null)));
    
        JSType result = typeA.getGreatestSubtypeHelper(typeB);
        assertEquals(registry.getNativeObjectType(JSTypeNative.NO_TYPE), result);
      }

    @Test
      public void testGetGreatestSubtypeNonRecordObjectNegationDetectsMutant() {
        // [R9] if (!thatRestrictedToObj.isEmptyType())
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        RecordType typeA = new RecordType(registry, ImmutableMap.of("a", new RecordProperty(stringType, null)));
        
        // Pass a non-record object type (like OBJECT_TYPE) that has overlaps or is valid
        JSType result = typeA.getGreatestSubtypeHelper(registry.getNativeType(JSTypeNative.OBJECT_TYPE));
        assertNotNull(result);
      }

    @Test
      public void testGetGreatestSubtypeReferenceTypeWithPropertyDetectsMutants() {
        // Covers lines 195-209: registry.getEachReferenceTypeWithProperty(propName)
        JSType stringType = getNativeType(JSTypeNative.STRING_TYPE);
        RecordType typeA = new RecordType(registry, ImmutableMap.of("toString", new RecordProperty(stringType, null)));
    
        // Passing a non-record object type like OBJECT_TYPE forces execution into the reference type iteration branch
        JSType result = typeA.getGreatestSubtypeHelper(registry.getNativeType(JSTypeNative.OBJECT_TYPE));
        assertNotNull(result);
      }
}
