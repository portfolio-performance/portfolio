package name.abuchen.portfolio.ui.util.format;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import name.abuchen.portfolio.money.Values;

public class AmountNumberFormat extends Format
{
    private static final long serialVersionUID = 1L;

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        if (!(obj instanceof Number))
            throw new IllegalArgumentException();

        return toAppendTo.append(Values.Amount.format(Values.Amount.factorize(((Number) obj).doubleValue())));
    }

    @Override
    public Object parseObject(String source, ParsePosition pos)
    {
        pos.setErrorIndex(0);
        return null;
    }
}