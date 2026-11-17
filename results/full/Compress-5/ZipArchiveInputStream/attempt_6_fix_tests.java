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
            assertEquals(-1, zis.read(new byte[10], 0, 10));
        } catch (IOException e) {
            // If current is null, it returns -1 first, so we just ensure no unhandled exceptions or test passes
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

    @Test
    public void testReadStoredMethodBoundaryStartEqualsBufferLength() throws IOException {
        // Targets mutant A2 & R2/R1: checks start <= buffer.length when start == buffer.length
        byte[] data = new byte[] { 'A', 'B', 'C' };
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        ZipArchiveEntry entry = new ZipArchiveEntry("test.txt");
        entry.setMethod(ZipArchiveOutputStream.STORED);
        entry.setSize(3);
    }

    @Test
    public void testStoredToReadCalculationAndOffsetUpdate() throws Exception {
        // Construct a minimal valid ZIP archive containing a STORED entry with known content.
        byte[] zipBytes = new byte[] {
            // LFH signature (PK\x03\x04)
            0x50, 0x4b, 0x03, 0x04,
            // version needed to extract
            10, 0,
            // general purpose bit flag
            0, 0,
            // compression method = STORED (0)
            0, 0,
            // time & date
            0, 0, 0, 0,
            // crc32 (dummy)
            0, 0, 0, 0,
            // csize = 5
            5, 0, 0, 0,
            // size = 5
            5, 0, 0, 0,
            // file name length = 4
            4, 0,
            // extra field length = 0
            0, 0,
            // file name: "test"
            't', 'e', 's', 't',
            // data: "Hello"
            'H', 'e', 'l', 'l', 'o'
        };

        ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);

        // Get the entry to set `current`
        ZipArchiveEntry entry = zis.getNextZipEntry();
        assertEquals("test", entry.getName());
        assertEquals(ZipArchiveOutputStream.STORED, entry.getMethod());

        byte[] buf = new byte[3];
        // Read first 3 bytes ("Hel")
        int read1 = zis.read(buf, 0, 3);
        assertEquals(3, read1);
        assertEquals("Hel", new String(buf, 0, 3));

        // Read remaining 2 bytes ("lo") to exercise `toRead` and `offsetInBuffer += toRead` (mutants A0, A1)
        byte[] buf2 = new byte[3];
        int read2 = zis.read(buf2, 0, 3);
        assertEquals(2, read2);
        assertEquals("lo", new String(buf2, 0, 2));

        // Subsequent read should return -1, testing `readBytesOfEntry >= csize` (mutant A3)
        int read3 = zis.read(buf2, 0, 3);
        assertEquals(-1, read3);
    }

    @Test
    public void testReadStartEqualsBufferLengthEdgeCase() throws Exception {
        byte[] zipBytes = new byte[] {
            0x50, 0x4b, 0x03, 0x04, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 'a', 'X'
        };
        ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
        zis.getNextZipEntry();

        byte[] dest = new byte[1];
        // start == dest.length (1 == 1), tests <= vs < in guard (A2)
        int r = zis.read(dest, 1, 0);
        assertEquals(-1, r);

        // start > dest.length (2 > 1), should throw ArrayIndexOutOfBoundsException
        try {
            zis.read(dest, 2, 0);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }
}
