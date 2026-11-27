package name.abuchen.portfolio.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyClient;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

/**
 * Resolves dynamic formula-based {@link LimitPrice} definitions (FIFO purchase
 * price multiples and average position value multiples) into concrete quotes in
 * the target security's currency, caching portfolio snapshots and per-security
 * FIFO cost calculations across table renders and sorts.
 */
public class LimitPriceResolver
{
    public enum Status
    {
        RESOLVED,
        NO_HOLDINGS,
        UNKNOWN_GROUP,
        EMPTY_GROUP
    }

    public record Resolution(Status status, OptionalLong resolvedQuote, OptionalLong fifoPurchaseQuote,
                    Money groupAverageValue, int groupCount, String matchedAttributeLabel)
    {
    }

    private record GroupStats(Status status, BigDecimal meanBaseAmountCents, Money averageMoney, int count,
                    String attributeLabel)
    {
    }

    private static final class SnapshotCache
    {
        private final LocalDate date;
        private final CurrencyConverter baseConverter;
        private final Map<Security, SecurityPosition> heldPositions;
        private final ConcurrentHashMap<Security, OptionalLong> fifoQuoteBySecurity = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, GroupStats> groupStatsByKey = new ConcurrentHashMap<>();

        private SnapshotCache(LocalDate date, CurrencyConverter baseConverter,
                        Map<Security, SecurityPosition> heldPositions)
        {
            this.date = date;
            this.baseConverter = baseConverter;
            this.heldPositions = heldPositions;
        }
    }

    private static final class WeakInvalidationListener implements PropertyChangeListener
    {
        private final Client client;
        private final WeakReference<LimitPriceResolver> resolverRef;

        private WeakInvalidationListener(Client client, LimitPriceResolver resolver)
        {
            this.client = client;
            this.resolverRef = new WeakReference<>(resolver);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            var resolver = resolverRef.get();
            if (resolver == null)
                client.removePropertyChangeListener(this);
            else
                resolver.invalidate();
        }
    }

    private final Client unwrappedClient;
    private final ExchangeRateProviderFactory exchangeRateFactory;
    private final CurrencyConverter fixedConverter;
    private final LocalDate fixedDate;

    private volatile SnapshotCache cache;

    public LimitPriceResolver(Client client)
    {
        this.unwrappedClient = ReadOnlyClient.unwrap(Objects.requireNonNull(client));
        this.exchangeRateFactory = new ExchangeRateProviderFactory(this.unwrappedClient);
        this.fixedConverter = null;
        this.fixedDate = null;

        this.unwrappedClient.addPropertyChangeListener(new WeakInvalidationListener(this.unwrappedClient, this));
    }

    public LimitPriceResolver(Client client, CurrencyConverter converter, LocalDate date)
    {
        this.unwrappedClient = ReadOnlyClient.unwrap(Objects.requireNonNull(client));
        this.exchangeRateFactory = null;
        this.fixedConverter = Objects.requireNonNull(converter).with(this.unwrappedClient.getBaseCurrency());
        this.fixedDate = Objects.requireNonNull(date);

        this.unwrappedClient.addPropertyChangeListener(new WeakInvalidationListener(this.unwrappedClient, this));
    }

    public void invalidate()
    {
        this.cache = null;
    }

    public OptionalLong resolveQuote(LimitPrice limit, Security security)
    {
        Objects.requireNonNull(limit);

        if (limit.isStatic())
            return OptionalLong.of(limit.getValue());

        if (security == null)
            return OptionalLong.empty();

        return resolve(limit, security).resolvedQuote();
    }

    public Resolution resolve(LimitPrice limit, Security security)
    {
        Objects.requireNonNull(limit);

        if (limit.isStatic())
            return new Resolution(Status.RESOLVED, OptionalLong.of(limit.getValue()), OptionalLong.empty(), null, 0,
                            null);

        if (security == null || security.getCurrencyCode() == null)
            return new Resolution(Status.NO_HOLDINGS, OptionalLong.empty(), OptionalLong.empty(), null, 0, null);

        var activeCache = getCache();

        switch (limit.getTargetMetric())
        {
            case PURCHASE_PRICE_FIFO:
                return resolvePurchasePriceFifo(activeCache, limit, security);
            case AVG_POSITION_VALUE:
                return resolveAvgPositionValue(activeCache, limit, security);
            case STATIC_PRICE:
            default:
                return new Resolution(Status.RESOLVED, OptionalLong.of(limit.getValue()), OptionalLong.empty(), null, 0,
                                null);
        }
    }

