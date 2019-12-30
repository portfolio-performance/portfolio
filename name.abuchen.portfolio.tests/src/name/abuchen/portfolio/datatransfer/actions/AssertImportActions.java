package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;

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

    public void check(List<Extractor.Item> items, ImportAction.Context context)
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
}
