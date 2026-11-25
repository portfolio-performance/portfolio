package name.abuchen.portfolio.snapshot.advancelumpsum;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

/**
 * Estimates the German Vorabpauschale (§ 18 InvStG) per portfolio and security
 * for one calendar year. Only securities with a partial exemption attribute
 * (Teilfreistellung) are treated as investment funds. The calculation does not
 * create any transactions.
 */
public class AdvanceLumpSumCalculation
{
    public record Item(Portfolio portfolio, Security security, long shares, double partialExemption,
                    Quote priceStart, Quote priceEnd, Quote distributionsPerShare, Quote baseYieldPerShare,
                    Quote advanceLumpSumPerShare, Money advanceLumpSum, Money taxableAmount, Money tax,
                    boolean hasMissingPrices, boolean hasUnresolvedDistributions, boolean hasMissingExchangeRates)
    {
    }

    public record Result(String currencyCode, List<Item> items)
    {
        public Map<Portfolio, List<Item>> getItemsByPortfolio()
        {
            var answer = new LinkedHashMap<Portfolio, List<Item>>();
            for (var item : items)
                answer.computeIfAbsent(item.portfolio(), p -> new ArrayList<>()).add(item);
            return answer;
        }

        public Money getTotalAdvanceLumpSum()
        {
            return sum(items, Item::advanceLumpSum, currencyCode);
        }

        public Money getTotalTaxableAmount()
        {
            return sum(items, Item::taxableAmount, currencyCode);
        }

        public Money getTotalTax()
        {
            return sum(items, Item::tax, currencyCode);
        }

        public static Money sum(List<Item> items, Function<Item, Money> value, String currencyCode)
        {
            return items.stream().map(value).collect(MoneyCollectors.sum(currencyCode));
        }
    }

    /** factor according to § 18 Abs. 1 Satz 2 InvStG */
    private static final BigDecimal BASE_YIELD_FACTOR = new BigDecimal("0.7"); //$NON-NLS-1$

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final MathContext MC = MathContext.DECIMAL64;

    /** brokers may credit the same distribution on different days */
    private static final int AMBIGUOUS_PAYMENT_WINDOW_DAYS = 31;

    private record Lot(LocalDate acquired, long shares)
    {
    }

    private record Booking(Account account, AccountTransaction transaction)
    {
    }

    private record Distributions(BigDecimal perShare, boolean unresolved, boolean missingRates)
    {
    }

    private final Client client;
    private final CurrencyConverter converter;

    public AdvanceLumpSumCalculation(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter.with(CurrencyUnit.EUR);
    }

