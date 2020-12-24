package name.abuchen.portfolio.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil
{
    public static final String PARAGRAPH_BREAK = "\n\n"; //$NON-NLS-1$

    private static final String VALID_NUM_CHARACTERS = "0123456789,.'-"; //$NON-NLS-1$

    public static final char DECIMAL_SEPARATOR = new DecimalFormatSymbols().getDecimalSeparator();

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
     * Strips all whitespace and space characters using {@link #strip} from all
     * values of the array.
     */
    public static String[] strip(String[] values)
    {
        if (values == null)
            return new String[0];

        String[] answer = new String[values.length];

        for (int i = 0; i < values.length; i++)
            answer[i] = TextUtil.strip(values[i]);

        return answer;
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

    public static char getListSeparatorChar()
    {
        // handle Switzerland differently because it uses a point as decimal
        // separator but a semicolon as a list separator

        if ("CH".equals(Locale.getDefault().getCountry())) //$NON-NLS-1$
            return ';';
        return DECIMAL_SEPARATOR == ',' ? ';' : ',';
    }

    /**
     * Create a readable name from a camel case string, e.g. converts
     * "replicationMethod" into "Replication Method"
     */
    public static String fromCamelCase(String camelCase)
    {
        if (camelCase == null)
            return null;

        String[] parts = camelCase.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"); //$NON-NLS-1$

        StringBuilder buffer = new StringBuilder();
        for (String string : parts)
        {
            if (buffer.length() > 0)
                buffer.append(' ');
            buffer.append(Character.toUpperCase(string.charAt(0)));
            buffer.append(string.substring(1));
        }
        return buffer.toString();
    }
}
