package name.abuchen.portfolio.datatransfer.pdf.layout;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public record PDFLayoutValueMatch(PDFLayoutSegmentRow row, int segmentIndex, PDFLayoutSegment segment)
{
    public String text()
    {
        return segment.text() == null ? "" : segment.text().trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public int page()
    {
        return row.page();
    }

    public int rowIndex()
    {
        return row.index();
    }

    public int xBucket()
    {
        return segment.xBucket();
    }
}
