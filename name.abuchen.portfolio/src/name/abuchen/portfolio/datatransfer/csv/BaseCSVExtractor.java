package name.abuchen.portfolio.datatransfer.csv;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/* package */ abstract class BaseCSVExtractor extends CSVExtractor
{
    private Client client;
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
        // careful: the security cache makes the extractor stateful because
        // securities extracted during a previous run will not be created again
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

        results.addAll(securityCache.createMissingSecurityItems(results));

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
        return getMoney(Messages.CSVColumn_Value, Messages.CSVColumn_TransactionCurrency, rawValues, field2column);
    }

    protected Money getMoney(String value, String currency, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        Long amount = getAmount(value, rawValues, field2column);
        if (amount == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Value), 0);
        String currencyCode = getCurrencyCode(currency, rawValues, field2column);
        return Money.of(currencyCode, amount);
    }

    protected Optional<SecurityPrice> getSecurityPrice(String dateField, String[] rawValues,
                    Map<String, Column> field2column) throws ParseException
    {
        Long amount = getQuote(Messages.CSVColumn_Quote, rawValues, field2column);
        if (amount == null)
            return Optional.empty();

        LocalDateTime date = getDate(dateField, null, rawValues, field2column);
        if (date == null)
            date = LocalDate.now().atStartOfDay();

        return Optional.of(new SecurityPrice(date.toLocalDate(), Math.abs(amount)));
    }
}
