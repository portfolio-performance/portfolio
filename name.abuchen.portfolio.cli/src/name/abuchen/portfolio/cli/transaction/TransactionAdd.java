package name.abuchen.portfolio.cli.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Builds the record an add describes, without putting it into the model:
 * reads the type and the kind, applies the {@link RuleTable} row, checks the
 * rules that span members, builds the sides with one of core's five builders,
 * runs core's import checks and the oversell check.
 */
final class TransactionAdd
{
    static final String OPERATION = "transaction.add";

    /** The input of an add. */
    static final Schema SCHEMA = TransactionShape.declareInput(Schema
                    .object("A transaction, in the shape the transactions output uses.")
                    .property(OperationSpec.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(OPERATION))));

    private static final int RATE_SCALE = 10;

    /** Core's non-mutating import checks, run on the built transaction as a safety net. */
    private static final List<ImportAction> CORE_CHECKS = List.of(new CheckTransactionDateAction(),
                    new CheckSecurityRelatedValuesAction(), new CheckCurrenciesAction(),
                    new CheckForexGrossValueAction(), new CheckValidTypesAction());

    /**
     * The checked record, or null if the input was refused.
     *
     * @param failures
     *            what was refused; empty if only unknown members, which the
     *            caller reports itself, refused it
     */
    record Result(BuiltRecord built, List<FieldFailure> failures)
    {
    }

    private final List<FieldFailure> failures = new ArrayList<>();
    private final OperationInput input;

    private TransactionAdd(Client client, Predicate<Object> existedBeforeBatch, JsonObject json)
    {
        this.input = new OperationInput(json, client, existedBeforeBatch, failures::add);
    }

    /**
     * @param replaced
     *            the sides the record replaces, in an update; their effect on
     *            the holdings is taken away before the oversell check
     * @param reportUnknown
     *            whether a member the add does not declare is reported; if
     *            not, it still refuses the input
     */
    static Result build(Client client, Predicate<Object> existedBeforeBatch, JsonObject json,
                    List<BuiltRecord.Side> replaced, boolean reportUnknown)
    {
        var add = new TransactionAdd(client, existedBeforeBatch, json);

        var unknown = json.keySet().stream().anyMatch(member -> !SCHEMA.declares(member));
        if (reportUnknown)
            add.input.rejectUnknownFields(SCHEMA);

        var built = add.build();
        if (built != null && add.failures.isEmpty())
            add.failures.addAll(OversellGuard.check(TransactionShape.SHARES.name(),
                            OversellGuard.effectsOf(replaced), OversellGuard.effectsOf(built.sides())));

        var refused = unknown || !add.failures.isEmpty();
        return new Result(refused ? null : built, add.failures);
    }

    private boolean failed()
    {
        return !failures.isEmpty();
    }

    private BuiltRecord build()
    {
        var type = type();
        var kind = TransactionRecord.TRANSFER.equals(type) ? kind() : null;
        var row = type == null ? null : RuleTable.Row.of(type, kind).orElse(null);

        var reader = new TransactionInput(input, type, row);
        reader.refuseByTable();

        if (row == null)
        {
            // still report what is wrong with the members that every type takes
            reader.dateTime();
            reader.currency(null);
            reader.amount(TransactionShape.AMOUNT.name(), Values.Amount);
            reader.text(TransactionShape.NOTE.name());
            reader.text(TransactionShape.SOURCE.name());
            return null;
        }

        return switch (row)
        {
            case BUY, SELL, DELIVERY_INBOUND, DELIVERY_OUTBOUND -> trade(reader, row);
            case CASH_TRANSFER -> cashTransfer(reader);
            case SECURITY_TRANSFER -> securityTransfer(reader);
            default -> cash(reader, row);
        };
    }

    private String type()
    {
        var member = TransactionShape.TYPE.name();
        var element = input.json().get(member);
        if (element == null || element.isJsonNull())
        {
            input.fail(member, "required", "A transaction needs a type.");
            return null;
        }
        if (!OperationInput.isText(element))
        {
            input.fail(member, "invalid-type", "The type must be a string.");
            return null;
        }

        var name = element.getAsString();
        if (!TransactionShape.TYPES.contains(name))
        {
            input.fail(member, "unsupported-type", "'" + name
                            + "' is not a transaction type this operation adds. Supported: "
                            + String.join(", ", TransactionShape.TYPES) + ".");
            return null;
        }
        return name;
    }

    private TransactionRecord.TransferKind kind()
    {
        var member = TransactionShape.KIND.name();
        var kinds = String.join(", ", TransactionRecord.TransferKind.ids());
        var kind = input.requiredText(member, "A transfer needs a kind: " + kinds + ".");
        if (kind == null)
            return null;

        for (var candidate : TransactionRecord.TransferKind.values())
        {
            if (candidate.id().equals(kind))
                return candidate;
        }

        input.fail(member, "unsupported-kind", "'" + kind + "' is not a kind of transfer: " + kinds + ".");
        return null;
    }

    /** A deposit, removal, interest, dividend, fee, tax or refund: one side in a cash account. */
    private BuiltRecord cash(TransactionInput reader, RuleTable.Row row)
    {
        var type = AccountTransaction.Type.valueOf(row.type());

        var account = reader.reference(RuleTable.Ruled.CASH_ACCOUNT, CashAccounts.CASH_ACCOUNTS.entityKind());
        var dateTime = reader.dateTime();
        var currencyCode = reader.currency(account);
        var amount = reader.amount(TransactionShape.AMOUNT.name(), Values.Amount);
        var security = reader.reference(RuleTable.Ruled.INSTRUMENT, Instruments.INSTRUMENTS.entityKind());
        var shares = reader.number(RuleTable.Ruled.SHARES, Values.Share);
        var units = reader.units(currencyCode, security);
        var exDate = reader.exDate(dateTime);
        reader.grossAmount(type.isCredit(), amount, units);
        var note = reader.text(TransactionShape.NOTE.name());
        var source = reader.text(TransactionShape.SOURCE.name());

        if (failed())
            return null;

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

        runCoreChecks(check -> check.process(transaction, account));
        return BuiltRecord.of(transaction, account);
    }

    /**
     * A buy or sell, with both sides created through core's
     * {@link BuySellEntry} so they cannot get out of step, or a delivery,
     * which has no cash side.
     */
    private BuiltRecord trade(TransactionInput reader, RuleTable.Row row)
    {
        var type = PortfolioTransaction.Type.valueOf(row.type());
        var delivery = row.rule(RuleTable.Ruled.CASH_ACCOUNT) == RuleTable.Rule.REFUSED;

        var portfolio = reader.reference(RuleTable.Ruled.INVESTMENT_ACCOUNT,
                        InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind());
        var account = reader.reference(RuleTable.Ruled.CASH_ACCOUNT, CashAccounts.CASH_ACCOUNTS.entityKind());
        var dateTime = reader.dateTime();
        var currencyCode = reader.currency(account);
        var amount = reader.amount(TransactionShape.AMOUNT.name(), Values.Amount);
        var security = reader.reference(RuleTable.Ruled.INSTRUMENT, Instruments.INSTRUMENTS.entityKind());
        var shares = reader.number(RuleTable.Ruled.SHARES, Values.Share);
        var units = reader.units(currencyCode, security);
        reader.grossAmount(type.isLiquidation(), amount, units);
        var note = reader.text(TransactionShape.NOTE.name());
        var source = reader.text(TransactionShape.SOURCE.name());

        if (failed())
            return null;

        if (delivery)
        {
            var transaction = new PortfolioTransaction();
            transaction.setType(type);
            transaction.setDateTime(dateTime);
            transaction.setSecurity(security);
            transaction.setShares(shares);
            transaction.setMonetaryAmount(Money.of(currencyCode, amount));
            transaction.setNote(note);
            transaction.setSource(source);
            units.forEach(transaction::addUnit);

            runCoreChecks(check -> check.process(transaction, portfolio));
            return BuiltRecord.of(transaction, portfolio);
        }

        var entry = new BuySellEntry(portfolio, account);
        entry.setType(type);
        entry.setDate(dateTime);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setMonetaryAmount(Money.of(currencyCode, amount));
        entry.setNote(note);
        entry.setSource(source);
        units.forEach(entry.getPortfolioTransaction()::addUnit);

        runCoreChecks(check -> check.process(entry, account, portfolio));
        return BuiltRecord.of(entry);
    }

    /** Shares between two investment accounts, both sides through core's {@link PortfolioTransferEntry}. */
    private BuiltRecord securityTransfer(TransactionInput reader)
    {
        var security = reader.reference(RuleTable.Ruled.INSTRUMENT, Instruments.INSTRUMENTS.entityKind());
        var from = reader.reference(RuleTable.Ruled.FROM, InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind());
        var to = reader.reference(RuleTable.Ruled.TO, InvestmentAccounts.INVESTMENT_ACCOUNTS.entityKind());
        rejectSameAccount(from, to);

        var dateTime = reader.dateTime();
        var currencyCode = reader.currency(null);
        if (currencyCode != null && security != null && !currencyCode.equals(security.getCurrencyCode()))
            input.fail(TransactionShape.CURRENCY_CODE.name(), "currency-mismatch",
                            "The amount must be in the instrument's currency " + security.getCurrencyCode()
                                            + ", not " + currencyCode + ".");
        var amount = reader.amount(TransactionShape.AMOUNT.name(), Values.Amount);
        var shares = reader.number(RuleTable.Ruled.SHARES, Values.Share);
        var note = reader.text(TransactionShape.NOTE.name());
        var source = reader.text(TransactionShape.SOURCE.name());

        if (failed())
            return null;

        var entry = new PortfolioTransferEntry(from, to);
        entry.setDate(dateTime);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(amount);
        entry.setNote(note);
        entry.setSource(source);

        runCoreChecks(check -> check.process(entry, from, to));
        return BuiltRecord.of(entry);
    }

    /**
     * Cash between two cash accounts, both sides through core's
     * {@link AccountTransferEntry}. Between accounts of different currencies
     * it needs the target amount, and the rate is derived from the two
     * amounts; between accounts of the same currency it takes neither.
     */
    private BuiltRecord cashTransfer(TransactionInput reader)
    {
        var from = reader.reference(RuleTable.Ruled.FROM, CashAccounts.CASH_ACCOUNTS.entityKind());
        var to = reader.reference(RuleTable.Ruled.TO, CashAccounts.CASH_ACCOUNTS.entityKind());
        rejectSameAccount(from, to);

        var dateTime = reader.dateTime();
        var currencyCode = reader.currency(from);
        var amount = reader.amount(TransactionShape.AMOUNT.name(), Values.Amount);
        var note = reader.text(TransactionShape.NOTE.name());
        var source = reader.text(TransactionShape.SOURCE.name());

        var crossCurrency = from != null && to != null && !from.getCurrencyCode().equals(to.getCurrencyCode());
        Long targetAmount = null;
        BigDecimal rate = null;
        if (crossCurrency)
        {
            targetAmount = reader.amount(RuleTable.Ruled.TARGET_AMOUNT.id(), Values.Amount);
            rate = checkedRate(amount, targetAmount, from.getCurrencyCode(), to.getCurrencyCode());
        }
        else if (from != null && to != null)
        {
            var message = "A transfer between accounts of the same currency must not have ";
            reader.refuseIfPresent(RuleTable.Ruled.TARGET_AMOUNT,
                            message + RuleTable.Ruled.TARGET_AMOUNT.id() + ".");
            reader.refuseIfPresent(RuleTable.Ruled.EXCHANGE_RATE,
                            message + RuleTable.Ruled.EXCHANGE_RATE.id() + ".");
        }

        if (failed())
            return null;

        var entry = new AccountTransferEntry(from, to);
        entry.setDate(dateTime);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(amount);
        entry.setNote(note);
        entry.setSource(source);

        if (crossCurrency)
        {
            entry.getTargetTransaction().setMonetaryAmount(Money.of(to.getCurrencyCode(), targetAmount));
            entry.getSourceTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                            Money.of(from.getCurrencyCode(), amount), Money.of(to.getCurrencyCode(), targetAmount),
                            rate));
        }

        runCoreChecks(check -> check.process(entry, from, to));
        return BuiltRecord.of(entry);
    }

    /**
     * The rate to store, always derived as amount divided by target amount. A
     * supplied rate is only checked against core's rounding tolerance.
     */
    private BigDecimal checkedRate(Long amount, Long targetAmount, String fromCurrency, String toCurrency)
    {
        var member = RuleTable.Ruled.EXCHANGE_RATE.id();
        var supplied = input.rawNumber(member);
        if (input.has(member) && supplied == null)
        {
            input.fail(member, "invalid-type", "The exchangeRate must be a number.");
            return null;
        }
        if (supplied != null && supplied.signum() <= 0)
        {
            input.fail(member, "not-positive", "The exchange rate must be positive.");
            return null;
        }

        if (amount == null || targetAmount == null)
            return null;

        // mirrors desktop AccountTransferModel.setAmount; ADR 0007-cli-arithmetic-mirrors-core-rounding-per-site.md
        var derived = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(targetAmount), RATE_SCALE,
                        RoundingMode.HALF_UP);

        if (supplied != null && !Transaction.Unit.isWithinRoundingTolerance(Money.of(fromCurrency, amount),
                        Money.of(toCurrency, targetAmount), supplied))
        {
            input.fail(member, "forex-mismatch",
                            "targetAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return derived;
    }

    private <T> void rejectSameAccount(T from, T to)
    {
        if (from != null && from == to)
            input.fail(RuleTable.Ruled.TO.id(), "same-account", "A transfer needs two different accounts.");
    }

    /** Runs core's non-mutating import checks, reporting every error they find. */
    private void runCoreChecks(Function<ImportAction, ImportAction.Status> run)
    {
        for (var check : CORE_CHECKS)
        {
            var status = run.apply(check);
            if (status.getCode() == ImportAction.Status.Code.ERROR)
                failures.add(new FieldFailure(
                                check instanceof CheckForexGrossValueAction ? RuleTable.Ruled.UNITS.id() : "",
                                "import-check-failed", status.getMessage(), check.getClass().getSimpleName()));
        }
    }
}
