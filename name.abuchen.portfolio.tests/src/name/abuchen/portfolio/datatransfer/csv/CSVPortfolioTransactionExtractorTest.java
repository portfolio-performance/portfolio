package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CSVPortfolioTransactionExtractorTest
{
    @Test
    public void testDeliveryTransactionPlusSecurityCreation()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "DE0007164600", "SAP.DE", "", "SAP SE",
                                        "100", "EUR", "11", "10", "", "", "", "1,2", "DELIVERY_INBOUND", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is(nullValue()));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));

        PortfolioTransaction t = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findAny().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(t.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(1.2)));
        assertThat(t.getSecurity(), is(security));
        assertThat(t.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 11_00)));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 10_00)));
    }

    @Test
    public void testTransferTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-01", "", "DE0007164600", "SAP", "", "SAP SE",
                                        "100", "EUR", "11", "10", "", "", "", "1,2", "TRANSFER_IN", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        PortfolioTransferEntry entry = (PortfolioTransferEntry) results.stream()
                        .filter(PortfolioTransferItem.class::isInstance).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        PortfolioTransaction source = entry.getSourceTransaction();
        assertThat(source.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(source.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(source.getNote(), is("Notiz"));
        assertThat(source.getDateTime(), is(LocalDateTime.parse("2013-01-01T00:00")));
        assertThat(source.getShares(), is(Values.Share.factorize(1.2)));
        assertThat(source.getSecurity(), is(security));
        // security transfers do not support fees and taxes at the moment
        assertThat(source.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0)));
        assertThat(source.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 0)));

        PortfolioTransaction target = entry.getTargetTransaction();
        assertThat(target.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(target.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0)));
        assertThat(target.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 0)));
    }

    @Test
    public void testBuyTransaction()
    {
        Client client = new Client();
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "10:00", "DE0007164600", "SAP", "",
                                        "SAP SE", "100", "EUR", "11", "", "", "", "", "1,9", "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        PortfolioTransaction t = entry.getPortfolioTransaction();
        assertThat(t.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getNote(), is("Notiz"));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2013-01-02T10:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(1.9)));
        assertThat(t.getSecurity(), is(security));
        assertThat(t.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 11_00)));
        assertThat(t.getUnit(Unit.Type.TAX).isPresent(), is(false));

        AccountTransaction at = entry.getAccountTransaction();
        assertThat(at.getType(), is(AccountTransaction.Type.BUY));
        assertThat(at.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0)));
    }

    @Test
    public void testBuyTransactionWithForex()
    {
        Client client = new Client();
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        security.setCurrencyCode("USD");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "", "DE0007164600", "SAP", "", "SAP SE",
                                        "-100", "EUR", "", "12", "110", "USD", "0,9091", "1,9", "SELL", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        PortfolioTransaction t = entry.getPortfolioTransaction();
        assertThat(t.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(t.getSecurity(), is(security));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", 12_00)));
        assertThat(t.getUnit(Unit.Type.FEE).isPresent(), is(false));

        Unit grossAmount = t.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossAmount.getAmount(), is(Money.of("EUR", 112_00)));
        assertThat(grossAmount.getForex(), is(Money.of("USD", 123_20)));
        assertThat(grossAmount.getExchangeRate(), is(BigDecimal.valueOf(0.9091)));
    }

    @Test
    public void testTypeIsInferred()
    {
        Client client = new Client();
        Security security = new Security();
        security.setTickerSymbol("SAP.DE");
        security.setCurrencyCode("EUR");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "12:00", "", "SAP", "", "SAP SE",
                                        "-100", "EUR", "11", "", "", "", "", "1,9", "", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        BuySellEntry entry = (BuySellEntry) results.get(0).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("EUR", 100_00)));
    }

    @Test
    public void testThatSecurityIsCreatedByName()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "", "", "", "", "SAP SE", "100", "EUR",
                                        "11", "", "", "", "", "1,9", "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());

        SecurityItem item = (SecurityItem) results.stream().filter(SecurityItem.class::isInstance).findAny()
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(item.getSecurity().getName(), is("SAP SE"));
    }

    @Test
    public void testRequiredFieldDate()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "", "", "DE0007164600", "", "", "SAP SE", "100", "EUR",
                                        "11", "", "", "", "", "1,9", "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testRequiredFieldAmount()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "", "DE0007164600", "", "", "SAP SE", "",
                                        "EUR", "11", "", "", "", "", "1,9", "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testGrossValueIsCreated()
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("LU0419741177");
        security.setCurrencyCode(CurrencyUnit.USD);
        client.addSecurity(security);

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2015-09-15", "XX:XX", "LU0419741177", "", "", "", "56",
                                        "EUR", "0,14", "", "", "USD", "1,1194", "-0,701124", "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        BuySellEntry entry = (BuySellEntry) results.get(0).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(56))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.701124)));

        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.14))));

        assertThat(entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new).getForex(),
                        is(Money.of(security.getCurrencyCode(), Values.Amount.factorize(62.53))));
    }

    @Test
    public void testPlusSignInShares()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVPortfolioTransactionExtractor(client);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "2013-01-02", "", "IE00B4L5Y983", "", "",
                                        "ISHSIII-CORE MSCI WLD DLA", "99,97", "EUR", "", "", "", "", "", "+ 1,978",
                                        "BUY", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        BuySellEntry entry = (BuySellEntry) results.get(0).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(99.97))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.978)));
    }

}
