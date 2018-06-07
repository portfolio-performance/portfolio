package name.abuchen.portfolio.datatransfer.csv;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.PeerCache;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Peer;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/* package */ abstract class BaseCSVExtractor extends CSVExtractor
{
    private Client client;
    private PeerCache peerCache;
    private SecurityCache securityCache;
    private String label;
    private List<Field> fields;

    /* package */ BaseCSVExtractor(Client client, String label)
    {
        this.client = client;
        this.label = label;
        this.fields = new ArrayList<>();
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    @Override
    public final List<Field> getFields()
    {
        return fields;
    }

    @Override
    public final String toString()
    {
        return label;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public List<Item> extract(int skipLines, List<String[]> rawValues, Map<String, Column> field2column,
                    List<Exception> errors)
    {
        // careful: the peer/security cache makes the extractor stateful because
        // peers/securities extracted during a previous run will not be created again
        peerCache     = new PeerCache(client);
        securityCache = new SecurityCache(client);

        List<Item> results = new ArrayList<>();
        int lineNo = 1 + skipLines; // +1 because of end user
        for (String[] strings : rawValues)
        {
            try
            {
                extract(results, strings, field2column);
            }
            catch (ParseException | UnsupportedOperationException | IllegalArgumentException e)
            {
                errors.add(new IOException(MessageFormat.format(Messages.CSVLineXwithMsgY, lineNo, e.getMessage()), e));
            }
            lineNo++;
        }

        results.addAll(peerCache.createMissingPeerItems(results));
        results.addAll(securityCache.createMissingSecurityItems(results));

        peerCache = null;
        securityCache = null;

        return results;
    }

    /* package */ abstract void extract(List<Item> items, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException;

    protected Security getSecurity(String[] rawValues, Map<String, Column> field2column,
                    Consumer<Security> onSecurityCreated)
    {
        Security security = null;

        String isin = getISIN(Messages.CSVColumn_ISIN, rawValues, field2column);
        String tickerSymbol = getText(Messages.CSVColumn_TickerSymbol, rawValues, field2column);
        String wkn = getText(Messages.CSVColumn_WKN, rawValues, field2column);
        String name = getText(Messages.CSVColumn_SecurityName, rawValues, field2column);

        if (isin != null || tickerSymbol != null || wkn != null || name != null)
        {
            name = constructName(isin, tickerSymbol, wkn, name);
            security = securityCache.lookup(isin, tickerSymbol, wkn, name, () -> {
                Security s = new Security();
                s.setCurrencyCode(client.getBaseCurrency());

                onSecurityCreated.accept(s);

                return s;
            });
        }

        return security;
    }

    protected Peer getPeer(String[] rawValues, Map<String, Column> field2column, Consumer<Peer> onPeerCreated)
    {
        Peer peer = null;

        String iban = getIBAN(Messages.CSVColumn_IBAN, rawValues, field2column);
        String partnerName = getText(Messages.CSVColumn_PartnerName, rawValues, field2column);

        System.err.println(">> BaseCSVExtractor::getPeer");                                      // TODO: still needed for debug?
        if (iban != null)                                                                        // TODO: still needed for debug?
            System.err.println(">> BaseCSVExtractor::getPeer IBAN       : " + iban);             // TODO: still needed for debug?
        if (partnerName != null)                                                                 // TODO: still needed for debug?
            System.err.println(">> BaseCSVExtractor::getPeer partnerName: " + partnerName);      // TODO: still needed for debug?

        if (iban != null || partnerName != null)
        {
            peer = peerCache.lookup(iban, partnerName, () -> {
                System.err.println(">> BaseCSVExtractor [peerCache.lookup]"); // TODO: still needed for debug?
                Peer p = new Peer();
                onPeerCreated.accept(p);
                return p;
            });

        }
        return peer;
    }

    private String constructName(String isin, String tickerSymbol, String wkn, String name)
    {
        if (name != null && !name.isEmpty())
        {
            return name;
        }
        else
        {
            String key = isin != null ? isin : tickerSymbol != null ? tickerSymbol : wkn;
            return MessageFormat.format(Messages.CSVImportedSecurityLabel, key);
        }
    }

    protected String getCurrencyCode(String name, String[] rawValues, Map<String, Column> field2column)
    {
        String value = getText(name, rawValues, field2column);
        if (value == null)
            return client.getBaseCurrency();

        CurrencyUnit unit = CurrencyUnit.getInstance(value.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
    }

    protected Money getMoney(String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        Long amount = getAmount(Messages.CSVColumn_Value, rawValues, field2column);
        if (amount == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Value), 0);
        String currencyCode = getCurrencyCode(Messages.CSVColumn_TransactionCurrency, rawValues, field2column);
        return Money.of(currencyCode, amount);
    }
}
