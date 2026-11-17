package org.jsoup.parser;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test suite for CharacterReader constructor and consumeToEnd method.
 */
public class CharacterReader_CandorTest {

    @Test
    public void consumeToEndFromStart() {
        CharacterReader reader = new CharacterReader("Hello World");
        String result = reader.consumeToEnd();
        assertEquals("Hello World", result);
        assertEquals(11, reader.pos());
    }

    @Test
    public void consumeToEndFromMiddle() {
        CharacterReader reader = new CharacterReader("Hello World");
        reader.consume();
        reader.consume();
        reader.consume();
        reader.consume();
        reader.consume();
        reader.consume(); // pos is now 6 ('W')
        String result = reader.consumeToEnd();
        assertEquals("World", result);
        assertEquals(11, reader.pos());
    }

    @Test
    public void consumeToEndEmptyInput() {
        CharacterReader reader = new CharacterReader("");
        String result = reader.consumeToEnd();
        assertEquals("", result);
        assertEquals(0, reader.pos());
    }

    @Test
    public void consumeToEndAtEnd() {
        CharacterReader reader = new CharacterReader("Test");
        reader.consumeToEnd();
        String result = reader.consumeToEnd();
        assertEquals("", result);
        assertEquals(4, reader.pos());
    }

    @Test
    public void consumeToEndSingleCharacter() {
        CharacterReader reader = new CharacterReader("A");
        String result = reader.consumeToEnd();
        assertEquals("A", result);
        assertEquals(1, reader.pos());
    }

    @Test
    public void consumeToEndAfterPartialConsumption() {
        CharacterReader reader = new CharacterReader("jsoup");
        reader.consume(); // consumes 'j', pos becomes 1
        String result = reader.consumeToEnd();
        assertEquals("soup", result);
        assertEquals(5, reader.pos());
    }

    @Test
    public void consumeToEndWithNormalizedCarriageReturns() {
        CharacterReader reader = new CharacterReader("line1\r\nline2");
        String result = reader.consumeToEnd();
        assertEquals("line1\nline2", result);
        assertEquals(10, reader.pos());
    }

    @Test
    public void consumeToEndMultipleConsecutiveCalls() {
        CharacterReader reader = new CharacterReader("ABC");
        String first = reader.consumeToEnd();
        assertEquals("ABC", first);
        String second = reader.consumeToEnd();
        assertEquals("", second);
        assertEquals(3, reader.pos());
    }

    @Test(expected = IllegalArgumentException.class)
    public void consumeToEndNullInputConstructorValidation() {
        new CharacterReader(null);
    }

    @Test
    public void consumeToEndWithWhitespace() {
        CharacterReader reader = new CharacterReader("   ");
        String result = reader.consumeToEnd();
        assertEquals("   ", result);
        assertEquals(3, reader.pos());
    }

    @Test
    public void consumeToEndSpecialCharacters() {
        CharacterReader reader = new CharacterReader("!@#$%^&*()");
        String result = reader.consumeToEnd();
        assertEquals("!@#$%^&*()", result);
        assertEquals(10, reader.pos());
    }

    @Test
    public void consumeToEndUnicodeCharacters() {
        String input = "unicode \uD83D\uDE80 test";
        CharacterReader reader = new CharacterReader(input);
        String result = reader.consumeToEnd();
        assertEquals(input, result);
        assertEquals(input.length(), reader.pos());
    }
}
