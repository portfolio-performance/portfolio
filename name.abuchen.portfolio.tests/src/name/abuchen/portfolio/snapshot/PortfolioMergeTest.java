package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class PortfolioMergeTest
{
    private final LocalDate referenceDate = LocalDate.of(2010, Month.JANUARY, 31);

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
        securityA.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), Values.Quote.factorize(10)));
        client.addSecurity(securityA);

        securityB = new Security();
        securityB.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), Values.Quote.factorize(11)));
        client.addSecurity(securityB);

        securityX = new Security();
        securityX.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), Values.Quote.factorize(12)));
        client.addSecurity(securityX);

        Portfolio portfolioA = new Portfolio();
        portfolioA.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0),
                        CurrencyUnit.EUR, 100_00, securityA, Values.Share.factorize(10), PortfolioTransaction.Type.BUY,
                        0, 0));
        portfolioA.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0),
                        CurrencyUnit.EUR, 121_00, securityX, Values.Share.factorize(10), PortfolioTransaction.Type.BUY,
                        100, 0));
        client.addPortfolio(portfolioA);

        Portfolio portfolioB = new Portfolio();
        portfolioB.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0),
                        CurrencyUnit.EUR, 110_00, securityB, Values.Share.factorize(10), PortfolioTransaction.Type.BUY,
                        0, 0));
        portfolioB.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0),
                        CurrencyUnit.EUR, 100_00, securityX, Values.Share.factorize(10), PortfolioTransaction.Type.BUY,
                        0, 0));
        client.addPortfolio(portfolioB);
    }

    @Test
    public void testMergingPortfolioSnapshots()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, new TestCurrencyConverter(), referenceDate);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionA = jointPortfolio.getPositionsBySecurity().get(securityA);
        assertThat(positionA.getShares(), is(Values.Share.factorize(10)));
        assertThat(positionA.calculateValue(), is(Money.of(CurrencyUnit.EUR, 100_00)));

        SecurityPosition positionB = jointPortfolio.getPositionsBySecurity().get(securityB);
        assertThat(positionB.getShares(), is(Values.Share.factorize(10)));
        assertThat(positionB.calculateValue(), is(Money.of(CurrencyUnit.EUR, 110_00)));

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);
        assertThat(positionX.getShares(), is(Values.Share.factorize(10 * 2)));
        assertThat(positionX.calculateValue(), is(Money.of(CurrencyUnit.EUR, 240_00)));
    }

    @Test
    public void testThatTransactionsAreMergedOnSecurityPosition()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, new TestCurrencyConverter(), referenceDate);
        assertNotNull(snapshot);

        PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();

        SecurityPosition positionX = jointPortfolio.getPositionsBySecurity().get(securityX);

        assertThat(positionX.getShares(), is(Values.Share.factorize(20)));
        assertThat(positionX.calculateValue(), is(Money.of(CurrencyUnit.EUR, 240_00)));
    }
}
