package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;

/**
 * The {@code investmentAccount} and {@code cashAccount} query parameters of
 * the report endpoints (holdings, performance): narrows the file to the given
 * investment and cash accounts with the same {@link PortfolioClientFilter} the
 * application's reporting filter uses. An investment account brings its
 * reference cash account's security-related transactions along (a buy is
 * then an inbound delivery unless the cash account is selected as well).
 * <p/>
 * Both parameters take one UUID or a comma-separated list; without either
 * parameter the file is reported unfiltered.
 */
public final class ReportFilter
{
    public static final String INVESTMENT_ACCOUNT = "investmentAccount"; //$NON-NLS-1$
    public static final String CASH_ACCOUNT = "cashAccount"; //$NON-NLS-1$

    private ReportFilter()
    {
    }

    /**
     * The client to report on: the client itself without a filter, else a
     * read-only filtered copy. An unknown UUID is a {@code 400} with
     * {@code unknown-reference}.
     */
    public static Client apply(Client client, String investmentAccountParam, String cashAccountParam)
    {
        if (isAbsent(investmentAccountParam) && isAbsent(cashAccountParam))
            return client;

        var errors = new ArrayList<ApiException.FieldError>();
        var portfolios = resolve(INVESTMENT_ACCOUNT, investmentAccountParam, client.getPortfolios(),
                        Portfolio::getUUID, errors);
        var accounts = resolve(CASH_ACCOUNT, cashAccountParam, client.getAccounts(), Account::getUUID, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        return new PortfolioClientFilter(portfolios, accounts).filter(client);
    }

    private static boolean isAbsent(String param)
    {
        return param == null || param.isBlank();
    }

    private static <T> List<T> resolve(String name, String param, List<T> candidates, Function<T, String> uuid,
                    List<ApiException.FieldError> errors)
    {
        var result = new ArrayList<T>();
        if (isAbsent(param))
            return result;

        for (var wanted : Arrays.stream(param.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList()) //$NON-NLS-1$
        {
            var match = candidates.stream().filter(c -> uuid.apply(c).equals(wanted)).findFirst();
            if (match.isEmpty())
                errors.add(new ApiException.FieldError(name, "unknown-reference", //$NON-NLS-1$
                                MessageFormat.format("{0} is not a known {1}", wanted, name))); //$NON-NLS-1$
            else if (!result.contains(match.get()))
                result.add(match.get());
        }
        return result;
    }
}
