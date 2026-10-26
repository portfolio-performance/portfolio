package name.abuchen.portfolio.cli.apply;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import name.abuchen.portfolio.cli.BuiltRecord;
import name.abuchen.portfolio.cli.Schema;
import name.abuchen.portfolio.cli.TransactionRecord;
import name.abuchen.portfolio.cli.TransactionShape;
import name.abuchen.portfolio.cli.TransactionsCommand;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * The <code>transaction.add</code> operation. This class takes the cash-account
 * events (deposits, removals, interest, dividends, fees, taxes and their
 * refunds) and dispatches the other types to {@link TradeOperations} and
 * {@link TransferOperations}. All of them read their input through
 * {@link TransactionInput}. The input mirrors the <code>transactions</code>
 * output, so an agent can send back what it read. Every rule reports a field-level failure, and the whole batch stays
 * atomic because nothing is added to the model until the operation is clean.
 */
public final class TransactionOperations
{
    private static final String OP = "transaction.add";
    private static final String UPDATE_OP = "transaction.update";

    private static final String UPDATE_DESCRIPTION = "The fields of a transaction to change, in the shape the transactions output uses. Only the fields present change; null clears an optional field.";

    /** A security is accepted on these; on the others it is refused. */
    private static final Set<Type> SECURITY_ALLOWED = EnumSet.of(Type.DIVIDENDS, Type.TAXES, Type.TAX_REFUND,
                    Type.FEES, Type.FEES_REFUND);

    /** A security is mandatory on these. */
    private static final Set<Type> SECURITY_REQUIRED = EnumSet.of(Type.DIVIDENDS);

    /** Shares are accepted only on these cash types. */
    private static final Set<Type> SHARES_ALLOWED = EnumSet.of(Type.DIVIDENDS);

    /** Core's non-mutating import checks, run on the built transaction as a safety net. */
    private static final List<ImportAction> CORE_CHECKS = List.of(new CheckTransactionDateAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction(), new CheckValidTypesAction());

