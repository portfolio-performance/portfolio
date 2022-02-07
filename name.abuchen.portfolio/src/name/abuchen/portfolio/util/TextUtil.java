package name.abuchen.portfolio.util;

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

        // add a space to correctly match a full line
        String raw = text + " "; //$NON-NLS-1$

        StringBuilder wrapped = new StringBuilder();
        Pattern p = Pattern.compile(".{0,80}[ \\t\\n\\x0b\\r\\f,.]+"); //$NON-NLS-1$
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

        // remove added space used for line breaking
        return wrapped.substring(0, wrapped.length() - 1);
    }

    public static final String tooltip(String text)
    {
        return text == null ? null : text.replace("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static final String sanitizeFilename(String filename)
    {
        // filter ? \ / : | < > // *
        filename = filename.replaceAll("[\\?\\\\/:|<>\\*]", " "); //$NON-NLS-1$ //$NON-NLS-2$
        // replace multiple spaces
        filename = filename.replaceAll("\\s+", "_"); //$NON-NLS-1$ //$NON-NLS-2$
        return filename;
    }

    /**
     * Since {@see String#trim} does not trim all whitespace and space
     * characters, this is an alternative implementation. Inspired by the blog
     * post at http://closingbraces.net/2008/11/11/javastringtrim/
     */
    public static String trim(String value)
    {
        if (value == null)
            return null;

        int len = value.length();
        int st = 0;

        while ((st < len) && isWhitespace(value.charAt(st)))
        {
            st++;
        }

        while ((st < len) && Character.isWhitespace(value.charAt(len - 1)))
        {
            len--;
        }
        return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;

    }

    public static boolean isWhitespace(char c)
    {
        if (Character.isWhitespace(c) || Character.isSpaceChar(c))
            return true;

        return c == '\uFEFF'; // zero width no-break space
    }

    /**
     * Strips all whitespace and space characters using {@link #strip} from all
     * values of the array.
     */
    public static String[] trim(String[] values)
    {
        if (values == null)
            return new String[0];

        String[] answer = new String[values.length];

        for (int i = 0; i < values.length; i++)
            answer[i] = TextUtil.trim(values[i]);

        return answer;
    }

    /**
     * Removes all blanks from the given string.
     */
    public static String stripBlanks(String input)
    {
        return input == null ? null : Pattern.compile("\\s").matcher(input).replaceAll(""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Remove all blanks followed by underscores from the given string.
     */
    public static String stripBlanksAndUnderscores(String input)
    {
        return input == null ? null : Pattern.compile("[\\s_]").matcher(input).replaceAll(""); //$NON-NLS-1$ //$NON-NLS-2$
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

    public static String stripJavaScriptCallback(String json)
    {
        if (json == null)
            return null;

        final int length = json.length();
        final int search = 200; // only check the first 200 characters

        int start = 0;
        int end = length;

        for (; start < length && start < search; start++)
        {
            char c = json.charAt(start);
            if (c == '{' || c == '[')
                break;
        }

        for (; end > start && end > length - search; end--)
        {
            char c = json.charAt(end - 1);
            if (c == '}' || c == ']')
                break;
        }

        // remove only if
        // a) start is before end
        // b) the limit of 200 characters to search has not been hit
        // c) a prefix *and* a postfix have been found

        if (start < end && start < search && end > length - search && (start > 0 && end < length))
            return json.substring(start, end);

        return json;
    }
}
