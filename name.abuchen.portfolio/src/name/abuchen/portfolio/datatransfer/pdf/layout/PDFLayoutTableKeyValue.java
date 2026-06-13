package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;
import java.util.stream.Collectors;

public record PDFLayoutTableKeyValue(String label, List<String> values)
{
    public String valueText()
    {
        return values.stream().filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" | ")); //$NON-NLS-1$
    }
}
