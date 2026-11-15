package name.abuchen.portfolio.datatransfer.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal builder for GitHub-flavored Markdown tables.
 */
@SuppressWarnings("nls")
/* package */ class MarkdownTable
{
    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    /* package */ MarkdownTable(String... headers)
    {
        this.headers = headers;
    }

    /**
     * Adds a row. Cells are used as given; use {@link MarkdownFormat#text} for
     * free-form text.
     */
    /* package */ MarkdownTable row(String... cells)
    {
        if (cells.length != headers.length)
            throw new IllegalArgumentException("expected " + headers.length + " cells but got " + cells.length + ": "
                            + String.join("|", cells));

        rows.add(cells);
        return this;
    }

    /* package */ boolean isEmpty()
    {
        return rows.isEmpty();
    }

    /* package */ int size()
    {
        return rows.size();
    }

    /* package */ void appendTo(StringBuilder out)
    {
        appendRow(out, headers);

        out.append('|');
        for (int ii = 0; ii < headers.length; ii++)
            out.append(" --- |");
        out.append('\n');

        for (var row : rows)
            appendRow(out, row);

        out.append('\n');
    }

    private void appendRow(StringBuilder out, String[] cells)
    {
        out.append('|');
        for (var cell : cells)
            out.append(' ').append(cell == null ? "" : cell).append(" |");
        out.append('\n');
    }
}
