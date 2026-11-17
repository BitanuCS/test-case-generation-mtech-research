package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class TypeAdapters_CandorTest {

  @Test
  public void createReturnsNullWhenTypeNotAssignable() {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<String> adapter = factory.create(gson, TypeToken.get(String.class));
    assertNull(adapter);
  }

  @Test
  public void createReturnsAdapterWhenTypeIsAssignable() {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Integer> adapter = factory.create(gson, TypeToken.get(Integer.class));
    assertNotNull(adapter);
  }

  @Test
  public void createReturnsAdapterWhenTypeExactMatch() {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Number> adapter = factory.create(gson, TypeToken.get(Number.class));
    assertNotNull(adapter);
  }

  @Test
  public void adapterReadReturnsValidInstanceMatchingRequestedType() throws IOException {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Number> adapter = factory.create(gson, TypeToken.get(Number.class));

    JsonReader reader = new JsonReader(new StringReader("42"));
    Number result = adapter.read(reader);

    assertNotNull(result);
    assertEquals(42, result.intValue());
  }

  @Test(expected = JsonSyntaxException.class)
  public void adapterReadThrowsJsonSyntaxExceptionWhenResultNotInstanceOfRequestedType() throws IOException {
    Gson gson = new Gson();
    // Create a custom type adapter that returns a Double (which is not an Integer)
    TypeAdapter<Number> doubleAdapter = new TypeAdapter<Number>() {
      @Override
      public Number read(JsonReader in) throws IOException {
        in.nextString();
        return Double.valueOf(42.0);
      }
      @Override
      public void write(JsonWriter out, Number value) throws IOException {
        out.value(value);
      }
    };

    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, doubleAdapter);
    // Request Integer.class type token, but the adapter returns a Double
    TypeAdapter<Integer> adapter = factory.create(gson, TypeToken.get(Integer.class));

    JsonReader reader = new JsonReader(new StringReader("42"));
    adapter.read(reader);
  }

  @Test
  public void adapterReadReturnsNullSafelyWithoutTypeCheckWhenResultIsNull() throws IOException {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Integer> adapter = factory.create(gson, TypeToken.get(Integer.class));

    JsonReader reader = new JsonReader(new StringReader("null"));
    Integer result = adapter.read(reader);

    assertNull(result);
  }

  @Test
  public void adapterWriteDelegatesToTypeAdapterCorrectly() throws IOException {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Number> adapter = factory.create(gson, TypeToken.get(Number.class));

    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    adapter.write(jsonWriter, 100);

    assertEquals("100", stringWriter.toString());
  }

  @Test
  public void toStringReturnsExpectedFormat() {
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(java.net.InetAddress.class, TypeAdapters.INET_ADDRESS);
    String str = factory.toString();
    assertEquals("Factory[typeHierarchy=java.net.InetAddress,adapter=" + TypeAdapters.INET_ADDRESS + "]", str);
  }

  @Test
  public void createWithInterfaceHierarchyMatch() {
    Gson gson = new Gson();
    // StringBuilder type adapter is registered for StringBuilder.class, so we match StringBuilder type hierarchy.
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(StringBuilder.class, TypeAdapters.STRING_BUILDER);
    TypeAdapter<StringBuilder> adapter = factory.create(gson, TypeToken.get(StringBuilder.class));
    assertNotNull(adapter);
  }

  @Test
  public void adapterReadSucceedsWhenResultIsExactSubclassInstance() throws IOException {
    Gson gson = new Gson();
    TypeAdapterFactory factory = TypeAdapters.newTypeHierarchyFactory(Number.class, TypeAdapters.NUMBER);
    TypeAdapter<Number> adapter = factory.create(gson, TypeToken.get(Number.class));

    JsonReader reader = new JsonReader(new StringReader("123"));
    Number result = adapter.read(reader);

    assertTrue(result instanceof LazilyParsedNumber);
    assertEquals(123, result.intValue());
  }
}
