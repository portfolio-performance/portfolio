package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;

import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshotTest;
import name.abuchen.portfolio.snapshot.filter.ClientClassificationFilter;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.WithoutTaxesFilter;
import name.abuchen.portfolio.util.Interval;

public class Issue1897AddFeesToDividendTransactionsTest
{
    private static Client client;
    private static CurrencyConverter converter = new TestCurrencyConverter();
    private static Interval period = Interval.of(LocalDate.parse("2020-11-01"), //$NON-NLS-1$
                    LocalDate.parse("2020-12-31")); //$NON-NLS-1$

    @BeforeClass
    public static void setup() throws IOException
    {
        client = ClientFactory.load(Issue371PurchaseValueWithTransfersTest.class
                        .getResourceAsStream("Issue1897AddFeesToDividendTransactions.xml")); //$NON-NLS-1$
    }

    @Test
    public void testDefaultSnapshot()
    {
        // fees on purchase = 5 EUR
        // fees on dividend transaction = 10 EUR

        AccountTransaction tx = client.getAccounts().stream().flatMap(accounts -> accounts.getTransactions().stream())
                        .filter(t -> t.getType() == AccountTransaction.Type.DIVIDENDS).findFirst()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.0))));

        // test that taxes and fees are properly sorted

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, period);
        ClientPerformanceSnapshot.Category feesCategory = snapshot.getCategoryByType(CategoryType.FEES);

        assertThat(feesCategory.getValuation(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0 + 5.0))));
        assertThat(snapshot.getFees().size(), is(2));
        assertThat(tx, is(in(snapshot.getFees().stream().map(pair -> (Transaction) pair.getTransaction())
                        .collect(Collectors.toList()))));

        ClientPerformanceSnapshotTest.assertThatCalculationWorksOut(snapshot, converter);
    }

    @Test
    public void testSnapshotWithoutTaxes()
    {
        // test that fees are included even if filtered without taxes

        Client clientWithoutTaxes = new WithoutTaxesFilter().filter(client);
        ClientPerformanceSnapshot snapshotWithoutTaxes = new ClientPerformanceSnapshot(clientWithoutTaxes, converter,
                        period);

        assertSnapshot(snapshotWithoutTaxes);
    }

    private void assertSnapshot(ClientPerformanceSnapshot snapshot)
    {
        ClientPerformanceSnapshot.Category feesCategory = snapshot.getCategoryByType(CategoryType.FEES);

        assertThat(feesCategory.getValuation(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0 + 5.0))));
        assertThat(snapshot.getFees().size(), is(2));

        AccountTransaction tx = snapshot.getFees().stream().map(TransactionPair::getTransaction)
                        .filter(t -> t instanceof AccountTransaction).map(AccountTransaction.class::cast).findAny()
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(tx.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.0))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.0))));

        ClientPerformanceSnapshotTest.assertThatCalculationWorksOut(snapshot, converter);
    }

    @Test
    public void testSnapshotWithClassificationOfSecurityOnly()
    {
        Taxonomy taxonomy = client.getTaxonomy("cadeb697-2d4f-41e9-9dee-7473d4841608"); //$NON-NLS-1$

        Classification classification = taxonomy.getClassificationById("a0914e8b-3c1d-4264-b987-6fdbb2f46e76"); //$NON-NLS-1$

        ClientFilter filter = new ClientClassificationFilter(classification);

        Client filteredClient = filter.filter(client);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);

        assertSnapshot(snapshot);
    }

    @Test
    public void testSnapshotWithClassificationPlusAccountClassified()
    {
        Taxonomy taxonomy = client.getTaxonomy("fe97ac67-6446-4ead-ba5b-21aaa6695425"); //$NON-NLS-1$

        Classification classification = taxonomy.getClassificationById("4cfba210-7693-4237-a2d8-cc06f85dcbc5"); //$NON-NLS-1$

        ClientFilter filter = new ClientClassificationFilter(classification);

        Client filteredClient = filter.filter(client);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);

        assertSnapshot(snapshot);
    }

    @Test
    public void testSnapshotWithSecurityFilter()
    {
        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("ALLIANZ SE NA O.N.")); //$NON-NLS-1$

        Client filteredClient = new ClientSecurityFilter(security).filter(client);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);

        assertSnapshot(snapshot);
    }
}
