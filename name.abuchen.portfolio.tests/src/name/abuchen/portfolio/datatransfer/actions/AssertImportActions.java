package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;


public class AssertImportActions
{
    public static class Currencies
    {
        private String primary;
        private String secondary;
        
        public Currencies(String primary, String secondary)
        {
            this.primary = primary;
            this.secondary = secondary;
        }
        
        public String getPrimary()
        {
            return primary;
        }

        public String getSecondary()
        {
            return secondary;
        }
    }

    public static class CheckResult
    {
        private Extractor.Item item;
        private ImportAction.Status status;

        public CheckResult(Extractor.Item item, ImportAction.Status status)
        {
            this.item = item;
            this.status = status;
        }
        
        public Extractor.Item getItem()
        {
            return item;
        }

        public ImportAction.Status getStatus()
        {
            return status;
        }

        @Override
        public String toString()
        {
            return item.toString() + " " + status.getCode() + ": " + status.getMessage();
        }
    }
    
    private static class TestContext implements ImportAction.Context
    {
        private class AccountPair
        {
            private Account primary;
            private Account secondary;

            public AccountPair(Currencies currencies)
            {
                this.primary = new Account();
                this.secondary = new Account();
                this.primary.setCurrencyCode(currencies.getPrimary());
                this.secondary.setCurrencyCode(currencies.getSecondary());
            }

            public Account getPrimary()
            {
                return primary;
            }

            public Account getSecondary()
            {
                return secondary;
            }
        }

        private Map<String, AccountPair> accounts;
        private Portfolio portfolio;
        private Portfolio secondaryPortfolio;

        public TestContext(String currency)
        {
            this(new Currencies(currency, currency));
        }

        public TestContext(Currencies... currencies)
        {
            this.portfolio = new Portfolio();
            this.secondaryPortfolio = new Portfolio();
            this.accounts = new HashMap<>();
            for (Currencies c : currencies)
            {
                this.accounts.put(c.getPrimary(), new AccountPair(c));
            }
        }

        @Override
        public Account getAccount(String currencyCode)
        {
            AccountPair pair = accounts.get(currencyCode);
            return pair != null ? pair.getPrimary() : accounts.values().stream().findFirst().map(AccountPair::getPrimary).orElse(null);
        }

        @Override
        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            AccountPair pair = accounts.get(currencyCode);
            return pair != null ? pair.getSecondary() : accounts.values().stream().findFirst().map(AccountPair::getSecondary).orElse(null);
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

    public List<CheckResult> check(List<Extractor.Item> items, Currencies... currencies)
    {
        var context = new TestContext(currencies);
        
        return items.stream()
                        .flatMap(item -> Stream.of(actions).map(action -> new CheckResult(item, item.apply(action, context))))
                        .filter(result -> result.getStatus().getCode() != ImportAction.Status.Code.OK)
                        .collect(Collectors.toList());
    }
}
