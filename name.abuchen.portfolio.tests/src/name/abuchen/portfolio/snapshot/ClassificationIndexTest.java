package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TaxonomyBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

import org.junit.Test;

@SuppressWarnings("nls")
public class ClassificationIndexTest
{
    private ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY( //
                    LocalDate.parse("2011-12-31"), LocalDate.parse("2012-01-08"));

    private Client createClient(int weight)
    {
        Client client = new Client();

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("one") //
                        .addTo(client);

        Security security = new SecurityBuilder() //
                        .addPrice("2011-12-31", 100 * Values.Quote.factor()) //
                        .addPrice("2012-01-03", 106 * Values.Quote.factor()) //
                        .addPrice("2012-01-08", 112 * Values.Quote.factor()) //
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
                        .buy(security, "2012-01-01", 50 * Values.Share.factor(), 50 * 101 * Values.Amount.factor()) //
                        .inbound_delivery(security, "2012-01-01", 100 * Values.Share.factor(),
                                        100 * 100 * Values.Amount.factor()) //
                        .sell(security, "2012-01-05", 50 * Values.Share.factor(), 50 * 105 * Values.Amount.factor()) //
                        .addTo(client);

        return client;
    }

    @Test
    public void testThat100PercentAssignmentIsIdenticalToClientPerformance()
    {
        Client client = createClient(Classification.ONE_HUNDRED_PERCENT);

        Classification classification = client.getTaxonomies().get(0).getClassificationById("one");

        List<Exception> warnings = new ArrayList<Exception>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification,
                        period, warnings);

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

        List<Exception> warnings = new ArrayList<Exception>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification,
                        period, warnings);

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

        List<Exception> warnings = new ArrayList<Exception>();

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex iClient = PerformanceIndex.forClient(client, converter, period, warnings);
        PerformanceIndex iClassification = PerformanceIndex.forClassification(client, converter, classification,
                        period, warnings);

        assertThat(warnings.isEmpty(), is(true));

        assertThat(iClient.getDates(), is(iClassification.getDates()));
        assertThat(iClient.getAccumulatedPercentage(), is(iClassification.getAccumulatedPercentage()));
        assertThat(iClient.getDeltaPercentage(), is(iClassification.getDeltaPercentage()));
        assertThat(half(iClient.getTotals()), is(iClassification.getTotals()));
        assertThat(half(iClient.getTransferals()), is(iClassification.getTransferals()));
    }

    private long[] half(long[] transferals)
    {
        long[] answer = new long[transferals.length];
        for (int ii = 0; ii < transferals.length; ii++)
            answer[ii] = transferals[ii] / 2;
        return answer;
    }
}
