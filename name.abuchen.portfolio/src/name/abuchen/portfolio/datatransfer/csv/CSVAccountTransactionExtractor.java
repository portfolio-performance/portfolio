package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/* package */ class CSVAccountTransactionExtractor extends BaseCSVExtractor
{
    /* package */ CSVAccountTransactionExtractor(Client client)
    {
        super(client, Messages.CSVDefAccountTransactions);

        var fields = getFields();
        fields.add(new DateField(FieldCode.DATE, Messages.CSVColumn_Date));
        fields.add(new Field(FieldCode.TIME, Messages.CSVColumn_Time).setOptional(true));
        fields.add(new ISINField(FieldCode.ISIN, Messages.CSVColumn_ISIN).setOptional(true));
        fields.add(new Field(FieldCode.TICKER, Messages.CSVColumn_TickerSymbol).setOptional(true));
        fields.add(new Field(FieldCode.WKN, Messages.CSVColumn_WKN).setOptional(true));
        fields.add(new AmountField(FieldCode.VALUE, Messages.CSVColumn_Value));
        fields.add(new Field(FieldCode.CURRENCY, Messages.CSVColumn_TransactionCurrency).setOptional(true));
        fields.add(new EnumField<AccountTransaction.Type>(FieldCode.TYPE, Messages.CSVColumn_Type, Type.class)
                        .setOptional(true));
        fields.add(new Field(FieldCode.NAME, Messages.CSVColumn_SecurityName).setOptional(true));
        fields.add(new AmountField(FieldCode.SHARES, Messages.CSVColumn_Shares).setOptional(true));
        fields.add(new Field(FieldCode.NOTE, Messages.CSVColumn_Note).setOptional(true));
        fields.add(new AmountField(FieldCode.TAXES, Messages.CSVColumn_Taxes).setOptional(true));
        fields.add(new AmountField(FieldCode.FEES, Messages.CSVColumn_Fees).setOptional(true));
        fields.add(new Field(FieldCode.ACCOUNT, Messages.CSVColumn_AccountName).setOptional(true));
        fields.add(new Field(FieldCode.ACCOUNT_2ND, Messages.CSVColumn_AccountName2nd).setOptional(true));
        fields.add(new Field(FieldCode.PORTFOLIO, Messages.CSVColumn_PortfolioName).setOptional(true));

        fields.add(new AmountField(FieldCode.GROSS, Messages.CSVColumn_GrossAmount).setOptional(true));
        fields.add(new Field(FieldCode.CURRENCY_GROSS, Messages.CSVColumn_CurrencyGrossAmount).setOptional(true));
        fields.add(new AmountField(FieldCode.EXCHANGE_RATE, Messages.CSVColumn_ExchangeRate).setOptional(true));
    }

    @Override
    public String getCode()
    {
        return "account-transaction"; //$NON-NLS-1$
    }

    @Override
    void extract(List<Item> items, String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        // check if we have a security
        var security = getSecurity(rawValues, field2column, s -> s.setCurrencyCode(
                        getCurrencyCode(FieldCode.CURRENCY, rawValues, field2column)));

        // check for the transaction amount
        var amount = getMoney(rawValues, field2column);

        // determine type (if not explicitly given by import)
        var type = inferType(rawValues, field2column, security, amount);

        // extract remaining fields
        var date = getDate(FieldCode.DATE, FieldCode.TIME, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);
        var note = getText(FieldCode.NOTE, rawValues, field2column);
        var shares = getShares(FieldCode.SHARES, rawValues, field2column);
        var taxes = getAmount(FieldCode.TAXES, rawValues, field2column);
        var fees = getAmount(FieldCode.FEES, rawValues, field2column);

        Optional<Unit> grossAmount = extractGrossAmount(rawValues, field2column, amount);

        var account = getAccount(getClient(), rawValues, field2column);
        var account2nd = getAccount(getClient(), rawValues, field2column, true);
        var portfolio = getPortfolio(getClient(), rawValues, field2column);

        Extractor.Item item = null;

        switch (type)
        {
            case TRANSFER_IN, TRANSFER_OUT:
                var entry = new AccountTransferEntry();
                if (grossAmount.isPresent())
                {
                    var grossAmountUnit = grossAmount.get();
                    entry.getSourceTransaction().setMonetaryAmount(grossAmountUnit.getAmount());
                    entry.getTargetTransaction().setMonetaryAmount(grossAmountUnit.getForex());
                    entry.getSourceTransaction().addUnit(grossAmountUnit);
                }
                else
                {
                    entry.setAmount(Math.abs(amount.getAmount()));
                    entry.setCurrencyCode(amount.getCurrencyCode());
                }
                entry.setDate(date);
                entry.setNote(note);
                item = new AccountTransferItem(entry, type == Type.TRANSFER_OUT);
                break;
            case BUY, SELL:
                if (security == null)
                    throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                                    new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                                    .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                                    .toString()),
                                    0);
                if (shares == null)
                    throw new ParseException(
                                    MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Shares), 0);

                var buySellEntry = new BuySellEntry();
                buySellEntry.setType(PortfolioTransaction.Type.valueOf(type.name()));
                buySellEntry.setAmount(Math.abs(amount.getAmount()));
                buySellEntry.setShares(Math.abs(shares));
                buySellEntry.setCurrencyCode(amount.getCurrencyCode());
                buySellEntry.setSecurity(security);
                buySellEntry.setDate(date);
                buySellEntry.setNote(note);

                if (grossAmount.isPresent())
                    buySellEntry.getPortfolioTransaction().addUnit(grossAmount.get());

                if (taxes != null && taxes.longValue() != 0)
                    buySellEntry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money
                                    .of(buySellEntry.getPortfolioTransaction().getCurrencyCode(), Math.abs(taxes))));

                if (fees != null && fees.longValue() != 0)
                    buySellEntry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money
                                    .of(buySellEntry.getPortfolioTransaction().getCurrencyCode(), Math.abs(fees))));

                if (!grossAmount.isPresent())
                    createGrossValueIfNecessary(rawValues, field2column, buySellEntry.getPortfolioTransaction());

                ExtractorUtils.fixGrossValueBuySell().accept(buySellEntry);

                if (buySellEntry.getPortfolioTransaction().getAmount() == 0L
                                && buySellEntry.getPortfolioTransaction().getType() == PortfolioTransaction.Type.SELL)
                {
                    // convert to outbound delivery if amount is 0
                    var tx = buySellEntry.getPortfolioTransaction();
                    item = new TransactionItem(convertToOutboundDelivery(tx));
                }
                else
                {
                    item = new BuySellEntryItem(buySellEntry);
                }

                break;
            case DIVIDENDS: // NOSONAR
                // dividends must have a security
                if (security == null)
                    throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                                    new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                                    .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                                    .toString()),
                                    0);
            case DEPOSIT, REMOVAL:
            case TAXES, TAX_REFUND:
            case FEES, FEES_REFUND:
            case INTEREST, INTEREST_CHARGE:
                var t = new AccountTransaction();
                t.setType(type);
                t.setAmount(Math.abs(amount.getAmount()));
                t.setCurrencyCode(amount.getCurrencyCode());
                if (type == Type.DIVIDENDS || type == Type.TAXES || type == Type.TAX_REFUND || type == Type.FEES
                                || type == Type.FEES_REFUND)
                    t.setSecurity(security);
                t.setDateTime(date);
                t.setNote(note);

                if (type == Type.DIVIDENDS)
                {
                    if (shares != null)
                        t.setShares(Math.abs(shares));

                    if (taxes != null && taxes.longValue() != 0)
                        t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), Math.abs(taxes))));

                    if (fees != null && fees.longValue() != 0)
                        t.addUnit(new Unit(Unit.Type.FEE, Money.of(t.getCurrencyCode(), Math.abs(fees))));
                }

                if (type == Type.INTEREST && taxes != null && taxes.longValue() != 0)
                {
                    t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), Math.abs(taxes))));
                }

                if (security != null && grossAmount.isPresent())
                {
                    // gross amount can only be relevant if a transaction is
                    // linked to a security (dividend, taxes, fees, and refunds)

                    t.addUnit(grossAmount.get());
                }

                item = new TransactionItem(t);
                break;
            default:
                throw new IllegalArgumentException(type.toString());
        }

        item.setAccountPrimary(account);
        item.setAccountSecondary(account2nd);
        item.setPortfolioPrimary(portfolio);

        items.add(item);
    }

    private PortfolioTransaction convertToOutboundDelivery(PortfolioTransaction tx)
    {
        var delivery = new PortfolioTransaction();
        delivery.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
        delivery.setDateTime(tx.getDateTime());
        delivery.setAmount(tx.getAmount());
        delivery.setCurrencyCode(tx.getCurrencyCode());
        delivery.setShares(tx.getShares());
        delivery.setSecurity(tx.getSecurity());
        delivery.setNote(tx.getNote());
        delivery.addUnits(tx.getUnits());
        return delivery;
    }

    private Type inferType(String[] rawValues, Map<String, Column> field2column, Security security, Money amount)
                    throws ParseException
    {
        var type = getEnum(FieldCode.TYPE, Type.class, rawValues, field2column);
        if (type == null)
        {
            if (security != null)
                type = amount.isNegative() ? AccountTransaction.Type.REMOVAL : AccountTransaction.Type.DIVIDENDS;
            else
                type = amount.isNegative() ? AccountTransaction.Type.REMOVAL : AccountTransaction.Type.DEPOSIT;
        }
        return type;
    }
}
