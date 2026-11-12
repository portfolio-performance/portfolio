package name.abuchen.portfolio.cli.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;

/**
 * A record to build, member by member, each value typed and checked on its
 * own: what {@link TransactionInput} reads from the input of an add, or the
 * members of a stored record with those of an update's patch read on top. A
 * member is null if it has no value or its value failed its own check. The
 * checks across members, the build and core's checks are
 * {@link TransactionAdd}'s, the same for both.
 * <p>
 * The type and the kind of a transfer fix the {@link RuleTable} row. A draft
 * holds only the members its row takes. A stored value the row refuses, such
 * as a security on interest, refuses the update (see
 * {@link #refuseStoredValues}); only an ex-date on another type than a
 * dividend is dropped.
 */
final class Draft
{
    /** A unit of the record, with the field its failures are reported under. */
    record Unit(String field, Transaction.Unit unit)
    {
    }

    final String type;
    final TransactionRecord.TransferKind kind;
    final RuleTable.Row row;

    LocalDate date;
    LocalTime time;
    Long amount;
    String currencyCode;
    Long targetAmount;
    BigDecimal exchangeRate;
    Security instrument;
    Long shares;
    Portfolio investmentAccount;
    Account cashAccount;
    TransactionOwner<?> from;
    TransactionOwner<?> to;
    LocalDate exDate;
    Long grossAmount;
    String note;
    String source;
    List<Unit> units = List.of();

    /** The members whose value failed its own check; they are not reported as missing as well. */
    final Set<String> failed = new HashSet<>();

    /** The members the record stores although its row refuses them; an update refuses them. */
    private final List<RuleTable.Ruled> storedRefused = new ArrayList<>();

    /**
     * @param type
     *            the record's type, or null if it is not known
     * @param kind
     *            the kind of a transfer, or null
     */
    Draft(String type, TransactionRecord.TransferKind kind)
    {
        this.type = type;
        this.kind = kind;
        this.row = type == null ? null : RuleTable.Row.of(type, kind).orElse(null);
    }

    /**
     * The draft of a record as it is stored.
     *
     * @param sides
     *            the record's sides with their owners, in the record's order
     */
    static Draft of(TransactionRecord record, List<BuiltRecord.Side> sides)
    {
        var primary = record.primary();
        var draft = new Draft(record.type(), record.kind().orElse(null));

        var dateTime = primary.getDateTime();
        draft.date = dateTime.toLocalDate();
        draft.time = dateTime.toLocalTime().equals(LocalTime.MIDNIGHT) ? null : dateTime.toLocalTime();
        draft.amount = primary.getAmount();
        draft.currencyCode = MasterData.isSet(primary.getCurrencyCode()) ? primary.getCurrencyCode() : null;
        draft.note = MasterData.isSet(primary.getNote()) ? primary.getNote() : null;
        draft.source = MasterData.isSet(primary.getSource()) ? primary.getSource() : null;

        if (draft.row == null)
            return draft;

        draft.storedRefused(RuleTable.Ruled.INSTRUMENT, primary.getSecurity() != null);
        draft.storedRefused(RuleTable.Ruled.SHARES, record.hasShares());
        var transferRate = record.transferRate();
        draft.storedRefused(RuleTable.Ruled.UNITS, primary.getUnits()
                        .anyMatch(unit -> transferRate.isEmpty() || unit != transferRate.get()));
        for (var side : sides)
        {
            if (!draft.takes(RuleTable.Ruled.FROM))
                draft.storedRefused(side.owner() instanceof Portfolio ? RuleTable.Ruled.INVESTMENT_ACCOUNT
                                : RuleTable.Ruled.CASH_ACCOUNT, true);
        }

        if (draft.takes(RuleTable.Ruled.TARGET_AMOUNT))
            record.crossCurrencyIncoming().ifPresent(incoming -> draft.targetAmount = incoming.getAmount());
        if (draft.takes(RuleTable.Ruled.EXCHANGE_RATE))
            record.transferRate().ifPresent(unit -> draft.exchangeRate = unit.getExchangeRate());
        if (draft.takes(RuleTable.Ruled.INSTRUMENT))
            draft.instrument = primary.getSecurity();
        if (draft.takes(RuleTable.Ruled.SHARES) && record.hasShares())
            draft.shares = primary.getShares();

        if (draft.takes(RuleTable.Ruled.FROM))
        {
            draft.from = sides.get(0).owner();
            draft.to = sides.get(1).owner();
        }
        else
        {
            for (var side : sides)
            {
                if (side.owner() instanceof Portfolio portfolio && draft.takes(RuleTable.Ruled.INVESTMENT_ACCOUNT))
                    draft.investmentAccount = portfolio;
                else if (side.owner() instanceof Account account && draft.takes(RuleTable.Ruled.CASH_ACCOUNT))
                    draft.cashAccount = account;
            }
        }

        if (draft.takes(RuleTable.Ruled.EX_DATE) && primary instanceof AccountTransaction account
                        && account.getExDate() != null)
            draft.exDate = account.getExDate().toLocalDate();

        if (draft.takes(RuleTable.Ruled.UNITS))
        {
            var stored = primary.getUnits().toList();
            draft.units = IntStream.range(0, stored.size())
                            .mapToObj(i -> new Unit(TransactionShape.UNITS.name() + "[" + i + "]", stored.get(i)))
                            .toList();
        }

        return draft;
    }

