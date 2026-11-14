package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * The bookings of a file, one row per transaction as it was recorded.
 * <p>
 * What {@link TradesHandler} reports are purchases and sales matched into
 * trades, so a trade is an answer about a position. This handler reports the
 * transactions underneath: the deposit that funded a purchase, the dividend
 * that arrived, the fee that was charged, the transfer that moved shares
 * between two accounts. Nothing else in this API reaches them.
 */
public class TransactionsHandler
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$

    private TransactionsHandler()
    {
    }

    /**
     * @param client
     *            the file to read
     * @param fromParam
     *            first day of the period, inclusive; by default no lower bound
     * @param toParam
     *            last day of the period, inclusive; by default no upper bound
     * @param cashAccountParam
     *            restrict to bookings with a side in this cash account, as its
     *            uuid; for a purchase or a sale that is the settlement account
     *            of the securities account, which the file derives
     * @param investmentAccountParam
     *            restrict to the bookings of this securities account, as its
     *            uuid; cash accounts take no part in them, so this filter alone
     *            never yields a deposit or a fee
     * @param instrumentParam
     *            restrict to bookings naming this instrument, as its uuid
     * @param typeParam
     *            only these types, comma-separated and case-insensitive; the
     *            names of both families, as the {@code type} field spells them
     * @param cursorParam
     *            where to continue a list that did not fit into one answer
     * @param limitParam
     *            at most this many rows
     */
    public static JsonObject list(Client client, String fromParam, String toParam, String cashAccountParam,
                    String investmentAccountParam, String instrumentParam, String typeParam, String cursorParam,
                    String limitParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var from = fromParam == null ? null : CalcParams.date("from", fromParam, errors); //$NON-NLS-1$
        var to = toParam == null ? null : CalcParams.date("to", toParam, errors); //$NON-NLS-1$
        if (from != null && to != null && to.isBefore(from))
            errors.add(new ApiException.FieldError("to", "invalid-range", "to must not be before from")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        var cashAccount = reference("cashAccount", cashAccountParam, client.getAccounts(), Account::getUUID, errors); //$NON-NLS-1$
        var investmentAccount = reference("investmentAccount", investmentAccountParam, client.getPortfolios(), //$NON-NLS-1$
                        Portfolio::getUUID, errors);
        var instrument = reference("instrument", instrumentParam, client.getSecurities(), Security::getUUID, errors); //$NON-NLS-1$
        var types = types(typeParam, errors);

        // parsed here rather than where it is applied: a bad cursor or limit is
        // a field error like any other, and the lists are built only once they are
        var paging = Paging.parse(Paging.Kind.TRANSACTIONS, scope(from, to), cursorParam, limitParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var rows = new ArrayList<Row>();

        // a cash account holds money, so a booking owned by one has no side in
        // a securities account - and asking for one of those rules the rest out
        if (investmentAccount == null)
        {
            for (var account : client.getAccounts())
            {
                if (cashAccount != null && !account.getUUID().equals(cashAccount.getUUID()))
                    continue;
                for (var transaction : account.getTransactions())
                {
                    if (fits(transaction, from, to, instrument, types))
                        rows.add(new Row(transaction, account, null));
                }
            }
        }

        for (var portfolio : client.getPortfolios())
        {
            if (investmentAccount != null && !portfolio.getUUID().equals(investmentAccount.getUUID()))
                continue;

            // where the money of a purchase or a sale comes from is not on the
            // transaction, it is the settlement account of the securities account
            var settlement = portfolio.getReferenceAccount();
            if (cashAccount != null && (settlement == null || !settlement.getUUID().equals(cashAccount.getUUID())))
                continue;

            for (var transaction : portfolio.getTransactions())
            {
                if (fits(transaction, from, to, instrument, types))
                    rows.add(new Row(transaction, settlement, portfolio));
            }
        }

        var json = new JsonObject();
        paging.apply(Paging.Kind.TRANSACTIONS, json, "items", rows, Row::key, //$NON-NLS-1$
                        TransactionsHandler::transactionJson);

        return json;
    }

    /**
     * The scope a cursor is tied to: the period decides which rows the list
     * holds, while the filters that only say which of them to leave out do not,
     * so a page may be carried over from one instrument to another.
     */
    private static String scope(LocalDate from, LocalDate to)
    {
        return "transactions|" + (from == null ? "" : from) + "|" + (to == null ? "" : to); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private record Row(Transaction transaction, Account cashAccount, Portfolio investmentAccount)
    {
        /**
         * Sorts the rows oldest first and keeps a page continuable: the date and
         * the time are fixed width, and the uuid of a booking never repeats.
         */
        String key()
        {
            var at = transaction.getDateTime();
            return at.toLocalDate() + "|" + TIME.format(at) + "|" + transaction.getUUID(); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static JsonObject transactionJson(Row row)
    {
        var transaction = row.transaction();
        var at = transaction.getDateTime();

        var json = new JsonObject();
        json.addProperty("date", at.toLocalDate().toString()); //$NON-NLS-1$
        json.addProperty("time", TIME.format(at)); //$NON-NLS-1$
        json.addProperty("type", typeOf(transaction));
        json.addProperty("uuid", transaction.getUUID()); //$NON-NLS-1$

        var security = transaction.getOptionalSecurity();
        if (security.isPresent())
            json.add("instrument", reference(security.get().getUUID(), security.get().getName())); //$NON-NLS-1$

        json.add("amount", EntityJson.toJson(transaction.getMonetaryAmount())); //$NON-NLS-1$

        if (transaction.getShares() != 0)
            json.add("shares", EntityJson.decimal(transaction.getShares(), Values.Share.precision())); //$NON-NLS-1$

        // charges belong to the booking that incurred them, which for a purchase
        // or a sale is the one in the securities account, never the cash side
        if (transaction instanceof PortfolioTransaction purchase)
        {
            json.add("grossAmount", EntityJson.toJson(purchase.getGrossValue())); //$NON-NLS-1$

            var fees = purchase.getUnitSum(Transaction.Unit.Type.FEE);
            if (!fees.isZero())
                json.add("fees", EntityJson.toJson(fees)); //$NON-NLS-1$

            var taxes = purchase.getUnitSum(Transaction.Unit.Type.TAX);
            if (!taxes.isZero())
                json.add("taxes", EntityJson.toJson(taxes)); //$NON-NLS-1$
        }

        if (row.cashAccount() != null)
            json.add("cashAccount", reference(row.cashAccount().getUUID(), row.cashAccount().getName())); //$NON-NLS-1$
        if (row.investmentAccount() != null)
            json.add("investmentAccount", reference(row.investmentAccount().getUUID(), //$NON-NLS-1$
                            row.investmentAccount().getName()));

        var note = transaction.getNote();
        if (note != null && !note.isBlank())
            json.addProperty("note", note); //$NON-NLS-1$

        return json;
    }

    private static JsonObject reference(String uuid, String name)
    {
        var json = new JsonObject();
        json.addProperty("uuid", uuid); //$NON-NLS-1$
        json.addProperty("name", name); //$NON-NLS-1$
        return json;
    }

    private static String typeOf(Transaction transaction)
    {
        return transaction instanceof PortfolioTransaction purchase ? purchase.getType().name()
                        : ((AccountTransaction) transaction).getType().name();
    }

    private static boolean fits(Transaction transaction, LocalDate from, LocalDate to, Security instrument,
                    Set<String> types)
    {
        var day = transaction.getDateTime().toLocalDate();
        if (from != null && day.isBefore(from))
            return false;
        if (to != null && day.isAfter(to))
            return false;
        if (!types.isEmpty() && !types.contains(typeOf(transaction)))
            return false;

        if (instrument != null)
        {
            var security = transaction.getOptionalSecurity();
            if (security.isEmpty() || !security.get().getUUID().equals(instrument.getUUID()))
                return false;
        }
        return true;
    }

    /**
     * The types asked for, or an empty set when none were asked for. Both
     * families share their names where a booking means the same thing on either
     * side of a transfer, so the names of one family simply select nothing.
     */
    private static Set<String> types(String raw, List<ApiException.FieldError> errors)
    {
        if (raw == null || raw.isBlank())
            return Set.of();

        var allowed = new LinkedHashSet<String>();
        for (var type : AccountTransaction.Type.values())
            allowed.add(type.name());
        for (var type : PortfolioTransaction.Type.values())
            allowed.add(type.name());

        var chosen = new LinkedHashSet<String>();
        for (var part : raw.split(",")) //$NON-NLS-1$
        {
            var name = part.trim().toUpperCase(Locale.ROOT);
            if (name.isEmpty())
                continue;
            if (!allowed.contains(name))
            {
                errors.add(new ApiException.FieldError("type", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "type must be one of " + String.join(", ", allowed))); //$NON-NLS-1$ //$NON-NLS-2$
                return chosen;
            }
            chosen.add(name);
        }
        return chosen;
    }

    private static <T extends Named> T reference(String field, String raw, List<T> candidates,
                    Function<T, String> uuidOf, List<ApiException.FieldError> errors)
    {
        if (raw == null || raw.isBlank())
            return null;

        var trimmed = raw.trim();
        for (var candidate : candidates)
        {
            if (uuidOf.apply(candidate).equals(trimmed))
                return candidate;
        }

        errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        "no such account or instrument of this file - see pp_list_investment_accounts, " //$NON-NLS-1$
                                        + "pp_list_cash_accounts and pp_list_instruments")); //$NON-NLS-1$
        return null;
    }
}
