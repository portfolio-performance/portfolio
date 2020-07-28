package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Context;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
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
        /**
         * Store arbitrary data with the extracted item. Currently to pass the
         * JSON structure of the transaction to the test cases
         */
        private Object data;

        private Account accountPrimary;

        private Account accountSecondary;

        private Portfolio portfolioPrimary;

        private Portfolio portfolioSecondary;
        
        private boolean investmentPlanItem = false;

        public abstract Annotated getSubject();

        public abstract Security getSecurity();

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

        public abstract Status apply(ImportAction action, Context context);

        public Object getData()
        {
            return data;
        }

        public void setData(Object data)
        {
            this.data = data;
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

    }

    /**
     * Represents an item which cannot be imported because it is either not
     * supported or not needed. It is used for documents that can be
     * successfully parsed, but do not contain any transaction relevant to
     * Portfolio Performance. For example, a tax refund of 0 Euro (Consorsbank)
     * can be parsed, but is of no further use to PP.
     */
    static class NonImportableItem extends Item implements Annotated
    {
        private String typeInformation;
        private String note;

        public NonImportableItem(String typeInformation)
        {
            this.typeInformation = typeInformation;
        }

        @Override
        public Annotated getSubject()
        {
            return this;
        }

        @Override
        public Security getSecurity()
        {
            return null;
        }

        @Override
        public String getTypeInformation()
        {
            return typeInformation;
        }

        @Override
        public LocalDateTime getDate()
        {
            return null;
        }

        @Override
        public Status apply(ImportAction action, Context context)
        {
            return action.process(this);
        }

        @Override
        public void setNote(String note)
        {
            this.note = note;
        }

        @Override
        public String getNote()
        {
            return note;
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
        public LocalDateTime getDate()
        {
            return transaction.getDateTime();
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
            {
                Account account = getAccountPrimary();
                if (account == null)
                    account = context.getAccount();
                return action.process((AccountTransaction) transaction, account);
            }
            else if (transaction instanceof PortfolioTransaction)
            {
                Portfolio portfolio = getPortfolioPrimary();
                if (portfolio == null)
                    portfolio = context.getPortfolio();
                return action.process((PortfolioTransaction) transaction, portfolio);
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
        public Status apply(ImportAction action, Context context)
        {
            Account account = getAccountPrimary();
            if (account == null)
                account = context.getAccount();

            Portfolio portfolio = getPortfolioPrimary();
            if (portfolio == null)
                portfolio = context.getPortfolio();

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
        public Status apply(ImportAction action, Context context)
        {
            Account account = getAccountPrimary();
            if (account == null)
                account = context.getAccount();

            Account accountSecondary = getAccountSecondary();
            if (accountSecondary == null)
                accountSecondary = context.getSecondaryAccount();

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
        public Status apply(ImportAction action, Context context)
        {
            Portfolio portfolio = getPortfolioPrimary();
            if (portfolio == null)
                portfolio = context.getPortfolio();

            Portfolio portfolioSecondary = getPortfolioSecondary();
            if (portfolioSecondary == null)
                portfolioSecondary = context.getSecondaryPortfolio();

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
        public Status apply(ImportAction action, Context context)
        {
            return action.process(security, price);
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

        if (this instanceof AbstractPDFExtractor)
        {
            client = ((AbstractPDFExtractor) this).getClient();
        }
        else if (this instanceof IBFlexStatementExtractor)
        {
            client = ((IBFlexStatementExtractor) this).getClient();
        }
        else
        {
            throw new IllegalArgumentException();
        }

        SecurityCache securityCache = new SecurityCache(client);

        List<Item> result = file.stream() //
                        .flatMap(f -> extract(securityCache, f, errors).stream()) //
                        .collect(Collectors.toList());

        Map<Extractor, List<Item>> itemsByExtractor = new HashMap<>();
        itemsByExtractor.put(this, postProcessing(result));

        securityCache.addMissingSecurityItems(itemsByExtractor);

        return result;
    }

    default List<Item> postProcessing(List<Item> result)
    {
        return result;
    }

}
