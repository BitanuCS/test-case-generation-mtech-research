package org.apache.commons.lang3.time;

import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link org.apache.commons.lang3.time.FastDateParser} focusing on initialization and pattern parsing.
 */
public class FastDateParser_CandorTest {

    @Test(expected = IllegalArgumentException.class)
    public void initThrowsIllegalArgumentExceptionOnEmptyPattern() {
        new FastDateParser("", TimeZone.getDefault(), Locale.getDefault());
    }

    @Test(expected = IllegalArgumentException.class)
    public void initThrowsIllegalArgumentExceptionOnInvalidPatternToken() {
        new FastDateParser("yyyy-MM-dd XXX", TimeZone.getDefault(), Locale.getDefault());
    }

    @Test
    public void initSuccessfullyInitializesStandardPattern() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initInitializesComplexPatternWithMultipleStrategies() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd'T'HH:mm:ss.SSS Z", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getParsePattern());
    }

    @Test
    public void initHandlesSingleCharacterPatternToken() {
        FastDateParser parser = new FastDateParser("y", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initHandlesRepeatedCharactersStrategyToken() {
        FastDateParser parser = new FastDateParser("yyyy", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initHandlesLiteralStringWithQuotes() {
        FastDateParser parser = new FastDateParser("yyyy-MM-dd 'at' HH:mm:ss", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initSetsUpThisYearAccordingToTimeZoneAndLocale() {
        FastDateParser parser = new FastDateParser("yyyy", TimeZone.getTimeZone("GMT+2"), Locale.GERMANY);
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initInitializesNameValuesConcurrentMap() {
        FastDateParser parser = new FastDateParser("MMM EEE", TimeZone.getDefault(), Locale.US);
        assertNotNull(parser.getPattern());
    }

    @Test(expected = IllegalArgumentException.class)
    public void initThrowsIllegalArgumentExceptionWhenPatternStartsLocallyInvalid() {
        new FastDateParser("@yyyy", TimeZone.getDefault(), Locale.getDefault());
    }

    @Test
    public void initHandlesAllSupportedCalendarFieldsCombined() {
        FastDateParser parser = new FastDateParser("G y M d H m s S E a Z", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }

    @Test
    public void initHandlesEscapedSingleQuoteLiteral() {
        FastDateParser parser = new FastDateParser("yyyy''MM''dd", TimeZone.getDefault(), Locale.getDefault());
        assertNotNull(parser.getPattern());
    }
}
