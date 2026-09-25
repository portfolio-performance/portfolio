package name.abuchen.portfolio.datatransfer.actions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;

public class DetectDuplicatesAction implements ImportAction
{
    /**
     * Key of the {@link Extractor.Item#getData(String) item data} holding a
     * value which uniquely identifies the input file the item was extracted
     * from (e.g. the absolute path). The file name stored as source of the
     * transaction is not unique if files with the same name from different
     * folders or archives are imported.
     */
    public static final String SOURCE_KEY = "sourceKey"; //$NON-NLS-1$

    private final Client client;

    /**
     * If true, the transactions of the current import are compared with each
     * other as well. Only transactions from different sources (files) are
     * compared, because a single document can legitimately contain identical
     * transactions.
     */
    private final boolean detectDuplicatesWithinImport;

    /**
     * Returns the key identifying the source (input file) of a transaction.
     * Transactions are only compared with each other if their keys differ.
     */
    private final Function<Transaction, Object> sourceKeyOf;

    /**
     * Transactions of the current import which have already been processed,
     * grouped by the account or portfolio they will be booked into.
     */
    private final Map<Account, List<AccountTransaction>> importedAccountTransactions = new HashMap<>();
    private final Map<Portfolio, List<PortfolioTransaction>> importedPortfolioTransactions = new HashMap<>();

    public DetectDuplicatesAction(Client client)
    {
        this(client, false);
    }

    public DetectDuplicatesAction(Client client, boolean detectDuplicatesWithinImport)
    {
        this(client, detectDuplicatesWithinImport, Transaction::getSource);
    }

    public DetectDuplicatesAction(Client client, boolean detectDuplicatesWithinImport,
                    Function<Transaction, Object> sourceKeyOf)
    {
        this.client = client;
        this.detectDuplicatesWithinImport = detectDuplicatesWithinImport;
        this.sourceKeyOf = sourceKeyOf;
    }

