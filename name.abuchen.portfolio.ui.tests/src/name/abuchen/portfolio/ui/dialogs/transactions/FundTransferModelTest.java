package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class FundTransferModelTest
{
    @Test
    public void testApplyCreatesFundTransferEntryWithPreviewLots()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        FundTransferModel model = new FundTransferModel(client);
        model.setSourcePortfolio(sourcePortfolio);
        model.setTargetPortfolio(targetPortfolio);
        model.setSourceSecurity(sourceFund);
        model.setTargetSecurity(targetFund);
        model.setDate(LocalDate.parse("2020-06-01"));
        model.setTime(LocalTime.parse("10:15"));
        model.setSourceShares(Values.Share.factorize(7));
        model.setTargetShares(Values.Share.factorize(11));
        model.setSourceAmount(Values.Amount.factorize(1050));
        model.setTargetAmount(Values.Amount.factorize(1050));
        model.setNote("tax neutral transfer");

        assertThat(model.getCalculationStatus().getSeverity(), is(IStatus.OK));
        assertThat(model.getCarriedLots().size(), is(1));

        model.applyChanges();

        assertThat(sourcePortfolio.getTransactions().size(), is(2));
        assertThat(targetPortfolio.getTransactions().size(), is(1));

        PortfolioTransaction sourceTransaction = sourcePortfolio.getTransactions().get(1);
        FundTransferEntry entry = (FundTransferEntry) sourceTransaction.getCrossEntry();

        assertThat(entry.getSourceTransaction().getDateTime().toLocalDate(), is(LocalDate.parse("2020-06-01")));
        assertThat(entry.getSourceTransaction().getDateTime().toLocalTime(), is(LocalTime.parse("10:15")));
        assertThat(entry.getSourceTransaction().getSecurity(), is(sourceFund));
        assertThat(entry.getTargetTransaction().getSecurity(), is(targetFund));
        assertThat(entry.getSourceTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getTargetTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getSourceTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050))));
        assertThat(entry.getTargetTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1050))));
        assertThat(entry.getCarriedLots().size(), is(1));
        assertThat(entry.getCarriedLots().get(0).getAcquisitionValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700))));
        assertThat(entry.getNote(), is("tax neutral transfer"));
    }

    @Test
    public void testDefaultsTargetPortfolioAndAmountFromSourceFields()
    {
        Client client = new Client();
        Portfolio sourcePortfolio = new PortfolioBuilder().addTo(client);
        Portfolio explicitTargetPortfolio = new PortfolioBuilder().addTo(client);

        FundTransferModel model = new FundTransferModel(client);

        model.setSourcePortfolio(sourcePortfolio);
        assertThat(model.getTargetPortfolio(), is(sourcePortfolio));

        model.setSourceAmount(Values.Amount.factorize(1234));
        assertThat(model.getTargetAmount(), is(Values.Amount.factorize(1234)));

        model.setTargetAmount(Values.Amount.factorize(999));
        model.setSourceAmount(Values.Amount.factorize(2000));
        assertThat(model.getTargetAmount(), is(Values.Amount.factorize(999)));

        model.setTargetPortfolio(explicitTargetPortfolio);
        model.setSourcePortfolio(sourcePortfolio);
        assertThat(model.getTargetPortfolio(), is(explicitTargetPortfolio));
    }

    @Test
    public void testValidationRejectsSameSecurityAndInsufficientHoldings()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(2),
                                        Values.Amount.factorize(200)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        FundTransferModel model = new FundTransferModel(client);
        model.setSourcePortfolio(sourcePortfolio);
        model.setTargetPortfolio(targetPortfolio);
        model.setSourceSecurity(sourceFund);
        model.setTargetSecurity(sourceFund);
        model.setDate(LocalDate.parse("2020-06-01"));
        model.setSourceShares(Values.Share.factorize(1));
        model.setTargetShares(Values.Share.factorize(1));
        model.setSourceAmount(Values.Amount.factorize(100));
        model.setTargetAmount(Values.Amount.factorize(100));

        assertThat(model.getCalculationStatus().getMessage(), is(Messages.MsgFundTransferSecuritiesMustDiffer));

        model.setTargetSecurity(targetFund);
        model.setSourceShares(Values.Share.factorize(3));

        assertThat(model.getCalculationStatus().getMessage(), is(Messages.MsgFundTransferNotEnoughShares));
    }

    @Test
    public void testEditingExistingTransferDoesNotConsumeItself()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDate.parse("2020-06-01").atStartOfDay());
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

        FundTransferModel model = new FundTransferModel(client);
        model.setSource(entry);
        model.setDate(LocalDate.parse("2020-07-01"));

        assertThat(model.getCalculationStatus().getSeverity(), is(IStatus.OK));
        assertThat(model.getCarriedLots().size(), is(1));
    }
}
