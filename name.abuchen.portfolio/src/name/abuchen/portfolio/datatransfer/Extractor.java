package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

public interface Extractor
{
    public static class InputFile
    {
        private File file;

        public InputFile(File file)
        {
            this.file = file;
        }

        public File getFile()
        {
            return file;
        }

        public String getName()
        {
            return file.getName();
        }
    }
    
    public abstract static class Item
    {
        public abstract Annotated getSubject();

        public abstract Security getSecurity();

        public abstract String getTypeInformation();

        public abstract LocalDate getDate();

        public Money getAmount()
        {
            return null;
        }

        public long getShares()
        {
            return 0;
        }

        public abstract Status apply(ImportAction action, Context context);
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
        public LocalDate getDate()
        {
            return transaction.getDate();
        }

        @Override
        public Money getAmount()
        {
            return transaction.getMonetaryAmount();
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
        public Status apply(ImportAction action, Context context)
        {
            if (transaction instanceof AccountTransaction)
                return action.process((AccountTransaction) transaction, context.getAccount());
            else if (transaction instanceof PortfolioTransaction)
                return action.process((PortfolioTransaction) transaction, context.getPortfolio());
            else
                throw new UnsupportedOperationException();
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
        public LocalDate getDate()
        {
            return entry.getAccountTransaction().getDate();
        }

        @Override
        public Money getAmount()
        {
            return entry.getAccountTransaction().getMonetaryAmount();
        }

        @Override
        public long getShares()
        {
            return entry.getPortfolioTransaction().getShares();
        }

        @Override
        public Security getSecurity()
        {
            return entry.getAccountTransaction().getSecurity();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(entry, context.getAccount(), context.getPortfolio());
        }
    }

    static class AccountTransferItem extends Item
    {
        private final AccountTransferEntry entry;
        private final boolean isOutbound;

        public AccountTransferItem(AccountTransferEntry entry, boolean isOutbound)
        {
            this.entry = entry;
            this.isOutbound = isOutbound;
        }

        @Override
        public Annotated getSubject()
        {
            return entry;
        }

        @Override
        public String getTypeInformation()
        {
            return isOutbound ? PortfolioTransaction.Type.TRANSFER_OUT.toString()
                            : PortfolioTransaction.Type.TRANSFER_IN.toString();
        }

        @Override
        public LocalDate getDate()
        {
            return entry.getSourceTransaction().getDate();
        }

        @Override
        public Money getAmount()
        {
            return entry.getSourceTransaction().getMonetaryAmount();
        }

        @Override
        public Security getSecurity()
        {
            return null;
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            if (isOutbound)
                return action.process(entry, context.getAccount(), context.getSecondaryAccount());
            else
                return action.process(entry, context.getSecondaryAccount(), context.getAccount());
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
        public LocalDate getDate()
        {
            return entry.getSourceTransaction().getDate();
        }

        @Override
        public Money getAmount()
        {
            return entry.getSourceTransaction().getMonetaryAmount();
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
        public Status apply(ImportAction action, Context context)
        {
            return action.process(entry, context.getPortfolio(), context.getSecondaryPortfolio());
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
        public LocalDate getDate()
        {
            return null;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(security);
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
    List<Item> extract(List<InputFile> files, List<Exception> errors);

}
