package name.abuchen.portfolio.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

    public static final String sanitizeFilename(String label)
    {
        // https://stackoverflow.com/a/10151795/1158146
            
        String filename = label;

        try
        {
            filename = new String(label.getBytes(), StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException ignore)
        {
            // UTF-8 is available
        }

        // filter ? \ / : | < > // *
        filename = filename.replaceAll("[\\?\\\\/:|<>\\*]", " "); //$NON-NLS-1$ //$NON-NLS-2$
        filename = filename.replaceAll("\\s+", "_"); //$NON-NLS-1$ //$NON-NLS-2$
        return filename;
    }
}
