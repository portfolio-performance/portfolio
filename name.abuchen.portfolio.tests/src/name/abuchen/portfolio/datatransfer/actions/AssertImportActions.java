package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class AssertImportActions
{
    private static class TestContext implements ImportAction.Context
    {
        private Account account;
        private Portfolio portfolio;
        private Account secondaryAccount;
        private Portfolio secondaryPortfolio;

        public TestContext(String currency)
        {
            this.account = new Account();
            this.account.setCurrencyCode(currency);
            this.portfolio = new Portfolio();
            this.secondaryAccount = new Account();
            this.secondaryAccount.setCurrencyCode(currency);
            this.secondaryPortfolio = new Portfolio();
        }

        @Override
        public Account getAccount()
        {
            return account;
        }

        @Override
        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        @Override
        public Account getSecondaryAccount()
        {
            return secondaryAccount;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return secondaryPortfolio;
        }
    }

    private static final ImportAction[] actions = new ImportAction[] { //
                    new CheckValidTypesAction(), new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction() };

    private void check(List<Extractor.Item> items, ImportAction.Context context)
    {
        for (Extractor.Item item : items)
        {
            for (ImportAction action : actions)
            {
                ImportAction.Status status = item.apply(action, context);
                assertThat(status.getMessage(), status.getCode(), is(ImportAction.Status.Code.OK));
            }
        }
    }

    public void check(List<Extractor.Item> items, String currencyCode)
    {
        check(items, new TestContext(currencyCode));
    }

    public void check(List<Extractor.Item> items)
    {
        for (Extractor.Item item : items)
        {
            Money money = item.getAmount();
            String currencyCode = money != null ? money.getCurrencyCode() : CurrencyUnit.EUR;
            check(Arrays.asList(item), new TestContext(currencyCode));
        }
    }
}
