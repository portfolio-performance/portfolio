package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * India's First Free Mutual Fund API
 */
public class MFAPIQuoteFeed implements QuoteFeed
{
    public static final String ID = "MFAPI"; //$NON-NLS-1$
    public static final String SCHEME_CODE = "SCHEMECODE"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "India's First Free Mutual Fund API (www.mfapi.in)"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        return Optional.empty();
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true);
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        Optional<String> schemeCode = security.getPropertyValue(SecurityProperty.Type.FEED, SCHEME_CODE);
        if (schemeCode.isEmpty())
        {
            return QuoteFeedData.withError(new IOException(MessageFormat
                            .format(Messages.MsgErrorMissingIdentifierForSecurity, "Scheme Code", security.getName()))); //$NON-NLS-1$
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            // https://api.mfapi.in/mf/119551
            WebAccess webaccess = new WebAccess("api.mfapi.in", "/mf/" + schemeCode.get()); //$NON-NLS-1$ //$NON-NLS-2$
            String content = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), content);

            JSONObject json = (JSONObject) JSONValue.parse(content);
            if (json != null)
            {
                var dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US); //$NON-NLS-1$

                JSONArray prices = (JSONArray) json.get("data"); //$NON-NLS-1$
                for (Object obj : prices)
                {
                    JSONObject price = (JSONObject) obj;

                    LatestSecurityPrice p = new LatestSecurityPrice();
                    p.setDate(LocalDate.parse((String) price.get("date"), dateFormat)); //$NON-NLS-1$
                    p.setValue(YahooHelper.asPrice((String) price.get("nav"))); //$NON-NLS-1$
                    p.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                    p.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                    p.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                    data.addPrice(p);
                }
            }

        }
        catch (IOException | URISyntaxException | ParseException e)
        {
            data.addError(e);
        }

        return data;
    }
}
