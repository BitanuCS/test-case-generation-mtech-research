package org.apache.commons.codec.language;

import junit.framework.TestCase;

public class Caverphone_CandorTest extends TestCase {

    private Caverphone caverphone;

    protected void setUp() throws Exception {
        super.setUp();
        this.caverphone = new Caverphone();
    }

    public void testNullInput() {
        String result = caverphone.caverphone(null);
        assertEquals("1111111111", result);
    }

    public void testEmptyStringInput() {
        String result = caverphone.caverphone("");
        assertEquals("1111111111", result);
    }

    public void testBasicWordEncoding() {
        String result = caverphone.caverphone("Lee");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionCough() {
        String result = caverphone.caverphone("cough");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionRough() {
        String result = caverphone.caverphone("rough");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionTough() {
        String result = caverphone.caverphone("tough");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionEnough() {
        String result = caverphone.caverphone("enough");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionTrough() {
        String result = caverphone.caverphone("trough");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionGn() {
        String result = caverphone.caverphone("gnat");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testStartOptionMb() {
        String result = caverphone.caverphone("mbira");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testNonAlphabetCharactersRemoval() {
        String resultWithSymbols = caverphone.caverphone("A-B_C.123!");
        String resultWithoutSymbols = caverphone.caverphone("ABC");
        assertEquals(resultWithoutSymbols, resultWithSymbols);
    }

    public void testFinalEStripping() {
        String resultWithE = caverphone.caverphone("name");
        String resultWithoutE = caverphone.caverphone("nam");
        assertEquals(resultWithoutE, resultWithE);
    }

    public void testVowelReplacementsAtStart() {
        String result = caverphone.caverphone("apple");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testConsonantClusterReplacements() {
        String result = caverphone.caverphone("succeeded");
        assertNotNull(result);
        assertEquals(10, result.length());
    }

    public void testCleanupAndPadding() {
        String result = caverphone.caverphone("Smith");
        assertNotNull(result);
        assertEquals(10, result.length());
        assertTrue(result.endsWith("1111"));
    }
}
