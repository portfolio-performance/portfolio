package name.abuchen.portfolio.money;

import java.text.DecimalFormat;

import name.abuchen.portfolio.money.Values.QuoteValues;

public class ValuesBuilder
{
    public static <T extends Number> Values<T> createNumberValues(Values<T> orgValues)
    {
        return new Values<T>(orgValues.pattern(), orgValues.precision())
        {
            private final DecimalFormat format = new DecimalFormat(pattern());

            @Override
            public String format(Number share)
            {
                if (DiscreetMode.isActive())
                    return DiscreetMode.HIDDEN_AMOUNT;
                else
                    return format.format(share.doubleValue() / divider());
            }
        };
    }

    public static <T extends Number> Values<T> createNumberValues(String pattern, int precision)
    {
        return new Values<T>(pattern, precision)
        {

            @Override
            public String format(T amount)
            {
                DecimalFormat df = new DecimalFormat(pattern);
                return df.format(amount);
            }
        };
    }

    public static void initQuoteValuesDecimalFormat()
    {
        QuoteValues.QUOTE_FORMAT.set(new DecimalFormat(QuoteValues.QUOTE_PATTERN));
    }
}
