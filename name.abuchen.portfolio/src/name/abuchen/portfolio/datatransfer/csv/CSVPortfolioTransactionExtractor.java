package name.abuchen.portfolio.datatransfer.csv;

import java.math.BigDecimal;
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

        List<Field> fields = getFields();
        fields.add(new DateField(Messages.CSVColumn_Date));
        fields.add(new ISINField(Messages.CSVColumn_ISIN).setOptional(true));
        fields.add(new Field(Messages.CSVColumn_TickerSymbol).setOptional(true));
        fields.add(new Field(Messages.CSVColumn_WKN).setOptional(true));
        fields.add(new Field(Messages.CSVColumn_SecurityName).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_Value));
        fields.add(new Field(Messages.CSVColumn_TransactionCurrency).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_Fees).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_Taxes).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_GrossAmount).setOptional(true));
        fields.add(new Field(Messages.CSVColumn_CurrencyGrossAmount).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_ExchangeRate).setOptional(true));
        fields.add(new AmountField(Messages.CSVColumn_Shares));
        fields.add(new EnumField<PortfolioTransaction.Type>(Messages.CSVColumn_Type, Type.class).setOptional(true));
        fields.add(new Field(Messages.CSVColumn_Note).setOptional(true));
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
        LocalDate date = getDate(Messages.CSVColumn_Date, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);

        Long shares = getShares(Messages.CSVColumn_Shares, rawValues, field2column);
        Long fees = getAmount(Messages.CSVColumn_Fees, rawValues, field2column);
        Long taxes = getAmount(Messages.CSVColumn_Taxes, rawValues, field2column);
        String note = getText(Messages.CSVColumn_Note, rawValues, field2column);

        Unit grossAmount = extractGrossAmount(rawValues, field2column, amount);

        switch (type)
        {
            case BUY:
            case SELL:
                items.add(createBuySell(rawValues, field2column, type, security, amount, fees, taxes, date, note,
                                shares, grossAmount));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                items.add(createTransfer(security, amount, fees, taxes, date, note, shares, grossAmount));
                break;
            case DELIVERY_INBOUND:
            case DELIVERY_OUTBOUND:
                items.add(createDelivery(rawValues, field2column, type, security, amount, fees, taxes, date, note,
                                shares, grossAmount));
                break;
            default:
                throw new IllegalArgumentException(type.toString());
        }

    }

    private Item createBuySell(String[] rawValues, Map<String, Column> field2column, Type type, Security security,
                    Money amount, Long fees, Long taxes, LocalDate date, String note, Long shares, Unit grossAmount)
                    throws ParseException
    {
        BuySellEntry entry = new BuySellEntry();
        entry.setType(type);
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setAmount(Math.abs(amount.getAmount()));
        entry.setCurrencyCode(amount.getCurrencyCode());
        entry.setShares(shares);
        entry.setNote(note);

        if (grossAmount != null)
            entry.getPortfolioTransaction().addUnit(grossAmount);

        if (fees != null && fees.longValue() != 0)
            entry.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE, Money.of(amount.getCurrencyCode(), Math.abs(fees))));

        if (taxes != null && taxes.longValue() != 0)
            entry.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX, Money.of(amount.getCurrencyCode(), Math.abs(taxes))));

        if (grossAmount == null)
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

            exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, BigDecimal.ROUND_HALF_DOWN);

            transaction.addUnit(new Unit(Unit.Type.GROSS_VALUE, grossValue, forex, exchangeRate));

        }
    }

    private Item createTransfer(Security security, Money amount, Long fees, Long taxes, LocalDate date, String note,
                    Long shares, Unit grossAmount)
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
                    Money amount, Long fees, Long taxes, LocalDate date, String note, Long shares, Unit grossAmount)
                    throws ParseException
    {
        PortfolioTransaction t = new PortfolioTransaction();

        t.setType(type);
        t.setSecurity(security);
        t.setDate(date);
        t.setAmount(Math.abs(amount.getAmount()));
        t.setCurrencyCode(amount.getCurrencyCode());
        t.setShares(shares);
        t.setNote(note);

        if (grossAmount != null)
            t.addUnit(grossAmount);

        if (fees != null && fees.longValue() != 0)
            t.addUnit(new Unit(Unit.Type.FEE, Money.of(amount.getCurrencyCode(), Math.abs(fees))));

        if (taxes != null && taxes.longValue() != 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(amount.getCurrencyCode(), Math.abs(taxes))));

        if (grossAmount == null)
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

    private Unit extractGrossAmount(String[] rawValues, Map<String, Column> field2column, Money amount)
                    throws ParseException
    {
        Long grossAmount = getAmount(Messages.CSVColumn_GrossAmount, rawValues, field2column);
        String currencyCode = getCurrencyCode(Messages.CSVColumn_CurrencyGrossAmount, rawValues, field2column);
        BigDecimal exchangeRate = getBigDecimal(Messages.CSVColumn_ExchangeRate, rawValues, field2column);

        // if no currency code is given, let's assume the gross amount is in the
        // same currency as the transaction itself. Either way, if the gross
        // amount currency equals the transaction currency, no unit is created
        if (currencyCode == null || amount.getCurrencyCode().equals(currencyCode))
            return null;

        // if no gross amount is given at all, no unit
        if (grossAmount == null || grossAmount.longValue() == 0)
            return null;

        // if no exchange rate is available, not unit to create
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0)
            return null;

        Money forex = Money.of(currencyCode, Math.abs(grossAmount.longValue()));
        BigDecimal grossAmountConverted = exchangeRate.multiply(BigDecimal.valueOf(grossAmount));
        Money converted = Money.of(amount.getCurrencyCode(), Math.round(grossAmountConverted.doubleValue()));

        return new Unit(Unit.Type.GROSS_VALUE, converted, forex, exchangeRate);
    }
}
