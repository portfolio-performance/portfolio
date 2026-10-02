package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.Element;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.ElementValueProvider;
import name.abuchen.portfolio.util.Interval;

public class StatementOfAssetsAccountPerformanceTest
{
    @Test
    @SuppressWarnings("nls")
    public void testLargePositiveAndNegativeInterestAllocations()
    {
        for (boolean charge : List.of(false, true))
        {
            Client client = new Client();
            long amount = 1_000_000_000_00L;
            var builder = new AccountBuilder().deposit_("2010-01-01", 2 * amount);
            if (charge)
                builder.interest_charge("2011-06-01", amount);
            else
                builder.interest("2011-06-01", amount);
            Account account = builder.addTo(client);
            Taxonomy taxonomy = new TaxonomyBuilder().addClassification("cash").addTo(client);
            for (int weight : List.of(Classification.ONE_HUNDRED_PERCENT, Classification.ONE_HUNDRED_PERCENT / 2))
            {
                var cash = taxonomy.getClassificationById("cash");
                cash.getAssignments().clear();
                cash.addAssignment(new Assignment(account, weight));
                var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                                new TestCurrencyConverter(), LocalDate.of(2011, 12, 31), taxonomy);
                Element element = model.getElements().stream().filter(Element::isAccount)
                                .filter(e -> e.getParent().getCategory().getClassification() == cash)
                                .findFirst().orElseThrow();
                long expected = amount * weight / Classification.ONE_HUNDRED_PERCENT;
                assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR,
                                model.getGlobalInterval()), is(Money.of(CurrencyUnit.EUR, charge ? -expected : expected)));
                assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR),
                                is(Money.of(CurrencyUnit.EUR, 2 * expected)));
            }
        }
    }

    @Test
    public void testAccountCashMovementsAndInterestAreSplitAcrossCategories()
    {
        Client client = new Client();
        Account account = new Account();
        account.setName("Cash"); //$NON-NLS-1$
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2010, 1, 1, 0, 0), CurrencyUnit.EUR,
                        1000_00, null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, 6, 1, 0, 0), CurrencyUnit.EUR,
                        50_00, null, AccountTransaction.Type.INTEREST));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, 12, 1, 0, 0), CurrencyUnit.EUR,
                        100_00, null, AccountTransaction.Type.REMOVAL));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, 11, 1, 0, 0), CurrencyUnit.EUR,
                        20_00, null, AccountTransaction.Type.FEES));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2012, 1, 1, 0, 0), CurrencyUnit.EUR,
                        500_00, null, AccountTransaction.Type.DEPOSIT));
        client.addAccount(account);

        Taxonomy taxonomy = new Taxonomy("Assets"); //$NON-NLS-1$
        Classification root = new Classification(null, "root", "Assets"); //$NON-NLS-1$ //$NON-NLS-2$
        taxonomy.setRootNode(root);
        Classification cash = new Classification(root, "cash", "Cash"); //$NON-NLS-1$ //$NON-NLS-2$
        root.addChild(cash);
        cash.addAssignment(new Assignment(account, Classification.ONE_HUNDRED_PERCENT * 2 / 5));
        Classification other = new Classification(root, "other", "Other"); //$NON-NLS-1$ //$NON-NLS-2$
        root.addChild(other);
        other.addAssignment(new Assignment(account, Classification.ONE_HUNDRED_PERCENT * 3 / 5));

        LocalDate date = LocalDate.of(2011, 12, 31);
        Interval period = Interval.of(LocalDate.of(2010, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, taxonomy);
        model.calculatePerformanceAndInjectIntoElements(CurrencyUnit.EUR, period);

        Element accountElement = model.getElements().stream().filter(Element::isAccount)
                        .filter(e -> e.getParent().getCategory().getClassification() == cash).findFirst().orElseThrow();
        Element categoryElement = accountElement.getParent();
        Element totalElement = model.getElements().stream().filter(Element::isGroupByTaxonomy).findFirst()
                        .orElseThrow();

        var returnProvider = new ElementValueProvider(
                        LazySecurityPerformanceRecord::getTrueTimeWeightedRateOfReturn, null,
                        PerformanceIndex::getFinalAccumulatedPercentage);
        assertThat((Double) returnProvider.getValue(accountElement, CurrencyUnit.EUR, period), closeTo(0.03, 0.0001));
        assertThat((Double) returnProvider.getValue(categoryElement, CurrencyUnit.EUR, period), closeTo(0.03, 0.0001));

        Function<Stream<Object>, Object> sum = values -> values.map(value -> (Money) value)
                        .collect(MoneyCollectors.sum(CurrencyUnit.EUR));
        var purchaseValueProvider = new ElementValueProvider(
                        record -> record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), sum,
                        (element, currencyCode, interval) -> StatementOfAssetsViewer.accountPurchaseValue(model,
                                        element, currencyCode), null);
        var profitLossProvider = new ElementValueProvider(
                        record -> record.getCapitalGainsOnHoldings(CostMethod.FIFO), sum,
                        (element, currencyCode, interval) -> StatementOfAssetsViewer.accountInterest(model, element,
                                        currencyCode, interval), null);

        assertThat(purchaseValueProvider.getValue(accountElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 352_00)));
        assertThat(profitLossProvider.getValue(accountElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 20_00)));
        assertThat(purchaseValueProvider.getValue(categoryElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 352_00)));
        assertThat(profitLossProvider.getValue(categoryElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 20_00)));
        assertThat(purchaseValueProvider.getValue(totalElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 880_00)));
        assertThat(profitLossProvider.getValue(totalElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(accountElement.getValuation(), is(Money.of(CurrencyUnit.EUR, 372_00)));

        var securityOnlyProvider = new ElementValueProvider(LazySecurityPerformanceRecord::getSumOfDividends, sum);
        assertThat(securityOnlyProvider.getValue(categoryElement, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 0)));

        Interval afterInterest = Interval.of(LocalDate.of(2011, 6, 1), date);
        assertThat(profitLossProvider.getValue(accountElement, CurrencyUnit.EUR, afterInterest),
                        is(Money.of(CurrencyUnit.EUR, 0)));
    }
}
