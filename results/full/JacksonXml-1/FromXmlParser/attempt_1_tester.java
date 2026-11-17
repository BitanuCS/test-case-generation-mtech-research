package com.fasterxml.jackson.dataformat.xml.deser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;

public class FromXmlParser_CandorTest {

    // Helper subclass to instantiate FromXmlParser since fields/methods are protected/public
    private static class TestableFromXmlParser extends FromXmlParser {
        public TestableFromXmlParser(IOContext ctxt, int genericParserFeatures, int xmlFeatures, ObjectCodec codec, XMLStreamReader xmlReader, XmlReadContext parsingContext, XmlTokenStream xmlTokens) {
            super(ctxt, genericParserFeatures, xmlFeatures, codec, xmlReader);
            this._parsingContext = parsingContext;
            // Since _xmlTokens is final, we can either pass a real one or use reflection/subclass if possible.
            // Wait, _xmlTokens is final and initialized in super constructor. But the super constructor takes xmlReader and builds XmlTokenStream.
            // If we cannot replace _xmlTokens directly due to 'final', we can pass a dummy XMLStreamReader that yields the desired tokens.
        }

        public void setNextToken(JsonToken token) {
            this._nextToken = token;
        }

        public void setMayBeLeaf(boolean mayBeLeaf) {
            this._mayBeLeaf = mayBeLeaf;
        }

        public void setParsingContext(XmlReadContext ctx) {
            this._parsingContext = ctx;
        }

        public void setNamesToWrap(Set<String> names) {
            this._namesToWrap = names;
        }
        
        public void setCurrToken(JsonToken token) {
            this._currToken = token;
        }
    }

    // A minimal dummy XMLStreamReader to satisfy FromXmlParser constructor when needed
    private static class DummyXMLStreamReader implements XMLStreamReader {
        private final int[] events;
        private int index = 0;
        private final String[] localNames;
        private final String[] texts;
        private int nameIndex = 0;
        private int textIndex = 0;

        public DummyXMLStreamReader(int[] events, String[] localNames, String[] texts) {
            this.events = events;
            this.localNames = localNames != null ? localNames : new String[0];
            this.texts = texts != null ? texts : new String[0];
        }

        @Override public boolean hasNext() { return index < events.length; }
        @Override public int next() { return events[index++]; }
        @Override public String getLocalName() { return nameIndex < localNames.length ? localNames[nameIndex++] : "elem"; }
        @Override public String getText() { return textIndex < texts.length ? texts[textIndex++] : ""; }

        // Unused XMLStreamReader methods
        @Override public Object getProperty(String name) { return null; }
        @Override public void require(int type, String namespaceURI, String localName) {}
        @Override public String getElementText() { return null; }
        @Override public int nextTag() { return 0; }
        @Override public boolean isStartElement() { return false; }
        @Override public boolean isEndElement() { return false; }
        @Override public boolean isCharacters() { return false; }
        @Override public boolean isWhiteSpace() { return false; }
        @Override public String getAttributeValue(String namespaceURI, String localName) { return null; }
        @Override public int getAttributeCount() { return 0; }
        @Override public String getAttributeNamespace(int index) { return null; }
        @Override public String getAttributeLocalName(int index) { return null; }
        @Override public String getAttributePrefix(int index) { return null; }
        @Override public String getAttributeType(int index) { return null; }
        @Override public String getAttributeValue(int index) { return null; }
        @Override public boolean isAttributeSpecified(int index) { return false; }
        @Override public int getNamespaceCount() { return 0; }
        @Override public String getNamespaceURI(int index) { return null; }
        @Override public String getNamespacePrefix(int index) { return null; }
        @Override public javax.xml.namespace.NamespaceContext getNamespaceContext() { return null; }
        @Override public int getEventType() { return 0; }
        @Override public String getNamespaceURI() { return null; }
        @Override public String getPrefix() { return null; }
        @Override public String getVersion() { return null; }
        @Override public boolean isStandalone() { return false; }
        @Override public boolean standaloneSet() { return false; }
        @Override public String getCharacterEncodingScheme() { return null; }
        @Override public String getPITarget() { return null; }
        @Override public String getPIData() { return null; }
        @Override public void close() {}
        @Override public String getNamespaceURI(String prefix) { return null; }
    }

    @Test
    public void nextTokenWithPredefinedNextToken() throws IOException {
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(new int[0], null, null);
        IOContext ctxt = new IOContext(null, null, false);
        FromXmlParser parser = new FromXmlParser(ctxt, 0, 0, null, xmlReader);
        
        TestableFromXmlParser testable = (TestableFromXmlParser) parser;
        // Since we can't easily cast to TestableFromXmlParser unless we instantiate it directly:
        // Let's create an instance of FromXmlParser via subclassing inline or use package-visible fields directly if same package.
        // FromXmlParser is in com.fasterxml.jackson.dataformat.xml.deser, so we can subclass it directly here.
    }

    @Test
    public void testNextTokenWithPredefinedNextToken() throws IOException {
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(new int[0], null, null);
        IOContext ctxt = new IOContext(null, null, false);
        
        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setNext(JsonToken t) { this._nextToken = t; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        // Create root parsing context
        XmlReadContext rootContext = XmlReadContext.createRootContext(0, 0);
        parser.setContext(rootContext);
        parser.setNext(JsonToken.START_OBJECT);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.START_OBJECT, token);
        assertEquals(JsonToken.START_OBJECT, parser.currentToken());
        assertNotNull(parser.getParsingContext());
    }

