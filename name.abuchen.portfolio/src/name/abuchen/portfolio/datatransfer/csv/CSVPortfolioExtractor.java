package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

/* package */class CSVPortfolioExtractor extends BaseCSVExtractor
{
    /* package */ CSVPortfolioExtractor(Client client)
    {
        super(client, Messages.CSVDefPortfolio);

        var fields = getFields();
        fields.add(new DateField(FieldCode.DATE, Messages.CSVColumn_DateValue).setOptional(true));
        fields.add(new Field(FieldCode.TIME, Messages.CSVColumn_Time).setOptional(true));

        fields.add(new ISINField(FieldCode.ISIN, Messages.CSVColumn_ISIN).setOptional(true));
        fields.add(new Field(FieldCode.TICKER, Messages.CSVColumn_TickerSymbol).setOptional(true));
        fields.add(new Field(FieldCode.WKN, Messages.CSVColumn_WKN).setOptional(true));
        fields.add(new Field(FieldCode.NAME, Messages.CSVColumn_SecurityName).setOptional(true));

        fields.add(new AmountField(FieldCode.VALUE, Messages.CSVColumn_Value));
        fields.add(new Field(FieldCode.CURRENCY, Messages.CSVColumn_Currency).setOptional(true));

        fields.add(new AmountField(FieldCode.SHARES, Messages.CSVColumn_Shares));
        fields.add(new Field(FieldCode.NOTE, Messages.CSVColumn_Note).setOptional(true));

        fields.add(new DateField(FieldCode.DATE_QUOTE, Messages.CSVColumn_DateQuote).setOptional(true));
        fields.add(new AmountField(FieldCode.QUOTE, Messages.CSVColumn_Quote, "Schluss", "Schlusskurs", "Close") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        .setOptional(true));
        fields.add(new Field(FieldCode.ACCOUNT, Messages.CSVColumn_AccountName).setOptional(true));
        fields.add(new Field(FieldCode.PORTFOLIO, Messages.CSVColumn_PortfolioName).setOptional(true));
    }

    @Override
    public String getCode()
    {
        return "portfolio"; //$NON-NLS-1$
    }

    @Override
    void extract(List<Item> items, String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        // check if we have a security
        var security = getSecurity(rawValues, field2column, s -> {
            var currency = getText(FieldCode.CURRENCY, rawValues, field2column);
            if (currency != null)
            {
                var unit = CurrencyUnit.getInstance(currency.trim());
                s.setCurrencyCode(unit == null ? getClient().getBaseCurrency() : unit.getCurrencyCode());
            }
        });

        if (security == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                            new StringJoiner(", ").add(Messages.CSVColumn_ISIN).add(Messages.CSVColumn_TickerSymbol) //$NON-NLS-1$
                                            .add(Messages.CSVColumn_WKN).toString()),
                            0);

        // check for valuation (either current or historic)
        var valuation = getMoney(FieldCode.VALUE, FieldCode.CURRENCY, rawValues, field2column);

        // check for the number of shares
        var shares = getShares(FieldCode.SHARES, rawValues, field2column);
        if (shares == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Shares),
                            0);

        // determine remaining fields
        var date = getDate(FieldCode.DATE, FieldCode.TIME, rawValues, field2column);
        if (date == null)
            date = LocalDate.now().atStartOfDay();

        var note = getText(FieldCode.NOTE, rawValues, field2column);

        var account = getAccount(getClient(), rawValues, field2column);
        var portfolio = getPortfolio(getClient(), rawValues, field2column);

        var entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setAmount(Math.abs(valuation.getAmount()));
        entry.setCurrencyCode(valuation.getCurrencyCode());
        entry.setShares(shares);
        entry.setNote(note);

        var item = new BuySellEntryItem(entry);

        item.setAccountPrimary(account);
        item.setPortfolioPrimary(portfolio);

        items.add(item);

        // check if the data contains price

        getSecurityPrice(FieldCode.DATE_QUOTE, rawValues, field2column)
                        .ifPresent(price -> items.add(new SecurityPriceItem(security, price)));
    }
}
