package name.abuchen.portfolio.json;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class JTransactionTest
{
    @Test
    public void testFundTransferExportsSourceOrientedShape()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        sourceFund.setName("Source Fund");
        Security targetFund = new SecurityBuilder().addTo(client);
        targetFund.setName("Target Fund");

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        sourcePortfolio.setName("Source Portfolio");
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);
        targetPortfolio.setName("Target Portfolio");

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(sourceFund);
        entry.setTargetSecurity(targetFund);
        entry.setSourceShares(Values.Share.factorize(7));
        entry.setTargetShares(Values.Share.factorize(11));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050)));
        entry.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                        Values.Share.factorize(7), Values.Share.factorize(11),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700)), sourceBuy.getUUID()));
        entry.insert();

        assertFundTransfer(JTransaction.from(new TransactionPair<>(sourcePortfolio, entry.getSourceTransaction())));
        assertFundTransfer(JTransaction.from(new TransactionPair<>(targetPortfolio, entry.getTargetTransaction())));
    }

    private void assertFundTransfer(JTransaction transaction)
    {
        assertThat(transaction.getType(), is(JTransaction.Type.FUND_TRANSFER));
        assertThat(transaction.getPortfolio(), is("Source Portfolio"));
        assertThat(transaction.getOtherPortfolio(), is("Target Portfolio"));
        assertThat(transaction.getSecurity().getName(), is("Source Fund"));
        assertThat(transaction.getTargetSecurity().getName(), is("Target Fund"));
        assertThat(transaction.getShares(), is(7d));
        assertThat(transaction.getTargetShares(), is(11d));
    }
}
