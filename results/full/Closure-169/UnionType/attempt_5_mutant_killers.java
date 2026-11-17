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

  

  

  public void testIdentityEquivalenceSameSizeAndAlternatesReturnsTrue() {
    JSType numType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
    JSType boolType = registry.getNativeType(JSTypeNative.BOOLEAN_TYPE);

    UnionType unionA = new UnionType(registry, Arrays.asList(numType, boolType));
    UnionType unionB = new UnionType(registry, Arrays.asList(numType, boolType));

    assertTrue(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
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

  

    @Override
      public void setUp2() throws Exception {
        super.setUp();
        registry = new JSTypeRegistry(new SimpleErrorReporter());
      }

    public void testCheckUnionEquivalenceHelper_ReturnsFalseWhenAlternateMissing() {
        JSType numType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
        JSType boolType = registry.getNativeType(JSTypeNative.BOOLEAN_TYPE);
        JSType strType = registry.getNativeType(JSTypeNative.STRING_TYPE);
    
        // unionA has {number}, unionB has {number, boolean}
        // That.alternates has boolean, which is not in unionA's alternates.
        // This exercises line 341 (return false inside the loop over that.alternates)
        // and kills mutant A3 and R5.
        UnionType unionA = new UnionType(registry, Arrays.asList(numType));
        UnionType unionB = new UnionType(registry, Arrays.asList(numType, boolType));
    
        assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
      }

    public void testCheckUnionEquivalenceHelper_ReturnsFalseWhenHasAlternateFails() {
        JSType numType = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
        JSType boolType = registry.getNativeType(JSTypeNative.BOOLEAN_TYPE);
        JSType strType = registry.getNativeType(JSTypeNative.STRING_TYPE);
    
        // unionA has {number}, unionB has {string}
        // hasAlternate will be called for string on unionA, which will not find it and return false,
        // causing hasAlternate to finish its loop and hit line 353 (return false).
        // This kills mutants A1 and R8.
        UnionType unionA = new UnionType(registry, Arrays.asList(numType));
        UnionType unionB = new UnionType(registry, Arrays.asList(strType));
    
        assertFalse(unionA.checkUnionEquivalenceHelper(unionB, EquivalenceMethod.IDENTITY));
      }
}
