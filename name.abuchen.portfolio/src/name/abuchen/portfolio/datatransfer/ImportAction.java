package name.abuchen.portfolio.datatransfer;

import name.abuchen.portfolio.datatransfer.Extractor.NonImportableItem;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

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

    default Status process(Security security)
    {
        return Status.OK_STATUS;
    }

    default Status process(Security security, SecurityPrice price)
    {
        return Status.OK_STATUS;
    }

    default Status process(AccountTransaction transaction, Account account)
    {
        return Status.OK_STATUS;
    }

    default Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return Status.OK_STATUS;
    }

    default Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        return Status.OK_STATUS;
    }

    default Status process(AccountTransferEntry entry, Account source, Account target)
    {
        return Status.OK_STATUS;
    }

    default Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        return Status.OK_STATUS;
    }

    default Status process(NonImportableItem item)
    {
        return Status.OK_STATUS;
    }
}
