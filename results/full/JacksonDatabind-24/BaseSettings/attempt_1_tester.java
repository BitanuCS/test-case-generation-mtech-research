package com.fasterxml.jackson.databind.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public class BaseSettings_CandorTest {

    private BaseSettings createSampleBaseSettings(DateFormat df) {
        ClassIntrospector ci = null;
        AnnotationIntrospector ai = null;
        VisibilityChecker<?> vc = null;
        PropertyNamingStrategy pns = null;
        TypeFactory tf = null;
        TypeResolverBuilder<?> typer = null;
        HandlerInstantiator hi = null;
        Locale locale = Locale.US;
        TimeZone tz = TimeZone.getDefault();
        Base64Variant base64 = null;

        return new BaseSettings(ci, ai, vc, pns, tf, typer, df, hi, locale, tz, base64);
    }

    @Test
    public void withDateFormatSameInstanceReturnsSame() {
        DateFormat df = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df);

        BaseSettings result = settings.withDateFormat(df);

        assertSame(settings, result);
    }

    @Test
    public void withDateFormatDifferentInstanceReturnsNew() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertNotSame(settings, result);
        assertEquals(df2, result.getDateFormat());
    }

    @Test
    public void withDateFormatNull() {
        DateFormat df = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df);

        BaseSettings result = settings.withDateFormat(null);

        assertNotSame(settings, result);
        assertEquals(null, result.getDateFormat());
    }

    @Test
    public void withDateFormatNullToNull() {
        BaseSettings settings = createSampleBaseSettings(null);

        BaseSettings result = settings.withDateFormat(null);

        assertSame(settings, result);
    }

    @Test
    public void withDateFormatStdDateFormat() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(df2, result.getDateFormat());
    }

    @Test
    public void withDateFormatClonedFormat() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(df2, result.getDateFormat());
    }

    @Test
    public void withDateFormatImmutabilityOriginalUnchanged() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        settings.withDateFormat(df2);

        assertEquals(df1, settings.getDateFormat());
    }

    @Test
    public void withDateFormatPreservesClassIntrospector() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getClassIntrospector(), result.getClassIntrospector());
    }

    @Test
    public void withDateFormatPreservesAnnotationIntrospector() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getAnnotationIntrospector(), result.getAnnotationIntrospector());
    }

    @Test
    public void withDateFormatPreservesVisibilityChecker() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getVisibilityChecker(), result.getVisibilityChecker());
    }

    @Test
    public void withDateFormatPreservesPropertyNamingStrategy() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getPropertyNamingStrategy(), result.getPropertyNamingStrategy());
    }

    @Test
    public void withDateFormatPreservesTypeFactory() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getTypeFactory(), result.getTypeFactory());
    }

    @Test
    public void withDateFormatPreservesTypeResolverBuilder() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getTypeResolverBuilder(), result.getTypeResolverBuilder());
    }

    @Test
    public void withDateFormatPreservesHandlerInstantiator() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getHandlerInstantiator(), result.getHandlerInstantiator());
    }

    @Test
    public void withDateFormatPreservesLocale() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getLocale(), result.getLocale());
    }

    @Test
    public void withDateFormatPreservesTimeZone() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getTimeZone(), result.getTimeZone());
    }

    @Test
    public void withDateFormatPreservesBase64Variant() {
        DateFormat df1 = new StdDateFormat();
        DateFormat df2 = new StdDateFormat();
        BaseSettings settings = createSampleBaseSettings(df1);

        BaseSettings result = settings.withDateFormat(df2);

        assertEquals(settings.getBase64Variant(), result.getBase64Variant());
    }
}