    /**
     * Calculates the Vorabpauschale for the given year.
     *
     * @param baseInterestRate
     *            Basiszins of the year
     * @param taxRate
     *            tax rate as fraction, e.g. 0.26375
     * @param valuationDate
     *            cutoff for transactions and end prices. For the current year,
     *            today's date yields an estimate.
     */
    public Result calculate(BaseInterestRate baseInterestRate, BigDecimal taxRate, LocalDate valuationDate)
    {
        var year = baseInterestRate.year();
        var currencyCode = converter.getTermCurrency();
        var items = new ArrayList<Item>();

        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType();
        if (attribute.isEmpty())
            return new Result(currencyCode, items);

        var yearEnd = LocalDate.of(year, 12, 31);
        var cutoff = valuationDate.isAfter(yearEnd) ? yearEnd : valuationDate;
        var dividendBookings = collectDividendBookings(year, cutoff);
        var holdings = new HashMap<Security, NavigableMap<LocalDate, Long>>();
        dividendBookings.forEach((security, bookings) -> {
            if (bookings.stream().anyMatch(b -> b.transaction().getShares() <= 0))
                holdings.put(security, new TreeMap<>());
        });
        var lotsByPortfolio = collectLots(attribute.get(), cutoff, holdings);
        var distributionsBySecurity = new HashMap<Security, Distributions>();

        for (var portfolio : client.getPortfolios())
        {
            var lotsBySecurity = lotsByPortfolio.get(portfolio);
            if (lotsBySecurity == null)
                continue;

            lotsBySecurity.entrySet().stream() //
                            .filter(e -> !e.getValue().isEmpty())
                            .sorted(Comparator.comparing(e -> e.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
                            .forEach(e -> {
                                var security = e.getKey();
                                var distributions = distributionsBySecurity.computeIfAbsent(security,
                                                s -> distributionsPerShare(dividendBookings.getOrDefault(s, List.of()),
                                                                holdings.get(s)));
                                var item = calculate(portfolio, security, e.getValue(), attribute.get(),
                                                baseInterestRate, taxRate, cutoff, distributions);
                                if (item != null)
                                    items.add(item);
                            });
        }

        return new Result(currencyCode, items);
    }

    private Map<Portfolio, Map<Security, Deque<Lot>>> collectLots(AttributeType attribute, LocalDate valuationDate,
                    Map<Security, NavigableMap<LocalDate, Long>> holdings)
    {
        var transactions = new ArrayList<PortfolioTransaction>();
        var owners = new HashMap<PortfolioTransaction, Portfolio>();

        for (var portfolio : client.getPortfolios())
        {
            for (var tx : portfolio.getTransactions())
            {
                if (tx.getSecurity() == null || tx.getDateTime().toLocalDate().isAfter(valuationDate))
                    continue;
                if (!(tx.getSecurity().getAttributes().get(attribute) instanceof Double))
                    continue;

                transactions.add(tx);
                owners.put(tx, portfolio);
            }
        }

        transactions.sort(Comparator.comparing(PortfolioTransaction::getDateTime)
                        .thenComparingInt(tx -> switch (tx.getType())
                        {
                            case BUY, DELIVERY_INBOUND, TRANSFER_IN -> 0;
                            case TRANSFER_OUT -> 1;
                            case SELL, DELIVERY_OUTBOUND -> 2;
                        }));

        var answer = new HashMap<Portfolio, Map<Security, Deque<Lot>>>();

        for (var tx : transactions)
        {
            var portfolio = owners.get(tx);
            var date = tx.getDateTime().toLocalDate();
            var history = holdings.get(tx.getSecurity());
            if (history != null)
            {
                var previous = history.isEmpty() ? 0L : history.lastEntry().getValue();
                history.put(date, previous + (tx.getType().isPurchase() ? tx.getShares() : -tx.getShares()));
            }

            switch (tx.getType())
            {
                case BUY, DELIVERY_INBOUND:
                    lots(answer, portfolio, tx.getSecurity()).add(new Lot(date, tx.getShares()));
                    break;
                case SELL, DELIVERY_OUTBOUND:
                    remove(lots(answer, portfolio, tx.getSecurity()), tx.getShares());
                    break;
                case TRANSFER_OUT:
                    var removed = remove(lots(answer, portfolio, tx.getSecurity()), tx.getShares());
                    if (tx.getCrossEntry() instanceof PortfolioTransferEntry entry && !removed.isEmpty())
                    {
                        var target = lots(answer, entry.getTargetPortfolio(), tx.getSecurity());
                        var merged = new ArrayList<>(target);
                        merged.addAll(removed);
                        merged.sort(Comparator.comparing(Lot::acquired));
                        target.clear();
                        target.addAll(merged);
                    }
                    break;
                case TRANSFER_IN:
                    // lots are moved with the original acquisition date when
                    // processing the outbound side of the transfer
                    if (!(tx.getCrossEntry() instanceof PortfolioTransferEntry))
                        lots(answer, portfolio, tx.getSecurity()).add(new Lot(date, tx.getShares()));
                    break;
                default:
                    throw new IllegalArgumentException(tx.getType().name());
            }
        }

        return answer;
    }

    private static Deque<Lot> lots(Map<Portfolio, Map<Security, Deque<Lot>>> lots, Portfolio portfolio,
                    Security security)
    {
        return lots.computeIfAbsent(portfolio, p -> new HashMap<>()).computeIfAbsent(security,
                        s -> new ArrayDeque<>());
    }

    private static List<Lot> remove(Deque<Lot> lots, long shares)
    {
        var removed = new ArrayList<Lot>();
        var remaining = shares;

        while (remaining > 0 && !lots.isEmpty())
        {
            var lot = lots.pollFirst();
            if (lot.shares() <= remaining)
            {
                removed.add(lot);
                remaining -= lot.shares();
            }
            else
            {
                removed.add(new Lot(lot.acquired(), remaining));
                lots.addFirst(new Lot(lot.acquired(), lot.shares() - remaining));
                remaining = 0;
            }
        }

        return removed;
    }

    private Item calculate(Portfolio portfolio, Security security, Deque<Lot> lots, AttributeType attribute,
                    BaseInterestRate baseInterestRate, BigDecimal taxRate, LocalDate valuationDate,
                    Distributions paidDistributions)
    {
        var shares = lots.stream().mapToLong(Lot::shares).sum();
        if (shares <= 0)
            return null;

        var year = baseInterestRate.year();
        var partialExemption = (Double) security.getAttributes().get(attribute);

        var startDate = LocalDate.of(year, 1, 1);
        var firstPrice = security.getPricesIncludingLatest().stream()
                        .filter(p -> !p.getDate().isBefore(startDate) && !p.getDate().isAfter(valuationDate))
                        .filter(p -> p.getValue() > 0).findFirst().orElse(null);
        var openingPrice = security.getSecurityPrice(startDate.minusDays(1));
        if ((firstPrice != null && firstPrice.getDate().equals(startDate))
                        || openingPrice.getDate().getYear() != year - 1 || openingPrice.getValue() <= 0)
            openingPrice = firstPrice;
        var lastPrice = security.getSecurityPrice(valuationDate);
        var hasMissingPrices = firstPrice == null || lastPrice.getDate().isBefore(startDate)
                        || lastPrice.getDate().isAfter(valuationDate) || lastPrice.getValue() <= 0;
        var openingValue = convertPrice(security, openingPrice);
        var firstValue = openingPrice == firstPrice ? openingValue : convertPrice(security, firstPrice);
        var lastValue = convertPrice(security, hasMissingPrices ? null : lastPrice);
        var missingRates = openingValue.isEmpty() || firstValue.isEmpty() || lastValue.isEmpty()
                        || paidDistributions.missingRates();
        var priceStart = openingValue.orElse(BigDecimal.ZERO);
        var priceEnd = lastValue.orElse(BigDecimal.ZERO);
        var distributions = paidDistributions.perShare();

        var baseYield = BigDecimal.ZERO;
        var advanceLumpSumPerShare = BigDecimal.ZERO;

        if (baseInterestRate.rate().signum() > 0 && !hasMissingPrices && !missingRates && !paidDistributions.unresolved())
        {
            baseYield = priceStart.multiply(baseInterestRate.rate(), MC).movePointLeft(2)
                            .multiply(BASE_YIELD_FACTOR, MC);

            // § 18 Abs. 1 Satz 3 InvStG uses two prices from the calendar year.
            var gain = priceEnd.subtract(firstValue.orElseThrow()).add(distributions).max(BigDecimal.ZERO);
            advanceLumpSumPerShare = baseYield.min(gain).subtract(distributions).max(BigDecimal.ZERO);
        }

        // § 18 Abs. 2 InvStG: reduced by one twelfth for each full month
        // preceding the month of acquisition
        var advanceLumpSum = BigDecimal.ZERO;
        for (var lot : lots)
        {
            var value = advanceLumpSumPerShare.multiply(BigDecimal.valueOf(lot.shares()), MC)
                            .movePointLeft(Values.Share.precision());

            if (lot.acquired().getYear() == year)
                value = value.multiply(BigDecimal.valueOf(13L - lot.acquired().getMonthValue()), MC).divide(TWELVE,
                                MC);

            advanceLumpSum = advanceLumpSum.add(value);
        }

        var taxable = advanceLumpSum.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(partialExemption)), MC);
        var tax = taxable.multiply(taxRate, MC);

        var currencyCode = converter.getTermCurrency();
        return new Item(portfolio, security, shares, partialExemption, //
                        toQuote(currencyCode, priceStart), toQuote(currencyCode, priceEnd),
                        toQuote(currencyCode, distributions), toQuote(currencyCode, baseYield),
                        toQuote(currencyCode, advanceLumpSumPerShare), //
                        toMoney(currencyCode, advanceLumpSum), toMoney(currencyCode, taxable),
                        toMoney(currencyCode, tax), hasMissingPrices, paidDistributions.unresolved(),
                        missingRates);
    }

