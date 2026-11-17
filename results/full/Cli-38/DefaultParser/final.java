package org.apache.commons.cli;

import junit.framework.TestCase;

public class DefaultParser_CandorTest extends TestCase {

    private DefaultParser parser;
    private Options options;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        parser = new DefaultParser();
        options = new Options();
        options.addOption("a", "alpha", false, "alpha option");
        options.addOption("x", "xray", false, "xray option");
    }

    public void testReturnsTrueForSingleValidShortOption() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-a");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsFalseWhenTokenDoesNotStartWithHyphen() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "a");
        assertEquals(Boolean.FALSE, result);
    }

    public void testReturnsFalseWhenTokenIsSingleHyphen() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-");
        assertEquals(Boolean.FALSE, result);
    }

    public void testReturnsTrueForShortOptionWithValueSeparator() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-a=value");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsTrueForConcatenatedShortOptions() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-abc");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsFalseForUnknownShortOption() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean resultUnknown = (Boolean) method.invoke(parser, "-z");
        assertEquals(Boolean.FALSE, resultUnknown);
    }

    public void testReturnsFalseForLongOptionStartingWithDoubleHyphen() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "--long");
        assertEquals(Boolean.FALSE, result);
    }

    public void testReturnsFalseWhenOptNameIsEmptyAfterHyphenStripping() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-=");
        assertEquals(Boolean.FALSE, result);
    }

    public void testReturnsTrueWhenFirstCharOfConcatenatedOptIsRegistered() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-xyz");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsFalseWhenFirstCharOfConcatenatedOptIsNotRegistered() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-zxy");
        assertEquals(Boolean.FALSE, result);
    }

    public void testReturnsTrueForShortOptionWithValueAndEqualsSignInValue() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-a=foo=bar");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsFalseForNullToken() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        try {
            method.invoke(parser, new Object[] { null });
            fail("Expected NullPointerException or InvocationTargetException wrapping NullPointerException");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    public void testReturnsTrueForMultipleEqualsSignsInShortOptionValue() throws Exception {
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "-a=b=c");
        assertEquals(Boolean.TRUE, result);
    }

    public void testReturnsFalseForNonHyphenTokenEvenIfMatchingShortOptionExists() throws Exception {
        options.addOption("ab", "ab-option", false, "multi-char short option name");
        parser.options = options;
        java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(parser, "ab");
        assertEquals(Boolean.FALSE, result);
    }

    public void testMutantA1_IndexOfVersusLastIndexOfWithMultipleEquals() throws Exception {
            // [A1] mutant uses lastIndexOf("=") instead of indexOf("=").
            // For token "-a=b=c", indexOf("=") finds the first '=' at index 2, so optName becomes "a".
            // options.hasShortOption("a") is true.
            // lastIndexOf("=") would find the '=' at index 4, so optName becomes "a=b", which has no short option.
            parser.options = options;
            java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
            method.setAccessible(true);
            Boolean result = (Boolean) method.invoke(parser, "-a=b=c");
            assertEquals(Boolean.TRUE, result);
        }

    public void testMutantR1_LogicalOrReplacementWithAnd() throws Exception {
            // [R1] mutant replaces || with && in: if (!token.startsWith("-") || token.length() == 1)
            // Test a token that starts with "-" but has length 1 (i.e. "-").
            // Original: startsWith("-") is true, !true is false. length() == 1 is true. false || true -> true -> returns false.
            // Mutant: !true && true -> false -> does not return false, proceeds to check options and errors/fails.
            parser.options = options;
            java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
            method.setAccessible(true);
            Boolean result = (Boolean) method.invoke(parser, "-");
            assertEquals(Boolean.FALSE, result);
        }

    public void testMutantR12_OptNameLengthGreaterThanOneInsteadOfZero() throws Exception {
            // [R12] mutant replaces length() > 0 with length() > 1 for concatenated short options check.
            // Test a token with a single-character option name containing an '=' but where optName length is 1, e.g. "-a=".
            // After removing leading "-" and "=value": optName is "a" (length = 1).
            // options.hasShortOption("a") is true.
            // Then it checks: optName.length() > 0 && ... (original passes because length is 1 > 0).
            // Mutant checks: optName.length() > 1 (1 > 1 is false), so it returns false instead of true.
            parser.options = options;
            java.lang.reflect.Method method = DefaultParser.class.getDeclaredMethod("isShortOption", String.class);
            method.setAccessible(true);
            Boolean result = (Boolean) method.invoke(parser, "-a=");
            assertEquals(Boolean.TRUE, result);
        }
}
