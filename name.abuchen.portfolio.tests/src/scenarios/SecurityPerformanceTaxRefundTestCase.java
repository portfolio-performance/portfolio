package scenarios;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

@SuppressWarnings("nls")
public class SecurityPerformanceTaxRefundTestCase
{
    /**
     * Feature: when calculating the performance of a security, do not include
     * taxes and tax refunds. Include taxes and tax refunds only when
     * calculating a performance of a porfolio and/or client.
     */
    @Test
    public void testSecurityPerformanceTaxRefund() throws IOException
    {
        Client client = ClientFactory.load(
                        SecurityTestCase.class.getResourceAsStream("security_performance_tax_refund.xml"),
                        new NullProgressMonitor());

        Security security = client.getSecurities().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        PortfolioTransaction delivery = portfolio.getTransactions().get(0);
        ReportingPeriod period = new ReportingPeriod.FromXtoY(Dates.date("2013-12-06"), Dates.date("2014-12-06"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, period);
        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity().getName(), is("Basf SE"));

        // no changes in holdings, ttwror must (without taxes and tax refunds):
        double startValue = delivery.getAmount() - delivery.getTaxes();
        double endValue = delivery.getShares() * security.getSecurityPrice(Dates.date("2014-12-06")).getValue()
                        / Values.Share.divider();
        double ttwror = (endValue / startValue) - 1;
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(ttwror, 0.0001));

        // accrued taxes must be 0 (paid 10 on delivery + 5 tax refund):
        assertThat(record.getTaxes(), is(5L * Values.Amount.factor()));

        // accrued fees must be 10 (paid 10 on delivery)
        assertThat(record.getFees(), is(10L * Values.Amount.factor()));

        // make sure that tax refund is included in transactions
        assertThat(record.getTransactions(), hasItem(isA(AccountTransaction.class)));

        // ttwror of classification must be identical to ttwror of security
        assertThatTTWROROfClassificationWithSecurityIsIdentical(client, period, ttwror);

        // check client performance + performance of portfolio + account
        PerformanceIndex clientIndex = assertPerformanceOfClient(client, period, ttwror);

        // the performance of the portfolio (w/o account) includes taxes

        // in this sample file, the valuation of the security drops by 971
        // euros. However, the client has total valuation of 8406 while the
        // portfolio has valuation of only 8401 as it does not include the tax
        // refund. Therefore the performances of the porfolio is worse than that
        // of the client.
        assertThatTTWROROfPortfolioIsLessThan(client, clientIndex, ttwror);

        // the irr must not include taxes as well (compared with Excel):
        assertThat(record.getIrr(), closeTo(-0.030745789, 0.0001));

        // ensure the performance of the account is zero
        List<Exception> warnings = new ArrayList<Exception>();
        PerformanceIndex accountIndex = PerformanceIndex.forAccount(client, client.getAccounts().get(0), period,
                        warnings);
        assertThat(warnings, empty());
        assertThat(accountIndex.getFinalAccumulatedPercentage(), is(0d));
    }

    /**
     * Feature: Same as {@link #testSecurityPerformanceTaxRefunds} except that
     * now the security has been sold. Taxes paid when selling the security must
     * be ignored.
     */
    @Test
    public void testSecurityPerformanceTaxRefundAllSold() throws IOException
    {
        Client client = ClientFactory.load(
                        SecurityTestCase.class.getResourceAsStream("security_performance_tax_refund_all_sold.xml"),
                        new NullProgressMonitor());

        Portfolio portfolio = client.getPortfolios().get(0);
        PortfolioTransaction delivery = portfolio.getTransactions().get(0);
        PortfolioTransaction sell = portfolio.getTransactions().get(1);
        ReportingPeriod period = new ReportingPeriod.FromXtoY(Dates.date("2013-12-06"), Dates.date("2014-12-06"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, period);
        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity().getName(), is("Basf SE"));
        assertThat(record.getSharesHeld(), is(0L));

        // no changes in holdings, ttwror must (without taxes and tax refunds):
        double startValue = delivery.getAmount() - delivery.getTaxes();
        double endValue = sell.getAmount() + sell.getTaxes();
        double ttwror = (endValue / startValue) - 1;
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(ttwror, 0.0001));

        // accrued taxes must be 0 (paid 10 on delivery + 5 tax refund + 10
        // taxes on sell):
        assertThat(record.getTaxes(), is(15L * Values.Amount.factor()));

        // accrued fees must be 20 (paid 10 on delivery + 10 on sell)
        assertThat(record.getFees(), is(20L * Values.Amount.factor()));

        // make sure that tax refund is included in transactions
        assertThat(record.getTransactions(), hasItem(isA(AccountTransaction.class)));

        // ttwror of classification must be identical to ttwror of security
        assertThatTTWROROfClassificationWithSecurityIsIdentical(client, period, ttwror);

        // check client performance + performance of portfolio + account
        PerformanceIndex clientIndex = assertPerformanceOfClient(client, period, ttwror);

        // the performance of the portfolio (w/o account) includes taxes
        assertThatTTWROROfPortfolioIsLessThan(client, clientIndex, ttwror);

        // the irr must not include taxes as well (compared with Excel):
        assertThat(record.getIrr(), closeTo(-0.032248297, 0.0001));
    }

    private void assertThatTTWROROfClassificationWithSecurityIsIdentical(Client client, ReportingPeriod period,
                    double ttwror)
    {
        // performance of the category of the taxonomy must be identical
        Classification classification = client.getTaxonomy("32ac1de9-b9a7-480a-b464-36abf7984e0a")
                        .getClassificationById("a41d1836-9f8e-493c-9304-7434d9bbaa05");

        List<Exception> warnings = new ArrayList<Exception>();
        PerformanceIndex classificationPerformance = PerformanceIndex.forClassification(client, classification, period,
                        warnings);
        assertThat(warnings, empty());
        assertThat(classificationPerformance.getFinalAccumulatedPercentage(), is(ttwror));
    }

    private PerformanceIndex assertPerformanceOfClient(Client client, ReportingPeriod period, double ttwror)
    {
        // the performance of the client must include taxes though -> worse
        List<Exception> warnings = new ArrayList<Exception>();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, period, warnings);
        assertThat(warnings, empty());
        assertThat(clientIndex.getFinalAccumulatedPercentage(), lessThan(ttwror));

        // the performance of portfolio + account must be identical to the
        // performance of the client
        PerformanceIndex portfolioPlusPerformance = PerformanceIndex.forPortfolioPlusAccount(client, client
                        .getPortfolios().get(0), period, warnings);
        assertThat(warnings, empty());
        assertThat(portfolioPlusPerformance.getFinalAccumulatedPercentage(),
                        is(clientIndex.getFinalAccumulatedPercentage()));
        return clientIndex;
    }

    private void assertThatTTWROROfPortfolioIsLessThan(Client client, PerformanceIndex clientIndex, double ttwror)
    {
        List<Exception> warnings = new ArrayList<Exception>();
        PerformanceIndex portfolioPerformance = PerformanceIndex.forPortfolio(client, client.getPortfolios().get(0),
                        clientIndex.getReportInterval(), warnings);
        assertThat(warnings, empty());
        assertThat(portfolioPerformance.getFinalAccumulatedPercentage(),
                        is(both(lessThan(clientIndex.getFinalAccumulatedPercentage())).and(lessThan(ttwror))));
    }
}
