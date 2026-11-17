package org.apache.commons.csv;

import java.io.Reader;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtendedBufferedReader_CandorTest {

    private ExtendedBufferedReader getBufferedReader(String s) {
        return new ExtendedBufferedReader(new StringReader(s));
    }

    @Test
    public void readSingleNormalCharacter() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("a");
        int c = reader.read();
        assertEquals('a', c);
        assertEquals(0, reader.getLineNumber());
    }

    @Test
    public void readCarriageReturnIncrementsLineCounter() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\r");
        int c = reader.read();
        assertEquals('\r', c);
        assertEquals(1, reader.getLineNumber());
    }

    @Test
    public void readNewlineAfterCarriageReturnDoesNotIncrementLineCounter() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\r\n");
        int first = reader.read();
        assertEquals('\r', first);
        assertEquals(1, reader.getLineNumber());

        int second = reader.read();
        assertEquals('\n', second);
        // lineCounter should still be 1 because \n follows \r
        assertEquals(1, reader.getLineNumber());
    }

    @Test
    public void readStandaloneNewlineIncrementsLineCounter() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\n");
        int c = reader.read();
        assertEquals('\n', c);
        assertEquals(1, reader.getLineNumber());
    }

    @Test
    public void readEndOfStreamReturnsMinusOne() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("");
        int c = reader.read();
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, c);
        assertEquals(0, reader.getLineNumber());
    }

    @Test
    public void readMultipleLinesWithMixedNewlines() throws IOException {
        // "a\nb\rc\r\nd"
        // 'a' -> read=97, line=0
        // '\n' -> read=10, line=1
        // 'b' -> read=98, line=1
        // '\r' -> read=13, line=2
        // 'c' -> read=99, line=2
        // '\r' -> read=13, line=3
        // '\n' -> read=10, line=3 (not incremented because lastChar was '\r')
        // 'd' -> read=100, line=3
        ExtendedBufferedReader reader = getBufferedReader("a\nb\rc\r\nd");

        assertEquals('a', reader.read());
        assertEquals(0, reader.getLineNumber());

        assertEquals('\n', reader.read());
        assertEquals(1, reader.getLineNumber());

        assertEquals('b', reader.read());
        assertEquals(1, reader.getLineNumber());

        assertEquals('\r', reader.read());
        assertEquals(2, reader.getLineNumber());

        assertEquals('c', reader.read());
        assertEquals(2, reader.getLineNumber());

        assertEquals('\r', reader.read());
        assertEquals(3, reader.getLineNumber());

        assertEquals('\n', reader.read());
        assertEquals(3, reader.getLineNumber());

        assertEquals('d', reader.read());
        assertEquals(3, reader.getLineNumber());
    }

    @Test
    public void readConsecutiveCarriageReturns() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\r\r");
        assertEquals('\r', reader.read());
        assertEquals(1, reader.getLineNumber());

        assertEquals('\r', reader.read());
        assertEquals(2, reader.getLineNumber());
    }

    @Test
    public void readConsecutiveNewlinesWithoutCarriageReturn() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\n\n");
        assertEquals('\n', reader.read());
        assertEquals(1, reader.getLineNumber());

        assertEquals('\n', reader.read());
        assertEquals(2, reader.getLineNumber());
    }

    @Test
    public void readAfterEndOfStreamMaintainsLastChar() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("a");
        assertEquals('a', reader.read());
        
        int end1 = reader.read();
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, end1);

        int end2 = reader.read();
        assertEquals(ExtendedBufferedReader.END_OF_STREAM, end2);
    }

    @Test
    public void readAfterCarriageReturnFollowedByNormalChar() throws IOException {
        ExtendedBufferedReader reader = getBufferedReader("\rx");
        assertEquals('\r', reader.read());
        assertEquals(1, reader.getLineNumber());

        assertEquals('x', reader.read());
        assertEquals(1, reader.getLineNumber());
    }

    private ExtendedBufferedReader getBufferedReader2(String s) {
            return new ExtendedBufferedReader(new java.io.StringReader(s));
        }

    

    private ExtendedBufferedReader getBufferedReader3(String s) {
            return new ExtendedBufferedReader(new StringReader(s));
        }

    @Test
        public void testReadReturnsLastCharField() throws IOException {
            // Since lastChar is assigned current and then returned, this test verifies
            // contract behavior via getBufferedReader / read interactions or subclasses, 
            // but given the method assigns lastChar = current and returns lastChar, 
            // a mutation returning current instead of lastChar can be detected if lastChar 
            // is independently expected to be identical, or we can test `readAgain()` 
            // if available, or check that `read()` returns the stored `lastChar`.
            // Specifically, ExtendedBufferedReader has a `readAgain()` method mentioned in signatures.
            // Let's test that `readAgain()` returns the exact value that `read()` just returned.
            ExtendedBufferedReader reader = getBufferedReader("ab");
            int c = reader.read();
            assertEquals('a', c);
            // readAgain() returns lastChar. If the mutant returns 'current' (which happens to be 'a' here),
            // let's check a scenario where lastChar and current could theoretically diverge or 
            // verify readAgain() directly.
            assertEquals('a', reader.readAgain());
        }

    @Test
        public void testReadAgainReflectsLastCharAfterReading() throws IOException {
            ExtendedBufferedReader reader = getBufferedReader("xyz");
            int first = reader.read();
            assertEquals('x', first);
            assertEquals('x', reader.readAgain());
    
            int second = reader.read();
            assertEquals('y', second);
            assertEquals('y', reader.readAgain());
        }
}