    /**
     * Creates a function which returns for each transaction of the given items
     * the {@link #SOURCE_KEY} of its item. If the item has no such key, the
     * source (file name) of the transaction is used.
     */
    public static Function<Transaction, Object> sourceKeysOf(List<Extractor.Item> items)
    {
        var keys = new IdentityHashMap<Transaction, Object>();

        for (var item : items)
        {
            var key = item.getData(SOURCE_KEY);
            if (key == null)
                continue;

            var subject = item.getSubject();

            if (subject instanceof Transaction transaction)
            {
                keys.put(transaction, key);
            }
            else if (subject instanceof BuySellEntry entry)
            {
                keys.put(entry.getPortfolioTransaction(), key);
                keys.put(entry.getAccountTransaction(), key);
            }
            else if (subject instanceof AccountTransferEntry entry)
            {
                keys.put(entry.getSourceTransaction(), key);
                keys.put(entry.getTargetTransaction(), key);
            }
            else if (subject instanceof PortfolioTransferEntry entry)
            {
                keys.put(entry.getSourceTransaction(), key);
                keys.put(entry.getTargetTransaction(), key);
            }
        }

        return transaction -> keys.containsKey(transaction) ? keys.get(transaction) : transaction.getSource();
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        var status = check(transaction, account.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(transaction, fromOtherSources(importedAccountTransactions, account, transaction));

        remember(importedAccountTransactions, account, transaction);
        return status;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        var status = check(transaction, portfolio.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(transaction, fromOtherSources(importedPortfolioTransactions, portfolio, transaction));

        remember(importedPortfolioTransactions, portfolio, transaction);
        return status;
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        // search for a match in existing investment plan transactions
        var plans = client.getPlans();
        var i = plans.stream() //
                        .filter(p -> p.getSecurity() != null
                                        && p.getSecurity().equals(entry.getPortfolioTransaction().getSecurity()))
                        .iterator();
        while (i.hasNext())
        {
            var transactions = i.next().getTransactions();
            for (Transaction t : transactions)
            {
                // use portfolio transaction, because it contains the number of
                // shares
                if (isInvestmentPlanDuplicate(entry.getPortfolioTransaction(), t))
                    return new Status(Status.Code.WARNING, Messages.InvestmentPlanItemImportToolTip);
            }
        }

        var accountTransaction = entry.getAccountTransaction();
        var portfolioTransaction = entry.getPortfolioTransaction();

        var status = check(accountTransaction, account.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(portfolioTransaction, portfolio.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(accountTransaction,
                            fromOtherSources(importedAccountTransactions, account, accountTransaction));
        if (status.getCode() == Status.Code.OK)
            status = check(portfolioTransaction,
                            fromOtherSources(importedPortfolioTransactions, portfolio, portfolioTransaction));

        remember(importedAccountTransactions, account, accountTransaction);
        remember(importedPortfolioTransactions, portfolio, portfolioTransaction);
        return status;
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        var transaction = entry.getSourceTransaction();

        var status = check(transaction, source.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(transaction, fromOtherSources(importedAccountTransactions, source, transaction));

        remember(importedAccountTransactions, source, transaction);
        return status;
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        var transaction = entry.getTargetTransaction();

        var status = check(transaction, source.getTransactions());
        if (status.getCode() == Status.Code.OK)
            status = check(transaction, fromOtherSources(importedPortfolioTransactions, source, transaction));

        remember(importedPortfolioTransactions, source, transaction);
        return status;
    }

    public Transaction findInvestmentPlanTransaction(Transaction subject, List<Transaction> transactions)
    {
        for (Transaction t : transactions)
        {
            // search investment plan transactions for potential duplicates
            if (isInvestmentPlanDuplicate(subject, t))
                return t;
        }
        return null;
    }

    /**
     * Returns the already processed transactions of the current import which
     * are booked into the same account or portfolio but stem from a different
     * source (input file) than the subject.
     */
    private <K, T extends Transaction> List<T> fromOtherSources(Map<K, List<T>> imported, K owner, Transaction subject)
    {
        if (!detectDuplicatesWithinImport)
            return Collections.emptyList();

        var subjectKey = sourceKeyOf.apply(subject);
        if (subjectKey == null)
            return Collections.emptyList();

        return imported.getOrDefault(owner, Collections.emptyList()).stream() //
                        .filter(t -> {
                            var key = sourceKeyOf.apply(t);
                            return key != null && !key.equals(subjectKey);
                        }) //
                        .toList();
    }

    private <K, T extends Transaction> void remember(Map<K, List<T>> imported, K owner, T transaction)
    {
        if (detectDuplicatesWithinImport)
            imported.computeIfAbsent(owner, k -> new ArrayList<>()).add(transaction);
    }

    private Status check(AccountTransaction subject, List<AccountTransaction> transactions)
    {
        var equivalentTypes = equivalentTypesOf(subject.getType());

        for (AccountTransaction t : transactions)
        {
            if (!equivalentTypes.contains(t.getType()))
                continue;

            if (isPotentialDuplicate(subject, t))
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
        }

        return Status.OK_STATUS;
    }

    private static EnumSet<AccountTransaction.Type> equivalentTypesOf(AccountTransaction.Type type)
    {
        return switch (type)
        {
            case DEPOSIT, TRANSFER_IN -> EnumSet.of(AccountTransaction.Type.DEPOSIT,
                            AccountTransaction.Type.TRANSFER_IN);
            case REMOVAL, TRANSFER_OUT -> EnumSet.of(AccountTransaction.Type.REMOVAL,
                            AccountTransaction.Type.TRANSFER_OUT);
            default -> EnumSet.of(type);
        };
    }

    private Status check(PortfolioTransaction subject, List<PortfolioTransaction> transactions)
    {
        var equivalentTypes = equivalentTypesOf(subject.getType());

        for (var t : transactions)
        {
            if (!equivalentTypes.contains(t.getType()))
                continue;

            if (isPotentialDuplicate(subject, t))
                return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
        }

        return Status.OK_STATUS;
    }

    private static EnumSet<PortfolioTransaction.Type> equivalentTypesOf(PortfolioTransaction.Type type)
    {
        return switch (type)
        {
            case BUY, DELIVERY_INBOUND -> EnumSet.of(PortfolioTransaction.Type.BUY,
                            PortfolioTransaction.Type.DELIVERY_INBOUND);
            case SELL, DELIVERY_OUTBOUND -> EnumSet.of(PortfolioTransaction.Type.SELL,
                            PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            default -> EnumSet.of(type);
        };
    }

    private boolean isPotentialDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getDateTime().toLocalDate().equals(subject.getDateTime().toLocalDate()))
            return false;

        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (other.getAmount() != subject.getAmount())
            return false;

        if (other.getShares() != subject.getShares())
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity())) // NOSONAR
            return false;

        return true;
    }

    /*
     * The other transaction is one generated by an investment plan, stored in
     * the client
     */
    private boolean isInvestmentPlanDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity())) // NOSONAR
            return false;

        // amount might differ due to rounding - accept a difference of 1%
        long amount = subject.getAmount();
        if (amount * 1.01 < other.getAmount() || amount * 0.99 > other.getAmount())
            return false;

        LocalDateTime date = subject.getDateTime();
        // date can be up to five days after other's date (due to shifted
        // executions on weekends and holidays)
        if (date.isBefore(other.getDateTime()) || date.minusDays(5).isAfter(other.getDateTime()))
            return false;

        // number of shares might differ due to differing prices per share used
        // by investment plan
        // accept a difference of 10%
        long shares = subject.getShares();
        if (shares * 1.1 < other.getShares() || shares * 0.9 > other.getShares())
            return false;

        return true;
    }
}
