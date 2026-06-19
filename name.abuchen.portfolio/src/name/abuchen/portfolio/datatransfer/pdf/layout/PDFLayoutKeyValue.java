package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public record PDFLayoutKeyValue(String label, PDFLayoutSegmentRow row, List<PDFLayoutValueMatch> values,
                List<PDFLayoutSegmentRow> continuationRows)
{
    public PDFLayoutKeyValue(String label, PDFLayoutSegmentRow row, List<PDFLayoutValueMatch> values)
    {
        this(label, row, values, List.of());
    }

    public Optional<PDFLayoutValueMatch> value(int index)
    {
        if (index < 0 || index >= values.size())
            return Optional.empty();

        return Optional.of(values.get(index));
    }

    public String valueText(int index)
    {
        if (index < 0 || index >= values.size())
            return ""; //$NON-NLS-1$

        List<String> parts = new ArrayList<>();
        parts.add(values.get(index).text());

        if (index == values.size() - 1)
        {
            for (PDFLayoutSegmentRow continuationRow : continuationRows)
            {
                String text = clean(continuationRow.text());

                if (!text.isBlank())
                    parts.add(text);
            }
        }

        return String.join(" ", parts).trim(); //$NON-NLS-1$
    }

    public List<String> valueTexts()
    {
        List<String> result = new ArrayList<>();

        for (int ii = 0; ii < values.size(); ii++)
            result.add(valueText(ii));

        return List.copyOf(result);
    }

    public List<PDFLayoutSegmentRow> consumedRows()
    {
        List<PDFLayoutSegmentRow> result = new ArrayList<>();
        result.add(row);
        result.addAll(continuationRows);

        return List.copyOf(result);
    }

    public int page()
    {
        return row.page();
    }

    public int rowIndex()
    {
        return row.index();
    }

    private static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }
}
