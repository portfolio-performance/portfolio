package name.abuchen.portfolio.ui.util.swt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

@SuppressWarnings("nls")
public class TextSearchTest
{
    private static final String TEXT = "Kauf HOME DEPOT INC.\nKurs : 260,0000 USD\nkurs geprüft\nEndbetrag : -5.987,16 USD";

    private static List<int[]> ranges(int... values)
    {
        var result = new ArrayList<int[]>();
        for (var ii = 0; ii < values.length; ii += 2)
            result.add(new int[] { values[ii], values[ii + 1] });
        return result;
    }

    private static List<String> format(List<int[]> ranges)
    {
        return ranges.stream().map(r -> r[0] + "+" + r[1]).toList();
    }

    // -- findMatches

    @Test
    public void testFindsAllMatchesIgnoringCase()
    {
        assertThat(TextSearch.findMatches(TEXT, "KURS"), contains(TEXT.indexOf("Kurs"), TEXT.indexOf("kurs")));
        assertThat(TextSearch.findMatches(TEXT, "USD").size(), is(2));
        assertThat(TextSearch.findMatches(TEXT, "Kauf"), contains(0));
    }

    @Test
    public void testFindsUmlauts()
    {
        assertThat(TextSearch.findMatches(TEXT, "geprüft"), contains(TEXT.indexOf("geprüft")));
    }

    @Test
    public void testNoMatches()
    {
        assertThat(TextSearch.findMatches(TEXT, "Dividende"), is(empty()));
        assertThat(TextSearch.findMatches(TEXT, ""), is(empty()));
        assertThat(TextSearch.findMatches(null, "Kurs"), is(empty()));
        assertThat(TextSearch.findMatches(TEXT, null), is(empty()));
    }

    @Test
    public void testMatchesDoNotOverlap()
    {
        assertThat(TextSearch.findMatches("aaaa", "aa"), contains(0, 2));
    }

    @Test
    public void testOffsetsReferToTheOriginalTextIfLowerCasingChangesTheLength()
    {
        var text = "\u0130STANBUL Kurs USD";

        // lower casing the dotted capital I adds a combining character
        assertThat(text.toLowerCase(Locale.ROOT).length() == text.length(), is(false));

        assertThat(TextSearch.findMatches(text, "kurs"), contains(text.indexOf("Kurs")));
        assertThat(TextSearch.findMatches(text, "\u0130stanbul"), contains(0));
    }

    @Test
    public void testOffsetsDependOnTheLineDelimiter()
    {
        // Text returns the content with the line delimiter of the platform,
        // StyledText does not: the offsets must be calculated per widget
        var styled = "Kurs USD\nEndbetrag USD\nProvision USD";
        var plain = styled.replace("\n", "\r\n");

        var inStyled = TextSearch.findMatches(styled, "USD");
        var inPlain = TextSearch.findMatches(plain, "USD");

        assertThat(inPlain.size(), is(inStyled.size()));
        assertThat(inPlain.get(2) + 3 > styled.length(), is(true));
        assertThat(inStyled.get(2) + 3 <= styled.length(), is(true));
    }

    // -- subtract

    @Test
    public void testSubtractWithoutHoles()
    {
        assertThat(format(TextSearch.subtract(ranges(10, 20), List.of())), contains("10+20"));
    }

    @Test
    public void testSubtractSplitsRangesAroundHoles()
    {
        assertThat(format(TextSearch.subtract(ranges(10, 20), ranges(15, 5))), contains("10+5", "20+10"));
        assertThat(format(TextSearch.subtract(ranges(10, 20), ranges(10, 5))), contains("15+15"));
        assertThat(format(TextSearch.subtract(ranges(10, 20), ranges(25, 5))), contains("10+15"));
        assertThat(format(TextSearch.subtract(ranges(10, 20), ranges(40, 5))), contains("10+20"));
    }

    @Test
    public void testSubtractRemovesCoveredRanges()
    {
        assertThat(TextSearch.subtract(ranges(10, 20), ranges(5, 40)), is(empty()));
    }

    @Test
    public void testSubtractMultipleRangesAndHoles()
    {
        assertThat(format(TextSearch.subtract(ranges(0, 30), ranges(5, 5, 20, 5))), contains("0+5", "10+10", "25+5"));
        assertThat(format(TextSearch.subtract(ranges(0, 10, 20, 10), ranges(5, 20))), contains("0+5", "25+5"));
    }
}