    static final OperationSpec ADD = new OperationSpec(OP,
                    "Adds a transaction in the shape the transactions output uses. Cash-account events (deposit, removal, interest, dividend, fee, tax, refund) are one-sided. BUY and SELL create an investment-account side and a cash-account side together; TRANSFER creates both sides of a cash or security transfer; DELIVERY_INBOUND and DELIVERY_OUTBOUND move shares without a cash side. The response is one created change whose uuid is the record id. A write that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    schema(false), TransactionOperations::add);

    static final OperationSpec UPDATE = new OperationSpec(UPDATE_OP,
                    "Changes a whole transaction record, found by the UUID of either of its sides. Only the fields present change; null clears an optional field. The patch is overlaid on the current record and then validated exactly like an add, so a partial edit can't bypass validation and nothing is recomputed: fees, taxes and forex units that no longer fit a changed amount or shares are refused with a field-level error. The type, and the kind of a transfer, can't change; delete the record and add a new one. Both sides stay in step and keep their UUIDs. The response is one updated change, with before and after for the changed fields, whose uuid is the record id. A change that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    schema(true), TransactionUpdates::update);

    public static final List<OperationSpec> ALL = List.of(ADD, UPDATE, TransactionUpdates.DELETE);

    private TransactionOperations()
    {
    }

    private static Schema schema(boolean update)
    {
        var schema = Schema.object(update ? UPDATE_DESCRIPTION : "A transaction, in the shape the transactions output uses.")
                        .property(ApplyBatch.OPERATION_MEMBER,
                                        Schema.enumOf("Operation name.", List.of(update ? UPDATE_OP : OP)));
        if (update)
            schema.property(TransactionUpdates.UUID_MEMBER,
                            Schema.string("UUID of either side of the record to change."));

        return update ? TransactionShape.declarePatch(schema) : TransactionShape.declareInput(schema);
    }

    static void add(OperationContext context)
    {
        var name = parseTypeName(context);
        if (TransactionRecord.TRANSFER.equals(name))
            TransferOperations.add(context);
        else if (name != null && TransactionShape.INVESTMENT_ACCOUNT_TYPES.contains(name))
            TradeOperations.add(context, PortfolioTransaction.Type.valueOf(name));
        else
            addCash(context, name == null ? null : Type.valueOf(name));
    }

    private static void addCash(OperationContext context, Type type)
    {
        var input = new TransactionInput(context);

        input.refuse("investmentAccount", "kind", "from", "to", "targetAmount", "exchangeRate");

        var account = input.cashAccount();
        var dateTime = input.dateTime();
        var currencyCode = input.currency(account);
        var amount = input.amount("amount", Values.Amount);
        var security = input.instrument(type == null || SECURITY_ALLOWED.contains(type),
                        type != null && SECURITY_REQUIRED.contains(type));
        var shares = parseShares(context, input, type);
        var units = input.units(currencyCode, security);
        var exDate = exDate(context, type, security, dateTime);

        if (type != null)
            input.grossAmount(type.isCredit(), amount, units);

        var note = input.text("note");
        var source = input.text("source");

        if (context.failed() || type == null || account == null || dateTime == null || currencyCode == null
                        || amount == null)
            return;

        var transaction = new AccountTransaction(dateTime, currencyCode, amount, security, type);
        if (shares != null)
            transaction.setShares(shares);
        if (exDate != null)
            transaction.setExDate(exDate.atStartOfDay());
        if (note != null)
            transaction.setNote(note);
        if (source != null)
            transaction.setSource(source);
        units.forEach(transaction::addUnit);

        runCoreChecks(context, check -> check.process(transaction, account));

        commit(context, BuiltRecord.of(transaction, account));
    }

    /**
     * Puts the checked record into the model and reports it, or in an update
     * has it replace the record it stands for. Refuses a change that would
     * oversell.
     */
    static void commit(OperationContext context, BuiltRecord built)
    {
        var replaced = context.replacing();

        OversellGuard.check(context, "shares", replaced.map(TransactionUpdates.Existing::effects).orElse(List.of()),
                        OversellGuard.effectsOf(built.sides()));
        if (context.failed())
            return;

        if (replaced.isPresent())
        {
            TransactionUpdates.replace(context, replaced.get(), built);
            return;
        }

        built.insert().run();
        reportCreated(context, built.primary());
    }

    /** Runs core's non-mutating import checks, reporting every error they find. */
    static void runCoreChecks(OperationContext context, Function<ImportAction, ImportAction.Status> run)
    {
        for (var check : CORE_CHECKS)
        {
            var status = run.apply(check);
            if (status.getCode() == ImportAction.Status.Code.ERROR)
                context.fail(check instanceof CheckForexGrossValueAction ? "units" : "", "import-check-failed",
                                status.getMessage(), check.getClass().getSimpleName());
        }
    }

    /** Reports the record the transaction is the primary side of as created. */
    private static void reportCreated(OperationContext context, Transaction primary)
    {
        var record = TransactionRecord.allOf(context.client()).stream().filter(r -> r.primary() == primary).findFirst()
                        .orElseThrow();
        context.created(new OperationContext.Change("transaction", record.id(), null,
                        TransactionsCommand.render(record, context.client())));
    }

    private static String parseTypeName(OperationContext context)
    {
        var member = context.input().json().get("type");
        if (member == null || member.isJsonNull())
        {
            context.fail("type", "required", "A transaction needs a type.");
            return null;
        }
        if (!OperationContext.isText(member))
        {
            context.fail("type", "invalid-type", "The type must be a string.");
            return null;
        }

        var name = member.getAsString();
        if (!TransactionShape.TYPES.contains(name))
        {
            context.fail("type", "unsupported-type", "'" + name
                            + "' is not a transaction type this operation adds. Supported: "
                            + String.join(", ", TransactionShape.TYPES) + ".");
            return null;
        }
        return name;
    }

    private static Long parseShares(OperationContext context, TransactionInput input, Type type)
    {
        if (!context.input().has("shares"))
            return null;

        if (type != null && !SHARES_ALLOWED.contains(type))
        {
            context.fail("shares", "shares-not-allowed", type + " must not have shares.");
            return null;
        }
        return input.amount("shares", Values.Share);
    }

    /** The valid ex-date to store, or null if it is absent or refused. */
    private static LocalDate exDate(OperationContext context, Type type, Security security, LocalDateTime dateTime)
    {
        var input = context.input();
        if (!input.has("exDate"))
            return null;

        var text = input.json().get("exDate");
        if (!OperationContext.isText(text))
        {
            context.fail("exDate", "invalid-type", "The ex-date must be a string.");
            return null;
        }

        if (type != Type.DIVIDENDS || security == null)
        {
            context.fail("exDate", "exdate-not-allowed", "An ex-date is allowed only on a dividend with an instrument.");
            return null;
        }

        LocalDate ex;
        try
        {
            ex = LocalDate.parse(text.getAsString());
        }
        catch (DateTimeParseException e)
        {
            context.fail("exDate", "invalid-date", "'" + text.getAsString() + "' is not an ISO date (yyyy-MM-dd).");
            return null;
        }

        if (dateTime != null && ex.isAfter(dateTime.toLocalDate()))
        {
            context.fail("exDate", "exdate-after-payment", "The ex-date must not be after the payment date.");
            return null;
        }
        return ex;
    }

}
