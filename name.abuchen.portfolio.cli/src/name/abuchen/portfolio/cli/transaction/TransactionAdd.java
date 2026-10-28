package name.abuchen.portfolio.cli.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckForexGrossValueAction;
import name.abuchen.portfolio.datatransfer.actions.CheckSecurityRelatedValuesAction;
import name.abuchen.portfolio.datatransfer.actions.CheckTransactionDateAction;
import name.abuchen.portfolio.datatransfer.actions.CheckValidTypesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/**
 * The build phase of a write: builds the record a {@link Draft} describes,
 * without putting it into the model. Checks that the members the row
 * requires are there, checks the rules that span members, builds the sides
 * with one of core's five builders, and runs core's import checks and the
 * oversell check. An add reads its input into the draft first; an update
 * lays its patch on the draft of the stored record.
 */
final class TransactionAdd
{
    static final String OPERATION = "transaction.add";

    /** The input of an add. */
    static final Schema SCHEMA = TransactionShape.declareInput(Schema
                    .object("A transaction, in the shape the transactions output uses.")
                    .property(OperationSpec.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(OPERATION))));

    /**
     * The failure codes an add raises beyond those its schema implies: the
     * rule table's refusals, the rules across members, core's import checks
     * and the oversell check.
     */
    static final Set<ErrorCode> FAILURE_CODES = failureCodes();

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

    private final Draft draft;
    private final List<FieldFailure> failures;

    private TransactionAdd(Draft draft, List<FieldFailure> failures)
    {
        this.draft = draft;
        this.failures = failures;
    }

    /**
     * Reads the input of an add and builds the record.
     *
     * @param reportUnknown
     *            whether a member the add does not declare is reported; if
     *            not, it still refuses the input
     */
    static Result add(Client client, Predicate<Object> existedBeforeBatch, JsonObject json, boolean reportUnknown)
    {
        var failures = new ArrayList<FieldFailure>();
        var input = new OperationInput(json, client, existedBeforeBatch, failures::add);

        var unknown = json.keySet().stream().anyMatch(member -> !SCHEMA.declares(member));
        if (reportUnknown)
            input.rejectUnknownFields(SCHEMA);

        return build(TransactionInput.read(input, failures), failures, List.of(), unknown);
    }

    /**
     * Builds the record the draft describes.
     *
     * @param failures
     *            those of reading the draft; the build adds its own
     * @param replaced
     *            the sides the record replaces, in an update; their effect on
     *            the holdings is taken away before the oversell check
     * @param refused
     *            whether the input is refused already for a reason the
     *            failures need not show, such as an unknown member
     */
    static Result build(Draft draft, List<FieldFailure> failures, List<BuiltRecord.Side> replaced, boolean refused)
    {
        var add = new TransactionAdd(draft, failures);

        var built = add.build();
        if (built != null && failures.isEmpty())
            failures.addAll(OversellGuard.check(TransactionShape.SHARES.name(), OversellGuard.effectsOf(replaced),
                            OversellGuard.effectsOf(built.sides())));

        return new Result(refused || !failures.isEmpty() ? null : built, failures);
    }

    private static Set<ErrorCode> failureCodes()
    {
        var codes = EnumSet.of(ErrorCode.UNSUPPORTED_TYPE, ErrorCode.UNSUPPORTED_KIND, ErrorCode.INVALID_DATE,
                        ErrorCode.INVALID_TIME, ErrorCode.INVALID_CURRENCY, ErrorCode.CURRENCY_MISMATCH,
                        ErrorCode.GROSS_AMOUNT_MISMATCH, ErrorCode.EXDATE_AFTER_PAYMENT,
                        ErrorCode.UNSUPPORTED_UNIT_TYPE, ErrorCode.FOREX_REQUIRED, ErrorCode.FOREX_INCOMPLETE,
                        ErrorCode.FOREX_CURRENCY_MISMATCH, ErrorCode.FOREX_MISMATCH,
                        ErrorCode.FOREX_GROSS_VALUE_REQUIRED, ErrorCode.SAME_ACCOUNT, ErrorCode.IMPORT_CHECK_FAILED,
                        ErrorCode.NEGATIVE_HOLDING);
        Arrays.stream(RuleTable.Ruled.values()).map(RuleTable.Ruled::refusalCode).forEach(codes::add);
        return Collections.unmodifiableSet(codes);
    }

    private boolean failed()
    {
        return !failures.isEmpty();
    }

    private void fail(String member, ErrorCode code, String message)
    {
        failures.add(new FieldFailure(member, code, message, null));
    }

    /** Reports a missing member, unless it was given and failed its own check. */
    private void require(Object value, String member, String message)
    {
        if (value == null && !draft.failed.contains(member))
            fail(member, ErrorCode.REQUIRED, message);
    }

    private BuiltRecord build()
    {
        require(draft.date, TransactionShape.DATE.name(), "A transaction needs a date.");
        require(draft.currencyCode, TransactionShape.CURRENCY_CODE.name(), "A transaction needs a currency.");
        require(draft.amount, TransactionShape.AMOUNT.name(), "A transaction needs amount.");

        var row = draft.row;
        if (row == null)
            return null;

        for (var member : RuleTable.Ruled.values())
        {
            if (member != RuleTable.Ruled.KIND && row.rule(member) == RuleTable.Rule.REQUIRED)
                require(draft.get(member), member.id(), "A transaction needs " + member.id() + ".");
        }

        return switch (row)
        {
            case BUY, SELL, DELIVERY_INBOUND, DELIVERY_OUTBOUND -> trade(row);
            case CASH_TRANSFER -> cashTransfer();
            case SECURITY_TRANSFER -> securityTransfer();
            default -> cash(row);
        };
    }

    /** A deposit, removal, interest, dividend, fee, tax or refund: one side in a cash account. */
    private BuiltRecord cash(RuleTable.Row row)
    {
        var type = AccountTransaction.Type.valueOf(row.type());

        var account = draft.cashAccount;
        var dateTime = draft.dateTime();
        var currencyCode = currencyOf(account);
        var amount = draft.amount;
        var security = draft.instrument;
        var shares = draft.shares;
        var units = units(currencyCode, security);
        var exDate = exDate();
        grossAmount(type.isCredit(), amount, units);

        if (failed())
            return null;

        var transaction = new AccountTransaction(dateTime, currencyCode, amount, security, type);
        if (shares != null)
            transaction.setShares(shares);
        if (exDate != null)
            transaction.setExDate(exDate.atStartOfDay());
        if (draft.note != null)
            transaction.setNote(draft.note);
        if (draft.source != null)
            transaction.setSource(draft.source);
        units.forEach(transaction::addUnit);

        runCoreChecks(check -> check.process(transaction, account));
        return BuiltRecord.of(transaction, account);
    }

    /**
     * A buy or sell, with both sides created through core's
     * {@link BuySellEntry} so they cannot get out of step, or a delivery,
     * which has no cash side.
     */
    private BuiltRecord trade(RuleTable.Row row)
    {
        var type = PortfolioTransaction.Type.valueOf(row.type());
        var delivery = row.rule(RuleTable.Ruled.CASH_ACCOUNT) == RuleTable.Rule.REFUSED;

        var portfolio = draft.investmentAccount;
        var account = draft.cashAccount;
        var dateTime = draft.dateTime();
        var currencyCode = currencyOf(account);
        var amount = draft.amount;
        var security = draft.instrument;
        var shares = draft.shares;
        var units = units(currencyCode, security);
        grossAmount(type.isLiquidation(), amount, units);

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
            transaction.setNote(draft.note);
            transaction.setSource(draft.source);
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
        entry.setNote(draft.note);
        entry.setSource(draft.source);
        units.forEach(entry.getPortfolioTransaction()::addUnit);

        runCoreChecks(check -> check.process(entry, account, portfolio));
        return BuiltRecord.of(entry);
    }

    /** Shares between two investment accounts, both sides through core's {@link PortfolioTransferEntry}. */
    private BuiltRecord securityTransfer()
    {
        var security = draft.instrument;
        var from = (Portfolio) draft.from;
        var to = (Portfolio) draft.to;
        rejectSameAccount(from, to);

        var currencyCode = draft.currencyCode;
        if (currencyCode != null && security != null && !currencyCode.equals(security.getCurrencyCode()))
            fail(TransactionShape.CURRENCY_CODE.name(), ErrorCode.CURRENCY_MISMATCH,
                            "The amount must be in the instrument's currency " + security.getCurrencyCode()
                                            + ", not " + currencyCode + ".");

        if (failed())
            return null;

        var entry = new PortfolioTransferEntry(from, to);
        entry.setDate(draft.dateTime());
        entry.setSecurity(security);
        entry.setShares(draft.shares);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(draft.amount);
        entry.setNote(draft.note);
        entry.setSource(draft.source);

        runCoreChecks(check -> check.process(entry, from, to));
        return BuiltRecord.of(entry);
    }

    /**
     * Cash between two cash accounts, both sides through core's
     * {@link AccountTransferEntry}. Between accounts of different currencies
     * it needs the target amount, and the rate is derived from the two
     * amounts; between accounts of the same currency it takes neither.
     */
    private BuiltRecord cashTransfer()
    {
        var from = (Account) draft.from;
        var to = (Account) draft.to;
        rejectSameAccount(from, to);

        var currencyCode = currencyOf(from);
        var amount = draft.amount;

        var crossCurrency = from != null && to != null && !from.getCurrencyCode().equals(to.getCurrencyCode());
        var targetAmount = draft.targetAmount;
        BigDecimal rate = null;
        if (crossCurrency)
        {
            require(targetAmount, RuleTable.Ruled.TARGET_AMOUNT.id(),
                            "A transaction needs " + RuleTable.Ruled.TARGET_AMOUNT.id() + ".");
            rate = checkedRate(amount, targetAmount, from.getCurrencyCode(), to.getCurrencyCode());
        }
        else if (from != null && to != null)
        {
            var message = "A transfer between accounts of the same currency must not have ";
            refuseIfPresent(RuleTable.Ruled.TARGET_AMOUNT, message + RuleTable.Ruled.TARGET_AMOUNT.id() + ".");
            refuseIfPresent(RuleTable.Ruled.EXCHANGE_RATE, message + RuleTable.Ruled.EXCHANGE_RATE.id() + ".");
        }

        if (failed())
            return null;

        var entry = new AccountTransferEntry(from, to);
        entry.setDate(draft.dateTime());
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(amount);
        entry.setNote(draft.note);
        entry.setSource(draft.source);

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

    /** Reports the member if it has a value, under its refusal code, because a rule across members refuses it. */
    private void refuseIfPresent(RuleTable.Ruled member, String message)
    {
        if (draft.has(member))
            fail(member.id(), member.refusalCode(), message);
    }

    /**
     * The currency, or null if it is missing or not that of the cash
     * account.
     *
     * @param account
     *            the cash account whose currency the amount must be in, or
     *            null if any currency goes
     */
    private String currencyOf(Account account)
    {
        var code = draft.currencyCode;
        if (code != null && account != null && !code.equals(account.getCurrencyCode()))
        {
            fail(TransactionShape.CURRENCY_CODE.name(), ErrorCode.CURRENCY_MISMATCH,
                            "The amount must be in the cash account's currency " + account.getCurrencyCode()
                                            + ", not " + code + ".");
            return null;
        }
        return code;
    }

    /**
     * The units that fit the record: in its currency, and with forex only in
     * the instrument's currency unless they are the gross value. An
     * instrument in another currency needs a forex gross value.
     */
    private List<Transaction.Unit> units(String currencyCode, Security security)
    {
        var units = new ArrayList<Transaction.Unit>();
        for (var given : draft.units)
        {
            var unit = given.unit();
            var fits = currencyCode != null;

            if (currencyCode != null && !unit.getAmount().getCurrencyCode().equals(currencyCode))
            {
                fail(given.field() + ".currencyCode", ErrorCode.CURRENCY_MISMATCH,
                                "A unit must be in the transaction's currency " + currencyCode + ".");
                fits = false;
            }

            if (unit.getType() != Transaction.Unit.Type.GROSS_VALUE && unit.getForex() != null && security != null
                            && !unit.getForex().getCurrencyCode().equals(security.getCurrencyCode()))
            {
                fail(given.field() + ".forexCurrencyCode", ErrorCode.FOREX_CURRENCY_MISMATCH,
                                "A fee or tax may carry forex only in the instrument's currency "
                                                + security.getCurrencyCode() + ".");
                fits = false;
            }

            if (fits)
                units.add(unit);
        }

        requireForexGrossValue(currencyCode, security, units);
        return units;
    }

    /** A record on an instrument in another currency needs a forex gross-value unit in that currency. */
    private void requireForexGrossValue(String currencyCode, Security security, List<Transaction.Unit> units)
    {
        if (security == null || currencyCode == null || security.getCurrencyCode() == null
                        || security.getCurrencyCode().equals(currencyCode))
            return;

        var hasMatch = units.stream().filter(u -> u.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .anyMatch(u -> u.getForex() != null
                                        && security.getCurrencyCode().equals(u.getForex().getCurrencyCode()));
        if (!hasMatch)
            fail(RuleTable.Ruled.UNITS.id(), ErrorCode.FOREX_GROSS_VALUE_REQUIRED,
                            "The instrument is in " + security.getCurrencyCode() + " but the transaction is in "
                                            + currencyCode
                                            + "; add a GROSS_VALUE unit with its forex amount and exchange rate.");
    }

    /** The ex-date to store, or null if there is none; it is never after the payment date. */
    private LocalDate exDate()
    {
        var dateTime = draft.dateTime();
        if (draft.exDate != null && dateTime != null && draft.exDate.isAfter(dateTime.toLocalDate()))
        {
            fail(RuleTable.Ruled.EX_DATE.id(), ErrorCode.EXDATE_AFTER_PAYMENT,
                            "The ex-date must not be after the payment date.");
            return null;
        }
        return draft.exDate;
    }

    /** Checks the optional gross amount against the amount, fees and taxes. */
    private void grossAmount(boolean credit, Long amount, List<Transaction.Unit> units)
    {
        var gross = draft.grossAmount;
        if (gross == null || amount == null)
            return;

        var taxAndFees = units.stream()
                        .filter(u -> u.getType() == Transaction.Unit.Type.FEE || u.getType() == Transaction.Unit.Type.TAX)
                        .mapToLong(u -> u.getAmount().getAmount()).sum();
        var expected = amount + (credit ? taxAndFees : -taxAndFees);

        if (gross != expected)
            fail(RuleTable.Ruled.GROSS_AMOUNT.id(), ErrorCode.GROSS_AMOUNT_MISMATCH,
                            "The gross amount does not reconcile with the amount, fees and taxes.");
    }

    /**
     * The rate to store, always derived as amount divided by target amount. A
     * supplied rate is only checked against core's rounding tolerance.
     */
    private BigDecimal checkedRate(Long amount, Long targetAmount, String fromCurrency, String toCurrency)
    {
        if (amount == null || targetAmount == null)
            return null;

        // mirrors desktop AccountTransferModel.setAmount; ADR 0007-cli-arithmetic-mirrors-core-rounding-per-site.md
        var derived = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(targetAmount), RATE_SCALE,
                        RoundingMode.HALF_UP);

        var supplied = draft.exchangeRate;
        if (supplied != null && !Transaction.Unit.isWithinRoundingTolerance(Money.of(fromCurrency, amount),
                        Money.of(toCurrency, targetAmount), supplied))
        {
            fail(RuleTable.Ruled.EXCHANGE_RATE.id(), ErrorCode.FOREX_MISMATCH,
                            "targetAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return derived;
    }

    private <T> void rejectSameAccount(T from, T to)
    {
        if (from != null && from == to)
            fail(RuleTable.Ruled.TO.id(), ErrorCode.SAME_ACCOUNT, "A transfer needs two different accounts.");
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
                                ErrorCode.IMPORT_CHECK_FAILED, status.getMessage(), check.getClass().getSimpleName()));
        }
    }
}
