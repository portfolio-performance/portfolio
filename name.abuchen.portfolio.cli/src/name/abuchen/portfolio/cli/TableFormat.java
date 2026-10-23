package name.abuchen.portfolio.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import com.github.freva.asciitable.OverflowBehaviour;

/**
 * Writes the items of a list response as an aligned text table. Stdout holds
 * only the table; warnings and a paging footer go to stderr, and the rest of
 * <code>meta</code> is dropped. The library counts UTF-16 characters, so wide
 * characters (CJK, emoji) may misalign.
 */
final class TableFormat
{
    /** Index of the header separator's line and joint characters in the library's border array. */
    private static final int HEADER_COLUMN_SEPARATOR = 5;
    private static final int SEPARATOR_LINE = 8;
    private static final int SEPARATOR_JOINT = 9;
    private static final int DATA_COLUMN_SEPARATOR = 12;

    /** The library pads every cell with one space on each side. */
    private static final int CELL_PADDING = 2;

    private TableFormat()
    {
    }

    static void print(PrintStream stdout, PrintStream stderr, Response response, List<TableColumn> columns)
    {
        var data = (Map<?, ?>) response.data();
        var items = (List<?>) data.get("items");

        stdout.print(render(columns, items));
        stdout.flush();

        for (var warning : response.warnings())
            stderr.println("warning: " + warning.code() + ": " + warning.detail());

        var total = (Integer) data.get("total");
        if (items.size() < total)
        {
            var footer = new StringBuilder("showing " + items.size() + " of " + total);
            if (data.get("nextOffset") instanceof Integer next)
                footer.append("; next page: --offset ").append(next);
            stderr.println(footer);
        }
    }

    /** The table, one line per row, each ending in a newline. */
    static String render(List<TableColumn> columns, List<?> items)
    {
        var table = AsciiTable.getTable(border(), columns(columns), rows(columns, items));

        // drop the leading cell padding and the trailing one before the line end
        var rendered = new StringBuilder();
        table.lines().forEach(line -> rendered.append(line.substring(1).stripTrailing()).append('\n'));
        return rendered.toString();
    }

    /** Only a header and a header separator; a missing border character still takes a column of space. */
    private static Character[] border()
    {
        var border = new Character[AsciiTable.NO_BORDERS.length];
        border[HEADER_COLUMN_SEPARATOR] = ' ';
        border[SEPARATOR_LINE] = '-';
        border[SEPARATOR_JOINT] = '-';
        border[DATA_COLUMN_SEPARATOR] = ' ';
        return border;
    }

    /** The library wraps a column at 80 characters unless told otherwise; the table must keep one line per item. */
    private static Column[] columns(List<TableColumn> columns)
    {
        return columns.stream().map(column -> {
            var width = column.maxLength() == TableColumn.UNBOUNDED ? Integer.MAX_VALUE
                            : column.maxLength() + CELL_PADDING;
            return new Column().header(column.header()).dataAlign(HorizontalAlign.LEFT).maxWidth(width,
                            OverflowBehaviour.ELLIPSIS_RIGHT);
        }).toArray(Column[]::new);
    }

    private static Object[][] rows(List<TableColumn> columns, List<?> items)
    {
        var rows = new Object[items.size()][];
        for (int ii = 0; ii < rows.length; ii++)
        {
            var item = (Map<?, ?>) items.get(ii);
            rows[ii] = columns.stream().map(column -> cell(item.get(column.key()))).toArray();
        }
        return rows;
    }

    private static String cell(Object value)
    {
        if (value == null)
            return "";
        if (value instanceof Boolean flag)
            return flag ? "yes" : "";
        return value.toString().replaceAll("[\\r\\n\\t]+", " ");
    }
}
