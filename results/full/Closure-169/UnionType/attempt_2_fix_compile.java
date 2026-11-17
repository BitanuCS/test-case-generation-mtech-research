package com.google.javascript.rhino.jstype;

import com.google.javascript.rhino.SimpleErrorReporter;
import junit.framework.TestCase;
import java.util.Arrays;
import java.util.Collections;

public class UnionType_CandorTest extends TestCase {

  private JSTypeRegistry registry;
  private NamedType unresolvedNamedType;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    registry = new JSTypeRegistry(new SimpleErrorReporter());
    unresolvedNamedType =
        new NamedType(registry, "not.resolved.named.type", null, -1, -1);
  }

  public void testIdentityEquivalenceDifferentSizeReturnsFalse() {
    UnionType unionA = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testStructuralEquivalenceDifferentSizeReturnsFalseIfMissingAlternate() {
    UnionType unionA = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE), registry.getNativeType(JSTypeNative.STRING_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.SUBTYPE));
  }

  public void testEquivalentUnionTypesSameAlternatesReturnsTrue() {
    UnionType unionA = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.SUBTYPE));
  }

  public void testIdentityEquivalenceSameSizeAndAlternatesReturnsTrue() {
    JSType numType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType boolType = registry.getNativeType(JSTypeNative.BOOLEAN_TYPE);

    UnionType unionA = new UnionType(registry, Arrays.asList(numType, boolType));
    UnionType unionB = new UnionType(registry, Arrays.asList(numType, boolType));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testCheckUnionEquivalenceHelperMissingAlternateReturnsFalse() {
    UnionType unionA = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.STRING_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.SUBTYPE));
  }

  public void testHasAlternateMatchingTypeReturnsTrue() {
    UnionType union = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertTrue(union.checkUnionEquivalenceHelper(
        new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE))),
        EquivalenceMethod.SUBTYPE));
  }

  public void testHasAlternateNoMatchingTypeReturnsFalse() {
    UnionType union = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertFalse(union.checkUnionEquivalenceHelper(
        new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.STRING_TYPE))),
        EquivalenceMethod.SUBTYPE));
  }

  public void testHasAlternateIdentityMismatchReturnsFalse() {
    NamedType type1 = new NamedType(registry, "A", null, -1, -1);
    NamedType type2 = new NamedType(registry, "A", null, -1, -1);

    UnionType unionC = new UnionType(registry, Arrays.asList(type1));
    UnionType unionD = new UnionType(registry, Arrays.asList(type2));

    assertFalse(unionC.checkUnionEquivalenceHelper(unionD, EquivalenceMethod.IDENTITY));
  }

  public void testHasAlternateIdentityMatchReturnsTrue() {
    NamedType type1 = new NamedType(registry, "A", null, -1, -1);

    UnionType unionA = new UnionType(registry, Arrays.asList(type1));
    UnionType unionB = new UnionType(registry, Arrays.asList(type1));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testEmptyUnionTypesReturnsTrue() {
    UnionType unionA = new UnionType(registry, Collections.<JSType>emptyList());
    UnionType unionB = new UnionType(registry, Collections.<JSType>emptyList());

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testDifferentOrderOfAlternatesReturnsTrue() {
    UnionType unionA = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.NUMBER_TYPE), registry.getNativeType(JSTypeNative.STRING_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(registry.getNativeType(JSTypeNative.STRING_TYPE), registry.getNativeType(JSTypeNative.NUMBER_TYPE)));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.SUBTYPE));
  }
}
