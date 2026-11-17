package org.apache.commons.compress.archivers.tar;

import java.io.ByteArrayOutputStream;

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
        tais.close();
    }

    @Test
    public void returnsNullWhenEndOftarArchiveReachedDuringRecordRead() throws IOException {
        // Stream with insufficient bytes for a full tar header record should hit EOF and return null
        InputStream shortStream = new ByteArrayInputStream(new byte[10]);
        TarArchiveInputStream tais = new TarArchiveInputStream(shortStream);
        
        TarArchiveEntry entry = tais.getNextTarEntry();
        assertNull(entry);
        tais.close();
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


    @Test
        public void testMultipleEntriesSkipsUnreadBytesOfPreviousEntry() throws IOException {
            // Create a tar archive with two valid entries.
            // We read only the first entry partially (or call getNextTarEntry twice directly)
            // to exercise the code path where currEntry != null and it skips remaining bytes.
            byte[] tarBytes = createSimpleTarArchiveWithTwoEntries();
            TarArchiveInputStream tais = new TarArchiveInputStream(new ByteArrayInputStream(tarBytes));
    
            // Get first entry
            TarArchiveEntry firstEntry = tais.getNextTarEntry();
            assertNotNull(firstEntry);
            assertEquals("entry1.txt", firstEntry.getName());
    
            // Calling getNextTarEntry again should skip the remaining content of entry1 and return entry2.
            // This exercises:
            // - currEntry != null check (R0 mutant)
            // - numToSkip = entrySize - entryOffset calculation (R1 mutant, A1 mutant)
            // - while (numToSkip > 0) loop (A0 mutant)
            TarArchiveEntry secondEntry = tais.getNextTarEntry();
            assertNotNull(secondEntry);
            assertEquals("entry2.txt", secondEntry.getName());
    
            tais.close();
        }

    @Test
        public void testGNULongNameEntryWithSingleCharacterNullTerminatedName() throws IOException {
            // Construct a tar archive containing a GNU long name entry where the long name
            // has length 1 and ends with a null terminator byte (0).
            // This exercises:
            // - if (longName.length() > 0 && ...) vs if (longName.length() > 1 && ...) (A2 mutant)
            byte[] tarBytes = createGNULongNameTarArchive((byte) 0);
            TarArchiveInputStream tais = new TarArchiveInputStream(new ByteArrayInputStream(tarBytes));
    
            TarArchiveEntry entry = tais.getNextTarEntry();
            assertNotNull(entry);
            // Since length > 0 is required to strip the null, length 1 with '\0' becomes empty string.
            // If A2 mutant survives (longName.length() > 1), it won't strip the null, leaving the '\0' char.
            assertEquals("", entry.getName());
    
            tais.close();
        }

    @Test
        public void testPaxHeaderEntryProcessing() throws IOException {
            // Construct a tar archive containing a Pax header entry.
            // This exercises the isPaxHeader branch and paxHeaders() execution,
            // killing the negated check mutant (A3 mutant: !currEntry.isPaxHeader()).
            byte[] tarBytes = createPaxHeaderTarArchive();
            TarArchiveInputStream tais = new TarArchiveInputStream(new ByteArrayInputStream(tarBytes));
    
            TarArchiveEntry entry = tais.getNextTarEntry();
            assertNotNull(entry);
            assertEquals("a", entry.getName());
    
            tais.close();
        }

    private byte[] createSimpleTarArchiveWithTwoEntries() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // 2 entries, each needs a 512-byte header + data padded to 512 blocks.
            // For simplicity, let's make them 0-byte size entries or small content.
            out.write(createTarHeader("entry1.txt", 10));
            out.write(new byte[512]); // data block for entry1 (10 bytes padded)
            out.write(createTarHeader("entry2.txt", 0));
            out.write(new byte[512]); // EOF / end of archive padding (two 512-byte zero blocks usually, let's add enough)
            out.write(new byte[512]);
            return out.toByteArray();
        }

    private byte[] createGNULongNameTarArchive(byte nameContent) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // GNU long name header has magic/typeflag indicating long name (e.g. L or similar, but TarArchiveEntry recognizes isGNULongNameEntry())
            // In Commons Compress TarArchiveEntry, a GNU long name entry has typeflag TarArchiveEntry.LF_GNUTYPE_LONGNAME ('L')
            byte[] longNameHeader = createTarHeader("././@LongLink", 1);
            longNameHeader[156] = 'L'; // typeflag for GNUTYPE_LONGNAME
            
            out.write(longNameHeader);
            // Data for long name entry: 1 byte of name + padding to 512 bytes
            byte[] longNameData = new byte[512];
            longNameData[0] = nameContent;
            out.write(longNameData);
    
            // Actual file header
            out.write(createTarHeader("normal.txt", 0));
            out.write(new byte[512]);
            out.write(new byte[512]);
            return out.toByteArray();
        }

    private byte[] createPaxHeaderTarArchive() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Pax header typeflag is 'x' (TarArchiveEntry.LF_PAX_EXTENDED_HEADER_LC)
            byte[] paxHeader = createTarHeader("PaxHeader", 11);
            paxHeader[156] = 'x';
            
            out.write(paxHeader);
            byte[] paxData = new byte[512];
            // simple pax header content: "11 path=a\n"
            byte[] content = "11 path=a\n".getBytes("UTF-8");
            System.arraycopy(content, 0, paxData, 0, content.length);
            out.write(paxData);
    
            // Actual file entry following Pax header
            out.write(createTarHeader("paxFile.txt", 0));
            out.write(new byte[512]);
            out.write(new byte[512]);
            return out.toByteArray();
        }

    private byte[] createTarHeader(String name, long size) {
            byte[] header = new byte[512];
            // Name
            byte[] nameBytes = name.getBytes();
            System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));
            
            // Mode, UID, GID
            putString("0000755 ", header, 100, 8);
            putString("0000000 ", header, 108, 8);
            putString("0000000 ", header, 116, 8);
            
            // Size (octal)
            String sizeString = Long.toOctalString(size);
            StringBuilder sb = new StringBuilder();
            while (sb.length() + sizeString.length() < 11) {
                sb.append('0');
            }
            sb.append(sizeString).append(' ');
            putString(sb.toString(), header, 124, 12);
    
            // Modification time
            putString("00000000000 ", header, 136, 12);
            
            // Typeflag ('0' or normal file)
            header[156] = '0';
    
            // Magic "ustar\0"
            System.arraycopy(new byte[] { 'u', 's', 't', 'a', 'r', 0 }, 0, header, 257, 6);
            System.arraycopy(new byte[] { '0', '0' }, 0, header, 263, 2);
    
            // Calculate simple checksum
            int sum = 0;
            for (int i = 0; i < 512; i++) {
                sum += header[i] & 0xFF;
            }
            String cksumString = Integer.toOctalString(sum);
            StringBuilder cs = new StringBuilder();
            while (cs.length() + cksumString.length() < 6) {
                cs.append('0');
            }
            cs.append(cksumString).append("\u0000 ");
            putString(cs.toString(), header, 148, 8);
    
            return header;
        }

    private void putString(String str, byte[] buf, int offset, int length) {
            byte[] bytes = str.getBytes();
            System.arraycopy(bytes, 0, buf, offset, Math.min(bytes.length, length));
        }
}
