package org.apache.commons.jxpath.ri.model.dom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.compiler.ProcessingInstructionTest;
import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

public class DOMNodePointer_CandorTest {

    private Document createDocument() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    @Test
    public void nullTestReturnsTrue() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");

        boolean result = DOMNodePointer.testNode(element, null);

        assertTrue(result);
    }

    @Test
    public void nodeNameTestNonElementNodeReturnsFalse() throws Exception {
        Document doc = createDocument();
        Text textNode = doc.createTextNode("hello");
        NodeNameTest test = new NodeNameTest(new QName("foo"));

        boolean result = DOMNodePointer.testNode(textNode, test);

        assertFalse(result);
    }

    @Test
    public void nodeNameTestWildcardNoPrefixReturnsTrue() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");
        NodeNameTest test = new NodeNameTest(new QName(null, "*"));

        boolean result = DOMNodePointer.testNode(element, test);

        assertTrue(result);
    }

    @Test
    public void nodeNameTestWildcardWithNamespaceMatching() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElementNS("http://ns", "prefix:foo");
        NodeNameTest test = new NodeNameTest(new QName(null, "*"), "http://ns");

        boolean result = DOMNodePointer.testNode(element, test);

        assertTrue(result);
    }

    @Test
    public void nodeNameTestLocalNameAndNamespaceMatch() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElementNS("http://ns", "foo");
        NodeNameTest test = new NodeNameTest(new QName("foo"), "http://ns");

        boolean result = DOMNodePointer.testNode(element, test);

        assertTrue(result);
    }

    @Test
    public void nodeNameTestLocalNameMatchNamespaceMismatch() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElementNS("http://ns1", "foo");
        NodeNameTest test = new NodeNameTest(new QName("foo"), "http://ns2");

        boolean result = DOMNodePointer.testNode(element, test);

        assertFalse(result);
    }

    @Test
    public void nodeNameTestLocalNameMismatch() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("bar");
        NodeNameTest test = new NodeNameTest(new QName("foo"));

        boolean result = DOMNodePointer.testNode(element, test);

        assertFalse(result);
    }

    @Test
    public void nodeTypeTestNodeNodeTypeElement() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);

        boolean result = DOMNodePointer.testNode(element, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestNodeNodeTypeDocument() throws Exception {
        Document doc = createDocument();
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);

        boolean result = DOMNodePointer.testNode(doc, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestNodeNodeTypeInvalidType() throws Exception {
        Document doc = createDocument();
        Comment comment = doc.createComment("test");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);

        boolean result = DOMNodePointer.testNode(comment, test);

        assertFalse(result);
    }

    @Test
    public void nodeTypeTestTextNodeText() throws Exception {
        Document doc = createDocument();
        Text text = doc.createTextNode("text");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_TEXT);

        boolean result = DOMNodePointer.testNode(text, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestTextNodeCdata() throws Exception {
        Document doc = createDocument();
        CDATASection cdata = doc.createCDATASection("cdata");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_TEXT);

        boolean result = DOMNodePointer.testNode(cdata, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestCommentNode() throws Exception {
        Document doc = createDocument();
        Comment comment = doc.createComment("comment");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_COMMENT);

        boolean result = DOMNodePointer.testNode(comment, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestPiNode() throws Exception {
        Document doc = createDocument();
        ProcessingInstruction pi = doc.createProcessingInstruction("target", "data");
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_PI);

        boolean result = DOMNodePointer.testNode(pi, test);

        assertTrue(result);
    }

    @Test
    public void nodeTypeTestUnknownCompilerType() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");
        NodeTypeTest test = new NodeTypeTest(999);

        boolean result = DOMNodePointer.testNode(element, test);

        assertFalse(result);
    }

    @Test
    public void processingInstructionTestMatchingTarget() throws Exception {
        Document doc = createDocument();
        ProcessingInstruction pi = doc.createProcessingInstruction("target1", "data");
        ProcessingInstructionTest test = new ProcessingInstructionTest("target1");

        boolean result = DOMNodePointer.testNode(pi, test);

        assertTrue(result);
    }

    @Test
    public void processingInstructionTestNonPiNodeReturnsFalse() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");
        ProcessingInstructionTest test = new ProcessingInstructionTest("target1");

        boolean result = DOMNodePointer.testNode(element, test);

        assertFalse(result);
    }

    @Test
    public void processingInstructionTestTargetMismatch() throws Exception {
        Document doc = createDocument();
        ProcessingInstruction pi = doc.createProcessingInstruction("target1", "data");
        ProcessingInstructionTest test = new ProcessingInstructionTest("target2");

        boolean result = DOMNodePointer.testNode(pi, test);

        assertFalse(result);
    }

    @Test
    public void unknownTestImplementationReturnsFalse() throws Exception {
        Document doc = createDocument();
        Element element = doc.createElement("foo");
        NodeTest customTest = new NodeTest() {};

        boolean result = DOMNodePointer.testNode(element, customTest);

        assertFalse(result);
    }
}
