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

    @Test
        public void testMutantA3_TypesCloned() {
            DateTimeFieldType[] types = new DateTimeFieldType[] { DateTimeFieldType.year() };
            int[] values = new int[] { 2023 };
            Partial partial = new Partial(types, values, null);
            // Mutant A3 assigns types directly instead of cloning.
            // Mutating the input array after construction should not affect the Partial if cloned.
            types[0] = DateTimeFieldType.monthOfYear();
            org.junit.Assert.assertEquals(DateTimeFieldType.year(), partial.getFieldType(0));
        }

    @Test
        public void testMutantA4_ValuesCloned() {
            DateTimeFieldType[] types = new DateTimeFieldType[] { DateTimeFieldType.year() };
            int[] values = new int[] { 2023 };
            Partial partial = new Partial(types, values, null);
            // Mutant A4 assigns values directly instead of cloning.
            // Mutating the input array after construction should not affect the Partial if cloned.
            values[0] = 2024;
            org.junit.Assert.assertEquals(2023, partial.getValue(0));
        }

    @Test(expected = IllegalArgumentException.class)
        public void testUnsupportedFieldOrderThrowsException() {
            // Triggers lines 218-223: unsupported fields where lastUnitField.isSupported() is true
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.era() },
                new int[] { 2023, 1 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testDuplicateUnsupportedFieldThrowsException() {
            // Triggers the else block for unsupported fields (lines 222-223)
            // Two unsupported fields that are equal so lastUnitField.isSupported() is false
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.era(), DateTimeFieldType.era() },
                new int[] { 1, 1 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testRangeDurationTypeNullOrderThrowsException() {
            // Triggers lines 237-239: compare == 0 && lastUnitField.equals(loopUnitField),
            // lastRangeField != null, but loopType.getRangeDurationType() == null
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.centuryOfEra() },
                new int[] { 2023, 20 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testRangeDurationTypeCompareLessThanZeroThrowsException() {
            // Triggers lines 243-245: lastRangeField.compareTo(loopRangeField) < 0
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.weekyear(), DateTimeFieldType.year() },
                new int[] { 2023, 2023 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testRangeDurationTypeCompareEqualsZeroThrowsException() {
            // Triggers lines 247-249: lastRangeField.compareTo(loopRangeField) == 0 (duplicate range & unit)
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.hourOfDay(), DateTimeFieldType.hourOfDay() },
                new int[] { 12, 12 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testLogicalOrInsteadOfAndMutantR12() {
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.centuryOfEra() },
                new int[] { 2023, 20 },
                null
            );
        }
}
