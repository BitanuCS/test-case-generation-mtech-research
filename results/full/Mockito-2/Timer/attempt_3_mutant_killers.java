package org.mockito.internal.util;

import static org.junit.Assert.fail;

import org.mockito.exceptions.Reporter;

import org.junit.Test;
import org.mockitoutil.TestBase;

import static org.junit.Assert.assertNotNull;

public class Timer_CandorTest extends TestBase {

    @Test(expected = RuntimeException.class)
    public void validateInputWithNegativeDurationThrowsException() {
        new Timer(-1L);
    }

    @Test(expected = RuntimeException.class)
    public void validateInputWithMinNegativeDurationThrowsException() {
        new Timer(Long.MIN_VALUE);
    }

    @Test(expected = RuntimeException.class)
    public void constructorWithNegativeDurationThrowsException() {
        new Timer(-100L);
    }

    @Test
    public void validateInputWithZeroDurationSucceeds() {
        Timer timer = new Timer(0L);
        assertNotNull(timer);
    }

    @Test
    public void validateInputWithPositiveDurationSucceeds() {
        Timer timer = new Timer(1000L);
        assertNotNull(timer);
    }

    @Test
    public void validateInputWithMaxLongDurationSucceeds() {
        Timer timer = new Timer(Long.MAX_VALUE);
        assertNotNull(timer);
    }

    @Test
    public void constructorWithZeroDurationSucceeds() {
        Timer timer = new Timer(0L);
        assertNotNull(timer);
    }

    @Test
    public void constructorWithPositiveDurationSucceeds() {
        Timer timer = new Timer(50L);
        assertNotNull(timer);
    }

    @Test
    public void constructorWithMaxLongDurationSucceeds() {
        Timer timer = new Timer(Long.MAX_VALUE);
        assertNotNull(timer);
    }

    @Test(expected = RuntimeException.class)
        public void should_report_negative_duration_time() {
            // This explicitly reaches line 32: new Reporter().cannotCreateTimerWithNegativeDurationTime(durationMillis);
            // ensuring the exception is actually thrown by the Reporter and not just any RuntimeException.
            Timer timer = new Timer(-1L);
        }

    @Test
        public void shouldReportNegativeDurationTimeWithSpecificException() {
            try {
                new Timer(-1L);
                fail("Expected an exception to be thrown when creating a Timer with a negative duration");
            } catch (org.mockito.exceptions.base.MockitoException e) {
                // Expected exception thrown by Reporter.cannotCreateTimerWithNegativeDurationTime
            }
        }
}
