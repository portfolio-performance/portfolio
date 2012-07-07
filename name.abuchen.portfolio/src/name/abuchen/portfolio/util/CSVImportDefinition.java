package name.abuchen.portfolio.util;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.util.CSVImporter.AmountField;
import name.abuchen.portfolio.util.CSVImporter.Column;
import name.abuchen.portfolio.util.CSVImporter.DateField;
import name.abuchen.portfolio.util.CSVImporter.EnumField;
import name.abuchen.portfolio.util.CSVImporter.Field;
import name.abuchen.portfolio.util.CSVImporter.FieldFormat;

public abstract class CSVImportDefinition
{
    private String label;
    private List<Field> fields;

    /* package */CSVImportDefinition(String label)
    {
        this.label = label;
        this.fields = new ArrayList<Field>();
    }

    public List<Field> getFields()
    {
        return fields;
    }

    @Override
    public String toString()
    {
        return label;
    }

    public abstract List<?> getTargets(Client client);

    /* package */abstract void build(Client client, Object target, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException;

    protected Long convertAmount(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        String value = getTextValue(name, rawValues, field2column);
        if (value == null)
            return null;

        Number num = (Number) field2column.get(name).getFormat().getFormat().parseObject(value);
        return Long.valueOf((long) Math.round(num.doubleValue() * Values.Amount.factor()));
    }

    protected Long convertShares(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        String value = getTextValue(name, rawValues, field2column);
        if (value == null)
            return null;

        Number num = (Number) field2column.get(name).getFormat().getFormat().parseObject(value);
        return (long) Math.round(num.doubleValue());
    }

    protected Date convertDate(String name, String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        String value = getTextValue(name, rawValues, field2column);
        if (value == null)
            return null;
        return (Date) field2column.get(name).getFormat().getFormat().parseObject(value);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Enum<E>> E convertEnum(String name, Class<E> type, String[] rawValues,
                    Map<String, Column> field2column) throws ParseException
    {
        String value = getTextValue(name, rawValues, field2column);
        if (value == null)
            return null;
        FieldFormat ff = field2column.get(name).getFormat();

        if (ff != null && ff.getFormat() != null)
            return (E) ff.getFormat().parseObject(value);
        else
            return Enum.valueOf(type, value);
    }

    protected String getTextValue(String name, String[] rawValues, Map<String, Column> field2column)
    {
        Column column = field2column.get(name);
        if (column == null)
            return null;

        int columnIndex = column.getColumnIndex();

        if (columnIndex < 0 || columnIndex >= rawValues.length)
            return null;

        String value = rawValues[columnIndex];
        return value != null && value.trim().length() == 0 ? null : value;
    }

    protected Security lookupSecurity(Client client, String isin, boolean doCreate)
    {
        Security security = null;
        for (Security s : client.getSecurities())
        {
            if (isin.equals(s.getIsin()))
            {
                security = s;
                break;
            }
        }
        if (security == null && doCreate)
        {
            security = new Security(MessageFormat.format(Messages.CSVImportedSecurityLabel, isin), isin, null,
                            AssetClass.EQUITY, QuoteFeed.MANUAL);
            client.addSecurity(security);
        }
        return security;
    }

    //
    // implementations
    //

    /* package */static class AccountTransactionDef extends CSVImportDefinition
    {
        /* package */AccountTransactionDef()
        {
            super(Messages.CSVDefAccountTransactions);

            List<Field> fields = getFields();
            fields.add(new DateField(Messages.CSVColumn_Date));
            fields.add(new Field(Messages.CSVColumn_ISIN).setOptional(true));
            fields.add(new AmountField(Messages.CSVColumn_Value));
            fields.add(new EnumField<AccountTransaction.Type>(Messages.CSVColumn_Type, AccountTransaction.Type.class)
                            .setOptional(true));
        }

        @Override
        public List<?> getTargets(Client client)
        {
            return client.getAccounts();
        }

        @Override
        void build(Client client, Object target, String[] rawValues, Map<String, Column> field2column)
                        throws ParseException
        {
            if (!(target instanceof Account))
                throw new IllegalArgumentException();

            Account account = (Account) target;

            Date date = convertDate(Messages.CSVColumn_Date, rawValues, field2column);
            if (date == null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date),
                                0);

            Long amount = convertAmount(Messages.CSVColumn_Value, rawValues, field2column);
            if (amount == null)
                throw new ParseException(
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Value), 0);

            AccountTransaction.Type type = convertEnum(Messages.CSVColumn_Type, AccountTransaction.Type.class,
                            rawValues, field2column);

            AccountTransaction transaction = new AccountTransaction();
            transaction.setDate(date);
            transaction.setAmount(Math.abs(amount));
            String isin = getTextValue(Messages.CSVColumn_ISIN, rawValues, field2column);
            if (isin != null)
            {
                Security security = lookupSecurity(client, isin, true);
                transaction.setSecurity(security);
            }

            if (type != null)
                transaction.setType(type);
            else if (transaction.getSecurity() != null)
                transaction.setType(amount < 0 ? AccountTransaction.Type.FEES : AccountTransaction.Type.DIVIDENDS);
            else
                transaction.setType(amount < 0 ? AccountTransaction.Type.REMOVAL : AccountTransaction.Type.DEPOSIT);

            account.addTransaction(transaction);
        }

    }

