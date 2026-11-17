package org.apache.commons.jxpath.ri.model.jdom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.compiler.ProcessingInstructionTest;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.junit.Test;

public class JDOMNodePointer_CandorTest {

    @Test
    public void testNullNodeTest() {
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), null);
        assertTrue(result);
    }

    @Test
    public void testNodeNameTestWithNonElementNode() {
        NodeNameTest test = new NodeNameTest(new QName("foo"));
        boolean result = JDOMNodePointer.testNode(null, new Text("some text"), test);
        assertFalse(result);
    }

    @Test
    public void testNodeNameTestWildcardNoPrefix() {
        NodeNameTest test = new NodeNameTest(new QName(null, "*"));
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeNameTestMatchingLocalNameAndNamespace() {
        Namespace ns = Namespace.getNamespace("prefix", "http://example.com");
        Element element = new Element("foo", ns);
        NodeNameTest test = new NodeNameTest(new QName("prefix", "foo"), "http://example.com");
        boolean result = JDOMNodePointer.testNode(null, element, test);
        assertTrue(result);
    }

    @Test
    public void testNodeNameTestMatchingLocalNameButMismatchedNamespace() {
        Namespace ns = Namespace.getNamespace("prefix", "http://example.com/ns1");
        Element element = new Element("foo", ns);
        NodeNameTest test = new NodeNameTest(new QName("http://example.com/ns2", "foo"));
        boolean result = JDOMNodePointer.testNode(null, element, test);
        assertFalse(result);
    }

    @Test
    public void testNodeNameTestWildcardWithNamespaceCheck() {
        Namespace ns = Namespace.getNamespace("prefix", "http://example.com/ns1");
        Element element = new Element("foo", ns);
        NodeNameTest test = new NodeNameTest(new QName("prefix", "*"), "http://example.com/ns1");
        boolean result = JDOMNodePointer.testNode(null, element, test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestNodeWithElement() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestNodeWithDocument() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);
        boolean result = JDOMNodePointer.testNode(null, new Document(), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestNodeWithText() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_NODE);
        boolean result = JDOMNodePointer.testNode(null, new Text("text"), test);
        assertFalse(result);
    }

    @Test
    public void testNodeTypeTestTextWithTextNode() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_TEXT);
        boolean result = JDOMNodePointer.testNode(null, new Text("text"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestTextWithCDATA() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_TEXT);
        boolean result = JDOMNodePointer.testNode(null, new CDATA("cdata"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestCommentWithCommentNode() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_COMMENT);
        boolean result = JDOMNodePointer.testNode(null, new Comment("comment"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestPIWithProcessingInstructionNode() {
        NodeTypeTest test = new NodeTypeTest(Compiler.NODE_TYPE_PI);
        boolean result = JDOMNodePointer.testNode(null, new ProcessingInstruction("target1", "data"), test);
        assertTrue(result);
    }

    @Test
    public void testNodeTypeTestInvalidType() {
        NodeTypeTest test = new NodeTypeTest(9999);
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), test);
        assertFalse(result);
    }

    @Test
    public void testProcessingInstructionTestMatchingTarget() {
        ProcessingInstructionTest test = new ProcessingInstructionTest("target1");
        ProcessingInstruction pi = new ProcessingInstruction("target1", "data");
        boolean result = JDOMNodePointer.testNode(null, pi, test);
        assertTrue(result);
    }

    @Test
    public void testProcessingInstructionTestMismatchedTarget() {
        ProcessingInstructionTest test = new ProcessingInstructionTest("target2");
        ProcessingInstruction pi = new ProcessingInstruction("target1", "data");
        boolean result = JDOMNodePointer.testNode(null, pi, test);
        assertFalse(result);
    }

    @Test
    public void testProcessingInstructionTestWithNonPINode() {
        ProcessingInstructionTest test = new ProcessingInstructionTest("target1");
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), test);
        assertFalse(result);
    }

    @Test
    public void testUnsupportedNodeTestClass() {
        NodeTest customTest = new NodeTest() {
            // anonymous subclass of NodeTest with no special handling in JDOMNodePointer.testNode
        };
        boolean result = JDOMNodePointer.testNode(null, new Element("foo"), customTest);
        assertFalse(result);
    }
}
