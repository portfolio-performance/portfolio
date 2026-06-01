package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.money.Money;

/**
 * Builds the carried acquisition lots for a tax-neutral fund transfer.
 * <p>
 * The transfer itself has a current market value, but the target fund must keep
 * the source fund's original acquisition basis. This helper reconstructs the
 * open source lots FIFO-style and then maps the consumed source shares to the
 * target shares entered by the user.
 */
public final class FundTransferLotBuilder
{
    private static final class OpenLot
    {
        private final LocalDate acquisitionDate;
        private final String sourceTransactionUUID;
        private long shares;
        private Money acquisitionValue;

        private OpenLot(LocalDate acquisitionDate, long shares, Money acquisitionValue, String sourceTransactionUUID)
        {
            this.acquisitionDate = acquisitionDate;
            this.shares = shares;
            this.acquisitionValue = acquisitionValue;
            this.sourceTransactionUUID = sourceTransactionUUID;
        }
    }

    private FundTransferLotBuilder()
    {
    }

    public static List<FundTransferEntry.CarriedLot> build(Client client, Portfolio sourcePortfolio,
                    Security sourceSecurity, LocalDateTime transferDate, long sourceShares, long targetShares,
                    String currencyCode)
    {
        return build(client, sourcePortfolio, sourceSecurity, transferDate, sourceShares, targetShares, currencyCode,
                        (FundTransferEntry) null);
    }

    public static List<FundTransferEntry.CarriedLot> build(Client client, Portfolio sourcePortfolio,
                    Security sourceSecurity, LocalDateTime transferDate, long sourceShares, long targetShares,
                    String currencyCode, FundTransferEntry ignoredEntry)
    {
        return build(client, sourcePortfolio, sourceSecurity, transferDate, sourceShares, targetShares, currencyCode,
                        ignoredEntry != null ? Set.of(ignoredEntry) : Set.of());
    }

    private static List<FundTransferEntry.CarriedLot> build(Client client, Portfolio sourcePortfolio,
                    Security sourceSecurity, LocalDateTime transferDate, long sourceShares, long targetShares,
                    String currencyCode, Set<CrossEntry> ignoredEntries)
    {
        if (sourceShares <= 0 || targetShares <= 0)
            throw new IllegalArgumentException("source and target shares must be positive"); //$NON-NLS-1$

        List<OpenLot> openLots = collectOpenLots(client, sourcePortfolio, sourceSecurity, transferDate, currencyCode,
                        ignoredEntries);
        List<OpenLot> consumedLots = consume(openLots, sourceShares, true);

        long carriedAmount = consumedLots.stream().mapToLong(lot -> lot.acquisitionValue.getAmount()).sum();
        long allocatedTargetShares = 0;
        long allocatedCarriedAmount = 0;

        List<FundTransferEntry.CarriedLot> result = new ArrayList<>();
        for (int index = 0; index < consumedLots.size(); index++)
        {
            OpenLot consumed = consumedLots.get(index);

            boolean isLast = index == consumedLots.size() - 1;

            // Share and money values are integer-scaled. The final carried lot
            // absorbs rounding so the persisted transfer exactly matches the
            // user-entered target shares and consumed source basis.
            long lotTargetShares = isLast ? targetShares - allocatedTargetShares
                            : proportional(consumed.shares, sourceShares, targetShares);
            long lotCarriedAmount = isLast ? carriedAmount - allocatedCarriedAmount
                            : consumed.acquisitionValue.getAmount();

            result.add(new FundTransferEntry.CarriedLot(consumed.acquisitionDate, consumed.shares, lotTargetShares,
                            Money.of(currencyCode, lotCarriedAmount), consumed.sourceTransactionUUID));

            allocatedTargetShares += lotTargetShares;
            allocatedCarriedAmount += lotCarriedAmount;
        }

        return result;
    }

