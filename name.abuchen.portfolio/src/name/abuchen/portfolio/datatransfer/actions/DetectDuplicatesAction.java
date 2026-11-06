package name.abuchen.portfolio.datatransfer.actions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
import name.abuchen.portfolio.model.Transaction.Unit;

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
     * Maximum difference in minutes between the times of two transactions of
     * the same day which can still be duplicates. Times are compared with
     * minute precision because some documents state seconds and others do not.
     * The tolerance covers rounding and documents which state a slightly
     * different time for the same execution.
     * <p>
     * A time of midnight means that the document does not state a time. Such a
     * transaction can be a duplicate of a transaction with any time.
     */
    static final long TIME_TOLERANCE_MINUTES = 2;

    /**
     * Transactions which are compared with each other to detect duplicates
     * within the import: same kind of transaction (incl. equivalent types),
     * date, currency, amount, shares and security. The time is compared
     * separately (see {@link #TIME_TOLERANCE_MINUTES}).
     * <p>
     * Amount and shares are compared exactly. The relaxed rules applied when
     * comparing with existing transactions (dividends without number of shares,
     * interest with and without taxes) do not apply within the import, because
     * they do not form distinct groups of identical transactions.
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

    /**
     * A transaction of the import together with the existing transactions it
     * is compared with.
     */
    private record Check(Transaction subject, List<? extends Transaction> transactions)
    {
    }

    /**
     * An item of the import which is matched one to one with the existing
     * transactions. The first check identifies the item when it is processed.
     */
    private record ImportedItem(Extractor.Item item, String sourceKey, int position, List<Check> checks)
    {
        Transaction key()
        {
            return checks.get(0).subject();
        }
    }

    /**
     * A possible assignment of an imported item to an existing transaction (or
     * the cross entry of an existing buy/sell or transfer).
     */
    private record Edge(ImportedItem item, Object existing, int sourceRank, long timeDistance, String existingKey)
    {
    }

    /**
     * Order in which imported items are preferred: by input file, then by
     * position within the file. The order does not depend on the order in
     * which the files were read.
     */
    private static final Comparator<ImportedItem> IMPORTED_ITEM_ORDER = Comparator
                    .comparing(ImportedItem::sourceKey).thenComparingInt(ImportedItem::position);

    /**
     * Order in which assignments are preferred: the same file name as the
     * existing transaction first, then the closest time, then the fixed order
     * of the imported items and of the existing transactions.
     */
    private static final Comparator<Edge> EDGE_ORDER = Comparator.comparingInt(Edge::sourceRank) //
                    .thenComparingLong(Edge::timeDistance) //
                    .thenComparing(Edge::item, IMPORTED_ITEM_ORDER) //
                    .thenComparing(Edge::existingKey);

    /**
     * Records which existing transactions an item is compared with. It uses
     * the same accounts and portfolios as the later call to
     * {@link Extractor.Item#apply}.
     */
    private static class CheckCollector implements ImportAction
    {
        private List<Check> checks = List.of();

        List<Check> collect(Extractor.Item item, Context context)
        {
            checks = List.of();
            try
            {
                item.apply(this, context);
            }
            catch (RuntimeException e)
            {
                // the item is compared with all existing transactions when it
                // is processed
                return List.of();
            }
            return checks;
        }

        @Override
        public Status process(AccountTransaction transaction, Account account)
        {
            checks = checksOf(transaction, account);
            return Status.OK_STATUS;
        }

        @Override
        public Status process(PortfolioTransaction transaction, Portfolio portfolio)
        {
            checks = checksOf(transaction, portfolio);
            return Status.OK_STATUS;
        }

        @Override
        public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
        {
            checks = checksOf(entry, account, portfolio);
            return Status.OK_STATUS;
        }

        @Override
        public Status process(AccountTransferEntry entry, Account source, Account target)
        {
            checks = checksOf(entry, source);
            return Status.OK_STATUS;
        }

        @Override
        public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
        {
            checks = checksOf(entry, source);
            return Status.OK_STATUS;
        }
    }

    private final Client client;

    /**
     * Transactions of the current import which are duplicates of other
     * transactions of the same import (identity based).
     */
    private final Set<Transaction> duplicatesWithinImport;

    /**
     * Result of the one to one assignment of imported items to existing
     * transactions, keyed by {@link ImportedItem#key()} (identity based):
     * {@code true} if the item is assigned to an existing transaction and
     * therefore a potential duplicate, {@code false} if not. Items without
     * entry are compared with all existing transactions.
     */
    private final Map<Transaction, Boolean> assignedToExisting;

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
        this.assignedToExisting = Collections.emptyMap();
    }

    /**
     * @param importedItems
     *            the items of the current import. They are matched one to one
     *            with the existing transactions: an existing transaction marks
     *            at most one imported item as potential duplicate.
     * @param withinImport
     *            if true, the items are additionally checked for duplicates
     *            among each other
     * @param context
     *            the context used to determine the account and portfolio of
     *            each item (as in {@link Extractor.Item#apply})
     */
    public DetectDuplicatesAction(Client client, List<Extractor.Item> importedItems, boolean withinImport,
                    Context context)
    {
        this.client = client;
        this.duplicatesWithinImport = withinImport ? detectDuplicatesWithinImport(importedItems)
                        : Collections.emptySet();
        this.assignedToExisting = assignToExisting(importedItems, context, duplicatesWithinImport);
    }

    /**
     * Determines the duplicates within the import. The result depends only on
     * the set of items, not on the order in which the files were read:
     * <ul>
     * <li>If all occurrences of a transaction stem from the same input file,
     * none of them is a duplicate (a document can legitimately contain
     * identical transactions).</li>
     * <li>The input file with the most occurrences of a transaction determines
     * how many of them are kept - but not which ones.</li>
     * <li>Which transactions are kept is determined by a fixed order across all
     * input files: by input file, then by position within the file. Therefore
     * the kept transactions can stem from different input files, and
     * occurrences in the input file with the most occurrences can be marked as
     * duplicates.</li>
     * <li>Amount and shares are compared exactly (see {@link DuplicateKey}).</li>
     * <li>Transactions whose times differ by more than
     * {@link #TIME_TOLERANCE_MINUTES} are no duplicates (see
     * {@link #markDuplicates}).</li>
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
            markDuplicates(group, duplicates);

        return duplicates;
    }

    /**
     * Marks the duplicates within a group of identical transactions of the same
     * day.
     * <p>
     * Transactions with a time are split into clusters of transactions whose
     * times are at most {@link #TIME_TOLERANCE_MINUTES} apart from the
     * previous one. Each cluster keeps as many transactions as the input file
     * with the most occurrences in that cluster.
     * <p>
     * Transactions without time (midnight) can be a duplicate of any of them.
     * They are only kept if the input file with the most occurrences in the
     * whole group contains more transactions than are kept already.
     */
    private static void markDuplicates(List<Candidate> group, Set<Transaction> duplicates)
    {
        if (group.size() < 2)
            return;

        var candidateOrder = Comparator.comparing(Candidate::sourceKey).thenComparingInt(Candidate::position);

        var timed = group.stream() //
                        .filter(c -> hasTime(c.transaction().getDateTime())) //
                        .sorted(Comparator.comparing((Candidate c) -> truncatedToMinutes(c.transaction()))
                                        .thenComparing(candidateOrder))
                        .toList();

        Set<Candidate> kept = Collections.newSetFromMap(new IdentityHashMap<>());

        for (var cluster : clustersByTime(timed))
        {
            cluster.stream() //
                            .sorted(candidateOrder) //
                            .limit(maxOccurrencesPerSource(cluster)) //
                            .forEach(kept::add);
        }

        var numberToKeep = Math.max(kept.size(), maxOccurrencesPerSource(group));

        group.stream() //
                        .filter(c -> !hasTime(c.transaction().getDateTime())) //
                        .sorted(candidateOrder) //
                        .limit(Math.max(0, numberToKeep - kept.size())) //
                        .forEach(kept::add);

        group.stream().filter(c -> !kept.contains(c)).forEach(c -> duplicates.add(c.transaction()));
    }

    /**
     * Splits the candidates (sorted by time) into clusters: a new cluster
     * starts if the time is more than {@link #TIME_TOLERANCE_MINUTES} after the
     * time of the previous candidate.
     */
    private static List<List<Candidate>> clustersByTime(List<Candidate> sortedByTime)
    {
        List<List<Candidate>> clusters = new ArrayList<>();
        List<Candidate> current = null;
        LocalDateTime previous = null;

        for (var candidate : sortedByTime)
        {
            var time = truncatedToMinutes(candidate.transaction());

            if (current == null || ChronoUnit.MINUTES.between(previous, time) > TIME_TOLERANCE_MINUTES)
            {
                current = new ArrayList<>();
                clusters.add(current);
            }

            current.add(candidate);
            previous = time;
        }

        return clusters;
    }

    private static int maxOccurrencesPerSource(List<Candidate> candidates)
    {
        Map<String, Integer> occurrencesPerSource = new HashMap<>();
        candidates.forEach(c -> occurrencesPerSource.merge(c.sourceKey(), 1, Integer::sum));
        return Collections.max(occurrencesPerSource.values());
    }

    /**
     * Assigns the imported items one to one to the existing transactions: an
     * existing transaction marks at most one imported item as potential
     * duplicate. Of n identical imported items, only as many are marked as
     * there are matching existing transactions.
     * <p>
     * The assignment prefers an imported item with the same file name as the
     * existing transaction, then the closest time, then the fixed order of
     * input file and position. It then assigns as many items as possible, so
     * that no existing transaction remains unused while an item matching it is
     * not marked. The result does not depend on the order of the items.
     * <p>
     * Items which are duplicates within the import are excluded: they are
     * marked anyway and must not use up an existing transaction.
     */
    private static Map<Transaction, Boolean> assignToExisting(List<Extractor.Item> items, Context context,
                    Set<Transaction> duplicatesWithinImport)
    {
        if (items.isEmpty() || context == null)
            return Collections.emptyMap();

        var importedItems = importedItemsOf(items, context, duplicatesWithinImport);

        Map<Object, String> existingKeys = new IdentityHashMap<>();
        List<Edge> edges = new ArrayList<>();
        for (var importedItem : importedItems)
            edges.addAll(edgesOf(importedItem, existingKeys));

        // the key of the existing transaction is only known once all edges
        // are collected (the smallest UUID of the transactions of a cross
        // entry)
        edges = edges.stream() //
                        .map(e -> new Edge(e.item(), e.existing(), e.sourceRank(), e.timeDistance(),
                                        existingKeys.get(e.existing())))
                        .sorted(EDGE_ORDER) //
                        .toList();

        Map<ImportedItem, List<Object>> candidates = new IdentityHashMap<>();
        edges.forEach(e -> candidates.computeIfAbsent(e.item(), k -> new ArrayList<>()).add(e.existing()));

        Map<Object, ImportedItem> existingToItem = new IdentityHashMap<>();
        Set<ImportedItem> assigned = Collections.newSetFromMap(new IdentityHashMap<>());

        // first assign the preferred pairs
        for (var edge : edges)
        {
            if (assigned.contains(edge.item()) || existingToItem.containsKey(edge.existing()))
                continue;

            existingToItem.put(edge.existing(), edge.item());
            assigned.add(edge.item());
        }

        // then assign as many of the remaining items as possible by moving
        // already assigned items to other existing transactions
        importedItems.stream() //
                        .filter(item -> candidates.containsKey(item) && !assigned.contains(item)) //
                        .sorted(IMPORTED_ITEM_ORDER) //
                        .forEach(item -> {
                            if (reassign(item, candidates, existingToItem,
                                            Collections.newSetFromMap(new IdentityHashMap<>())))
                                assigned.add(item);
                        });

        Map<Transaction, Boolean> result = new IdentityHashMap<>();
        importedItems.forEach(item -> result.put(item.key(), assigned.contains(item)));
        return result;
    }

    /**
     * Collects the imported items which are matched with the existing
     * transactions.
     */
    private static List<ImportedItem> importedItemsOf(List<Extractor.Item> items, Context context,
                    Set<Transaction> duplicatesWithinImport)
    {
        var collector = new CheckCollector();
        Map<String, Integer> positions = new HashMap<>();
        List<ImportedItem> importedItems = new ArrayList<>();

        for (var item : items)
        {
            if (item.isFailure() || item.isSkipped())
                continue;

            var representative = representativeTransactionOf(item);
            if (representative == null || duplicatesWithinImport.contains(representative))
                continue;

            var checks = collector.collect(item, context);
            if (checks.isEmpty())
                continue;

            var sourceKey = Objects.requireNonNullElse(sourceKeyOf(item), ""); //$NON-NLS-1$
            var position = positions.merge(sourceKey, 1, Integer::sum);

            importedItems.add(new ImportedItem(item, sourceKey, position, checks));
        }

        return importedItems;
    }

    /**
     * Returns the possible assignments of the imported item: one per matching
     * existing transaction, with the best rank if the item matches the
     * existing transaction in several checks.
     */
    private static List<Edge> edgesOf(ImportedItem importedItem, Map<Object, String> existingKeys)
    {
        Map<Object, Edge> best = new IdentityHashMap<>();

        for (var check : importedItem.checks())
        {
            var subject = check.subject();

            for (var other : check.transactions())
            {
                if (!isEquivalentType(subject, other) || !isPotentialDuplicate(subject, other))
                    continue;

                var existing = other.getCrossEntry() != null ? other.getCrossEntry() : other;
                existingKeys.merge(existing, other.getUUID(), (a, b) -> a.compareTo(b) <= 0 ? a : b);

                var sourceRank = other.getSource() != null && other.getSource().equals(subject.getSource()) ? 0 : 1;
                var edge = new Edge(importedItem, existing, sourceRank, timeDistance(subject, other), null);

                best.merge(existing, edge, (a, b) -> a.sourceRank() < b.sourceRank()
                                || (a.sourceRank() == b.sourceRank() && a.timeDistance() <= b.timeDistance()) ? a : b);
            }
        }

        return new ArrayList<>(best.values());
    }

    /**
     * Tries to assign the item to one of its existing transactions. If the
     * existing transaction is assigned already, tries to move that item to
     * another existing transaction (augmenting path).
     */
    private static boolean reassign(ImportedItem item, Map<ImportedItem, List<Object>> candidates,
                    Map<Object, ImportedItem> existingToItem, Set<Object> visited)
    {
        for (var existing : candidates.get(item))
        {
            if (!visited.add(existing))
                continue;

            var other = existingToItem.get(existing);
            if (other == null || reassign(other, candidates, existingToItem, visited))
            {
                existingToItem.put(existing, item);
                return true;
            }
        }

        return false;
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

    private static List<Check> checksOf(AccountTransaction transaction, Account account)
    {
        return List.of(new Check(transaction, account.getTransactions()));
    }

    private static List<Check> checksOf(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return List.of(new Check(transaction, portfolio.getTransactions()));
    }

    private static List<Check> checksOf(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        return List.of(new Check(entry.getAccountTransaction(), account.getTransactions()),
                        new Check(entry.getPortfolioTransaction(), portfolio.getTransactions()));
    }

    private static List<Check> checksOf(AccountTransferEntry entry, Account source)
    {
        return List.of(new Check(entry.getSourceTransaction(), source.getTransactions()));
    }

    private static List<Check> checksOf(PortfolioTransferEntry entry, Portfolio source)
    {
        return List.of(new Check(entry.getTargetTransaction(), source.getTransactions()));
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        var status = checkExisting(checksOf(transaction, account));
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(transaction);
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        var status = checkExisting(checksOf(transaction, portfolio));
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

        var status = checkExisting(checksOf(entry, account, portfolio));
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(entry.getPortfolioTransaction());
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        var status = checkExisting(checksOf(entry, source));
        if (status.getCode() != Status.Code.OK)
            return status;
        return checkWithinImport(entry.getSourceTransaction());
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        var status = checkExisting(checksOf(entry, source));
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

    /**
     * Checks the transactions against the existing transactions. Uses the one
     * to one assignment if the item was part of it, otherwise any matching
     * existing transaction marks the item as potential duplicate.
     */
    private Status checkExisting(List<Check> checks)
    {
        var assigned = assignedToExisting.get(checks.get(0).subject());
        if (assigned != null)
            return assigned.booleanValue() ? new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate)
                            : Status.OK_STATUS;

        for (var check : checks)
        {
            for (var other : check.transactions())
            {
                if (isEquivalentType(check.subject(), other) && isPotentialDuplicate(check.subject(), other))
                    return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);
            }
        }

        return Status.OK_STATUS;
    }

    private static boolean isEquivalentType(Transaction subject, Transaction other)
    {
        if (subject instanceof AccountTransaction s && other instanceof AccountTransaction o)
            return equivalentTypesOf(s.getType()).contains(o.getType());
        else if (subject instanceof PortfolioTransaction s && other instanceof PortfolioTransaction o)
            return equivalentTypesOf(s.getType()).contains(o.getType());
        else
            return false;
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

    private static boolean isPotentialDuplicate(Transaction subject, Transaction other)
    {
        if (!other.getDateTime().toLocalDate().equals(subject.getDateTime().toLocalDate()))
            return false;

        if (!isWithinTimeTolerance(subject.getDateTime(), other.getDateTime()))
            return false;

        if (!other.getCurrencyCode().equals(subject.getCurrencyCode()))
            return false;

        if (other.getAmount() != subject.getAmount() && !hasEquivalentInterestAmount(subject, other))
            return false;

        if (other.getShares() != subject.getShares() && !hasMissingDividendShares(subject, other))
            return false;

        if (!Objects.equals(other.getSecurity(), subject.getSecurity())) // NOSONAR
            return false;

        return true;
    }

    /**
     * Returns true if the document states a time (i.e. the time is not
     * midnight).
     */
    private static boolean hasTime(LocalDateTime dateTime)
    {
        return !dateTime.toLocalTime().equals(LocalTime.MIDNIGHT);
    }

    private static LocalDateTime truncatedToMinutes(Transaction transaction)
    {
        return transaction.getDateTime().truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Returns true if both times are at most {@link #TIME_TOLERANCE_MINUTES}
     * apart (with minute precision) or if one of them has no time.
     */
    private static boolean isWithinTimeTolerance(LocalDateTime a, LocalDateTime b)
    {
        if (!hasTime(a) || !hasTime(b))
            return true;

        return minutesBetween(a, b) <= TIME_TOLERANCE_MINUTES;
    }

    /**
     * Returns the distance in minutes used to prefer the closest existing
     * transaction. A transaction without time is less close than any
     * transaction within the time tolerance.
     */
    private static long timeDistance(Transaction subject, Transaction other)
    {
        if (!hasTime(subject.getDateTime()) || !hasTime(other.getDateTime()))
            return TIME_TOLERANCE_MINUTES + 1;

        return minutesBetween(subject.getDateTime(), other.getDateTime());
    }

    private static long minutesBetween(LocalDateTime a, LocalDateTime b)
    {
        return Math.abs(ChronoUnit.MINUTES.between(a.truncatedTo(ChronoUnit.MINUTES),
                        b.truncatedTo(ChronoUnit.MINUTES)));
    }

    private static boolean hasEquivalentInterestAmount(Transaction subject, Transaction other)
    {
        if (!(subject instanceof AccountTransaction accountTransaction)
                        || accountTransaction.getType() != AccountTransaction.Type.INTEREST)
            return false;

        long subjectTax = subject.getUnitSum(Unit.Type.TAX).getAmount();
        long otherTax = other.getUnitSum(Unit.Type.TAX).getAmount();
        return (subjectTax > 0 && otherTax == 0 && subject.getGrossValue().getAmount() == other.getAmount())
                        || (otherTax > 0 && subjectTax == 0 && other.getGrossValue().getAmount() == subject.getAmount());
    }

    private static boolean hasMissingDividendShares(Transaction subject, Transaction other)
    {
        return subject instanceof AccountTransaction accountTransaction
                        && accountTransaction.getType() == AccountTransaction.Type.DIVIDENDS
                        && (subject.getShares() == 0 || other.getShares() == 0);
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
