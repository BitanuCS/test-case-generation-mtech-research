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
        public void testRangeDurationTypeCompareEqualsZeroThrowsException() {
            // Triggers lines 247-249: lastRangeField.compareTo(loopRangeField) == 0 (duplicate range & unit)
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.hourOfDay(), DateTimeFieldType.hourOfDay() },
                new int[] { 12, 12 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testCoverageRangeDurationTypeNullOpposite() {
            // dayOfMonth and dayOfYear both have unit duration 'days' (compare == 0).
            // dayOfMonth has range duration 'months' (!= null).
            // dayOfYear has range duration 'years' (!= null).
            // Since dayOfYear has a range duration, loopType.getRangeDurationType() != null,
            // so it falls through the null check and hits the order/comparison logic where dayOfMonth < dayOfYear is invalid.
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.dayOfMonth(), DateTimeFieldType.dayOfYear() },
                new int[] { 1, 1 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testCoverageRangeDurationCompareLessThanZero() {
            // Targets lines 244-245:
            // if (lastRangeField.compareTo(loopRangeField) < 0) { throw ... }
            // This kills R18 (< to <=) and R19 (constant 1->2 / index shift).
            // We need same unit duration (compare == 0), both have range duration types, 
            // but `lastRangeField.compareTo(loopRangeField) < 0` (or `== 0` for R18).
            // If lastRangeField.compareTo(loopRangeField) < 0, original throws exception. 
            // With R18 (`<=`), if `compareTo == 0`, original does NOT throw on line 244 (it goes to check == 0 next), 
            // but mutant with `<=` WOULD throw on line 244 instead of line 249!
            // Wait! If `compareTo == 0`, original proceeds to `if (lastRangeField.compareTo(loopRangeField) == 0)` and throws "Types array must not contain duplicate".
            // Mutant with `<=` throws "Types array must be in order largest-smallest: ... < ..." instead of duplicate!
            // Therefore, passing a duplicate pair where both have range fields (e.g., two dayOfMonths? But dayOfMonth and dayOfMonth have compare == 0 and range compare == 0).
            // Let's test two identical fields with ranges, like `dayOfMonth` and `dayOfMonth`:
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.dayOfMonth(), DateTimeFieldType.dayOfMonth() },
                new int[] { 1, 1 },
                null
            );
        }
}
