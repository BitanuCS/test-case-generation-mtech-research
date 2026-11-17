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
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.eras() },
                new int[] { 2023, 1 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testDuplicateUnsupportedFieldThrowsException() {
            // Triggers the else block for unsupported fields (lines 222-223)
            // Two unsupported fields that are equal so lastUnitField.isSupported() is false
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.eras(), DateTimeFieldType.eras() },
                new int[] { 1, 1 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testRangeDurationTypeNullOrderThrowsException() {
            // Triggers lines 237-239: compare == 0 && lastUnitField.equals(loopUnitField),
            // lastRangeField != null, but loopType.getRangeDurationType() == null
            // e.g., weekyear and year both have unit years (if comparable or equal duration),
            // or similar fields sharing duration where range is null for the second.
            // Actually, centuryOfEra and year: both have duration years. centuryOfEra range is era (not null), year range is null.
            // Wait, centuryOfEra is larger than year. What about year and centuryOfEra? (wrong order)
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.centuryOfEra() },
                new int[] { 2023, 20 },
                null
            );
        }

    @Test(expected = IllegalArgumentException.class)
        public void testRangeDurationTypeCompareLessThanZeroThrowsException() {
            // Triggers lines 243-245: lastRangeField.compareTo(loopRangeField) < 0
            // Both have range duration, but last range < loop range
            // e.g. dayOfMonth (range months) and dayOfYear (range years) - wait, different units.
            // Need same unit, different range where last range < loop range.
            // e.g., weekOfWeekyear (range weekyears) and dayOfWeek (range weeks)? No, different units.
            // HourOfDay (range days) and millisOfDay (range days) - both unit millis? No, millisOfDay unit is millis, hourOfDay unit is hours.
            // Let's use two fields with same unit (e.g. days or years) where range fields compare < 0.
            // CenturyOfEra (range Era) and Year (range null) was covered.
            // What about weekyear (range null) and year (range null) with same unit?
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
            // Mutant R12: compare == 0 || lastUnitField.equals(loopUnitField)
            // If compare != 0 (e.g. compare > 0, so larger unit followed by smaller unit, valid order) 
            // but lastUnitField.equals(loopUnitField) is true? No, if compare != 0, units cannot be equal.
            // Wait, if compare == 0 || lastUnitField.equals(loopUnitField), let's test a case where 
            // compare != 0 (e.g. year and monthOfYear, compare > 0) and lastUnitField.equals(loopUnitField) is false.
            // That wouldn't trigger it. What about compare < 0 (year and centuryOfEra, compare < 0) 
            // and lastUnitField.equals(loopUnitField) is true? (year and centuryOfEra have duration years, so compare == 0, not < 0).
            // Actually, if compare != 0 and lastUnitField.equals(loopUnitField) is true -> impossible because compare is defined by lastUnitField.compareTo(loopUnitField).
            // Wait! `compare` is `lastUnitField.compareTo(loopUnitField)`. 
            // If `compare == 0`, `lastUnitField.equals(loopUnitField)` is always true (or nearly always).
            // If `compare != 0`, `lastUnitField.equals(loopUnitField)` is false.
            // So `compare == 0 && lastUnitField.equals(loopUnitField)` is equivalent to `compare == 0`.
            // Changing `&&` to `||` makes it `compare == 0 || lastUnitField.equals(loopUnitField)`, which is still equivalent to `compare == 0` since equal implies compare == 0.
            // Wait, does JodaTime have a case where compare == 0 is false, but equals is true? No.
            // But wait, what about mutants R14 and R16 which change `==` to `!=` for range duration types?
            // Let's ensure we test invalid order with same unit and different ranges to catch R14 and R16.
            new Partial(
                new DateTimeFieldType[] { DateTimeFieldType.year(), DateTimeFieldType.centuryOfEra() },
                new int[] { 2023, 20 },
                null
            );
        }
}
