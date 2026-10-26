package name.abuchen.portfolio.cli.apply;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.cli.query.BuiltRecord;
import name.abuchen.portfolio.cli.query.TransactionRecord;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * The part of <code>transaction.add</code> that records transfers: shares
 * between investment accounts and cash between cash accounts. Both sides are
 * created together through core's transfer entries. Nothing is inserted until
 * every check passed.
 */
final class TransferOperations
{
    private static final int RATE_SCALE = 10;

    private TransferOperations()
    {
    }

    static void add(OperationContext context)
    {
        var input = new TransactionInput(context);

        input.refuse("investmentAccount", "cashAccount", "units", "grossAmount", "exDate");

        var kind = parseKind(context);
        var security = kind == TransactionRecord.TransferKind.CASH ? null
                        : input.instrument(true, kind == TransactionRecord.TransferKind.SECURITY);
        if (kind == TransactionRecord.TransferKind.CASH)
        {
            input.refuse("instrument", "shares");
            addCash(context, input);
        }
        else if (kind == TransactionRecord.TransferKind.SECURITY)
        {
            input.refuse("targetAmount", "exchangeRate");
            addSecurity(context, input, security);
        }
    }

    private static TransactionRecord.TransferKind parseKind(OperationContext context)
    {
        var kind = context.input().text("kind");
        var kinds = String.join(", ", TransactionRecord.TransferKind.ids());
        if (kind == null)
        {
            context.fail("kind", "required", "A transfer needs a kind: " + kinds + ".");
            return null;
        }

        for (var candidate : TransactionRecord.TransferKind.values())
        {
            if (candidate.id().equals(kind))
                return candidate;
        }

        context.fail("kind", "unsupported-kind", "'" + kind + "' is not a kind of transfer: " + kinds + ".");
        return null;
    }

    private static void addSecurity(OperationContext context, TransactionInput input, Security security)
    {
        var from = input.investmentAccount("from");
        var to = input.investmentAccount("to");
        rejectSameAccount(context, from, to);

        var dateTime = input.dateTime();
        var currencyCode = input.currency(null);
        if (currencyCode != null && security != null && !currencyCode.equals(security.getCurrencyCode()))
        {
            context.fail("currencyCode", "currency-mismatch",
                            "The amount must be in the instrument's currency " + security.getCurrencyCode() + ", not "
                                            + currencyCode + ".");
            currencyCode = null;
        }
        var amount = input.amount("amount", Values.Amount);
        var shares = input.amount("shares", Values.Share);
        var note = input.text("note");
        var source = input.text("source");

        if (context.failed() || from == null || to == null || security == null || dateTime == null
                        || currencyCode == null || amount == null || shares == null)
            return;

        var entry = new PortfolioTransferEntry(from, to);
        entry.setDate(dateTime);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(amount);
        entry.setNote(note);
        entry.setSource(source);

        TransactionOperations.runCoreChecks(context, check -> check.process(entry, from, to));
        TransactionOperations.commit(context, BuiltRecord.of(entry));
    }

    private static void addCash(OperationContext context, TransactionInput input)
    {
        var from = input.cashAccount("from");
        var to = input.cashAccount("to");
        rejectSameAccount(context, from, to);

        var dateTime = input.dateTime();
        var currencyCode = input.currency(from);
        var amount = input.amount("amount", Values.Amount);
        var note = input.text("note");
        var source = input.text("source");

        var crossCurrency = from != null && to != null && !from.getCurrencyCode().equals(to.getCurrencyCode());
        Long targetAmount = null;
        BigDecimal rate = null;
        if (crossCurrency)
        {
            targetAmount = input.amount("targetAmount", Values.Amount);
            rate = checkedRate(context, amount, targetAmount, from.getCurrencyCode(), to.getCurrencyCode());
        }
        else if (from != null && to != null)
        {
            input.refuse("targetAmount", "exchangeRate");
        }

        if (context.failed() || from == null || to == null || dateTime == null || currencyCode == null
                        || amount == null || crossCurrency && (targetAmount == null || rate == null))
            return;

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

        TransactionOperations.runCoreChecks(context, check -> check.process(entry, from, to));
        TransactionOperations.commit(context, BuiltRecord.of(entry));
    }

    /**
     * The rate to store, always derived as amount divided by target amount. A
     * supplied rate is only checked against core's rounding tolerance.
     */
    private static BigDecimal checkedRate(OperationContext context, Long amount, Long targetAmount, String fromCurrency,
                    String toCurrency)
    {
        var input = context.input();
        var supplied = input.rawNumber("exchangeRate");
        if (input.has("exchangeRate") && supplied == null)
        {
            context.fail("exchangeRate", "invalid-type", "The exchangeRate must be a number.");
            return null;
        }
        if (supplied != null && supplied.signum() <= 0)
        {
            context.fail("exchangeRate", "not-positive", "The exchange rate must be positive.");
            return null;
        }

        if (amount == null || targetAmount == null)
            return null;

        var derived = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(targetAmount), RATE_SCALE,
                        RoundingMode.HALF_UP);

        if (supplied != null && !Transaction.Unit.isWithinRoundingTolerance(Money.of(fromCurrency, amount),
                        Money.of(toCurrency, targetAmount), supplied))
        {
            context.fail("exchangeRate", "forex-mismatch",
                            "targetAmount times exchangeRate is not amount within core's rounding tolerance.");
            return null;
        }
        return derived;
    }

    private static <T> void rejectSameAccount(OperationContext context, T from, T to)
    {
        if (from != null && from == to)
            context.fail("to", "same-account", "A transfer needs two different accounts.");
    }
}
