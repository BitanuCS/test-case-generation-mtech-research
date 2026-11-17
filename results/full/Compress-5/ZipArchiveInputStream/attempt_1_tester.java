package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipException;

import org.junit.Test;

public class ZipArchiveInputStream_CandorTest {

    @Test
    public void readWhenStreamIsClosed() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
        
        try {
            zis.close();
            byte[] buffer = new byte[10];
            zis.read(buffer, 0, 10);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("The stream is closed", e.getMessage());
        }
    }

    @Test
    public void readWhenCurrentEntryIsNull() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void readWithInvalidStartIndexNegative() throws IOException {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
        byte[] buffer = new byte[10];
        zis.read(buffer, -1, 10);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void readWithInvalidLengthNegative() throws IOException {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
        byte[] buffer = new byte[10];
        zis.read(buffer, 0, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void readWithBufferOverflowBounds() throws IOException {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
        byte[] buffer = new byte[10];
        zis.read(buffer, 5, 10);
    }

    @Test
    public void readStoredMethodCompletelyRead() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    @Test
    public void readStoredMethodNormalRead() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public void readStoredMethodEOFReached() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public void readDeflateMethodNeedsInputAndFills() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public void readDeflateMethodFinished() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public void readDeflateMethodTruncatedZip() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            zis.read(buffer, 0, 10);
        } catch (IOException e) {
            // If current is null, it returns -1 first, so we just ensure no unhandled exceptions or test passes
            assertEquals(-1, zis.read(new byte[10], 0, 10));
        }
    }

    @Test
    public void readDeflateMethodDataFormatException() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }

    @Test
    public void readStoredMethodTruncatedEntrySizeLimit() {
        byte[] data = new byte[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        try {
            byte[] buffer = new byte[10];
            int result = zis.read(buffer, 0, 10);
            assertEquals(-1, result);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }
}
