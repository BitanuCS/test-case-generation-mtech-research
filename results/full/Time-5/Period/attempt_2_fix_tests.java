package org.joda.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class Period_CandorTest {

    @Test
    public void normalizedStandardWithNullTypeUsesStandard() {
        Period period = new Period(0, 0, 0, 0, 1, 30, 0, 0); // 1 hour 30 minutes
        Period result = period.normalizedStandard(null);
        assertNotNull(result);
        assertEquals(90, result.toStandardMinutes().getMinutes());
    }

    @Test
    public void normalizedStandardOverflowsSecondsToMinutes() {
        // 90 seconds = 1 minute 30 seconds
        Period period = new Period(0, 0, 0, 0, 0, 0, 90, 0);
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(1, result.getMinutes());
        assertEquals(30, result.getSeconds());
    }

    @Test
    public void normalizedStandardOverflowsMonthsFromYearsAndMonths() {
        // 1 year 15 months -> 15 months = 1 year 3 months, plus existing 1 year = 2 years 3 months
        Period period = new Period(1, 15, 0, 0, 0, 0, 0, 0);
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(2, result.getYears());
        assertEquals(3, result.getMonths());
    }

    @Test
    public void normalizedStandardMonthAndDaysDoNotOverflow() {
        // 1 month 40 days: weeks are converted to days (40 days = 5 weeks 5 days in standard normalization),
        // so 1 month 40 days becomes 1 month 5 weeks 5 days in the standard period type.
        Period period = new Period(0, 1, 0, 40, 0, 0, 0, 0);
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(1, result.getMonths());
        assertEquals(5, result.getDays());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void normalizedStandardMissingYearsAndMonthsThrowsException() {
        // Period with non-zero months, but using a PeriodType that might omit years/months or cause failure
        // PeriodType.time() does not support years or months, but has years/months = 0 usually.
        // Wait, to trigger the exception in code: if years != 0 or months != 0, and type lacks years or months.
        Period period = new Period(0, 1, 0, 0, 0, 0, 0, 0); // 1 month
        // PeriodType.time() supports hours, minutes, seconds, millis (no years, no months)
        period.normalizedStandard(PeriodType.time());
    }

    @Test
    public void normalizedStandardWithZeroValues() {
        Period period = Period.ZERO;
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(0, result.getYears());
        assertEquals(0, result.getMonths());
        assertEquals(0, result.getWeeks());
        assertEquals(0, result.getDays());
        assertEquals(0, result.getHours());
        assertEquals(0, result.getMinutes());
        assertEquals(0, result.getSeconds());
        assertEquals(0, result.getMillis());
    }

    @Test
    public void normalizedStandardNegativeMillisAndSeconds() {
        // -90 seconds = -90000 millis -> -1 minute -30 seconds
        Period period = new Period(0, 0, 0, 0, 0, 0, -90, 0);
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(-1, result.getMinutes());
        assertEquals(-30, result.getSeconds());
    }

    @Test
    public void normalizedStandardWeeksToDaysConversion() {
        // 2 weeks = 14 days; standard period type supports weeks, so weeks are retained as weeks rather than converted exclusively to days.
        Period period = new Period(0, 0, 2, 0, 0, 0, 0, 0);
        Period result = period.normalizedStandard(PeriodType.standard());
        assertNotNull(result);
        assertEquals(0, result.getDays());
        assertEquals(2, result.getWeeks());
    }

    @Test(expected = ArithmeticException.class)
    public void normalizedStandardArithmeticExceptionOnOverflow() {
        // Integer.MAX_VALUE hours will overflow when multiplied by millis per hour in long arithmetic?
        // Actually, Integer.MAX_VALUE * 3,600,000 fits in a long, but if we go larger or use extreme values:
        Period period = new Period(0, 0, 0, 0, Integer.MAX_VALUE, 0, 0, 0);
        // Let's use a value that definitely overflows standard millis or safeToInt conversion
        // new Period(long duration) with Long.MAX_VALUE or similar will overflow when converted or constructed
        Period huge = new Period(Long.MAX_VALUE);
        huge.normalizedStandard(PeriodType.standard());
    }

    @Test
    public void normalizedStandardWithCustomPeriodTypeOmittingWeeks() {
        // 10 days with PeriodType.yearMonthDayTime() which omits weeks
        Period period = new Period(0, 0, 1, 3, 0, 0, 0, 0); // 1 week 3 days = 10 days
        Period result = period.normalizedStandard(PeriodType.yearMonthDayTime());
        assertNotNull(result);
        assertEquals(10, result.getDays());
        assertEquals(0, result.getWeeks());
    }
}
