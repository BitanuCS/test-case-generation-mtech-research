package org.apache.commons.cli2.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.junit.Test;

public class PatternBuilder_CandorTest {

    private void invokeCreateOption(PatternBuilder builder, char type, boolean required, char opt) throws Exception {
        Method method = PatternBuilder.class.getDeclaredMethod("createOption", char.class, boolean.class, char.class);
        method.setAccessible(true);
        method.invoke(builder, type, required, opt);
    }

    @Test
    public void createSingleOption() throws Exception {
        GroupBuilder gb = new GroupBuilder();
        DefaultOptionBuilder ob = new DefaultOptionBuilder();
        ArgumentBuilder ab = new ArgumentBuilder();
        PatternBuilder pb = new PatternBuilder(gb, ob, ab);

        invokeCreateOption(pb, ' ', false, 'a');
        Option option = pb.create();

        assertNotNull(option);
        // Since options.size() == 1, it should return the option directly (not a Group)
        assertEquals("-a", option.getPreferredName());
    }

    @Test
    public void createMultipleOptions() throws Exception {
        GroupBuilder gb = new GroupBuilder();
        DefaultOptionBuilder ob = new DefaultOptionBuilder();
        ArgumentBuilder ab = new ArgumentBuilder();
        PatternBuilder pb = new PatternBuilder(gb, ob, ab);

        invokeCreateOption(pb, ' ', false, 'a');
        invokeCreateOption(pb, ' ', false, 'b');
        Option option = pb.create();

        assertNotNull(option);
        // Since options.size() > 1, it should wrap them in a Group via gbuilder
        assertTrue(option instanceof Group);
    }

    @Test
    public void createResetsStateAfterCreation() throws Exception {
        GroupBuilder gb = new GroupBuilder();
        DefaultOptionBuilder ob = new DefaultOptionBuilder();
        ArgumentBuilder ab = new ArgumentBuilder();
        PatternBuilder pb = new PatternBuilder(gb, ob, ab);

        invokeCreateOption(pb, ' ', false, 'a');
        Option first = pb.create();
        assertNotNull(first);

        // After create(), options set is cleared. Calling create() again would try to create empty or fail/handle empty,
        // but here options is empty so calling create() will try to take iterator from empty set (which fails or returns null/empty group depending on implementation).
        // Specifically, options.size() == 1 is false (it's 0), so it goes to else branch: gbuilder.reset(), empty loop, gbuilder.create().
        Option second = pb.create();
        assertNotNull(second);
    }

    @Test
    public void resetClearsOptions() throws Exception {
        GroupBuilder gb = new GroupBuilder();
        DefaultOptionBuilder ob = new DefaultOptionBuilder();
        ArgumentBuilder ab = new ArgumentBuilder();
        PatternBuilder pb = new PatternBuilder(gb, ob, ab);

        invokeCreateOption(pb, ' ', false, 'a');
        PatternBuilder returned = pb.reset();
        assertSame(pb, returned);
        
        // After reset, calling create() with empty options
        Option option = pb.create();
        assertNotNull(option);
    }

    @Test
    public void resetOnEmptyBuilder() {
        PatternBuilder pb = new PatternBuilder();
        PatternBuilder returned = pb.reset();
        assertSame(pb, returned);
    }

    @Test
    public void createOptionWithoutArgument() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, ' ', false, 'a');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-a", option.getPreferredName());
    }

    @Test
    public void createOptionWithRequiredArgument() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '%', true, 'b');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-b", option.getPreferredName());
    }

    @Test
    public void createOptionWithStarTypeArgument() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '*', false, 'c');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-c", option.getPreferredName());
    }

    @Test
    public void createOptionWithClassValidatorTypeAt() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '@', false, 'd');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-d", option.getPreferredName());
    }

    @Test
    public void createOptionWithClassValidatorTypePlus() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '+', false, 'e');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-e", option.getPreferredName());
    }

    @Test
    public void createOptionWithDateValidator() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '#', false, 'f');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-f", option.getPreferredName());
    }

    @Test
    public void createOptionWithFileValidatorExisting() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '<', false, 'g');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-g", option.getPreferredName());
    }

    @Test
    public void createOptionWithFileValidatorGreaterThan() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '>', false, 'h');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-h", option.getPreferredName());
    }

    @Test
    public void createOptionWithUrlValidator() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '/', false, 'i');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-i", option.getPreferredName());
    }

    private static void assertTrue(boolean condition) {
        org.junit.Assert.assertTrue(condition);
    }
}
