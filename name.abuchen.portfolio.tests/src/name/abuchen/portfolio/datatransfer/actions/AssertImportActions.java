package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                    new CheckTransactionDateAction(), new CheckValidTypesAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction() };

    public void check(List<Extractor.Item> items, String... currencyCode)
    {
        var contexts = Arrays.asList(currencyCode).stream()
                        .collect(Collectors.toMap((c) -> c, (c) -> new TestContext(c)));

        for (Extractor.Item item : items)
        {
            // do not apply further checks if the item is a (permanent) failure
            // as the transactions most likely has further errors
            if (item.isFailure())
                continue;

            // items that have no amount (e.g. a security) are checked against a
            // pseudo currency for the account to make sure that no attempt is
            // made to import into an account
            ImportAction.Context context;
            if (item.getAmount() == null)
            {
                context = new TestContext("XYZ"); //$NON-NLS-1$
            }
            else
            {
                context = contexts.get(item.getAmount().getCurrencyCode());
                assertThat(MessageFormat.format("No account available for currency ''{0}''", //$NON-NLS-1$
                                item.getAmount().getCurrencyCode()), context, is(not(nullValue())));
            }

            for (ImportAction action : actions)
            {
                ImportAction.Status status = item.apply(action, context);
                assertThat(status.getMessage() + "\n" + item, //$NON-NLS-1$
                                status.getCode(), is(ImportAction.Status.Code.OK));
            }
        }
    }
}
