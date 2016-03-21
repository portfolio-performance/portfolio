package name.abuchen.portfolio.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil
{
    private TextUtil()
    {}

    public static final String wordwrap(String text)
    {
        if (text == null)
            return null;

        // add a word boundary to correctly match a full line
        String raw = text + "X"; //$NON-NLS-1$

        StringBuilder wrapped = new StringBuilder();
        Pattern p = Pattern.compile(".{0,80}\\b[ \\t\\n\\x0b\\r\\f,.]*"); //$NON-NLS-1$
        Matcher m = p.matcher(raw);
        while (m.find())
        {
            if (wrapped.length() > 0)
                wrapped.append("\n"); //$NON-NLS-1$

            String substring = raw.substring(m.start(), m.end());
            wrapped.append(substring.replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // remove added character needed to create a word boundary
        return wrapped.substring(0, wrapped.length() - 2);
    }

    public static final String tooltip(String text)
    {
        return text == null ? null : text.replaceAll("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
