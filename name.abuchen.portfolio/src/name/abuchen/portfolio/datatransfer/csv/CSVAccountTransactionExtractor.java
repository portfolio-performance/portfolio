package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/* package */ class CSVAccountTransactionExtractor extends BaseCSVExtractor
{
    /* package */ CSVAccountTransactionExtractor(Client client)
    {
        super(client, Messages.CSVDefAccountTransactions);

        List<Field> fields = getFields();
        fields.add(new DateField("date", Messages.CSVColumn_Date)); //$NON-NLS-1$
        fields.add(new Field("time", Messages.CSVColumn_Time).setOptional(true)); //$NON-NLS-1$
        fields.add(new ISINField("isin", Messages.CSVColumn_ISIN).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("ticker", Messages.CSVColumn_TickerSymbol).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("wkn", Messages.CSVColumn_WKN).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("value", Messages.CSVColumn_Value)); //$NON-NLS-1$
        fields.add(new Field("currency", Messages.CSVColumn_TransactionCurrency).setOptional(true)); //$NON-NLS-1$
        fields.add(new EnumField<AccountTransaction.Type>("type", Messages.CSVColumn_Type, Type.class) //$NON-NLS-1$
                        .setOptional(true));
        fields.add(new Field("name", Messages.CSVColumn_SecurityName).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("shares", Messages.CSVColumn_Shares).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("note", Messages.CSVColumn_Note).setOptional(true)); //$NON-NLS-1$
        fields.add(new AmountField("taxes", Messages.CSVColumn_Taxes).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("account", Messages.CSVColumn_AccountName).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("account2nd", Messages.CSVColumn_AccountName2nd).setOptional(true)); //$NON-NLS-1$
        fields.add(new Field("portfolio", Messages.CSVColumn_PortfolioName).setOptional(true)); //$NON-NLS-1$
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
        Security security = getSecurity(rawValues, field2column, s -> s.setCurrencyCode(
                        getCurrencyCode(Messages.CSVColumn_TransactionCurrency, rawValues, field2column)));

        // check for the transaction amount
        Money amount = getMoney(rawValues, field2column);

        // determine type (if not explicitly given by import)
        Type type = inferType(rawValues, field2column, security, amount);

        // extract remaining fields
        LocalDateTime date = getDate(Messages.CSVColumn_Date, Messages.CSVColumn_Time, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);
        String note = getText(Messages.CSVColumn_Note, rawValues, field2column);
        Long shares = getShares(Messages.CSVColumn_Shares, rawValues, field2column);
        Long taxes = getAmount(Messages.CSVColumn_Taxes, rawValues, field2column);

        Account account = getAccount(getClient(), rawValues, field2column);
        Account account2nd = getAccount(getClient(), rawValues, field2column, true);
        Portfolio portfolio = getPortfolio(getClient(), rawValues, field2column);

        Extractor.Item item = null;

        switch (type)
        {
            case TRANSFER_IN:
            case TRANSFER_OUT:
                AccountTransferEntry entry = new AccountTransferEntry();
                entry.setAmount(Math.abs(amount.getAmount()));
                entry.setCurrencyCode(amount.getCurrencyCode());
                entry.setDate(date.withHour(0).withMinute(0));
                entry.setNote(note);
                item = new AccountTransferItem(entry, type == Type.TRANSFER_OUT);
                break;
            case BUY:
            case SELL:
                if (security == null)
                    throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                                    new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                                    .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                                    .toString()),
                                    0);
                if (shares == null)
                    throw new ParseException(
                                    MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Shares), 0);

                BuySellEntry buySellEntry = new BuySellEntry();
                buySellEntry.setType(PortfolioTransaction.Type.valueOf(type.name()));
                buySellEntry.setAmount(Math.abs(amount.getAmount()));
                buySellEntry.setShares(Math.abs(shares));
                buySellEntry.setCurrencyCode(amount.getCurrencyCode());
                buySellEntry.setSecurity(security);
                buySellEntry.setDate(date);
                buySellEntry.setNote(note);
                item = new BuySellEntryItem(buySellEntry);
                break;
            case DIVIDENDS: // NOSONAR
                // dividends must have a security
                if (security == null)
                    throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                                    new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                                    .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                                    .toString()),
                                    0);
            case DEPOSIT:
            case TAXES:
            case TAX_REFUND:
            case FEES:
            case FEES_REFUND:
            case INTEREST:
            case INTEREST_CHARGE:
            case REMOVAL:
                AccountTransaction t = new AccountTransaction();
                t.setType(type);
                t.setAmount(Math.abs(amount.getAmount()));
                t.setCurrencyCode(amount.getCurrencyCode());
                if (type == Type.DIVIDENDS || type == Type.TAXES || type == Type.TAX_REFUND || type == Type.FEES || type == Type.FEES_REFUND)
                    t.setSecurity(security);
                t.setDateTime(date.withHour(0).withMinute(0));
                t.setNote(note);
                if (shares != null && type == Type.DIVIDENDS)
                    t.setShares(Math.abs(shares));
                if (type == Type.DIVIDENDS && taxes != null && taxes.longValue() != 0)
                    t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), Math.abs(taxes))));
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

    private Type inferType(String[] rawValues, Map<String, Column> field2column, Security security, Money amount)
                    throws ParseException
    {
        Type type = getEnum(Messages.CSVColumn_Type, Type.class, rawValues, field2column);
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
