package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.Messages;

/**
 * Investment plans. The model gives a plan no identifier, so the API
 * addresses it by its name, which it keeps unique: a create or rename to a
 * name that is taken is refused.
 * <p/>
 * The fields and rules follow the application's plan dialog: a
 * {@code purchase} plan buys an instrument into an investment account, paid
 * from a cash account (or, without one, booked as a delivery); a
 * {@code deposit}, {@code removal} or {@code interest} plan books on a cash
 * account only. {@code amount} is the total the plan books: for a purchase
 * the gross value plus fees and taxes, for interest the net amount after
 * taxes. The kind of a plan cannot be changed.
 */
public final class InvestmentPlansHandler
{
    private static final String KIND = "investment-plan"; //$NON-NLS-1$
    private static final int MAX_INTERVAL = InvestmentPlan.WEEKS_THRESHOLD - 1;

    private InvestmentPlansHandler()
    {
    }

    /** the wire name of a plan type */
    public static String wireKind(InvestmentPlan.Type type)
    {
        if (type == null)
            return "purchase"; //$NON-NLS-1$

        return switch (type)
        {
            case PURCHASE_OR_DELIVERY -> "purchase"; //$NON-NLS-1$
            case DEPOSIT -> "deposit"; //$NON-NLS-1$
            case REMOVAL -> "removal"; //$NON-NLS-1$
            case INTEREST -> "interest"; //$NON-NLS-1$
        };
    }

    private static InvestmentPlan.Type parseKind(String kind)
    {
        for (var type : InvestmentPlan.Type.values())
        {
            if (wireKind(type).equals(kind))
                return type;
        }
        return null;
    }

    private static boolean isPurchase(InvestmentPlan plan)
    {
        return plan.getPlanType() == null || plan.getPlanType() == InvestmentPlan.Type.PURCHASE_OR_DELIVERY;
    }

