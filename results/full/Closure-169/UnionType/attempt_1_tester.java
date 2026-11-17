package com.google.javascript.rhino.jstype;

import com.google.javascript.rhino.testing.BaseJSTypeTestCase;
import java.util.Arrays;
import java.util.Collections;

public class UnionType_CandorTest extends BaseJSTypeTestCase {

  private NamedType unresolvedNamedType;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    unresolvedNamedType =
        new NamedType(registry, "not.resolved.named.type", null, -1, -1);
  }

  public void testIdentityEquivalenceDifferentSizeReturnsFalse() {
    UnionType unionA = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testStructuralEquivalenceDifferentSizeReturnsFalseIfMissingAlternate() {
    UnionType unionA = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE), getNativeType(JSTypeNative.STRING_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.STRUCTURAL));
  }

  public void testEquivalentUnionTypesSameAlternatesReturnsTrue() {
    UnionType unionA = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.STRUCTURAL));
  }

  public void testIdentityEquivalenceSameSizeAndAlternatesReturnsTrue() {
    JSType numType = getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType boolType = getNativeType(JSTypeNative.BOOLEAN_TYPE);

    UnionType unionA = new UnionType(registry, Arrays.asList(numType, boolType));
    UnionType unionB = new UnionType(registry, Arrays.asList(numType, boolType));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
  }

  public void testCheckUnionEquivalenceHelperMissingAlternateReturnsFalse() {
    UnionType unionA = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.STRING_TYPE)));

    assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.STRUCTURAL));
  }

  public void testHasAlternateMatchingTypeReturnsTrue() {
    UnionType union = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertTrue(union.checkUnionEquivalenceHelper(
        new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE))),
        EquivalenceMethod.STRUCTURAL));
  }

  public void testHasAlternateNoMatchingTypeReturnsFalse() {
    UnionType union = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.BOOLEAN_TYPE)));

    assertFalse(union.checkUnionEquivalenceHelper(
        new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.STRING_TYPE))),
        EquivalenceMethod.STRUCTURAL));
  }

  public void testHasAlternateIdentityMismatchReturnsFalse() {
    JSType numType1 = getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType numType2 = getNativeType(JSTypeNative.NUMBER_TYPE);

    UnionType unionA = new UnionType(registry, Arrays.asList(numType1));
    UnionType unionB = new UnionType(registry, Arrays.asList(numType2));

    // When identities are compared, distinct instances (even if structurally equal native types) may fail under strict identity depending on registry caching,
    // but here we test the checkUnionEquivalenceHelper branch logic with IDENTITY method.
    // If numType1 and numType2 happen to be same instance from native types, let's create distinct instances or test where identity differs.
    // Actually BaseJSTypeTestCase native types are singletons, so let's use NamedType instances for identity testing.
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
    UnionType unionA = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.NUMBER_TYPE), getNativeType(JSTypeNative.STRING_TYPE)));
    UnionType unionB = new UnionType(registry, Arrays.asList(getNativeType(JSTypeNative.STRING_TYPE), getNativeType(JSTypeNative.NUMBER_TYPE)));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.STRUCTURAL));
  }
}
