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

    protected void setUp2() throws Exception {
            super.setUp();
            this.caverphone = new Caverphone();
        }

    public void testRoughVsRou3fMutant() {
            String originalResult = caverphone.caverphone("rough");
            // Let's assert a specific known correct encoding for "rough" under Caverphone 2.0
            // "rough" -> start "rou2f" -> ... -> "RF11111111" (or check that it differs from what rou3f produces)
            assertFalse("1111111111".equals(originalResult));
            // Directly test that "rough" produces the expected Caverphone 2.0 output
            assertEquals("RF11111111", originalResult);
        }

    public void testVReplacementMutant() {
            String resultVine = caverphone.caverphone("vine");
            String resultFine = caverphone.caverphone("fine");
            assertEquals(resultFine, resultVine);
        }

    public void testPaddingMutantA3() {
            String result = caverphone.caverphone("a");
            // Original code appends 10 ones total, resulting in "A111111111" (1 'A' + 9 ones).
            // It does not end with 5 ones from the second group uniquely in a way that... wait.
            // Let's check exact string for "a":
            // "a" -> "A" + 9 ones = "A111111111" (length 10).
            // Does it end with "1111"? Yes. Does it end with "11111"? Yes, because 5 ones is suffix of 9 ones.
            // What input gives a result where the 4-digit padding is partially cut or visible?
            // Let's test an input that has length 9 before padding, e.g. 9 characters.
            // Or even simpler, test the exact expected encoding of a short word like "a":
            assertEquals("A111111111", result);
        }

    public void testSingleCharacterInputForRelationalAndConstantMutants() {
            String result = caverphone.caverphone("a");
            // Original code returns "A111111111"
            // Mutants [R2] and [R3] return "1111111111"
            assertEquals("A111111111", result);
        }
}