    /**
     * The currency of the plan's amounts: that of its cash account or, for a
     * delivery plan, of the investment account's reference account.
     */
    /* package */ static String currencyOf(Client client, InvestmentPlan plan)
    {
        if (plan.getAccount() != null)
            return plan.getAccount().getCurrencyCode();
        if (plan.getPortfolio() != null && plan.getPortfolio().getReferenceAccount() != null)
            return plan.getPortfolio().getReferenceAccount().getCurrencyCode();
        return client.getBaseCurrency();
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getPlans(), plan -> EntityJson.toJson(client, plan));
    }

    public static JsonElement get(Client client, String name)
    {
        return EntityJson.toJson(client, find(client, name));
    }

    /**
     * Creates an investment plan; {@code name}, {@code kind} and
     * {@code amount} are required, {@code start} defaults to today and the
     * interval to one month, as in the application's dialog. With a
     * {@code clientRef} that an earlier create used, answers that plan
     * ({@code replayed: true}). Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();

        var existing = idempotency.findObject(context.file().getPath(), KIND, context.clientRef())
                        .filter(p -> client.getPlans().stream().anyMatch(other -> other == p))
                        .map(InvestmentPlan.class::cast);
        if (existing.isPresent())
            return MasterDataWrites.replayed(EntityJson.toJson(client, existing.get()), context.dryRun());

        var plan = new InvestmentPlan();
        plan.setStart(LocalDate.now());

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        for (var field : List.of("name", "kind", "amount")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            if (!json.has(field))
                json.add(new ApiException.FieldError(field, "required", MessageFormat.format("{0} is required", field))); //$NON-NLS-1$ //$NON-NLS-2$
        }
        var setters = stage(client, null, json);
        json.throwIfErrors();

        setters.forEach(setter -> setter.accept(plan));
        validate(client, null, plan);

        if (context.dryRun())
            return MasterDataWrites.dryRun(EntityJson.toJson(client, plan));

        client.addPlan(plan);
        client.markDirty();
        idempotency.rememberObject(context.file().getPath(), KIND, context.clientRef(), plan);

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND, plan.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(client, plan), true);
    }

    /**
     * Applies a JSON Merge Patch; the merged plan is validated as a whole. A
     * patch that changes nothing does not mark the file dirty. Must be called
     * on the UI thread.
     */
    public static MasterDataWrites.WriteResult patch(WriteContext context, String name, JsonObject body)
    {
        var client = context.client();
        var plan = find(client, name);

        var json = new Json(body);
        var setters = stage(client, plan, json);
        json.throwIfErrors();

        var preview = copyOf(plan);
        setters.forEach(setter -> setter.accept(preview));
        validate(client, plan, preview);

        var before = EntityJson.toJson(client, plan);
        var after = EntityJson.toJson(client, preview);

        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var changes = MasterDataWrites.diff(before, after);
        if (changes.isEmpty())
            return new MasterDataWrites.WriteResult(before, false);

        var oldName = plan.getName();
        setters.forEach(setter -> setter.accept(plan));
        client.markDirty();

        ChangeLog.recordChanges(changes, Messages.MsgApiEntityChanged, KIND, oldName, context.file().getLabel());

        return new MasterDataWrites.WriteResult(EntityJson.toJson(client, plan), true);
    }

    /**
     * Parses the fields of the body into setters; violations are recorded in
     * {@code json}. Rules that involve several fields are checked on the
     * merged plan, see {@link #validate}.
     */
    private static List<Consumer<InvestmentPlan>> stage(Client client, InvestmentPlan current, Json json)
    {
        var setters = new ArrayList<Consumer<InvestmentPlan>>();

        if (json.has("name")) //$NON-NLS-1$
        {
            var name = json.requireString("name"); //$NON-NLS-1$
            if (name != null)
                setters.add(p -> p.setName(name));
        }

        if (json.has("kind")) //$NON-NLS-1$
        {
            var kind = json.requireString("kind"); //$NON-NLS-1$
            var type = kind != null ? parseKind(kind) : null;
            if (kind != null && type == null)
                json.add(new ApiException.FieldError("kind", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("kind must be purchase, deposit, removal or interest, got {0}", //$NON-NLS-1$
                                                kind)));
            else if (type != null && current != null && type != current.getPlanType())
                json.add(new ApiException.FieldError("kind", "not-allowed-for-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "the kind of an investment plan cannot be changed")); //$NON-NLS-1$
            else if (type != null)
                setters.add(p -> p.setType(type));
        }

        stageReference(json, "instrument", uuid -> client.getSecurities().stream() //$NON-NLS-1$
                        .filter(s -> s.getUUID().equals(uuid)).findFirst().orElse(null), InvestmentPlan::setSecurity,
                        setters);
        stageReference(json, "investmentAccount", uuid -> client.getPortfolios().stream() //$NON-NLS-1$
                        .filter(p -> p.getUUID().equals(uuid)).findFirst().orElse(null),
                        InvestmentPlan::setPortfolio, setters);
        stageReference(json, "cashAccount", uuid -> client.getAccounts().stream() //$NON-NLS-1$
                        .filter(a -> a.getUUID().equals(uuid)).findFirst().orElse(null), InvestmentPlan::setAccount,
                        setters);

        if (json.has("start")) //$NON-NLS-1$
        {
            var start = json.requireDate("start"); //$NON-NLS-1$
            if (start != null)
                setters.add(p -> p.setStart(start));
        }

        stageInterval(json, setters);

        if (json.has("amount")) //$NON-NLS-1$
        {
            var amount = json.requireDecimal("amount", Values.Amount.precision()); //$NON-NLS-1$
            if (amount != null && amount.signum() <= 0)
                json.add(new ApiException.FieldError("amount", "must-be-positive", "amount must be positive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            else if (amount != null)
                setters.add(p -> p.setAmount(Amounts.toAmount(amount)));
        }

        stageFeeOrTax(json, "fees", InvestmentPlan::setFees, setters); //$NON-NLS-1$
        stageFeeOrTax(json, "taxes", InvestmentPlan::setTaxes, setters); //$NON-NLS-1$

        if (json.has("autoGenerate")) //$NON-NLS-1$
        {
            var autoGenerate = json.bool("autoGenerate"); //$NON-NLS-1$
            setters.add(p -> p.setAutoGenerate(Boolean.TRUE.equals(autoGenerate)));
        }

        if (json.has("note")) //$NON-NLS-1$
        {
            var note = json.optString("note"); //$NON-NLS-1$
            setters.add(p -> p.setNote(note));
        }

        json.rejectUnknownFields();
        return setters;
    }

    /** a reference by UUID; {@code null} clears it */
    private static <T> void stageReference(Json json, String field, Function<String, T> lookup,
                    BiConsumer<InvestmentPlan, T> setter, List<Consumer<InvestmentPlan>> setters)
    {
        if (!json.has(field))
            return;

        if (json.isNull(field))
        {
            setters.add(p -> setter.accept(p, null));
            return;
        }

        var uuid = json.optString(field);
        if (uuid == null)
            return;

        var entity = lookup.apply(uuid);
        if (entity == null)
            json.add(new ApiException.FieldError(field, "unknown-reference", //$NON-NLS-1$
                            MessageFormat.format("no {0} with uuid {1}", field, uuid))); //$NON-NLS-1$
        else
            setters.add(p -> setter.accept(p, entity));
    }

    /**
     * {@code intervalMonths} (1-99) or {@code intervalWeeks} (1-99), mapped
     * to the model's encoding of weekly intervals as 100 + weeks.
     */
    private static void stageInterval(Json json, List<Consumer<InvestmentPlan>> setters)
    {
        var months = json.has("intervalMonths") ? interval(json, "intervalMonths") : null; //$NON-NLS-1$ //$NON-NLS-2$
        var weeks = json.has("intervalWeeks") ? interval(json, "intervalWeeks") : null; //$NON-NLS-1$ //$NON-NLS-2$

        if (months != null && weeks != null)
            json.add(new ApiException.FieldError("intervalWeeks", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "give either intervalMonths or intervalWeeks, not both")); //$NON-NLS-1$
        else if (months != null)
            setters.add(p -> p.setInterval(months));
        else if (weeks != null)
            setters.add(p -> p.setInterval(InvestmentPlan.WEEKS_THRESHOLD + weeks));
    }

    private static Integer interval(Json json, String field)
    {
        var value = json.requireDecimal(field, 0);
        if (value == null)
            return null;

        if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(MAX_INTERVAL)) > 0)
        {
            json.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} must be between 1 and {1}", field, MAX_INTERVAL))); //$NON-NLS-1$
            return null;
        }
        return value.intValueExact();
    }

    /** fees or taxes: not negative; {@code null} means zero */
    private static void stageFeeOrTax(Json json, String field, ObjLongConsumer<InvestmentPlan> setter,
                    List<Consumer<InvestmentPlan>> setters)
    {
        if (!json.has(field))
            return;

        var errorCount = json.errors().size();
        var value = json.decimal(field, Values.Amount.precision());
        if (json.errors().size() > errorCount)
            return;

        if (value != null && value.signum() < 0)
        {
            json.add(new ApiException.FieldError(field, "must-be-positive", field + " must not be negative")); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        var amount = value != null ? Amounts.toAmount(value) : 0L;
        setters.add(p -> setter.accept(p, amount));
    }

    /**
     * The rules of the application's plan dialog, checked on the merged plan:
     * the accounts and instrument a kind needs, and a total that is
     * consistent with fees and taxes.
     */
    private static void validate(Client client, InvestmentPlan current, InvestmentPlan plan)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        if (client.getPlans().stream().anyMatch(p -> p != current && p.getName() != null
                        && p.getName().equals(plan.getName())))
            errors.add(new ApiException.FieldError("name", "already-exists", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("an investment plan named {0} already exists", plan.getName()))); //$NON-NLS-1$

        if (isPurchase(plan))
        {
            if (plan.getSecurity() == null)
                errors.add(new ApiException.FieldError("instrument", "required", "a purchase plan needs an instrument")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (plan.getPortfolio() == null)
                errors.add(new ApiException.FieldError("investmentAccount", "required", //$NON-NLS-1$ //$NON-NLS-2$
                                "a purchase plan needs an investment account")); //$NON-NLS-1$
            if (plan.getAmount() - plan.getFees() - plan.getTaxes() <= 0)
                errors.add(new ApiException.FieldError("amount", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "the amount of a purchase plan must exceed its fees and taxes")); //$NON-NLS-1$
        }
        else
        {
            if (plan.getAccount() == null)
                errors.add(new ApiException.FieldError("cashAccount", "required", //$NON-NLS-1$ //$NON-NLS-2$
                                "this kind of plan needs a cash account")); //$NON-NLS-1$
            if (plan.getSecurity() != null)
                errors.add(new ApiException.FieldError("instrument", "not-allowed-for-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "only a purchase plan has an instrument")); //$NON-NLS-1$
            if (plan.getPortfolio() != null)
                errors.add(new ApiException.FieldError("investmentAccount", "not-allowed-for-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "only a purchase plan has an investment account")); //$NON-NLS-1$
            if (plan.getFees() != 0)
                errors.add(new ApiException.FieldError("fees", "not-allowed-for-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "only a purchase plan has fees")); //$NON-NLS-1$
            if (plan.getTaxes() != 0 && plan.getPlanType() != InvestmentPlan.Type.INTEREST)
                errors.add(new ApiException.FieldError("taxes", "not-allowed-for-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "only a purchase or an interest plan has taxes")); //$NON-NLS-1$
        }

        if (!errors.isEmpty())
            throw ApiException.validation(errors);
    }

    /**
     * Deletes the plan. As in the application, the transactions it generated
     * are kept. A dry run answers the plan that would be removed; a real
     * delete answers null. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String name)
    {
        var client = context.client();
        var plan = find(client, name);

        if (context.dryRun())
            return MasterDataWrites.deletePreview(EntityJson.toJson(client, plan));

        client.removePlan(plan);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND, plan.getName(), context.file().getLabel());
        return null;
    }

    /**
     * The plan with the given name; 404 if there is none, 409
     * {@code ambiguous-name} if the file has several (the application does
     * not enforce unique names).
     */
    /* package */ static InvestmentPlan find(Client client, String name)
    {
        var matches = client.getPlans().stream().filter(p -> name.equals(p.getName())).toList();
        if (matches.isEmpty())
            throw ApiException.notFound();
        if (matches.size() > 1)
            throw ApiException.conflict("ambiguous-name", "Several entities have this name", //$NON-NLS-1$ //$NON-NLS-2$
                            MessageFormat.format("{0} investment plans are named {1}; rename them in the application", //$NON-NLS-1$
                                            matches.size(), name),
                            List.of());
        return matches.get(0);
    }

    /** a detached copy of the plan to preview a patch on; it is never added to the client */
    private static InvestmentPlan copyOf(InvestmentPlan plan)
    {
        var copy = new InvestmentPlan(plan.getName());
        copy.setType(plan.getPlanType());
        copy.setNote(plan.getNote());
        copy.setSecurity(plan.getSecurity());
        copy.setPortfolio(plan.getPortfolio());
        copy.setAccount(plan.getAccount());
        copy.setAutoGenerate(plan.isAutoGenerate());
        copy.setStart(plan.getStart());
        copy.setInterval(plan.getInterval());
        copy.setAmount(plan.getAmount());
        copy.setFees(plan.getFees());
        copy.setTaxes(plan.getTaxes());
        copy.setAttributes(plan.getAttributes().copy());
        copy.getTransactions().addAll(plan.getTransactions());
        return copy;
    }
}
