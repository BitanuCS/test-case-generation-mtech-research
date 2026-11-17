package org.jsoup.nodes;

import org.junit.Test;
import org.jsoup.select.Elements;
import org.jsoup.parser.Tag;

import static org.junit.Assert.*;

public class Document_CandorTest {

    @Test
    public void normaliseWithMissingHtmlHeadBody() {
        Document doc = new Document("http://example.com");
        doc.appendChild(new TextNode("Hello World", "http://example.com"));

        doc.normalise();

        assertNotNull(doc.head());
        assertNotNull(doc.body());
        assertEquals("Hello World", doc.body().text());
    }

    @Test
    public void normaliseWithBlankTextNodesInRoot() {
        Document doc = new Document("http://example.com");
        doc.appendChild(new TextNode("   ", "http://example.com"));

        doc.normalise();

        assertNotNull(doc.body());
        assertTrue(doc.body().text().isEmpty());
    }

    @Test
    public void normaliseWithMultipleHeadElements() {
        Document doc = Document.createShell("http://example.com");
        Element html = doc.getElementsByTag("html").first();
        
        Element head1 = doc.head();
        head1.appendChild(new TextNode("HeadOne", ""));

        Element head2 = html.appendElement("head");
        head2.appendChild(new TextNode("HeadTwo", ""));

        doc.normalise();

        Elements heads = doc.getElementsByTag("head");
        assertEquals(1, heads.size());
        assertEquals("HeadOne HeadTwo", doc.head().text());
    }

    @Test
    public void normaliseWithMultipleBodyElements() {
        Document doc = Document.createShell("http://example.com");
        Element html = doc.getElementsByTag("html").first();
        
        Element body1 = doc.body();
        body1.appendChild(new TextNode("BodyOne", ""));

        Element body2 = html.appendElement("body");
        body2.appendChild(new TextNode("BodyTwo", ""));

        doc.normalise();

        Elements bodies = doc.getElementsByTag("body");
        assertEquals(1, bodies.size());
        assertEquals("BodyOne BodyTwo", doc.body().text());
    }

    @Test
    public void normaliseWithHeadNotParentedByHtml() {
        Document doc = new Document("http://example.com");
        Element head = new Element(Tag.valueOf("head"), "http://example.com");
        doc.appendChild(head);

        doc.normalise();

        Element html = doc.getElementsByTag("html").first();
        assertNotNull(html);
        assertEquals(html, doc.head().parent());
    }

    @Test
    public void normaliseWithBodyNotParentedByHtml() {
        Document doc = new Document("http://example.com");
        Element body = new Element(Tag.valueOf("body"), "http://example.com");
        doc.appendChild(body);

        doc.normalise();

        Element html = doc.getElementsByTag("html").first();
        assertNotNull(html);
        assertEquals(html, doc.body().parent());
    }

    @Test
    public void normaliseTextNodesInHead() {
        Document doc = Document.createShell("http://example.com");
        doc.head().appendChild(new TextNode("HeadText", ""));

        doc.normalise();

        assertEquals("HeadText", doc.body().text());
    }

    @Test
    public void normaliseTextNodesInHtml() {
        Document doc = Document.createShell("http://example.com");
        Element html = doc.getElementsByTag("html").first();
        html.appendChild(new TextNode("HtmlText", ""));

        doc.normalise();

        assertEquals("HtmlText", doc.body().text());
    }

    @Test
    public void normaliseAlreadyNormalisedDocument() {
        Document doc = Document.createShell("http://example.com");
        String originalOuterHtml = doc.outerHtml();

        doc.normalise();

        assertEquals(originalOuterHtml, doc.outerHtml());
    }

    @Test
    public void normaliseStructureWithSingleElement() {
        Document doc = Document.createShell("http://example.com");
        Element head = doc.head();

        doc.normalise();

        assertEquals(head, doc.head());
    }

    @Test
    public void normaliseStructureWithDuplicateElementsAndOrphanParent() {
        Document doc = new Document("http://example.com");
        Element orphanParent = new Element(Tag.valueOf("div"), "http://example.com");
        Element head1 = new Element(Tag.valueOf("head"), "http://example.com");
        head1.appendChild(new TextNode("Child1", ""));
        orphanParent.appendChild(head1);

        Element head2 = new Element(Tag.valueOf("head"), "http://example.com");
        head2.appendChild(new TextNode("Child2", ""));
        orphanParent.appendChild(head2);

        doc.appendChild(orphanParent);

        doc.normalise();

        Elements heads = doc.getElementsByTag("head");
        assertEquals(1, heads.size());
        assertEquals("Child1 Child2", doc.head().text());
        Element html = doc.getElementsByTag("html").first();
        assertEquals(html, doc.head().parent());
    }
}
