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
    public void parseNumberBufferSplitBasicInteger() throws IOException {
        ReaderBasedJsonParser parser = createParser("123");
        // Trigger parsing via public/protected entry points that eventually hit _parseNumber2 if split
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertEquals("123", parser.getText());
    }

    @Test
    public void parseNumberLeadingZeroVerification() throws IOException {
        ReaderBasedJsonParser parser = createParser("05");
        JsonToken token = parser._parseNumber('0');
        assertNotNull(token);
    }

    @Test(expected = JsonParseException.class)
    public void parseNumberMissingIntegerPartThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("-a");
        parser._parseNumber('-');
    }

    @Test
    public void parseNumberDecimalFractionAndExponentSplit() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.34e5");
        JsonToken token = parser._parseNumber('1');
        assertNotNull(token);
        assertEquals("12.34e5", parser.getText());
    }
}
