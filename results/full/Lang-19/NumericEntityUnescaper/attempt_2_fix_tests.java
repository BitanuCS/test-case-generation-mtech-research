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
}
