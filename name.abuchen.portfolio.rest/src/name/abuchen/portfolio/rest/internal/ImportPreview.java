package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.datatransfer.SecurityUpdate;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/**
 * Checks extracted import items as the application's import wizard does
 * ({@code ReviewExtractedItemsPage#checkEntries}) and renders them for the
 * preview. Each item reports every check with its status; the item's
 * {@code status} is the worst of them: {@code ok}, {@code warning} (e.g. a
 * probable duplicate; imported only when selected explicitly) or
 * {@code error} (not importable). Must be called on the UI thread.
 */
public final class ImportPreview
{
    /** the outcome of one check of an item */
    public record Check(String code, Code status, String message)
    {
    }

    /** an item with the outcome of its checks */
    public record Checked(int index, Extractor.Item item, List<Check> checks, Described described)
    {
        /** the worst status of all checks; a skipped item counts as error */
        public Code worst()
        {
            var worst = Code.OK;
            for (var check : checks)
            {
                var code = check.status() == Code.SKIP ? Code.ERROR : check.status();
                if (code.isHigherSeverityAs(worst))
                    worst = code;
            }
            return worst;
        }
    }

    /** where an item would be booked, as resolved from the item and the targets */
    public static final class Described implements ImportAction
    {
        private Object owner;
        private Object secondaryOwner;
        private SecurityPrice price;

