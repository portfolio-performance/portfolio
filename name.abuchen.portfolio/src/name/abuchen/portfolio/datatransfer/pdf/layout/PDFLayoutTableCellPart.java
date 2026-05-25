package name.abuchen.portfolio.datatransfer.pdf.layout;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;

public record PDFLayoutTableCellPart(int rowIndex, int segmentIndex, int xBucket, String text)
{
    public static PDFLayoutTableCellPart of(int rowIndex, int segmentIndex, PDFLayoutSegment segment)
    {
        return new PDFLayoutTableCellPart(rowIndex, segmentIndex, segment.xBucket(), clean(segment.text()));
    }

    private static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }
}
