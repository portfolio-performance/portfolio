package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.Comparator;
import java.util.Optional;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public record PDFLayoutTableRow(PDFLayoutSegmentRow row)
{
    public Optional<PDFLayoutValueMatch> segmentNearX(int xBucket, int tolerance)
    {
        return row.segments().stream().filter(segment -> Math.abs(segment.xBucket() - xBucket) <= tolerance)
                        .min(Comparator.comparingInt(segment -> Math.abs(segment.xBucket() - xBucket)))
                        .map(segment -> new PDFLayoutValueMatch(row, row.segments().indexOf(segment), segment));
    }

    public String textNearX(int xBucket, int tolerance)
    {
        return segmentNearX(xBucket, tolerance).map(PDFLayoutValueMatch::text).orElse(""); //$NON-NLS-1$
    }

    public boolean hasSegmentNearX(int xBucket, int tolerance)
    {
        return segmentNearX(xBucket, tolerance).isPresent();
    }

    public boolean isSingleSegment()
    {
        return row.segments().size() == 1;
    }

    public String text()
    {
        return row.text();
    }

    public int page()
    {
        return row.page();
    }

    public int rowIndex()
    {
        return row.index();
    }

    public PDFLayoutTableRowType type(PDFLayoutTableRegion table)
    {
        int columnCount = table.columns().size();

        if (columnCount == 0)
            return PDFLayoutTableRowType.EMPTY;

        String first = cellText(table, 0);
        String middle = columnCount > 1 ? cellText(table, 1) : ""; //$NON-NLS-1$
        String last = cellText(table, columnCount - 1);

        if (isBlank(first) && isBlank(middle) && isBlank(last))
            return PDFLayoutTableRowType.EMPTY;

        if (PDFLayoutTextClassifier.looksLikeDate(first) && !isBlank(last))
            return PDFLayoutTableRowType.DATA;

        if (isBlank(first) && !isBlank(middle) && isBlank(last))
            return PDFLayoutTableRowType.CONTINUATION;

        if (!isBlank(first) && isBlank(middle) && isBlank(last))
        {
            if (PDFLayoutTextClassifier.looksLikeDate(first))
                return PDFLayoutTableRowType.CONTINUATION;

            return PDFLayoutTableRowType.SECTION;
        }

        return PDFLayoutTableRowType.CONTINUATION;
    }

    public String cellText(PDFLayoutTableRegion table, int columnIndex)
    {
        if (columnIndex < 0 || columnIndex >= table.columns().size())
            return ""; //$NON-NLS-1$

        PDFLayoutTableColumn column = table.columns().get(columnIndex);
        return textNearX(column.xBucket(), 50);
    }

    private boolean isBlank(String value)
    {
        return clean(value).isBlank();
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
