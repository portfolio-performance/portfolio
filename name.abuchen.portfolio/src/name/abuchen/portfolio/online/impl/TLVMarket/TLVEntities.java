package name.abuchen.portfolio.online.impl.TLVMarket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.util.WebAccess;

public class TLVEntities
{
    private final String URL = "api.tase.co.il"; //$NON-NLS-1$
    private final String PATH = "/api/content/searchentities"; //$NON-NLS-1$

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

    public Optional<List<IndiceListing>> getAllListings(Language lang) throws IOException
    {
        return responsetoEntitiesList(rpcAllIndices(lang));
    }

    @VisibleForTesting
    public String rpcAllIndices(Language lang) throws IOException
    {
        // https://api.tase.co.il/api/content/searchentities?lang=1
        final int RETRY_TIMES = 5;
        int times_tried = 0;
        String response = null;

        while (times_tried < RETRY_TIMES)
        {
            try
            {
                response = new WebAccess(URL, PATH)
                                .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL 7.0.6.01001") //$NON-NLS-1$
                                .addParameter("lang", lang.toString()) //$NON-NLS-1$
                                .addHeader("Accept", "*/*") //$NON-NLS-1$ //$NON-NLS-2$
                                .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$ //$NON-NLS-2$
                                .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
                                .addHeader("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                                .get();
                return response;

            }
            catch (ConnectException e)
            {
                times_tried++;
            }
            catch (IOException e)
            {
                times_tried++;
            }
        }
        return response;
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

    private Optional<List<IndiceListing>> responsetoEntitiesList(String response)
    {
        Gson gson = new Gson();
        
        Type IndiceListingType = new TypeToken<List<IndiceListing>>()
        {
        }.getType();


        try
        {
            List<IndiceListing> list = gson.fromJson(response, IndiceListingType);


            return Optional.of(list);
        }
        catch (Exception e)
        {
            PortfolioLog.error(e);
            return Optional.empty();
        }
    }
}
