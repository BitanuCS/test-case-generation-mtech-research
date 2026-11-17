package org.apache.commons.lang3.text.translate;

import junit.framework.TestCase;
import java.io.StringWriter;
import java.io.IOException;

/**
 * Unit tests for {@link org.apache.commons.lang3.text.translate.NumericEntityUnescaper}.
 */
public class NumericEntityUnescaper_CandorTest extends TestCase {

    public void testTranslateDecimalEntityWithSemicolon() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#65;", 0, writer);
        assertEquals(5, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateHexEntityLowercaseWithSemicolon() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#x41;", 0, writer);
        assertEquals(6, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateHexEntityUppercaseWithSemicolon() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#X41;", 0, writer);
        assertEquals(6, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateDecimalEntityWithoutSemicolon() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#65", 0, writer);
        assertEquals(4, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateSupplementaryCodepoint() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        // 0x1D306 is a supplementary codepoint (> 0xFFFF)
        int consumed = unescaper.translate("&#x1D306;", 0, writer);
        assertEquals(9, consumed);
        assertEquals(new String(Character.toChars(0x1D306)), writer.toString());
    }

    public void testTranslateNonEntityNoAmpersand() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("AB&#65;", 2, writer);
        assertEquals(5, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateIncompleteEntityJustAmpersandHash() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#", 0, writer);
        assertEquals(0, consumed);
        assertEquals("", writer.toString());
    }

    public void testTranslateIncompleteHexEntityOnlyX() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#x", 0, writer);
        assertEquals(0, consumed);
        assertEquals("", writer.toString());
    }

    public void testTranslateInvalidCharactersInEntity() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#Z;", 0, writer);
        assertEquals(0, consumed);
        assertEquals("", writer.toString());
    }

    public void testTranslateWithOffsetIndex() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("abc&#65;def", 3, writer);
        assertEquals(5, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateHexEntityWithoutSemicolon() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#x41", 0, writer);
        assertEquals(5, consumed);
        assertEquals("A", writer.toString());
    }

    public void testTranslateBoundaryIndexTooCloseToEnd() throws IOException {
        NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&a", 0, writer);
        assertEquals(0, consumed);
        assertEquals("", writer.toString());
    }

    public void testTranslateHexEntityLowercaseF() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            int consumed = unescaper.translate("&#x66;", 0, writer);
            assertEquals(6, consumed);
            assertEquals("f", writer.toString());
        }

    public void testTranslateExactBoundaryLengthThree() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            int consumed = unescaper.translate("&#9", 0, writer);
            assertEquals(3, consumed);
            assertEquals("\t", writer.toString());
        }

    public void testTranslateDecimalEntityDigitNine() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            int consumed = unescaper.translate("&#57;", 0, writer);
            assertEquals(5, consumed);
            assertEquals("9", writer.toString());
        }

    public void testTranslateHexEntityUppercaseA() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            int consumed = unescaper.translate("&#x41;", 0, writer);
            assertEquals(6, consumed);
            assertEquals("A", writer.toString());
        }

    public void testTranslateHexEntityUppercaseF() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            int consumed = unescaper.translate("&#x46;", 0, writer);
            assertEquals(6, consumed);
            assertEquals("F", writer.toString());
        }

    public void testTranslateHexEntityLowercaseFWithoutSemicolonKillsA4() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            // Mutant [A4] checks up to 'e' instead of 'f' for lowercase hex.
            // For "&#x66", the 'f' (hex 66) should be consumed, resulting in "f" (consumed 5).
            // Mutant [A4] stops at '6', leaving '6;' or treating '6' as end, returning incorrect value or incomplete parse.
            int consumed = unescaper.translate("&#x66", 0, writer);
            assertEquals(5, consumed);
            assertEquals("f", writer.toString());
        }

    public void testTranslateHexEntityUppercaseAGreaterThanKillsR15() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            // Mutant [R15] changes (input.charAt(end) >= 'A') to (> 'A').
            // For 'A' (hex 41), '> 'A'' is false, so 'A' would not be matched in the while loop.
            int consumed = unescaper.translate("&#x41;", 0, writer);
            assertEquals(6, consumed);
            assertEquals("A", writer.toString());
        }

    public void testTranslateHexEntityUppercaseFLessThanKillsR16() throws IOException {
            NumericEntityUnescaper unescaper = new NumericEntityUnescaper();
            StringWriter writer = new StringWriter();
            // Mutant [R16] changes (input.charAt(end) <= 'F') to (< 'F').
            // For 'F' (hex 46), '< 'F'' is false, so 'F' would not be matched in the while loop.
            int consumed = unescaper.translate("&#x46;", 0, writer);
            assertEquals(6, consumed);
            assertEquals("F", writer.toString());
        }
}
