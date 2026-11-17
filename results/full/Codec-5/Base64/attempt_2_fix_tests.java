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
}
