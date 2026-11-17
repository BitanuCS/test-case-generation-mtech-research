package org.apache.commons.collections.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapIterator;
import org.junit.Test;

public class Flat3Map_CandorTest {

    @Test(expected = IllegalStateException.class)
    public void setValueInvalidStateThrowsException() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "value1");
        MapIterator it = map.mapIterator();
        // Without calling it.next(), canRemove is false
        it.setValue("newValue");
    }

    @Test
    public void setValueFlatModeIndex1() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "oldValue");
        MapIterator it = map.mapIterator();
        it.next(); // sets canRemove = true, nextIndex = 1
        
        Object old = it.setValue("newValue");
        assertEquals("oldValue", old);
        assertEquals("newValue", map.get("key1"));
    }

    @Test
    public void setValueFlatModeIndex2() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "val1");
        map.put("key2", "oldValue");
        MapIterator it = map.mapIterator();
        it.next();
        it.next(); // nextIndex = 2
        
        Object old = it.setValue("newValue");
        assertEquals("oldValue", old);
        assertEquals("newValue", map.get("key2"));
    }

    @Test
    public void setValueFlatModeIndex3() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "val1");
        map.put("key2", "val2");
        map.put("key3", "oldValue");
        MapIterator it = map.mapIterator();
        it.next();
        it.next();
        it.next(); // nextIndex = 3
        
        Object old = it.setValue("newValue");
        assertEquals("oldValue", old);
        assertEquals("newValue", map.get("key3"));
    }

    @Test
    public void setValueWithNullValue() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "oldValue");
        MapIterator it = map.mapIterator();
        it.next();
        
        Object old = it.setValue(null);
        assertEquals("oldValue", old);
        assertEquals(null, map.get("key1"));
    }

    @Test
    public void setValueReplacingNullWithNonNull() {
        Flat3Map map = new Flat3Map();
        map.put("key1", null);
        map.put("key2", "val2");
        MapIterator it = map.mapIterator();
        it.next();
        it.next(); // points to key2, but let's point to key1
        
        // Reset iterator by creating a new one and calling next once
        MapIterator it2 = map.mapIterator();
        it2.next();
        
        Object old = it2.setValue("newValue");
        assertEquals(null, old);
        assertEquals("newValue", map.get("key1"));
    }

    @Test
    public void setValueMultipleTimesConsecutively() {
        Flat3Map map = new Flat3Map();
        map.put("key1", "v1");
        MapIterator it = map.mapIterator();
        it.next();
        
        Object old1 = it.setValue("v2");
        assertEquals("v1", old1);
        assertEquals("v2", map.get("key1"));

        Object old2 = it.setValue("v3");
        assertEquals("v2", old2);
        assertEquals("v3", map.get("key1"));
    }

    @Test
    public void setValueDelegateMode() {
        Flat3Map map = new Flat3Map();
        // Exceed size 3 to enter delegate mode
        map.put("key1", "v1");
        map.put("key2", "v2");
        map.put("key3", "v3");
        map.put("key4", "oldDelegateVal");
        
        MapIterator it = map.mapIterator();
        while (it.hasNext()) {
            Object key = it.next();
            if ("key4".equals(key)) {
                Object old = it.setValue("newDelegateVal");
                assertEquals("oldDelegateVal", old);
                break;
            }
        }
        assertEquals("newDelegateVal", map.get("key4"));
    }
}
