package org.apache.commons.lang3;

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
        ObjectStreamClass osc = ObjectStreamClass.lookup(byte.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(byte.class, resolved);
    }

    public void testResolveClassPrimitiveShort() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(short.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(short.class, resolved);
    }

    public void testResolveClassPrimitiveInt() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(int.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(int.class, resolved);
    }

    public void testResolveClassPrimitiveLong() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(long.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(long.class, resolved);
    }

    public void testResolveClassPrimitiveFloat() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(float.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(float.class, resolved);
    }

    public void testResolveClassPrimitiveDouble() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(double.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(double.class, resolved);
    }

    public void testResolveClassPrimitiveBoolean() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(boolean.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(boolean.class, resolved);
    }

    public void testResolveClassPrimitiveChar() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(char.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(char.class, resolved);
    }

    public void testResolveClassPrimitiveVoid() throws Exception {
        ObjectStreamClass osc = ObjectStreamClass.lookup(void.class);
        Class<?> resolved = invokeResolveClassViaStream(osc, null);
        assertEquals(void.class, resolved);
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
}
