package com.fasterxml.jackson.databind.deser.impl;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.AnnotatedElement;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;

public class CreatorCollector_CandorTest {

    // Helper subclass to expose protected verifyNonDup method and state fields for testing.
    private static class TestableCreatorCollector extends CreatorCollector {
        public TestableCreatorCollector(BeanDescription beanDesc, boolean canFixAccess) {
            super(beanDesc, canFixAccess);
        }

        public void callVerifyNonDup(AnnotatedWithParams newOne, int typeIndex, boolean explicit) {
            verifyNonDup(newOne, typeIndex, explicit);
        }

        public AnnotatedWithParams getCreator(int index) {
            return _creators[index];
        }

        public int getExplicitCreators() {
            return _explicitCreators;
        }

        public boolean hasNonDefaultCreator() {
            return _hasNonDefaultCreator;
        }
    }

    // A minimal concrete subclass of AnnotatedWithParams to allow testing raw parameter types and class equality.
    private static class DummyAnnotatedWithParams extends AnnotatedWithParams {
        private final Class<?> _paramType;

        public DummyAnnotatedWithParams(Class<?> paramType) {
            super(null, null, null);
            _paramType = paramType;
        }

        @Override
        public Class<?> getRawParameterType(int index) {
            return _paramType;
        }

        @Override
        public java.lang.reflect.Type getGenericParameterType(int index) {
            return _paramType;
        }

        @Override
        public int getParameterCount() {
            return 1;
        }

        @Override
        public com.fasterxml.jackson.databind.JavaType getParameterType(int index) {
            return null;
        }

        @Override
        public AnnotatedElement getAnnotated() {
            return null;
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public com.fasterxml.jackson.databind.JavaType getType() {
            return null;
        }

        @Override
        public AnnotatedWithParams withAnnotations(com.fasterxml.jackson.databind.introspect.AnnotationMap ann) {
            return this;
        }

        @Override
        public Object call() throws Exception {
            return null;
        }

        @Override
        public Object call(Object[] args) throws Exception {
            return null;
        }

        @Override
        public Object call1(Object arg) throws Exception {
            return null;
        }
    }

    // Another subclass of AnnotatedWithParams with a different runtime class for class equality tests.
    private static class AnotherDummyAnnotatedWithParams extends DummyAnnotatedWithParams {
        public AnotherDummyAnnotatedWithParams(Class<?> paramType) {
            super(paramType);
        }
    }

    @Test
    public void firstCreatorAddedNoExisting() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams newOne = new DummyAnnotatedWithParams(String.class);

        collector.callVerifyNonDup(newOne, 1, false);

        assertSame(newOne, collector.getCreator(1));
        assertTrue(collector.hasNonDefaultCreator());
        assertEquals(0, collector.getExplicitCreators());
    }

    @Test
    public void firstExplicitCreatorAddedNoExisting() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams newOne = new DummyAnnotatedWithParams(String.class);

        collector.callVerifyNonDup(newOne, 1, true);

        assertSame(newOne, collector.getCreator(1));
        assertTrue(collector.hasNonDefaultCreator());
        assertEquals(1 << 1, collector.getExplicitCreators());
    }

    @Test
    public void existingExplicitNewImplicitIgnoresNew() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams explicitCreator = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicitCreator, 1, true);

        AnnotatedWithParams newImplicit = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(newImplicit, 1, false);

        assertSame(explicitCreator, collector.getCreator(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void existingExplicitNewExplicitSameClassAndRawParamThrowsException() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams explicit1 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicit1, 1, true);

        AnnotatedWithParams explicit2 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicit2, 1, true);
    }

    @Test
    public void existingExplicitNewExplicitSameClassNewParamMoreGenericKeepsOld() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        // String is more specific than Object. old = String, new = Object.
        // newType.isAssignableFrom(oldType) -> Object.isAssignableFrom(String) is true.
        AnnotatedWithParams explicit1 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicit1, 1, true);

        AnnotatedWithParams explicit2 = new DummyAnnotatedWithParams(Object.class);
        collector.callVerifyNonDup(explicit2, 1, true);

        assertSame(explicit1, collector.getCreator(1));
    }

    @Test
    public void existingExplicitNewExplicitSameClassNewParamMoreSpecificReplacesOld() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        // old = Object, new = String.
        // Object.isAssignableFrom(String) is false.
        AnnotatedWithParams explicit1 = new DummyAnnotatedWithParams(Object.class);
        collector.callVerifyNonDup(explicit1, 1, true);

        AnnotatedWithParams explicit2 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicit2, 1, true);

        assertSame(explicit2, collector.getCreator(1));
    }

    @Test
    public void existingImplicitNewExplicitOverridesOld() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams implicit1 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(implicit1, 1, false);

        AnnotatedWithParams explicit2 = new DummyAnnotatedWithParams(String.class);
        collector.callVerifyNonDup(explicit2, 1, true);

        assertSame(explicit2, collector.getCreator(1));
        assertEquals(1 << 1, collector.getExplicitCreators());
    }

    @Test(expected = IllegalArgumentException.class)
    public void existingImplicitNewImplicitVerifyTrueSameClassSameTypeThrowsException() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams implicit1 = new DummyAnnotatedWithParams(Integer.class);
        collector.callVerifyNonDup(implicit1, 2, false);

        AnnotatedWithParams implicit2 = new DummyAnnotatedWithParams(Integer.class);
        collector.callVerifyNonDup(implicit2, 2, false);
    }

    @Test
    public void existingImplicitNewImplicitDifferentClassSkipsVerification() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams implicit1 = new DummyAnnotatedWithParams(Integer.class);
        collector.callVerifyNonDup(implicit1, 1, false);

        AnnotatedWithParams implicit2 = new AnotherDummyAnnotatedWithParams(Integer.class);
        collector.callVerifyNonDup(implicit2, 1, false);

        assertSame(implicit2, collector.getCreator(1));
    }

    @Test
    public void typeIndexBoundaryZeroDefaultCreator() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams defaultCreator = new DummyAnnotatedWithParams(void.class);

        collector.callVerifyNonDup(defaultCreator, 0, true);

        assertSame(defaultCreator, collector.getCreator(0));
        assertTrue(collector.hasNonDefaultCreator());
        assertEquals(1 << 0, collector.getExplicitCreators());
    }

    @Test
    public void typeIndexBoundaryMaxPropertyCreator() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, false);
        AnnotatedWithParams propCreator = new DummyAnnotatedWithParams(Object[].class);

        collector.callVerifyNonDup(propCreator, 7, true);

        assertSame(propCreator, collector.getCreator(7));
        assertTrue(collector.hasNonDefaultCreator());
        assertEquals(1 << 7, collector.getExplicitCreators());
    }

    @Test
    public void fixAccessCalledWhenCanFixAccessTrue() {
        TestableCreatorCollector collector = new TestableCreatorCollector(null, true);
        AnnotatedWithParams newOne = new DummyAnnotatedWithParams(String.class);

        collector.callVerifyNonDup(newOne, 1, false);

        assertSame(newOne, collector.getCreator(1));
    }
}
