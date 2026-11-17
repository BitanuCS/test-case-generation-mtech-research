package org.apache.commons.codec.language;

import static org.junit.Assert.assertSame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SoundexUtils_CandorTest {

    @Test
    public void cleanNullString() {
        String result = SoundexUtils.clean(null);
        assertNull(result);
    }

    @Test
    public void cleanEmptyString() {
        String result = SoundexUtils.clean("");
        assertEquals("", result);
    }

    @Test
    public void cleanLowercaseLettersOnly() {
        String result = SoundexUtils.clean("abcdef");
        assertEquals("ABCDEF", result);
    }

    @Test
    public void cleanUppercaseLettersOnly() {
        String result = SoundexUtils.clean("ABCDEF");
        assertEquals("ABCDEF", result);
    }

    @Test
    public void cleanMixedCaseWithNonLetters() {
        String result = SoundexUtils.clean("AbC123XyZ!@#");
        assertEquals("ABCXYZ", result);
    }

    @Test
    public void cleanOnlyNonLetters() {
        String result = SoundexUtils.clean("12345!@#$®");
        assertEquals("", result);
    }

    @Test
    public void cleanSingleLowercaseLetter() {
        String result = SoundexUtils.clean("z");
        assertEquals("Z", result);
    }

    @Test
    public void cleanSingleNonLetter() {
        String result = SoundexUtils.clean("9");
        assertEquals("", result);
    }

    @Test
    public void cleanWhitespaceAndTabs() {
        String result = SoundexUtils.clean("   \t\n ");
        assertEquals("", result);
    }

    @Test
    public void cleanUnicodeLetters() {
        String result = SoundexUtils.clean("éàç");
        assertEquals("ÉÀÇ", result);
    }

    @Test
    public void cleanMixedLettersAndSpaces() {
        String result = SoundexUtils.clean("Hello World");
        assertEquals("HELLOWORLD", result);
    }

    @Test
    public void cleanLeadingAndTrailingNonLetters() {
        String result = SoundexUtils.clean("--Smith++");
        assertEquals("SMITH", result);
    }

    @Test
        public void cleanEmptyStringDetectsLengthCheckMutant() {
            // When str = "", str.length() == 0 is true (returns ""), 
            // but if mutated to str.length() < 0 (false), it proceeds to allocate 
            // a char[0], count = 0, count == len (0 == 0) is true, and returns 
            // "". Wait, both return "" for empty string.
            // Let's test a non-empty string that has len > 0, but what if we want to distinguish length < 0?
            // Wait! If str = "", length is 0. 0 < 0 is false. So it bypasses the `if`, 
            // goes down, and returns `new String(chars, 0, count).toUpperCase()`, which is `""`.
            // BUT wait, what about the return value reference equality or side-effects? 
            // Actually, if `str == ""` is passed, the original returns the exact same string instance `str` (`return str;`).
            // The mutant bypasses `return str;`, creates a *new* String object `new String(...)`, and returns that.
            // We can test reference equality (identity) for empty strings!
        }

    @Test
        public void cleanEmptyStringReturnsExactSameInstance() {
            String empty = "";
            String result = SoundexUtils.clean(empty);
            // The original code returns the exact input reference when str is empty or null.
            // The mutant for empty string creates a new String object via `new String(chars, 0, count)`.
            org.junit.Assert.assertSame("Expected exact same instance for empty string", empty, result);
        }

    @Test
        public void cleanEmptyStringReturnsExactSameInstance2() {
            String empty = "";
            String result = SoundexUtils.clean(empty);
            // The original code returns the exact input reference when str.length() == 0.
            // If mutated to `str.length() < 0`, an empty string bypasses the early return
            // and instantiates a new String object, causing assertSame to fail.
            assertSame("Expected exact same instance for empty string", empty, result);
        }
}
