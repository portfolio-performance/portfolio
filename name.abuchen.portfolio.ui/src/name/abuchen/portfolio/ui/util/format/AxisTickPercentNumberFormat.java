package name.abuchen.portfolio.ui.util.format;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class AxisTickPercentNumberFormat extends Format
{
    private String decimalFormat;
    private static final long serialVersionUID = 1L;

    public AxisTickPercentNumberFormat(String decimalFormat)
    {
        this.decimalFormat = decimalFormat;
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        if (!(obj instanceof Number))
            throw new IllegalArgumentException("object must be a subclass of Number"); //$NON-NLS-1$

        toAppendTo.append(new DecimalFormat(decimalFormat).format(((Number) obj).doubleValue()));

        return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos)
    {
        pos.setErrorIndex(0);
        return null;
    }
}
