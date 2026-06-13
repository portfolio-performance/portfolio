package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;
import java.util.Optional;

public record PDFLayoutTableKeyValueProjection(PDFLayoutTableRegion table, List<PDFLayoutTableKeyValue> values)
{
    public Optional<PDFLayoutTableKeyValue> find(String label)
    {
        String needle = clean(label);

        return values.stream().filter(value -> clean(value.label()).equals(needle)).findFirst();
    }

    private static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }
}
