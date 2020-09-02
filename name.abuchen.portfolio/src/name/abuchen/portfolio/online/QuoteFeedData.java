package name.abuchen.portfolio.online;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityPrice;

public class QuoteFeedData
{
    public static class RawResponse
    {
        private final String url;
        private final String content;

        public RawResponse(String url, String content)
        {
            super();
            this.url = url;
            this.content = content;
        }

        public String getUrl()
        {
            return url;
        }

        public String getContent()
        {
            return content;
        }
    }

    private final List<LatestSecurityPrice> prices = new ArrayList<>();
    private final List<Exception> errors = new ArrayList<>();
    private final List<RawResponse> responses = new ArrayList<>();

    public static QuoteFeedData withError(Exception error)
    {
        QuoteFeedData data = new QuoteFeedData();
        data.getErrors().add(error);
        return data;
    }

    public void addPrice(LatestSecurityPrice price)
    {
        this.prices.add(price);
    }

    public void addAllPrices(Collection<LatestSecurityPrice> prices)
    {
        this.prices.addAll(prices);
    }

    public List<LatestSecurityPrice> getLatestPrices()
    {
        return prices;
    }

    public List<SecurityPrice> getPrices()
    {
        return Collections.unmodifiableList(prices.stream().map(p -> new SecurityPrice(p.getDate(), p.getValue()))
                        .collect(Collectors.toList()));
    }

    public void addError(Exception error)
    {
        this.errors.add(error);
    }

    public List<Exception> getErrors()
    {
        return errors;
    }

    public void addResponse(String url, String content)
    {
        this.responses.add(new RawResponse(url, content));
    }

    public List<RawResponse> getResponses()
    {
        return responses;
    }
}
