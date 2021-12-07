package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TaxonomyBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class ClassificationIndexTest
{
    private Interval period = Interval.of( //
                    LocalDate.parse("2011-12-31"), LocalDate.parse("2012-01-08"));

    private Client createClient(int weight)
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("one") //
                        .addTo(client);

        Security security = new SecurityBuilder() //
                        .addPrice("2011-12-31", Values.Quote.factorize(100)) //
                        .addPrice("2012-01-03", Values.Quote.factorize(106)) //
                        .addPrice("2012-01-08", Values.Quote.factorize(112)) //
                        .assign(taxonomy, "one", weight) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2011-12-31", 10000 * Values.Amount.factor()) //
                        .interest("2012-01-01", 230 * Values.Amount.factor()) //
                        .deposit_("2012-01-02", 200 * Values.Amount.factor()) //
                        .interest("2012-01-02", 200 * Values.Amount.factor()) //
                        .withdraw("2012-01-03", 400 * Values.Amount.factor()) //
                        .fees____("2012-01-03", 234 * Values.Amount.factor()) //
                        .interest("2012-01-04", 293 * Values.Amount.factor()) //
                        .interest("2012-01-05", 293 * Values.Amount.factor()) //
                        .deposit_("2012-01-06", 5400 * Values.Amount.factor()) //
                        .interest("2012-01-06", 195 * Values.Amount.factor()) //
                        .withdraw("2012-01-07", 3697 * Values.Amount.factor()) //
                        .fees____("2012-01-07", 882 * Values.Amount.factor()) //
                        .fees____("2012-01-08", 1003 * Values.Amount.factor()) //
                        .dividend("2012-01-08", 100 * Values.Amount.factor(), security) //
                        .assign(taxonomy, "one", weight) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2012-01-01", Values.Share.factorize(50), Values.Amount.factorize(50 * 101)) //
                        .inbound_delivery(security, "2012-01-01", Values.Share.factorize(100),
                                        Values.Amount.factorize(100 * 100)) //
                        .sell(security, "2012-01-05", Values.Share.factorize(50), Values.Amount.factorize(50 * 105)) //
                        .addTo(client);

        return client;
    }

    @Test
    public void testThat100PercentAssignmentIsIdenticalToClientPerformance()
    {
        Client client = createClient(Classification.ONE_HUNDRED_PERCENT);

        Classification classification = client.getTaxonomies().get(0).getClassificationById("one");

        List<Exception> warnings = new ArrayList<>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification, period,
                        warnings);

        assertThat(warnings.isEmpty(), is(true));

        assertThat(iClient.getDates(), is(iClassification.getDates()));
        assertThat(iClient.getAccumulatedPercentage(), is(iClassification.getAccumulatedPercentage()));
        assertThat(iClient.getDeltaPercentage(), is(iClassification.getDeltaPercentage()));
        assertThat(iClient.getTotals(), is(iClassification.getTotals()));
        assertThat(iClient.getTransferals(), is(iClassification.getTransferals()));
    }

    @Test
    public void testThatPartialAssignmentIsNOTIdenticalToClientPerformance()
    {
        Client client = createClient(Classification.ONE_HUNDRED_PERCENT);

        Classification classification = client.getTaxonomies().get(0).getClassificationById("one");

        // remove account assignment
        classification.getAssignments().remove(1);

        List<Exception> warnings = new ArrayList<>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification, period,
                        warnings);

        assertThat(warnings.isEmpty(), is(true));

        assertThat(iClient.getAccumulatedPercentage(), is(not(iClassification.getAccumulatedPercentage())));
        assertThat(iClient.getDeltaPercentage(), is(not(iClassification.getDeltaPercentage())));
        assertThat(iClient.getTotals(), is(not(iClassification.getTotals())));
        assertThat(iClient.getTransferals(), is(not(iClassification.getTransferals())));
    }

    @Test
    public void testThat50PercentAssignmentHasIdenticalPerformanceButOnly50PercentTotals()
    {
        Client client = createClient(Classification.ONE_HUNDRED_PERCENT / 2);

        Classification classification = client.getTaxonomies().get(0).getClassificationById("one");

        List<Exception> warnings = new ArrayList<>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification, period,
                        warnings);

        assertThat(warnings.isEmpty(), is(true));

        assertThat(iClient.getDates(), is(iClassification.getDates()));
        assertThat(half(iClient.getTotals()), is(iClassification.getTotals()));
        assertThat(half(iClient.getTransferals()), is(iClassification.getTransferals()));
        assertThat(iClient.getAccumulatedPercentage(), is(iClassification.getAccumulatedPercentage()));
        assertThat(iClient.getDeltaPercentage(), is(iClassification.getDeltaPercentage()));
    }

    private long[] half(long[] transferals)
    {
        long[] answer = new long[transferals.length];
        for (int ii = 0; ii < transferals.length; ii++)
            answer[ii] = Math.round(transferals[ii] / 2.0);
        return answer;
    }

    @Test
    public void testThatTaxesAreNotIncludedInTTWRORCalculation()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2015-12-31", Values.Quote.factorize(100)) //
                        .addPrice("2016-12-31", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2014-01-01", Values.Amount.factorize(1000)).addTo(client);

        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setDateTime(LocalDateTime.parse("2016-06-01T00:00"));
        t.setSecurity(security);
        t.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        t.setShares(Values.Share.factorize(10));
        account.addTransaction(t);

        Portfolio portfolio = new PortfolioBuilder(account) //
                        .addTo(client);

        BuySellEntry buy = new BuySellEntry(portfolio, account);
        buy.setType(PortfolioTransaction.Type.BUY);
        buy.setDate(LocalDateTime.parse("2015-12-31T00:00"));
        buy.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        buy.setShares(Values.Share.factorize(10));
        buy.setSecurity(security);
        buy.insert();

        BuySellEntry sell = new BuySellEntry(portfolio, account);
        sell.setType(PortfolioTransaction.Type.SELL);
        sell.setDate(LocalDateTime.parse("2016-12-31T00:00"));
        sell.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1070)));
        sell.setShares(Values.Share.factorize(10));
        sell.setSecurity(security);
        sell.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30))));
        sell.insert();

        Classification classification = new Classification(null, null);
        classification.addAssignment(new Assignment(security));

        List<Exception> warnings = new ArrayList<>();

        PerformanceIndex index = PerformanceIndex.forClassification(client, new TestCurrencyConverter(), classification,
                        Interval.of(LocalDate.parse("2015-01-01"), LocalDate.parse("2017-01-01")), warnings);

        assertThat(warnings.isEmpty(), is(true));

        // dividend payment 10% * quote change 10%
        assertThat(index.getFinalAccumulatedPercentage(), IsCloseTo.closeTo((1.1 * 1.1) - 1, 0.000000001d));

        // add taxes to dividend payment

        t.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50))));

        index = PerformanceIndex.forClassification(client, new TestCurrencyConverter(), classification,
                        Interval.of(LocalDate.parse("2015-01-01"), LocalDate.parse("2017-01-01")), warnings);

        // dividend payment 15% * quote change 10%
        assertThat(index.getFinalAccumulatedPercentage(), IsCloseTo.closeTo((1.15 * 1.1) - 1, 0.000000001d));
    }
}
