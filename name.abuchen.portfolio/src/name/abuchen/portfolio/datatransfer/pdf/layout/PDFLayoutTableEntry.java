package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record PDFLayoutTableEntry(PDFLayoutTableRegion table, List<PDFLayoutTableRow> rows)
{
    public String text(int columnIndex)
    {
        List<String> values = new ArrayList<>();

        for (PDFLayoutTableRow row : rows)
        {
            String value = clean(row.cellText(table, columnIndex));

            if (!value.isBlank() && !values.contains(value))
                values.add(value);
        }

        return String.join(" | ", values); //$NON-NLS-1$
    }

    public List<String> values()
    {
        return table.columns().stream().map(column -> text(column.segmentIndex())).collect(Collectors.toList());
    }

    public PDFLayoutTableRow firstRow()
    {
        return rows.get(0);
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }
}
