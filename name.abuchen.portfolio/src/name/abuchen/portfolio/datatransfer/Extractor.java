package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

public interface Extractor
{
    interface Item
    {
        Object getSubject();

        String getTypeInformation();

        Date getDate();

        long getAmount();

        Security getSecurity();

        void insert(Client client, Portfolio portfolio, Account account);
    }

    class TransactionItem implements Item
    {
        private Transaction transaction;

        public TransactionItem(AccountTransaction transaction)
        {
            this.transaction = transaction;
        }

        public TransactionItem(PortfolioTransaction transaction)
        {
            this.transaction = transaction;
        }

        @Override
        public Object getSubject()
        {
            return transaction;
        }

        @Override
        public String getTypeInformation()
        {
            if (transaction instanceof AccountTransaction)
                return ((AccountTransaction) transaction).getType().toString();
            else if (transaction instanceof PortfolioTransaction)
                return ((PortfolioTransaction) transaction).getType().toString();
            else
                throw new UnsupportedOperationException();
        }

        @Override
        public Date getDate()
        {
            return transaction.getDate();
        }

        @Override
        public long getAmount()
        {
            return transaction.getAmount();
        }

        @Override
        public Security getSecurity()
        {
            return transaction.getSecurity();
        }

        @Override
        public void insert(Client client, Portfolio portfolio, Account account)
        {
            // ensure consistency (in case the user deleted the creation of the
            // security via the dialog)
            Security security = transaction.getSecurity();
            if (security != null && !client.getSecurities().contains(security))
                client.addSecurity(security);

            if (transaction instanceof AccountTransaction)
                account.addTransaction((AccountTransaction) transaction);
            else if (transaction instanceof PortfolioTransaction)
                portfolio.addTransaction((PortfolioTransaction) transaction);
            else
                throw new UnsupportedOperationException();
        }
    }

    class BuySellEntryItem implements Item
    {
        private BuySellEntry entry;

        public BuySellEntryItem(BuySellEntry entry)
        {
            this.entry = entry;
        }

        @Override
        public Object getSubject()
        {
            return entry;
        }

        @Override
        public String getTypeInformation()
        {
            return entry.getAccountTransaction().getType().toString();
        }

        @Override
        public Date getDate()
        {
            return entry.getAccountTransaction().getDate();
        }

        @Override
        public long getAmount()
        {
            return entry.getAccountTransaction().getAmount();
        }

        @Override
        public Security getSecurity()
        {
            return entry.getAccountTransaction().getSecurity();
        }

        @Override
        public void insert(Client client, Portfolio portfolio, Account account)
        {
            entry.setPortfolio(portfolio);
            entry.setAccount(account);
            entry.insert();
        }

    }

    class SecurityItem implements Item
    {
        private Security security;

        public SecurityItem(Security security)
        {
            this.security = security;
        }

        @Override
        public Object getSubject()
        {
            return security;
        }

        @Override
        public String getTypeInformation()
        {
            return Messages.LabelSecurity;
        }

        @Override
        public Date getDate()
        {
            return null;
        }

        @Override
        public long getAmount()
        {
            return 0;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public void insert(Client client, Portfolio portfolio, Account account)
        {
            // might have been added via a transaction
            if (!client.getSecurities().contains(security))
                client.addSecurity(security);
        }

    }

    /**
     * Returns a readable label for the type of documents
     */
    String getLabel();

    /**
     * Returns the filter extension for the file dialog, e.g. "*.pdf"
     */
    String getFilterExtension();

    /**
     * Returns a list of extracted items.
     */
    List<Item> extract(List<File> files);

}
