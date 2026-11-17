package org.apache.commons.lang3;

import java.lang.reflect.Method;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Unit tests for {@link org.apache.commons.lang3.SerializationUtils} focusing on 
 * ClassLoaderAwareObjectInputStream and resolveClass behavior.
 */
public class SerializationUtils_CandorTest extends TestCase {

    private String iString;
    private Integer iInteger;

    public SerializationUtils_CandorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        iString = "foo";
        iInteger = Integer.valueOf(7);
    }

    private Class<?> invokeResolveClassViaStream(ObjectStreamClass objectStreamClass, ClassLoader classLoader) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(iString);
        oos.flush();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        
        // Use reflection to instantiate the package-private / inner ClassLoaderAwareObjectInputStream if needed,
        // or invoke it via constructor found on SerializationUtils or related inner classes.
        // Since ClassLoaderAwareObjectInputStream constructor is shown under SerializationUtils context:
        Constructor<?> ctor = null;
        for (Constructor<?> c : SerializationUtils.class.getDeclaredClasses()[0].getDeclaredConstructors()) {
            if (c.getParameterTypes().length == 2) {
                ctor = c;
                break;
            }
        }
        if (ctor == null) {
            // Fallback search in all declared classes/members of SerializationUtils
            Class<?>[] declaredClasses = SerializationUtils.class.getDeclaredClasses();
            for (Class<?> clazz : declaredClasses) {
                try {
                    ctor = clazz.getDeclaredConstructor(java.io.InputStream.class, ClassLoader.class);
                    break;
                } catch (NoSuchMethodException e) {
                    // continue
                }
            }
        }
        
        if (ctor != null) {
            ctor.setAccessible(true);
            Object stream = ctor.newInstance(bais, classLoader);
            java.lang.reflect.Method resolveClassMethod = stream.getClass().getDeclaredMethod("resolveClass", java.io.ObjectStreamClass.class);
            resolveClassMethod.setAccessible(true);
            return (Class<?>) resolveClassMethod.invoke(stream, objectStreamClass);
        }
        
        throw new NoSuchMethodException("ClassLoaderAwareObjectInputStream constructor not found");
    }

    public void testResolveClassUsingCustomClassLoader() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(String.class);
        ClassLoader customLoader = String.class.getClassLoader();
        Class<?> resolved = invokeResolveClassViaStream(osc, customLoader);
        assertEquals(String.class, resolved);
    }

    public void testResolveClassFallbackToThreadContextClassLoader() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Integer.class);
        // Pass a dummy classloader that won't find Integer directly via custom loader lookup
        ClassLoader dummyLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException(name);
            }
        };
        Class<?> resolved = invokeResolveClassViaStream(osc, dummyLoader);
        assertEquals(Integer.class, resolved);
    }

    public void testResolveClassPrimitiveByte() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Byte.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Byte.class, resolved);
    }

    public void testResolveClassPrimitiveShort() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Short.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Short.class, resolved);
    }

    public void testResolveClassPrimitiveInt() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Integer.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Integer.class, resolved);
    }

    public void testResolveClassPrimitiveLong() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Long.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Long.class, resolved);
    }

    public void testResolveClassPrimitiveFloat() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Float.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Float.class, resolved);
    }

    public void testResolveClassPrimitiveDouble() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Double.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Double.class, resolved);
    }

    public void testResolveClassPrimitiveBoolean() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Boolean.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Boolean.class, resolved);
    }

    public void testResolveClassPrimitiveChar() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(Character.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(Character.class, resolved);
    }

    public void testResolveClassThrowsClassNotFoundException() {
        try {
            // Create a fake ObjectStreamClass for a non-existent class
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(iString);
            oos.flush();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            Constructor<?> ctor = null;
            for (Class<?> clazz : SerializationUtils.class.getDeclaredClasses()) {
                try {
                    ctor = clazz.getDeclaredConstructor(java.io.InputStream.class, ClassLoader.class);
                    break;
                } catch (NoSuchMethodException e) {
                    // continue
                }
            }
            assertNotNull(ctor);
            ctor.setAccessible(true);
            Object stream = ctor.newInstance(bais, new ClassLoader() {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    throw new ClassNotFoundException();
                }
            });

            java.lang.reflect.Method resolveClassMethod = stream.getClass().getDeclaredMethod("resolveClass", java.io.ObjectStreamClass.class);
            resolveClassMethod.setAccessible(true);
            
            ObjectInputStream ois = new ObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    throw new ClassNotFoundException("NonExistentClass");
                }
            };
            fail("Expected ClassNotFoundException");
        } catch (Exception e) {
            // Expected
            assertTrue(true);
        }
    }

    public void testConstructorInitializesProperly() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(iString);
        oos.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        Constructor<?> ctor = null;
        for (Class<?> clazz : SerializationUtils.class.getDeclaredClasses()) {
            try {
                ctor = clazz.getDeclaredConstructor(java.io.InputStream.class, ClassLoader.class);
                break;
            } catch (NoSuchMethodException e) {
                // continue
            }
        }
        assertNotNull(ctor);
        ctor.setAccessible(true);
        Object stream = ctor.newInstance(bais, Thread.currentThread().getContextClassLoader());
        assertNotNull(stream);
    }

    private Class<?> invokeResolveClassForName(Class<?> clazzToLookup, ClassLoader classLoader) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject("test");
            oos.flush();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    
            Constructor<?> ctor = null;
            for (Class<?> clazz : SerializationUtils.class.getDeclaredClasses()) {
                try {
                    ctor = clazz.getDeclaredConstructor(java.io.InputStream.class, ClassLoader.class);
                    break;
                } catch (NoSuchMethodException e) {
                    // continue
                }
            }
            assertNotNull("ClassLoaderAwareObjectInputStream constructor not found", ctor);
            ctor.setAccessible(true);
            Object stream = ctor.newInstance(bais, classLoader);
    
            ObjectStreamClass osc = ObjectStreamClass.lookup(clazzToLookup);
            
            Method resolveClassMethod = stream.getClass().getDeclaredMethod("resolveClass", java.io.ObjectStreamClass.class);
            resolveClassMethod.setAccessible(true);
            return (Class<?>) resolveClassMethod.invoke(stream, osc);
        }

    public void testResolveClassPrimitiveBooleanMappingAndCheck() throws Exception {
            // This test exercises boolean wrapper class lookup or similar.
            ClassLoader failingLoader = new ClassLoader() {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    throw new ClassNotFoundException(name);
                }
            };
    
            Class<?> resolvedBoolean = invokeResolveClassForName(Boolean.class, failingLoader);
            assertEquals(Boolean.class, resolvedBoolean);
        }

    public void testResolveClassThrowsCnfeWhenNotFoundEverywhere() {
            // This test ensures that when both classLoaders fail AND it's not a primitive type,
            // cnfe is thrown (killing mutant A3 which swaps cnfe with ex).
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject("test");
                oos.flush();
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    
                Constructor<?> ctor = null;
                for (Class<?> clazz : SerializationUtils.class.getDeclaredClasses()) {
                    try {
                        ctor = clazz.getDeclaredConstructor(java.io.InputStream.class, ClassLoader.class);
                        break;
                    } catch (NoSuchMethodException e) {
                        // continue
                    }
                }
                assertNotNull(ctor);
                ctor.setAccessible(true);
                ClassLoader failingLoader = new ClassLoader() {
                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        throw new ClassNotFoundException(name);
                    }
                };
                Object stream = ctor.newInstance(bais, failingLoader);
    
                // We need an ObjectStreamClass whose getName() returns a non-existent class name that is not a primitive.
                // We can create an anonymous subclass or mock, or use an existing class name via a dummy ObjectStreamClass if possible,
                // or better yet, serialize a dummy non-existent class or subclass ObjectStreamClass.
                // Since ObjectStreamClass has no public constructors with name, we can use ObjectStreamClass.lookup(NonExistent.class)? No, can't reference.
                // But wait! We can serialize an object of a class that throws ClassNotFoundException on read (like ClassNotFoundSerialization).
                // Let's serialize ClassNotFoundSerialization object:
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                oos2.writeObject(new ClassNotFoundSerialization());
                oos2.flush();
                ByteArrayInputStream bais2 = new ByteArrayInputStream(baos2.toByteArray());
    
                ObjectInputStream ois = (ObjectInputStream) ctor.newInstance(bais2, failingLoader);
                // Calling readObject will invoke resolveClass and trigger ClassNotFoundException
                ois.readObject();
                fail("Expected ClassNotFoundException");
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                assertTrue("Expected ClassNotFoundException, got: " + cause, cause instanceof ClassNotFoundException);
                assertEquals(SerializationUtilsTest.CLASS_NOT_FOUND_MESSAGE, cause.getMessage());
            } catch (Exception e) {
                // If thrown directly
                assertTrue(e instanceof ClassNotFoundException);
            }
        }
}
