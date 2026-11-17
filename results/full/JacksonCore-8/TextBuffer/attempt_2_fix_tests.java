package com.fasterxml.jackson.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextBuffer_CandorTest {

    @Test
    public void sharedInputBufferActive() {
        TextBuffer textBuffer = new TextBuffer(null);
        char[] shared = new char[] { 'h', 'e', 'l', 'l', 'o' };
        textBuffer.resetWithShared(shared, 0, 5);

        char[] result = textBuffer.getTextBuffer();
        assertSame(shared, result);
    }

    @Test
    public void resultArrayAlreadyCached() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("cached");
        // Force caching of _resultArray via contentsAsArray()
        char[] firstArray = textBuffer.contentsAsArray();
        assertNotNull(firstArray);

        char[] result = textBuffer.getTextBuffer();
        assertSame(firstArray, result);
    }

    @Test
    public void resultStringCached() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("hello");

        char[] result = textBuffer.getTextBuffer();
        assertNotNull(result);
        assertArrayEquals(new char[] { 'h', 'e', 'l', 'l', 'o' }, result);
    }

    @Test
    public void singleUnsegmentedCurrentSegment() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithEmpty();
        textBuffer.append('a');
        textBuffer.append('b');

        char[] result = textBuffer.getTextBuffer();
        assertNotNull(result);
        assertSame(textBuffer.getCurrentSegment(), result);
    }

    @Test
    public void multipleSegmentsOrRequiresConsolidation() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithEmpty();
        // Append enough characters to exceed MIN_SEGMENT_LEN or force multiple segments if possible,
        // or directly simulate multiple segments by filling current and finishing.
        // Since we only use existing APIs, let's append characters up to MIN_SEGMENT_LEN + 10 to trigger segments.
        char[] large = new char[TextBuffer.MIN_SEGMENT_LEN + 10];
        textBuffer.append(large, 0, large.length);

        char[] result = textBuffer.getTextBuffer();
        assertNotNull(result);
        assertEquals(TextBuffer.MIN_SEGMENT_LEN + 10, result.length);
    }

    @Test
    public void emptyBufferFreshInstance() {
        TextBuffer textBuffer = new TextBuffer(null);
        char[] result = textBuffer.getTextBuffer();
        assertNull(result);
    }

    @Test
    public void afterResetWithEmpty() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithString("temp");
        textBuffer.resetWithEmpty();

        char[] result = textBuffer.getTextBuffer();
        assertNull(result);
    }

    @Test
    public void afterResetWithCopy() {
        TextBuffer textBuffer = new TextBuffer(null);
        char[] source = new char[] { 'c', 'o', 'p', 'y' };
        textBuffer.resetWithCopy(source, 0, 4);

        char[] result = textBuffer.getTextBuffer();
        assertNotNull(result);
        assertEquals("copy", new String(result, 0, textBuffer.size()));
    }

    @Test
    public void appendedCharactersSingleSegment() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithEmpty();
        textBuffer.append('x');

        char[] result = textBuffer.getTextBuffer();
        assertSame(textBuffer.getCurrentSegment(), result);
    }

    @Test
    public void appendedCharactersMultipleSegments() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithEmpty();
        char[] large = new char[TextBuffer.MIN_SEGMENT_LEN + 50];
        textBuffer.append(large, 0, large.length);

        char[] result = textBuffer.getTextBuffer();
        assertNotNull(result);
        assertEquals(TextBuffer.MIN_SEGMENT_LEN + 50, result.length);
    }

    @Test
    public void sharedBufferOverriddenByResetWithString() {
        TextBuffer textBuffer = new TextBuffer(null);
        char[] shared = new char[] { 's', 'h', 'a', 'r', 'e', 'd' };
        textBuffer.resetWithShared(shared, 0, 6);
        textBuffer.resetWithString("override");

        char[] result = textBuffer.getTextBuffer();
        assertArrayEquals("override".toCharArray(), result);
    }

    @Test
    public void resultArrayPrioritizedOverCurrentSegment() {
        TextBuffer textBuffer = new TextBuffer(null);
        textBuffer.resetWithEmpty();
        textBuffer.append('t');
        // Call contentsAsArray() to populate _resultArray while _currentSegment is also non-null
        char[] array1 = textBuffer.contentsAsArray();
        assertNotNull(array1);

        char[] result = textBuffer.getTextBuffer();
        assertSame(array1, result);
    }
}
