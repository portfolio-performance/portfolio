package name.abuchen.portfolio.cli.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * One business event: a buy or sell joins its investment-account and
 * cash-account sides, a transfer joins its outgoing and incoming side. A side
 * whose partner is missing, which only happens in damaged or hand-edited
 * files, is a record of its own under its raw core type.
 * {@link TransactionRecords} joins the sides.
 *
 * @param id
 *            the investment-account side of a buy or sell, the outgoing side of
 *            a transfer, and the transaction itself otherwise
 * @param type
 *            core's enum name, except that an intact transfer is
 *            <code>TRANSFER</code>
 * @param kind
 *            of a transfer: <code>cash</code> or <code>security</code>
 * @param primary
 *            the side that supplies date, amount, units, note and source
 * @param sides
 *            the UUIDs of all sides, {@code id} first
 * @param incoming
 *            the incoming side of a transfer
 */
public record TransactionRecord(String id, String type, Optional<TransferKind> kind, Transaction primary,
                List<String> sides, Optional<String> investmentAccount, Optional<String> cashAccount,
                Optional<String> from, Optional<String> to, Optional<Transaction> incoming)
{
    public static final String TRANSFER = "TRANSFER";

    /** What a transfer moves. */
    public enum TransferKind
    {
        CASH, SECURITY;

        /** The name published in the output. */
        public String id()
        {
            return name().toLowerCase(Locale.ROOT);
        }

        public static List<String> ids()
        {
            return Arrays.stream(values()).map(TransferKind::id).toList();
        }
    }

    private boolean isTransferOf(TransferKind expected)
    {
        return kind.filter(expected::equals).isPresent();
    }

    /** The latest last modified time across the sides of the record; empty if none of them has one. */
    public Optional<Instant> updatedAt()
    {
        var partner = incoming.or(() -> sides.size() > 1 && primary.getCrossEntry() != null
                        ? Optional.ofNullable(primary.getCrossEntry().getCrossTransaction(primary))
                        : Optional.empty());

        return Stream.concat(Stream.of(primary), partner.stream()).map(Transaction::getUpdatedAt)
                        .filter(Objects::nonNull).max(Comparator.naturalOrder());
    }

    /** The incoming side of a cash transfer between accounts of different currencies. */
    Optional<Transaction> crossCurrencyIncoming()
    {
        return incoming.filter(side -> isTransferOf(TransferKind.CASH) && side.getCurrencyCode() != null
                        && !side.getCurrencyCode().equals(primary.getCurrencyCode()));
    }

    /** Portfolio sides always have shares; of the cash sides only a dividend, and only when it states them. */
    boolean hasShares()
    {
        return primary instanceof PortfolioTransaction || primary instanceof AccountTransaction account
                        && account.getType() == AccountTransaction.Type.DIVIDENDS && account.getShares() != 0;
    }

    /** Whether any side is in the given investment account. */
    public boolean touchesInvestmentAccount(String uuid)
    {
        return investmentAccount.filter(uuid::equals).isPresent()
                        || isTransferOf(TransferKind.SECURITY) && transfersBetween(uuid);
    }

    /** Whether any side is in the given cash account. */
    public boolean touchesCashAccount(String uuid)
    {
        return cashAccount.filter(uuid::equals).isPresent() || isTransferOf(TransferKind.CASH) && transfersBetween(uuid);
    }

    private boolean transfersBetween(String accountUuid)
    {
        return from.filter(accountUuid::equals).isPresent() || to.filter(accountUuid::equals).isPresent();
    }

    /** The record's own rate into the reporting currency: that of its forex unit or of its transfer. */
    public Optional<BigDecimal> ownRate(String reportingCode)
    {
        var forex = primary.getUnits().filter(unit -> unit.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .filter(unit -> unit.getForex() != null && reportingCode.equals(unit.getForex().getCurrencyCode()))
                        .map(Transaction.Unit::getExchangeRate);

        var transfer = crossCurrencyIncoming().filter(side -> reportingCode.equals(side.getCurrencyCode()))
                        .flatMap(side -> primary.getUnit(Transaction.Unit.Type.GROSS_VALUE))
                        .map(Transaction.Unit::getExchangeRate);

        // the stored rate converts into the record's currency, so the inverse converts out of it
        return forex.findFirst().or(() -> transfer).filter(stored -> stored.signum() != 0)
                        .map(stored -> ExchangeRate.inverse(stored));
    }
}
