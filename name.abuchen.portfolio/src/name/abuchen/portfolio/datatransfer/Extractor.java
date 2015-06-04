package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
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

        long getShares();

        Security getSecurity();

        Annotated getAnnotated();

        void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount, Portfolio secondaryPortfolio,
                        Account secondaryAccount);
    }

    class TransactionItem implements Item
    {
        private Transaction transaction;

        public TransactionItem(AccountTransaction transaction)
        {
            if (EnumSet.of(AccountTransaction.Type.BUY, //
                            AccountTransaction.Type.SELL, //
                            AccountTransaction.Type.TRANSFER_IN, //
                            AccountTransaction.Type.TRANSFER_OUT) //
                            .contains(transaction.getType()))
                throw new UnsupportedOperationException();
            this.transaction = transaction;
        }

        public TransactionItem(PortfolioTransaction transaction)
        {
            if (EnumSet.of(PortfolioTransaction.Type.BUY, //
                            PortfolioTransaction.Type.SELL, //
                            PortfolioTransaction.Type.TRANSFER_IN, //
                            PortfolioTransaction.Type.TRANSFER_OUT) //
                            .contains(transaction.getType()))
                throw new UnsupportedOperationException();
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
        public long getShares()
        {
            return transaction.getShares();
        }

        @Override
        public Security getSecurity()
        {
            return transaction.getSecurity();
        }

        @Override
        public Annotated getAnnotated()
        {
            return transaction;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            // ensure consistency (in case the user deleted the creation of the
            // security via the dialog)
            Security security = transaction.getSecurity();
            if (security != null && !client.getSecurities().contains(security))
                client.addSecurity(security);

            if (transaction instanceof AccountTransaction)
                primaryAccount.addTransaction((AccountTransaction) transaction);
            else if (transaction instanceof PortfolioTransaction)
                primaryPortfolio.addTransaction((PortfolioTransaction) transaction);
            else
                throw new UnsupportedOperationException();
        }
    }

    class BuySellEntryItem implements Item
    {
        private final BuySellEntry entry;

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
        public long getShares()
        {
            return entry.getAccountTransaction().getShares();
        }

        @Override
        public Security getSecurity()
        {
            return entry.getAccountTransaction().getSecurity();
        }

        @Override
        public Annotated getAnnotated()
        {
            return entry;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setPortfolio(primaryPortfolio);
            entry.setAccount(primaryAccount);
            entry.insert();
        }

    }

    class AccountTransferItem implements Item
    {
        private final AccountTransferEntry entry;

        public AccountTransferItem(AccountTransferEntry entry)
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
            return Messages.LabelTransferAccount;
        }

        @Override
        public Date getDate()
        {
            return entry.getSourceTransaction().getDate();
        }

        @Override
        public long getAmount()
        {
            return entry.getSourceTransaction().getAmount();
        }

        @Override
        public long getShares()
        {
            return 0;
        }

        @Override
        public Security getSecurity()
        {
            return null;
        }

        @Override
        public Annotated getAnnotated()
        {
            return entry;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setSourceAccount(primaryAccount);
            entry.setTargetAccount(secondaryAccount);
            entry.insert();
        }
    }

    class PortfolioTransferItem implements Item
    {
        private final PortfolioTransferEntry entry;

        public PortfolioTransferItem(PortfolioTransferEntry entry)
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
            return Messages.LabelTransferPortfolio;
        }

        @Override
        public Date getDate()
        {
            return entry.getSourceTransaction().getDate();
        }

        @Override
        public long getAmount()
        {
            return entry.getSourceTransaction().getAmount();
        }

        @Override
        public long getShares()
        {
            return entry.getSourceTransaction().getShares();
        }

        @Override
        public Security getSecurity()
        {
            return entry.getSourceTransaction().getSecurity();
        }

        @Override
        public Annotated getAnnotated()
        {
            return entry;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setSourcePortfolio(primaryPortfolio);
            entry.setTargetPortfolio(secondaryPortfolio);
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
        public long getShares()
        {
            return 0;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public Annotated getAnnotated()
        {
            return security;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
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
    List<Item> extract(List<File> files, List<Exception> errors);

}
