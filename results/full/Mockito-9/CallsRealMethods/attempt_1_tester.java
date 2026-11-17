package org.mockito.internal.stubbing.answers;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class CallsRealMethods_CandorTest {

    // Helper interface / classes for reflection method acquisition
    private interface SampleInterface {
        void interfaceMethod();
    }

    private abstract static class SampleAbstractClass {
        public abstract void abstractMethod();
        public void concreteMethod() {}
    }

    private static class SampleConcreteClass {
        public void concreteMethod() {}
    }

    @Test
    public void callsRealMethodWhenMethodIsConcrete() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleConcreteClass.class.getMethod("concreteMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return null; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { return "realMethodResult"; }
        };

        Object result = answer.answer(invocation);
        assertEquals("realMethodResult", result);
    }

    @Test
    public void delegatesToGloballyConfiguredAnswerWhenMethodIsAbstract() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleAbstractClass.class.getMethod("abstractMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return new SampleAbstractClass() {
                @Override public void abstractMethod() {}
            }; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { 
                fail("Should not call real method on abstract method");
                return null;
            }
        };

        // GloballyConfiguredAnswer returns null by default for void/object methods
        Object result = answer.answer(invocation);
        assertNull(result);
    }

    @Test(expected = RuntimeException.class)
    public void propagatesThrowableWhenCallRealMethodThrowsException() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleConcreteClass.class.getMethod("concreteMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return null; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { throw new RuntimeException("fail"); }
        };

        answer.answer(invocation);
    }

    @Test(expected = IOException.class)
    public void propagatesCheckedExceptionWhenCallRealMethodThrowsCheckedException() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleConcreteClass.class.getMethod("concreteMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return null; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { throw new IOException("IO error"); }
        };

        answer.answer(invocation);
    }

    @Test(expected = NullPointerException.class)
    public void handlesNullInvocationSafely() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        answer.answer(null);
    }

    @Test
    public void handlesAbstractMethodWithGloballyConfiguredAnswerReturningNull() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleAbstractClass.class.getMethod("abstractMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return new SampleAbstractClass() { @Override public void abstractMethod() {} }; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { return "shouldNotBeCalled"; }
        };

        Object result = answer.answer(invocation);
        assertNull(result);
    }

    @Test
    public void handlesInterfaceMethodWhichIsImplicitlyAbstract() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        Method method = SampleInterface.class.getMethod("interfaceMethod");
        assertTrue(Modifier.isAbstract(method.getModifiers()));

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return (SampleInterface) () -> {}; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { return "shouldNotBeCalled"; }
        };

        Object result = answer.answer(invocation);
        assertNull(result);
    }

    @Test
    public void handlesStaticAbstractMethodIfPossible() throws Throwable {
        CallsRealMethods answer = new CallsRealMethods();
        // Construct or fetch a method with abstract modifier (interfaces methods are abstract)
        Method method = SampleInterface.class.getMethod("interfaceMethod");

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return null; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { return "shouldNotBeCalled"; }
        };

        Object result = answer.answer(invocation);
        assertNull(result);
    }

    @Test
    public void serializabilityContractMaintained() throws Exception {
        CallsRealMethods answer = new CallsRealMethods();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(answer);
        }

        CallsRealMethods deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (CallsRealMethods) ois.readObject();
        }

        assertNotNull(deserialized);

        Method method = SampleConcreteClass.class.getMethod("concreteMethod");
        InvocationOnMock invocation = new InvocationOnMock() {
            @Override public Object getMock() { return null; }
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public <T> T getArgument(int index) { return null; }
            @Override public <T> T getArgument(int index, Class<T> clazz) { return null; }
            @Override public Object callRealMethod() throws Throwable { return "deserializedSuccess"; }
        };

        Object result = deserialized.answer(invocation);
        assertEquals("deserializedSuccess", result);
    }

    @Test
    public void serialVersionUIDIsRespected() throws Exception {
        java.io.ObjectStreamClass osc = java.io.ObjectStreamClass.lookup(CallsRealMethods.class);
        assertNotNull(osc);
        assertEquals(9057165148930624087L, osc.getSerialVersionUID());
    }
}
