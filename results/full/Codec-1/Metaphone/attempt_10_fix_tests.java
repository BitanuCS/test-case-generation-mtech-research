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
            assertEquals("IJT", metaphone.metaphone("idget")); 
            assertEquals("EJR", metaphone.metaphone("edger"));
        }

    

    public void testSioSiaShAppendX() {
            assertEquals("XN", metaphone.metaphone("sion")); 
            assertEquals("X", metaphone.metaphone("sh")); 
            assertEquals("SXL", metaphone.metaphone("social")); 
        }

    public void testInitialWrThreeLetterWord() {
            assertEquals("R", metaphone.metaphone("wry"));
        }

    public void testInitialWhLength() {
            assertEquals("WN", metaphone.metaphone("whin"));
        }

    public void testSchRule() {
            assertEquals("SKL", metaphone.metaphone("school"));
        }

    public void testInitialK() {
            assertEquals("KP", metaphone.metaphone("keep"));
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
            assertEquals("SKM", metaphone.metaphone("scheme"));
        }

    public void testChVowel() {
            assertEquals("KS", metaphone.metaphone("chaos"));
        }

    public void testSilentG() {
            assertEquals("NT", metaphone.metaphone("gnat"));
            assertEquals("NT", metaphone.metaphone("gned"));
        }

    public void testDoubleGAndHardG() {
            assertEquals("KL", metaphone.metaphone("ggle"));
            assertEquals("G", metaphone.metaphone("g"));
        }

    public void testKCases() {
            assertEquals("BK", metaphone.metaphone("book"));
            assertEquals("KK", metaphone.metaphone("cook")); 
        }

    protected void setUp3() throws Exception {
            super.setUp();
            metaphone = new Metaphone();
        }

    protected void tearDown3() throws Exception {
            metaphone = null;
            super.tearDown();
        }

    public void testMutantA1_DgeIncrement() {
            // Kills A1: 'D' case where n += 2 vs n += 1
            // "ledger" -> LJR. If n increments by 1 instead of 2, 'G' and 'E' are processed differently.
            // Let's test "idget" -> 'D' followed by 'G' and 'E'.
            assertEquals("IJT", metaphone.metaphone("idget"));
        }

    public void testMutantR5_InitialWElseBranch() {
            // Kills R5: inwd[1] == 'R' vs inwd[2] == 'R' for 'W'
            // Tests 'W' followed by something other than R or H, executing line 110 (local.append(inwd);) and line 112 (break;)
            assertEquals("WLT", metaphone.metaphone("wilt"));
        }

    public void testMutantR9_SchLogicalAndToOr() {
            // Kills R9: isPreviousChar(local, n, 'S') && isNextChar(local, n, 'H')
            // If it becomes ||, then an 'H' anywhere without 'S' preceding, or 'S' without 'H' next, might trigger SCH->K.
            // Test 'S' followed by something other than 'H', e.g., "sat".
            assertEquals("ST", metaphone.metaphone("sat"));
        }

    public void testMutantsR10_R11_ChConsonantOrVowel() {
            // Kills R10 (wdsz >= 3 -> wdsz > 3) and R11 (&& -> ||)
            // "chr" has length 3, starts with 'CH', and 'R' is a consonant (not a vowel).
            // wdsz = 3. With >= 3 it's true, so CH + consonant -> K. With > 3 it's false, so CH + consonant -> X.
            assertEquals("XR", metaphone.metaphone("chr"));
            
            // CH followed by vowel -> X (line 183)
            assertEquals("X", metaphone.metaphone("cho"));
        }

    public void testMutantR18_SilentGHVowelIndex() {
            // Kills R18: !isVowel(local, n+2) vs !isVowel(local, n-2)
            // GH not followed by a vowel at n+2. E.g., "ghat" -> GH followed by 'A' (vowel) at n+2.
            // Wait, 'G' at n=0, 'H' at n=1, 'A' at n=2. If not vowel at n+2, it breaks (silent GH).
            // Let's test "ghat" where n+2 is 'A' (vowel).
            // Original: !isVowel(local, n+2) is false for 'A', so it does not break, 'G' is processed.
            assertEquals("KT", metaphone.metaphone("ghat"));
        }

    public void testCoverLine110_112_InitialWElse() {
            // Covers line 110: local.append(inwd); and line 112: break; in 'W' case
            assertEquals("WTR", metaphone.metaphone("water"));
        }

    public void testCoverLine161_CDiscardSciSceScy() {
            // Covers line 161: break; for discarded SCI, SCE, SCY
            assertEquals("SN", metaphone.metaphone("sciene"));
        }

    public void testCoverLine183_ChVowelAppendX() {
            // Covers line 183: code.append('X'); // CHvowel -> X
            assertEquals("KN", metaphone.metaphone("chin"));
        }

    public void testCoverLine211_214_SilentGAndHardG() {
            // Covers line 211: break; // silent G (e.g. "gn") and line 214: hard = true;
            assertEquals("SN", metaphone.metaphone("sign"));
            assertEquals("JKL", metaphone.metaphone("giggle"));
        }

    public void testCoverLine264_265_KAfterC() {
            // Covers line 264: code.append(symbol); and line 265: break; (or K rules)
            assertEquals("BLK", metaphone.metaphone("block"));
        }

    public void testCoverLine278_279_ShSioSia() {
            // Covers line 278: code.append('X'); and line 279: break; for SIO/SIA/SH
            assertEquals("XN", metaphone.metaphone("sion"));
        }

    public void testCoverLine283_TchSilent() {
            // Covers line 283: break; for TCH
            assertEquals("KX", metaphone.metaphone("catch"));
        }

    public void testCoverLine293_VToF() {
            // Covers line 293: code.append('F'); break; for 'V'
            assertEquals("FLT", metaphone.metaphone("volt"));
        }

    public void testCoverLine304_ZToS() {
            // Covers line 304: code.append('S'); break; for 'Z'
            assertEquals("SP", metaphone.metaphone("zip"));
        }

    public void testCoverLine309_MaxCodeLenTruncation() {
            // Covers line 309: code.setLength(this.getMaxCodeLen());
            metaphone.setMaxCodeLen(3);
            assertEquals("ENS", metaphone.metaphone("encyclopedia"));
        }
}
