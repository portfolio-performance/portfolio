package scenarios;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;

@SuppressWarnings("nls")
public class SecurityTaxAndFeeAccountTransactionsTestCase
{
    private static TestCurrencyConverter converter = new TestCurrencyConverter();

    private static Client client;

    private static Security adidas;

    private static ReportingPeriod interval = new ReportingPeriod.FromXtoY(LocalDate.parse("2016-12-31"),
                    LocalDate.parse("2017-01-31"));

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(
                        SecurityTestCase.class.getResourceAsStream("security_tax_and_fee_account_transactions.xml"));

        adidas = client.getSecurities().stream().filter(s -> "Adidas AG".equals(s.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    @Test
    public void testAdidas()
    {
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, converter, interval);

        SecurityPerformanceRecord record = snapshot.getRecords().stream().filter(r -> r.getSecurity().equals(adidas))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        assertThat(record.getTransactions().size(), is(6));

        // fees and taxes from the buy transaction + separate transactions
        assertThat(record.getFees(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10d + 10d - 5d))));
        assertThat(record.getTaxes(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10d + 25d - 5d))));

        // delta is end value - starting value including all fees and taxes
        assertThat(record.getDelta(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1456.5 - 1533 - 10 + 5 - 25 + 5))));

        // IRR as calculated in Excel

        // @formatter:off
        
        // date          w/ tax       w/o tax
        // 02.01.17     -1533         -1523
        // 09.01.17       -10           -10
        // 10.01.17         5             5
        // 11.01.17       -25             0
        // 12.01.17         5             0
        // 31.01.17      1456,5        1456,5        
        // XIRR    -0,573350433  -0,453151944
        
        // @formatter:on

        assertThat(record.getIrr(), closeTo(-0.453151944d, 1.0e-8));

        // check filtering - ClientSecurityFilter

        Client filteredClient = new ClientSecurityFilter(adidas).filter(client);

        checkFilteredClientAdidas(filteredClient, 1.0);

        // check filtering - ClientClassificationFilter

        Taxonomy case1 = client.getTaxonomies().stream()
                        .filter(t -> "case_full_classification_adidas".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Classification classification = case1.getAllClassifications().stream()
                        .filter(c -> "category_security".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        filteredClient = new ClientClassificationFilter(classification).filter(client);

        checkFilteredClientAdidas(filteredClient, 1.0);

        // TTWROR must be identical to one calculated via ClientSecurityFilter
        // (implicitly used by the SecurityPeformanceSnapshot)

        PerformanceIndex index = PerformanceIndex.forClassification(client, converter, classification, interval,
                        new ArrayList<>());
        assertThat(index.getFinalAccumulatedPercentage(), is(record.getTrueTimeWeightedRateOfReturn()));

        // a partial assignment of the security should be identical to the full
        // assignment - only the weight differs

        Taxonomy case2 = client.getTaxonomies().stream()
                        .filter(t -> "case_partial_classification_adidas".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        classification = case2.getAllClassifications().stream().filter(c -> "category_security".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        filteredClient = new ClientClassificationFilter(classification).filter(client);
        checkFilteredClientAdidas(filteredClient, 0.5);

    }

    private void checkFilteredClientAdidas(Client filteredClient, double weight)
    {
        List<AccountTransaction> txa = filteredClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // expect 4 transactions: the two fees-related ones plus the
        // deposit/removal to have the account balance at zero

        assertThat(txa.size(), is(4));

        // check balance is zero

        ClientSnapshot balance = ClientSnapshot.create(filteredClient, converter, interval.getEndDate());
        assertThat(balance.getMonetaryAssets(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1456.5 * weight))));
        assertThat(balance.getAccounts().size(), is(1));
        assertThat(balance.getAccounts().iterator().next().getFunds(), is(Money.of(CurrencyUnit.EUR, 0)));

        // check for additional transactions

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0 * weight)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount",
                                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.0 * weight)))))));
    }

    @Test
    public void checkTaxonomyWihtSecurityAndAccountAssignment()
    {
        // if account and security are both classified, only the tax
        // transactions are converted to deposits or removals

        Taxonomy case3 = client.getTaxonomies().stream()
                        .filter(t -> "case_full_classification_adidas_with_account".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Classification classification = case3.getAllClassifications().stream()
                        .filter(c -> "category_security".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Client filteredClient = new ClientClassificationFilter(classification).filter(client);

        List<AccountTransaction> txa1 = filteredClient.getAccounts().stream()
                        .flatMap(a1 -> a1.getTransactions().stream()).collect(Collectors.toList());

        // expect 7 transactions: the original 6 plus the removal of the taxes
        // as
        // part of the buy

        assertThat(txa1.size(), is(7));

        // check for additional transactions

        assertThat(txa1, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.FEES)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0)))))));

        assertThat(txa1, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.FEES_REFUND)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.0)))))));

        assertThat(txa1, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-11"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.0)))))));

        assertThat(txa1, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-12"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.0)))))));

        // if the account is fully classified, but the security only partially,
        // there must be offset transactions to make sure that the balance of
        // the account remains correct

        Taxonomy case4 = client.getTaxonomies().stream()
                        .filter(t -> "case_partial_classification_adidas_with_account".equals(t.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        classification = case4.getAllClassifications().stream().filter(c -> "category_security".equals(c.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        filteredClient = new ClientClassificationFilter(classification).filter(client);

        List<AccountTransaction> txa = filteredClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // expect 9 transactions: the original 6 plus the removal of the fees as
        // part of the buy + partial transactions to offset fees

        assertThat(txa.size(), is(9));

        // check for additional transactions

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.FEES)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.FEES_REFUND)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.5)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.5)))))));
    }

    @Test
    public void checkTaxonomyWihtAccountOnlyAssignment()
    {
        // if only the account is classified, the fees and taxes related to the
        // security must be plain deposit and removal transactions

        Taxonomy case5 = client.getTaxonomies().stream().filter(t -> "case_account_classification".equals(t.getName()))
                        .findAny().orElseThrow(IllegalArgumentException::new);

        Classification classification = case5.getAllClassifications().stream()
                        .filter(c -> "category_account".equals(c.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        Client filteredClient = new ClientClassificationFilter(classification).filter(client);

        List<AccountTransaction> txa = filteredClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // expect the 6 original transactions

        assertThat(txa.size(), is(6));

        // check for additional transactions

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5)))))));
    }

    @Test
    public void checkPortfolioClientFilter()
    {
        Client filteredClient = new PortfolioClientFilter(client.getPortfolios().get(0)).filter(client);

        List<AccountTransaction> txa = filteredClient.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .collect(Collectors.toList());

        // expect 8 transactions: the 4 tax and fees transactions plus the
        // offset transactions to keep the balance zero

        assertThat(txa.size(), is(8));

        // check balance is zero

        ClientSnapshot balance = ClientSnapshot.create(filteredClient, converter, interval.getEndDate());
        assertThat(balance.getMonetaryAssets(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1456.5))));
        assertThat(balance.getAccounts().size(), is(1));
        assertThat(balance.getAccounts().iterator().next().getFunds(), is(Money.of(CurrencyUnit.EUR, 0)));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-09"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-10"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.0)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-11"))), //
                        hasProperty("type", is(AccountTransaction.Type.DEPOSIT)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.0)))))));

        assertThat(txa, hasItem(allOf( //
                        hasProperty("date", is(LocalDate.parse("2017-01-12"))), //
                        hasProperty("type", is(AccountTransaction.Type.REMOVAL)), //
                        hasProperty("monetaryAmount", is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.0)))))));

    }

}
