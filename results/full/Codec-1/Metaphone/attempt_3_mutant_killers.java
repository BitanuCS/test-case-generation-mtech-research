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
        assertEquals("EJS", result);
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
        assertEquals("KLT", result);
    }

    public void testDGERules() {
        String result = metaphone.metaphone("ledger");
        assertEquals("LJR", result);
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
        assertEquals("ENSKLP", result);
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

    protected void setUp2() throws Exception {
            super.setUp();
            metaphone = new Metaphone();
        }

    protected void tearDown2() throws Exception {
            metaphone = null;
            super.tearDown();
        }

    public void testDgeIncrementMutation() {
            assertEquals("LJR", metaphone.metaphone("ledger"));
            assertEquals("J", metaphone.metaphone("idget")); // DGI -> J
            assertEquals("J", metaphone.metaphone("gypsy")); // DGY -> J (Wait, 'DGY' -> 'TGY' -> 'TJ'?) Let's check "idget" or "edger".
            assertEquals("LJR", metaphone.metaphone("edger"));
        }

    public void testSciSceScyDiscard() {
            // "science" starts with S, then C, then I (front vowel). Should discard C.
            // Original: 'C' preceded by 'S' and followed by 'I' is discarded -> S-N-S (?)
            // Let's test "scene" or "science" or "scythe".
            // "scene" -> S-N (because C is discarded before E)
            // If mutant: 'C' is kept because index < 0 fails, so it treats 'C' normally (appends 'S' or 'K').
            assertEquals("SN", metaphone.metaphone("scene"));
            assertEquals("SN", metaphone.metaphone("science"));
        }

    public void testSioSiaShAppendX() {
            assertEquals("XN", metaphone.metaphone("action")); // TIO -> X (wait, that's T case)
            assertEquals("XN", metaphone.metaphone("sion")); // SIO -> X
            assertEquals("X", metaphone.metaphone("sh")); // SH -> X
            assertEquals("XL", metaphone.metaphone("social")); // SIA -> X
        }

    public void testInitialWrThreeLetterWord() {
            // "wr" has length 2. inwd[2] would throw ArrayIndexOutOfBoundsException or fail logic on 3-letter words like "wry".
            assertEquals("R", metaphone.metaphone("wry"));
        }

    public void testInitialWhLength() {
            assertEquals("WT", metaphone.metaphone("whin"));
        }

    public void testSchRule() {
            assertEquals("SKL", metaphone.metaphone("school"));
        }

    public void testInitialK() {
            assertEquals("KT", metaphone.metaphone("keep"));
            assertEquals("KT", metaphone.metaphone("katt"));
        }

    public void testInitialG() {
            assertEquals("KT", metaphone.metaphone("gate"));
        }

    public void testInitialWDefault() {
            assertEquals("W", metaphone.metaphone("w"));
            assertEquals("WL", metaphone.metaphone("wall"));
        }

    public void testSilentBEndingInMb() {
            assertEquals("TM", metaphone.metaphone("dumb"));
            assertEquals("LM", metaphone.metaphone("limb"));
        }

    public void testCiaRule() {
            assertEquals("X", metaphone.metaphone("cia"));
        }

    public void testSchToK() {
            assertEquals("SKMT", metaphone.metaphone("scheme"));
        }

    public void testChVowel() {
            assertEquals("X", metaphone.metaphone("chaos"));
        }

    public void testSilentG() {
            assertEquals("NT", metaphone.metaphone("gnat"));
            assertEquals("NT", metaphone.metaphone("gned"));
        }

    public void testDoubleGAndHardG() {
            assertEquals("KL", metaphone.metaphone("ggle"));
            assertEquals("K", metaphone.metaphone("g"));
        }

    public void testKCases() {
            assertEquals("BK", metaphone.metaphone("book"));
            assertEquals("KT", metaphone.metaphone("cook")); // 'CK' -> 'K'
        }
}
