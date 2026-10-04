package name.abuchen.portfolio.ui.util.swt;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the matches of a search term within a text and splits highlighted
 * ranges around the matches. Used by the text view of the {@link PDFViewer};
 * the class has no user interface.
 */
final class TextSearch
{
    private TextSearch()
    {
    }

    /**
     * Returns the start offsets of all matches of the term within the text,
     * ignoring case. An empty term has no matches.
     */
    static List<Integer> findMatches(String text, String term)
    {
        var offsets = new ArrayList<Integer>();

        if (text == null || term == null || term.isEmpty())
            return offsets;

        // search the original text: lower casing can change the length of
        // the text and therefore the offsets of the matches
        var index = 0;
        while (index + term.length() <= text.length())
        {
            if (text.regionMatches(true, index, term, 0, term.length()))
            {
                offsets.add(index);
                index += term.length();
            }
            else
            {
                index++;
            }
        }

        return offsets;
    }

    /**
     * Removes the parts of the ranges which overlap one of the holes. Both are
     * given as {start, length}.
     */
    static List<int[]> subtract(List<int[]> ranges, List<int[]> holes)
    {
        var result = new ArrayList<int[]>();

        for (var range : ranges)
        {
            var parts = new ArrayList<int[]>();
            parts.add(new int[] { range[0], range[0] + range[1] });

            for (var hole : holes)
            {
                var remaining = new ArrayList<int[]>();

                for (var part : parts)
                {
                    var holeStart = hole[0];
                    var holeEnd = hole[0] + hole[1];

                    if (holeEnd <= part[0] || holeStart >= part[1])
                    {
                        remaining.add(part);
                        continue;
                    }

                    if (part[0] < holeStart)
                        remaining.add(new int[] { part[0], holeStart });

                    if (holeEnd < part[1])
                        remaining.add(new int[] { holeEnd, part[1] });
                }

                parts = remaining;
            }

            for (var part : parts)
                result.add(new int[] { part[0], part[1] - part[0] });
        }

        return result;
    }
}
