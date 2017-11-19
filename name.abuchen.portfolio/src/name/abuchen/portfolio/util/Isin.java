package name.abuchen.portfolio.util;

public class Isin
{
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"; //$NON-NLS-1$
    public static final String PATTERN = "[A-Z]{2}[A-Z0-9]{9}\\d"; //$NON-NLS-1$

    private Isin()
    {}

    public static final boolean isValid(String isin) // NOSONAR
    {
        if (isin == null || isin.length() != 12)
            return false;

        int sum = 0;
        boolean even = false;

        for (int ii = 11; ii >= 0; ii--)
        {
            int v = CHARACTERS.indexOf(isin.charAt(ii));
            if (v < 0)
                return false;

            int digit = v % 10 * (even ? 2 : 1);
            sum += digit > 9 ? digit - 9 : digit;
            even = !even;

            if (v >= 10)
            {
                digit = v / 10 * (even ? 2 : 1);
                sum += digit > 9 ? digit - 9 : digit;
                even = !even;
            }
        }

        return sum % 10 == 0;
    }
}