    private Resolution resolvePurchasePriceFifo(SnapshotCache activeCache, LimitPrice limit, Security security)
    {
        var fifoQuote = activeCache.fifoQuoteBySecurity.computeIfAbsent(security,
                        sec -> computeFifoPurchaseQuote(activeCache, sec));

        if (fifoQuote.isEmpty())
            return new Resolution(Status.NO_HOLDINGS, OptionalLong.empty(), OptionalLong.empty(), null, 0, null);

        long triggerQuote = BigDecimal.valueOf(fifoQuote.getAsLong())
                        .multiply(BigDecimal.valueOf(limit.getFactor()), Values.MC)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();

        return new Resolution(Status.RESOLVED, OptionalLong.of(triggerQuote), fifoQuote, null, 0, null);
    }

    private OptionalLong computeFifoPurchaseQuote(SnapshotCache activeCache, Security security)
    {
        if (security.getCurrencyCode() == null)
            return OptionalLong.empty();

        var held = activeCache.heldPositions.get(security);
        if (held == null || held.getShares() <= 0)
            return OptionalLong.empty();

        var secConverter = activeCache.baseConverter.with(security.getCurrencyCode());
        if (secConverter == null)
            return OptionalLong.empty();

        var filtered = new ClientSecurityFilter(security).filter(unwrappedClient);
        var record = LazySecurityPerformanceSnapshot
                        .create(filtered, secConverter, Interval.of(LocalDate.MIN, activeCache.date))
                        .getRecord(security);

        if (record.isEmpty() || record.get().getSharesHeld() <= 0)
            return OptionalLong.empty();

        var purchaseQuote = record.get().getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED);
        return purchaseQuote.isZero() ? OptionalLong.empty() : OptionalLong.of(purchaseQuote.getAmount());
    }

    private Resolution resolveAvgPositionValue(SnapshotCache activeCache, LimitPrice limit, Security security)
    {
        var rawGroup = limit.getGroupFilter();
        var cacheKey = rawGroup == null ? "" : rawGroup.trim().toLowerCase(Locale.ROOT); //$NON-NLS-1$

        var stats = activeCache.groupStatsByKey.computeIfAbsent(cacheKey,
                        k -> computeGroupStats(activeCache, rawGroup));

        if (stats.status() != Status.RESOLVED)
            return new Resolution(stats.status(), OptionalLong.empty(), OptionalLong.empty(), stats.averageMoney(),
                            stats.count(), stats.attributeLabel());

        var targetPos = activeCache.heldPositions.get(security);
        if (targetPos == null || targetPos.getShares() <= 0)
            return new Resolution(Status.NO_HOLDINGS, OptionalLong.empty(), OptionalLong.empty(), stats.averageMoney(),
                            stats.count(), stats.attributeLabel());

        var baseCurrency = activeCache.baseConverter.getTermCurrency();
        var secCurrency = security.getCurrencyCode();

        var fxRate = BigDecimal.ONE;
        if (!baseCurrency.equals(secCurrency))
        {
            var secConverter = activeCache.baseConverter.with(secCurrency);
            if (secConverter == null)
                return new Resolution(Status.NO_HOLDINGS, OptionalLong.empty(), OptionalLong.empty(),
                                stats.averageMoney(), stats.count(), stats.attributeLabel());

            var rate = secConverter.getRate(activeCache.date, baseCurrency);
            fxRate = rate.getValue();
        }

        var targetPositionValueSecCents = stats.meanBaseAmountCents()
                        .multiply(fxRate, Values.MC)
                        .multiply(BigDecimal.valueOf(limit.getFactor()), Values.MC);

        int scaleShift = Values.Share.precision() + Values.Quote.precisionDeltaToMoney();
        long triggerQuote = targetPositionValueSecCents
                        .movePointRight(scaleShift)
                        .divide(BigDecimal.valueOf(targetPos.getShares()), Values.MC)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();

        return new Resolution(Status.RESOLVED, OptionalLong.of(triggerQuote), OptionalLong.empty(),
                        stats.averageMoney(), stats.count(), stats.attributeLabel());
    }

    private GroupStats computeGroupStats(SnapshotCache activeCache, String rawGroupFilter)
    {
        List<SecurityPosition> peerPositions;
        String attributeLabel = null;

        if (rawGroupFilter == null || rawGroupFilter.isBlank())
        {
            peerPositions = new ArrayList<>(activeCache.heldPositions.values());
        }
        else
        {
            var attrOpt = findBooleanSecurityAttribute(rawGroupFilter);
            if (attrOpt.isEmpty())
                return new GroupStats(Status.UNKNOWN_GROUP, BigDecimal.ZERO, null, 0, rawGroupFilter.trim());

            var attr = attrOpt.get();
            attributeLabel = attr.getColumnLabel() != null && !attr.getColumnLabel().isBlank()
                            ? attr.getColumnLabel().trim()
                            : attr.getName();

            peerPositions = activeCache.heldPositions.values().stream()
                            .filter(p -> Boolean.TRUE.equals(p.getSecurity().getAttributes().get(attr)))
                            .toList();
        }

        if (peerPositions.isEmpty())
            return new GroupStats(Status.EMPTY_GROUP, BigDecimal.ZERO, null, 0, attributeLabel);

        var totalBaseCents = BigDecimal.ZERO;
        for (var position : peerPositions)
        {
            var valInBase = activeCache.baseConverter.convert(activeCache.date, position.calculateValue());
            totalBaseCents = totalBaseCents.add(BigDecimal.valueOf(valInBase.getAmount()));
        }

        int count = peerPositions.size();
        var meanBaseCents = totalBaseCents.divide(BigDecimal.valueOf(count), Values.MC);
        long roundedMeanCents = meanBaseCents.setScale(0, RoundingMode.HALF_UP).longValueExact();
        var avgMoney = Money.of(activeCache.baseConverter.getTermCurrency(), roundedMeanCents);

        return new GroupStats(Status.RESOLVED, meanBaseCents, avgMoney, count, attributeLabel);
    }

    private Optional<AttributeType> findBooleanSecurityAttribute(String filter)
    {
        var needle = filter.trim();

        // First prefer exact columnLabel match, then exact name match
        // (both case-insensitive and trimmed)
        var byColumnLabel = unwrappedClient.getSettings().getAttributeTypes()
                        .filter(a -> a.supports(Security.class) && a.getType() == Boolean.class)
                        .filter(a -> matchesIgnoreCase(a.getColumnLabel(), needle))
                        .findFirst();

        if (byColumnLabel.isPresent())
            return byColumnLabel;

        return unwrappedClient.getSettings().getAttributeTypes()
                        .filter(a -> a.supports(Security.class) && a.getType() == Boolean.class)
                        .filter(a -> matchesIgnoreCase(a.getName(), needle))
                        .findFirst();
    }

    private static boolean matchesIgnoreCase(String candidate, String needle)
    {
        return candidate != null && candidate.trim().equalsIgnoreCase(needle);
    }

    private SnapshotCache getCache()
    {
        var effectiveDate = fixedDate != null ? fixedDate : LocalDate.now();
        var current = this.cache;
        if (current != null && current.date.equals(effectiveDate)
                        && current.baseConverter.getTermCurrency().equals(unwrappedClient.getBaseCurrency()))
        {
            return current;
        }

        synchronized (this)
        {
            current = this.cache;
            if (current != null && current.date.equals(effectiveDate)
                            && current.baseConverter.getTermCurrency().equals(unwrappedClient.getBaseCurrency()))
            {
                return current;
            }

            var baseConverter = fixedConverter != null
                            ? fixedConverter.with(unwrappedClient.getBaseCurrency())
                            : new CurrencyConverterImpl(exchangeRateFactory, unwrappedClient.getBaseCurrency());

            var snapshot = ClientSnapshot.create(unwrappedClient, baseConverter, effectiveDate);
            var held = new HashMap<Security, SecurityPosition>();
            for (var position : snapshot.getJointPortfolio().getPositions())
            {
                if (position.getSecurity() != null && position.getShares() > 0)
                    held.put(position.getSecurity(), position);
            }

            var created = new SnapshotCache(effectiveDate, baseConverter, held);
            this.cache = created;
            return created;
        }
    }
}
