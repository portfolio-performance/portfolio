package name.abuchen.portfolio.util;

public final class Strings
{

    private Strings()
    {}

    /**
     * Since {@see String#trim} does not trim all whitespace and space
     * characters, this is an alternative implementation. Inspired by the blog
     * post at http://closingbraces.net/2008/11/11/javastringtrim/
     */
    public static String strip(String value)
    {
        int len = value.length();
        int st = 0;

        while ((st < len) && (Character.isWhitespace(value.charAt(st)) || Character.isSpaceChar(value.charAt(st))))
        {
            st++;
        }

        while ((st < len)
                        && (Character.isWhitespace(value.charAt(len - 1)) || Character.isSpaceChar(value
                                        .charAt(len - 1))))
        {
            len--;
        }
        return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;

    }

}
