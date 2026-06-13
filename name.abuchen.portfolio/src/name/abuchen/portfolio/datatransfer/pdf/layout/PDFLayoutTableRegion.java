package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public record PDFLayoutTableRegion(PDFLayoutSegmentRow header, List<PDFLayoutSegmentRow> rows)
{
    public boolean headerContains(String text)
    {
        return clean(header.text()).contains(clean(text));
    }

    public boolean headerContainsAll(String... values)
    {
        for (String value : values)
        {
            if (!headerContains(value))
                return false;
        }

        return true;
    }

    public List<PDFLayoutTableColumn> columns()
    {
        List<PDFLayoutTableColumn> answer = new ArrayList<>();

        for (int ii = 0; ii < header.segments().size(); ii++)
        {
            PDFLayoutSegment segment = header.segments().get(ii);
            answer.add(new PDFLayoutTableColumn(clean(segment.text()), ii, segment.xBucket()));
        }

        return List.copyOf(answer);
    }

    public List<PDFLayoutTableRow> tableRows()
    {
        return rows.stream().map(PDFLayoutTableRow::new).collect(Collectors.toList());
    }

    public Optional<PDFLayoutValueMatch> headerSegmentContaining(String text)
    {
        String needle = clean(text);

        for (int ii = 0; ii < header.segments().size(); ii++)
        {
            PDFLayoutSegment segment = header.segments().get(ii);

            if (clean(segment.text()).contains(needle))
                return Optional.of(new PDFLayoutValueMatch(header, ii, segment));
        }

        return Optional.empty();
    }

    public Optional<PDFLayoutTableColumn> columnByHeader(String text)
    {
        return columnByHeaderContaining(text);
    }

    public Optional<PDFLayoutValueMatch> valueAt(PDFLayoutSegmentRow row, String headerText, int tolerance)
    {
        Optional<PDFLayoutTableColumn> column = columnByHeader(headerText);

        if (column.isEmpty())
            return Optional.empty();

        return new PDFLayoutTableRow(row).segmentNearX(column.get().xBucket(), tolerance);
    }

    private static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }

    public Optional<PDFLayoutTableColumn> columnByHeaderExact(String text)
    {
        String needle = clean(text);

        return columns().stream().filter(column -> clean(column.headerText()).equals(needle)).findFirst();
    }

    public Optional<PDFLayoutTableColumn> columnByHeaderContaining(String text)
    {
        String needle = clean(text);

        return columns().stream().filter(column -> clean(column.headerText()).contains(needle)).findFirst();
    }

    public List<PDFLayoutTableEntry> entries()
    {
        List<PDFLayoutTableEntry> answer = new ArrayList<>();
        List<PDFLayoutTableRow> rows = tableRows();
        List<PDFLayoutTableRow> current = new ArrayList<>();

        for (int ii = 0; ii < rows.size(); ii++)
        {
            PDFLayoutTableRow row = rows.get(ii);
            PDFLayoutTableRow next = ii + 1 < rows.size() ? rows.get(ii + 1) : null;

            PDFLayoutTableRowType type = row.type(this);

            if (type == PDFLayoutTableRowType.HELPER)
                continue;

            if (type == PDFLayoutTableRowType.SECTION)
            {
                if (!current.isEmpty())
                {
                    answer.add(new PDFLayoutTableEntry(this, List.copyOf(current)));
                    current.clear();
                }

                continue;
            }

            if (isEntryStart(row, next, current))
            {
                if (!current.isEmpty())
                {
                    answer.add(new PDFLayoutTableEntry(this, List.copyOf(current)));
                    current.clear();
                }
            }

            current.add(row);
        }

        if (!current.isEmpty())
            answer.add(new PDFLayoutTableEntry(this, List.copyOf(current)));

        return List.copyOf(answer);
    }

    private boolean isEntryStart(PDFLayoutTableRow row, PDFLayoutTableRow next, List<PDFLayoutTableRow> current)
    {
        String first = row.cellText(this, 0);
        String second = columns().size() > 1 ? row.cellText(this, 1) : ""; //$NON-NLS-1$
        String last = columns().isEmpty() ? "" : row.cellText(this, columns().size() - 1); //$NON-NLS-1$

        if (!PDFLayoutTextClassifier.looksLikeDate(first))
            return false;

        if (!clean(last).isBlank())
            return true;

        if (current.isEmpty())
            return !clean(second).isBlank();

        return nextHasAmountForSameDate(first, next);
    }

    private boolean nextHasAmountForSameDate(String date, PDFLayoutTableRow next)
    {
        if (next == null)
            return false;

        String nextFirst = next.cellText(this, 0);
        String nextLast = columns().isEmpty() ? "" : next.cellText(this, columns().size() - 1); //$NON-NLS-1$

        return clean(date).equals(clean(nextFirst)) && !clean(nextLast).isBlank();
    }
}
