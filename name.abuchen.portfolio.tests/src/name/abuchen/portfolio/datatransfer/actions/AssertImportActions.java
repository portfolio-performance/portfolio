package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;

public class AssertImportActions
{
    private static class TestContext implements ImportAction.Context
    {
        private Map<String, Account> currency2account = new HashMap<>();
        private Map<String, Account> currency2secondaryAccount = new HashMap<>();

        private Portfolio portfolio;
        private Portfolio secondaryPortfolio;

        public TestContext(String... currency)
        {
            for (String c : currency)
            {
                Account account = new Account();
                account.setCurrencyCode(c);
                this.currency2account.put(c, account);

                Account secondaryAccount = new Account();
                secondaryAccount.setCurrencyCode(c);
                this.currency2secondaryAccount.put(c, secondaryAccount);
            }
            this.portfolio = new Portfolio();
            this.secondaryPortfolio = new Portfolio();
        }

        @Override
        public Account getAccount(String currency)
        {
            return currency2account.get(currency);
        }

        @Override
        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        @Override
        public Account getSecondaryAccount(String currency)
        {
            return currency2secondaryAccount.get(currency);
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return secondaryPortfolio;
        }
    }

    private static final ImportAction[] actions = new ImportAction[] { //
                    new CheckTransactionDateAction(), new CheckValidTypesAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction() };

    public void check(List<Extractor.Item> items, String... currencyCode)
    {
        var context = new TestContext(currencyCode);

        for (Extractor.Item item : items)
        {
            // do not apply further checks if the item is a (permanent) failure
            // as the transactions most likely has further errors
            if (item.isFailure())
                continue;

            // do not further check items which are meant to be ignored
            if (item.isSkipped())
                continue;

            for (ImportAction action : actions)
            {
                ImportAction.Status status = item.apply(action, context);
                assertThat(status.getMessage() + "\n" + item, //$NON-NLS-1$
                                status.getCode(), is(ImportAction.Status.Code.OK));
            }
        }
    }
}
