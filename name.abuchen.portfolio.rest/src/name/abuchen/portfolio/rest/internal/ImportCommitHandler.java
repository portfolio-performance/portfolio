package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.rest.spi.OpenFile;

/**
 * The preview and the commit of an import (PDF or CSV). The preview checks
 * the extracted items and keeps them in the {@link ImportSessionRegistry};
 * the commit checks them again with the targets it names and inserts the
 * selected ones, as the import wizard's finish does ({@code ImportController}).
 */
public final class ImportCommitHandler
{
    private ImportCommitHandler()
    {
    }

    /**
     * Parses and validates the {@code targets} of an extraction request before
     * the extraction runs; 422 on unknown references. Must be called on the
     * UI thread.
     */
    public static void validateTargets(Client client, JsonElement targets)
    {
        var json = new Json(new JsonObject());
        ImportContext.of(client, targets, json);
        json.throwIfErrors();
    }

    /**
     * Keeps the extracted items as a new import session and answers the
     * preview: every item with its checks, run with the given targets (or
     * their defaults). Must be called on the UI thread.
     */
    public static JsonObject preview(OpenFile file, ImportSessionRegistry sessions, String kind,
                    List<Extractor.Item> items, JsonElement targets, JsonArray errors)
    {
        var client = file.getClient();
        var json = new Json(new JsonObject());
        var context = ImportContext.of(client, targets, json);
        json.throwIfErrors();

        var session = sessions.create(file.getPath(), kind, items);
        var checked = ImportPreview.check(client, session.items(), context);
        return ImportPreview.toJson(client, session, checked, errors);
    }

