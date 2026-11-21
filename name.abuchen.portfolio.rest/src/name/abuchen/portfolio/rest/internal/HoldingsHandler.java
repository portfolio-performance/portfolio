package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.util.TextUtil;

public final class HoldingsHandler
{
    private HoldingsHandler()
    {
    }

    /**
     * Values all holdings at the given date (default: today) in the given
     * currency (default: the file's base currency). A currency without an
     * exchange rate series converts 1:1 - the same silent fallback the UI
     * applies - and the report names every such pair it actually converted in
     * {@code fxWarnings}, so an unconverted amount is never presented as a
     * converted one. The factory
     * is the host's per-file instance - it registers a listener on the client,
     * so this handler must not construct its own.
     *
     * Sent without either account parameter this is the whole file: one row per
     * vehicle, an instrument held in two investment accounts as one row.
     * {@code investmentAccount} or {@code cashAccount} narrows the report to that
     * one account, which is both the shorter answer and the only way to see where
     * a position sits. A scoped report states its scope once, in {@code scope},
     * and no column per row - the account is already known, and repeating a uuid
     * on every line is the more expensive way to say the same thing. What a scope
     * means for the numbers: {@code totalAssets} is that account's value, so
     * every {@code weight} is the share of it, exactly as an unscoped weight is
     * the share of the file.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String dateParam,
                    String currencyParam, String investmentAccountParam, String cashAccountParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var date = dateParam == null ? LocalDate.now() : CalcParams.date("date", dateParam, errors); //$NON-NLS-1$
        var currency = CalcParams.reportingCurrency(client, currencyParam, errors);
        var investmentAccount = resolve("investmentAccount", investmentAccountParam, //$NON-NLS-1$
                        client.getPortfolios(), Portfolio::getUUID, errors);
        var cashAccount = resolve("cashAccount", cashAccountParam, client.getAccounts(), //$NON-NLS-1$
                        Account::getUUID, errors);

        if (investmentAccount != null && cashAccount != null)
            errors.add(new ApiException.FieldError("investmentAccount", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "investmentAccount and cashAccount cannot be combined: scope the report to one account")); //$NON-NLS-1$

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var conversions = new ConversionRecorder(factory);
        var converter = conversions.wrap(new CurrencyConverterImpl(factory, currency));

        var report = investmentAccount != null ? scopedTo(converter, date, currency, investmentAccount)
                        : cashAccount != null ? scopedTo(converter, date, currency, cashAccount)
                                        : EntityJson.toJson(ClientSnapshot.create(client, converter, date), currency);

        FxWarnings.attach(report, conversions);
        return report;
    }

    /**
     * The report of one investment account: the positions of that one portfolio,
     * not the file's aggregate. The settlement cash of a portfolio is a cash
     * account of its own, so it is not among these rows; the scope names it, and
     * asking for it is one call with {@code cashAccount}.
     */
    private static JsonObject scopedTo(CurrencyConverter converter, LocalDate date, String currency,
                    Portfolio portfolio)
    {
        var positions = PortfolioSnapshot.create(portfolio, converter, date) //
                        .getPositions() //
                        .stream() //
                        .sorted(Comparator.comparing((SecurityPosition p) -> p.getInvestmentVehicle().getName(),
                                        TextUtil::compare)) //
                        .toList();

        // the total first: a weight is a share of this account, and the rows are
        // worth nothing until it is known
        var valuations = positions.stream() //
                        .map(position -> converter.convert(date, position.calculateValue())) //
                        .toList();
        var total = Money.of(currency, valuations.stream().mapToLong(Money::getAmount).sum());

        var items = new JsonArray();
        for (var i = 0; i < positions.size(); i++)
            items.add(EntityJson.holding(positions.get(i), valuations.get(i), total, date));

        var scope = new JsonObject();
        scope.addProperty("type", "investment-account"); //$NON-NLS-1$ //$NON-NLS-2$
        scope.addProperty("uuid", portfolio.getUUID()); //$NON-NLS-1$
        scope.addProperty("name", portfolio.getName()); //$NON-NLS-1$

        // where the money belonging to this account is kept, so that the caller
        // does not have to guess which cash account to ask for next
        var reference = portfolio.getReferenceAccount();
        if (reference != null)
        {
            var cash = new JsonObject();
            cash.addProperty("uuid", reference.getUUID()); //$NON-NLS-1$
            cash.addProperty("name", reference.getName()); //$NON-NLS-1$
            scope.add("referenceCashAccount", cash); //$NON-NLS-1$
        }

        return report(date, currency, scope, total, items);
    }

    /** the balance of one cash account, stated as a holding row like every other */
    private static JsonObject scopedTo(CurrencyConverter converter, LocalDate date, String currency, Account account)
    {
        var snapshot = AccountSnapshot.create(account, converter, date);
        var funds = snapshot.getFunds();

        var items = new JsonArray();
        items.add(EntityJson.holding(new SecurityPosition(snapshot), funds, funds, date));

        var scope = new JsonObject();
        scope.addProperty("type", "cash-account"); //$NON-NLS-1$ //$NON-NLS-2$
        scope.addProperty("uuid", account.getUUID()); //$NON-NLS-1$
        scope.addProperty("name", account.getName()); //$NON-NLS-1$

        return report(date, currency, scope, funds, items);
    }

    /**
     * The shape an unscoped report has, with the scope stated once at the top.
     * Without a scope this key is absent and the answer is what it always was.
     */
    private static JsonObject report(LocalDate date, String currency, JsonObject scope, Money total, JsonArray items)
    {
        var json = new JsonObject();
        json.addProperty("date", date.toString()); //$NON-NLS-1$
        json.addProperty("reportingCurrency", currency); //$NON-NLS-1$
        json.add("scope", scope); //$NON-NLS-1$
        json.add("totalAssets", EntityJson.toJson(total)); //$NON-NLS-1$
        json.add("items", items); //$NON-NLS-1$
        return json;
    }

    /**
     * A query parameter naming one account of this file, by uuid. The bound is
     * {@link Named} and not an investment vehicle because {@link Portfolio} is
     * not one - only the read-only view of it is - so the uuid arrives as a
     * method reference rather than through the bound.
     */
    private static <T extends Named> T resolve(String field, String raw, List<T> candidates, Function<T, String> uuidOf,
                    List<ApiException.FieldError> errors)
    {
        if (raw == null)
            return null;

        var trimmed = raw.trim();
        for (var candidate : candidates)
        {
            if (uuidOf.apply(candidate).equals(trimmed))
                return candidate;
        }

        errors.add(new ApiException.FieldError(field, "invalid-value", //$NON-NLS-1$
                        "no account of this file has this uuid - see pp_list_investment_accounts " //$NON-NLS-1$
                                        + "and pp_list_cash_accounts")); //$NON-NLS-1$
        return null;
    }
}
