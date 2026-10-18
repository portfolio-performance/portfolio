package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;

/**
 * Core's converter never says that it found no rate: it falls back to 1.0. For
 * <code>performance</code>, whose figures core calculates, this looks up the
 * rates the figures of a period depend on, which notes a missing or stale one
 * for the warnings of {@link ExchangeRates}. A foreign currency is looked up on
 * the last day of the period if it is held at the start or used during the
 * period, and on the opening day if it is held then. A day after today is
 * replaced by today, as no rate is dated later.
 */
final class PerformanceRateCheck
{
    /** The warnings of <code>performance</code>: those of {@link ExchangeRates}, with its own count. */
    static final List<WarningSpec> WARNINGS = List.of(
                    new WarningSpec(ExchangeRates.MISSING.code(),
                                    "No usable exchange rate was found for a currency pair. Core calculates the figures, so it used its rate of 1.0 for the pair and they are unreliable. Carries baseCurrency, termCurrency and the count of days checked: the opening day and the last day of the period."),
                    new WarningSpec(ExchangeRates.STALE.code(),
                                    "An exchange rate more than " + Prices.STALE_AFTER_DAYS
                                                    + " days before the day checked was used. Carries baseCurrency, termCurrency, the count of days checked (the opening day and the last day of the period) and the maxAgeDays."),
                    ExchangeRates.CACHE_UNAVAILABLE);

    private PerformanceRateCheck()
    {
    }

    static void run(Client client, ExchangeRates rates, PerformancePeriod period, String currency, LocalDate today)
    {
        var heldAtOpening = heldOn(client, rates.converter(currency), period.openingDay());
        var neededAtEnd = new TreeSet<>(heldAtOpening);
        neededAtEnd.addAll(usedDuring(client, period));

        // the reporting currency needs no rate
        heldAtOpening.remove(currency);
        neededAtEnd.remove(currency);

        var openingDay = notAfter(period.openingDay(), today);
        var lastDay = notAfter(period.lastDay(), today);
        heldAtOpening.forEach(code -> rates.lookup(openingDay, code, currency));
        neededAtEnd.forEach(code -> rates.lookup(lastDay, code, currency));
    }

    private static LocalDate notAfter(LocalDate day, LocalDate limit)
    {
        return day.isAfter(limit) ? limit : day;
    }

    /** The currencies of the cash accounts with a balance and of the securities with shares on the day. */
    private static Set<String> heldOn(Client client, CurrencyConverter converter, LocalDate day)
    {
        var cash = client.getAccounts().stream().map(a -> AccountSnapshot.create(a, converter, day))
                        .filter(snapshot -> !snapshot.getUnconvertedFunds().isZero())
                        .map(snapshot -> snapshot.getAccount().getCurrencyCode());
        var securities = client.getPortfolios().stream()
                        .flatMap(p -> PortfolioSnapshot.create(p, converter, day).getPositionsBySecurity().keySet()
                                        .stream())
                        .map(Security::getCurrencyCode);

        return sorted(Stream.concat(cash, securities));
    }

    /** The currencies of the cash accounts and of the securities with a transaction in the period. */
    private static Set<String> usedDuring(Client client, PerformancePeriod period)
    {
        Predicate<Transaction> inPeriod = transaction -> period.contains(transaction.getDateTime().toLocalDate());

        var cash = client.getAccounts().stream().filter(a -> a.getTransactions().stream().anyMatch(inPeriod))
                        .map(Account::getCurrencyCode);
        var securities = Stream
                        .<Transaction>concat(client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream()),
                                        client.getAccounts().stream().flatMap(a -> a.getTransactions().stream()))
                        .filter(inPeriod).map(Transaction::getSecurity).filter(Objects::nonNull)
                        .map(Security::getCurrencyCode);

        return sorted(Stream.concat(cash, securities));
    }

    /** The distinct currency codes, in a stable order for the warnings; a security may have none. */
    private static Set<String> sorted(Stream<String> currencies)
    {
        return currencies.filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
    }
}