    private Optional<BigDecimal> convertPrice(Security security, SecurityPrice price)
    {
        if (price == null)
            return Optional.of(BigDecimal.ZERO);
        return convert(price.getDate(), security.getCurrencyCode(), price.getValue(), Values.Quote.precision());
    }

    private Optional<BigDecimal> convert(LocalDate date, String currency, long amount, int precision)
    {
        return converter.findRate(date, currency)
                        .map(r -> r.getValue().multiply(BigDecimal.valueOf(amount)).setScale(0, RoundingMode.HALF_DOWN)
                                        .movePointLeft(precision));
    }

    private Map<Security, List<Booking>> collectDividendBookings(int year, LocalDate valuationDate)
    {
        var bookings = new HashMap<Security, List<Booking>>();
        for (var account : client.getAccounts())
        {
            for (var tx : account.getTransactions())
            {
                if (tx.getType() != AccountTransaction.Type.DIVIDENDS || tx.getSecurity() == null)
                    continue;

                var date = tx.getDateTime().toLocalDate();
                // § 18 Abs. 1 InvStG counts distributions paid within the calendar year.
                if (date.getYear() == year && !date.isAfter(valuationDate))
                    bookings.computeIfAbsent(tx.getSecurity(), s -> new ArrayList<>()).add(new Booking(account, tx));
            }
        }
        return bookings;
    }

