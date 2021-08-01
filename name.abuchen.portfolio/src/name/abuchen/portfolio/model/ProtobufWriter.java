package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;

import com.google.protobuf.Timestamp;

import name.abuchen.portfolio.model.ClientFactory.ClientPersister;
import name.abuchen.portfolio.model.protos.PClient;
import name.abuchen.portfolio.model.protos.PLatestSecurityPrice;
import name.abuchen.portfolio.model.protos.PSecurity;
import name.abuchen.portfolio.model.protos.PSecurityPrice;

/* package */ class ProtobufWriter implements ClientPersister
{
    @Override
    public Client load(InputStream input) throws IOException
    {
        PClient newClient = PClient.parseFrom(input);

        Client client = new Client();

        for (PSecurity newSecurity : newClient.getSecuritiesList())
        {
            Security security = new Security(newSecurity.getUuid());

            if (newSecurity.hasOnlineId())
                security.setOnlineId(newSecurity.getOnlineId());

            security.setName(newSecurity.getName());
            if (newSecurity.hasCurrencyCode())
                security.setCurrencyCode(newSecurity.getCurrencyCode());
            if (newSecurity.hasTargetCurrencyCode())
                security.setTargetCurrencyCode(newSecurity.getTargetCurrencyCode());

            if (newSecurity.hasNote())
                security.setNote(newSecurity.getNote());

            if (newSecurity.hasIsin())
                security.setIsin(newSecurity.getIsin());
            if (newSecurity.hasTickerSymbol())
                security.setTickerSymbol(newSecurity.getTickerSymbol());
            if (newSecurity.hasWkn())
                security.setWkn(newSecurity.getWkn());
            if (newSecurity.hasCalendar())
                security.setCalendar(newSecurity.getCalendar());

            if (newSecurity.hasFeed())
                security.setFeed(newSecurity.getFeed());
            if (newSecurity.hasFeedURL())
                security.setFeedURL(newSecurity.getFeedURL());

            security.addAllPrices(newSecurity.getPricesList().stream()
                            .map(p -> new SecurityPrice(LocalDate.ofEpochDay(p.getDate()), p.getClose()))
                            .collect(Collectors.toList()));

            if (newSecurity.hasLatestFeed())
                security.setLatestFeed(newSecurity.getLatestFeed());
            if (newSecurity.hasLatestFeedURL())
                security.setLatestFeedURL(newSecurity.getLatestFeedURL());

            if (newSecurity.hasLatest())
            {
                PLatestSecurityPrice latest = newSecurity.getLatest();
                security.setLatest(new LatestSecurityPrice(LocalDate.ofEpochDay(latest.getDate()), latest.getClose(),
                                latest.getHigh(), latest.getLow(), latest.getVolume()));
            }

            security.setRetired(newSecurity.getIsRetired());

            Timestamp ts = newSecurity.getUpdatedAt();
            security.setUpdatedAt(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()));

            client.addSecurity(security);
        }

        return client;
    }

    @Override
    public void save(Client client, OutputStream output) throws IOException
    {
        PClient.Builder newClient = PClient.newBuilder();

        for (Security security : client.getSecurities())
        {
            PSecurity.Builder newSecurity = PSecurity.newBuilder();
            newSecurity.setUuid(security.getUUID());

            if (security.getOnlineId() != null)
                newSecurity.setOnlineId(security.getOnlineId());

            newSecurity.setName(security.getName());
            if (security.getCurrencyCode() != null)
                newSecurity.setCurrencyCode(security.getCurrencyCode());
            if (security.getTargetCurrencyCode() != null)
                newSecurity.setTargetCurrencyCode(security.getTargetCurrencyCode());

            if (security.getNote() != null)
                newSecurity.setNote(security.getNote());

            if (security.getIsin() != null)
                newSecurity.setIsin(security.getIsin());
            if (security.getTickerSymbol() != null)
                newSecurity.setTickerSymbol(security.getTickerSymbol());
            if (security.getWkn() != null)
                newSecurity.setWkn(security.getWkn());
            if (security.getCalendar() != null)
                newSecurity.setCalendar(security.getCalendar());

            if (security.getFeed() != null)
                newSecurity.setFeed(security.getFeed());
            if (security.getFeedURL() != null)
                newSecurity.setFeedURL(security.getFeedURL());

            for (SecurityPrice price : security.getPrices())
            {
                newSecurity.addPrices(PSecurityPrice.newBuilder().setDate(price.getDate().toEpochDay())
                                .setClose(price.getValue()).build());
            }

            if (security.getLatestFeed() != null)
                newSecurity.setLatestFeed(security.getLatestFeed());
            if (security.getLatestFeedURL() != null)
                newSecurity.setLatestFeedURL(security.getLatestFeedURL());

            LatestSecurityPrice latest = security.getLatest();
            if (latest != null)
            {
                newSecurity.setLatest(PLatestSecurityPrice.newBuilder() //
                                .setDate(latest.getDate().toEpochDay()) //
                                .setClose(latest.getValue()) //
                                .setHigh(latest.getHigh()) //
                                .setLow(latest.getLow()) //
                                .setVolume(latest.getVolume()) //
                                .build());
            }

            newSecurity.setIsRetired(security.isRetired());

            Instant updatedAt = security.getUpdatedAt();
            newSecurity.setUpdatedAt(Timestamp.newBuilder().setSeconds(updatedAt.getEpochSecond())
                            .setNanos(updatedAt.getNano()).build());

            newClient.addSecurities(newSecurity.build());
        }

        newClient.build().writeTo(output);
    }
}
