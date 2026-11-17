package org.joda.time.field;

import org.junit.Test;
import static org.junit.Assert.*;

import org.joda.time.DurationField;
import org.joda.time.DurationFieldType;
import org.joda.time.Days;

public class UnsupportedDurationField_CandorTest {

    @Test
    public void compareToWithAnotherUnsupportedFieldOfSameType() {
        UnsupportedDurationField field1 = UnsupportedDurationField.getInstance(DurationFieldType.days());
        UnsupportedDurationField field2 = UnsupportedDurationField.getInstance(DurationFieldType.days());
        
        int result = field1.compareTo(field2);
        assertEquals(0, result);
    }

    @Test
    public void compareToWithAnotherUnsupportedFieldOfDifferentType() {
        UnsupportedDurationField field1 = UnsupportedDurationField.getInstance(DurationFieldType.days());
        UnsupportedDurationField field2 = UnsupportedDurationField.getInstance(DurationFieldType.hours());
        
        int result = field1.compareTo(field2);
        assertEquals(0, result);
    }

    @Test
    public void compareToWithSupportedDurationField() {
        UnsupportedDurationField unsupportedField = UnsupportedDurationField.getInstance(DurationFieldType.days());
        DurationField supportedField = Days.days(1).getField();
        
        int result = unsupportedField.compareTo(supportedField);
        assertEquals(0, result);
    }

    @Test
    public void compareToWithNull() {
        UnsupportedDurationField field = UnsupportedDurationField.getInstance(DurationFieldType.days());
        
        int result = field.compareTo(null);
        assertEquals(0, result);
    }
}
