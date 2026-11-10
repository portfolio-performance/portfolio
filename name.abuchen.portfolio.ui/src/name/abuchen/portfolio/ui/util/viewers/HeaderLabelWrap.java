package name.abuchen.portfolio.ui.util.viewers;

import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * Splits a column header label onto at most two lines at word boundaries. The
 * width of a text is passed in, so that the logic does not depend on SWT.
 */
final class HeaderLabelWrap
{
    private static final String SPACE = " "; //$NON-NLS-1$
    private static final String NEWLINE = "\n"; //$NON-NLS-1$

    private HeaderLabelWrap()
    {
    }

    /**
     * Returns the text to show for a label in a column header: the label itself
     * if it fits, otherwise the label on two lines.
     * <ul>
     * <li>A wrapped label is unwrapped only once it fits with the hysteresis to
     * spare, so that a column dragged right at the boundary does not flip
     * between one and two lines on every pixel.</li>
     * <li>The current split is kept as long as both lines fit. This keeps the
     * header stable where its width depends on the label: GTK widens a column
     * until the longest line fits, and re-splitting at the new width would make
     * it narrow again.</li>
     * <li>Otherwise the first line takes as many words as fit, but leaves at
     * least one word for the second line. If the second line does not fit
     * either and the header grows to the label (GTK) instead of cutting it
     * off, the most balanced split is used: it widens the column the least.</li>
     * </ul>
     *
     * @param label
     *            the label without line breaks
     * @param current
     *            the text currently shown in the header
     * @param available
     *            the width available for the label
     * @param hysteresis
     *            the extra width a wrapped label needs to be unwrapped
     * @param growsToFit
     *            whether the header widens the column to the label instead of
     *            cutting it off
     * @param width
     *            the width of a text
     */
    static String wrap(String label, String current, int available, int hysteresis, boolean growsToFit,
                    ToIntFunction<String> width)
    {
        if (!label.contains(SPACE))
            return label;

        var wasWrapped = current.contains(NEWLINE);
        var fitThreshold = wasWrapped ? available - hysteresis : available;
        if (width.applyAsInt(label) <= fitThreshold)
            return label;

        if (wasWrapped && current.replace(NEWLINE, SPACE).equals(label) && fits(current, available, width))
            return current;

        var words = label.split(SPACE);
        var fillTo = 1;
        for (int ii = 2; ii < words.length; ii++)
        {
            if (width.applyAsInt(join(words, 0, ii)) > available)
                break;
            fillTo = ii;
        }

        if (growsToFit && width.applyAsInt(join(words, fillTo, words.length)) > available)
            fillTo = balancedSplit(words, width);

        return join(words, 0, fillTo) + NEWLINE + join(words, fillTo, words.length);
    }

    /**
     * Returns the longer line of the most balanced two-line split of the label,
     * that is of the split whose longer line is the shortest. Returns the label
     * itself if it has no space.
     *
     * @param label
     *            the label without line breaks
     * @param width
     *            the width of a text
     */
    static String longerLineOfBalancedSplit(String label, ToIntFunction<String> width)
    {
        var words = label.split(SPACE);
        if (words.length < 2)
            return label;

        var splitAt = balancedSplit(words, width);
        var line1 = join(words, 0, splitAt);
        var line2 = join(words, splitAt, words.length);
        return width.applyAsInt(line1) >= width.applyAsInt(line2) ? line1 : line2;
    }

    /**
     * Returns the number of words on the first line of the most balanced split,
     * that is of the split whose longer line is the shortest (the first one if
     * several are equally long). Expects at least two words.
     */
    private static int balancedSplit(String[] words, ToIntFunction<String> width)
    {
        var best = 1;
        var bestWidth = Integer.MAX_VALUE;
        for (int ii = 1; ii < words.length; ii++)
        {
            var longer = Math.max(width.applyAsInt(join(words, 0, ii)), width.applyAsInt(join(words, ii, words.length)));
            if (longer < bestWidth)
            {
                bestWidth = longer;
                best = ii;
            }
        }
        return best;
    }

    private static boolean fits(String text, int available, ToIntFunction<String> width)
    {
        for (String line : text.split(NEWLINE))
        {
            if (width.applyAsInt(line) > available)
                return false;
        }
        return true;
    }

    private static String join(String[] words, int from, int to)
    {
        return String.join(SPACE, Arrays.copyOfRange(words, from, to));
    }
}
