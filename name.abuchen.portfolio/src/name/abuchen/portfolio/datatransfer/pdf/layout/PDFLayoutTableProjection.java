package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;

public record PDFLayoutTableProjection(PDFLayoutTableRegion table, List<List<PDFLayoutTableCell>> rows)
{
}