    private static List<OpenLot> collectOpenLots(Client client, Portfolio sourcePortfolio, Security sourceSecurity,
                    LocalDateTime transferDate, String currencyCode, Set<CrossEntry> ignoredEntries)
    {
        List<OpenLot> openLots = new ArrayList<>();
        LocalDate transferLocalDate = transferDate.toLocalDate();

        for (PortfolioTransaction transaction : Transaction.sortByDate(new ArrayList<>(sourcePortfolio.getTransactions())))
        {
            // When editing an existing transfer, rebuild the preview from the
            // portfolio state as if that transfer had not been recorded yet.
            if (ignoredEntries.contains(transaction.getCrossEntry()))
                continue;

            // Statement of Assets and portfolio snapshots are date-based. The
            // fund transfer dialog defaults new transactions to 00:00, so using
            // a strict timestamp comparison would hide same-day lots that the
            // user can already see as held on the transfer date.
            if (!sourceSecurity.equals(transaction.getSecurity())
                            || transaction.getDateTime().toLocalDate().isAfter(transferLocalDate))
                continue;

            switch (transaction.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                    addOpenLot(openLots, transaction.getDateTime().toLocalDate(), transaction.getShares(),
                                    requireCurrency(transaction.getMonetaryAmount(), currencyCode),
                                    transaction.getUUID());
                    break;
                case FUND_TRANSFER_IN:
                    addCarriedFundTransferLots(openLots, transaction, currencyCode);
                    break;
                case TRANSFER_IN:
                    addTransferredLots(client, openLots, transaction, currencyCode, ignoredEntries);
                    break;
                case SELL:
                case DELIVERY_OUTBOUND:
                case FUND_TRANSFER_OUT:
                case TRANSFER_OUT:
                    consume(openLots, transaction.getShares(), false);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return openLots;
    }

    private static void addCarriedFundTransferLots(List<OpenLot> openLots, PortfolioTransaction transaction,
                    String currencyCode)
    {
        if (!(transaction.getCrossEntry() instanceof FundTransferEntry entry))
            throw new UnsupportedOperationException();

        for (FundTransferEntry.CarriedLot lot : entry.getCarriedLots())
            addOpenLot(openLots, lot.getAcquisitionDate(), lot.getTargetShares(),
                            requireCurrency(lot.getAcquisitionValue(), currencyCode), lot.getSourceTransactionUUID());
    }

    private static void addTransferredLots(Client client, List<OpenLot> openLots, PortfolioTransaction transaction,
                    String currencyCode, Set<CrossEntry> ignoredEntries)
    {
        if (!(transaction.getCrossEntry().getCrossOwner(transaction) instanceof Portfolio sourcePortfolio))
            throw new UnsupportedOperationException();

        Set<CrossEntry> transferIgnoredEntries = new HashSet<>(ignoredEntries);
        transferIgnoredEntries.add(transaction.getCrossEntry());

        List<FundTransferEntry.CarriedLot> lots = build(client, sourcePortfolio, transaction.getSecurity(),
                        transaction.getDateTime(), transaction.getShares(), transaction.getShares(), currencyCode,
                        transferIgnoredEntries);

        for (FundTransferEntry.CarriedLot lot : lots)
            addOpenLot(openLots, lot.getAcquisitionDate(), lot.getTargetShares(), lot.getAcquisitionValue(),
                            lot.getSourceTransactionUUID());
    }

    private static void addOpenLot(List<OpenLot> openLots, LocalDate acquisitionDate, long shares, Money acquisitionValue,
                    String sourceTransactionUUID)
    {
        if (shares <= 0)
            return;

        openLots.add(new OpenLot(acquisitionDate, shares, acquisitionValue, sourceTransactionUUID));
    }

    private static List<OpenLot> consume(List<OpenLot> openLots, long shares, boolean collect)
    {
        long remaining = shares;
        List<OpenLot> consumedLots = new ArrayList<>();

        for (OpenLot lot : new ArrayList<>(openLots))
        {
            if (remaining == 0)
                break;

            long consumedShares = Math.min(remaining, lot.shares);
            long consumedAmount = consumedShares == lot.shares ? lot.acquisitionValue.getAmount()
                            : proportional(consumedShares, lot.shares, lot.acquisitionValue.getAmount());

            if (collect)
                consumedLots.add(new OpenLot(lot.acquisitionDate, consumedShares,
                                Money.of(lot.acquisitionValue.getCurrencyCode(), consumedAmount),
                                lot.sourceTransactionUUID));

            lot.shares -= consumedShares;
            lot.acquisitionValue = Money.of(lot.acquisitionValue.getCurrencyCode(),
                            lot.acquisitionValue.getAmount() - consumedAmount);
            if (lot.shares == 0)
                openLots.remove(lot);

            remaining -= consumedShares;
        }

        if (remaining > 0)
            throw new IllegalArgumentException("not enough source shares for fund transfer"); //$NON-NLS-1$

        return consumedLots;
    }

    private static Money requireCurrency(Money money, String currencyCode)
    {
        if (!currencyCode.equals(money.getCurrencyCode()))
            throw new IllegalArgumentException("fund transfer lot currency mismatch"); //$NON-NLS-1$

        return money;
    }

    private static long proportional(long numerator, long denominator, long value)
    {
        return Math.round(numerator / (double) denominator * value);
    }
}
