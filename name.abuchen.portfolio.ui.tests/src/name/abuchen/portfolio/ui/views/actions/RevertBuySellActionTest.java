package name.abuchen.portfolio.ui.views.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RevertBuySellActionTest
{
    private final Client client = new Client();
    private final Account account = new Account("Account");
    private final Portfolio portfolio = new Portfolio("Portfolio");

    public RevertBuySellActionTest()
    {
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
    }

    private static Money eur(double amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static Money usd(double amount)
    {
        return Money.of(CurrencyUnit.USD, Values.Amount.factorize(amount));
    }

    private BuySellEntry buy(Security security, Money amount)
    {
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2024-03-01T00:00"));
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(1000));
        entry.setMonetaryAmount(amount);
        return entry;
    }

    private void revert(BuySellEntry entry)
    {
        new RevertBuySellAction(client, new TransactionPair<>(portfolio, entry.getPortfolioTransaction())).run();
    }

    @Test
    public void testRevertKeepsAccruedInterest()
    {
        Security bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        // 1,000.00 + accrued interest 10.00 + fees 5.00
        BuySellEntry entry = buy(bond, eur(1015));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, eur(10)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, eur(5)));
        entry.insert();

        PortfolioTransaction tx = entry.getPortfolioTransaction();

        // buy -> sell: 1,000.00 + accrued interest 10.00 - fees 5.00
        revert(entry);

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(tx.getMonetaryAmount(), is(eur(1005)));
        assertThat(entry.getAccountTransaction().getMonetaryAmount(), is(eur(1005)));
        assertThat(tx.getGrossValue(), is(eur(1000)));
        assertThat(tx.getAccruedInterest(), is(eur(10)));

        // sell -> buy: back to the original transaction
        revert(entry);

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(tx.getMonetaryAmount(), is(eur(1015)));
        assertThat(tx.getGrossValue(), is(eur(1000)));
        assertThat(tx.getAccruedInterest(), is(eur(10)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(eur(5)));
    }

    @Test
    public void testRevertKeepsAccruedInterestInForex()
    {
        Security bond = new Security("Bond", CurrencyUnit.USD);
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        BigDecimal rate = BigDecimal.valueOf(0.9);

        // USD 1,100.00 = EUR 990.00, accrued interest USD 10.00 = EUR 9.00,
        // fees EUR 5.00
        BuySellEntry entry = buy(bond, eur(1004));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, eur(990), usd(1100), rate));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, eur(9), usd(10), rate));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, eur(5)));
        entry.insert();

        PortfolioTransaction tx = entry.getPortfolioTransaction();

        // buy -> sell: 990.00 + 9.00 - 5.00
        revert(entry);

        assertThat(tx.getMonetaryAmount(), is(eur(994)));
        assertThat(tx.getGrossValue(), is(eur(990)));
        assertThat(tx.getAccruedInterest(), is(eur(9)));
        assertThat(tx.getUnit(Unit.Type.ACCRUED_INTEREST).orElseThrow().getForex(), is(usd(10)));

        // sell -> buy
        revert(entry);

        assertThat(tx.getMonetaryAmount(), is(eur(1004)));
        assertThat(tx.getGrossValue(), is(eur(990)));
        assertThat(tx.getUnit(Unit.Type.ACCRUED_INTEREST).orElseThrow().getForex(), is(usd(10)));
    }
}
