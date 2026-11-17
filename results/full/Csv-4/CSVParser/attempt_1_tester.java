package org.apache.commons.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

public class CSVParser_CandorTest {

    @Test
    public void testGetHeaderMapWhenNull() throws IOException {
        final CSVParser parser = new CSVParser(new StringReader("a,b\n1,2"), CSVFormat.DEFAULT);
        assertNull(parser.getHeaderMap());
    }

    @Test
    public void testGetHeaderMapWhenPresent() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("Col1", "Col2");
        final CSVParser parser = new CSVParser(new StringReader("1,2"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(2, headerMap.size());
        assertTrue(headerMap.containsKey("Col1"));
        assertTrue(headerMap.containsKey("Col2"));
    }

    @Test
    public void testGetHeaderMapReturnsCopy() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("Col1", "Col2");
        final CSVParser parser = new CSVParser(new StringReader("1,2"), format);
        final Map<String, Integer> map1 = parser.getHeaderMap();
        final Map<String, Integer> map2 = parser.getHeaderMap();
        assertNotNull(map1);
        assertNotNull(map2);
        assertTrue(map1 != map2); // Different instances (copy)
        
        map1.put("Modified", 99);
        assertTrue(!parser.getHeaderMap().containsKey("Modified"));
    }

    @Test
    public void testGetHeaderMapColumnOrder() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("Z", "A", "M");
        final CSVParser parser = new CSVParser(new StringReader("1,2,3"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        
        String[] keys = headerMap.keySet().toArray(new String[0]);
        assertEquals(3, keys.length);
        assertEquals("Z", keys[0]);
        assertEquals("A", keys[1]);
        assertEquals("M", keys[2]);
    }

    @Test
    public void testGetHeaderMapIndicesValues() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("first", "second", "third");
        final CSVParser parser = new CSVParser(new StringReader("1,2,3"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        
        assertEquals(Integer.valueOf(0), headerMap.get("first"));
        assertEquals(Integer.valueOf(1), headerMap.get("second"));
        assertEquals(Integer.valueOf(2), headerMap.get("third"));
    }

    @Test
    public void testGetHeaderMapWithAutomaticHeaderDetection() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader();
        final CSVParser parser = new CSVParser(new StringReader("ColA,ColB\nval1,val2"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(Integer.valueOf(0), headerMap.get("ColA"));
        assertEquals(Integer.valueOf(1), headerMap.get("ColB"));
    }

    @Test
    public void testGetHeaderMapSingleHeader() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("SingleCol");
        final CSVParser parser = new CSVParser(new StringReader("val"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(1, headerMap.size());
        assertEquals(Integer.valueOf(0), headerMap.get("SingleCol"));
    }

    @Test
    public void testGetHeaderMapEmptyHeaderArray() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader(new String[0]);
        final CSVParser parser = new CSVParser(new StringReader("val"), format);
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertTrue(headerMap.isEmpty());
    }

    @Test
    public void testGetHeaderMapAfterRecordsParsed() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("Col1", "Col2");
        final CSVParser parser = new CSVParser(new StringReader("Col1,Col2\nval1,val2\nval3,val4"), format);
        parser.nextRecord(); // consume a record
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(2, headerMap.size());
        assertEquals(Integer.valueOf(0), headerMap.get("Col1"));
        assertEquals(Integer.valueOf(1), headerMap.get("Col2"));
    }

    @Test
    public void testGetHeaderMapAfterParserClosed() throws IOException {
        final CSVFormat format = CSVFormat.DEFAULT.withHeader("Col1", "Col2");
        final CSVParser parser = new CSVParser(new StringReader("val1,val2"), format);
        parser.close();
        final Map<String, Integer> headerMap = parser.getHeaderMap();
        assertNotNull(headerMap);
        assertEquals(2, headerMap.size());
        assertEquals(Integer.valueOf(0), headerMap.get("Col1"));
        assertEquals(Integer.valueOf(1), headerMap.get("Col2"));
    }
}
