package name.abuchen.portfolio.datatransfer.actions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import name.abuchen.portfolio.model.Security;
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

    /**
     * Transactions which are compared with each other to detect duplicates
     * within the import: same kind of transaction (incl. equivalent types),
     * date, currency, amount, shares and security.
     */
    private record DuplicateKey(Set<?> types, LocalDate date, String currencyCode, long amount, long shares,
                    Security security)
    {
    }

    /**
     * A transaction of the import together with the input file it stems from
     * and its position within that file.
     */
    private record Candidate(Transaction transaction, String sourceKey, int position)
    {
    }

    private final Client client;

    /**
     * Transactions of the current import which are duplicates of other
     * transactions of the same import (identity based).
     */
    private final Set<Transaction> duplicatesWithinImport;

    public DetectDuplicatesAction(Client client)
    {
        this(client, Collections.emptyList());
    }

    /**
     * @param importedItems
     *            the items of the current import which are additionally
     *            checked for duplicates among each other. Pass an empty list
     *            to only check against the existing transactions.
     */
    public DetectDuplicatesAction(Client client, List<Extractor.Item> importedItems)
    {
        this.client = client;
        this.duplicatesWithinImport = detectDuplicatesWithinImport(importedItems);
    }

    /**
     * Determines the duplicates within the import. The result depends only on
     * the set of items, not on the order in which the files were read:
     * <ul>
     * <li>Identical transactions from the same input file are no duplicates
     * (a document can legitimately contain identical transactions).</li>
     * <li>The input file with the most occurrences of a transaction determines
     * how many of them are kept.</li>
     * <li>Which transactions are kept is determined by a fixed order: by input
     * file, then by position within the file.</li>
     * </ul>
     */
    private static Set<Transaction> detectDuplicatesWithinImport(List<Extractor.Item> items)
    {
        Map<DuplicateKey, List<Candidate>> groups = new HashMap<>();
        Map<String, Integer> positions = new HashMap<>();

        for (var item : items)
        {
            var transaction = representativeTransactionOf(item);
            if (transaction == null)
                continue;

            var sourceKey = sourceKeyOf(item);
            if (sourceKey == null)
                continue;

            var position = positions.merge(sourceKey, 1, Integer::sum);

            groups.computeIfAbsent(duplicateKeyOf(transaction), k -> new ArrayList<>())
                            .add(new Candidate(transaction, sourceKey, position));
        }

        Set<Transaction> duplicates = Collections.newSetFromMap(new IdentityHashMap<>());

        for (var group : groups.values())
        {
            if (group.size() < 2)
                continue;

            Map<String, Integer> occurrencesPerSource = new HashMap<>();
            group.forEach(c -> occurrencesPerSource.merge(c.sourceKey(), 1, Integer::sum));

            var numberToKeep = Collections.max(occurrencesPerSource.values());

            group.stream() //
                            .sorted(Comparator.comparing(Candidate::sourceKey) //
                                            .thenComparingInt(Candidate::position)) //
                            .skip(numberToKeep) //
                            .forEach(c -> duplicates.add(c.transaction()));
        }

        return duplicates;
    }

    /**
     * Returns the transaction of the item which is used to detect duplicates
     * within the import - the same transaction which is checked against the
     * existing transactions.
     */
    private static Transaction representativeTransactionOf(Extractor.Item item)
    {
        var subject = item.getSubject();

        if (subject instanceof AccountTransaction transaction)
            return transaction;
        else if (subject instanceof PortfolioTransaction transaction)
            return transaction;
        else if (subject instanceof BuySellEntry entry)
            return entry.getPortfolioTransaction();
        else if (subject instanceof AccountTransferEntry entry)
            return entry.getSourceTransaction();
        else if (subject instanceof PortfolioTransferEntry entry)
            return entry.getTargetTransaction();
        else
            return null;
    }

    /**
     * Returns the key identifying the input file of the item. If the item has
     * no {@link #SOURCE_KEY}, the source (file name) is used.
     */
    private static String sourceKeyOf(Extractor.Item item)
    {
        if (item.getData(SOURCE_KEY) instanceof String sourceKey)
            return sourceKey;

        return item.getSource();
    }

    private static DuplicateKey duplicateKeyOf(Transaction transaction)
    {
        Set<?> types;
        if (transaction instanceof AccountTransaction t)
            types = equivalentTypesOf(t.getType());
        else if (transaction instanceof PortfolioTransaction t)
            types = equivalentTypesOf(t.getType());
        else
            throw new IllegalArgumentException(transaction.getClass().getName());

        return new DuplicateKey(types, transaction.getDateTime().toLocalDate(), transaction.getCurrencyCode(),
                        transaction.getAmount(), transaction.getShares(), transaction.getSecurity());
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        var status = check(transaction, account.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(transaction);
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        var status = check(transaction, portfolio.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(transaction);
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

        Status status = check(entry.getAccountTransaction(), account.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        status = check(entry.getPortfolioTransaction(), portfolio.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(entry.getPortfolioTransaction());
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        var status = check(entry.getSourceTransaction(), source.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(entry.getSourceTransaction());
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        var status = check(entry.getTargetTransaction(), source.getTransactions());
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(entry.getTargetTransaction());
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

    private Status checkWithinImport(Transaction subject)
    {
        if (duplicatesWithinImport.contains(subject))
            return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);

        return Status.OK_STATUS;
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