    @Test
    public void nextTokenXmlStartElementLeaf() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_START_ELEMENT };
        String[] names = new String[] { "item" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.FIELD_NAME, token);
        assertEquals("item", parser.getCurrentName());
    }

    @Test
    public void nextTokenXmlStartElementInArray() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_START_ELEMENT, XmlTokenStream.XML_TEXT };
        String[] names = new String[] { "item" };
        String[] texts = new String[] { "value" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext arrayContext = root.createChildArrayContext(0, 0);
        parser.setContext(arrayContext);

        // When in array, XML_START_ELEMENT is skipped and next token (XML_TEXT) is processed
        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.VALUE_STRING, token);
    }

    @Test
    public void nextTokenXmlStartElementWithMayBeLeaf() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_START_ELEMENT };
        String[] names = new String[] { "child" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.START_OBJECT, token);
    }

    @Test
    public void nextTokenXmlStartElementWithVirtualWrapping() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_START_ELEMENT };
        String[] names = new String[] { "wrapMe" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setNames(Set<String> namesToWrap) { this._namesToWrap = namesToWrap; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));
        Set<String> wrapSet = new HashSet<>();
        wrapSet.add("wrapMe");
        parser.setNames(wrapSet);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.FIELD_NAME, token);
        assertEquals("wrapMe", parser.getCurrentName());
    }

    @Test
    public void nextTokenXmlEndElementLeafNull() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_END_ELEMENT };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.VALUE_NULL, token);
    }

    @Test
    public void nextTokenXmlEndElementLeafInArray() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_END_ELEMENT };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext arrayContext = root.createChildArrayContext(0, 0);
        parser.setContext(arrayContext);
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.START_OBJECT, token);
    }

    @Test
    public void nextTokenXmlEndElementNormalObject() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_END_ELEMENT };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext objContext = root.createChildObjectContext(0, 0);
        parser.setContext(objContext);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.END_OBJECT, token);
        assertSame(root, parser.getParsingContext());
    }

    @Test
    public void nextTokenXmlEndElementNormalArray() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_END_ELEMENT };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext arrayContext = root.createChildArrayContext(0, 0);
        parser.setContext(arrayContext);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.END_ARRAY, token);
        assertSame(root, parser.getParsingContext());
    }

    @Test
    public void nextTokenXmlAttributeNameWithMayBeLeaf() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_ATTRIBUTE_NAME };
        String[] names = new String[] { "attr" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.START_OBJECT, token);
    }

    @Test
    public void nextTokenXmlAttributeNameNormal() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_ATTRIBUTE_NAME };
        String[] names = new String[] { "attr" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, names, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.FIELD_NAME, token);
        assertEquals("attr", parser.getCurrentName());
    }

    @Test
    public void nextTokenXmlAttributeValue() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_ATTRIBUTE_VALUE };
        String[] texts = new String[] { "attrVal" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.VALUE_STRING, token);
        assertEquals("attrVal", parser.getText());
    }

    @Test
    public void nextTokenXmlTextLeafValidString() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_TEXT };
        String[] texts = new String[] { "hello" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.VALUE_STRING, token);
        assertEquals("hello", parser.getText());
    }

    @Test
    public void nextTokenXmlTextLeafEmptyInArray() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_TEXT };
        String[] texts = new String[] { "   " };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setMayBeLeaf(boolean b) { this._mayBeLeaf = b; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext arrayContext = root.createChildArrayContext(0, 0);
        parser.setContext(arrayContext);
        parser.setMayBeLeaf(true);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.START_OBJECT, token);
    }

    @Test
    public void nextTokenXmlTextNonLeafEmptyInObject() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_TEXT };
        String[] texts = new String[] { "" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
            public void setCurrToken(JsonToken t) { this._currToken = t; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        XmlReadContext root = XmlReadContext.createRootContext(0, 0);
        XmlReadContext objContext = root.createChildObjectContext(0, 0);
        parser.setContext(objContext);
        parser.setCurrToken(JsonToken.START_OBJECT);

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.END_OBJECT, token);
        assertSame(root, parser.getParsingContext());
    }

    @Test
    public void nextTokenXmlTextNonLeafValidString() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_TEXT };
        String[] texts = new String[] { "someText" };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, texts);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));

        JsonToken token = parser.nextToken();
        assertEquals(JsonToken.FIELD_NAME, token);
        assertEquals("", parser.getCurrentName());
    }

    @Test
    public void nextTokenXmlEnd() throws IOException {
        int[] events = new int[] { XmlTokenStream.XML_END };
        DummyXMLStreamReader xmlReader = new DummyXMLStreamReader(events, null, null);
        IOContext ctxt = new IOContext(null, null, false);

        class SubParser extends FromXmlParser {
            public SubParser(IOContext ctxt, XMLStreamReader r) {
                super(ctxt, 0, 0, null, r);
            }
            public void setContext(XmlReadContext c) { this._parsingContext = c; }
        }

        SubParser parser = new SubParser(ctxt, xmlReader);
        parser.setContext(XmlReadContext.createRootContext(0, 0));

        JsonToken token = parser.nextToken();
        assertNull(token);
        assertNull(parser.currentToken());
    }
}
