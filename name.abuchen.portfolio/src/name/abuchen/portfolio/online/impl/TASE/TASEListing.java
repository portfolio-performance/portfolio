package name.abuchen.portfolio.online.impl.TASE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

public abstract class TASEListing
{

    static final ThreadLocal<DecimalFormat> FMT_PRICE = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            DecimalFormat fmt = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
            fmt.setParseBigDecimal(true);
            return fmt;
        }
    };
    /**
     * Calculate the first date to request historical quotes for.
     */
    public LocalDate caculateStart(Security security)
    {
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            return lastHistoricalQuote.getDate();
        }
        else
        {
            return LocalDate.now().minusMonths(3);
        }
    }


    protected LocalDate asDate(String s)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // $NON-NLS-1$

        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return LocalDate.parse(dt, formatter); // $NON-NLS-1$
    }

    protected static long asLong(String value)
    {
        if (value == null)
            return 0;
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    protected static long convertILS(long price, String quoteCurrency, String securityCurrency)
    {
        if (quoteCurrency != null)
        {
            if ("ILA".equals(quoteCurrency) && "ILS".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price / 100;
            if ("ILS".equals(quoteCurrency) && "ILA".equals(securityCurrency)) //$NON-NLS-1$ //$NON-NLS-2$
                return price * 100;
        }
        return price;
    }

    protected long asPrice(String s)
    {
        try
        {
            return asPrice(s, BigDecimal.ONE);
        }
        catch (ParseException e)
        {
            return -1l;
        }
    }

    protected static long asPrice(String s, BigDecimal factor) throws ParseException
    {
        if ("N/A".equals(s) || "null".equals(s) || "NaN".equals(s) || ".".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return LatestSecurityPrice.NOT_AVAILABLE;
        BigDecimal v = (BigDecimal) FMT_PRICE.get().parse(s);
        return v.multiply(factor).multiply(Values.Quote.getBigDecimalFactor()).setScale(0, RoundingMode.HALF_UP)
                        .longValue();
    }

    protected static int asNumber(String s) throws ParseException
    {
        if ("N/A".equals(s) || "null".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$
            return -1;
        return FMT_PRICE.get().parse(s).intValue();
    }

    protected double roundQuoteValue(double value)
    {
        return Math.round(value * 10000) / 10000d;
    }



    abstract protected Optional<String> getQuoteCurrency(Security security);

}
