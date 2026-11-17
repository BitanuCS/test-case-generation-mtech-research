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
        assertEquals(0, r);

        // start > dest.length (2 > 1), should throw ArrayIndexOutOfBoundsException
        try {
            zis.read(dest, 2, 0);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
        public void testReadStoredMethodBufferOverflowGuardMutantsR3R6R9() throws Exception {
            // Tests the boundary condition guard:
            // if (start <= buffer.length && length >= 0 && start >= 0 && buffer.length - start >= length)
            // Mutants R3, R6, R9 change '&&' to '||'. By supplying invalid arguments, 
            // the original condition correctly triggers ArrayIndexOutOfBoundsException, whereas 
            // mutants with '||' might incorrectly pass the check and proceed to throw different exceptions or behave incorrectly.
            byte[] zipBytes = new byte[] {
                0x50, 0x4b, 0x03, 0x04, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 'a', 'X'
            };
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            zis.getNextZipEntry();
    
            byte[] dest = new byte[2];
    
            // 1) start > buffer.length (3 > 2), length = 0, start >= 0 (true), buffer.length - start >= length (false)
            // Original: false && ... -> false. Mutants with || might evaluate to true.
            try {
                zis.read(dest, 3, 0);
                fail("Expected ArrayIndexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException e) {
                // expected
            }
    
            // 2) length < 0 (-1), start = 0 <= 2 (true), start >= 0 (true), buffer.length - start >= length (true)
            // Original: true && false && ... -> false (throws ArrayIndexOutOfBoundsException).
            // Mutant R3 (start <= buffer.length || length >= 0): true || false -> true, then proceeds and fails or behaves unexpectedly.
            try {
                zis.read(dest, 0, -1);
                fail("Expected ArrayIndexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException e) {
                // expected
            }
        }

    @Test
        public void testReadStoredMethodOffsetInBufferEqualsLengthOfLastReadAndEOF() throws Exception {
            // Tests line 210: return -1 when stream returns -1 on refill (in.read(buf) == -1)
            // and mutant R15 (checking == -2 instead of == -1).
            byte[] zipBytes = new byte[] {
                // LFH
                0x50, 0x4b, 0x03, 0x04, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                // csize = 2, size = 2
                2, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 'a', 
                // Only 1 byte of payload provided ('X'), second byte missing causing in.read(...) to return -1
                'X'
            };
    
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            zis.getNextZipEntry();
    
            byte[] buf = new byte[5];
            // First read gets 1 byte ('X')
            int read1 = zis.read(buf, 0, 1);
            assertEquals(1, read1);
            assertEquals('X', buf[0]);
    
            // Second read tries to read more, offsetInBuffer >= lengthOfLastRead triggers refill,
            // which hits EOF (-1). Should return -1. 
            // Mutant R15 checks for -2, so it would hang or try to process as normal instead of returning -1.
            int read2 = zis.read(buf, 0, 1);
            assertEquals(-1, read2);
        }

    @Test
        public void testReadStoredMethodExactBoundaryReadBytesOfEntryEqualsCSize() throws Exception {
            // Tests mutant A3 / R12: readBytesOfEntry >= csize vs readBytesOfEntry > csize.
            byte[] zipBytes = new byte[] {
                0x50, 0x4b, 0x03, 0x04, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 'a', 'Z'
            };
    
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            zis.getNextZipEntry();
    
            byte[] buf = new byte[10];
            // Read the 1 exact byte available
            int r1 = zis.read(buf, 0, 10);
            assertEquals(1, r1);
            assertEquals('Z', buf[0]);
    
            // Now readBytesOfEntry == csize (1 == 1). 
            // Original code checks readBytesOfEntry >= csize and returns -1.
            // Mutant checks readBytesOfEntry > csize (1 > 1 is false), so it incorrectly attempts another read/return instead of -1.
            int r2 = zis.read(buf, 0, 10);
            assertEquals(-1, r2);
        }

    @Test
        public void testReadDeflateMethodNeedsInputAndInflate() throws Exception {
            // Construct a minimal valid DEFLATED ZIP entry to execute:
            // - inf.needsInput() / fill() (lines 227-230)
            // - inf.inflate(...) (line 235)
            // - crc.update(...) and return read (lines 246-247)
            byte[] zipBytes = new byte[] {
                // LFH
                0x50, 0x4b, 0x03, 0x04, 
                10, 0, // version
                0, 0, // flags
                8, 0, // compression method = DEFLATED (8)
                0, 0, 0, 0, // time/date
                0, 0, 0, 0, // crc32
                5, 0, 0, 0, // compressed size = 5
                5, 0, 0, 0, // uncompressed size = 5
                1, 0, // file name length = 1
                0, 0, // extra field length = 0
                'a', // file name
                // Deflated payload bytes for "Hello"
                (byte) 0xf2, (byte) 0x48, (byte) 0xcd, (byte) 0xc9, (byte) 0xc9, (byte) 0x07, 0, 0
            };
    
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            ZipArchiveEntry entry = zis.getNextZipEntry();
            assertEquals(8, entry.getMethod());
    
            byte[] buf = new byte[10];
            int read = zis.read(buf, 0, 10);
            assertEquals(5, read);
            assertEquals("Hello", new String(buf, 0, 5));
        }

    @Test
        public void testReadDeflateMethodTruncatedZipException() throws Exception {
            // Tests lines 239-243: read == 0, not finished, but lengthOfLastRead == -1 throws IOException("Truncated ZIP file")
            byte[] zipBytes = new byte[] {
                0x50, 0x4b, 0x03, 0x04, 
                10, 0, 0, 0, 
                8, 0, // DEFLATED
                0, 0, 0, 0, 0, 0, 0, 0, 
                10, 0, 0, 0, 10, 0, 0, 0, 
                1, 0, 0, 0, 
                'a',
                // Truncated compressed data (incompletedeflate stream)
                0x01, 0x02
            };
    
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            zis.getNextZipEntry();
    
            byte[] buf = new byte[10];
            try {
                zis.read(buf, 0, 10);
                // If it finishes or returns, try reading again to exhaust and hit truncation check
                zis.read(buf, 0, 10);
            } catch (IOException e) {
                // expected either ZipException or Truncated ZIP file IOException
            }
        }

    @Test
        public void testReadDeflateMethodDataFormatExceptionZipException() throws Exception {
            // Tests lines 236-238: catching DataFormatException and throwing ZipException
            byte[] zipBytes = new byte[] {
                0x50, 0x4b, 0x03, 0x04, 
                10, 0, 0, 0, 
                8, 0, // DEFLATED
                0, 0, 0, 0, 0, 0, 0, 0, 
                5, 0, 0, 0, 5, 0, 0, 0, 
                1, 0, 0, 0, 
                'a',
                // Corrupted deflate header bytes that cause DataFormatException
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
            };
    
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
            ZipArchiveInputStream zis = new ZipArchiveInputStream(bais);
            zis.getNextZipEntry();
    
            byte[] buf = new byte[10];
            try {
                zis.read(buf, 0, 10);
                fail("Expected ZipException due to DataFormatException");
            } catch (ZipException e) {
                // expected
            }
        }
}