    private void storedRefused(RuleTable.Ruled member, boolean stored)
    {
        if (stored && !takes(member) && !storedRefused.contains(member))
            storedRefused.add(member);
    }

    /**
     * Refuses the stored values that the row refuses, unless the patch names
     * the member: then the patch's value is checked instead, and null clears
     * it. An ex-date stored on another type than a dividend is not refused;
     * the update drops it, as core ignores it.
     *
     * @return a failure under the member's refusal code per such value
     */
    List<FieldFailure> refuseStoredValues(JsonObject patch)
    {
        return storedRefused.stream().filter(member -> !patch.has(member.id()))
                        .map(member -> new FieldFailure(member.id(), member.refusalCode(),
                                        member.refusalMessage(row.label()), null))
                        .toList();
    }

    /** Whether the row takes the member at all. */
    private boolean takes(RuleTable.Ruled member)
    {
        return row.rule(member) != RuleTable.Rule.REFUSED;
    }

    /** The value of a ruled member, or null if it has none. */
    Object get(RuleTable.Ruled member)
    {
        return switch (member)
        {
            case KIND -> kind;
            case TARGET_AMOUNT -> targetAmount;
            case EXCHANGE_RATE -> exchangeRate;
            case INSTRUMENT -> instrument;
            case SHARES -> shares;
            case INVESTMENT_ACCOUNT -> investmentAccount;
            case CASH_ACCOUNT -> cashAccount;
            case FROM -> from;
            case TO -> to;
            case EX_DATE -> exDate;
            case GROSS_AMOUNT -> grossAmount;
            case UNITS -> units.isEmpty() ? null : units;
        };
    }

    /** Whether the member has a value, or was given one that failed its own check. */
    boolean has(RuleTable.Ruled member)
    {
        return get(member) != null || failed.contains(member.id());
    }

    /** The date and time, or null if either failed or there is no date. */
    LocalDateTime dateTime()
    {
        if (date == null || failed.contains(TransactionShape.TIME.name()))
            return null;
        return time == null ? date.atStartOfDay() : date.atTime(time);
    }

    /**
     * Refuses a patch that changes the type or the kind of a transfer; they
     * are fixed, so they are checked before any other member of the patch.
     *
     * @return the <code>type-immutable</code> and <code>kind-immutable</code>
     *         failures
     */
    List<FieldFailure> refuseTypeOrKindChange(JsonObject patch)
    {
        var failures = new ArrayList<FieldFailure>();

        var typeMember = TransactionShape.TYPE.name();
        if (patch.has(typeMember) && differs(patch.get(typeMember), type))
            failures.add(new FieldFailure(typeMember, ErrorCode.TYPE_IMMUTABLE,
                            "The type of a record can't change; delete it and add a new one.", null));

        var kindMember = TransactionShape.KIND.name();
        if (kind != null && patch.has(kindMember) && differs(patch.get(kindMember), kind.id()))
            failures.add(new FieldFailure(kindMember, ErrorCode.KIND_IMMUTABLE,
                            "The kind of a transfer can't change; delete it and add a new one.", null));

        return failures;
    }

    private static boolean differs(JsonElement value, String current)
    {
        return !(OperationInput.isText(value) && current.equals(value.getAsString()));
    }
}
