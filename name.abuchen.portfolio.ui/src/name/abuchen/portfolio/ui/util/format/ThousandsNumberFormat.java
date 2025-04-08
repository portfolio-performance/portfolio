package name.abuchen.portfolio.ui.util.format;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.money.Values;

public class ThousandsNumberFormat extends Format
{
    private static final long serialVersionUID = 1L;

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        if (!(obj instanceof Number))
            throw new IllegalArgumentException("object must be a subclass of Number"); //$NON-NLS-1$

        if (DiscreetMode.isActive())
            toAppendTo.append(DiscreetMode.HIDDEN_AMOUNT);
        else
            toAppendTo.append(Values.Thousands.format(((Number) obj).doubleValue()));

        return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos)
    {
        pos.setErrorIndex(0);
        return null;
    }
}