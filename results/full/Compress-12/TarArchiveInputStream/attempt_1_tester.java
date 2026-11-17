package org.apache.commons.compress.archivers.tar;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarArchiveInputStream_CandorTest {

    @Test
    public void alreadyAtEOFReturnsNull() throws IOException {
        // Empty stream leads immediately to EOF
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        TarArchiveInputStream tais = new TarArchiveInputStream(emptyStream);
        
        TarArchiveEntry entry = tais.getNextTarEntry();
        assertNull(entry);
        ais.close();
    }

    @Test
    public void returnsNullWhenEndOftarArchiveReachedDuringRecordRead() throws IOException {
        // Stream with insufficient bytes for a full tar header record should hit EOF and return null
        InputStream shortStream = new ByteArrayInputStream(new byte[10]);
        TarArchiveInputStream tais = new TarArchiveInputStream(shortStream);
        
        TarArchiveEntry entry = tais.getNextTarEntry();
        assertNull(entry);
        ais.close();
    }

    @Test(expected = IOException.class)
    public void throwsIOExceptionOnInvalidHeaderBytes() throws IOException {
        // 512 bytes of invalid/random data that fail the TarArchiveEntry header parsing
        byte[] invalidHeader = new byte[512];
        for (int i = 0; i < 512; i++) {
            invalidHeader[i] = (byte) 0xFF;
        }
        InputStream stream = new ByteArrayInputStream(invalidHeader);
        TarArchiveInputStream tais = new TarArchiveInputStream(stream);
        
        try {
            tais.getNextTarEntry();
        } finally {
            tais.close();
        }
    }

}
