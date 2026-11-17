package org.apache.commons.lang3.text.translate;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

/**
 * Unit tests for {@link org.apache.commons.lang3.text.translate.LookupTranslator}.
 */
public class LookupTranslator_CandorTest {

    @Test
    public void translateGreedyMatch() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"},
                {"caterpillar", "bug"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("caterpillar", 0, out);
        assertEquals(11, result);
        assertEquals("bug", out.toString());
    }

    @Test
    public void translateShorterMatchWhenLongestExceedsInputLength() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"},
                {"caterpillar", "bug"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("cats", 0, out);
        assertEquals(3, result);
        assertEquals("dog", out.toString());
    }

    @Test
    public void translateNoMatchReturnsZero() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("bird", 0, out);
        assertEquals(0, result);
        assertEquals("", out.toString());
    }

    @Test
    public void translatePartialMatchWithinString() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("the cat sat", 4, out);
        assertEquals(3, result);
        assertEquals("dog", out.toString());
    }

    @Test
    public void translateNullLookupTable() throws IOException {
        final LookupTranslator translator = new LookupTranslator((CharSequence[][]) null);
        final StringWriter out = new StringWriter();
        final int result = translator.translate("test", 0, out);
        assertEquals(0, result);
        assertEquals("", out.toString());
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void translateIndexBeyondInputLength() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"}
            }
        );
        final StringWriter out = new StringWriter();
        translator.translate("cat", 5, out);
    }

    @Test
    public void translateEmptyInputString() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"cat", "dog"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("", 0, out);
        assertEquals(0, result);
        assertEquals("", out.toString());
    }

    @Test
    public void translateMappingToEmptyString() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"bad", ""}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("badword", 0, out);
        assertEquals(3, result);
        assertEquals("", out.toString());
    }

    @Test
    public void translateOverlappingKeysPrefersLongest() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"a", "1"},
                {"ab", "2"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("abc", 0, out);
        assertEquals(2, result);
        assertEquals("2", out.toString());
    }

    @Test
    public void translateExactMatchBoundary() throws IOException {
        final LookupTranslator translator = new LookupTranslator(
            new CharSequence[][] {
                {"exact", "match"}
            }
        );
        final StringWriter out = new StringWriter();
        final int result = translator.translate("exact", 0, out);
        assertEquals(5, result);
        assertEquals("match", out.toString());
    }
}
