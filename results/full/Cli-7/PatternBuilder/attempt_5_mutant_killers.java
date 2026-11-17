package org.apache.commons.cli2.builder;

import org.apache.commons.cli2.Argument;

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

    @Test
    public void createOptionWithRequiredArgumentBasic() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '%', true, 'r');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-r", option.getPreferredName());
    }

    @Test
    public void createOptionWithNonStarTypeBasic() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '%', false, 'm');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-m", option.getPreferredName());
    }

    @Test
    public void createOptionWithStarTypeBasic() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '*', false, 's');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-s", option.getPreferredName());
    }

    @Test
    public void testRequiredParameterPassedCorrectlyMutant() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, ' ', true, 'q');
        Option option = pb.create();
        assertNotNull(option);
        org.junit.Assert.assertTrue("Option should be required when required=true is passed", option.isRequired());
    }

    @Test
    public void createOptionWithNumberValidatorBasic() throws Exception {
        PatternBuilder pb = new PatternBuilder();
        invokeCreateOption(pb, '%', false, 'x');
        Option option = pb.create();
        assertNotNull(option);
        assertEquals("-x", option.getPreferredName());
    }

    private void invokeCreateOption2(PatternBuilder builder, char type, boolean required, char opt) throws Exception {
            Method method = PatternBuilder.class.getDeclaredMethod("createOption", char.class, boolean.class, char.class);
            method.setAccessible(true);
            method.invoke(builder, type, required, opt);
        }

    @Test
        public void testArgumentMinimumWhenRequiredIsTrue() throws Exception {
            PatternBuilder pb = new PatternBuilder();
            // type = '%' (requires an argument), required = true
            invokeCreateOption(pb, '%', true, 't');
            Option option = pb.create();
            assertNotNull(option);
            
            // Retrieve the argument and check its minimum. 
            // If the mutant changes withMinimum(1) to withMinimum(0), minimum will be 0 instead of 1.
            Argument argument = option.getArgument();
            assertNotNull(argument);
            assertEquals(1, argument.getMinimum());
        }

    @Test
        public void testArgumentMaximumForNonStarType() throws Exception {
            PatternBuilder pb = new PatternBuilder();
            // type = '%' (not '*'), should have maximum set to 1.
            // If mutant changes type != '*' to type == '*', maximum won't be set to 1 (it will be unbounded or default).
            invokeCreateOption(pb, '%', false, 'u');
            Option option = pb.create();
            assertNotNull(option);
    
            Argument argument = option.getArgument();
            assertNotNull(argument);
            assertEquals(1, argument.getMaximum());
        }

    @Test
        public void testArgumentMaximumForStarType() throws Exception {
            PatternBuilder pb = new PatternBuilder();
            // type = '*' (star type), should NOT have maximum set to 1 (should be unlimited/default, e.g. Integer.MAX_VALUE or > 1).
            // If mutant changes type != '*' to type == '*', it incorrectly applies maximum(1) to '*'.
            invokeCreateOption(pb, '*', false, 'v');
            Option option = pb.create();
            assertNotNull(option);
    
            Argument argument = option.getArgument();
            assertNotNull(argument);
            // '*' allows multiple arguments, so maximum should be greater than 1 (typically Integer.MAX_VALUE)
            org.junit.Assert.assertTrue("Maximum for '*' argument should be greater than 1", argument.getMaximum() > 1);
        }
}
