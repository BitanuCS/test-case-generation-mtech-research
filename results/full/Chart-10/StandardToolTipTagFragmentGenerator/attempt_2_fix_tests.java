package org.jfree.chart.imagemap;

import org.junit.Test;
import static org.junit.Assert.*;

public class StandardToolTipTagFragmentGenerator_CandorTest {

    @Test
    public void standardToolTipText() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("Sales: $1000");
        assertEquals(" title=\"Sales: $1000\" alt=\"\"", result);
    }

    @Test
    public void emptyToolTipText() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("");
        assertEquals(" title=\"\" alt=\"\"", result);
    }

    @Test
    public void specialCharactersHtmlEscape() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("A & B < C > D \"E\"");
        String expectedEscaped = ImageMapUtilities.htmlEscape("A & B < C > D \"E\"");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullToolTipText() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        generator.generateToolTipFragment(null);
    }

    @Test
    public void toolTipTextWithSingleQuotes() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("It's a test");
        String expectedEscaped = ImageMapUtilities.htmlEscape("It's a test");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithUnicode() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("\u00dcbung & Pr\u00fcfung");
        String expectedEscaped = ImageMapUtilities.htmlEscape("\u00dcbung & Pr\u00fcfung");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithNewlines() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("Line 1\nLine 2");
        String expectedEscaped = ImageMapUtilities.htmlEscape("Line 1\nLine 2");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithTabs() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("Column1\tColumn2");
        String expectedEscaped = ImageMapUtilities.htmlEscape("Column1\tColumn2");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void veryLongToolTipText() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append('a');
        }
        String longText = sb.toString();
        String result = generator.generateToolTipFragment(longText);
        String expectedEscaped = ImageMapUtilities.htmlEscape(longText);
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithOnlySpaces() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("   ");
        assertEquals(" title=\"   \" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithHtmlTags() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("<b>Bold</b>");
        String expectedEscaped = ImageMapUtilities.htmlEscape("<b>Bold</b>");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }

    @Test
    public void toolTipTextWithPercentSigns() {
        StandardToolTipTagFragmentGenerator generator = new StandardToolTipTagFragmentGenerator();
        String result = generator.generateToolTipFragment("100% complete");
        String expectedEscaped = ImageMapUtilities.htmlEscape("100% complete");
        assertEquals(" title=\"" + expectedEscaped + "\" alt=\"\"", result);
    }
}
