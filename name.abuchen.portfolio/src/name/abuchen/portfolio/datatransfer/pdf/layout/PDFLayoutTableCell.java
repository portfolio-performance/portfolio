package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;
import java.util.stream.Collectors;

public record PDFLayoutTableCell(int columnIndex, List<PDFLayoutTableCellPart> parts)
{
    public String text()
    {
        return parts.stream().map(PDFLayoutTableCellPart::text).filter(text -> !text.isBlank()).distinct()
                        .collect(Collectors.joining(" | ")); //$NON-NLS-1$
    }

    public List<String> values()
    {
        return parts.stream().map(PDFLayoutTableCellPart::text).filter(text -> !text.isBlank()).distinct()
                        .collect(Collectors.toList());
    }

    public boolean isEmpty()
    {
        return values().isEmpty();
    }
}
