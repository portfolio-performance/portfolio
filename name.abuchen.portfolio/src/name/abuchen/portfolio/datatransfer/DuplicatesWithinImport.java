package name.abuchen.portfolio.datatransfer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;

/**
 * Detects duplicates between the input files of one import, e.g. the same
 * document imported twice with different file names, or an account statement
 * and a purchase document describing the same transaction.
 * <p>
 * The detection consists of three parts:
 * <ul>
 * <li>Matching: which items describe the same transaction (see
 * {@link DuplicateKey}).</li>
 * <li>Which copy to keep: the more detailed document first (see
 * {@link #BY_DETAIL}), then a fixed order of input file and position.</li>
 * <li>How many copies to keep: as many as the input file with the most
 * occurrences contains.</li>
 * </ul>
 * The result depends only on the set of items, not on the order in which the
 * files were read.
 */
public final class DuplicatesWithinImport
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
     * Prefers the more detailed document: more fee and tax units first, then a
     * gross value unit (forex). For example, a purchase document with fees and
     * taxes is kept rather than the account statement which only lists the
     * total. Amount and shares are equal within a group (see
     * {@link DuplicateKey}).
     */
    public static final Comparator<Extractor.Item> BY_DETAIL = Comparator
                    .comparingLong((Extractor.Item item) -> feeAndTaxUnitsOf(item)).reversed()
                    .thenComparing(item -> hasGrossValueUnit(item) ? 0 : 1);

    /**
     * Transactions which are compared with each other: same kind of
     * transaction (incl. equivalent types), date, currency, amount, shares and
     * security.
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
     * An item of the import together with the input file it stems from and
     * its position within that file.
     */
    private record Candidate(Extractor.Item item, String sourceKey, int position)
    {
    }

    /**
     * Order in which the copies are kept: the more detailed document first,
     * then by input file and position within the file.
     */
    private static final Comparator<Candidate> KEEP_ORDER = Comparator
                    .comparing(Candidate::item, BY_DETAIL) //
                    .thenComparing(Candidate::sourceKey) //
                    .thenComparingInt(Candidate::position);

    private static final DuplicatesWithinImport NONE = new DuplicatesWithinImport(Collections.emptyMap());

    /**
     * Duplicates (identity based) mapped to the kept copy from another input
     * file.
     */
    private final Map<Extractor.Item, Extractor.Item> duplicates;

    private DuplicatesWithinImport(Map<Extractor.Item, Extractor.Item> duplicates)
    {
        this.duplicates = duplicates;
    }

    /**
     * Returns an instance without any duplicates (e.g. if the import does not
     * identify the input files).
     */
    public static DuplicatesWithinImport none()
    {
        return NONE;
    }

    /**
     * Determines the duplicates within the given items. Items without input
     * file (neither {@link #SOURCE_KEY} nor source) and items which are no
     * transactions are ignored.
     * <ul>
     * <li>If all occurrences of a transaction stem from the same input file,
     * none of them is a duplicate (a document can legitimately contain
     * identical transactions).</li>
     * <li>The input file with the most occurrences of a transaction determines
     * how many of them are kept - but not which ones.</li>
     * <li>Which ones are kept is determined by {@link #KEEP_ORDER}. Therefore
     * the kept transactions can stem from different input files, and
     * occurrences in the input file with the most occurrences can be marked as
     * duplicates.</li>
     * </ul>
     */
    public static DuplicatesWithinImport of(List<Extractor.Item> items)
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
                            .add(new Candidate(item, sourceKey, position));
        }

        Map<Extractor.Item, Extractor.Item> duplicates = new IdentityHashMap<>();

        for (var group : groups.values())
            markDuplicates(group, duplicates);

        return duplicates.isEmpty() ? NONE : new DuplicatesWithinImport(duplicates);
    }

    /**
     * Keeps as many candidates of the group as the input file with the most
     * occurrences contains and maps the others to a kept copy from another
     * input file.
     */
    private static void markDuplicates(List<Candidate> group, Map<Extractor.Item, Extractor.Item> duplicates)
    {
        if (group.size() < 2)
            return;

        Map<String, Integer> occurrencesPerSource = new HashMap<>();
        group.forEach(c -> occurrencesPerSource.merge(c.sourceKey(), 1, Integer::sum));
        var numberToKeep = Collections.max(occurrencesPerSource.values());

        var sorted = group.stream().sorted(KEEP_ORDER).toList();
        var kept = sorted.subList(0, numberToKeep);

        var index = 0;
        for (var duplicate : sorted.subList(numberToKeep, sorted.size()))
        {
            var others = kept.stream().filter(c -> !c.sourceKey().equals(duplicate.sourceKey())).toList();

            // at least one kept copy stems from another input file, otherwise
            // the input file of the duplicate would contain more occurrences
            // than are kept
            duplicates.put(duplicate.item(), others.get(index++ % others.size()).item());
        }
    }

    /**
     * Returns the kept copy (from another input file) if the item is a
     * duplicate within the import.
     */
    public Optional<Extractor.Item> keptCopyOf(Extractor.Item item)
    {
        return Optional.ofNullable(duplicates.get(item));
    }

    /**
     * Returns true if the item is a duplicate of an item from another input
     * file of the import.
     */
    public boolean isDuplicate(Extractor.Item item)
    {
        return duplicates.containsKey(item);
    }

    /**
     * Returns the transaction of the item which is compared within the import
     * - the same transaction which is checked against the existing
     * transactions.
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

    /**
     * Returns the key which groups identical transactions (see
     * {@link DuplicateKey}).
     */
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

    /**
     * Returns the number of fee and tax units of the item.
     */
    private static long feeAndTaxUnitsOf(Extractor.Item item)
    {
        var transaction = representativeTransactionOf(item);
        if (transaction == null)
            return 0;

        return transaction.getUnits() //
                        .filter(u -> u.getType() == Unit.Type.FEE || u.getType() == Unit.Type.TAX) //
                        .count();
    }

    /**
     * Returns true if the item has a gross value unit (forex).
     */
    private static boolean hasGrossValueUnit(Extractor.Item item)
    {
        var transaction = representativeTransactionOf(item);
        return transaction != null && transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent();
    }

    /**
     * Returns the account transaction types which are considered equivalent
     * (as in the comparison with existing transactions): deposit and inbound
     * transfer, removal and outbound transfer.
     */
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

    /**
     * Returns the portfolio transaction types which are considered equivalent
     * (as in the comparison with existing transactions): buy and inbound
     * delivery, sell and outbound delivery.
     */
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
}
