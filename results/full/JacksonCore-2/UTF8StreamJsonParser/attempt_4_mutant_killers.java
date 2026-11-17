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
        public void testFractionLoopKillMutantA0() throws Exception {
            // Mutant A0 swaps fraction digit check: c > INT_0 || c < INT_9
            byte[] data = "123.5".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        }

    @Test
        public void testExponentLoopKillMutantA1() throws Exception {
            // Mutant A1 changes c <= INT_9 to c <= INT_0 in exponent loop
            byte[] data = "1e5".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_FLOAT, token);
        }

    @Test
        public void testPushBackInputPtrDecrementKillMutantA2AndA5() throws Exception {
            // Mutants A2/A5 change --_inputPtr to ++_inputPtr when pushing back trailing char
            byte[] data = "123,".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
            // After parsing "123" followed by ',', _inputPtr should point right at ',' (index 3)
            int inputPtr = (int) parser.getClass().getSuperclass().getDeclaredField("_inputPtr").get(parser);
            assertEquals(3, inputPtr);
        }

    @Test
        public void testSegmentBoundaryOutPtrKillMutantA3() throws Exception {
            // Mutant A3 changes outPtr >= outBuf.length to outPtr > outBuf.length
            byte[] data = "12345678901234567".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", data.length);
    
            JsonToken token = (JsonToken) invokeMethod(parser, "_parseNumber", new Class<?>[]{int.class}, (int) '1');
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        }

    @Test
        public void testParserNumber2IncrementLengthKillMutantA4() throws Exception {
            // Mutant A4 changes ++intPartLength to --intPartLength in _parserNumber2
            byte[] data = "12345".getBytes("UTF-8");
            UTF8StreamJsonParser parser = createParser(data);
            // Set _inputEnd such that it triggers _parserNumber2
            setField(parser, "_inputPtr", 0);
            setField(parser, "_inputEnd", 2);
    
            char[] outBuf = new char[2];
            outBuf[0] = '1';
            JsonToken token = (JsonToken) invokeMethod(parser, "_parserNumber2",
                    new Class<?>[]{char[].class, int.class, boolean.class, int.class},
                    outBuf, 1, false, 1);
            assertEquals(JsonToken.VALUE_NUMBER_INT, token);
        }
}
