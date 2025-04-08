package name.abuchen.portfolio.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Isin
{
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"; //$NON-NLS-1$
    @SuppressWarnings("nls")
    private static final Set<String> INVALID_CODES = new HashSet<>(
                    Arrays.asList("DU", "EV", "HF", "HS", "QS", "QT", "QU", "QY", "TE", "XF", "XX", "ZZ"));

    public static final String PATTERN = "[A-Z]{2}[A-Z0-9]{9}\\d"; //$NON-NLS-1$

    private Isin()
    {
    }

    public static final boolean isValid(String isin) // NOSONAR
    {
        if (isin == null || isin.length() != 12)
            return false;

        // do not calculate checksum for ISIN numbers where the first two digits
        // are considered invalid codes because some institutions use them
        // internally

        // see https://github.com/portfolio-performance/portfolio/issues/1761

        if (INVALID_CODES.contains(isin.substring(0, 2)))
            return true;

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
                sum += digit;
                even = !even;
            }
        }

        return sum % 10 == 0;
    }
}
