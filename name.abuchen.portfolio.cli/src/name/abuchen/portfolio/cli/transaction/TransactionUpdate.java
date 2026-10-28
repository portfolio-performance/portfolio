package name.abuchen.portfolio.cli.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;

/**
 * Updates and deletes a whole record, addressed by the UUID of either side.
 * Core can't give a transaction a UUID, so an update builds the record from
 * its {@link Draft} like an add and copies the result onto the existing sides.
 */
final class TransactionUpdate
{
    static final String UPDATE = "transaction.update";
    static final String DELETE = "transaction.delete";
    static final String UUID_MEMBER = "uuid";

    /** The input of an update: every input member of a record, none required, the optional ones nullable. */
    static final Schema PATCH_SCHEMA = TransactionShape.declarePatch(Schema.object(
                    "The fields of a transaction to change, in the shape the transactions output uses. Only the fields present change; null clears an optional field.")
                    .property(OperationSpec.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(UPDATE)))
                    .property(UUID_MEMBER, Schema.reference("UUID of either side of the record to change.")));

    static final Schema DELETE_SCHEMA = Schema.object("The record to delete.")
                    .property(OperationSpec.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(DELETE)))
                    .property(UUID_MEMBER, Schema.reference("UUID of either side of the record."));

    /**
     * The failure codes of an update beyond those its schema implies: those
     * of an add that a patch can reach, and what an update refuses.
     */
    static final Set<ErrorCode> UPDATE_CODES = updateCodes();

    /** The failure codes of a delete beyond those its schema implies. */
    static final Set<ErrorCode> DELETE_CODES = Set.of(ErrorCode.NEGATIVE_HOLDING);

    /** Members of the patch that address the record rather than change it. */
    private static final Set<String> ADDRESSING = Set.of(OperationSpec.OPERATION_MEMBER, UUID_MEMBER);

    /** Members that can't change through an update, or change with every write. */
    private static final Set<String> NOT_COMPARED = Set.of(TransactionShape.ID.name(), TransactionShape.SIDES.name(),
                    TransactionShape.UPDATED_AT.name());

    /** The record that an update or delete addresses: its sides, primary first, with their owners. */
    private record Existing(TransactionRecord record, List<BuiltRecord.Side> sides)
    {
    }

    /** The members that differ, with their values before and after. */
    private record Diff(Map<String, Object> before, Map<String, Object> after)
    {
        static Diff of(Map<String, Object> before, Map<String, Object> after, Set<String> ignored)
        {
            var members = new LinkedHashSet<>(before.keySet());
            members.addAll(after.keySet());
            members.removeAll(ignored);

            var diff = new Diff(new LinkedHashMap<>(), new LinkedHashMap<>());
            for (var member : members)
            {
                if (!Objects.equals(before.get(member), after.get(member)))
                {
                    diff.before().put(member, before.get(member));
                    diff.after().put(member, after.get(member));
                }
            }
            return diff;
        }
    }

    private final TransactionRecords records;
    private final Client client;
    private final Predicate<Object> existedBeforeBatch;
    private final List<FieldFailure> failures = new ArrayList<>();

    TransactionUpdate(TransactionRecords records, Client client, Predicate<Object> existedBeforeBatch)
    {
        this.records = records;
        this.client = client;
        this.existedBeforeBatch = existedBeforeBatch;
    }

    private static Set<ErrorCode> updateCodes()
    {
        var codes = EnumSet.of(ErrorCode.TYPE_IMMUTABLE, ErrorCode.KIND_IMMUTABLE, ErrorCode.RECORD_INCOMPLETE);
        codes.addAll(TransactionAdd.FAILURE_CODES);
        // the draft keeps the stored type and kind, which a patch can't change
        codes.removeAll(Set.of(ErrorCode.UNSUPPORTED_TYPE, ErrorCode.UNSUPPORTED_KIND));
        return Collections.unmodifiableSet(codes);
    }

    Outcome update(JsonObject patch, Claims claims, boolean reportUnknown)
    {
        var input = new OperationInput(patch, client, existedBeforeBatch, failures::add);
        if (reportUnknown)
            input.rejectUnknownFields(PATCH_SCHEMA);

        var existing = find(input, claims);
        if (existing == null)
            return refused();

        if (isIncomplete(existing.record()))
        {
            input.fail(UUID_MEMBER, ErrorCode.RECORD_INCOMPLETE,
                            "The record is missing a side, so it can't be updated; delete it and add it again.");
            return refused();
        }

        var draft = Draft.of(existing.record(), existing.sides());
        if (draft.row == null)
        {
            TransactionInput.unsupportedType(input, draft.type);
            return refused();
        }

        failures.addAll(draft.refuseTypeOrKindChange(patch));
        if (!failures.isEmpty())
            return refused();

        // a member an add doesn't declare refuses the patch; it is reported above if at all
        var unknown = TransactionInput.lay(input, failures, draft, ADDRESSING);
        var built = TransactionAdd.build(draft, failures, existing.sides(), unknown).built();
        if (built == null)
            return refused();

        if (built.sides().size() != existing.sides().size())
        {
            input.fail(UUID_MEMBER, ErrorCode.RECORD_INCOMPLETE,
                            "The record is missing a side, so it can't be updated; delete it and add it again.");
            return refused();
        }

        var id = existing.record().id();
        var before = records.render(existing.record());
        var candidate = records.render(new TransactionRecords.Join(built.sides()).toRecord(built.primary())
                        .orElseThrow());
        if (Diff.of(before, candidate, NOT_COMPARED).after().isEmpty())
            return new Outcome.Unchanged(id);

        var sides = existing.sides();
        for (var i = 0; i < sides.size(); i++)
            copy(built.sides().get(i).transaction(), sides.get(i).transaction());
        move(sides, built.sides());

        var after = records.render(new TransactionRecords.Join(client).toRecord(existing.record().primary())
                        .orElseThrow());
        var diff = Diff.of(before, after, Set.of(TransactionShape.UPDATED_AT.name()));
        return new Outcome.Applied(Outcome.Entity.TRANSACTION, id, diff.before(), diff.after());
    }

    Outcome delete(JsonObject json, Claims claims, boolean reportUnknown)
    {
        var input = new OperationInput(json, client, existedBeforeBatch, failures::add);
        if (reportUnknown)
            input.rejectUnknownFields(DELETE_SCHEMA);
        var unknown = json.keySet().stream().anyMatch(member -> !DELETE_SCHEMA.declares(member));

        var existing = find(input, claims);
        if (existing == null)
            return refused();

        failures.addAll(OversellGuard.check(UUID_MEMBER, OversellGuard.effectsOf(existing.sides()), List.of()));
        if (unknown || !failures.isEmpty())
            return refused();

        var before = records.render(existing.record());
        existing.sides().forEach(this::detach);
        return new Outcome.Applied(Outcome.Entity.TRANSACTION, existing.record().id(), before, Map.of());
    }

    private Outcome refused()
    {
        return new Outcome.Refused(failures);
    }

    /** The record with a side of the given UUID, every side claimed; null after reporting why not. */
    private Existing find(OperationInput input, Claims claims)
    {
        var member = input.json().get(UUID_MEMBER);
        if (member == null || member.isJsonNull())
        {
            input.fail(UUID_MEMBER, ErrorCode.REQUIRED, "This operation needs the UUID of a side of the transaction.");
            return null;
        }
        if (!OperationInput.isText(member))
        {
            input.fail(UUID_MEMBER, ErrorCode.INVALID_TYPE, "The UUID must be a string.");
            return null;
        }

        var uuid = member.getAsString();
        var record = records.find(uuid).orElse(null);
        if (record == null)
        {
            input.fail(UUID_MEMBER, ErrorCode.REFERENCE_NOT_FOUND, "No transaction with UUID " + uuid + " in the file.");
            return null;
        }

        for (var side : record.sides())
        {
            var earlier = claims.claim(side);
            if (earlier.isPresent())
            {
                input.fail(UUID_MEMBER, ErrorCode.DUPLICATE_TARGET, "Operation " + earlier.getAsInt() + " already changes "
                                + side + "; one batch may change an entity only once.");
                return null;
            }
        }
        return new Existing(record, new TransactionRecords.Join(client).withOwners(record));
    }

    /** A side whose partner is gone: the record can only be deleted. */
    private static boolean isIncomplete(TransactionRecord record)
    {
        return record.sides().size() == 1 && record.primary().getCrossEntry() != null;
    }

    private static void copy(Transaction from, Transaction to)
    {
        to.setDateTime(from.getDateTime());
        to.setSecurity(from.getSecurity());
        to.setShares(from.getShares());
        to.setCurrencyCode(from.getCurrencyCode());
        to.setAmount(from.getAmount());
        to.setNote(from.getNote());
        to.setSource(from.getSource());
        if (from instanceof AccountTransaction source && to instanceof AccountTransaction target)
            target.setExDate(source.getExDate());

        to.clearUnits();
        from.getUnits().forEach(to::addUnit);
    }

    /** Moves the sides that are in another account now, and tells the cross entry where its sides are. */
    private static void move(List<BuiltRecord.Side> existing, List<BuiltRecord.Side> built)
    {
        for (var i = 0; i < existing.size(); i++)
        {
            var side = existing.get(i);
            var target = built.get(i).owner();
            if (side.owner() == target)
                continue;

            side.owner().getTransactions().remove(side.transaction());
            attach(target, side.transaction());
        }

        var entry = existing.get(0).transaction().getCrossEntry();
        if (entry instanceof BuySellEntry buySell)
        {
            buySell.setPortfolio((Portfolio) built.get(0).owner());
            buySell.setAccount((Account) built.get(1).owner());
        }
        else if (entry instanceof PortfolioTransferEntry transfer)
        {
            transfer.setSourcePortfolio((Portfolio) built.get(0).owner());
            transfer.setTargetPortfolio((Portfolio) built.get(1).owner());
        }
        else if (entry instanceof AccountTransferEntry transfer)
        {
            transfer.setSourceAccount((Account) built.get(0).owner());
            transfer.setTargetAccount((Account) built.get(1).owner());
        }
    }

    private static void attach(TransactionOwner<? extends Transaction> owner, Transaction transaction)
    {
        if (owner instanceof Account account)
            account.addTransaction((AccountTransaction) transaction);
        else
            ((Portfolio) owner).addTransaction((PortfolioTransaction) transaction);
    }

    private void detach(BuiltRecord.Side side)
    {
        if (side.owner() instanceof Account account)
            account.shallowDeleteTransaction((AccountTransaction) side.transaction(), client);
        else
            ((Portfolio) side.owner()).shallowDeleteTransaction((PortfolioTransaction) side.transaction(), client);
    }
}