    /* package */static class PortfolioTransactionDef extends CSVImportDefinition
    {
        /* package */PortfolioTransactionDef()
        {
            super(Messages.CSVDefPortfolioTransactions);

            List<Field> fields = getFields();
            fields.add(new DateField(Messages.CSVColumn_Date));
            fields.add(new Field(Messages.CSVColumn_ISIN));
            fields.add(new AmountField(Messages.CSVColumn_Value));
            fields.add(new AmountField(Messages.CSVColumn_Fees).setOptional(true));
            fields.add(new AmountField(Messages.CSVColumn_Shares));
            fields.add(new EnumField<PortfolioTransaction.Type>(Messages.CSVColumn_Type,
                            PortfolioTransaction.Type.class).setOptional(true));
        }

        @Override
        public List<?> getTargets(Client client)
        {
            return client.getPortfolios();
        }

        @Override
        void build(Client client, Object target, String[] rawValues, Map<String, Column> field2column)
                        throws ParseException
        {
            if (!(target instanceof Portfolio))
                throw new IllegalArgumentException();

            Portfolio portfolio = (Portfolio) target;

            Date date = convertDate(Messages.CSVColumn_Date, rawValues, field2column);
            if (date == null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date),
                                0);

            Long amount = convertAmount(Messages.CSVColumn_Value, rawValues, field2column);
            if (amount == null)
                throw new ParseException(
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Value), 0);

            Long fees = convertAmount(Messages.CSVColumn_Fees, rawValues, field2column);
            if (fees == null)
                fees = Long.valueOf(0);

            String isin = getTextValue(Messages.CSVColumn_ISIN, rawValues, field2column);
            if (isin == null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_ISIN),
                                0);

            Long shares = convertShares(Messages.CSVColumn_Shares, rawValues, field2column);
            if (shares == null)
                throw new ParseException(
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Shares), 0);

            PortfolioTransaction.Type type = convertEnum(Messages.CSVColumn_Type, PortfolioTransaction.Type.class,
                            rawValues, field2column);

            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setDate(date);
            transaction.setAmount(Math.abs(amount));
            transaction.setSecurity(lookupSecurity(client, isin, true));
            transaction.setShares(Math.abs(shares));
            transaction.setFees(Math.abs(fees));

            if (type != null)
                transaction.setType(type);
            else
                transaction.setType(amount < 0 ? PortfolioTransaction.Type.BUY : PortfolioTransaction.Type.SELL);

            portfolio.addTransaction(transaction);
        }
    }

    /* package */static class SecurityPriceDef extends CSVImportDefinition
    {
        /* package */SecurityPriceDef()
        {
            super(Messages.CSVDefHistoricalQuotes);

            List<Field> fields = getFields();
            fields.add(new DateField(Messages.CSVColumn_Date));
            fields.add(new AmountField(Messages.CSVColumn_Quote));
        }

        @Override
        public List<?> getTargets(Client client)
        {
            return client.getSecurities();
        }

        @Override
        void build(Client client, Object target, String[] rawValues, Map<String, Column> field2column)
                        throws ParseException
        {
            if (!(target instanceof Security))
                throw new IllegalArgumentException();

            Security security = (Security) target;

            Date date = convertDate(Messages.CSVColumn_Date, rawValues, field2column);
            if (date == null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date),
                                0);

            Long amount = convertAmount(Messages.CSVColumn_Quote, rawValues, field2column);
            if (amount == null)
                throw new ParseException(
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Quote), 0);

            security.addPrice(new SecurityPrice(date, Math.abs(amount)));
        }
    }

    /* package */static class SecurityDef extends CSVImportDefinition
    {
        /* package */SecurityDef()
        {
            super(Messages.CSVDefSecurities);

            List<Field> fields = getFields();
            fields.add(new Field(Messages.CSVColumn_ISIN));
            fields.add(new Field(Messages.CSVColumn_Description).setOptional(true));
            fields.add(new Field(Messages.CSVColumn_TickerSymbol).setOptional(true));
            fields.add(new EnumField<AssetClass>(Messages.CSVColumn_Type, AssetClass.class).setOptional(true));
        }

        @Override
        public List<?> getTargets(Client client)
        {
            return Arrays.asList(new String[] { Messages.CSVDefSecurityMasterData });
        }

        @Override
        void build(Client client, Object target, String[] rawValues, Map<String, Column> field2column)
                        throws ParseException
        {
            if (!(target instanceof String))
                throw new IllegalArgumentException();

            String isin = getTextValue(Messages.CSVColumn_ISIN, rawValues, field2column);
            if (isin == null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_ISIN),
                                0);

            Security security = lookupSecurity(client, isin, false);
            if (security != null)
                throw new ParseException(MessageFormat.format(Messages.CSVImportISINExists, isin), 0);

            String description = getTextValue(Messages.CSVColumn_Description, rawValues, field2column);
            if (description == null)
                description = MessageFormat.format(Messages.CSVImportedSecurityLabel, isin);

            String tickerSymbol = getTextValue(Messages.CSVColumn_TickerSymbol, rawValues, field2column);

            AssetClass assetClass = convertEnum(Messages.CSVColumn_Type, AssetClass.class, rawValues, field2column);
            if (assetClass == null)
                assetClass = AssetClass.EQUITY;

            String feed = QuoteFeed.MANUAL;
            if (tickerSymbol != null)
                feed = YahooFinanceQuoteFeed.ID;

            security = new Security(description, isin, tickerSymbol, assetClass, feed);
            client.addSecurity(security);
        }
    }

}
