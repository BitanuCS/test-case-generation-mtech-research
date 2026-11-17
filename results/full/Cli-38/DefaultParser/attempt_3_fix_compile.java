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
}
