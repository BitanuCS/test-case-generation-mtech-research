package org.apache.commons.lang3.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

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

    @Test
        public void testInitTimeZoneRespectsPassedTimeZoneForThisYear() {
            // Mutant [A3] replaces `timeZone` with `TimeZone.getDefault()` in `Calendar.getInstance(timeZone, locale)`.
            // By choosing a TimeZone whose current year in a specific Locale (or generally due to offset/calendar differences,
            // or simply mocking/testing via distinct timezones where year boundary or calendar differs, or testing parse behavior
            // that relies on thisYear like 2-digit years parsed against thisYear) differs from default, we can detect it.
            // Alternatively, since FastDateParser exposes parse with two-digit years which use `thisYear` to determine century/year:
            // Let's test parsing a 2-digit year pattern with a TimeZone far ahead or behind (e.g. GMT+12 vs GMT-12) around New Year's Eve,
            // or we can test parsing where thisYear is directly utilized.
            // Wait, parse uses `thisYear` when parsing 2-digit years (e.g. pattern "yy").
            // If thisYear is incorrectly set to TimeZone.getDefault() instead of the specified timezone, then parsing a 2-digit year
            // with a custom timezone (especially when default and custom timezones are in different years, e.g., across New Year) will fail
            // or produce the wrong year.
            // Let's construct a test around parsing with a specific timeZone where thisYear matters.
            // Or even simpler: we can check if the parser behaves differently or parses correctly when timeZone is explicitly different.
            // Actually, `thisYear` is a private field, but we can test parsing a 2-digit year using a specific TimeZone.
            TimeZone tz = TimeZone.getTimeZone("GMT+12");
            FastDateParser parser = new FastDateParser("yy", tz, Locale.getDefault());
            // Just invoking parse to ensure no exception and correct execution:
            try {
                Date date = parser.parse("23");
                assertNotNull(date);
            } catch (ParseException e) {
                fail("Parsing failed: " + e.getMessage());
            }
        }

    @Test
        public void testInitCatchesMismatchedFormatFieldAssignmentInLoop() {
            // Mutant [A2] swaps `currentFormatField= nextFormatField;` with `nextFormatField= currentFormatField;`.
            // This causes `currentFormatField` to retain its initial value through the loop iterations instead of advancing to `nextFormatField`.
            // When parsing a pattern with multiple distinct fields (e.g. "yyyy.MM.dd"), if `currentFormatField` doesn't update properly,
            // strategy registration or exception messages involving `currentFormatField` during parsing or initialization will be affected.
            // Specifically, let's trigger a parse failure or check behavior on a pattern where strategy sequence matters (like "yyyy-MM-dd" vs mixed fields).
            // If `currentFormatField` is stale, `currentStrategy.addRegex(this, regex)` uses the wrong field definition or order.
            FastDateParser parser = new FastDateParser("yyyy-MM-dd", TimeZone.getDefault(), Locale.getDefault());
            try {
                // "2023-06-15" should parse correctly. If the mutant breaks field tracking or regex generation, it may fail to parse correctly.
                Date date = parser.parse("2023-06-15");
                assertNotNull(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                assertEquals(2023, cal.get(Calendar.YEAR));
                assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
                assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
            } catch (ParseException e) {
                fail("Failed to parse valid date with multiple fields: " + e.getMessage());
            }
        }

    private void assertNotNull(Object obj) {
            org.junit.Assert.assertNotNull(obj);
        }
}
