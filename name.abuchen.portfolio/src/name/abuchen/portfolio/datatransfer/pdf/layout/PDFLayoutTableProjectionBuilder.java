package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public final class PDFLayoutTableProjectionBuilder
{
    private static final int COLUMN_TOLERANCE = 50;

    public PDFLayoutTableProjection build(PDFLayoutTableRegion table)
    {
        List<List<PDFLayoutTableCell>> rows = new ArrayList<>();

        rows.add(buildCells(table, table.header(), -1));

        int rowIndex = 0;

        for (PDFLayoutSegmentRow row : table.rows())
            rows.add(buildCells(table, row, rowIndex++));

        return new PDFLayoutTableProjection(table, List.copyOf(rows));
    }

    private List<PDFLayoutTableCell> buildCells(PDFLayoutTableRegion table, PDFLayoutSegmentRow row, int rowIndex)
    {
        List<PDFLayoutTableCell> cells = new ArrayList<>();

        for (int columnIndex = 0; columnIndex < table.columns().size(); columnIndex++)
        {

            List<PDFLayoutTableCellPart> parts = new ArrayList<>();

            for (int segmentIndex = 0; segmentIndex < row.segments().size(); segmentIndex++)
            {
                PDFLayoutSegment segment = row.segments().get(segmentIndex);

                if (belongsToColumn(table, columnIndex, segment))
                    parts.add(PDFLayoutTableCellPart.of(rowIndex, segmentIndex, segment));
            }

            cells.add(new PDFLayoutTableCell(columnIndex, List.copyOf(parts)));
        }

        return List.copyOf(cells);
    }

    private boolean belongsToColumn(PDFLayoutTableRegion table, int columnIndex, PDFLayoutSegment segment)
    {
        PDFLayoutTableColumn nearest = table.columns().stream()
                        .min(Comparator.comparingInt(column -> Math.abs(column.xBucket() - segment.xBucket())))
                        .orElse(null);

        if (nearest == null)
            return false;

        PDFLayoutTableColumn current = table.columns().get(columnIndex);

        return nearest.segmentIndex() == current.segmentIndex()
                        && Math.abs(current.xBucket() - segment.xBucket()) <= COLUMN_TOLERANCE;
    }
}
