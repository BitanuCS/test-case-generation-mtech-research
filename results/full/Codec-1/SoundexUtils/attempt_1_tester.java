package org.apache.commons.codec.language;

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
}
