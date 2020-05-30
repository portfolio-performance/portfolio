package name.abuchen.portfolio.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil
{
    public static final String PARAGRAPH_BREAK = "\n\n"; //$NON-NLS-1$

    private static final String VALID_NUM_CHARACTERS = "0123456789,.'-"; //$NON-NLS-1$

    private TextUtil()
    {
    }

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

            String fragment = raw.substring(m.start(), m.end());

            // if fragment includes a line-break, do not add another one
            if (fragment.length() > 0 && fragment.charAt(fragment.length() - 1) == '\n')
                fragment = fragment.substring(0, fragment.length() - 1);

            wrapped.append(fragment.replace("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // remove added character needed to create a word boundary
        return wrapped.substring(0, wrapped.length() - 2);
    }

    public static final String tooltip(String text)
    {
        return text == null ? null : text.replace("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Adds a space before and after the text. Typically used for tool tips
     * where the label background is colored according to the data series. On
     * Windows the labels do not look right without padding.
     */
    public static final String pad(String text)
    {
        return text == null ? null : " " + text + " "; //$NON-NLS-1$ //$NON-NLS-2$
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

    /**
     * Since {@see String#trim} does not trim all whitespace and space
     * characters, this is an alternative implementation. Inspired by the blog
     * post at http://closingbraces.net/2008/11/11/javastringtrim/
     */
    public static String strip(String value)
    {
        if (value == null)
            return null;

        int len = value.length();
        int st = 0;

        while ((st < len) && (Character.isWhitespace(value.charAt(st)) || Character.isSpaceChar(value.charAt(st))))
        {
            st++;
        }

        while ((st < len) && (Character.isWhitespace(value.charAt(len - 1))
                        || Character.isSpaceChar(value.charAt(len - 1))))
        {
            len--;
        }
        return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;

    }

    /**
     * Removes unwanted characters before and after any number characters. Used
     * when importing data from CSV files.
     */
    public static String stripNonNumberCharacters(String value)
    {
        int start = 0;
        int len = value.length();

        while ((start < len) && VALID_NUM_CHARACTERS.indexOf(value.charAt(start)) < 0)
            start++;

        while ((start < len) && VALID_NUM_CHARACTERS.indexOf(value.charAt(len - 1)) < 0)
            len--;

        return ((start > 0) || (len < value.length())) ? value.substring(start, len) : value;
    }
}
