package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.Dates;

import org.junit.Before;
import org.junit.Test;

public class PortfolioMergeTest
{
    private final Date referenceDate = Dates.date(2010, Calendar.JANUARY, 31);

    private Client client;

    private Security securityA;
    private Security securityB;
    private Security securityX;

    @Before
    public void setUpClient()
    {
        // Portfolio A : Security A + Security X
        // Portfolio B : Security B + Security X

        client = new Client();

        securityA = new Security();
        securityA.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1000));
        client.addSecurity(securityA);

        securityB = new Security();
        securityB.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1100));
        client.addSecurity(securityB);

        securityX = new Security();
        securityX.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 1200));
        client.addSecurity(securityX);

        Portfolio portfolioA = new Portfolio();
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityA,
                        PortfolioTransaction.Type.BUY, 1000000, 10000, 0, 0));
        portfolioA.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 1000000, 12100, 100, 0));
        client.addPortfolio(portfolioA);

        Portfolio portfolioB = new Portfolio();
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityB,
                        PortfolioTransaction.Type.BUY, 1000000, 11000, 0, 0));
        portfolioB.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), securityX,
                        PortfolioTransaction.Type.BUY, 1000000, 10000, 0, 0));
        client.addPortfolio(portfolioB);
    }

    @Test
    public void testMergingPortfolioSnapshots()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionA = jointPortfolio.getPositionsBySecurity().get(securityA);
        assertThat(positionA.getShares(), is(1000000L));
        assertThat(positionA.calculateValue(), is(Money.of(CurrencyUnit.EUR, 100_00)));

        SecurityPosition positionB = jointPortfolio.getPositionsBySecurity().get(securityB);
        assertThat(positionB.getShares(), is(1000000L));
        assertThat(positionB.calculateValue(), is(Money.of(CurrencyUnit.EUR, 110_00)));

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);
        assertThat(positionX.getShares(), is(2000000L));
        assertThat(positionX.calculateValue(), is(Money.of(CurrencyUnit.EUR, 240_00)));
    }

    @Test
    public void testThatTransactionsAreMergedOnSecurityPosition()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);

        assertThat(positionX.getShares(), is(2000000L));
        assertThat(positionX.calculateValue(), is(Money.of(CurrencyUnit.EUR, 240_00)));
        // calculate purchase price w/o costs
        assertThat(positionX.getFIFOPurchasePrice(), is(Money.of(CurrencyUnit.EUR, 11_00)));
        // calculate purchase value w/ costs
        assertThat(positionX.getFIFOPurchaseValue(), is(Money.of(CurrencyUnit.EUR, 221_00)));
        assertThat(positionX.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, 19_00)));
    }

}