    /**
     * Inserts the selected items of the import into the file. Body
     * {@code {select?: [index], targets?: {...}, options?: {convertBuySellToDelivery,
     * removeDividends, importNotes}}}. Without {@code select}, the items whose
     * checks are all {@code ok} are imported (and investment plan items with
     * a warning), as the wizard preselects them; {@code select} may name
     * items with warnings, but not items with errors
     * ({@code item-not-importable}). A dry run answers what would be imported
     * and keeps the session; a commit consumes it. Must be called on the UI
     * thread.
     */
    public static JsonObject commit(WriteContext context, ImportSessionRegistry sessions, String importId,
                    JsonObject body)
    {
        var client = context.client();
        var session = sessions.find(context.file().getPath(), importId).orElseThrow(ApiException::notFound);

        var json = new Json(body);
        json.ignore("select", "targets", "options"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        var targets = ImportContext.of(client, body.get("targets"), json); //$NON-NLS-1$
        var options = Options.parse(body.get("options"), json); //$NON-NLS-1$
        json.rejectUnknownFields();
        json.throwIfErrors();

        var checked = ImportPreview.check(client, session.items(), targets);
        var selected = select(body.get("select"), checked, json); //$NON-NLS-1$
        json.throwIfErrors();

        if (context.dryRun())
        {
            var result = new JsonObject();
            result.addProperty("dryRun", true); //$NON-NLS-1$
            result.addProperty("importId", session.id()); //$NON-NLS-1$
            var items = new JsonArray();
            selected.forEach(index -> items.add(ImportPreview.toJson(client, checked.get(index))));
            result.add("items", items); //$NON-NLS-1$
            result.addProperty("count", selected.size()); //$NON-NLS-1$
            return result;
        }

        var before = legs(client);
        var securitiesBefore = Collections.newSetFromMap(new IdentityHashMap<Security, Boolean>());
        securitiesBefore.addAll(client.getSecurities());

        var action = new InsertAction(client);
        action.setConvertBuySellToDelivery(options.convertBuySellToDelivery());
        action.setRemoveDividends(options.removeDividends());

        // instruments first, so that transactions find them in the file
        var order = new ArrayList<Integer>();
        selected.stream().filter(i -> checked.get(i).item() instanceof Extractor.SecurityItem).forEach(order::add);
        selected.stream().filter(i -> !(checked.get(i).item() instanceof Extractor.SecurityItem)).forEach(order::add);

        var imported = new JsonArray();
        for (var index : order)
        {
            var item = checked.get(index).item();
            if (!options.importNotes())
                item.setNote(null);
            action.setInvestmentPlanItem(item.isInvestmentPlanItem());
            item.apply(action, targets);

            var entry = new JsonObject();
            entry.addProperty("index", index); //$NON-NLS-1$
            entry.addProperty("kind", ImportPreview.kind(item)); //$NON-NLS-1$
            imported.add(entry);
        }

        sessions.remove(session);

        var result = new JsonObject();
        result.addProperty("importId", session.id()); //$NON-NLS-1$
        result.addProperty("count", selected.size()); //$NON-NLS-1$
        result.add("items", imported); //$NON-NLS-1$
        result.add("transactions", newTransactions(client, before)); //$NON-NLS-1$

        var instruments = new JsonArray();
        client.getSecurities().stream().filter(s -> !securitiesBefore.contains(s)).forEach(s -> {
            var instrument = new JsonObject();
            instrument.addProperty("uuid", s.getUUID()); //$NON-NLS-1$
            instrument.addProperty("name", s.getName()); //$NON-NLS-1$
            instruments.add(instrument);
        });
        result.add("instruments", instruments); //$NON-NLS-1$

        if (!selected.isEmpty())
        {
            client.markDirty();
            ChangeLog.recordEvent(Messages.MsgApiImportCommitted, selected.size(), session.kind(),
                            context.file().getLabel());
        }

        return result;
    }

    /** the options of a commit, as the wizard's check boxes */
    private record Options(boolean convertBuySellToDelivery, boolean removeDividends, boolean importNotes)
    {
        static Options parse(JsonElement element, Json json)
        {
            if (element == null || element.isJsonNull())
                return new Options(false, false, true);
            if (!element.isJsonObject())
            {
                json.add(new ApiException.FieldError("options", "invalid-type", "options must be an object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                return new Options(false, false, true);
            }

            var options = new Json(element.getAsJsonObject());
            var convert = options.bool("convertBuySellToDelivery"); //$NON-NLS-1$
            var remove = options.bool("removeDividends"); //$NON-NLS-1$
            var notes = options.bool("importNotes"); //$NON-NLS-1$
            options.rejectUnknownFields();
            options.errors().forEach(e -> json.add(new ApiException.FieldError("options." + e.field(), e.code(), //$NON-NLS-1$
                            e.message())));

            return new Options(convert != null && convert, remove != null && remove, notes == null || notes);
        }
    }

    /** the indices to import, in ascending order */
    private static List<Integer> select(JsonElement element, List<ImportPreview.Checked> checked, Json json)
    {
        var selected = new TreeSet<Integer>();

        if (element == null || element.isJsonNull())
        {
            for (var entry : checked)
            {
                var worst = entry.worst();
                if (worst == Code.OK || worst == Code.WARNING && entry.item().isInvestmentPlanItem())
                    selected.add(entry.index());
            }
            return new ArrayList<>(selected);
        }

        if (!element.isJsonArray())
        {
            json.add(new ApiException.FieldError("select", "invalid-type", "select must be an array of item indices")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return List.of();
        }

        var array = element.getAsJsonArray();
        for (var ii = 0; ii < array.size(); ii++)
        {
            var field = "select[" + ii + "]"; //$NON-NLS-1$ //$NON-NLS-2$
            var value = array.get(ii);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                            || !value.getAsString().matches("\\d+")) //$NON-NLS-1$
            {
                json.add(new ApiException.FieldError(field, "invalid-type", "an item index must be an integer")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            var index = value.getAsInt();
            if (index >= checked.size())
            {
                json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                                MessageFormat.format("the import has no item {0}", index))); //$NON-NLS-1$
                continue;
            }

            var entry = checked.get(index);
            if (entry.worst() == Code.ERROR)
            {
                var reason = entry.checks().stream().filter(c -> c.status() == Code.ERROR || c.status() == Code.SKIP)
                                .map(c -> c.code() + (c.message() != null ? ": " + c.message() : "")) //$NON-NLS-1$ //$NON-NLS-2$
                                .findFirst().orElse(""); //$NON-NLS-1$
                json.add(new ApiException.FieldError(field, "item-not-importable", //$NON-NLS-1$
                                MessageFormat.format("item {0} cannot be imported ({1})", index, reason))); //$NON-NLS-1$
                continue;
            }

            selected.add(index);
        }

        return new ArrayList<>(selected);
    }

    /** every transaction leg of the file */
    private static Set<Transaction> legs(Client client)
    {
        var legs = Collections.newSetFromMap(new IdentityHashMap<Transaction, Boolean>());
        client.getAccounts().forEach(a -> legs.addAll(a.getTransactions()));
        client.getPortfolios().forEach(p -> legs.addAll(p.getTransactions()));
        return legs;
    }

    /** the transactions the import added, one per buy/sell or transfer, oldest first */
    private static JsonArray newTransactions(Client client, Set<Transaction> before)
    {
        var added = new LinkedHashMap<Transaction, TransactionPair<?>>();
        client.getAccounts().forEach(a -> a.getTransactions().stream().filter(t -> !before.contains(t))
                        .forEach(t -> add(added, TransactionsHandler.pair(a, t))));
        client.getPortfolios().forEach(p -> p.getTransactions().stream().filter(t -> !before.contains(t))
                        .forEach(t -> add(added, TransactionsHandler.pair(p, t))));

        var pairs = new ArrayList<>(added.values());
        pairs.sort(TransactionPair.BY_DATE);

        var array = new JsonArray();
        pairs.forEach(pair -> array.add(EntityJson.toJsonDetailed(pair)));
        return array;
    }

    private static void add(LinkedHashMap<Transaction, TransactionPair<?>> added, TransactionPair<?> pair)
    {
        var canonical = TransactionsHandler.canonical(pair);
        added.putIfAbsent(canonical.getTransaction(), canonical);
    }
}