    private Distributions distributionsPerShare(List<Booking> bookings, NavigableMap<LocalDate, Long> holdings)
    {
        record DistributionDate(LocalDate date, boolean exDate)
        {
            LocalDate holdingDate()
            {
                return exDate ? date.minusDays(1) : date;
            }
        }

        record Payment(BigDecimal amount, long shares, boolean withoutShares)
        {
        }

        record AssignedBooking(Account account, LocalDate paymentDate, DistributionDate distributionDate)
        {
        }

        // A booking without an ex-date can join a same-day booking with an
        // ex-date, but only if that date identifies a single distribution.
        var exDatesByPaymentDate = new HashMap<LocalDate, Optional<LocalDate>>();
        for (var booking : bookings)
        {
            var tx = booking.transaction();
            if (tx.getExDate() != null && !tx.getGrossValue().isZero())
                exDatesByPaymentDate.merge(tx.getDateTime().toLocalDate(), Optional.of(tx.getExDate().toLocalDate()),
                                (a, b) -> a.equals(b) ? a : Optional.empty());
        }

        var payments = new HashMap<DistributionDate, Payment>();
        var assigned = new ArrayList<AssignedBooking>();
        var missingRates = false;
        for (var booking : bookings)
        {
            var tx = booking.transaction();
            var grossValue = tx.getGrossValue();
            if (grossValue.isZero())
                continue;

            var paymentDate = tx.getDateTime().toLocalDate();
            var amount = convert(paymentDate, grossValue.getCurrencyCode(), grossValue.getAmount(),
                            Values.Amount.precision());
            missingRates |= amount.isEmpty();

            var exDate = tx.getExDate() != null ? Optional.of(tx.getExDate().toLocalDate())
                            : exDatesByPaymentDate.getOrDefault(paymentDate, Optional.empty());
            var distributionDate = new DistributionDate(exDate.orElse(paymentDate), exDate.isPresent());
            assigned.add(new AssignedBooking(booking.account(), paymentDate, distributionDate));
            payments.merge(distributionDate,
                            new Payment(amount.orElse(BigDecimal.ZERO), tx.getShares(), tx.getShares() <= 0),
                            (a, b) -> new Payment(a.amount().add(b.amount()), a.shares() + b.shares(),
                                            a.withoutShares() || b.withoutShares()));
        }

        var answer = BigDecimal.ZERO;
        var unresolved = false;
        for (var entry : payments.entrySet())
        {
            var key = entry.getKey();
            var accounts = assigned.stream().filter(b -> b.distributionDate().equals(key)).map(AssignedBooking::account)
                            .toList();
            // Without an ex-date, a nearby booking of an account missing from
            // this group may belong to the same distribution. Nearby groups
            // with the same accounts still add up correctly per share, as
            // every account books each distribution once.
            var ambiguous = !key.exDate() && assigned.stream()
                            .anyMatch(b -> !b.distributionDate().equals(key) && !accounts.contains(b.account())
                                            && Math.abs(ChronoUnit.DAYS.between(key.date(),
                                                            b.paymentDate())) <= AMBIGUOUS_PAYMENT_WINDOW_DAYS);
            if (ambiguous)
            {
                unresolved = true;
                continue;
            }

            var payment = entry.getValue();
            long shares = payment.shares();
            if (payment.withoutShares())
            {
                var holding = holdings.floorEntry(key.holdingDate());
                shares = holding == null ? 0 : holding.getValue();
            }
            if (shares <= 0)
            {
                unresolved = true;
                continue;
            }

            answer = answer.add(payment.amount().multiply(Values.Share.getBigDecimalFactor())
                            .divide(BigDecimal.valueOf(shares), MC));
        }
        return new Distributions(unresolved || missingRates ? BigDecimal.ZERO : answer, unresolved, missingRates);
    }

    private static Quote toQuote(String currencyCode, BigDecimal value)
    {
        return Quote.of(currencyCode, value.movePointRight(Values.Quote.precision())
                        .setScale(0, RoundingMode.HALF_UP).longValue());
    }

    private static Money toMoney(String currencyCode, BigDecimal value)
    {
        return Money.of(currencyCode, value.movePointRight(Values.Amount.precision())
                        .setScale(0, RoundingMode.HALF_UP).longValue());
    }
}
