package name.abuchen.portfolio.datatransfer;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;

public interface ImportAction
{
    public interface Context
    {
        Account getAccount();

        Portfolio getPortfolio();

        Account getSecondaryAccount();

        Portfolio getSecondaryPortfolio();
    }

    static class Status
    {
        public enum Code
        {
            OK, WARNING, ERROR;

            public boolean isHigherSeverityAs(Code other)
            {
                return ordinal() > other.ordinal();
            }
        }

        public static final Status OK_STATUS = new Status(Code.OK, null);

        private Code code;
        private String message;

        public Status(Code code, String message)
        {
            this.code = code;
            this.message = message;
        }

        public Code getCode()
        {
            return code;
        }

        public String getMessage()
        {
            return message;
        }
    }

    Status process(Security security);

    Status process(AccountTransaction transaction, Account account);

    Status process(PortfolioTransaction transaction, Portfolio portfolio);

    Status process(BuySellEntry entry, Account account, Portfolio portfolio);

    Status process(AccountTransferEntry entry, Account source, Account target);

    Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target);
}
