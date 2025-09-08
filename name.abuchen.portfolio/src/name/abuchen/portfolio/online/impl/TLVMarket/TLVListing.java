package name.abuchen.portfolio.online.impl.TLVMarket;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

public class TLVListing
{

    /**
     * Calculate the first date to request historical quotes for.
     */
    /* package */final LocalDate caculateStart(Security security)
    {
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            return lastHistoricalQuote.getDate();
        }
        else
        {
            return LocalDate.of(1900, 1, 1);
        }
    }


    // protected long DoubletoLong(Object item, String str, Optional<String>
    // quoteCurrency, String securityCurrency)
    // {
    // Double doublevalue = null;
    // Long longValue = null;
    //
    // try
    // {
    // doublevalue = (Double) item;
    // }
    // catch (IndexOutOfBoundsException e)
    // {
    // //
    // }
    // catch (ClassCastException e)
    // {
    // longValue = (Long) item.get(str);
    // if (longValue != null && longValue > 0l)
    // {
    // long convertedprice =
    // convertILS(Values.Quote.factorize(roundQuoteValue(longValue)),
    // quoteCurrency.orElse(null), securityCurrency);
    // longValue = convertedprice;
    // }
    // }
    //
    // if (doublevalue != null && doublevalue.doubleValue() > 0)
    // {
    // long convertedprice =
    // convertILS(Values.Quote.factorize(roundQuoteValue(doublevalue)),
    // quoteCurrency.orElse(null), securityCurrency);
    // longValue = convertedprice;
    // }
    // else
    // {
    // longValue = LatestSecurityPrice.NOT_AVAILABLE;
    // }
    // return longValue;
    //
    // }

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

    protected double roundQuoteValue(double value)
    {
        return Math.round(value * 10000) / 10000d;
    }
}
