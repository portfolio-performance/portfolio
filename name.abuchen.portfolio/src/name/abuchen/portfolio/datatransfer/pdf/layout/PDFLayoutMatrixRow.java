package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;

public record PDFLayoutMatrixRow(int rowIndex, List<PDFLayoutTableCell> cells)
{
}
