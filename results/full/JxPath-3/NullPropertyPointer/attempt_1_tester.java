package org.apache.commons.jxpath.ri.model.beans;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathAbstractFactoryException;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.junit.Test;

import static org.junit.Assert.*;

public class NullPropertyPointer_CandorTest {

    // Helper subclass to expose protected/package-private methods or behavior if needed,
    // or we can test using concrete subclasses/stubbing behavior where possible.
    // Since we only use actual public/protected APIs available in the class or its superclasses:

    @Test(expected = JXPathAbstractFactoryException.class)
    public void parentNullPointerUnchangedThrowsBadFactoryException() {
        // Setup: parent is a NullPointer and parent.createPath(context) returns the same NullPointer.
        // We construct a NullPointer as parent.
        NullPointer nullParent = new NullPointer(new QName("test"));
        NullPropertyPointer pointer = new NullPropertyPointer(nullParent) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        // nullParent.createPath(context) when parent is NullPointer and uncreated will return itself (or another NullPointer equal to it).
        // Let's invoke createPath which should trigger parent.createPath(context) returning the same NullPointer,
        // throwing JXPathAbstractFactoryException.
        pointer.createPath(context);
    }

    @Test(expected = JXPathAbstractFactoryException.class)
    public void parentNullPointerUnchangedThrowsBadFactoryExceptionWithObjectValue() {
        NullPointer nullParent = new NullPointer(new QName("test"));
        NullPropertyPointer pointer = new NullPropertyPointer(nullParent) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        pointer.createPath(context, "test");
    }

    @Test
    public void attributePathCreation() {
        // parent.createPath(context) returns a valid parent NodePointer, isAttribute() is true
        // Since we cannot easily mock without a mocking framework, we use a concrete NodePointer implementation
        // or a subclass of NodePointer / NullPointer where createPath returns a stub/mock-like pointer.
        // Wait, NullPointer.createPath returns a NodePointer. Let's create a custom NodePointer subclass for testing.
        NodePointer parentPointer = new NodePointer(null, new QName("parent")) {
            @Override
            public NodePointer createPath(JXPathContext context) {
                return this; // returns a valid parent NodePointer
            }

            @Override
            public NodePointer createAttribute(JXPathContext context, QName name) {
                return this; // mock attribute creation returning this
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Object getBaseValue() {
                return null;
            }

            @Override
            public Object getImmediateNode() {
                return null;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
                return 0;
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return true;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        NodePointer result = pointer.createPath(context);
        assertNotNull(result);
        assertSame(parentPointer, result);
    }

    @Test
    public void attributePathCreationWithValue() {
        NodePointer parentPointer = new NodePointer(null, new QName("parent")) {
            private Object storedValue;

            @Override
            public NodePointer createPath(JXPathContext context) {
                return this;
            }

            @Override
            public NodePointer createAttribute(JXPathContext context, QName name) {
                return this;
            }

            @Override
            public void setValue(Object value) {
                this.storedValue = value;
            }

            @Override
            public Object getImmediateNode() {
                return storedValue;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Object getBaseValue() {
                return null;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
                return 0;
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return true;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        NodePointer result = pointer.createPath(context, "someValue");
        assertNotNull(result);
        assertEquals("someValue", result.getImmediateNode());
    }

    @Test
    public void propertyOwnerPointerParentExpansion() {
        // parent.createPath(context) returns a PropertyOwnerPointer (not a NullPointer), isAttribute() is false
        // PropertyOwnerPointer is abstract, but BeanPointer is a concrete subclass of PropertyOwnerPointer.
        // We can use a simple bean object as the base value for a BeanPointer, or subclass PropertyOwnerPointer.
        Object bean = new Object();
        BeanPointer parentPointer = new BeanPointer(NodePointer.newNodePointer(new QName("root"), bean, null), new QName("bean"), bean, null) {
            @Override
            public NodePointer createPath(JXPathContext context) {
                return this; // returns itself (PropertyOwnerPointer)
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(bean);
        // This will call parent.createPath(context) -> returns parentPointer (PropertyOwnerPointer),
        // casts to PropertyOwnerPointer, takes getPropertyPointer(), and calls createChild(...)
        // Since BeanPointer.getPropertyPointer() returns a PropertyPointer, calling createChild on it creates a child pointer.
        NodePointer result = pointer.createPath(context);
        assertNotNull(result);
    }

    @Test
    public void propertyOwnerPointerParentExpansionWithValue() {
        Object bean = new Object();
        BeanPointer parentPointer = new BeanPointer(NodePointer.newNodePointer(new QName("root"), bean, null), new QName("bean"), bean, null) {
            @Override
            public NodePointer createPath(JXPathContext context) {
                return this;
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(bean);
        NodePointer result = pointer.createPath(context, "val");
        assertNotNull(result);
    }

    @Test
    public void nonPropertyOwnerPointerParentExpansion() {
        // parent.createPath(context) returns a non-PropertyOwnerPointer (e.g., NodePointer that is not PropertyOwnerPointer)
        NodePointer parentPointer = new NodePointer(null, new QName("parent")) {
            @Override
            public NodePointer createPath(JXPathContext context) {
                return this; // returns self, which is NOT an instance of PropertyOwnerPointer
            }

            @Override
            public NodePointer createChild(JXPathContext context, QName name, int index) {
                return this; // directly called
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Object getBaseValue() {
                return null;
            }

            @Override
            public Object getImmediateNode() {
                return null;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
                return 0;
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        NodePointer result = pointer.createPath(context);
        assertNotNull(result);
        assertSame(parentPointer, result);
    }

    @Test
    public void nonPropertyOwnerPointerParentExpansionWithValue() {
        NodePointer parentPointer = new NodePointer(null, new QName("parent")) {
            @Override
            public NodePointer createPath(JXPathContext context) {
                return this;
            }

            @Override
            public NodePointer createChild(JXPathContext context, QName name, int index, Object value) {
                return this;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Object getBaseValue() {
                return null;
            }

            @Override
            public Object getImmediateNode() {
                return null;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
                return 0;
            }
        };

        NullPropertyPointer pointer = new NullPropertyPointer(parentPointer) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        NodePointer result = pointer.createPath(context, 123);
        assertNotNull(result);
        assertSame(parentPointer, result);
    }

    @Test
    public void badFactoryExceptionMessageFormattingWithoutNameAttribute() {
        // Test createBadFactoryException indirectly or via exception message check when parent NullPointer is unchanged
        NullPointer nullParent = new NullPointer(new QName("test"));
        NullPropertyPointer pointer = new NullPropertyPointer(nullParent) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };
        pointer.setPropertyIndex(0);
        pointer.setPropertyName("*");

        JXPathContext context = JXPathContext.newContext(new Object());
        try {
            pointer.createPath(context);
            fail("Expected JXPathAbstractFactoryException");
        } catch (JXPathAbstractFactoryException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Factory"));
        }
    }

    @Test
    public void badFactoryExceptionMessageFormattingWithNameAttribute() {
        NullPointer nullParent = new NullPointer(new QName("test"));
        NullPropertyPointer pointer = new NullPropertyPointer(nullParent) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };
        pointer.setPropertyIndex(0);
        pointer.setPropertyName("id");
        pointer.setNameAttributeValue("id");

        JXPathContext context = JXPathContext.newContext(new Object());
        try {
            pointer.createPath(context);
            fail("Expected JXPathAbstractFactoryException");
        } catch (JXPathAbstractFactoryException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Factory"));
            assertTrue(e.getMessage().contains("@name='id'"));
        }
    }
}
