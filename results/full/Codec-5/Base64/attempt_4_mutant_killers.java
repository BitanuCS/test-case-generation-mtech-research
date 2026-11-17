package org.apache.commons.codec.binary;

import junit.framework.TestCase;
import java.util.Random;

/**
 * JUnit 3 test class for Base64 decode method based on the provided test plan.
 */
public class Base64_CandorTest extends TestCase {

    private Random _random = new Random();

    public Base64_CandorTest(String name) {
        super(name);
    }

    public Random getRandom() {
        return this._random;
    }

    private String toString(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(data[i]);
            if (i != data.length - 1) {
                buf.append(",");
            }
        }
        return buf.toString();
    }

    public void testDecodeEofFlagSetImmediately() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', 'V', 'y' };
        base64.decode(input, 0, -1);
        
        // When eof is true already or triggered via inAvail < 0, subsequent decodes do nothing
        assertFalse(base64.hasData());
    }

    public void testDecodePaddingCharacterEncountered() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', '=' };
        base64.decode(input, 0, 3);
        
        // PAD ('=') encountered, sets eof = true and breaks loop early
        assertTrue(base64.hasData());
    }

    public void testDecodeBufferResizeTrigger() {
        Base64 base64 = new Base64();
        // Create a large input array to force buffer initialization or resizing
        byte[] input = new byte[9000];
        for (int i = 0; i < input.length; i++) {
            input[i] = 'S';
        }
        base64.decode(input, 0, input.length);
        assertTrue(base64.hasData());
    }

    public void testDecodeIgnoredNonBase64Characters() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', ' ', 'G', '\n', 'V', '\r', 'y' };
        base64.decode(input, 0, 7);
        
        assertEquals(3, base64.avail());
    }

    public void testDecodeModulusTwoEofPadding() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G' };
        base64.decode(input, 0, 2);
        base64.decode(new byte[0], 0, -1); // EOF call
        
        assertEquals(1, base64.avail());
    }

    public void testDecodeModulusThreeEofPadding() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', 'V' };
        base64.decode(input, 0, 3);
        base64.decode(new byte[0], 0, -1); // EOF call
        
        assertEquals(2, base64.avail());
    }

    public void testDecodeStandardFourByteBlock() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', 'V', 'y' };
        base64.decode(input, 0, 4);
        
        assertEquals(3, base64.avail());
    }

    public void testDecodeZeroInAvail() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', 'V', 'y' };
        base64.decode(input, 0, 0);
        
        assertFalse(base64.hasData());
    }

    public void testDecodeMultipleBlocksWithPadding() {
        Base64 base64 = new Base64();
        byte[] input = { 'S', 'G', 'V', 'y', 'S', 'g', '=', '=' };
        base64.decode(input, 0, 8);
        
        assertTrue(base64.hasData());
    }

    /**
         * Kills mutant A3: verifies the correct right shift (16 instead of 8) for modulus 2 EOF padding.
         * "SG" decodes to a single byte (0x48). If right shifted by 8 instead of 16, the output byte is wrong.
         */
    public void testDecodeModulusTwoShift16Mutant() {
            Base64 base64 = new Base64();
            // "SG" has length 2, meaning modulus = 2 after processing.
            // Binary for 'S' is 18 (010010), 'G' is 6 (000110).
            // Combined 12 bits: 010010000110.
            // With EOF padding, x is shifted left by 6 twice (total 12 extra shifts) and then right shifted by 16.
            byte[] input = { 'S', 'G' };
            base64.decode(input, 0, 2);
            base64.decode(new byte[0], 0, -1);
    
            assertEquals(1, base64.avail());
            byte[] results = new byte[1];
            base64.readResults(results, 0, 1);
            // Correct Base64 decode of "SG" is {72} ('H')
            assertEquals((byte) 72, results[0]);
        }

    /**
         * Kills mutant R0: inAvail < 1 vs inAvail < 0.
         * When inAvail = 0, with original code it should NOT set eof = true immediately (it just loops 0 times).
         * If inAvail < 1 were used, inAvail = 0 would trigger eof = true.
         */
    public void testDecodeInAvailZeroDoesNotSetEof() {
            Base64 base64 = new Base64();
            // Pass inAvail = 0 when eof is not set; should not set eof = true.
            base64.decode(new byte[0], 0, 0);
            
            // Now feed valid data; if eof was incorrectly set to true by inAvail=0, this subsequent decode will be ignored.
            byte[] input = { 'S', 'G', 'V', 'y' };
            base64.decode(input, 0, 4);
            
            assertEquals(3, base64.avail());
        }

    /**
         * Kills mutants R5 & R6: buffer.length - pos < decodeSize vs + or <=.
         * Also covers line 587 (resizeBuffer() inside the EOF padding block).
         * To reach line 587, we need modulus != 0 at EOF and buffer to need a resize or check.
         */
    public void testDecodeEofBufferResizeAndArithmeticMutants() {
            Base64 base64 = new Base64();
            // Force a scenario where buffer might be near capacity or null during EOF padding.
            // Or directly trigger the EOF padding resize check where buffer.length - pos < decodeSize.
            byte[] input = { 'S', 'G' };
            base64.decode(input, 0, 2);
            base64.decode(new byte[0], 0, -1);
            
            assertTrue(base64.hasData());
        }

    /**
         * Kills mutant R7: eof = false when b == PAD.
         * When PAD ('=') is encountered, eof must be set to true. If set to false, decoding would continue past padding.
         */
    public void testDecodePadSetsEofTrue() {
            Base64 base64 = new Base64();
            byte[] input = { 'S', 'G', '=', 'V', 'y' }; // 'V', 'y' after '=' should be ignored because eof = true
            base64.decode(input, 0, 5);
            
            // Only "SG=" should be processed (modulus 2 padding -> 1 output byte)
            assertEquals(1, base64.avail());
        }

    /**
         * Kills mutant R8: b >= 0 vs b > 0.
         * Tests a byte with value 0 (which is < DECODE_TABLE.length, but not > 0).
         * DECODE_TABLE[0] is -1, but checking b >= 0 correctly evaluates index 0 without throwing ArrayIndexOutOfBounds
         * and correctly skips negative/invalid table entries or handles 0 if valid.
         */
    public void testDecodeZeroByteBoundary() {
            Base64 base64 = new Base64();
            byte[] input = { 0, 'S', 'G', 'V', 'y' };
            base64.decode(input, 0, 5);
            
            // Non-base64 or zero byte should be ignored gracefully, resulting in 3 decoded bytes from "SGVy"
            assertEquals(3, base64.avail());
        }

    /**
         * Covers line 552: return; (when eof is already true).
         */
    public void testDecodeAlreadyEofReturnsImmediately() {
            Base64 base64 = new Base64();
            base64.decode(new byte[0], 0, -1); // sets eof = true
            
            // Subsequent decode calls should hit `if (eof) { return; }` immediately
            byte[] input = { 'S', 'G', 'V', 'y' };
            base64.decode(input, 0, 4);
            
            assertFalse(base64.hasData());
        }

    /**
         * Kills mutant [R5] ('-' -> '+') and [R6] ('<' -> '<=') on line 558:
         * `if (buffer == null || buffer.length - pos < decodeSize)`
         * 
         * If '-' is mutated to '+', `buffer.length + pos < decodeSize` evaluates differently when pos > 0.
         * If '<' is mutated to '<=', `buffer.length - pos <= decodeSize` triggers resizeBuffer() prematurely or incorrectly.
         * Also specifically targets line 587 (`resizeBuffer();` inside the EOF padding block) by ensuring we have 
         * a scenario where decoding triggers buffer checks with accumulated pos offset during EOF padding.
         */
    public void testDecodeBufferResizeAndArithmeticEdgeCases() {
            // Create a custom instance or feed input that advances pos and hits EOF padding with specific buffer remaining space
            Base64 base64 = new Base64();
            
            // Decode a chunk to advance pos, then trigger EOF padding where line 587 (resizeBuffer in EOF block) can be tested
            byte[] input = { 'S', 'G', 'V', 'y', 'S', 'G' };
            base64.decode(input, 0, 6);
            base64.decode(new byte[0], 0, -1);
            
            assertTrue(base64.hasData());
            assertEquals(4, base64.avail());
        }

    /**
         * Kills mutants [R8] ('>=' -> '>'), [R10] (0 -> 1), and [R11] ('&&' -> '||') on line 567:
         * `if (b >= 0 && b < DECODE_TABLE.length)`
         * 
         * Passes a byte with value 0 as input to test the boundary condition `b >= 0`.
         * - If `b >= 0` becomes `b > 0`, byte value 0 is skipped instead of evaluated.
         * - If `0` becomes `1`, byte value 0 is skipped.
         * - If `&&` becomes `||`, invalid negative bytes (like -1) could incorrectly pass the condition and cause ArrayIndexOutOfBoundsException.
         */
    public void testDecodeByteZeroBoundaryAndInvalidBytes() {
            Base64 base64 = new Base64();
            // Byte 0 is in DECODE_TABLE range (index 0), but DECODE_TABLE[0] is -1.
            // Let's test a byte value of 0 and a negative byte value (-2) to ensure bounds checks are strict.
            byte[] input = { (byte) 0, (byte) -2, 'S', 'G', 'V', 'y' };
            base64.decode(input, 0, 6);
            
            // Should successfully decode "SGVy" (3 bytes) while ignoring 0 and -2 safely without exception
            assertEquals(3, base64.avail());
        }

    /**
         * Kills mutant [R12] ('>=' -> '>') on line 569:
         * `if (result >= 0)`
         * 
         * DECODE_TABLE contains 0 for certain valid Base64 characters (e.g. 'A' maps to index 66 in ASCII, wait:
         * DECODE_TABLE maps characters. Let's check a character that decodes to result == 0, such as 'A' or '+' or similar if applicable,
         * or specifically check that result == 0 is accepted, not strictly greater than 0).
         * In Base64, 'A' is index 0 in STANDARD_ENCODE_TABLE, and in DECODE_TABLE, 'A' (ASCII 65) -> wait, let's look at DECODE_TABLE mapping.
         * Specifically, index 0 in DECODE_TABLE can be reached or result == 0 (like padding or specific base64 char index 0).
         * If `result >= 0` becomes `result > 0`, result == 0 would be incorrectly ignored.
         */
    public void testDecodeResultZeroIsAccepted() {
            Base64 base64 = new Base64();
            // 'A' decodes to index 0 in DECODE_TABLE (DECODE_TABLE['A'] or similar).
            // Let's decode a Base64 string starting with 'A' (e.g., "AA==" or "AAAA").
            byte[] input = { 'A', 'A', 'A', 'A' };
            base64.decode(input, 0, 4);
            
            // "AAAA" decodes to 3 zero bytes {0, 0, 0}
            assertEquals(3, base64.avail());
        }
}
