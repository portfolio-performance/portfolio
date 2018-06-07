package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Peer;
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
        public abstract Peer getPeer();

        public abstract Annotated getSubject();

        public abstract Security getSecurity();

        public abstract String getTypeInformation();

        public abstract LocalDate getDate();

        public abstract String getNote();

        public boolean getDefaultImported()
        {
            return true;
        }

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
        public Peer getPeer()
        {

            System.err.println(">>>> Extractor::TransactionItem::getPeer: " + transaction.toString()); // TODO: still needed for debug?
            if (transaction instanceof AccountTransaction)
                return ((AccountTransaction) transaction).getPeer();
            return null;
        }

        @Override
        public Annotated getSubject()
        {
            return transaction;
        }

        public Transaction getTransaction()
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
        public String getNote()
        {
            return transaction.getNote();
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
        public Peer getPeer()
        {
            return null;
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
        public String getNote()
        {
            return entry.getAccountTransaction().getNote();
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
        public Peer getPeer()
        {
            System.err.println(">>>> Extractor::AccountTransferItem::getPeer entry.source: " + entry.getSourceTransaction().toString()); // TODO: still needed for debug?
            System.err.println(">>>> Extractor::AccountTransferItem::getPeer entry.target: " + entry.getTargetTransaction().toString()); // TODO: still needed for debug?
            System.err.println(">>>> Extractor::AccountTransferItem::getPeer outbound " + isOutbound + " entry: " + entry.toString()); // TODO: still needed for debug?
            if (isOutbound)
                return entry.getSourceTransaction().getPeer();
            else
                return entry.getTargetTransaction().getPeer();
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
        public String getNote()
        {
            return entry.getSourceTransaction().getNote();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            // TODO: still needed for debug?System.err.println(">>>> Extractor::apply entry.source: " + entry.getSourceTransaction().toString());
            // TODO: still needed for debug?System.err.println(">>>> Extractor::apply entry.target: " + entry.getTargetTransaction().toString());
            Account source;
            Account target;
            if (isOutbound)
            {
                source = context.getAccount();
                target = context.getSecondaryAccount();
            }
            else
            {
                source = context.getSecondaryAccount();
                target = context.getAccount();
            }
            // TODO: still needed for debug?System.err.println(">>>> Extractor::apply intermediate source: " + source.toString());
            // TODO: still needed for debug?System.err.println(">>>> Extractor::apply intermediate target: " + target.toString());
            if (entry.getSourceTransaction().getPeer() != null && entry.getSourceTransaction().getPeer().isAccount())
                target = entry.getSourceTransaction().getPeer().getAccount();
            if (entry.getTargetTransaction().getPeer() != null && entry.getTargetTransaction().getPeer().isAccount())
                source = entry.getTargetTransaction().getPeer().getAccount();
            System.err.println(">>>> Extractor::apply source: " + source.toString()); // TODO: still needed for debug?
            System.err.println(">>>> Extractor::apply target: " + target.toString()); // TODO: still needed for debug?
            return action.process(entry, source, target);
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
        public Peer getPeer()
        {
            return null;
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
        public String getNote()
        {
            return entry.getSourceTransaction().getNote();
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
        public Peer getPeer()
        {
            return null;
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
        public String getNote()
        {
            return security.getNote();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(security);
        }
    }

    static class PeerItem extends Item
    {
        private Peer peer;

        public PeerItem(Peer peer)
        {
            this.peer = peer;
        }

        @Override
        public Peer getPeer()
        {
            // TODO: still needed for debug? System.err.println(">>>> Extractor::PeerItem peer::getPeer " + peer.toString());
            return peer;
        }

        @Override
        public Annotated getSubject()
        {
            // TODO: still needed for debug? System.err.println(">>>> Extractor::PeerItem peer::getSubject " + peer.toString());
            return peer;
        }

        @Override
        public String getTypeInformation()
        {
            return Messages.LabelPeer;
        }

        @Override
        public LocalDate getDate()
        {
            return null;
        }

        @Override
        public Security getSecurity()
        {
            return null;
        }

        @Override
        public String getNote()
        {
            return peer.getNote();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(peer);
        }

        @Override
        public boolean getDefaultImported()
        {
            return false;
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
