package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumMapFormat;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.FieldFormat;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CSVAccountTransactionExtractorTest
{
    @Test
    public void testDividendTransactionPlusSecurityCreation() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is(nullValue()));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(10)));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testValuesAreTrimmed() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { " 2013-01-01 ", "", " DE0007164600 ", " SAP.DE ", "",
                                        " 100 ", " EUR ", " DIVIDENDS ", " SAP SE ", " 10 ", " Notiz " }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is(nullValue()));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(10)));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testDividendTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(10)));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testDividendTransaction_whenSecurityIsMissing()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "", "100", "EUR", "DIVIDENDS", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }

    @Test
    public void testIfMultipleSecuritiesWithSameISINExist()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);
        Security security2 = new Security();
        security2.setIsin("DE0007164600");
        client.addSecurity(security2);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100",
                                        "EUR", "DIVIDENDS", "SAP SE", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }

    @Test
    public void testTypeIsDeterminedByPositiveAmount()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-02", "", "", "", "", "100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.get(0).getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-02T00:00")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testTypeIsDeterminedByNegativeUnaryMinusOperator()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "10:00", "", "", "", "-100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.get(0).getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));

        // date and time is read
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T10:00")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testTypeIsDeterminedByUnaryMinusOperatorAndSecurity()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "DE0007164600", "", "", "100", "EUR", "", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
    }

    @Test
    public void testThatSecurityIsAddedOnlyOnce()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2013-01-01", "", "DE0007164600", "", "", "100", "EUR", "", "", "", "Notiz" },
                        new String[] { "2013-01-02", "", "DE0007164600", "", "", "200", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
    }

    @Test
    public void testBuyTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(new String[] { "2013-01-01", "10:00",
                        "DE0007164600", "", "", "100", "EUR", "BUY", "", "10", "Notiz" }), buildField2Column(extractor),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        BuySellEntry e = (BuySellEntry) results.get(0).getSubject();
        AccountTransaction t = e.getAccountTransaction();
        assertThat(t.getType(), is(AccountTransaction.Type.BUY));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T10:00")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(security));
    }

    @Test
    public void testBuyTransactionFailsWhenSharesAreMissing()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(new String[] { "2013-01-01", "",
                        "DE0007164600", "", "", "100", "EUR", "BUY", "", "", "Notiz" }), buildField2Column(extractor),
                        errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testBuyTransactionFailsWhenSecurityIsMissing()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "", "100", "EUR", "BUY", "", "10", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testTransferTransaction()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2013-01-01", "", "", "", "", "100", "EUR", "TRANSFER_OUT", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());

        AccountTransferEntry entry = (AccountTransferEntry) results.get(0).getSubject();
        AccountTransaction t = entry.getSourceTransaction();
        assertThat(t.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(t.getShares(), is(0L));
        assertThat(t.getSecurity(), is(nullValue()));
    }

    @Test
    public void testTransferOutTransactionWithCurrencyConversion()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        new String[] { "2013-01-01", "", "", "", "", "110", "EUR", "TRANSFER_OUT", "", "", "Notiz", "",
                                        "", "", "", "", "100", "USD", "1.1" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());

        AccountTransferEntry entry = (AccountTransferEntry) results.get(0).getSubject();
        AccountTransaction source = entry.getSourceTransaction();
        assertThat(source.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(source.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(source.getNote(), is("Notiz"));
        assertThat(source.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(source.getShares(), is(0L));
        assertThat(source.getSecurity(), is(nullValue()));

        List<Unit> units = source.getUnits().toList();
        assertThat(units.size(), is(1));
        assertThat(units.get(0).getType(), is(Unit.Type.GROSS_VALUE));
        assertThat(units.get(0).getAmount(), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(units.get(0).getForex(), is(Money.of(CurrencyUnit.USD, 100_00)));
        assertThat(units.get(0).getExchangeRate(), is(BigDecimal.valueOf(1.1)));

        AccountTransaction target = entry.getTargetTransaction();
        assertThat(target.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(target.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, 100_00)));
        assertThat(target.getNote(), is("Notiz"));
        assertThat(target.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(target.getShares(), is(0L));
        assertThat(target.getSecurity(), is(nullValue()));
        assertThat(target.getUnits().toList(), empty());
    }

    @Test
    public void testTransferInTransactionWithCurrencyConversion()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "", "", "", "110", "EUR",
                                        "TRANSFER_IN", "", "", "Notiz", "", "", "", "", "", "100", "USD", "1.1" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());

        AccountTransferEntry entry = (AccountTransferEntry) results.get(0).getSubject();
        AccountTransaction source = entry.getSourceTransaction();
        assertThat(source.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(source.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(source.getNote(), is("Notiz"));
        assertThat(source.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(source.getShares(), is(0L));
        assertThat(source.getSecurity(), is(nullValue()));

        List<Unit> units = source.getUnits().toList();
        assertThat(units.size(), is(1));
        assertThat(units.get(0).getType(), is(Unit.Type.GROSS_VALUE));
        assertThat(units.get(0).getAmount(), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(units.get(0).getForex(), is(Money.of(CurrencyUnit.USD, 100_00)));
        assertThat(units.get(0).getExchangeRate(), is(BigDecimal.valueOf(1.1)));

        AccountTransaction target = entry.getTargetTransaction();
        assertThat(target.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(target.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, 100_00)));
        assertThat(target.getNote(), is("Notiz"));
        assertThat(target.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(target.getShares(), is(0L));
        assertThat(target.getSecurity(), is(nullValue()));
        assertThat(target.getUnits().toList(), empty());
    }

    @Test
    public void testRequiredFieldDate()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "", "", "", "", "", "100", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testRequiredFieldAmount()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2015-01-01", "", "", "", "", "", "EUR", "", "", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testTaxesOnDividends()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100", "EUR", "DIVIDENDS",
                                        "SAP SE", "10", "Notiz", "10" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findAny()
                        .get().getSubject();
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 10_00)));
    }

    @Test
    public void testDetectionOfFeeRefunds()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        // setup custom mapping from string -> type

        Map<String, Column> field2column = buildField2Column(extractor);
        Column typeColumn = field2column.get(Messages.CSVColumn_Type);
        @SuppressWarnings("unchecked")
        EnumField<AccountTransaction.Type> field = (EnumField<AccountTransaction.Type>) typeColumn.getField();

        @SuppressWarnings("unchecked")
        EnumMapFormat<AccountTransaction.Type> format = (EnumMapFormat<Type>) field.guessFormat(new Client(), null)
                        .getFormat();
        format.map().put(AccountTransaction.Type.FEES_REFUND, "Gebührenerstattung");
        format.map().put(AccountTransaction.Type.FEES, "Gebühren");
        typeColumn.setFormat(new FieldFormat(Messages.CSVColumn_Type, format));

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2017-04-21", "", "", "", "", "10", "", "Gebührenerstattung", "", "", "", "" },
                        new String[] { "2017-04-21", "", "", "", "", "20", "", "Gebühren", "", "", "", "" }),
                        field2column, errors);

        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction t1 = (AccountTransaction) results.stream() //
                        .filter(i -> i instanceof TransactionItem) //
                        .filter(i -> ((AccountTransaction) ((TransactionItem) i).getSubject())
                                        .getType() == AccountTransaction.Type.FEES_REFUND)
                        .findAny().get().getSubject();

        assertThat(t1.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));

        AccountTransaction t2 = (AccountTransaction) results.stream() //
                        .filter(i -> i instanceof TransactionItem) //
                        .filter(i -> ((AccountTransaction) ((TransactionItem) i).getSubject())
                                        .getType() == AccountTransaction.Type.FEES)
                        .findAny().get().getSubject();

        assertThat(t2.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20))));
    }

    @Test
    public void testScientificNotation()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        Map<String, Column> field2column = buildField2Column(extractor);

        // configure shares column to use english format

        Field field = extractor.getFields().stream().filter(f -> "shares".equals(f.getCode())).findFirst()
                        .orElseThrow();
        FieldFormat fieldFormat = field.getAvailableFieldFormats().stream()
                        .filter(ff -> "0,000.00".equals(ff.getCode())).findFirst().orElseThrow();

        Column column = new Column(9, field.getName());
        column.setField(field);
        column.setFormat(fieldFormat);
        field2column.put(field.getName(), column);

        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(
                        // upper capital
                        new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100", "EUR", "DIVIDENDS",
                                        "SAP SE", "1.98E-6", "Notiz" },
                        // lower capital
                        new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "100", "EUR", "DIVIDENDS",
                                        "SAP SE", "2.12e-6", "Notiz" }),
                        field2column, errors);

        assertThat(results, hasItem(dividend(hasShares(0.00000198))));
        assertThat(results, hasItem(dividend(hasShares(0.00000212))));
    }

    @Test
    public void testInterestWithTaxesTransaction()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "2013-01-01", "11:00:00", // Date + Time
                                        "", "", "", // ISIN + TickerSymbol + WKN
                                        "7.5", "EUR", // Amount + Currency
                                        "INTEREST", // Type
                                        "", "", //  Security name + Shares
                                        "Notiz", // Note
                                        "2.5", "", // Taxes +  Fee
                                        "", "", "", // account + account2nd + portfolio
                                        "10", "EUR", // Gross + Gross currency
                                        "" }), // Exchange rate
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(
                        hasDate("2013-01-01T11:00"),
                        hasAmount("EUR", 7.50), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 2.50),  hasFees("EUR", 0.00), //
                        hasSource(null), hasNote("Notiz"))));
    }

    @Test
    public void testBuyTransactionWithForex()
    {
        Client client = new Client();
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        security.setCurrencyCode("USD");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(new String[] { //
                        "2013-01-02", "", // Date + Time
                        "DE0007164600", "SAP", "", // ISIN + TickerSymbol + WKN
                        "-100", "EUR", // Amount + Currency
                        "SELL", // Type
                        "SAP SE", "1.9", // Security name + Shares
                        "Notiz", // Note
                        "12", "", // Taxes + Fee
                        "", "", "", // account + account2nd + portfolio
                        "110", "USD", // Gross + Gross currency
                        "0.9091" }), // Exchange rate
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check that gross value is fixed taken taxes into account
        assertThat(results, hasItem(sale( //
                        hasDate("2013-01-02"), hasShares(1.9), //
                        hasSecurity(hasTicker("SAP.DE")), //
                        hasAmount("EUR", 100.00), //
                        hasGrossValue("EUR", 112.00), //
                        hasForexGrossValue("USD", 123.20), // 112.00 * 0.9091
                        hasTaxes("EUR", 12.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGrossValueIsCreated()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("LU0419741177");
        security.setCurrencyCode(CurrencyUnit.USD);
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(new String[] { //
                        "2015-09-15", "XX:XX", // Date + Time
                        "LU0419741177", "", "", // ISIN + TickerSymbol + WKN
                        "56", "EUR", // Amount + Currency
                        "BUY", // Type
                        "", "-0.701124", // Security name + Shares
                        "Notiz", // Note
                        "", "0.14", // Taxes + Fee
                        "", "", "", // account + account2nd + portfolio
                        "", "USD", // Gross + Gross currency
                        "1.1194" }), // Exchange rate
                        buildField2Column(extractor), errors);
        
        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check that gross value is created with given exchange rate
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-09-15"), hasShares(0.701124), //
                        hasSecurity(hasIsin("LU0419741177")), //
                        hasAmount("EUR", 56.00), //
                        hasGrossValue("EUR", 55.86), //
                        hasForexGrossValue("USD", 62.53), // 1.1194 * 55.86
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.14))));
    }

    @Test
    public void testExchangeRateIsDerivedFromGrossValue()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("LU0419741177");
        security.setCurrencyCode(CurrencyUnit.USD);
        client.addSecurity(security);

        CSVExtractor extractor = new CSVAccountTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList(new String[] { //
                        "2015-09-15", "XX:XX", // Date + Time
                        "LU0419741177", "", "", // ISIN + TickerSymbol + WKN
                        "1000", "EUR", // Amount + Currency
                        "TRANSFER_OUT", // Type
                        "", "", // Security name + Shares
                        "Notiz", // Note
                        "", "", // Taxes + Fee
                        "", "", "", // account + account2nd + portfolio
                        "1112", "USD", // Gross + Gross currency
                        "" }), // Exchange rate
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR, "USD");

        // check that gross value is created with given exchange rate
        assertThat(results, hasItem(outboundCash( //
                        hasDate("2015-09-15"), //
                        hasAmount("EUR", 1000.00), //
                        hasForexGrossValue("USD", 1112.00), hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRoundingOnExchangeRates()
    {
        var rawInput = """
                        Date,Value,Type,Currency,Name,Shares,Portfolio,ExchangeRate,Gross,CurrencyGross,Ticker,Account
                        2025-06-20,95.26,DIVIDENDS,CAD,iShares MSCI Emerging Markets Min Vol Factor ETF,,THE_CASH_ACCOUNT,1.3795,69.05400507,USD,EEMV,THE_CASH_ACCOUNT
                        """;

        var csv = rawInput.lines() //
                        .filter(line -> !line.isBlank()) //
                        .map(line -> line.split(",", -1)) // keep empty fields
                        .toList();

        var extractor = new CSVAccountTransactionExtractor(new Client());

        var errors = new ArrayList<Exception>();
        // the extract method gets only the data rows -> use sub list
        var results = extractor.extract(1, csv.subList(1, 2), buildField2Column(extractor, csv.get(0)), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertThat(results, hasItem(security( //
                        hasTicker("EEMV"), //
                        hasName("iShares MSCI Emerging Markets Min Vol Factor ETF"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-20"), //
                        hasShares(0), //
                        hasAmount("CAD", 95.26), //
                        hasForexGrossValue("USD", 69.05400507), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        var dividendTransaction = results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .map(i -> (AccountTransaction) i.getSubject()).get();

        var status = new CheckForexGrossValueAction().process(dividendTransaction, new Account());
        assertThat(status.getMessage(), status.getCode(), is(Status.Code.OK));
    }
}
