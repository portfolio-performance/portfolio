package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.junit.Test;

import com.google.common.base.Strings;

@SuppressWarnings("nls")
public class TextUtilTest
{

    @Test
    public void testStripNonNumberCharacters()
    {
        assertThat(TextUtil.stripNonNumberCharacters("+ 123,34 x"), is("123,34"));
        assertThat(TextUtil.stripNonNumberCharacters("abcd"), is(""));
        assertThat(TextUtil.stripNonNumberCharacters(",123"), is(",123"));
        assertThat(TextUtil.stripNonNumberCharacters("-1,23"), is("-1,23"));
        assertThat(TextUtil.stripNonNumberCharacters("+1,23"), is("1,23"));
    }

    @Test
    public void testWordwrap()
    {
        String text = Strings.repeat("t ", 40) + "(test)";
        assertThat(TextUtil.wordwrap(text), is(endsWith("\n(test)")));

        // check if words longer than 80 characters are kept
        text = Strings.repeat("1234567890", 10) + " (test)";
        assertThat(TextUtil.wordwrap(text),
                        is(Strings.repeat("1234567890", 8) + "\n" + Strings.repeat("1234567890", 2) + " (test)"));

    }

    @Test
    public void testWordwrapForTrailingSpaces()
    {
        // check breaking a long text - line should have no trailing spaces
        var text = "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        assertThat(TextUtil.wordwrap(text), is("""
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor
                        incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
                        exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute
                        iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
                        pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui
                        officia deserunt mollit anim id est laborum."""));
    }

    @Test
    public void testWordwrapWithSingleLineBreak()
    {
        var text = """
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua.
                        Ut enim ad minim veniam, quis nostrud""";
        assertThat(TextUtil.wordwrap(text), is("""
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor
                        incidunt ut labore et dolore magna aliqua.
                        Ut enim ad minim veniam, quis nostrud"""));

    }

    @Test
    public void testWordwrapWithMultipleLineBreaks()
    {
        // check that multiple line breaks are kept
        var text = """
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua.

                        Ut enim ad minim veniam, quis nostrud""";
        assertThat(TextUtil.wordwrap(text), is("""
                        Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor
                        incidunt ut labore et dolore magna aliqua.

                        Ut enim ad minim veniam, quis nostrud"""));

    }

    @Test
    public void testWordwrapWithAmpersand()
    {
        // check that the & is doubled
        var text = "Lorem ipsum dolor sit amet & consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua.";
        assertThat(TextUtil.wordwrap(text), is("""
                        Lorem ipsum dolor sit amet && consectetur adipisici elit, sed eiusmod tempor
                        incidunt ut labore et dolore magna aliqua."""));

    }

    @Test
    public void testWordwrapWithEmptyLinesAtTheEnd()
    {
        var text = "Lorem ipsum dolor\n\n";
        assertThat(TextUtil.wordwrap(text), is("Lorem ipsum dolor\n\n"));
    }

    @Test
    public void testStripCallbacksJsonObjects()
    {
        String json = "{\"name\"=\"value\"}";

        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("callback(" + json + ");"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback(null), is(nullValue()));
        assertThat(TextUtil.stripJavaScriptCallback("something"), is("something"));
        assertThat(TextUtil.stripJavaScriptCallback("}something{"), is("}something{"));

        json = Strings.repeat("texttext", 100);
        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
    }

    @Test
    public void testStripCallbacksJsonArrays()
    {
        String json = "[{\"name\"=\"value\"}]";

        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("callback(" + json + ");"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("angular.callbacks._u(" + json + ")"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("sd" + json), is("sd" + json));
        assertThat(TextUtil.stripJavaScriptCallback("]something["), is("]something["));
    }

    @Test
    public void testSanitizeFilename()
    {
        assertThat(TextUtil.sanitizeFilename("?a\\b/c:d|e<f>g//h*i"), is("_a_b_c_d_e_f_g_h_i"));
        assertThat(TextUtil.sanitizeFilename("a    b"), is("a_b"));
        assertThat(TextUtil.sanitizeFilename("äöüÄÖÜß"), is("äöüÄÖÜß"));
        assertThat(TextUtil.sanitizeFilename("Акти"), is("Акти"));
    }

    @Test
    public void testStripBlanks()
    {
        assertThat(TextUtil.stripBlanks("a b c"), is("abc"));
        assertThat(TextUtil.stripBlanks(" a  b  c "), is("abc"));
    }

    @Test
    public void testReplaceMultipleBlanksBetween()
    {
        assertThat(TextUtil.replaceMultipleBlanks("a     b       c"), is("a b c"));
        assertThat(TextUtil.replaceMultipleBlanks("   a      b     c    "), is(" a b c "));
    }

    @Test
    public void testStripBlanksAndUnderscores()
    {
        assertThat(TextUtil.stripBlanksAndUnderscores("a _ b _ c"), is("abc"));
        assertThat(TextUtil.stripBlanksAndUnderscores("_ a _ b _ c _"), is("abc"));
    }

    @Test
    public void testTooltip()
    {
        assertThat(TextUtil.tooltip("Drag & drop"), is("Drag && drop"));
    }

    @Test
    public void testLimit()
    {
        String text = Strings.repeat("Lorem ipsum ", 2);
        assertThat(TextUtil.limit(text, 5), is("Lorem…"));
    }

    @Test
    public void testTrimStringInArray()
    {
        String toTrimString = " Portfolio , Performance ,   is  , a, great tool! ";
        String[] trimPartsAnswer = { "Portfolio", "Performance", "is", "a", "great tool!" };

        String[] trimParts = TextUtil.trim(toTrimString.split(","));

        assertThat(TextUtil.trim(" "), is(""));
        assertThat(trimParts, is(trimPartsAnswer));
    }

    @Test
    public void testConcatenate()
    {
        assertNull(TextUtil.concatenate(null, null, "-"));
        assertEquals("first", TextUtil.concatenate("first", null, "-"));
        assertEquals("first", TextUtil.concatenate("first", "first", "-"));
        assertEquals("second", TextUtil.concatenate(null, "second", "-"));
        assertEquals("first-second", TextUtil.concatenate("first", "second", "-"));
    }

    @Test
    public void testCompareNullCheck()
    {
        assertEquals(-1, TextUtil.compare(null, "a"));
        assertEquals(1, TextUtil.compare("a", null));
        assertEquals(0, TextUtil.compare(null, null));
        assertEquals(0, TextUtil.compare("abc", "abc"));
    }

    @Test
    public void testIsNumericMatch()
    {
        var decimalSeparator = new DecimalFormatSymbols(Locale.getDefault()).getDecimalSeparator();
        var decimalSep = String.valueOf(decimalSeparator);
        var groupingSeparator = new DecimalFormatSymbols(Locale.getDefault()).getGroupingSeparator();
        var groupSep = String.valueOf(groupingSeparator);

        // Whole numbers
        assertTrue(TextUtil.isNumericSearchMatch("2", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("26", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("260", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2601", 2601));
        assertFalse(TextUtil.isNumericSearchMatch("26010", 2601));

        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "6", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "60", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601", 2601));
        assertFalse(TextUtil.isNumericSearchMatch("2" + groupSep + "6010", 2601));

        // Decimal numbers
        assertTrue(TextUtil.isNumericSearchMatch("2", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("26", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("260", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2601", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep, 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep + "2", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep + "26", 2601.26));
        assertFalse(TextUtil.isNumericSearchMatch("2601" + decimalSep + "261", 2601.26));

        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "6", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "60", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601" + decimalSep, 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601" + decimalSep + "2", 2601.26));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601" + decimalSep + "26", 2601.26));
        assertFalse(TextUtil.isNumericSearchMatch("2" + groupSep + "601" + decimalSep + "261", 2601.26));

        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep, 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep + "0", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2601" + decimalSep + "00", 2601));
        assertFalse(TextUtil.isNumericSearchMatch("2601" + decimalSep + "01", 2601));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "601" + decimalSep, 2601));

        assertFalse(TextUtil.isNumericSearchMatch("25", 1250));

        assertTrue(TextUtil.isNumericSearchMatch("0", 0));
        assertTrue(TextUtil.isNumericSearchMatch("0", 0.01));
        assertTrue(TextUtil.isNumericSearchMatch("0" + decimalSep, 0.01));
        assertTrue(TextUtil.isNumericSearchMatch("0" + decimalSep + "0", 0.01));
        assertTrue(TextUtil.isNumericSearchMatch("0" + decimalSep + "01", 0.01));
        assertFalse(TextUtil.isNumericSearchMatch("0" + decimalSep + "012", 0.01));
        assertFalse(TextUtil.isNumericSearchMatch("0", 1));

        assertFalse(TextUtil.isNumericSearchMatch("", 12));
        assertFalse(TextUtil.isNumericSearchMatch("abc", 12));
        assertFalse(TextUtil.isNumericSearchMatch("12a", 12));
        assertFalse(TextUtil.isNumericSearchMatch("25" + decimalSep, 2500));
        assertFalse(TextUtil.isNumericSearchMatch("25" + decimalSep + "0", 2500));
        assertFalse(TextUtil.isNumericSearchMatch("25" + decimalSep + "33", 254.33));
        assertFalse(TextUtil.isNumericSearchMatch("250000", 2500));
        assertTrue(TextUtil.isNumericSearchMatch("12", 128));
        assertTrue(TextUtil.isNumericSearchMatch("128" + decimalSep, 128));

        // We don't care about sign because PP displays both positive and
        // negative numbers as positive
        assertTrue(TextUtil.isNumericSearchMatch("25", -25));
        assertTrue(TextUtil.isNumericSearchMatch("25" + decimalSep + "0", -25));
        assertTrue(TextUtil.isNumericSearchMatch("25" + decimalSep + "5", -25.5));
        assertTrue(TextUtil.isNumericSearchMatch("25" + decimalSep + "5", -25.55));
        assertTrue(TextUtil.isNumericSearchMatch("2" + groupSep + "500" + decimalSep + "0", -2500));

        assertTrue(TextUtil.isNumericSearchMatch("-25", -25));
        assertTrue(TextUtil.isNumericSearchMatch("-25", 25));
    }
}
