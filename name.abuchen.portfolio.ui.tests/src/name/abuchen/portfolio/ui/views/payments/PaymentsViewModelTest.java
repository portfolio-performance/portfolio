package name.abuchen.portfolio.ui.views.payments;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Mode;

@SuppressWarnings("nls")
public class PaymentsViewModelTest
{
    @Test
    public void testShareDividendGrossAndNetValues()
    {
        var model = createModel();
        for (var mode : new Mode[] { Mode.DIVIDENDS, Mode.EARNINGS })
        {
            model.configure(2024, mode, true, false, CostMethod.FIFO);
            assertEquals(393, model.getSum().getSum());
            model.setUseGrossValue(false);
            assertEquals(243, model.getSum().getSum());
            assertEquals(243, model.getLines().get(0).getValue(0));
        }
    }

    @Test
    public void testAllModeDeductsShareDividendChargesOnce()
    {
        var model = createModel();
        model.configure(2024, Mode.ALL, true, false, CostMethod.FIFO);
        assertEquals(243, model.getSum().getSum());
        model.setUseGrossValue(false);
        assertEquals(243, model.getSum().getSum());
    }

    private PaymentsViewModel createModel()
    {
        var client = new Client();
        var portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        var security = new Security("Reward", "EUR");
        client.addSecurity(security);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2024, 1, 1, 0, 0), "EUR", 393,
                        security, Values.Share.factorize(0.0243), PortfolioTransaction.Type.DIVIDENDS, 50, 100));
        return new PaymentsViewModel(new TestCurrencyConverter(), client);
    }
}
