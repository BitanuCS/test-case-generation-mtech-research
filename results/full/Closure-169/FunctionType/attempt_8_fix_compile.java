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

    public void testTryMergePiecewiseReturnTypeInferredOrLogic() {
        Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
        
        // Create arrow types where only one has returnTypeInferred = true
        ArrowType arrow1 = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), true);
        ArrowType arrow2 = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.STRING_TYPE), false);
    
        ObjectType typeOfThis = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
    
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow1, typeOfThis, null, false, false);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow2, typeOfThis, null, false, false);
    
        // Call tryMergeFunctionPiecewise via package-private/private or equivalent mechanism if accessible,
        // or invoke methods that exercise it. Since tryMergeFunctionPiecewise is private, we test via
        // supAndInfHelper which calls tryMergeFunctionPiecewise.
        FunctionType merged = fn1.supAndInfHelper(fn2, true);
        assertNotNull(merged);
        assertTrue("Merged return type should be inferred when at least one input is inferred (OR logic)",
            merged.isReturnTypeInferred());
      }

    

    public void testTryMergePiecewiseTernaryLeastSuperVsGreatestSubtypeOrder() {
        Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
        ArrowType arrow = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
    
        // Create distinct object types to verify that leastSuper vs greatestSubtype branches are distinct
        ObjectType typeOfThis1 = registry.getNativeObjectType(JSTypeNative.LEAST_FUNCTION_TYPE);
        ObjectType typeOfThis2 = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
    
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow, typeOfThis1, null, false, false);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow, typeOfThis2, null, false, false);
    
        FunctionType mergedLeast = fn1.supAndInfHelper(fn2, true);
        FunctionType mergedGreatest = fn1.supAndInfHelper(fn2, false);
    
        assertNotNull(mergedLeast);
        assertNotNull(mergedGreatest);
        // The result of leastSuper and greatestSubtype should differ for these types, ensuring the ternary branches are not swapped
        assertFalse(mergedLeast.getTypeOfThis().equals(mergedGreatest.getTypeOfThis()));
      }

    public void testTryMergePiecewiseConstructorAndNativeFlagsAreFalse() {
        Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
        ArrowType arrow = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
        ObjectType typeOfThis = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
    
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow, typeOfThis, null, false, false);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow, typeOfThis, null, false, false);
    
        FunctionType merged = fn1.supAndInfHelper(fn2, true);
        assertNotNull(merged);
        assertFalse("Merged function should not be a constructor", merged.isConstructor());
        assertFalse("Merged function should not be an instance type", merged.isInstanceType());
      }

    public void testTryMergePiecewiseReturnsNullWhenParametersDiffer() {
        Node params1 = new Node(com.google.javascript.rhino.Token.LP);
        Node params2 = new Node(com.google.javascript.rhino.Token.LP);
        params2.addChildToBack(Node.newString(com.google.javascript.rhino.Token.NAME, "arg"));
    
        ArrowType arrow1 = new ArrowType(registry, params1, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
        ArrowType arrow2 = new ArrowType(registry, params2, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
    
        ObjectType typeOfThis = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
    
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow1, typeOfThis, null, false, false);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow2, typeOfThis, null, false, false);
    
        FunctionType merged = fn1.supAndInfHelper(fn2, true);
        assertNull("Merging functions with different parameters should return null (line 797)", merged);
      }

    public void testTryMergePiecewiseFallbackWhenMaybeNewTypeOfThisNotObjectType() {
        Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
        ArrowType arrow = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
    
        // Using two distinct types that yield a non-ObjectType union/intersection (e.g., JSType union or primitive-like or no-type results)
        // Specifically, combining two disjoint types or specific native types where leastSuper/greatestSubtype produces a non-ObjectType.
        JSType typeA = registry.getNativeType(JSTypeNative.NUMBER_TYPE);
        JSType typeB = registry.getNativeType(JSTypeNative.STRING_TYPE);
    
        // We need typeOfThis to be an ObjectType so we can call getLeastSupertype/getGreatestSubtype, 
        // but we can craft a scenario where maybeNewTypeOfThis is NOT an instance of ObjectType.
        // For instance, NumberType and StringType least supertype is JSType (not an ObjectType).
        // But typeOfThis must be ObjectType. Let's create an ObjectType whose leastSupertype with another is not an ObjectType,
        // or mock/use native types that result in a non-ObjectType.
        ObjectType objType = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
        
        // Let's test leastSuper = true and leastSuper = false falling through to:
        // registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE) vs NO_OBJECT_TYPE.
        // By forcing maybeNewTypeOfThis to be a non-ObjectType (e.g. by using a type combination that results in a non-ObjectType).
        // Since ObjectType.getLeastSupertype with something can return a JSType that is not an ObjectType,
        // let's test via supAndInfHelper directly with custom types if possible, or use types that trigger lines 814-816.
        
        // We can use a FunctionType as typeOfThis vs something else, or test the exact fallback values:
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow, objType, null, false, false);
        
        // Create another function with a typeOfThis that when combined via leastSuper or greatestSubtype 
        // results in a non-ObjectType (or we test both leastSuper=true and leastSuper=false).
        // Let's create a function with a non-matching typeOfThis that forces the fallback branches [A3] and [A4].
        ObjectType otherObjType = registry.getNativeObjectType(JSTypeNative.LEAST_FUNCTION_TYPE);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow, otherObjType, null, false, false);
    
        FunctionType mergedLeast = fn1.supAndInfHelper(fn2, true);
        assertNotNull(mergedLeast);
        // When leastSuper is true, fallback should be OBJECT_TYPE if not an ObjectType
        assertSame(registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE), mergedLeast.getTypeOfThis());
    
        FunctionType mergedGreatest = fn1.supAndInfHelper(fn2, false);
        assertNotNull(mergedGreatest);
        // When leastSuper is false, fallback should be NO_OBJECT_TYPE if not an ObjectType
        assertSame(registry.getNativeObjectType(JSTypeNative.NO_OBJECT_TYPE), mergedGreatest.getTypeOfThis());
      }

    public void testTryMergePiecewiseConstructorAndNativeFlagsAreExplicitlyFalse() {
        Node paramsNode = new Node(com.google.javascript.rhino.Token.LP);
        ArrowType arrow = new ArrowType(registry, paramsNode, registry.getNativeType(JSTypeNative.NUMBER_TYPE), false);
        ObjectType typeOfThis = registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE);
    
        FunctionType fn1 = new FunctionType(registry, "fn1", null, arrow, typeOfThis, null, false, false);
        FunctionType fn2 = new FunctionType(registry, "fn2", null, arrow, typeOfThis, null, false, false);
    
        FunctionType merged = fn1.supAndInfHelper(fn2, true);
        assertNotNull(merged);
        assertFalse("Mutant R1 detected: isConstructor should be false", merged.isConstructor());
        assertFalse("Mutant R2 detected: isInstanceType should be false", merged.isInstanceType());
      }
}