        @Override
        public Status process(AccountTransaction transaction, Account account)
        {
            owner = account;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(PortfolioTransaction transaction, Portfolio portfolio)
        {
            owner = portfolio;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
        {
            owner = portfolio;
            secondaryOwner = account;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(AccountTransferEntry entry, Account source, Account target)
        {
            owner = source;
            secondaryOwner = target;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
        {
            owner = source;
            secondaryOwner = target;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(Security security, SecurityPrice price)
        {
            this.price = price;
            return Status.OK_STATUS;
        }

        @Override
        public Status process(Security security, List<SecurityUpdate> updates)
        {
            return Status.OK_STATUS;
        }
    }

    private ImportPreview()
    {
    }

    /** a check of the wizard and the code the API reports it by */
    private record NamedAction(String code, ImportAction action)
    {
    }

    /** the wizard's checks, in its order */
    private static List<NamedAction> actions(Client client)
    {
        return List.of(new NamedAction("transaction-date", new CheckTransactionDateAction()), //$NON-NLS-1$
                        new NamedAction("valid-types", new CheckValidTypesAction()), //$NON-NLS-1$
                        new NamedAction("security-values", new CheckSecurityRelatedValuesAction()), //$NON-NLS-1$
                        new NamedAction("duplicates", new DetectDuplicatesAction(client)), //$NON-NLS-1$
                        new NamedAction("currencies", new CheckCurrenciesAction()), //$NON-NLS-1$
                        new NamedAction("forex-gross-value", new CheckForexGrossValueAction())); //$NON-NLS-1$
    }

    private static List<Check> runChecks(List<NamedAction> actions, Extractor.Item item, ImportAction.Context context)
    {
        var checks = new ArrayList<Check>();
        for (var action : actions)
        {
            try
            {
                var status = item.apply(action.action(), context);
                checks.add(new Check(action.code(), status.getCode(), status.getMessage()));
            }
            catch (Exception e)
            {
                // as the wizard: an unexpected failure marks the item as not importable
                checks.add(new Check(action.code(), Code.ERROR, e.getMessage()));
                PortfolioLog.error(e);
            }
        }
        return checks;
    }

    /** checks every item with the given targets */
    public static List<Checked> check(Client client, List<Extractor.Item> items, ImportAction.Context context)
    {
        var actions = actions(client);
        var result = new ArrayList<Checked>();
        for (var index = 0; index < items.size(); index++)
        {
            var item = items.get(index);
            var checks = new ArrayList<Check>();
            var described = new Described();

            if (item.isFailure())
            {
                checks.add(new Check("extraction", Code.ERROR, item.getFailureMessage())); //$NON-NLS-1$
            }
            else if (item.isSkipped())
            {
                var reason = item instanceof Extractor.SkippedItem skipped ? skipped.getSkipReason() : null;
                checks.add(new Check("skipped", Code.SKIP, reason)); //$NON-NLS-1$
            }
            else
            {
                // resolves the accounts; an item without a suitable account
                // fails every check the same way, so report it once
                var targets = item.apply(described, context);
                if (targets.getCode() == Code.ERROR)
                    checks.add(new Check("targets", Code.ERROR, targets.getMessage())); //$NON-NLS-1$
                else
                    checks.addAll(runChecks(actions, item, context));
            }

            result.add(new Checked(index, item, checks, described));
        }
        return result;
    }

    /** the kind of an extracted item */
    public static String kind(Extractor.Item item)
    {
        if (item.isSkipped())
            return "skipped"; //$NON-NLS-1$
        if (item instanceof Extractor.BuySellEntryItem)
            return "buy-sell"; //$NON-NLS-1$
        if (item instanceof Extractor.AccountTransferItem)
            return TransactionTypes.CASH_TRANSFER;
        if (item instanceof Extractor.PortfolioTransferItem)
            return TransactionTypes.SECURITY_TRANSFER;
        if (item instanceof Extractor.TransactionItem)
            return "transaction"; //$NON-NLS-1$
        if (item instanceof Extractor.SecurityItem)
            return "instrument"; //$NON-NLS-1$
        if (item instanceof Extractor.SecurityUpdateItem)
            return "instrument-update"; //$NON-NLS-1$
        if (item instanceof Extractor.SecurityPriceItem)
            return "price"; //$NON-NLS-1$
        return "other"; //$NON-NLS-1$
    }

    /** the wire name of a check status */
    public static String wireStatus(Code code)
    {
        return switch (code)
        {
            case OK -> "ok"; //$NON-NLS-1$
            case WARNING -> "warning"; //$NON-NLS-1$
            case ERROR, SKIP -> "error"; //$NON-NLS-1$
        };
    }

    /** one item of the preview: index, kind, status, checks and what it would book */
    public static JsonObject toJson(Client client, Checked checked)
    {
        var item = checked.item();

        var json = new JsonObject();
        json.addProperty("index", checked.index()); //$NON-NLS-1$
        json.addProperty("kind", kind(item)); //$NON-NLS-1$
        json.addProperty("status", wireStatus(checked.worst())); //$NON-NLS-1$

        var checks = new JsonArray();
        for (var check : checked.checks())
        {
            var c = new JsonObject();
            c.addProperty("code", check.code()); //$NON-NLS-1$
            c.addProperty("status", wireStatus(check.status())); //$NON-NLS-1$
            if (check.message() != null)
                c.addProperty("message", check.message()); //$NON-NLS-1$
            checks.add(c);
        }
        json.add("checks", checks); //$NON-NLS-1$

        if (item.isInvestmentPlanItem())
            json.addProperty("investmentPlanItem", true); //$NON-NLS-1$

        addEntity(json, client, item, checked.described());
        return json;
    }

    private static void addEntity(JsonObject json, Client client, Extractor.Item item, Described described)
    {
        if (item.isSkipped())
        {
            if (item.getDate() != null)
                json.addProperty("date", item.getDate().toString()); //$NON-NLS-1$
            json.addProperty("description", item.getTypeInformation()); //$NON-NLS-1$
            return;
        }

        var subject = item.getSubject();
        if (subject instanceof BuySellEntry entry)
        {
            json.add("transaction", EntityJson.toJsonUnbooked(entry.getPortfolioTransaction())); //$NON-NLS-1$
            addReference(json, "investmentAccount", described.owner); //$NON-NLS-1$
            addReference(json, "cashAccount", described.secondaryOwner); //$NON-NLS-1$
        }
        else if (subject instanceof AccountTransferEntry entry)
        {
            json.add("transaction", EntityJson.toJsonUnbooked(entry.getSourceTransaction())); //$NON-NLS-1$
            addReference(json, "fromCashAccount", described.owner); //$NON-NLS-1$
            addReference(json, "toCashAccount", described.secondaryOwner); //$NON-NLS-1$
        }
        else if (subject instanceof PortfolioTransferEntry entry)
        {
            json.add("transaction", EntityJson.toJsonUnbooked(entry.getSourceTransaction())); //$NON-NLS-1$
            addReference(json, "fromInvestmentAccount", described.owner); //$NON-NLS-1$
            addReference(json, "toInvestmentAccount", described.secondaryOwner); //$NON-NLS-1$
        }
        else if (subject instanceof AccountTransaction transaction)
        {
            json.add("transaction", EntityJson.toJsonUnbooked(transaction)); //$NON-NLS-1$
            addReference(json, "cashAccount", described.owner); //$NON-NLS-1$
        }
        else if (subject instanceof PortfolioTransaction transaction)
        {
            json.add("transaction", EntityJson.toJsonUnbooked(transaction)); //$NON-NLS-1$
            addReference(json, "investmentAccount", described.owner); //$NON-NLS-1$
        }
        else if (item.getSecurity() != null)
        {
            json.add("instrument", instrument(client, item.getSecurity())); //$NON-NLS-1$
            if (described.price != null)
            {
                var price = new JsonObject();
                price.addProperty("date", described.price.getDate().toString()); //$NON-NLS-1$
                price.add("value", EntityJson.decimal(described.price.getValue(), Values.Quote.precision())); //$NON-NLS-1$
                json.add("price", price); //$NON-NLS-1$
            }
        }
    }

    private static void addReference(JsonObject json, String field, Object owner)
    {
        var reference = ImportContext.reference(owner);
        if (reference != null)
            json.add(field, reference);
    }

    private static JsonObject instrument(Client client, Security security)
    {
        var json = new JsonObject();
        json.addProperty("uuid", security.getUUID()); //$NON-NLS-1$
        json.addProperty("name", security.getName()); //$NON-NLS-1$
        if (security.getCurrencyCode() != null)
            json.addProperty("currencyCode", security.getCurrencyCode()); //$NON-NLS-1$
        if (security.getIsin() != null)
            json.addProperty("isin", security.getIsin()); //$NON-NLS-1$
        if (security.getWkn() != null)
            json.addProperty("wkn", security.getWkn()); //$NON-NLS-1$
        if (security.getTickerSymbol() != null)
            json.addProperty("tickerSymbol", security.getTickerSymbol()); //$NON-NLS-1$
        // an instrument the import would create
        json.addProperty("new", !client.getSecurities().contains(security)); //$NON-NLS-1$
        return json;
    }

    /**
     * The whole preview: the import id, when it expires, every item, the
     * number of items per status, and the files that could not be read.
     */
    public static JsonObject toJson(Client client, ImportSessionRegistry.Session session, List<Checked> checked,
                    JsonArray errors)
    {
        var json = new JsonObject();
        json.addProperty("importId", session.id()); //$NON-NLS-1$
        json.addProperty("kind", session.kind()); //$NON-NLS-1$
        json.addProperty("expiresAt", session.expiresAt().toString()); //$NON-NLS-1$

        var items = new JsonArray();
        int ok = 0;
        int warning = 0;
        int error = 0;
        for (var entry : checked)
        {
            items.add(toJson(client, entry));
            switch (entry.worst())
            {
                case OK -> ok++;
                case WARNING -> warning++;
                default -> error++;
            }
        }
        json.add("items", items); //$NON-NLS-1$

        var counts = new JsonObject();
        counts.addProperty("ok", ok); //$NON-NLS-1$
        counts.addProperty("warning", warning); //$NON-NLS-1$
        counts.addProperty("error", error); //$NON-NLS-1$
        json.add("counts", counts); //$NON-NLS-1$

        json.add("errors", errors == null ? new JsonArray() : errors); //$NON-NLS-1$
        return json;
    }
}
