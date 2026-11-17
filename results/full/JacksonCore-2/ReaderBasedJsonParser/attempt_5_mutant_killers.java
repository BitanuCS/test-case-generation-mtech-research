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
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("12345", parser.getText());
    }

    @Test
    public void parseDecimalNumberInBuffer() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.34");
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("12.34", parser.getText());
    }

    @Test
    public void parseExponentialNumberInBuffer() throws IOException {
        ReaderBasedJsonParser parser = createParser("1e+10");
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertTrue(token.isNumeric());
        assertEquals("1e+10", parser.getText());
    }

    @Test
    public void parseNumberBufferSplitTriggersParseNumber2() throws IOException {
        ReaderBasedJsonParser parser = createParser("1");
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertEquals("1", parser.getText());
    }

    @Test(expected = JsonParseException.class)
    public void parseInvalidNumberStartTriggersHandler() throws IOException {
        ReaderBasedJsonParser parser = createParser("-a");
        parser.nextToken();
    }

    @Test(expected = JsonParseException.class)
    public void decimalPointNotFollowedByDigitThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.a");
        parser.nextToken();
    }

    @Test(expected = JsonParseException.class)
    public void exponentIndicatorNotFollowedByDigitThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("1ea");
        parser.nextToken();
    }

    @Test
    public void parseNumberBufferSplitBasicInteger() throws IOException {
        ReaderBasedJsonParser parser = createParser("123");
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertEquals("123", parser.getText());
    }

    @Test(expected = JsonParseException.class)
    public void parseNumberMissingIntegerPartThrowsException() throws IOException {
        ReaderBasedJsonParser parser = createParser("-a");
        parser.nextToken();
    }

    @Test
    public void parseNumberDecimalFractionAndExponentSplit() throws IOException {
        ReaderBasedJsonParser parser = createParser("12.34e5");
        JsonToken token = parser.nextToken();
        assertNotNull(token);
        assertEquals("12.34e5", parser.getText());
    }

    private ReaderBasedJsonParser createParser2(String content) {
            BufferRecycler recycler = new BufferRecycler();
            IOContext ctxt = new IOContext(recycler, content, false);
            StringReader reader = new StringReader(content);
            CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot();
            ReaderBasedJsonParser parser = new ReaderBasedJsonParser(ctxt, 0, reader, null, symbols);
            try {
                parser.loadMore();
            } catch (IOException e) {
                // ignore
            }
            return parser;
        }

    @Test
        public void testRootSpaceValidationAfterNumber() throws IOException {
            // Triggers root space verification (e.g. valid root numbers followed by space)
            ReaderBasedJsonParser parser = createParser("123 ");
            JsonToken token = parser.nextToken();
            assertNotNull(token);
            assertEquals("123", parser.getText());
        }

    @Test
        public void testExponentNineDigitEdgeCase() throws IOException {
            // Triggers the exponent loop with digit '9' to kill mutant on <= vs <
            ReaderBasedJsonParser parser = createParser("1e9");
            JsonToken token = parser.nextToken();
            assertNotNull(token);
            assertEquals("1e9", parser.getText());
        }

    @Test(expected = JsonParseException.class)
        public void testDecimalPointNotFollowedByDigitInParseNumber() throws IOException {
            // Triggers the check for fractLen == 0 vs fractLen < 0 or fractLen == 1 in _parseNumber
            ReaderBasedJsonParser parser = createParser("12. ");
            parser.nextToken();
        }

    @Test(expected = JsonParseException.class)
        public void testIntegerZeroComparisonInParseNumber() throws IOException {
            // Triggers relational operator mutation on INT_0 check (ch < INT_0 vs ch <= INT_0)
            ReaderBasedJsonParser parser = createParser("00");
            parser.nextToken();
        }

    @Test
        public void testBufferSplitWithSegmentFinish() throws IOException {
            // Forces segment finish in _parseNumber2 by providing a longer number that causes segment rollover
            // We can create a long string of digits that exceeds the initial small segment size if applicable,
            // or trigger segment finish paths.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5000; i++) {
                sb.append('9');
            }
            sb.append(".0e1");
            ReaderBasedJsonParser parser = createParser(sb.toString());
            JsonToken token = parser.nextToken();
            assertNotNull(token);
            assertTrue(token.isNumeric());
        }
}
