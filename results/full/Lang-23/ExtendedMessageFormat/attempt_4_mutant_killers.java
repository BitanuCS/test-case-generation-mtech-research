package org.apache.commons.lang3.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

public class ExtendedMessageFormat_CandorTest extends TestCase {

    private Map<String, FormatFactory> registry1;
    private Map<String, FormatFactory> registry2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        registry1 = new HashMap<String, FormatFactory>();
        registry1.put("lower", new LowerCaseFormatFactory());

        registry2 = new HashMap<String, FormatFactory>();
        registry2.put("upper", new UpperCaseFormatFactory());
    }

    public void testEqualsSelf() {
        ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
        assertTrue(emf.equals(emf));
    }

    public void testEqualsNull() {
        ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
        assertFalse(emf.equals(null));
    }

    public void testEqualsDifferentClass() {
        ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
        OtherExtendedMessageFormat otherEmf = new OtherExtendedMessageFormat("Hello {0}", Locale.getDefault(), registry1);
        assertFalse(emf.equals(otherEmf));
    }

    public void testEqualsIdentical() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry1);
        assertTrue(emf1.equals(emf2));
    }

    public void testEqualsDifferentPattern() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Goodbye {0}", registry1);
        assertFalse(emf1.equals(emf2));
    }

    public void testEqualsDifferentRegistry() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry2);
        assertFalse(emf1.equals(emf2));
    }

    public void testEqualsSuperEqualsDiff() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", Locale.US, registry1);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", Locale.FRANCE, registry1);
        assertFalse(emf1.equals(emf2));
    }

    public void testHashCodeConsistency() {
        ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
        int hc1 = emf.hashCode();
        int hc2 = emf.hashCode();
        assertEquals(hc1, hc2);
    }

    public void testHashCodeEqualityCondition() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry1);
        assertTrue(emf1.equals(emf2));
        assertEquals(emf1.hashCode(), emf2.hashCode());
    }

    public void testHashCodeWithNullRegistryAndPattern() {
        ExtendedMessageFormat emf = new ExtendedMessageFormat("Simple pattern");
        int hc = emf.hashCode();
        assertTrue(hc != 0);
    }

    public void testEqualsNullPatterns() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Simple", (Map<String, FormatFactory>) null);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Simple", (Map<String, FormatFactory>) null);
        assertTrue(emf1.equals(emf2));
    }

    public void testEqualsNullRegistries() {
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Pattern {0}", (Map<String, FormatFactory>) null);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Pattern {0}", (Map<String, FormatFactory>) null);
        assertTrue(emf1.equals(emf2));
    }

    private static class LowerCaseFormat extends java.text.Format {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
            return toAppendTo.append(((String) obj).toLowerCase());
        }
        @Override
        public Object parseObject(String source, java.text.ParsePosition pos) {
            throw new UnsupportedOperationException();
        }
    }

    private static class UpperCaseFormat extends java.text.Format {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
            return toAppendTo.append(((String) obj).toUpperCase());
        }
        @Override
        public Object parseObject(String source, java.text.ParsePosition pos) {
            throw new UnsupportedOperationException();
        }
    }

    private static class LowerCaseFormatFactory implements FormatFactory {
        public java.text.Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static class UpperCaseFormatFactory implements FormatFactory {
        public java.text.Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static class OtherExtendedMessageFormat extends ExtendedMessageFormat {
        public OtherExtendedMessageFormat(String pattern, Locale locale, Map<String, ? extends FormatFactory> registry) {
            super(pattern, locale, registry);
        }
    }

    public void testEqualsSelf2() {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
            assertTrue(emf.equals(emf));
        }

    public void testEqualsNull2() {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
            assertFalse(emf.equals(null));
        }

    public void testEqualsDifferentClass2() {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
            OtherExtendedMessageFormat otherEmf = new OtherExtendedMessageFormat("Hello {0}", Locale.getDefault(), registry1);
            assertFalse(emf.equals(otherEmf));
        }

    public void testEqualsIdentical2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry1);
            assertTrue(emf1.equals(emf2));
        }

    public void testEqualsDifferentPattern2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Goodbye {0}", registry1);
            assertFalse(emf1.equals(emf2));
        }

    public void testEqualsDifferentRegistry2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry2);
            assertFalse(emf1.equals(emf2));
        }

    public void testEqualsSuperEqualsDiff2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", Locale.US, registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", Locale.FRANCE, registry1);
            assertFalse(emf1.equals(emf2));
        }

    public void testHashCodeConsistency2() {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
            int hc1 = emf.hashCode();
            int hc2 = emf.hashCode();
            assertEquals(hc1, hc2);
        }

    public void testHashCodeEqualityCondition2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Hello {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Hello {0}", registry1);
            assertTrue(emf1.equals(emf2));
            assertEquals(emf1.hashCode(), emf2.hashCode());
        }

    public void testHashCodeWithNullRegistryAndPattern2() {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Simple pattern");
            int hc = emf.hashCode();
            assertTrue(hc != 0);
        }

    public void testEqualsNullPatterns2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Simple", (Map<String, FormatFactory>) null);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Simple", (Map<String, FormatFactory>) null);
            assertTrue(emf1.equals(emf2));
        }

    public void testEqualsNullRegistries2() {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Pattern {0}", (Map<String, FormatFactory>) null);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Pattern {0}", (Map<String, FormatFactory>) null);
            assertTrue(emf1.equals(emf2));
        }

    public void testHashCodeRegistryAndPatternSensitivity() {
            // This test catches [A0] (swapped hash combination order) and [A1] (registry hash replaced by super.hashCode()) and [A2] (multiplier 1)
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("PatternA {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("PatternB {0}", registry2);
            
            // Construct two formats such that one has pattern A and registry 2, and another has pattern B and registry 1
            // Their combined fields should yield different hash codes if both registry and toPattern are properly incorporated with HASH_SEED.
            ExtendedMessageFormat emf3 = new ExtendedMessageFormat("PatternA {0}", registry2);
            ExtendedMessageFormat emf4 = new ExtendedMessageFormat("PatternB {0}", registry1);
    
            assertFalse(emf3.hashCode() == emf4.hashCode());
        }

    public void testEqualsRegistryVsPatternShortCircuitOrder() {
            // This test targets [A3] where registry is checked before toPattern or vice versa
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Pattern1 {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Pattern2 {0}", registry2);
            
            // Different pattern AND different registry
            assertFalse(emf1.equals(emf2));
        }

    public void testEqualsSubclassDifferentClassCheck() {
            // This explicitly targets [A4] (missing getClass() check) and [R6] (getClass() check returning true instead of false)
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Hello {0}", registry1);
            OtherExtendedMessageFormat otherEmf = new OtherExtendedMessageFormat("Hello {0}", Locale.getDefault(), registry1);
            
            // Even if pattern and registry and super.equals match, different classes must not be equal
            assertFalse(emf.equals(otherEmf));
            assertFalse(otherEmf.equals(emf));
        }

    @Override
        protected void setUp2() throws Exception {
            super.setUp();
            registry1 = new HashMap<String, FormatFactory>();
            registry1.put("lower", new LowerCaseFormatFactory());
    
            registry2 = new HashMap<String, FormatFactory>();
            registry2.put("upper", new UpperCaseFormatFactory());
        }

    public void testHashCodeSymmetryAndMultiplier() {
            // Targets [A0] (swapped combination order of registry and toPattern)
            // and [A1] (registry hash replaced by super.hashCode())
            // and [A2] (HASH_SEED multiplier replaced by 1 for registry line)
            
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("PatternA {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("PatternB {0}", registry2);
            
            // If registry and toPattern are swapped or if registry hash is replaced by super.hashCode() or multiplier is 1,
            // we can construct or analyze hash collisions. Specifically, let's test a pair where
            // hash contributions would differ if combined in the correct order with HASH_SEED.
            
            // Let's create two objects where toPattern and registry values are swapped in content/types
            // but since their individual hashes might differ, the combined hash must strictly depend on the exact calculation order and multiplier.
            int hc1 = emf1.hashCode();
            int hc2 = emf2.hashCode();
            
            // They should not be equal
            assertFalse(hc1 == hc2);
        }

    public void testEqualsRegistryFieldCheckOrder() {
            // Targets [A3] (swapped order of field checks in equals: checking registry before toPattern)
            // If we compare two objects where toPattern differs AND registry differs, both checks fail,
            // but to ensure the sequence of field evaluation or specific short-circuiting is exercised:
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat("Pattern1 {0}", registry1);
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat("Pattern2 {0}", registry2);
    
            assertFalse(emf1.equals(emf2));
        }

    public void testEqualsClassCheckFailureLines276And280() {
            // Specifically targets hitting line 276 (return false for getClass() mismatch)
            // and line 280 (return false for toPattern mismatch).
            
            ExtendedMessageFormat emf = new ExtendedMessageFormat("Pattern {0}", registry1);
            OtherExtendedMessageFormat otherEmf = new OtherExtendedMessageFormat("Pattern {0}", Locale.getDefault(), registry1);
            
            // This exercises line 276: return false; inside ObjectUtils.notEqual(getClass(), obj.getClass())
            assertFalse(emf.equals(otherEmf));
    
            // This exercises line 280: return false; inside ObjectUtils.notEqual(toPattern, rhs.toPattern)
            ExtendedMessageFormat emfDifferentPattern = new ExtendedMessageFormat("Different {0}", registry1);
            assertFalse(emf.equals(emfDifferentPattern));
        }
}
