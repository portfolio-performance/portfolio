package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

/** Rules about an instrument's prices that more than one command relies on. */
final class Prices
{
    /** A latest price older than this many calendar days is stale. */
    static final int STALE_AFTER_DAYS = 7;

    static final WarningSpec STALE_PRICE = new WarningSpec("stale-price",
                    "The latest price is more than " + STALE_AFTER_DAYS
                                    + " days old. Not emitted for retired instruments, instruments with a manual quote feed or instruments without a price.");

    private Prices()
    {
    }

    /**
     * The price valuation uses on the given day: the feed's latest price when
     * it is current, otherwise the last stored price. Empty if no price is
     * dated on or before that day, where core would return a zero placeholder or
     * a future-dated price.
     */
    static Optional<SecurityPrice> latest(Security security, LocalDate asOf)
    {
        var hasPriceUpToToday = security.getPricesIncludingLatest().stream()
                        .anyMatch(price -> !price.getDate().isAfter(asOf));
        return hasPriceUpToToday ? Optional.of(security.getSecurityPrice(asOf)) : Optional.empty();
    }

    /**
     * Whether the latest price is stale: more than {@link #STALE_AFTER_DAYS} days
     * before the given day, unless the instrument is retired or its quote feed is
     * manual. This mirrors the warning colour of the securities table.
     */
    static boolean isStale(Security security, SecurityPrice latest, LocalDate asOf)
    {
        if (security.isRetired())
            return false;

        var feed = security.getLatestFeed() != null ? security.getLatestFeed() : security.getFeed();
        if (QuoteFeed.MANUAL.equals(feed))
            return false;

        return latest.getDate().isBefore(asOf.minusDays(STALE_AFTER_DAYS));
    }

    /** The {@link #STALE_PRICE} warning, if the latest price is stale. */
    static Optional<Warning> staleWarning(Security security, SecurityPrice latest, LocalDate asOf)
    {
        if (!isStale(security, latest, asOf))
            return Optional.empty();

        var ageDays = ChronoUnit.DAYS.between(latest.getDate(), asOf);
        return Optional.of(new Warning(STALE_PRICE.code(),
                        "The latest price of '" + MasterData.orEmpty(security.getName()) + "' is from "
                                        + latest.getDate() + ", " + ageDays + " days before " + asOf + ".")
                                        .with("instrument", security.getUUID()).with("date", latest.getDate().toString())
                                        .with("ageDays", ageDays));
    }
}
