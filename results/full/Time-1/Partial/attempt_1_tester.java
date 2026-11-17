package org.joda.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.joda.time.chrono.ISOChronology;
import org.junit.Test;

public class Partial_CandorTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullTypesThrowsException() {
        new Partial(null, new int[] { 2023 }, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullValuesThrowsException() {
        new Partial(new DateTimeFieldType[] { DateTimeFieldType.year() }, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorMismatchedLengthThrowsException() {
        new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.year() },
            new int[] { 2023, 6 },
            null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullTypeInArrayThrowsException() {
        new Partial(
            new DateTimeFieldType[] { null },
            new int[] { 2023 },
            null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWrongOrderThrowsException() {
        new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.monthOfYear(), DateTimeFieldType.year() },
            new int[] { 6, 2023 },
            null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorDuplicateTypeThrowsException() {
        new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.year() },
            new int[] { 2023, 2023 },
            null
        );
    }

    @Test
    public void constructorEmptyArraysSuccess() {
        Partial partial = new Partial(new DateTimeFieldType[0], new int[0], null);
        assertNotNull(partial);
        assertEquals(0, partial.size());
    }

    @Test
    public void constructorValidSingleFieldSuccess() {
        Partial partial = new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.year() },
            new int[] { 2023 },
            null
        );
        assertNotNull(partial);
        assertEquals(1, partial.size());
        assertEquals(2023, partial.getValue(0));
        assertEquals(DateTimeFieldType.year(), partial.getFieldType(0));
    }

    @Test
    public void constructorValidMultipleFieldsSuccess() {
        Partial partial = new Partial(
            new DateTimeFieldType[] {
                DateTimeFieldType.year(),
                DateTimeFieldType.monthOfYear(),
                DateTimeFieldType.dayOfMonth()
            },
            new int[] { 2023, 6, 15 },
            null
        );
        assertNotNull(partial);
        assertEquals(3, partial.size());
        assertEquals(2023, partial.getValue(0));
        assertEquals(6, partial.getValue(1));
        assertEquals(15, partial.getValue(2));
        assertEquals(DateTimeFieldType.year(), partial.getFieldType(0));
        assertEquals(DateTimeFieldType.monthOfYear(), partial.getFieldType(1));
        assertEquals(DateTimeFieldType.dayOfMonth(), partial.getFieldType(2));
    }

    @Test
    public void constructorExplicitChronologySuccess() {
        Chronology chrono = ISOChronology.getInstanceUTC();
        Partial partial = new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.year() },
            new int[] { 2023 },
            chrono
        );
        assertNotNull(partial);
        assertEquals(1, partial.size());
        assertEquals(2023, partial.getValue(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorInvalidFieldValueThrowsException() {
        new Partial(
            new DateTimeFieldType[] { DateTimeFieldType.monthOfYear() },
            new int[] { 13 },
            null
        );
    }
}
