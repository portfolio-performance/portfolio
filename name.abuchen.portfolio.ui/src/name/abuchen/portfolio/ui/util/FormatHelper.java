package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.google.common.base.Strings;

import name.abuchen.portfolio.money.Values;

public class FormatHelper
{
    private static final NumberFormat sharesFormat = new DecimalFormat("#,##0.###"); //$NON-NLS-1$
    private static String sharesDecimalPlaceholder = "000"; //$NON-NLS-1$

    private static final NumberFormat quoteFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    private FormatHelper()
    {
    }

    public static void setSharesDisplayPrecision(int precision)
    {
        // only maximum, hiding trailing zeroes
        sharesFormat.setMaximumFractionDigits(precision);
        sharesDecimalPlaceholder = Strings.repeat("0", precision); //$NON-NLS-1$
    }

    public static NumberFormat getSharesFormat()
    {
        return sharesFormat;
    }

    public static String getSharesDecimalPartPlaceholder()
    {
        return sharesDecimalPlaceholder;
    }

    public static void setQuoteDisplayPrecision(int precision)
    {
        // both min and max to show trailing zeroes
        quoteFormat.setMinimumFractionDigits(precision);
        quoteFormat.setMaximumFractionDigits(precision);
    }

    public static String format(String currency, Long quote, String skipCurrency)
    {
        String value = quoteFormat.format(quote / Values.Quote.divider());
        if (currency == null || currency.equals(skipCurrency))
        {
            return value;
        }
        else
        {
            return currency + " " + value; //$NON-NLS-1$
        }
    }
}
