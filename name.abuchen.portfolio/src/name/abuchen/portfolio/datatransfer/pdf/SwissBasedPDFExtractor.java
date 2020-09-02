package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

public abstract class SwissBasedPDFExtractor extends AbstractPDFExtractor
{
    private final DecimalFormat swissNumberFormat;

    public SwissBasedPDFExtractor(Client client)
    {
        super(client);

        swissNumberFormat = (DecimalFormat) DecimalFormat.getInstance(new Locale("de", "CH")); //$NON-NLS-1$ //$NON-NLS-2$
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        decimalFormatSymbols.setGroupingSeparator('\'');
        swissNumberFormat.setDecimalFormatSymbols(decimalFormatSymbols);

    }

    @Override
    protected long asAmount(String value)
    {
        return asValue(value, Values.Amount);
    }

    @Override
    protected long asShares(String value)
    {
        return asValue(value, Values.Share);
    }

    protected long asValue(String value, Values<Long> valueType)
    {
        try
        {
            return Math.abs(Math.round(swissNumberFormat.parse(value).doubleValue() * valueType.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        try
        {
            return BigDecimal.valueOf(swissNumberFormat.parse(value).doubleValue());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
