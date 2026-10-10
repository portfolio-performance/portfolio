package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertSame;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.Element;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer.ElementValueProvider;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class StatementOfAssetsAccountPerformanceTest
{
    @Test
    public void testInterestAllocationRoundsHalfCentsDown()
    {
        assertSmallInterestAllocation(false, Classification.ONE_HUNDRED_PERCENT / 2, 1, 1);
    }

    @Test
    public void testInterestChargeAllocationRoundsHalfCentsDown()
    {
        assertSmallInterestAllocation(true, Classification.ONE_HUNDRED_PERCENT / 2, -1, -1);
    }

    @Test
    public void testInterestAllocationIncludesUnassignedPosition()
    {
        assertSmallInterestAllocation(false, Classification.ONE_HUNDRED_PERCENT / 4, 1, 2);
    }

    private void assertSmallInterestAllocation(boolean charge, int weight, long expectedCash, long expectedUnassigned)
    {
        var client = new Client();
        var builder = new AccountBuilder().deposit_("2010-01-01", 100);
        if (charge)
            builder.interest_charge("2011-06-01", 3);
        else
            builder.interest("2011-06-01", 3);
        var account = builder.addTo(client);
        var taxonomy = new TaxonomyBuilder().addClassification("cash").addTo(client);
        var cash = taxonomy.getClassificationById("cash");
        cash.addAssignment(new Assignment(account, weight));
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), LocalDate.of(2011, 12, 31), taxonomy);
        var cashElement = model.getElements().stream().filter(Element::isAccount)
                        .filter(e -> e.getParent().getCategory().getClassification() == cash).findFirst().orElseThrow();
        var unassignedElement = model.getElements().stream().filter(Element::isAccount)
                        .filter(e -> Classification.UNASSIGNED_ID
                                        .equals(e.getParent().getCategory().getClassification().getId()))
                        .findFirst().orElseThrow();

        assertThat(StatementOfAssetsViewer.accountInterest(model, cashElement, CurrencyUnit.EUR,
                        model.getGlobalInterval()), is(Money.of(CurrencyUnit.EUR, expectedCash)));
        assertThat(StatementOfAssetsViewer.accountInterest(model, unassignedElement, CurrencyUnit.EUR,
                        model.getGlobalInterval()), is(Money.of(CurrencyUnit.EUR, expectedUnassigned)));
    }

    @Test
    public void testLargeInterestAllocation()
    {
        assertLargeInterestAllocation(false, Classification.ONE_HUNDRED_PERCENT);
    }

    @Test
    public void testLargeSplitInterestAllocation()
    {
        assertLargeInterestAllocation(false, Classification.ONE_HUNDRED_PERCENT / 2);
    }

    @Test
    public void testLargeInterestChargeAllocation()
    {
        assertLargeInterestAllocation(true, Classification.ONE_HUNDRED_PERCENT);
    }

    @Test
    public void testLargeSplitInterestChargeAllocation()
    {
        assertLargeInterestAllocation(true, Classification.ONE_HUNDRED_PERCENT / 2);
    }

    private void assertLargeInterestAllocation(boolean charge, int weight)
    {
        var client = new Client();
        long amount = 1_000_000_000_00L;
        var builder = new AccountBuilder().deposit_("2010-01-01", 2 * amount);
        if (charge)
            builder.interest_charge("2011-06-01", amount);
        else
            builder.interest("2011-06-01", amount);
        var account = builder.addTo(client);
        var taxonomy = new TaxonomyBuilder().addClassification("cash").addTo(client);
        var cash = taxonomy.getClassificationById("cash");
        cash.addAssignment(new Assignment(account, weight));
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), LocalDate.of(2011, 12, 31), taxonomy);
        var element = model.getElements().stream().filter(Element::isAccount)
                        .filter(e -> e.getParent().getCategory().getClassification() == cash).findFirst().orElseThrow();
        long expected = amount * weight / Classification.ONE_HUNDRED_PERCENT;
        assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR,
                        model.getGlobalInterval()), is(Money.of(CurrencyUnit.EUR, charge ? -expected : expected)));
        assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR,
                        model.getGlobalInterval()), is(Money.of(CurrencyUnit.EUR, 2 * expected)));
    }

    @Test
    public void testAccountCashMovementsAndInterestAreSplitAcrossCategories()
    {
        var client = new Client();
        var account = new AccountBuilder().deposit_("2010-01-01", 1000_00).interest("2011-06-01", 50_00)
                        .withdraw("2011-12-01", 100_00).fees____("2011-11-01", 20_00)
                        .deposit_("2012-01-01", 500_00).addTo(client);

        var taxonomy = new Taxonomy("Assets");
        var root = new Classification(null, "root", "Assets");
        taxonomy.setRootNode(root);
        var cash = new Classification(root, "cash", "Cash");
        root.addChild(cash);
        cash.addAssignment(new Assignment(account, Classification.ONE_HUNDRED_PERCENT * 2 / 5));
        var other = new Classification(root, "other", "Other");
        root.addChild(other);
        other.addAssignment(new Assignment(account, Classification.ONE_HUNDRED_PERCENT * 3 / 5));

        var date = LocalDate.of(2011, 12, 31);
        var period = Interval.of(LocalDate.of(2010, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, taxonomy);
        model.calculatePerformanceAndInjectIntoElements(CurrencyUnit.EUR, period);

        var accountElement = model.getElements().stream().filter(Element::isAccount)
                        .filter(e -> e.getParent().getCategory().getClassification() == cash).findFirst().orElseThrow();
        var categoryElement = accountElement.getParent();
        var totalElement = model.getElements().stream().filter(Element::isGroupByTaxonomy).findFirst()
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
                                        element, currencyCode, interval), null);
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

        var afterInterest = Interval.of(LocalDate.of(2011, 6, 1), date);
        assertThat(purchaseValueProvider.getValue(accountElement, CurrencyUnit.EUR, afterInterest),
                        is(Money.of(CurrencyUnit.EUR, 372_00)));
        assertThat(profitLossProvider.getValue(accountElement, CurrencyUnit.EUR, afterInterest),
                        is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(purchaseValueProvider.getValue(categoryElement, CurrencyUnit.EUR, afterInterest),
                        is(Money.of(CurrencyUnit.EUR, 372_00)));
        assertThat(purchaseValueProvider.getValue(totalElement, CurrencyUnit.EUR, afterInterest),
                        is(Money.of(CurrencyUnit.EUR, 930_00)));
    }

    @Test
    public void testForeignCurrencyAccountInReportingCurrency()
    {
        assertForeignCurrencyAccount(CurrencyUnit.EUR, 1047_72, 103_93);
    }

    @Test
    public void testForeignCurrencyAccountInAccountBaseCurrency()
    {
        assertForeignCurrencyAccount(CurrencyUnit.USD, 1214_10, 120_43);
    }

    private void assertForeignCurrencyAccount(String currency, long purchaseValue, long profitLoss)
    {
        var client = new Client();
        new AccountBuilder(CurrencyUnit.USD).deposit_("2014-12-31", 1214_10)
                        .interest("2015-01-02", 120_43).addTo(client);
        var date = LocalDate.of(2015, 1, 16);
        var period = Interval.of(LocalDate.of(2014, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, null);
        var element = model.getElements().stream().filter(Element::isAccount).findFirst().orElseThrow();
        var purchaseProvider = new ElementValueProvider(
                        record -> record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), null,
                        (e, c, i) -> StatementOfAssetsViewer.accountPurchaseValue(model, e, c, i), null);
        var profitProvider = new ElementValueProvider(
                        record -> record.getCapitalGainsOnHoldings(CostMethod.FIFO), null,
                        (e, c, i) -> StatementOfAssetsViewer.accountInterest(model, e, c, i), null);

        assertThat(purchaseProvider.getValue(element, currency, period), is(Money.of(currency, purchaseValue)));
        assertThat(profitProvider.getValue(element, currency, period), is(Money.of(currency, profitLoss)));
    }

    @Test
    public void testForeignCurrencyInterestAndChargesUseStatementDateRate()
    {
        var client = new Client();
        new AccountBuilder(CurrencyUnit.USD).deposit_("2014-12-31", 1214_10)
                        .interest("2015-01-02", 120_43).interest_charge("2015-01-05", 115_88).addTo(client);
        var date = LocalDate.of(2015, 1, 16);
        var period = Interval.of(LocalDate.of(2014, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, null);
        var element = model.getElements().stream().filter(Element::isAccount).findFirst().orElseThrow();
        var interest = StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR, period);
        var purchaseValue = StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR, period);
        assertThat(interest, is(Money.of(CurrencyUnit.EUR, 3_93)));
        assertThat(purchaseValue, is(Money.of(CurrencyUnit.EUR, 1047_72)));
        assertThat(purchaseValue.add(interest), is(element.getValuation()));
        assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.USD, period),
                        is(Money.of(CurrencyUnit.USD, 4_55)));
        assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.USD, period),
                        is(Money.of(CurrencyUnit.USD, 1214_10)));
    }

    @Test
    public void testWeightedClientFilterAndSharedAccountPerformance()
    {
        var client = new Client();
        var account = new AccountBuilder().deposit_("2010-01-01", 1000_00)
                        .interest("2011-12-31", 100_00).addTo(client);
        var taxonomy = new TaxonomyBuilder().addClassification("cash").addTo(client);
        taxonomy.getClassificationById("cash")
                        .addAssignment(new Assignment(account, Classification.ONE_HUNDRED_PERCENT / 2));
        var filter = new PortfolioClientFilter(List.of(), List.of(account),
                        Map.of(account, Classification.ONE_HUNDRED_PERCENT * 2 / 5));
        var date = LocalDate.of(2011, 12, 31);
        var period = Interval.of(LocalDate.of(2010, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, filter,
                        new TestCurrencyConverter(), date, taxonomy);
        model.calculatePerformanceAndInjectIntoElements(CurrencyUnit.EUR, period);
        var elements = model.getElements().stream().filter(Element::isAccount).toList();
        assertThat(elements.size(), is(2));
        for (var element : elements)
        {
            assertThat(element.getValuation(), is(Money.of(CurrencyUnit.EUR, 220_00)));
            assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR, period),
                            is(Money.of(CurrencyUnit.EUR, 20_00)));
            assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR, period),
                            is(Money.of(CurrencyUnit.EUR, 200_00)));
            assertThat(element.getPerformanceForCategoryTotals(CurrencyUnit.EUR, period)
                            .getFinalAccumulatedPercentage(), closeTo(0.1, 0.0001));
        }
        assertSame(elements.get(0).getPerformanceForCategoryTotals(CurrencyUnit.EUR, period),
                        elements.get(1).getPerformanceForCategoryTotals(CurrencyUnit.EUR, period));
    }

    @Test
    public void testFeesAndTaxesAffectPerformanceButNotInterestProfitLoss()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2010-01-01", 1000_00).interest("2011-12-31", 10_00, 2_00)
                        .fees____("2011-12-31", 30_00).fees_refund("2011-12-31", 5_00)
                        .tax_____("2011-12-31", 4_00).taxrefnd("2011-12-31", 1_00).addTo(client);
        var date = LocalDate.of(2011, 12, 31);
        var period = Interval.of(LocalDate.of(2010, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, null);
        var element = model.getElements().stream().filter(Element::isAccount).findFirst().orElseThrow();
        assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 972_00)));
        assertThat(element.getPerformanceForCategoryTotals(CurrencyUnit.EUR, period)
                        .getFinalAccumulatedPercentage(), closeTo(-0.018, 0.0001));
    }

    @Test
    public void testRepeatedAccountValueReadsReuseCurrencyConversions()
    {
        var conversions = new AtomicInteger();
        var converter = new TestCurrencyConverter()
        {
            @Override
            public ExchangeRate getRate(LocalDate date, String currencyCode)
            {
                conversions.incrementAndGet();
                return super.getRate(date, currencyCode);
            }
        };
        var client = new Client();
        new AccountBuilder(CurrencyUnit.USD).deposit_("2014-12-31", 1214_10)
                        .interest("2015-01-02", 120_43).addTo(client);
        var date = LocalDate.of(2015, 1, 16);
        var period = Interval.of(LocalDate.of(2014, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        converter, date, null);
        var element = model.getElements().stream().filter(Element::isAccount).findFirst().orElseThrow();
        model.calculatePerformanceAndInjectIntoElements(CurrencyUnit.EUR, period);
        int before = conversions.get();
        assertThat(StatementOfAssetsViewer.accountInterest(model, element, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 103_93)));
        assertThat(StatementOfAssetsViewer.accountPurchaseValue(model, element, CurrencyUnit.EUR, period),
                        is(Money.of(CurrencyUnit.EUR, 1047_72)));
        assertThat(conversions.get(), is(before));
    }

    @Test
    public void testAccountIRRAndAnnualizedTTWROR()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2010-01-01", 1000_00).interest("2011-12-31", 100_00).addTo(client);
        var date = LocalDate.of(2011, 12, 31);
        var period = Interval.of(LocalDate.of(2010, 12, 31), date);
        var model = new StatementOfAssetsViewer.Model(new PreferenceStore(), client, ClientFilter.NO_FILTER,
                        new TestCurrencyConverter(), date, null);
        model.calculatePerformanceAndInjectIntoElements(CurrencyUnit.EUR, period);
        var element = model.getElements().stream().filter(Element::isAccount).findFirst().orElseThrow();
        var irrProvider = new ElementValueProvider(LazySecurityPerformanceRecord::getIrr, null,
                        PerformanceIndex::getPerformanceIRR);
        var annualizedProvider = new ElementValueProvider(
                        LazySecurityPerformanceRecord::getTrueTimeWeightedRateOfReturnAnnualized, null,
                        PerformanceIndex::getFinalAccumulatedAnnualizedPercentage);
        assertThat((Double) irrProvider.getValue(element, CurrencyUnit.EUR, period), closeTo(0.1, 0.0001));
        assertThat((Double) annualizedProvider.getValue(element, CurrencyUnit.EUR, period), closeTo(0.1, 0.0001));
    }
}
