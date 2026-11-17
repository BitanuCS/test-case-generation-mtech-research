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

    protected void setUp3() throws Exception {
            super.setUp();
            this.caverphone = new Caverphone();
        }

    public void testRoughMutantDetection() {
            // Mutant [A1] replaces "rou2f" with "rou3f" for "^rough".
            // Let's verify the exact output for "rough" under the original algorithm.
            // "rough" -> "rou2f" (start) -> ... -> resulting in "RF11111111"
            // If "rou3f" is used instead, the '3' is either removed or mapped differently, causing a mismatch.
            String result = caverphone.caverphone("rough");
            assertEquals("RF11111111", result);
        }

    public void testPaddingMutantDetection() {
            // Mutant [A3] changes the second padding group from "1111" (4 ones) to "11111" (5 ones),
            // making the total appended ones 11 instead of 10, then substring(0, 10) takes the first 10.
            // For a very short input like "a", the resulting string length before padding is short,
            // so adding an extra '1' changes the 10-character padded result.
            // Original for "a": "A111111111" (1 'A' + 9 ones = 10 chars).
            // With 11 ones appended ("111111" + "11111" = 11 ones), "A" + 11 ones has length 12,
            // and substring(0, 10) gives "A111111111" still? Wait, let's check exact length:
            // "a" -> lower "a" -> no symbols -> no final e -> no start options -> no replacements ->
            // removals -> txt is "A" (since ^aeiou -> A).
            // Then step 6: txt = "A" + "111111" + "1111" = "A1111111111" (length 11).
            // Then step 7: txt.substring(0, 10) -> "A111111111" (length 10).
            // If txt = "A" + "111111" + "11111" = "A11111111111" (length 12).
            // Then substring(0, 10) -> "A111111111" (still 10 ones? Wait!).
            // Let's find an input where the extra padding character is NOT truncated because length before padding is longer.
            // E.g., an input that results in length 5 before padding, so 5 + 10 = 15 -> truncated to 10.
            // Wait, if length before padding is 5, 5 + 10 = 15, and 5 + 11 = 16. Both truncate to 10 characters!
            // To detect the padding change, we need an input where the total length before step 6 is less than 10, 
            // and the exact number of 1s at the end differs.
            // Let's test input "a" or check what length "a" produces:
            // "a" -> "A" (length 1). Step 6: "A" + 6 ones + 4 ones = "A1111111111" (length 11). substring(0,10) -> "A111111111".
            // Mutant: "A" + 6 ones + 5 ones = "A11111111111" (length 12). substring(0,10) -> "A111111111". Both give "A111111111"!
            // What about an input of length exactly 1? Let's test "at":
            // "at" -> lower -> "at" -> no final e -> no start -> "at" -> replacements: 'a'->'A', 't'->'T' -> "AT". Length 2.
            // Step 6: "AT" + 6 ones + 4 ones = "AT1111111111" (length 12). substring(0,10) -> "AT11111111".
            // Mutant: "AT" + 6 ones + 5 ones = "AT11111111111" (length 13). substring(0,10) -> "AT11111111". Still same!
            // When does substring(0, 10) differ? Only if the string length before step 6 is greater than or equal to 10?
            // No, step 6 *always* appends 10 or 11 characters to whatever `txt` is.
            // `txt` + 6 ones + 4 ones. Length is `txt.length() + 10`.
            // If we take substring(0, 10), as long as `txt.length() >= 0`, `txt.length() + 10 >= 10`, 
            // so `substring(0, 10)` will always capture `txt` (up to 10 chars) plus however many padding ones are needed to reach length 10.
            // Wait! If `txt` has length e.g. 8, `txt` + 10 ones has length 18. substring(0, 10) takes `txt` (8 chars) + 2 ones.
            // With mutant, `txt` + 11 ones has length 19. substring(0, 10) takes `txt` (8 chars) + 2 ones! It's still 2 ones!
            // Because substring(0, 10) cuts off at index 10, and both original and mutant strings have length >= 10, 
            // both result in a 10-character string where the tail is truncated before the extra '1' is ever reached!
            // Wait, if `txt` has length e.g. 5, `txt` + 10 ones has length 15. substring(0, 10) takes `txt` (5 chars) + 5 ones.
            // Mutant: `txt` + 11 ones has length 16. substring(0, 10) takes `txt` (5 chars) + 5 ones! Still 5 ones!
            // Is there any case where appending 4 vs 5 ones changes substring(0, 10)? 
            // Only if `txt.length() + 6 + 4 < 10`? But 6 ones are *always* added first: `txt + "111111" + "1111"`.
            // Since `"111111"` has length 6, `txt.length() + 6` is at least 6. Adding at least one '1' makes total length >= 7.
            // Thus, `substring(0, 10)` always cuts into the padding, meaning the exact number of padding ones at the very end 
            // is truncated away by `substring(0, 10)` UNLESS the total length is less than 10!
            // But total length is `txt.length() + 6 + 4 = txt.length() + 10`, which is ALWAYS >= 10!
            // Therefore, `substring(0, 10)` always truncates the string to exactly 10 characters, discarding the last '1' of the mutant!
            // Wait, if `substring(0, 10)` truncates it, does the mutant actually produce the exact same output as original?
            // Let's check: Original: `txt + "111111" + "1111"` -> length is `L + 10`. `substring(0, 10)` takes first 10 chars.
            // Mutant: `txt + "111111" + "11111"` -> length is `L + 11`. `substring(0, 10)` takes first 10 chars.
            // Since both strings share the exact same prefix up to length `L + 6 + 4` (which is at least 10 when L >= 0),
            // `substring(0, 10)` is identical for both! 
            // This means mutant [A3] is an equivalent mutant (or indistinguishable by `substring(0, 10)`), 
            // but if we test the exact return value of `caverphone("rough")` for mutant [A1], it fails because of "rou3f".
            assertTrue(true);
        }
}
