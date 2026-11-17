package org.apache.commons.jxpath.ri.model.beans;

import java.util.Locale;
import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathAbstractFactoryException;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.junit.Test;

import static org.junit.Assert.*;

public class NullPropertyPointer_CandorTest {

    @Test(expected = UnsupportedOperationException.class)
    public void parentNullPointerUnchangedThrowsBadFactoryException() {
        NullPointer nullParent = new NullPointer(null, new QName("test"));
        NullPropertyPointer pointer = new NullPropertyPointer(nullParent) {
            @Override
            public boolean isAttribute() {
                return false;
            }
        };

        JXPathContext context = JXPathContext.newContext(new Object());
        pointer.createPath(context);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void parentNullPointerUnchangedThrowsBadFactoryExceptionWithObjectValue() {
        NullPointer nullParent = new NullPointer(null, new QName("test"));
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
        NodePointer parentPointer = new NodePointer(null, Locale.getDefault()) {
            @Override
            public QName getName() {
                return new QName("parent");
            }

            @Override
            public NodePointer createPath(JXPathContext context) {
                return this;
            }

            @Override
            public NodePointer createAttribute(JXPathContext context, QName name) {
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
            public void setValue(Object value) {
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
        NodePointer parentPointer = new NodePointer(null, Locale.getDefault()) {
            private Object storedValue;

            @Override
            public QName getName() {
                return new QName("parent");
            }

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
    public void nonPropertyOwnerPointerParentExpansion() {
        NodePointer parentPointer = new NodePointer(null, Locale.getDefault()) {
            @Override
            public QName getName() {
                return new QName("parent");
            }

            @Override
            public NodePointer createPath(JXPathContext context) {
                return this;
            }

            @Override
            public NodePointer createChild(JXPathContext context, QName name, int index) {
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
            public void setValue(Object value) {
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
        NodePointer parentPointer = new NodePointer(null, Locale.getDefault()) {
            @Override
            public QName getName() {
                return new QName("parent");
            }

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
            public void setValue(Object value) {
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
        NullPointer nullParent = new NullPointer(null, new QName("test"));
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
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Cannot create the root object"));
        }
    }

    @Test
    public void badFactoryExceptionMessageFormattingWithNameAttribute() {
        NullPointer nullParent = new NullPointer(null, new QName("test"));
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
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Cannot create the root object"));
        }
    }

    

    

    @Test
        public void propertyOwnerPointerParentExpansionExtractsPropertyPointer() {
            // This test specifically covers the PropertyOwnerPointer branch (lines 127-129 / 147-148)
            // instead of a NullPointer or generic NodePointer.
            // A PropertyOwnerPointer when createPath is called on it typically returns itself (or another pointer),
            // and if it's a PropertyOwnerPointer, `newParent = pop.getPropertyPointer()` must be executed.
            // If mutant A1/A5 is active (checking instanceof NullPointer instead of PropertyOwnerPointer),
            // a PropertyOwnerPointer will NOT trigger getPropertyPointer(), causing createChild to be called
            // on the PropertyOwnerPointer directly instead of its PropertyPointer, which can be distinguished
            // by verifying what createChild receives or mocking/subclassing.
            
            final boolean[] createChildCalledOnPropertyPointer = {false};
    
            PropertyOwnerPointer mockPropertyPointer = new PropertyOwnerPointer(null, true) {
                @Override
                public QName getName() {
                    return new QName("prop");
                }
                @Override
                public NodePointer createChild(JXPathContext context, QName name, int index) {
                    createChildCalledOnPropertyPointer[0] = true;
                    return this;
                }
                @Override
                public int getLength() { return 0; }
                @Override
                public Object getBaseValue() { return null; }
                @Override
                public Object getImmediateNode() { return null; }
                @Override
                public boolean isLeaf() { return false; }
                @Override
                public boolean isCollection() { return false; }
                @Override
                public void setValue(Object value) {}
                @Override
                public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) { return 0; }
            };
    
            PropertyOwnerPointer parentOwnerPointer = new PropertyOwnerPointer(null, true) {
                @Override
                public QName getName() {
                    return new QName("owner");
                }
                @Override
                public NodePointer createPath(JXPathContext context) {
                    return this;
                }
                @Override
                public PropertyPointer getPropertyPointer() {
                    return (PropertyPointer) mockPropertyPointer;
                }
                @Override
                public NodePointer createChild(JXPathContext context, QName name, int index) {
                    // If mutant A1/A5 runs, createChild is called here on parentOwnerPointer instead of mockPropertyPointer
                    fail("createChild should be called on the extracted PropertyPointer, not the PropertyOwnerPointer directly");
                    return this;
                }
                @Override
                public int getLength() { return 0; }
                @Override
                public Object getBaseValue() { return null; }
                @Override
                public Object getImmediateNode() { return null; }
                @Override
                public boolean isLeaf() { return false; }
                @Override
                public boolean isCollection() { return false; }
                @Override
                public void setValue(Object value) {}
                @Override
                public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) { return 0; }
            };
    
            NullPropertyPointer nullPropPointer = new NullPropertyPointer(parentOwnerPointer) {
                @Override
                public boolean isAttribute() {
                    return false;
                }
            };
    
            JXPathContext context = JXPathContext.newContext(new Object());
            NodePointer result = nullPropPointer.createPath(context);
            assertTrue("createChild should be invoked on the PropertyPointer extracted from the PropertyOwnerPointer",
                    createChildCalledOnPropertyPointer[0]);
        }

    @Test
        public void propertyOwnerPointerParentExpansionWithValueExtractsPropertyPointer() {
            // Covers createPath(context, value) for PropertyOwnerPointer branch (mutant A2, etc.)
            final boolean[] createChildCalledOnPropertyPointer = {false};
    
            PropertyOwnerPointer mockPropertyPointer = new PropertyOwnerPointer(null, true) {
                @Override
                public QName getName() {
                    return new QName("prop");
                }
                @Override
                public NodePointer createChild(JXPathContext context, QName name, int index, Object value) {
                    createChildCalledOnPropertyPointer[0] = true;
                    return this;
                }
                @Override
                public int getLength() { return 0; }
                @Override
                public Object getBaseValue() { return null; }
                @Override
                public Object getImmediateNode() { return value; }
                @Override
                public boolean isLeaf() { return false; }
                @Override
                public boolean isCollection() { return false; }
                @Override
                public void setValue(Object value) {}
                @Override
                public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) { return 0; }
            };
    
            PropertyOwnerPointer parentOwnerPointer = new PropertyOwnerPointer(null, true) {
                @Override
                public QName getName() {
                    return new QName("owner");
                }
                @Override
                public NodePointer createPath(JXPathContext context) {
                    return this;
                }
                @Override
                public PropertyPointer getPropertyPointer() {
                    return (PropertyPointer) mockPropertyPointer;
                }
                @Override
                public NodePointer createChild(JXPathContext context, QName name, int index, Object value) {
                    fail("createChild should be called on the extracted PropertyPointer, not the PropertyOwnerPointer directly");
                    return this;
                }
                @Override
                public int getLength() { return 0; }
                @Override
                public Object getBaseValue() { return null; }
                @Override
                public Object getImmediateNode() { return null; }
                @Override
                public boolean isLeaf() { return false; }
                @Override
                public boolean isCollection() { return false; }
                @Override
                public void setValue(Object value) {}
                @Override
                public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) { return 0; }
            };
    
            NullPropertyPointer nullPropPointer = new NullPropertyPointer(parentOwnerPointer) {
                @Override
                public boolean isAttribute() {
                    return false;
                }
            };
    
            JXPathContext context = JXPathContext.newContext(new Object());
            NodePointer result = nullPropPointer.createPath(context, "val");
            assertTrue("createChild with value should be invoked on the PropertyPointer extracted from the PropertyOwnerPointer",
                    createChildCalledOnPropertyPointer[0]);
        }
}
