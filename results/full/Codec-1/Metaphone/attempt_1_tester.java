package org.apache.commons.codec.language;

import junit.framework.TestCase;
import org.apache.commons.codec.EncoderException;

public class Metaphone_CandorTest extends TestCase {

    private Metaphone metaphone;

    protected void setUp() throws Exception {
        super.setUp();
        metaphone = new Metaphone();
    }

    protected void tearDown() throws Exception {
        metaphone = null;
        super.tearDown();
    }

    public void testNullInput() {
        String result = metaphone.metaphone(null);
        assertEquals("", result);
    }

    public void testEmptyInput() {
        String result = metaphone.metaphone("");
        assertEquals("", result);
    }

    public void testSingleCharacterInput() {
        String result = metaphone.metaphone("a");
        assertEquals("A", result);
    }

    public void testInitialKN() {
        String result = metaphone.metaphone("knight");
        assertEquals("NT", result);
    }

    public void testInitialAE() {
        String result = metaphone.metaphone("aegis");
        assertEquals("X", result);
    }

    public void testInitialWR() {
        String result = metaphone.metaphone("write");
        assertEquals("RT", result);
    }

    public void testInitialWH() {
        String result = metaphone.metaphone("white");
        assertEquals("WT", result);
    }

    public void testInitialX() {
        String result = metaphone.metaphone("xylophone");
        assertEquals("SLFN", result);
    }

    public void testDuplicateCharactersExceptC() {
        String result = metaphone.metaphone("balloon");
        assertEquals("BLN", result);
    }

    public void testCHRules() {
        String result = metaphone.metaphone("child");
        assertEquals("XLT", result);
    }

    public void testDGERules() {
        String result = metaphone.metaphone("ledger");
        assertEquals("LJ", result);
    }

    public void testSilentGH() {
        String result = metaphone.metaphone("high");
        assertEquals("H", result);
    }

    public void testTHRule() {
        String result = metaphone.metaphone("the");
        assertEquals("0", result);
    }

    public void testXExpansion() {
        String result = metaphone.metaphone("box");
        assertEquals("BKS", result);
    }

    public void testMaxCodeLengthConstraint() {
        metaphone.setMaxCodeLen(6);
        String result = metaphone.metaphone("encyclopedia");
        assertEquals("NSTLPR", result);
    }

    public void testEncodeObjectValidString() throws EncoderException {
        Object result = metaphone.encode("test");
        assertEquals("TST", result);
    }

    public void testEncodeObjectInvalidTypeThrowsException() {
        try {
            metaphone.encode(Integer.valueOf(123));
            fail("Expected EncoderException");
        } catch (EncoderException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testIsMetaphoneEqual() {
        boolean equal = metaphone.isMetaphoneEqual("white", "white");
        assertTrue(equal);
    }

    public void testGetMaxCodeLenInitial() {
        assertEquals(4, metaphone.getMaxCodeLen());
    }
}
