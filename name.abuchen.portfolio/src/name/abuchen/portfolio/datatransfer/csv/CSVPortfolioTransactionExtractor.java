package name.abuchen.portfolio.datatransfer.csv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
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

        List<Field> fields = getFields();
        fields.add(new DateField("date", Messages.CSVColumn_Date)); //$NON-NLS-1$
        fields.add(new Field("time", Messages.CSVColumn_Time).setOptional(true)); //$NON-NLS-1$
        fields.add(new ISINField("isin", Messages.CSVColumn_ISIN).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("ticker", Messages.CSVColumn_TickerSymbol).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("wkn", Messages.CSVColumn_WKN).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("name", Messages.CSVColumn_SecurityName).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("value", Messages.CSVColumn_Value)); //$NON-NLS-1$
        fields.add(new Field("currency", Messages.CSVColumn_TransactionCurrency).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("fees", Messages.CSVColumn_Fees).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("taxes", Messages.CSVColumn_Taxes).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("gross", Messages.CSVColumn_GrossAmount).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("currencyGross", Messages.CSVColumn_CurrencyGrossAmount).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("exchangeRate", Messages.CSVColumn_ExchangeRate).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("shares", Messages.CSVColumn_Shares)); //$NON-NLS-1$
        fields.add(new EnumField<PortfolioTransaction.Type>("type", Messages.CSVColumn_Type, Type.class) //$NON-NLS-1$
                        .setOptional(true));
        fields.add(new Field("note", Messages.CSVColumn_Note).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("account", Messages.CSVColumn_AccountName).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("portfolio", Messages.CSVColumn_PortfolioName).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("portfolio2nd", Messages.CSVColumn_PortfolioName2nd).setOptional(true)); //$NON-NLS-1$
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
        Security security = getSecurity(rawValues, field2column, s -> {
            String currency = getText(Messages.CSVColumn_CurrencyGrossAmount, rawValues, field2column);
            if (currency == null || currency.isEmpty())
                currency = getText(Messages.CSVColumn_TransactionCurrency, rawValues, field2column);

            if (currency != null)
            {
                CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
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
        Money amount = getMoney(rawValues, field2column);

        // determine type (if not explicitly given by import)
        Type type = inferType(rawValues, field2column, amount);

        // determine remaining fields
        LocalDateTime date = getDate(Messages.CSVColumn_Date, Messages.CSVColumn_Time, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);

        Long shares = getShares(Messages.CSVColumn_Shares, rawValues, field2column);
        Long fees = getAmount(Messages.CSVColumn_Fees, rawValues, field2column);
        Long taxes = getAmount(Messages.CSVColumn_Taxes, rawValues, field2column);
        String note = getText(Messages.CSVColumn_Note, rawValues, field2column);

        Optional<Unit> grossAmount = extractGrossAmount(rawValues, field2column, amount);

        Account account = getAccount(getClient(), rawValues, field2column);
        Portfolio portfolio = getPortfolio(getClient(), rawValues, field2column);
        Portfolio portfolio2nd = getPortfolio(getClient(), rawValues, field2column, true);

        Extractor.Item item = null;

        switch (type)
        {
            case BUY:
            case SELL:
                item = createBuySell(rawValues, field2column, type, security, amount, fees, taxes, date, note, shares,
                                grossAmount);
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                item = createTransfer(security, amount, date, note, shares);
                break;
            case DELIVERY_INBOUND:
            case DELIVERY_OUTBOUND:
                item = createDelivery(rawValues, field2column, type, security, amount, fees, taxes, date, note, shares,
                                grossAmount);
                break;
            default:
                throw new IllegalArgumentException(type.toString());
        }

        item.setAccountPrimary(account);
        item.setPortfolioPrimary(portfolio);
        item.setPortfolioSecondary(portfolio2nd);

        items.add(item);
    }

    private Item createBuySell(String[] rawValues, Map<String, Column> field2column, Type type, Security security,
                    Money amount, Long fees, Long taxes, LocalDateTime date, String note, Long shares,
                    Optional<Unit> grossAmount) throws ParseException
    {
        BuySellEntry entry = new BuySellEntry();
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

        return new BuySellEntryItem(entry);
    }

    private void createGrossValueIfNecessary(String[] rawValues, Map<String, Column> field2column,
                    PortfolioTransaction transaction) throws ParseException
    {
        if (transaction.getSecurity().getCurrencyCode().equals(transaction.getCurrencyCode()))
            return;

        BigDecimal exchangeRate = getBigDecimal(Messages.CSVColumn_ExchangeRate, rawValues, field2column);
        if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) != 0)
        {
            Money grossValue = transaction.getGrossValue();

            Money forex = Money.of(transaction.getSecurity().getCurrencyCode(), Math
                            .round(exchangeRate.multiply(BigDecimal.valueOf(grossValue.getAmount())).doubleValue()));

            exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

            transaction.addUnit(new Unit(Unit.Type.GROSS_VALUE, grossValue, forex, exchangeRate));

        }
    }

    private Item createTransfer(Security security, Money amount, LocalDateTime date, String note, Long shares)
    {
        PortfolioTransferEntry entry = new PortfolioTransferEntry();
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
        PortfolioTransaction t = new PortfolioTransaction();

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

        return new TransactionItem(t);
    }

    private Type inferType(String[] rawValues, Map<String, Column> field2column, Money amount) throws ParseException
    {
        Type type = getEnum(Messages.CSVColumn_Type, PortfolioTransaction.Type.class, rawValues, field2column);
        if (type == null)
            type = amount.isNegative() ? Type.BUY : Type.SELL;
        return type;
    }
}
