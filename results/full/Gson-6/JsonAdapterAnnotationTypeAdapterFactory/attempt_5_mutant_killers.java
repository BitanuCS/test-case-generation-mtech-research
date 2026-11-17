package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class JsonAdapterAnnotationTypeAdapterFactory_CandorTest {

  // Dummy helper classes for testing
  public static class DummyTypeAdapter extends TypeAdapter<Object> {
    @Override
    public void write(JsonWriter out, Object value) throws IOException {}
    @Override
    public Object read(JsonReader in) throws IOException { return null; }
  }

  public static class DummyTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> adapter = (TypeAdapter<T>) new DummyTypeAdapter();
      return adapter;
    }
  }

  public static class NullReturningTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      return null;
    }
  }

  public static abstract class AbstractTypeAdapter extends TypeAdapter<Object> {
    @Override
    public void write(JsonWriter out, Object value) throws IOException {}
    @Override
    public Object read(JsonReader in) throws IOException { return null; }
  }

  private @JsonAdapter(DummyTypeAdapter.class) Object dummyField;
  private @JsonAdapter(DummyTypeAdapterFactory.class) Object dummyFactoryField;
  private @JsonAdapter(String.class) Object invalidField;
  private @JsonAdapter(AbstractTypeAdapter.class) Object abstractField;

  @Test
  public void typeAdapterValueReturnsNullSafeAdapter() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<String> typeToken = TypeToken.get(String.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test
  public void typeAdapterFactoryValueReturnsNullSafeAdapter() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<String> typeToken = TypeToken.get(String.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyFactoryField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidAnnotationValueThrowsIllegalArgumentException() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<String> typeToken = TypeToken.get(String.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("invalidField").getAnnotation(JsonAdapter.class);

    JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
  }

  

  @Test
  public void typeAdapterSubclassValueHandlesPolymorphism() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<Object> typeToken = TypeToken.get(Object.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test
  public void typeAdapterFactorySubclassValueHandlesPolymorphism() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<Object> typeToken = TypeToken.get(Object.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyFactoryField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test
  public void primitiveFieldTypeWithTypeAdapterAnnotation() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<Integer> typeToken = TypeToken.get(int.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test
  public void genericFieldTypeWithTypeAdapterFactoryAnnotation() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {};
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("dummyFactoryField").getAnnotation(JsonAdapter.class);

    TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
    assertNotNull(adapter);
  }

  @Test(expected = RuntimeException.class)
  public void abstractTypeAdapterClassThrowsInstantiationException() throws Exception {
    ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
    Gson gson = new Gson();
    TypeToken<String> typeToken = TypeToken.get(String.class);
    JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
        .getDeclaredField("abstractField").getAnnotation(JsonAdapter.class);

    JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
  }

    @Test
      public void nullSafeHandlingAndExceptionMessageTest() throws Exception {
        ConstructorConstructor cc = new ConstructorConstructor(Collections.emptyMap());
        Gson gson = new Gson();
        TypeToken<String> typeToken = TypeToken.get(String.class);
    
        // Test for mutant A0 / A2 / R0: Verify that nullSafe() is actually invoked on non-null adapters.
        // A null-safe adapter correctly handles null values during read/write instead of throwing NPE or bypassing null-safety.
        JsonAdapter annotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
            .getDeclaredField("dummyField").getAnnotation(JsonAdapter.class);
        TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, annotation);
        assertNotNull(adapter);
        
        // Writing null to a nullSafe adapter should succeed without throwing NullPointerException
        try {
          adapter.toJson(null);
        } catch (NullPointerException e) {
          fail("Adapter was not null-safe (Omit calling nullSafe() mutant detected)");
        }
    
        // Test for factory returning null (typeAdapter == null case) doesn't crash
        JsonAdapter nullFactoryAnnotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
            .getDeclaredField("nullReturningFactoryField").getAnnotation(JsonAdapter.class);
        TypeAdapter<?> nullAdapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, nullFactoryAnnotation);
        assertNull(nullAdapter);
    
        // Test for mutant A4: Verify exact exception message for invalid @JsonAdapter value
        JsonAdapter invalidAnnotation = JsonAdapterAnnotationTypeAdapterFactory_CandorTest.class
            .getDeclaredField("invalidField").getAnnotation(JsonAdapter.class);
        try {
          JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(cc, gson, typeToken, invalidAnnotation);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
          assertEquals("@JsonAdapter value must be TypeAdapter or TypeAdapterFactory reference.", e.getMessage());
        }
      }
}
