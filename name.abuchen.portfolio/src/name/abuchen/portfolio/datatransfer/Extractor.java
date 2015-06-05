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
    public abstract static class Item
    {
        private boolean isImported = true;
        private boolean isDuplicate = false;

        public boolean isImported()
        {
            return isImported;
        }

        public void setImported(boolean isImported)
        {
            this.isImported = isImported;
        }

        public boolean isDuplicate()
        {
            return isDuplicate;
        }

        public void setDuplicate(boolean isDuplicate)
        {
            this.isDuplicate = isDuplicate;
        }

        public abstract Annotated getSubject();

        public abstract Security getSecurity();

        public abstract String getTypeInformation();

        public abstract Date getDate();

        public long getAmount()
        {
            return 0;
        }

        public long getShares()
        {
            return 0;
        }

        public abstract void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount);

        public <T extends Transaction> void markDuplicates(Class<T> type, List<T> transactions)
        {}

        protected <T extends Transaction> void check(Transaction transaction, List<T> transactions)
        {
            for (T t : transactions)
            {
                if (transaction.isPotentialDuplicate(t))
                {
                    this.setDuplicate(true);
                    break;
                }
            }
        }
    }

    static class TransactionItem extends Item
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
        public Annotated getSubject()
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

        @Override
        public <T extends Transaction> void markDuplicates(Class<T> type, List<T> transactions)
        {
            if (type != transaction.getClass())
                return;

            check(transaction, transactions);
        }
    }

    static class BuySellEntryItem extends Item
    {
        private final BuySellEntry entry;

        public BuySellEntryItem(BuySellEntry entry)
        {
            this.entry = entry;
        }

        @Override
        public Annotated getSubject()
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
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setPortfolio(primaryPortfolio);
            entry.setAccount(primaryAccount);
            entry.insert();
        }

        @Override
        public <T extends Transaction> void markDuplicates(Class<T> type, List<T> transactions)
        {
            Transaction transaction = type == AccountTransaction.class ? entry.getAccountTransaction() : entry
                            .getPortfolioTransaction();

            check(transaction, transactions);
        }
    }

    static class AccountTransferItem extends Item
    {
        private final AccountTransferEntry entry;

        public AccountTransferItem(AccountTransferEntry entry)
        {
            this.entry = entry;
        }

        @Override
        public Annotated getSubject()
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
        public Security getSecurity()
        {
            return null;
        }

        @Override
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setSourceAccount(primaryAccount);
            entry.setTargetAccount(secondaryAccount);
            entry.insert();
        }

        public <T extends Transaction> void markDuplicates(Class<T> type, List<T> transactions)
        {
            if (type != AccountTransaction.class)
                return;

            check(entry.getSourceTransaction(), transactions);
        }
    }

    static class PortfolioTransferItem extends Item
    {
        private final PortfolioTransferEntry entry;

        public PortfolioTransferItem(PortfolioTransferEntry entry)
        {
            this.entry = entry;
        }

        @Override
        public Annotated getSubject()
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
        public void insert(Client client, Portfolio primaryPortfolio, Account primaryAccount,
                        Portfolio secondaryPortfolio, Account secondaryAccount)
        {
            entry.setSourcePortfolio(primaryPortfolio);
            entry.setTargetPortfolio(secondaryPortfolio);
            entry.insert();
        }

        public <T extends Transaction> void markDuplicates(Class<T> type, List<T> transactions)
        {
            if (type != PortfolioTransaction.class)
                return;

            check(entry.getSourceTransaction(), transactions);
        }
    }

    static class SecurityItem extends Item
    {
        private Security security;

        public SecurityItem(Security security)
        {
            this.security = security;
        }

        @Override
        public Annotated getSubject()
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
        public Security getSecurity()
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
