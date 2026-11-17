package com.fasterxml.jackson.core.json;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;
import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;

public class ReaderBasedJsonParser_CandorTest {

    private ReaderBasedJsonParser createParser(String content) {
        BufferRecycler recycler = new BufferRecycler();
        IOContext ctxt = new IOContext(recycler, content, false);
        StringReader reader = new StringReader(content);
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot();
        ReaderBasedJsonParser parser = new ReaderBasedJsonParser(ctxt, 0, reader, null, symbols);
        // Force initial buffer load
        try {
            parser.loadMore();
        } catch (IOException e) {
            // ignore
        }
        return parser;
    }

    @Test
    public void parseSimpleIntegerInBuffer() throws IOException {
        ReaderBasedJsonParser parser = createParser("12345");
        // ch is already read, let's call _parseNumber with '1' (INT_1 = '1')
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("12345", parser.getText());
    }

    @Test
    public void parseDecimalNumberInBuffer() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.34");
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("12.34", parser.getText());
    }

    @Test
    public void parseExponentialNumberInBuffer() throws IOException {
        ReaderBasedJsonParser parser = createParser("1e+10");
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("1e+10", parser.getText());
    }

    @Test
    public void parseNumberBufferSplitTriggersParseNumber2() throws IOException {
        // Setup input shorter than the lookahead or forced buffer boundary split scenario
        ReaderBasedJsonParser parser = createParser("1");
        // _inputEnd is 1, so ptr >= _inputEnd will break dummy_loop and trigger _parseNumber2
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertEquals("1", parser.getText());
    }

    @Test(expected = JsonParseException.class)
    public void parseInvalidNumberStartTriggersHandler() throws IOException {
        ReaderBasedJsonParser parser = createParser("-a");
        // '-' is negative sign, next char is 'a' which is not a digit
        parser._parseNumber('-');
    }

    @Test(expected = JsonParseException.class)
    public void decimalPointNotFollowedByDigitThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.a");
        parser._parseNumber('1');
    }

    @Test(expected = JsonParseException.class)
    public void exponentIndicatorNotFollowedByDigitThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("1ea");
        parser._parseNumber('1');
    }

    @Test
    public void parseNumber2BasicIntegerWithSegmentCopy() throws IOException {
        ReaderBasedJsonParser parser = createParser("123");
        // Trigger _parseNumber2 by calling it directly
        JsonToken token = parser._parseNumber2(false);
        assertNotNull(token);
        assertEquals("123", parser.getText());
    }

    @Test
    public void parseNumber2LeadingZeroVerification() throws IOException {
        ReaderBasedJsonParser parser = createParser("05");
        // Trigger _parseNumber2 with negative = false starting at '0'
        JsonToken token = parser._parseNumber2(false);
        assertNotNull(token);
    }

    @Test(expected = JsonParseException.class)
    public void parseNumber2MissingIntegerPartThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("-a");
        // If negative is true, _parseNumber2 looks for digit after minus
        parser._parseNumber2(true);
    }

    @Test
    public void parseNumber2DecimalFractionAndExponentSplit() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.34e5");
        JsonToken token = parser._parseNumber2(false);
        assertNotNull(token);
        assertEquals("12.34e5", parser.getText());
    }

    @Test
    public void verifyRootSpaceWithSpaceCharacter() throws IOException {
        ReaderBasedJsonParser parser = createParser(" 123");
        // _inputPtr is 0, let's call _verifyRootSpace with ' '
        parser._verifyRootSpace(' ');
    }

    @Test
    public void verifyRootSpaceWithTabulator() throws IOException {
        ReaderBasedJsonParser parser = createParser("\t123");
        parser._verifyRootSpace('\t');
    }

    @Test
    public void verifyRootSpaceWithCarriageReturn() throws IOException {
        ReaderBasedJsonParser parser = createParser("\r\n123");
        parser._verifyRootSpace('\r');
    }

    @Test
    public void verifyRootSpaceWithLineFeed() throws IOException {
        ReaderBasedJsonParser parser = createParser("\n123");
        parser._verifyRootSpace('\n');
    }

    @Test(expected = JsonParseException.class)
    public void verifyRootSpaceInvalidCharacterThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("1123");
        parser._verifyRootSpace('1');
    }
}
