package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 * <code>transaction.update</code> and <code>transaction.delete</code>, both
 * addressing a whole record by the UUID of either side. An update runs the add
 * path on the patch overlaid on the record, then copies the result onto the
 * existing sides, because core can't give a transaction a UUID.
 */
final class TransactionUpdates
{
    static final String UUID_MEMBER = "uuid";

    private static final Gson GSON = new Gson();

    static final OperationSpec DELETE = new OperationSpec("transaction.delete",
                    "Deletes a whole transaction record, both sides, found by the UUID of either of its sides. The response is one deleted change whose uuid is the record id, with the record as it was in before. A delete that would make an investment account's holding of an instrument negative at the end of any date, by removing a buy or a delivery or transfer into it, is refused.",
                    Schema.object("The record to delete.")
                                    .property(ApplyBatch.OPERATION_MEMBER,
                                                    Schema.enumOf("Operation name.", List.of("transaction.delete")))
                                    .property(UUID_MEMBER, Schema.string("UUID of either side of the record.")),
                    TransactionUpdates::delete);

    /** The record that an update or delete addresses: its sides, primary first, with their owners. */
    record Existing(TransactionRecord record, List<BuiltRecord.Side> sides)
    {
        List<OversellGuard.Effect> effects()
        {
            return OversellGuard.effectsOf(sides);
        }
    }

    private TransactionUpdates()
    {
    }

    static void update(OperationContext context)
    {
        var existing = find(context);
        if (existing == null)
            return;

        var merged = overlay(context, existing.record());
        TransactionOperations.add(context.replacing(merged, existing));
    }

    private static void delete(OperationContext context)
    {
        var existing = find(context);
        if (existing == null)
            return;

        OversellGuard.check(context, UUID_MEMBER, existing.effects(), List.of());
        if (context.failed())
            return;

        var before = TransactionsCommand.render(existing.record(), context.client());
        existing.sides().forEach(side -> detach(context.client(), side));
        context.deleted(new OperationContext.Change("transaction", existing.record().id(), before, Map.of()));
    }

    /** The record with a side of the given UUID, claimed for this operation; null after reporting why not. */
    private static Existing find(OperationContext context)
    {
        var member = context.input().json().get(UUID_MEMBER);
        if (member == null || member.isJsonNull())
        {
            context.fail(UUID_MEMBER, "required", "This operation needs the UUID of a side of the transaction.");
            return null;
        }
        if (!OperationContext.isText(member))
        {
            context.fail(UUID_MEMBER, "invalid-type", "The UUID must be a string.");
            return null;
        }

        var record = context.input().find(UUID_MEMBER, member.getAsString(), EntityKind.TRANSACTION_RECORD);
        if (record == null)
            return null;

        var client = context.client();
        for (var side : record.sides())
        {
            if (!context.claim(UUID_MEMBER, side))
                return null;
        }
        return new Existing(record, record.sides().stream()
                        .map(side -> TransactionRecord.sideOf(client, side).orElseThrow()).toList());
    }

    /** The current record in the shape of an add, with the members of the patch laid over it. */
    private static JsonObject overlay(OperationContext context, TransactionRecord record)
    {
        var merged = new JsonObject();
        var rendered = GSON.toJsonTree(TransactionsCommand.render(record, context.client())).getAsJsonObject();
        for (var member : rendered.entrySet())
        {
            var value = member.getValue();
            // an empty list of units is the same as none, and a transfer refuses any
            if (TransactionShape.isInput(member.getKey())
                            && !(value.isJsonArray() && value.getAsJsonArray().isEmpty()))
                merged.add(member.getKey(), value);
        }

        for (var member : context.input().json().entrySet())
        {
            var name = member.getKey();
            if (name.equals(ApplyBatch.OPERATION_MEMBER) || name.equals(UUID_MEMBER))
                continue;

            if (name.equals("type") && differs(member.getValue(), record.type()))
                context.fail("type", "type-immutable",
                                "The type of a record can't change; delete it and add a new one.");
            else if (name.equals("kind") && record.kind().isPresent()
                            && differs(member.getValue(), record.kind().get().id()))
                context.fail("kind", "kind-immutable",
                                "The kind of a transfer can't change; delete it and add a new one.");
            else
                merged.add(name, member.getValue());
        }
        return merged;
    }

    private static boolean differs(JsonElement value, String current)
    {
        return !(OperationContext.isText(value) && current.equals(value.getAsString()));
    }

    /**
     * Copies the checked record onto the existing one and reports what
     * changed. Nothing is touched if the record has lost a side that the new
     * one has.
     */
    static void replace(OperationContext context, Existing existing, BuiltRecord built)
    {
        var sides = existing.sides();
        if (sides.size() != built.sides().size())
        {
            context.fail(UUID_MEMBER, "record-incomplete",
                            "The record is missing a side, so it can't be updated; delete it and add it again.");
            return;
        }

        var client = context.client();
        var before = TransactionsCommand.render(existing.record(), client);
        var updatedAt = sides.stream().map(side -> side.transaction().getUpdatedAt()).toList();

        for (var i = 0; i < sides.size(); i++)
            copy(built.sides().get(i).transaction(), sides.get(i).transaction());
        move(sides, built.sides());

        var id = existing.record().id();
        var after = TransactionsCommand.render(TransactionRecord.allOf(client).stream()
                        .filter(record -> record.id().equals(id)).findFirst().orElseThrow(), client);

        var changedBefore = new LinkedHashMap<String, Object>();
        var changedAfter = new LinkedHashMap<String, Object>();
        var members = new LinkedHashSet<>(before.keySet());
        members.addAll(after.keySet());
        members.remove("updatedAt"); // set by every write, so never what the patch changed
        for (var member : members)
        {
            if (!sameJson(before.get(member), after.get(member)))
            {
                changedBefore.put(member, before.get(member));
                changedAfter.put(member, after.get(member));
            }
        }

        if (changedAfter.isEmpty())
        {
            for (var i = 0; i < sides.size(); i++)
                sides.get(i).transaction().setUpdatedAt(updatedAt.get(i));
            return;
        }

        context.updated(new OperationContext.Change("transaction", id, changedBefore, changedAfter));
    }

    /** Values are compared as the JSON they are rendered to, as the numbers in them don't share a type. */
    private static boolean sameJson(Object a, Object b)
    {
        return a == null ? b == null : b != null && a.toString().equals(b.toString());
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

    private static void detach(Client client, BuiltRecord.Side side)
    {
        if (side.owner() instanceof Account account)
            account.shallowDeleteTransaction((AccountTransaction) side.transaction(), client);
        else
            ((Portfolio) side.owner()).shallowDeleteTransaction((PortfolioTransaction) side.transaction(), client);
    }
}
