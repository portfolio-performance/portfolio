package name.abuchen.portfolio.datatransfer.pdf.layout;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;

final class PDFLayoutTextClassifier
{
    private PDFLayoutTextClassifier()
    {
    }

    static boolean looksLikeDate(String value)
    {
        return ExtractorUtils.looksLikeDate(clean(value));
    }

    static boolean containsAmountLikeValue(String value)
    {
        return clean(value).matches(".*[-+]?\\d{1,3}([.,'’\\s]\\d{3})*[.,]\\d{2}.*"); //$NON-NLS-1$
    }

    static boolean looksLikeNumericValue(String value)
    {
        String text = clean(value);
        return text.matches("[-+]?\\d+([.,]\\d+)?"); //$NON-NLS-1$
    }

    static boolean looksLikeLabel(String value)
    {
        String text = clean(value);

        if (text.length() < 3 || text.length() > 80)
            return false;

        if (looksLikeDate(text))
            return false;

        if (containsAmountLikeValue(text))
            return false;

        return text.matches(".*\\p{L}.*"); //$NON-NLS-1$
    }

    static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }
}