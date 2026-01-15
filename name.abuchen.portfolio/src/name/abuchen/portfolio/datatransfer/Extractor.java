package name.abuchen.portfolio.datatransfer;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.io.File;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexStatementExtractor;
import name.abuchen.portfolio.datatransfer.pdf.AbstractPDFExtractor;
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
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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
        private Map<String, Object> data = new HashMap<>();

        private Account accountPrimary;

        private Account accountSecondary;

        private Portfolio portfolioPrimary;

        private Portfolio portfolioSecondary;

        private String failureMessage;

        private boolean investmentPlanItem = false;

        public abstract Annotated getSubject();

        public abstract Security getSecurity();

        public abstract void setSecurity(Security security);

        public abstract String getTypeInformation();

        public abstract LocalDateTime getDate();

        public Money getAmount()
        {
            return null;
        }

        public long getShares()
        {
            return 0; // NOSONAR
        }

        public String getFailureMessage()
        {
            return failureMessage;
        }

        public void setFailureMessage(String failureMessage)
        {
            this.failureMessage = failureMessage;
        }

        public boolean isFailure()
        {
            return failureMessage != null;
        }

        public boolean isSkipped()
        {
            return this instanceof SkippedItem;
        }

        public String getSource()
        {
            return null;
        }

        public abstract void setNote(String note);

        public abstract Status apply(ImportAction action, Context context);

        public Object getData(String key)
        {
            return this.data.get(key);
        }

        public void setData(String key, Object value)
        {
            this.data.put(key, value);
        }

        public Account getAccountPrimary()
        {
            return accountPrimary;
        }

        public void setAccountPrimary(Account account)
        {
            accountPrimary = account;
        }

        public Account getAccountSecondary()
        {
            return accountSecondary;
        }

        public void setAccountSecondary(Account account)
        {
            accountSecondary = account;
        }

        public Portfolio getPortfolioPrimary()
        {
            return portfolioPrimary;
        }

        public void setPortfolioPrimary(Portfolio portfolio)
        {
            portfolioPrimary = portfolio;
        }

        public Portfolio getPortfolioSecondary()
        {
            return portfolioSecondary;
        }

        public void setPortfolioSecondary(Portfolio portfolio)
        {
            portfolioSecondary = portfolio;
        }

        public boolean isInvestmentPlanItem()
        {
            return investmentPlanItem;
        }

        public void setInvestmentPlanItem(boolean flag)
        {
            investmentPlanItem = flag;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            // debug output
            return String.format("%s %s %s %s %s", getDate() != null ? Values.DateTime.format(getDate()) : "",
                            getTypeInformation(), getAmount() != null ? Values.Money.format(getAmount()) : "",
                            getSecurity() != null ? getSecurity().getName() : "",
                            getSource() != null ? getSource() : "");
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
            if (transaction instanceof AccountTransaction at)
                return at.getType().toString();
            else if (transaction instanceof PortfolioTransaction pt)
                return pt.getType().toString();
            else
                throw new UnsupportedOperationException();
        }

        @Override
        public LocalDateTime getDate()
        {
            return transaction.getDateTime();
        }

        @Override
        public Money getAmount()
        {
            var currencyCode = transaction.getCurrencyCode();
            var amount = transaction.getAmount();

            // if the transaction is not parsed completely, then the
            // currency code maybe null
            return currencyCode != null ? Money.of(currencyCode, amount) : null;
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
        public void setSecurity(Security security)
        {
            transaction.setSecurity(security);
        }

        @Override
        public void setNote(String note)
        {
            transaction.setNote(note);
        }

        @Override
        public String getSource()
        {
            return transaction.getSource();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            if (transaction instanceof AccountTransaction at)
            {
                Account account = getAccountPrimary();
                if (account == null)
                    account = context.getAccount(at.getCurrencyCode());
                if (account == null)
                    return new Status(Status.Code.ERROR,
                                    MessageFormat.format(Messages.MsgCheckTransactionCurrencyDoesNotMatchAccount,
                                                    at.getCurrencyCode(), "?")); //$NON-NLS-1$

                return action.process(at, account);
            }
            else if (transaction instanceof PortfolioTransaction pt)
            {
                Portfolio portfolio = getPortfolioPrimary();
                if (portfolio == null)
                    portfolio = context.getPortfolio();
                if (portfolio == null)
                    return new Status(Status.Code.ERROR, MessageFormat.format(Messages.CSVImportMissingField,
                                    Messages.CSVColumn_PortfolioName));

                return action.process(pt, portfolio);
            }
            else
            {
                throw new UnsupportedOperationException();
            }
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
        public LocalDateTime getDate()
        {
            return entry.getAccountTransaction().getDateTime();
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
        public void setSecurity(Security security)
        {
            entry.setSecurity(security);
        }

        @Override
        public void setNote(String note)
        {
            entry.setNote(note);
        }

        @Override
        public String getSource()
        {
            return entry.getAccountTransaction().getSource();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            Account account = getAccountPrimary();
            if (account == null)
                account = context.getAccount(entry.getAccountTransaction().getCurrencyCode());
            if (account == null)
                return new Status(Status.Code.ERROR,
                                MessageFormat.format(Messages.MsgCheckTransactionCurrencyDoesNotMatchAccount,
                                                entry.getAccountTransaction().getCurrencyCode(), "?")); //$NON-NLS-1$

            Portfolio portfolio = getPortfolioPrimary();
            if (portfolio == null)
                portfolio = context.getPortfolio();
            if (portfolio == null)
                return new Status(Status.Code.ERROR,
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_PortfolioName));

            Status status = action.process(entry, account, portfolio);

            // check if message was set in DetectDuplicatesAction
            if (Messages.InvestmentPlanItemImportToolTip.equals(status.getMessage()))
            {
                super.setInvestmentPlanItem(true);
            }
            return status;
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
        public LocalDateTime getDate()
        {
            return entry.getSourceTransaction().getDateTime();
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
        public void setSecurity(Security security)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNote(String note)
        {
            entry.setNote(note);
        }

        @Override
        public String getSource()
        {
            return entry.getSourceTransaction().getSource();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            Account account = getAccountPrimary();
            if (account == null)
                account = context.getAccount(entry.getSourceTransaction().getCurrencyCode());
            if (account == null)
                return new Status(Status.Code.ERROR,
                                MessageFormat.format(Messages.MsgCheckTransactionCurrencyDoesNotMatchAccount,
                                                entry.getSourceTransaction().getCurrencyCode(), "?")); //$NON-NLS-1$

            Account accountSecondary = getAccountSecondary();
            if (accountSecondary == null)
                accountSecondary = context.getSecondaryAccount(entry.getTargetTransaction().getCurrencyCode());
            if (accountSecondary == null)
                return new Status(Status.Code.ERROR,
                                MessageFormat.format(Messages.MsgCheckTransactionCurrencyDoesNotMatchAccount,
                                                entry.getTargetTransaction().getCurrencyCode(), "?")); //$NON-NLS-1$

            if (isOutbound)
                return action.process(entry, account, accountSecondary);
            else
                return action.process(entry, accountSecondary, account);
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
        public LocalDateTime getDate()
        {
            return entry.getSourceTransaction().getDateTime();
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
        public void setSecurity(Security security)
        {
            entry.setSecurity(security);
        }

        @Override
        public void setNote(String note)
        {
            entry.setNote(note);
        }

        @Override
        public String getSource()
        {
            return entry.getSourceTransaction().getSource();
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            Portfolio portfolio = getPortfolioPrimary();
            if (portfolio == null)
                portfolio = context.getPortfolio();
            if (portfolio == null)
                return new Status(Status.Code.ERROR,
                                MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_PortfolioName));

            Portfolio portfolioSecondary = getPortfolioSecondary();
            if (portfolioSecondary == null)
                portfolioSecondary = context.getSecondaryPortfolio();
            if (portfolioSecondary == null)
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.CSVImportMissingField,
                                Messages.CSVColumn_PortfolioName2nd));

            return action.process(entry, portfolio, portfolioSecondary);
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
            return getSecurity();
        }

        @Override
        public String getTypeInformation()
        {
            return Messages.LabelSecurity;
        }

        @Override
        public LocalDateTime getDate()
        {
            return null;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public void setSecurity(Security security)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNote(String note)
        {
            security.setNote(note);
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(security);
        }
    }

    static class SecurityPriceItem extends Item
    {
        private Security security;
        private SecurityPrice price;

        public SecurityPriceItem(Security security, SecurityPrice price)
        {
            this.security = security;
            this.price = price;
        }

        @Override
        public Annotated getSubject()
        {
            return getSecurity();
        }

        @Override
        public String getTypeInformation()
        {
            return Messages.LabelSecurityPrice;
        }

        @Override
        public LocalDateTime getDate()
        {
            return price.getDate().atStartOfDay();
        }

        @Override
        public Money getAmount()
        {
            return Money.of(security.getCurrencyCode(), Math.round(price.getValue() / Values.Quote.dividerToMoney()));
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public void setSecurity(Security security)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNote(String note)
        {
            // not supported; prices have no notes
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(security, price);
        }
    }

    /**
     * An item representing transactions that will be displayed to the user but
     * otherwise get skipped during further processing steps. A typical reason
     * for an item to get ignored is lack of information found in the document.
     */
    static class SkippedItem extends Item
    {
        private String skipReason;

        private LocalDateTime dateTime;
        private Money amount;
        private String typeInformation;
        private String source;
        private Security security;
        private SkippedSubject subject;

        private class SkippedSubject implements Annotated
        {
            private String note;

            @Override
            public String getNote()
            {
                return note;
            }

            @Override
            public void setNote(String note)
            {
                this.note = note;
            }
        }

        /**
         * Constructs a SkippedItem based on the information found in another
         * item and assigns a text describing the reason for the skip.
         *
         * @param item
         *            The original item as a data source
         * @param reason
         *            Human-readable explanation
         * @return A new item that will be displayed but otherwise skipped.
         */
        public SkippedItem(Item item, String reason)
        {
            skipReason = reason;
            subject = new SkippedSubject();

            // copy basic data from original item
            dateTime = item.getDate();
            amount = item.getAmount();
            typeInformation = item.getTypeInformation();
            source = item.getSource();
            security = item.getSecurity();
        }

        @Override
        public Annotated getSubject()
        {
            return subject;
        }

        @Override
        public Security getSecurity()
        {
            return security;
        }

        @Override
        public void setSecurity(Security security)
        {
            this.security = security;
        }

        @Override
        public String getTypeInformation()
        {
            return typeInformation;
        }

        @Override
        public LocalDateTime getDate()
        {
            return dateTime;
        }

        @Override
        public Money getAmount()
        {
            return amount;
        }

        @Override
        public void setNote(String note)
        {
            subject.setNote(note);
        }

        @Override
        public String getSource()
        {
            return source;
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            throw new UnsupportedOperationException();
        }

        public void setSkipReason(String reason)
        {
            skipReason = reason;
        }

        public String getSkipReason()
        {
            return skipReason;
        }

    }

    /**
     * Returns a readable label for the type of documents
     */
    String getLabel();

    /**
     * Returns a list of extracted items.
     */
    List<Item> extract(SecurityCache securityCache, InputFile file, List<Exception> errors);

    default List<Item> extract(List<InputFile> file, List<Exception> errors)
    {
        // keep the method signature stable to avoid changing *all* test cases.
        // one could move the Client away from the constructor and into the
        // extract method. Maybe even remove all state from the extractors by
        // passing on a ExtractionContext.

        Client client = null;

        if (this instanceof AbstractPDFExtractor pdf)
        {
            client = pdf.getClient();
        }
        else if (this instanceof IBFlexStatementExtractor ibflex)
        {
            client = ibflex.getClient();
        }
        else
        {
            throw new IllegalArgumentException("can't evaluate client for class " + this.getClass().getName()); //$NON-NLS-1$
        }

        SecurityCache securityCache = new SecurityCache(client);

        List<Item> result = file.stream() //
                        .flatMap(f -> extract(securityCache, f, errors).stream()) //
                        .collect(toMutableList());

        Map<Extractor, List<Item>> itemsByExtractor = new HashMap<>();
        postProcessing(result);
        itemsByExtractor.put(this, result);

        securityCache.addMissingSecurityItems(itemsByExtractor);

        return result;
    }

    default void postProcessing(List<Item> result)
    {
    }

}
