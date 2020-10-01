package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.google.common.base.Strings;

public class FormatHelper
{
    private static final NumberFormat sharesFormat = new DecimalFormat("#,##0.###"); //$NON-NLS-1$
    private static String sharesDecimalPlaceholder = "000"; //$NON-NLS-1$

    private FormatHelper()
    {
    }

    public static void setSharesDisplayPrecision(int precision)
    {
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
}
