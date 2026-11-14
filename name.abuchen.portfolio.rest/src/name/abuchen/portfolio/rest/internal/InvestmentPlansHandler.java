package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;

/**
 * The savings plans of a file - what is scheduled to move, how often, and
 * between which two accounts. Nothing else on this API speaks of the future: a
 * holding states what is there, a transaction states what happened, and a plan
 * states what the file intends to do next, which is the one question a caller
 * cannot answer from any other read.
 * <p/>
 * A plan is a schedule, not a booking. What it has already executed is ordinary
 * transactions in the accounts it names, visible in
 * {@code pp_list_transactions} like any other booking; this endpoint counts
 * them and names the dates, but does not repeat them. And a plan is not
 * identified by a uuid - it is named. Nothing here can be written through this
 * API, so an identifier would only be a number without a use.
 * <p/>
 * The interval is stated as a number and a unit. The file keeps weekly plans as
 * a hundred and more, which is an internal trick no reader should have to know,
 * so a weekly plan arrives as its number of weeks with {@code intervalUnit}
 * {@code week}, and a monthly one as its number of months.
 */
public final class InvestmentPlansHandler
{
    /** what the file adds to a weekly interval to tell it from a monthly one */
    private static final int WEEKS_THRESHOLD = InvestmentPlan.WEEKS_THRESHOLD;

    private InvestmentPlansHandler()
    {
    }

    /**
     * The plans of one file, in the order the file keeps them, narrowed by
     * whichever of the three accounts or the instrument the caller names. There
     * is no paging: a file holds plans by the dozen, and the answer stays far
     * below what one response carries.
     *
     * @throws ApiException
     *             400 when a named account or instrument is not of this file
     */
    public static JsonObject list(Client client, String investmentAccountParam, String cashAccountParam,
                    String instrumentParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var investmentAccount = resolve("investmentAccount", "securities account", //$NON-NLS-1$ //$NON-NLS-2$
                        "pp_list_investment_accounts", investmentAccountParam, client.getPortfolios(), //$NON-NLS-1$
                        Portfolio::getUUID, errors);
        var cashAccount = resolve("cashAccount", "cash account", //$NON-NLS-1$ //$NON-NLS-2$
                        "pp_list_cash_accounts", cashAccountParam, client.getAccounts(), //$NON-NLS-1$
                        Account::getUUID, errors);
        var instrument = resolve("instrument", "instrument", //$NON-NLS-1$ //$NON-NLS-2$
                        "pp_list_instruments", instrumentParam, client.getSecurities(), //$NON-NLS-1$
                        Security::getUUID, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var items = new JsonArray();
        for (var plan : client.getPlans())
        {
            if (fits(plan, investmentAccount, cashAccount, instrument))
                items.add(planJson(plan));
        }

        var filtered = investmentAccount != null || cashAccount != null || instrument != null;
        return EntityJson.envelope(items, filtered
                        ? "no plan of this file matches the account or instrument asked for" //$NON-NLS-1$
                        : "this file has no investment plans"); //$NON-NLS-1$
    }

    private static JsonObject planJson(InvestmentPlan plan)
    {
        var json = new JsonObject();
        json.addProperty("name", plan.getName()); //$NON-NLS-1$
        json.addProperty("type", plan.getPlanType().name()); //$NON-NLS-1$

        var settlement = settlementOf(plan);
        if (settlement != null)
        {
            var currency = settlement.getCurrencyCode();
            json.add("amount", EntityJson.toJson(Money.of(currency, plan.getAmount()))); //$NON-NLS-1$
            if (plan.getFees() != 0)
                json.add("fees", EntityJson.toJson(Money.of(currency, plan.getFees()))); //$NON-NLS-1$
            if (plan.getTaxes() != 0)
                json.add("taxes", EntityJson.toJson(Money.of(currency, plan.getTaxes()))); //$NON-NLS-1$
        }

        var weekly = plan.getInterval() >= WEEKS_THRESHOLD;
        json.addProperty("interval", weekly ? plan.getInterval() - WEEKS_THRESHOLD : plan.getInterval()); //$NON-NLS-1$
        json.addProperty("intervalUnit", weekly ? "week" : "month"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        json.addProperty("start", plan.getStart().toString()); //$NON-NLS-1$
        json.addProperty("autoGenerate", plan.isAutoGenerate()); //$NON-NLS-1$
        json.addProperty("executions", plan.getTransactions().size()); //$NON-NLS-1$
        json.addProperty("nextExecution", plan.getDateOfNextTransactionToBeGenerated().toString()); //$NON-NLS-1$

        var last = plan.getLastDate();
        if (last.isPresent())
            json.addProperty("lastExecution", last.get().toString()); //$NON-NLS-1$

        if (plan.getSecurity() != null)
            json.add("instrument", //$NON-NLS-1$
                            namedReference(plan.getSecurity().getUUID(), plan.getSecurity().getName()));
        if (plan.getPortfolio() != null)
            json.add("investmentAccount", //$NON-NLS-1$
                            namedReference(plan.getPortfolio().getUUID(), plan.getPortfolio().getName()));
        if (settlement != null)
            json.add("cashAccount", namedReference(settlement.getUUID(), settlement.getName())); //$NON-NLS-1$

        if (plan.getNote() != null && !plan.getNote().isBlank())
            json.addProperty("note", plan.getNote()); //$NON-NLS-1$

        return json;
    }

    /**
     * The account the plan's money moves through. A plan that moves money
     * between accounts names its own; a plan that buys states no account at all
     * and takes its money from the settlement account of the securities account,
     * which is the same derivation a purchase booking rests on.
     */
    private static Account settlementOf(InvestmentPlan plan)
    {
        if (plan.getAccount() != null)
            return plan.getAccount();

        var portfolio = plan.getPortfolio();
        return portfolio != null ? portfolio.getReferenceAccount() : null;
    }

    private static boolean fits(InvestmentPlan plan, Portfolio investmentAccount, Account cashAccount,
                    Security instrument)
    {
        var portfolio = plan.getPortfolio();
        if (investmentAccount != null && (portfolio == null
                        || !investmentAccount.getUUID().equals(portfolio.getUUID())))
            return false;

        var security = plan.getSecurity();
        if (instrument != null && (security == null || !instrument.getUUID().equals(security.getUUID())))
            return false;

        var settlement = settlementOf(plan);
        if (cashAccount != null && (settlement == null || !cashAccount.getUUID().equals(settlement.getUUID())))
            return false;

        return true;
    }

    static JsonObject namedReference(String uuid, String name)
    {
        var json = new JsonObject();
        json.addProperty("uuid", uuid); //$NON-NLS-1$
        json.addProperty("name", name); //$NON-NLS-1$
        return json;
    }

    /**
     * One of the things a plan refers to, by uuid. The refusal names the list it
     * was looked for in and the tool that reads it: a reader sent to the wrong
     * list of the file wastes the round trip that brought it there.
     */
    /** uuid of one thing the caller named, out of a list of the file; shared with the watchlists */
    static <T> T resolve(String field, String what, String tool, String raw, List<T> candidates,
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

        var hint = "no " + what + " of this file has this uuid - see " + tool; //$NON-NLS-1$ //$NON-NLS-2$
        errors.add(new ApiException.FieldError(field, "invalid-value", hint)); //$NON-NLS-1$
        return null;
    }
}
