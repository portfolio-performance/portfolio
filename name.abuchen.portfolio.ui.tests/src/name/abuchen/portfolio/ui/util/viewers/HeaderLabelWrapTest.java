package name.abuchen.portfolio.ui.util.viewers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.ToIntFunction;

import org.junit.Test;

@SuppressWarnings("nls")
public class HeaderLabelWrapTest
{
    /** one unit per character, like a monospaced font */
    private static final ToIntFunction<String> WIDTH = String::length;

    private static final String LONG_LABEL = "Kursgewinn (GLD) % 1 Handelstag";

    private static String wrap(String label, String current, int available)
    {
        return HeaderLabelWrap.wrap(label, current, available, 8, false, WIDTH);
    }

    @Test
    public void testLabelThatFitsStaysOnOneLine()
    {
        assertThat(wrap("IZF 1 Jahr", "IZF 1 Jahr", 10), is("IZF 1 Jahr"));
    }

    @Test
    public void testLabelWithoutSpaceIsNeverWrapped()
    {
        assertThat(wrap("Kontostand", "Kontostand", 4), is("Kontostand"));
    }

    @Test
    public void testLabelThatDoesNotFitIsWrappedWithAsManyWordsAsFitOnTheFirstLine()
    {
        assertThat(wrap("aaa bbb ccc", "aaa bbb ccc", 8), is("aaa bbb\nccc"));
    }

    @Test
    public void testFirstWordLongerThanColumnStaysOnTheFirstLine()
    {
        assertThat(wrap("Kursgewinn %", "Kursgewinn %", 5), is("Kursgewinn\n%"));
    }

    @Test
    public void testWrappedLabelStaysWrappedWithinTheHysteresis()
    {
        // fits the 12 units, but not with the hysteresis of 8 to spare
        assertThat(wrap("aaa bbb ccc", "aaa bbb\nccc", 12), is("aaa bbb\nccc"));
    }

    @Test
    public void testUnwrappedLabelIsNotWrappedWithinTheHysteresis()
    {
        assertThat(wrap("aaa bbb ccc", "aaa bbb ccc", 12), is("aaa bbb ccc"));
    }

    @Test
    public void testWrappedLabelIsUnwrappedOnceItFitsWithTheHysteresisToSpare()
    {
        assertThat(wrap("aaa bbb ccc", "aaa bbb\nccc", 19), is("aaa bbb ccc"));
    }

    @Test
    public void testFirstLineIsFilledUpToTheFullWidthWhenWrapped()
    {
        // the column fitted to the longer line of the balanced split while the
        // previous split no longer fits
        var available = "Kursgewinn (GLD)".length();
        assertThat(wrap(LONG_LABEL, "Kursgewinn\n(GLD) % 1 Handelstag", available),
                        is("Kursgewinn (GLD)\n% 1 Handelstag"));
    }

    @Test
    public void testCurrentSplitIsKeptWhileItFits()
    {
        assertThat(wrap("aaa bbb ccc", "aaa\nbbb ccc", 8), is("aaa\nbbb ccc"));
    }

    @Test
    public void testCurrentSplitThatNoLongerFitsIsReplaced()
    {
        // "bb cc dd" (8) no longer fits into 5: split again, filling the first line
        assertThat(wrap("aa bb cc dd", "aa\nbb cc dd", 5), is("aa bb\ncc dd"));
    }

    @Test
    public void testSplitOfARenamedLabelIsNotKept()
    {
        assertThat(wrap("aaa bbb ccc", "xxx\nyyy", 8), is("aaa bbb\nccc"));
    }

    @Test
    public void testHeaderThatCutsOffKeepsTheFullFirstLine()
    {
        // nothing fits into 9: the first line is filled, the second is cut off
        assertThat(wrap("aaaaaaa bb cccccccccc", "aaaaaaa bb cccccccccc", 9), is("aaaaaaa\nbb cccccccccc"));
    }

    @Test
    public void testHeaderThatGrowsToFitUsesTheBalancedSplitIfNothingFits()
    {
        assertThat(HeaderLabelWrap.wrap("aaaaaaa bb cccccccccc", "aaaaaaa bb cccccccccc", 9, 8, true, WIDTH),
                        is("aaaaaaa bb\ncccccccccc"));
    }

    @Test
    public void testHeaderThatGrowsToFitIsStableAtTheWidthItGrewTo()
    {
        // GTK widened the column from 9 to 10 so that the balanced split fits;
        // at 10 the greedy split would differ, the current split is kept
        var grown = HeaderLabelWrap.wrap("aaaaaaa bb cccccccccc", "aaaaaaa bb\ncccccccccc", 10, 8, true, WIDTH);
        assertThat(grown, is("aaaaaaa bb\ncccccccccc"));
        assertThat(HeaderLabelWrap.wrap("aaaaaaa bb cccccccccc", grown, 9, 8, true, WIDTH), is(grown));
    }

    @Test
    public void testLongerLineOfBalancedSplit()
    {
        assertThat(HeaderLabelWrap.longerLineOfBalancedSplit(LONG_LABEL, WIDTH), is("Kursgewinn (GLD)"));
        assertThat(HeaderLabelWrap.longerLineOfBalancedSplit("Div% 1 Jahr", WIDTH), is("1 Jahr"));
        // "Gewinn /" (8) | "Verlust" (7) beats "Gewinn" (6) | "/ Verlust" (9)
        assertThat(HeaderLabelWrap.longerLineOfBalancedSplit("Gewinn / Verlust", WIDTH), is("Gewinn /"));
    }

    @Test
    public void testLongerLineOfBalancedSplitOfLabelWithoutSpaceIsTheLabel()
    {
        assertThat(HeaderLabelWrap.longerLineOfBalancedSplit("Kontostand", WIDTH), is("Kontostand"));
    }

    @Test
    public void testBalancedSplitWrapsIntoTwoLinesThatFit()
    {
        var line = HeaderLabelWrap.longerLineOfBalancedSplit(LONG_LABEL, WIDTH);
        var wrapped = wrap(LONG_LABEL, LONG_LABEL, line.length());

        for (String part : wrapped.split("\n"))
            assertThat(part + " fits", part.length() <= line.length(), is(true));
    }
}
