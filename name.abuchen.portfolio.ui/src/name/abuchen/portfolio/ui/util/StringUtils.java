package name.abuchen.portfolio.ui.util;

public enum StringUtils
{
    ;
    private StringUtils()
    {
    }

    /**
     * Check if a string is empty or null
     * 
     * @param s
     * @return
     */
    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    /**
     * Check if a string is not empty and not null
     * 
     * @param s
     * @return
     */
    public static boolean isNotEmpty(String s)
    {
        return !isEmpty(s);
    }

}
