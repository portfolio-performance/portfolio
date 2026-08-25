package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/* package */class CSVPortfolioTransactionExtractor extends BaseCSVExtractor
{
    /* package */ CSVPortfolioTransactionExtractor(Client client)
    {
        super(client, Messages.CSVDefPortfolioTransactions);

        var fields = getFields();
        fields.add(new DateField(FieldCode.DATE, Messages.CSVColumn_Date));
        fields.add(new Field(FieldCode.TIME, Messages.CSVColumn_Time).setOptional(true));
        fields.add(new ISINField(FieldCode.ISIN, Messages.CSVColumn_ISIN).setOptional(true));
        fields.add(new Field(FieldCode.TICKER, Messages.CSVColumn_TickerSymbol).setOptional(true));
        fields.add(new Field(FieldCode.WKN, Messages.CSVColumn_WKN).setOptional(true));
        fields.add(new Field(FieldCode.NAME, Messages.CSVColumn_SecurityName).setOptional(true));
        fields.add(new AmountField(FieldCode.VALUE, Messages.CSVColumn_Value));
        fields.add(new Field(FieldCode.CURRENCY, Messages.CSVColumn_TransactionCurrency).setOptional(true));
        fields.add(new AmountField(FieldCode.FEES, Messages.CSVColumn_Fees).setOptional(true));
        fields.add(new AmountField(FieldCode.TAXES, Messages.CSVColumn_Taxes).setOptional(true));
        fields.add(new AmountField(FieldCode.GROSS, Messages.CSVColumn_GrossAmount).setOptional(true));
        fields.add(new Field(FieldCode.CURRENCY_GROSS, Messages.CSVColumn_CurrencyGrossAmount).setOptional(true));
        fields.add(new AmountField(FieldCode.EXCHANGE_RATE, Messages.CSVColumn_ExchangeRate).setOptional(true));
        fields.add(new AmountField(FieldCode.SHARES, Messages.CSVColumn_Shares));
        fields.add(new EnumField<PortfolioTransaction.Type>(FieldCode.TYPE, Messages.CSVColumn_Type, Type.class)
                        .setOptional(true));
        fields.add(new Field(FieldCode.NOTE, Messages.CSVColumn_Note).setOptional(true));
        fields.add(new Field(FieldCode.ACCOUNT, Messages.CSVColumn_AccountName).setOptional(true));
        fields.add(new Field(FieldCode.PORTFOLIO, Messages.CSVColumn_PortfolioName).setOptional(true));
        fields.add(new Field(FieldCode.PORTFOLIO_2ND, Messages.CSVColumn_PortfolioName2nd).setOptional(true));
    }

    @Override
    public String getCode()
    {
        return "portfolio-transaction"; //$NON-NLS-1$
    }

    @Override
    void extract(List<Item> items, String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        // if forex gross amount is available then assume that is the currency
        // of the security to be created

        // check if we have a security
        var security = getSecurity(rawValues, field2column, s -> {
            var currency = getText(FieldCode.CURRENCY_GROSS, rawValues, field2column);
            if (currency == null || currency.isEmpty())
                currency = getText(FieldCode.CURRENCY, rawValues, field2column);

            if (currency != null)
            {
                var unit = CurrencyUnit.getInstance(currency.trim());
                s.setCurrencyCode(unit == null ? getClient().getBaseCurrency() : unit.getCurrencyCode());
            }
        });

        if (security == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                            new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                            .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                            .toString()),
                            0);

        // check for the transaction amount
        var amount = getMoney(rawValues, field2column);

        // determine type (if not explicitly given by import)
        var type = inferType(rawValues, field2column, amount);

        // determine remaining fields
        var date = getDate(FieldCode.DATE, FieldCode.TIME, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);

        var shares = getShares(FieldCode.SHARES, rawValues, field2column);
        var fees = getAmount(FieldCode.FEES, rawValues, field2column);
        var taxes = getAmount(FieldCode.TAXES, rawValues, field2column);
        var note = getText(FieldCode.NOTE, rawValues, field2column);

        Optional<Unit> grossAmount = extractGrossAmount(rawValues, field2column, amount);

        var account = getAccount(getClient(), rawValues, field2column);
        var portfolio = getPortfolio(getClient(), rawValues, field2column);
        var portfolio2nd = getPortfolio(getClient(), rawValues, field2column, true);

        var item = switch (type)
        {
            case BUY, SELL -> //
                            createBuySell(rawValues, field2column, type, security, amount, fees, taxes, date, note,
                                            shares, grossAmount);
            case TRANSFER_IN, TRANSFER_OUT -> //
                            createTransfer(security, amount, date, note, shares);
            case DELIVERY_INBOUND, DELIVERY_OUTBOUND -> //
                            createDelivery(rawValues, field2column, type, security, amount, fees, taxes, date, note,
                                            shares, grossAmount);
            default ->
                throw new IllegalArgumentException(type.toString());
        };

        item.setAccountPrimary(account);
        item.setPortfolioPrimary(portfolio);
        item.setPortfolioSecondary(portfolio2nd);

        items.add(item);
    }

    private Item createBuySell(String[] rawValues, Map<String, Column> field2column, Type type, Security security,
                    Money amount, Long fees, Long taxes, LocalDateTime date, String note, Long shares,
                    Optional<Unit> grossAmount) throws ParseException
    {
        var entry = new BuySellEntry();
        entry.setType(type);
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setAmount(Math.abs(amount.getAmount()));
        entry.setCurrencyCode(amount.getCurrencyCode());
        entry.setShares(shares);
        entry.setNote(note);

        if (grossAmount.isPresent())
            entry.getPortfolioTransaction().addUnit(grossAmount.get());

        if (fees != null && fees.longValue() != 0)
            entry.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE, Money.of(amount.getCurrencyCode(), Math.abs(fees))));

        if (taxes != null && taxes.longValue() != 0)
            entry.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX, Money.of(amount.getCurrencyCode(), Math.abs(taxes))));

        if (!grossAmount.isPresent())
            createGrossValueIfNecessary(rawValues, field2column, entry.getPortfolioTransaction());

        ExtractorUtils.fixGrossValueBuySell().accept(entry);
        
        return new BuySellEntryItem(entry);
    }

    private Item createTransfer(Security security, Money amount, LocalDateTime date, String note, Long shares)
    {
        var entry = new PortfolioTransferEntry();
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setAmount(Math.abs(amount.getAmount()));
        entry.setCurrencyCode(amount.getCurrencyCode());
        entry.setShares(shares);
        entry.setNote(note);

        return new PortfolioTransferItem(entry);
    }

    private Item createDelivery(String[] rawValues, Map<String, Column> field2column, Type type, Security security,
                    Money amount, Long fees, Long taxes, LocalDateTime date, String note, Long shares,
                    Optional<Unit> grossAmount) throws ParseException
    {
        var t = new PortfolioTransaction();

        t.setType(type);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(Math.abs(amount.getAmount()));
        t.setCurrencyCode(amount.getCurrencyCode());
        t.setShares(shares);
        t.setNote(note);

        if (grossAmount.isPresent())
            t.addUnit(grossAmount.get());

        if (fees != null && fees.longValue() != 0)
            t.addUnit(new Unit(Unit.Type.FEE, Money.of(amount.getCurrencyCode(), Math.abs(fees))));

        if (taxes != null && taxes.longValue() != 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(amount.getCurrencyCode(), Math.abs(taxes))));

        if (!grossAmount.isPresent())
            createGrossValueIfNecessary(rawValues, field2column, t);

        ExtractorUtils.fixGrossValue().accept(t);
       
        return new TransactionItem(t);
    }

    private Type inferType(String[] rawValues, Map<String, Column> field2column, Money amount) throws ParseException
    {
        var type = getEnum(FieldCode.TYPE, PortfolioTransaction.Type.class, rawValues, field2column);
        if (type == null)
            type = amount.isNegative() ? Type.BUY : Type.SELL;
        return type;
    }
}
