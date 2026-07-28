package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class CapitalGainsCalculation extends AbstractCapitalGainsCalculation
{
    private static class LineItem
    {
        private long shares;
        private LocalDate date;

        /**
         * Lot cost basis in the term (reporting) currency.
         */
        private long value;

        /**
         * Lot cost basis in the security's currency. Carried alongside
         * {@link #value} so that the currency component can be derived from the
         * exchange rate recorded on the transaction rather than the historic
         * day rate of the exchange-rate provider - matching the moving-average
         * sibling and {@code CostCalculation}.
         */
        private long valueForex;

        private final TrailRecord trail;

        /**
         * Trail for {@link #valueForex}, i.e. the cost basis in the security's
         * currency.
         */
        private final TrailRecord forexTrail;

        /**
         * Holds the original number of shares (of the transaction). The
         * original shares are needed to calculate fractions if the transaction
         * is split up multiple times
         */
        private final long originalShares;

        private final CalculationLineItem source;

        public LineItem(long shares, LocalDate date, long value, long valueForex, TrailRecord trail,
                        TrailRecord forexTrail, CalculationLineItem source)
        {
            this.shares = shares;
            this.date = Objects.requireNonNull(date);
            this.value = value;
            this.valueForex = valueForex;
            this.trail = trail;
            this.forexTrail = forexTrail;
            this.originalShares = shares;
            this.source = source;
        }
    }

    private List<LineItem> fifo = new ArrayList<>();

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart valuation)
    {
        SecurityPosition position = valuation.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        // the valuation is expressed in the security's currency
        Money valueForex = valuation.getValue();
        Money converted = valueForex.with(converter.at(valuation.getDateTime()));

        // the valuation carries no transaction exchange rate, hence the
        // historic conversion at that date is correct for the term-currency
        // basis

        TrailRecord forexTrail = TrailRecord.ofPosition(valuation.getDateTime().toLocalDate(),
                        (Portfolio) valuation.getOwner(), position);
        TrailRecord trail = forexTrail;
        if (!valueForex.getCurrencyCode().equals(converter.getTermCurrency()))
            trail = trail.convert(converted, converter.getRate(valuation.getDateTime(), valueForex.getCurrencyCode()));

        fifo.add(new LineItem(position.getShares(), valuation.getDateTime().toLocalDate(), converted.getAmount(),
                        valueForex.getAmount(), trail, forexTrail, valuation));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem transactionItem,
                    PortfolioTransaction t)
    {
        String termCurrency = getTermCurrency();
        String securityCurrency = t.getSecurity().getCurrencyCode();

        Money grossValue = t.getGrossValue();

        // prefer the exchange rate recorded on the transaction (via
        // getGrossValue(converter)) over the historic day rate of the
        // exchange-rate provider, in both the term and the security currency
        Money termBasis = t.getGrossValue(converter);
        Money forexBasis = t.getGrossValue(converter.with(securityCurrency));

        TrailRecord txTrail = TrailRecord.ofTransaction(t).asGrossValue(grossValue);
        if (!grossValue.getCurrencyCode().equals(termCurrency))
            txTrail = txTrail.convert(termBasis, impliedRate(grossValue, termBasis, t.getDateTime().toLocalDate()));

        TrailRecord forexTxTrail = TrailRecord.ofTransaction(t).asGrossValue(grossValue);
        if (!grossValue.getCurrencyCode().equals(securityCurrency))
            forexTxTrail = forexTxTrail.convert(forexBasis,
                            impliedRate(grossValue, forexBasis, t.getDateTime().toLocalDate()));

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                fifo.add(new LineItem(t.getShares(), t.getDateTime().toLocalDate(), termBasis.getAmount(),
                                forexBasis.getAmount(), txTrail, forexTxTrail, transactionItem));
                break;

            case SELL:
            case DELIVERY_OUTBOUND:

                long value = termBasis.getAmount();
                long valueForex = forexBasis.getAmount();

                long sold = t.getShares();

                for (LineItem item : fifo) // NOSONAR
                {
                    if (item.shares == 0)
                        continue;

                    if (!item.source.getOwner().equals(transactionItem.getOwner()))
                        continue;

                    if (sold <= 0)
                        break;

                    long soldShares = Math.min(sold, item.shares);
                    long start = Math.round((double) soldShares / item.shares * item.value);
                    long startForex = Math.round((double) soldShares / item.shares * item.valueForex);
                    long end = Math.round((double) soldShares / t.getShares() * value);

                    TrailRecord startTrail = item.trail.fraction(Money.of(termCurrency, start), soldShares,
                                    item.originalShares);

                    long forexGain = 0L;
                    TrailRecord forexGainTrail = TrailRecord.empty();

                    // valueForex can be zero because an outbound delivery can
                    // be zero value
                    if (!termCurrency.equals(securityCurrency) && valueForex != 0)
                    {
                        // calculate currency gains as the relieved cost basis
                        // in the security currency, valued at the exchange rate
                        // recorded on the sale transaction, less the relieved
                        // cost basis in the term currency (equivalent to
                        // holding the money as cash in the security's
                        // currency).

                        // derive the exchange rate from netAmount /
                        // netAmountForex because a) we want the actual exchange
                        // rate of the transaction and b) the account currency
                        // might be different than the reporting currency

                        BigDecimal exchangeRate = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(valueForex),
                                        Values.MC);

                        long relievedForexInTerm = BigDecimal.valueOf(startForex).multiply(exchangeRate)
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();
                        forexGain = relievedForexInTerm - start;

                        forexGainTrail = item.forexTrail
                                        .fraction(Money.of(securityCurrency, startForex), soldShares,
                                                        item.originalShares)
                                        .convert(Money.of(termCurrency, relievedForexInTerm),
                                                        new ExchangeRate(t.getDateTime().toLocalDate(), exchangeRate))
                                        .subtract(startTrail);
                    }

                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, end - start));
                    realizedCapitalGains.addCapitalGainsTrail(txTrail //
                                    .fraction(Money.of(termCurrency, end), soldShares, t.getShares())
                                    .subtract(startTrail));
                    realizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, forexGain));
                    realizedCapitalGains.addForexCapitalGainsTrail(forexGainTrail);

                    item.shares -= soldShares;
                    item.value -= start;
                    item.valueForex -= startForex;

                    sold -= soldShares;
                }

                if (sold > 0)
                {
                    // Report that more was sold than bought to log
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }

                break;

            case TRANSFER_IN:
                long moved = t.getShares();

                TransactionOwner<?> source = t.getCrossEntry().getCrossOwner(t);

                // iterate on copy b/c underlying list can be changed
                for (LineItem entry : new ArrayList<>(fifo))
                {
                    if (moved <= 0)
                        break;

                    if (!entry.source.getOwner().equals(source))
                        continue;

                    if (entry.shares == 0)
                        continue;

                    long n = Math.min(moved, entry.shares);

                    long transferredValue = Math.round(n / (double) entry.shares * entry.value);
                    long transferredValueForex = Math.round(n / (double) entry.shares * entry.valueForex);
                    LineItem transfer = new LineItem(n, t.getDateTime().toLocalDate(), transferredValue,
                                    transferredValueForex, //
                                    entry.trail.fraction( //
                                                    Money.of(termCurrency, transferredValue), //
                                                    n, //
                                                    entry.originalShares //
                                    ).transfer(t.getDateTime().toLocalDate(), entry.source.getOwner(),
                                                    transactionItem.getOwner()),
                                    entry.forexTrail.fraction( //
                                                    Money.of(securityCurrency, transferredValueForex), //
                                                    n, //
                                                    entry.originalShares //
                                    ).transfer(t.getDateTime().toLocalDate(), entry.source.getOwner(),
                                                    transactionItem.getOwner()),
                                    transactionItem);

                    if (n == entry.shares)
                    {
                        fifo.add(fifo.indexOf(entry) + 1, transfer);
                        fifo.remove(entry);
                    }
                    else
                    {
                        entry.value -= transferredValue;
                        entry.valueForex -= transferredValueForex;
                        entry.shares -= n;

                        fifo.add(fifo.indexOf(entry) + 1, transfer);
                    }

                    moved -= n;
                }

                if (moved > 0)
                {
                    // FIXME Oops. More moved than available.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(moved), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }

                break;

            case TRANSFER_OUT:
                // ignore -> handled via TRANSFER_IN
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        // calculate the unrealized capital gains in one go (i.e. looking at all
        // ValuationAtEnd objects at once instead of individually inside the
        // #visit method) in order to avoid a) rounding errors and b) splitting
        // up the trails into very many fractions

        String termCurrency = getTermCurrency();
        String securityCurrency = getSecurity().getCurrencyCode();

        List<CalculationLineItem.ValuationAtEnd> valuationsAtEnd = lineItems.stream()
                        .filter(item -> item instanceof CalculationLineItem.ValuationAtEnd)
                        .map(item -> (CalculationLineItem.ValuationAtEnd) item) //
                        .collect(Collectors.toList());

        if (valuationsAtEnd.isEmpty())
        {
            // no holdings at the end of the period -> no unrealized capital
            // gains -> nothing to do

            // log warning message in case there are no holdings but unmatched
            // transactions

            long value = fifo.stream().mapToLong(item -> item.value).sum();
            if (value != 0)
            {
                PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                Values.Money.format(Money.of(termCurrency, value)), getSecurity().getName(),
                                fifo.stream().map(item -> Values.Date.format(item.date))
                                                .collect(Collectors.joining(",")))); //$NON-NLS-1$
            }

            return;
        }

        squashForexValuationsAtStart(converter);

        // starting valuation (based on open line items)

        long start = fifo.stream().mapToLong(item -> item.value).sum();
        long startForex = fifo.stream().mapToLong(item -> item.valueForex).sum();

        TrailRecord startTrail = TrailRecord.of(fifo.stream() //
                        .filter(item -> item.shares != 0).map(item -> item.trail
                                        .fraction(Money.of(termCurrency, item.value), item.shares, item.originalShares))
                        .collect(Collectors.toList()));

        TrailRecord startForexTrail = TrailRecord.of(fifo.stream() //
                        .filter(item -> item.shares != 0)
                        .map(item -> item.forexTrail.fraction(Money.of(securityCurrency, item.valueForex), item.shares,
                                        item.originalShares))
                        .collect(Collectors.toList()));

        // end value (based on the security positions)

        LocalDateTime valuationAtEndDate = valuationsAtEnd.get(0).getDateTime();

        Money endValue = valuationsAtEnd.stream().map(
                        item -> item.getSecurityPosition().orElseThrow(IllegalArgumentException::new).calculateValue())
                        .collect(MoneyCollectors.sum(getSecurity().getCurrencyCode()));
        TrailRecord endTrail = TrailRecord.of(valuationsAtEnd.stream()
                        .map(item -> TrailRecord.ofPosition(valuationAtEndDate.toLocalDate(),
                                        (Portfolio) item.getOwner(),
                                        item.getSecurityPosition().orElseThrow(IllegalArgumentException::new)))
                        .collect(Collectors.toList()));

        // the valuation at the end carries no transaction exchange rate, hence
        // the historic conversion at that date is correct

        Money convertedEndValue = endValue.with(converter.at(valuationAtEndDate));
        if (!endValue.getCurrencyCode().equals(converter.getTermCurrency()))
            endTrail = endTrail.convert(convertedEndValue,
                            converter.getRate(valuationAtEndDate, endValue.getCurrencyCode()));

        long end = convertedEndValue.getAmount();
        long endForex = endValue.getAmount();
        long forexGain = 0L;
        TrailRecord forexGainTrail = TrailRecord.empty();

        // endForex can be zero if there is no holding value at the end
        if (!termCurrency.equals(securityCurrency) && endForex != 0)
        {
            // calculate currency gains as the accumulated cost basis of the
            // still-open lots in the security currency, valued at the closing
            // exchange rate, less the accumulated cost basis in the term
            // currency - mirroring the realized calculation, but with the
            // closing valuation providing the rate

            BigDecimal exchangeRate = BigDecimal.valueOf(end).divide(BigDecimal.valueOf(endForex), Values.MC);

            long forexInTerm = BigDecimal.valueOf(startForex).multiply(exchangeRate).setScale(0, RoundingMode.HALF_DOWN)
                            .longValue();
            forexGain = forexInTerm - start;

            forexGainTrail = startForexTrail
                            .convert(Money.of(termCurrency, forexInTerm),
                                            new ExchangeRate(valuationAtEndDate.toLocalDate(), exchangeRate))
                            .subtract(startTrail);
        }

        unrealizedCapitalGains.addCapitalGains(Money.of(termCurrency, end - start));
        unrealizedCapitalGains.addCapitalGainsTrail(endTrail.subtract(startTrail));
        unrealizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, forexGain));
        unrealizedCapitalGains.addForexCapitalGainsTrail(forexGainTrail);

        fifo.clear();
    }

    /**
     * Derives the exchange rate implied by two amounts (from {@code from} to
     * {@code to}). Used to label the conversion step of a trail so that the
     * rate shown is the one actually applied to the value rather than the
     * historic day rate.
     */
    private static ExchangeRate impliedRate(Money from, Money to, LocalDate date)
    {
        BigDecimal rate = from.getAmount() == 0 ? BigDecimal.ONE
                        : BigDecimal.valueOf(to.getAmount()).divide(BigDecimal.valueOf(from.getAmount()), Values.MC);
        return new ExchangeRate(date, rate);
    }

    private void squashForexValuationsAtStart(CurrencyConverter converter)
    {
        // squash multiple ValuationsAtStart iff
        // - valuation is done in forex (to have only one currency conversion
        // instead of multiple)
        // - valuation is not modified, i.e. has not been (partially) matched
        // with a sale transaction

        if (getSecurity().getCurrencyCode().equals(getTermCurrency()))
            return;

        List<LineItem> itemsToSquash = fifo.stream()
                        .filter(item -> item.source instanceof CalculationLineItem.ValuationAtStart)
                        .filter(item -> item.shares == item.originalShares) //
                        .collect(Collectors.toList());

        if (itemsToSquash.size() < 2)
            return;

        LocalDateTime valuationAtStartDate = itemsToSquash.get(0).source.getDateTime();

        long shares = itemsToSquash.stream().mapToLong(item -> item.shares).sum();

        Money value = itemsToSquash.stream()
                        .map(item -> item.source.getSecurityPosition().orElseThrow(IllegalArgumentException::new)
                                        .calculateValue())
                        .collect(MoneyCollectors.sum(getSecurity().getCurrencyCode()));
        Money converted = value.with(converter.at(valuationAtStartDate));

        TrailRecord forexTrail = TrailRecord.of(itemsToSquash.stream()
                        .map(item -> TrailRecord.ofPosition(valuationAtStartDate.toLocalDate(),
                                        (Portfolio) item.source.getOwner(),
                                        item.source.getSecurityPosition().orElseThrow(IllegalArgumentException::new)))
                        .collect(Collectors.toList()));

        TrailRecord trail = forexTrail.convert(converted,
                        converter.getRate(valuationAtStartDate, value.getCurrencyCode()));

        LineItem replacement = new LineItem(shares, valuationAtStartDate.toLocalDate(), converted.getAmount(),
                        value.getAmount(), trail, forexTrail, null);

        fifo.removeAll(itemsToSquash);
        fifo.add(0, replacement);
    }
}
