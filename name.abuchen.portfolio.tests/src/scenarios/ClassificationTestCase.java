package scenarios;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class ClassificationTestCase
{
    private static TestCurrencyConverter converter = new TestCurrencyConverter();

    private static Client client;

    private static Security adidas;

    private static Interval interval = Interval.of(LocalDate.parse("2011-12-31"),
                    LocalDate.parse("2012-01-31"));

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("classification_test_case.xml"));

        adidas = client.getSecurities().stream().filter(s -> "Adidas AG".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    @Test
    public void testCase1()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_1".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        List<Exception> warnings = new ArrayList<>();

        // check performance of sub-category (containing only one security) is
        // identical with performance of security

        Classification category_security = case1.getAllClassifications().stream()
                        .filter(c -> "category_security".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        PerformanceIndex index_cat_security = PerformanceIndex.forClassification(client, converter, category_security,
                        interval, warnings);
        assertThat(warnings.isEmpty(), is(true));

        PerformanceIndex index_adidas = PerformanceIndex.forInvestment(client, converter, adidas, interval, warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_cat_security.getDates(), is(index_adidas.getDates()));
        assertThat(index_cat_security.getAccumulatedPercentage(), is(index_adidas.getAccumulatedPercentage()));
        assertThat(index_cat_security.getDeltaPercentage(), is(index_adidas.getDeltaPercentage()));
        assertThat(index_cat_security.getTotals(), is(index_adidas.getTotals()));
        assertThat(index_cat_security.getTransferals(), is(index_adidas.getTransferals()));
    }

    @Test
    public void testCase1_subcategory()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_1".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification subcategory = case1.getAllClassifications().stream()
                        .filter(c -> "subcategory".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Client subcategoryClient = new ClientClassificationFilter(subcategory).filter(client);

        List<PortfolioTransaction> txp = subcategoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(1));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60)))))));

        List<AccountTransaction> txa = subcategoryClient.getAccounts().stream()
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60)))))));

        // check that other purchase transactions are converted into removals

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-01T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1019.80)))))));

        // check that dividend is included without taxes

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DIVIDENDS)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.44)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, subcategory,
                        interval, warnings);
        assertThat(warnings.isEmpty(), is(true));

        // total must match value shown in taxonomy view (calculated by
        // distributing the total portfolio among the categories)

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(3805.90)));
    }

    @Test
    public void testCase2_AccountWith50PercentAssignment()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_2".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification subcategory = case1.getAllClassifications().stream()
                        .filter(c -> "subcategory".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Client subcategoryClient = new ClientClassificationFilter(subcategory).filter(client);

        List<PortfolioTransaction> txp = subcategoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        // b/c the account is added only with 50% (but the security with 100%),
        // the purchase transaction is split into two

        assertThat(txp, hasSize(2));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("shares", is(Values.Share.factorize(23.5))), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 / 2d)))))));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_INBOUND)), //
                        hasProperty("shares", is(Values.Share.factorize(23.5))), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 / 2d)))))));

        List<AccountTransaction> txa = subcategoryClient.getAccounts().stream()
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 / 2d)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-01T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1019.80 / 2d)))))));

        // check that dividend is included without taxes

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DIVIDENDS)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.44)))))));

        // check correction for dividend payment (w/o taxes and only 50%)

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.44 - 19.44 / 2)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, subcategory,
                        interval, warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(3181.82)));
    }

    @Test
    public void testCase3_Security30Account50()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_3".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification subcategory = case1.getAllClassifications().stream()
                        .filter(c -> "subcategory".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Client subcategoryClient = new ClientClassificationFilter(subcategory).filter(client);

        List<PortfolioTransaction> txp = subcategoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(1));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.3d)))))));

        List<AccountTransaction> txa = subcategoryClient.getAccounts().stream()
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.3d)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.2d)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-01T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1019.80 / 2d)))))));

        // check that dividend is included without taxes

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DIVIDENDS)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.44 * 0.3d)))))));

        // check correction for dividend payment (w/o taxes and only 50%)

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR,
                                        Values.Amount.factorize(19.44 / 2 - 24.44 * 0.3d)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, subcategory,
                        interval, warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(1391.40)));
    }

    @Test
    public void testCase4_TwoAccountsEach100Percent()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_4".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case1.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(0));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60)))))));

        // check that transfer between two accounts is created

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)))))));

        // check that taxes are not included:

        // tax refund w/o security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-26T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)))))));

        // tax refund w/ security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-27T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)))))));

        // tax transaction
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-28T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(1783.02)));
    }

    @Test
    public void testCase5_Account70Account30()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_5".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case1.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(0));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.7)))))));

        // check that transfer between two accounts is created

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.4)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        // check that taxes are not included:

        // tax refund w/o security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-26T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 * 0.3)))))));

        // tax refund w/ security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-27T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 * 0.3)))))));

        // tax transaction
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-28T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 * 0.3)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(1034.17)));
    }

    @Test
    public void testCase6_Account30Account80()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_6".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case1.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(0));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().filter(a -> "Konto 1".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.3)))))));

        // check that transfer between two accounts is created

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        txa = categoryClient.getAccounts().stream().filter(a -> "Konto 2".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.5)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(802.34)));
    }

    @Test
    public void testCase7_Account30Account30()
    {
        Taxonomy case1 = client.getTaxonomies().stream().filter(t -> "case_7".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case1.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(0));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().filter(a -> "Konto 1".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.60 * 0.3)))))));

        // check that transfer between two accounts is created

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        txa = categoryClient.getAccounts().stream().filter(a -> "Konto 2".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-25T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100 * 0.3)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(534.91)));
    }

    @Test
    public void testCase8_Security100Account30_SecurityTransfer()
    {
        Taxonomy case8 = client.getTaxonomies().stream().filter(t -> "case_8".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case8.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        // check that inbound delivery is w/o taxes
        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-01T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_INBOUND)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3813.58 - 20)))))));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-02T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_INBOUND)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6640.30)))))));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // check that dividends are included + removal to reflect the 30%
        // assignment of the account

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DIVIDENDS)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.5)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.5 * 0.7)))))));

        // tax refund w/ security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-27T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 * 0.3)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(10563.68)));
    }

    @Test
    public void testCase9_Security100Account30_SecuritySold()
    {
        Taxonomy case9 = client.getTaxonomies().stream().filter(t -> "case_9".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case9.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        // check that inbound delivery is w/o taxes
        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-01T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_INBOUND)), //
                        hasProperty("shares", is(Values.Share.factorize(33))), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3813.58 - 20)))))));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(688.36 * 0.3d)))))));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_OUTBOUND)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR,
                                        Values.Amount.factorize((688.36 + 10) - (688.36 * 0.3d))))))));

        // check that shares of the split transaction add up to the expected
        // value, e.g. the total value of the original transaction
        assertThat(txp.stream().filter(t -> t.getDateTime().equals(LocalDateTime.parse("2012-01-10T00:00")))
                        .mapToLong(PortfolioTransaction::getShares).sum(), is(Values.Share.factorize(74.8)));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(688.36 * 0.3)))))));

        // check that dividends are included + removal to reflect the 30%
        // assignment of the account

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DIVIDENDS)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.5)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.5 * 0.7)))))));

        // tax refund w/ security
        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-27T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10 * 0.3)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(4105.28)));
    }

    @Test
    public void testCase10_Security30Account100_SecuritySold()
    {
        Taxonomy case10 = client.getTaxonomies().stream().filter(t -> "case_10".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case10.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.SELL)), //
                        hasProperty("shares", is(Values.Share.factorize(74.8 * 0.3d))), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(698.36 * 0.3d)))))));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.SELL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(698.36 * 0.3)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-10T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR,
                                        Values.Amount.factorize(688.36 - (698.36 * 0.3))))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(534.86)));
    }

    @Test
    public void testCase11_ForexSecurity100_ForexAccountTransfers()
    {
        Taxonomy case11 = client.getTaxonomies().stream().filter(t -> "case_11".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case11.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-16T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(476.48 * 0.7d)))))));

        assertThat(txp, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-16T00:00"))), //
                        hasProperty("type", is(PortfolioTransaction.Type.DELIVERY_INBOUND)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.USD,
                                        Values.Amount.factorize((476.48 - 10) - (476.48 * 0.7d))))))));

        // check that shares of the split transaction add up to the expected
        // value, e.g. the total value of the original transaction
        assertThat(txp.stream().filter(t -> t.getDateTime().equals(LocalDateTime.parse("2012-01-16T00:00")))
                        .mapToLong(PortfolioTransaction::getShares).sum(), is(Values.Share.factorize(1)));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream()
                        .filter(a -> "Account Forex".equals(a.getName())).flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-16T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.BUY)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(476.48 * 0.7d)))))));

        // check cash transfers

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(100 * 0.7d)))))));

        txa = categoryClient.getAccounts().stream().filter(a -> "Konto 3".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(78.19 * 0.7d)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(78.19 * 0.3d)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        // 686.84 EUR (Konto 3 EUR)
        // 296.46 USD * 1.2141 (exchange rate in TestCurrencyConverter)
        // 2 * Apple 456.4799 / 1.2141 (quote for Apple)

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(686.84) //
                                        + Values.Amount.factorize(296.46 / 1.2141)
                                        + Values.Amount.factorize(2 * 456.4799 / 1.2141)));
    }

    @Test
    public void testCase12_ForexAccountOut100_AccountIn50()
    {
        Taxonomy case12 = client.getTaxonomies().stream().filter(t -> "case_12".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        // check performance of 'subcategory'

        Classification ccategory = case12.getAllClassifications().stream().filter(c -> "category".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Client categoryClient = new ClientClassificationFilter(ccategory).filter(client);

        List<PortfolioTransaction> txp = categoryClient.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txp, hasSize(0));

        List<AccountTransaction> txa = categoryClient.getAccounts().stream()
                        .filter(a -> "Account Forex".equals(a.getName())).flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // check cash transfers

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_OUT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(50)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(50)))))));

        txa = categoryClient.getAccounts().stream().filter(a -> "Konto 3".equals(a.getName()))
                        .flatMap(a -> a.getTransactions().stream()).collect(Collectors.toList());

        assertThat(txa, hasItem(allOf( //
                        hasProperty("dateTime", is(LocalDateTime.parse("2012-01-17T00:00"))), //
                        hasProperty("type", is(AccountTransaction.Type.TRANSFER_IN)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(78.19 * 0.5d)))))));

        // create index

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index_subcategory = PerformanceIndex.forClassification(client, converter, ccategory, interval,
                        warnings);
        assertThat(warnings.isEmpty(), is(true));

        // 343.42 EUR (Konto 3 EUR)
        // 423.52 USD / 1.2141 (exchange rate in TestCurrencyConverter)

        assertThat(index_subcategory.getTotals()[index_subcategory.getTotals().length - 1],
                        is(Values.Amount.factorize(343.42) //
                                        + Values.Amount.factorize(423.52 / 1.2141)));
    }
}
