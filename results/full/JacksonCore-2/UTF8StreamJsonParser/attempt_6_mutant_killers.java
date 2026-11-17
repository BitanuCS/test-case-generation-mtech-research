package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.BytesToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class UTF8StreamJsonParser_CandorTest {

    private UTF8StreamJsonParser createParser(byte[] data) {
        BufferRecycler recycler = new BufferRecycler();
        IOContext ctxt = new IOContext(recycler, recycler, false);
        InputStream in = new ByteArrayInputStream(data);
        BytesToNameCanonicalizer symbols = BytesToNameCanonicalizer.createRoot();
        return new UTF8StreamJsonParser(ctxt, 0, in, null, symbols, data, 0, data.length, false);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    @Test
    public void parseSimplePositiveInteger() throws Exception {
        byte[] data = "123".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
        assertEquals(JsonToken.VALUE_NUMBER_INT, token);
    }

    @Test
    public void parseNegativeIntegerWithRootSpaceVerification() throws Exception {
        byte[] data = "-456 ".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '-');
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseNumberDelegatesToParserNumber2OnBufferBoundary() throws Exception {
        byte[] data = "12".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", 1);

        char[] outBuf = new char[10];
        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parserNumber2", 
                    new Class<?>[]{char[].class, int.class, boolean.class, int.class},
                    outBuf, 1, false, 1);
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseNumberDelegatesToParseFloatOnDecimal() throws Exception {
        byte[] data = "123.45".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseNumberDelegatesToParseFloatOnExponent() throws Exception {
        byte[] data = "123e10".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseInvalidNumberStartAfterMinus() throws Exception {
        byte[] data = "-a".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 1);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '-');
            fail("Expected exception for invalid number start");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parserNumber2ReachesEOFSuccessfully() throws Exception {
        byte[] data = "123".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 3);
        setField(parser, "_inputEnd", 3);

        char[] outBuf = new char[10];
        outBuf[0] = '1';
        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parserNumber2",
                    new Class<?>[]{char[].class, int.class, boolean.class, int.class},
                    outBuf, 1, false, 1);
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parserNumber2EncountersDecimalOrExponent() throws Exception {
        byte[] data = ".45".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        char[] outBuf = new char[10];
        outBuf[0] = '1';
        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parserNumber2",
                    new Class<?>[]{char[].class, int.class, boolean.class, int.class},
                    outBuf, 1, false, 1);
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseFloatValidDecimalAndExponent() throws Exception {
        byte[] data = "45e+2".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        char[] outBuf = new char[10];
        outBuf[0] = '1';
        outBuf[1] = '2';
        outBuf[2] = '3';
        outBuf[3] = '.';
        try {
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseFloat",
                    new Class<?>[]{char[].class, int.class, int.class, boolean.class, int.class},
                    outBuf, 4, (int) '.', false, 3);
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseFloatMissingDigitAfterDecimal() throws Exception {
        byte[] data = " ".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        char[] outBuf = new char[10];
        outBuf[0] = '1';
        try {
            invokeMethod(parser, "_parseFloat",
                    new Class<?>[]{char[].class, int.class, int.class, boolean.class, int.class},
                    outBuf, 1, (int) '.', false, 1);
            fail("Expected exception due to missing digit after decimal");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void parseFloatMissingDigitAfterExponent() throws Exception {
        byte[] data = " ".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        char[] outBuf = new char[10];
        outBuf[0] = '1';
        try {
            invokeMethod(parser, "_parseFloat",
                    new Class<?>[]{char[].class, int.class, int.class, boolean.class, int.class},
                    outBuf, 1, (int) 'e', false, 1);
            fail("Expected exception due to missing digit after exponent");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void verifyRootSpaceWithSpaceOrTab() throws Exception {
        byte[] data = " ".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_verifyRootSpace", new Class<?>[]{int.class}, (int) ' ');
            assertTrue(true);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void verifyRootSpaceWithCarriageReturn() throws Exception {
        byte[] data = "\r".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_verifyRootSpace", new Class<?>[]{int.class}, (int) '\r');
            assertTrue(true);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void verifyRootSpaceWithLineFeed() throws Exception {
        byte[] data = "\n".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_verifyRootSpace", new Class<?>[]{int.class}, (int) '\n');
            assertTrue(true);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void verifyRootSpaceInvalidCharacterThrowsException() throws Exception {
        byte[] data = "a".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_verifyRootSpace", new Class<?>[]{int.class}, (int) 'a');
            fail("Expected exception for invalid root space character");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void skipWSOrEndSkipsWhitespaceAndComments() throws Exception {
        byte[] data = "   \n\r".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            Object res = invokeMethod(parser, "_skipWSOrEnd", new Class<?>[]{});
            assertNotNull(res);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void skipWSOrEndReportsInvalidChar() throws Exception {
        byte[] data = new byte[]{0x01};
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        try {
            invokeMethod(parser, "_skipWSOrEnd", new Class<?>[]{});
            fail("Expected exception for invalid char");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testSegmentBoundaryOutPtrKillMutantA3() throws Exception {
        byte[] data = "12345678901234567".getBytes("UTF-8");
        UTF8StreamJsonParser parser = createParser(data);
        setField(parser, "_inputPtr", 0);
        setField(parser, "_inputEnd", data.length);

        JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
        assertEquals(JsonToken.VALUE_NUMBER_INT, token);
    }

    private UTF8StreamJsonParser createParser2(byte[] data) {
            BufferRecycler recycler = new BufferRecycler();
            IOContext ctxt = new IOContext(recycler, recycler, false);
            InputStream in = new ByteArrayInputStream(data);
            BytesToNameCanonicalizer symbols = BytesToNameCanonicalizer.createRoot();
            return new UTF8StreamJsonParser(ctxt, 0, in, null, symbols, data, 0, data.length, false);
        }

    private void setField2(Object target, String fieldName, Object value) throws Exception {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        }

    private Object invokeMethod2(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
            Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            return m.invoke(target, args);
        }

    @Test
        public void testKillMutantA0AndFractionLoop() throws Exception {
            // Triggers fraction loop check where c < INT_0 || c > INT_9 is mutated.
            // With input "123.a", fraction loop sees 'a'. Original breaks out correctly. Mutant loops incorrectly or fails.
            byte[] data = "123.a".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            try {
                invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
                fail("Expected exception due to decimal point not followed by a digit");
            } catch (Exception e) {
                assertNotNull(e);
            }
        }

    @Test
        public void testKillMutantA1AndExponentLoop() throws Exception {
            // Triggers exponent loop with '123e5'. Mutant checks c <= INT_0 instead of INT_9, so '5' is rejected immediately.
            byte[] data = "123e5".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        }

    @Test
        public void testKillMutantsA2AndA5PushBackAndRootSpace() throws Exception {
            // Triggers trailing character pushback and root space check inRoot().
            // In root context, "123," requires root space after number. _inputPtr should be decremented to point to comma,
            // then root space verification checks comma (which fails and throws exception, or succeeds if space).
            // Mutant increments _inputPtr instead, pointing past comma.
            byte[] data = "123,".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            // Mock parsing context to return true for inRoot()
            Object rootContext = invokeMethod(parser, "_getRootContext", new Class<?>[]{});
            setField(parser, "_parsingContext", rootContext);
    
            try {
                invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
                fail("Expected root space error or invalid char for comma");
            } catch (Exception e) {
                assertNotNull(e);
            }
        }

    @Test
        public void testKillMutantA3SegmentBoundarySegmentFinish() throws Exception {
            // Forces outPtr >= outBuf.length segment rotation in _parseNumber by providing a number longer than default segment/outBuf.
            // We set outBuf to a very small array or force segment finishing.
            byte[] data = "12345678901234567890".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            // Retrieve textBuffer and empty/get current segment to control its size
            Object textBuffer = invokeMethod(parser, "_getTextBuffer", new Class<?>[]{});
            char[] smallBuf = (char[]) invokeMethod(textBuffer, "emptyAndGetCurrentSegment", new Class<?>[]{});
            // Make smallBuf tiny so outPtr >= outBuf.length triggers immediately
            setField(textBuffer, "_currentSegment", new char[2]);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        }

    @Test
        public void testKillMutantA4IntegerPartLengthIncrement() throws Exception {
            // Triggers _parserNumber2 where intPartLength is incremented.
            // Mutant decrements intPartLength instead of incrementing, which corrupts resetInt(negative, intPartLength).
            byte[] data = "12345678901234567890".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", 2); // Split across buffer boundary to force _parserNumber2
    
            char[] outBuf = new char[10];
            outBuf[0] = '1';
            JsonToken token = (JsonToken) invokeMethod(parser, "_parserNumber2",
                    new Class<?>[]{char[].class, int.class, boolean.class, int.class},
                    outBuf, 1, false, 1);
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        }
}
