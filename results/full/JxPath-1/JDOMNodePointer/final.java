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

    @Test
        public void testNodeNameTestWildcardWithPrefix() {
            // Targets mutants [A0], [R4], [R5] for line 361:
            // if (wildcard && testPrefix == null) -> If wildcard is true, but testPrefix IS NOT null (e.g. "prefix:*"),
            // the original code proceeds past the fast-path 'return true;' to do standard checks.
            // The mutants would incorrectly return true immediately when wildcard=true even if testPrefix != null.
            Namespace ns = Namespace.getNamespace("prefix", "http://example.com");
            Element element = new Element("foo", ns);
            
            // Wildcard with a prefix set ("prefix:*") and a different namespace URI context that won't match,
            // or ensuring it doesn't short-circuit to true.
            NodeNameTest test = new NodeNameTest(new QName("prefix", "*"), "http://wrong-namespace.com");
            boolean result = JDOMNodePointer.testNode(null, element, test);
            assertFalse(result);
        }

    @Test
        public void testNodeNameTestEqualStringsArgumentOrderSwap() {
            // Targets mutant [A2]: equalStrings(namespaceURI, nodeNS) vs equalStrings(nodeNS, namespaceURI).
            // Since equalStrings handles nulls asymmetrically:
            // private static boolean equalStrings(String s1, String s2) {
            //     if (s1 == null && s2 != null) { return false; }
            //     if (s1 != null && s2 == null) { return false; }
            //     ...
            // If namespaceURI is null and nodeNS is non-null:
            // Original: equalStrings(null, nodeNS) -> first check (s1 == null && s2 != null) triggers -> returns false.
            // Mutant:   equalStrings(nodeNS, null) -> second check (s1 != null && s2 == null) triggers -> returns false.
            // Wait, what if namespaceURI is non-null and nodeNS is null?
            // Original: equalStrings(nonNull, null) -> second check (s1 != null && s2 == null) triggers -> returns false.
            // Mutant:   equalStrings(null, nonNull) -> first check (s1 == null && s2 != null) triggers -> returns false.
            // What if both are non-null but one has trailing spaces trimmed? equalStrings trims s1 and s2!
            // wait: if (s1 != null && !s1.trim().equals(s2.trim())) -> s1.trim() is called on the FIRST argument (s1)!
            // If s1 is null, calling s1.trim() would throw NullPointerException unless guarded, but wait:
            // if (s1 == null && s2 != null) return false;
            // if (s1 != null && s2 == null) return false;
            // So by the time s1.trim() is reached, neither s1 nor s2 is null.
            // BUT if s1 is namespaceURI and s2 is nodeNS, s1.trim() trims namespaceURI. If s2.trim() trims nodeNS,
            // what if namespaceURI has trailing spaces or nodeNS has trailing spaces?
            // Let's test with a namespaceURI that has whitespace padding, where namespaceURI is the first arg (testName's namespaceURI)
            // and nodeNS has no whitespace.
            // Original: equalStrings(namespaceURI, nodeNS) -> namespaceURI.trim().equals(nodeNS.trim())
            // If namespaceURI = " http://example.com ", nodeNS = "http://example.com" -> trim() makes them equal -> returns true.
            // Mutant: equalStrings(nodeNS, namespaceURI) -> nodeNS.trim().equals(namespaceURI.trim()) -> same here.
            // What if nodeNS has whitespace and namespaceURI doesn't? Then nodeNS.trim() vs namespaceURI.trim().
            // JDOM getNamespaceURI(node) returns the exact URI from the element (usually no whitespace).
            // But what if namespaceURI in NodeNameTest has whitespace?
            Element element = new Element("foo"); // nodeNS = null
            // NodeNameTest with namespaceURI = " " (non-null, but trims to empty or different)
            // Actually, if nodeNS is null and namespaceURI is "   ", 
            // Original: equalStrings("   ", null) -> s1 != null && s2 == null -> returns false.
            // Mutant: equalStrings(null, "   ") -> s1 == null && s2 != null -> returns false.
            // Let's test when namespaceURI is null and nodeNS is non-null:
            Namespace ns = Namespace.getNamespace("http://example.com");
            Element elementWithNs = new Element("foo", ns); // nodeNS = "http://example.com"
            NodeNameTest test = new NodeNameTest(new QName("foo")); // namespaceURI = null
            boolean result = JDOMNodePointer.testNode(null, elementWithNs, test);
            assertFalse(result);
        }

    @Test
        public void testEqualStringsArgumentOrderSensitivityWithTrimmingAndNulls() {
            // Targets mutant [A2]: equalStrings(namespaceURI, nodeNS) vs equalStrings(nodeNS, namespaceURI).
            // 
            // Let's analyze equalStrings(s1, s2):
            //   if (s1 == null && s2 != null) return false;
            //   if (s1 != null && s2 == null) return false;
            //   if (s1 != null && !s1.trim().equals(s2.trim())) return false;
            //   return true;
            //
            // Notice that s1.trim() is called when s1 != null. Thus, s1 is the receiver of .trim().
            // In the original code: equalStrings(namespaceURI, nodeNS) -> namespaceURI is s1, nodeNS is s2.
            // In the mutant: equalStrings(nodeNS, namespaceURI) -> nodeNS is s1, namespaceURI is s2.
            //
            // If we supply a namespaceURI with leading/trailing whitespace (e.g., "  http://example.com  ") 
            // and a nodeNS without whitespace ("http://example.com"):
            // Original: s1 = "  http://example.com  " (non-null), s2 = "http://example.com" (non-null).
            //           s1.trim().equals(s2.trim()) -> "http://example.com".equals("http://example.com") -> true.
            // Mutant:   s1 = "http://example.com" (non-null), s2 = "  http://example.com  " (non-null).
            //           s1.trim().equals(s2.trim()) -> "http://example.com".equals("http://example.com") -> true.
            //
            // What if namespaceURI is null, and nodeNS is non-null?
            // Original: equalStrings(null, nodeNS) -> first check: s1 == null && s2 != null -> returns false.
            // Mutant:   equalStrings(nodeNS, null) -> second check: s1 != null && s2 == null -> returns false.
            //
            // What if namespaceURI is non-null, and nodeNS is null?
            // Original: equalStrings(namespaceURI, null) -> second check: s1 != null && s2 == null -> returns false.
            // Mutant:   equalStrings(null, namespaceURI) -> first check: s1 == null && s2 != null -> returns false.
            //
            // Wait, is there any case where swapping argument order changes the boolean result?
            // Let's check when one is null and the other is non-null: both order checks return false, EXCEPT if 
            // one order triggers the first `if` and the other triggers the second `if`, but both return `false`.
            // What if one argument is null and the other is *also* null? Both return true.
            // What if one argument is a whitespace-only string (e.g., "   ") and the other is null?
            // Original: equalStrings("   ", null) -> s1 != null && s2 == null -> returns false.
            // Mutant:   equalStrings(null, "   ") -> s1 == null && s2 != null -> returns false.
            // 
            // Is there a case where s1.trim() behaves differently or where strict equality of trimmed strings differs?
            // Since equals() and trim() are symmetric for strings, the only way [A2] can be detected is if 
            // code behavior relies on which argument is s1 versus s2. But wait, equalStrings is private static.
            // Let's look closely at line 369 in JDOMNodePointer:
            // return equalStrings(namespaceURI, nodeNS); vs return equalStrings(nodeNS, namespaceURI);
            // Since namespaceURI (from NodeNameTest) and nodeNS (from element) are compared, if we set up a test 
            // where we specifically test matching namespace where one is padded or non-null/null, let's make sure 
            // line 372 is fully exercised and any subtle argument-order dependency (if any) is probed.
            Namespace ns = Namespace.getNamespace("http://example.com");
            Element element = new Element("foo", ns);
            
            // NodeNameTest where namespaceURI has whitespace padding that trims to the exact nodeNS
            NodeNameTest test = new NodeNameTest(new QName("foo"), "  http://example.com  ");
            boolean result = JDOMNodePointer.testNode(null, element, test);
            assertTrue(result);
        }
}
