package name.abuchen.portfolio.snapshot.security;

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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

/* package */class CapitalGainsCalculation extends Calculation
{
    private static class LineItem
    {
        private long shares;
        private LocalDate date;
        private long value;

        private final TrailRecord trail;

        /**
         * Holds the original number of shares (of the transaction). The
         * original shares are needed to calculate fractions if the transaction
         * is split up multiple times
         */
        private final long originalShares;

        private final CalculationLineItem source;

        public LineItem(long shares, LocalDate date, long value, TrailRecord trail, CalculationLineItem source)
        {
            this.shares = shares;
            this.date = Objects.requireNonNull(date);
            this.value = value;
            this.trail = trail;
            this.originalShares = shares;
            this.source = source;
        }
    }

    private List<LineItem> fifo = new ArrayList<>();

    private CapitalGainsRecord realizedCapitalGains;
    private CapitalGainsRecord unrealizedCapitalGains;

    @Override
    public void prepare()
    {
        this.realizedCapitalGains = new CapitalGainsRecord(getSecurity(), getTermCurrency());
        this.unrealizedCapitalGains = new CapitalGainsRecord(getSecurity(), getTermCurrency());
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart valuation)
    {
        SecurityPosition position = valuation.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        Money value = valuation.getValue();
        Money converted = value.with(converter.at(valuation.getDateTime()));

        TrailRecord trail = TrailRecord.ofPosition(valuation.getDateTime().toLocalDate(),
                        (Portfolio) valuation.getOwner(), position);
        if (!value.getCurrencyCode().equals(converter.getTermCurrency()))
            trail = trail.convert(converted, converter.getRate(valuation.getDateTime(), value.getCurrencyCode()));

        fifo.add(new LineItem(position.getShares(), valuation.getDateTime().toLocalDate(), converted.getAmount(), trail,
                        valuation));
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem transactionItem,
                    PortfolioTransaction t)
    {
        String termCurrency = getTermCurrency();
        Money grossValue = t.getGrossValue();
        Money convertedGrossValue = grossValue.with(converter.at(t.getDateTime()));

        TrailRecord txTrail = TrailRecord.ofTransaction(t).asGrossValue(grossValue);
        if (!grossValue.getCurrencyCode().equals(converter.getTermCurrency()))
            txTrail = txTrail.convert(convertedGrossValue, converter
                            .getRate(transactionItem.getDateTime().toLocalDate(), grossValue.getCurrencyCode()));

        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                fifo.add(new LineItem(t.getShares(), t.getDateTime().toLocalDate(), convertedGrossValue.getAmount(),
                                txTrail, transactionItem));
                break;

            case SELL:
            case DELIVERY_OUTBOUND:

                long value = convertedGrossValue.getAmount();

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
                    long end = Math.round((double) soldShares / t.getShares() * value);

                    TrailRecord startTrail = item.trail.fraction(Money.of(termCurrency, start), soldShares,
                                    item.originalShares);

                    long forexGain = 0L;
                    TrailRecord forexGainTrail = TrailRecord.empty();

                    if (!termCurrency.equals(t.getSecurity().getCurrencyCode()))
                    {
                        // calculate currency gains (if the security is
                        // traded in forex) by converting the start value to
                        // forex and converting it back with the exchange
                        // rate at the end of the period (equivalent to
                        // holding the money as cash in forex currency)

                        CurrencyConverter convert2forex = converter.with(t.getSecurity().getCurrencyCode());

                        Money forex = convert2forex.convert(item.date, Money.of(termCurrency, start));
                        Money back = forex.with(converter.at(t.getDateTime()));
                        forexGain = back.getAmount() - start;

                        forexGainTrail = startTrail //
                                        .convert(forex, convert2forex.getRate(item.date, termCurrency)) //
                                        .convert(back, converter.getRate(t.getDateTime(),
                                                        t.getSecurity().getCurrencyCode()))
                                        .substract(startTrail);
                    }

                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, end - start));
                    realizedCapitalGains.addCapitalGainsTrail(txTrail //
                                    .fraction(Money.of(termCurrency, end), soldShares, t.getShares())
                                    .substract(startTrail));
                    realizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, forexGain));
                    realizedCapitalGains.addForexCapitalGainsTrail(forexGainTrail);

                    item.shares -= soldShares;
                    item.value -= start;

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

                    if (n == entry.shares)
                    {
                        LineItem transfer = new LineItem(entry.shares, entry.date, entry.value,
                                        entry.trail.transfer(t.getDateTime().toLocalDate(), entry.source.getOwner(),
                                                        transactionItem.getOwner()),
                                        transactionItem);

                        fifo.add(fifo.indexOf(entry) + 1, transfer);
                        fifo.remove(entry);
                    }
                    else
                    {
                        long transferredValue = Math.round(n / (double) entry.shares * entry.value);

                        LineItem transfer = new LineItem(n, //
                                        t.getDateTime().toLocalDate(), transferredValue, //
                                        entry.trail.fraction(Money.of(getTermCurrency(), transferredValue), n,
                                                        entry.originalShares) //
                                                        .transfer(t.getDateTime().toLocalDate(),
                                                                        entry.source.getOwner(),
                                                                        transactionItem.getOwner()),
                                        transactionItem);

                        entry.value -= transferredValue;
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

        TrailRecord startTrail = TrailRecord.of(fifo.stream() //
                        .filter(item -> item.shares != 0).map(item -> item.trail
                                        .fraction(Money.of(termCurrency, item.value), item.shares, item.originalShares))
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

        Money convertedEndValue = endValue.with(converter.at(valuationAtEndDate));
        if (!endValue.getCurrencyCode().equals(converter.getTermCurrency()))
            endTrail = endTrail.convert(convertedEndValue,
                            converter.getRate(valuationAtEndDate, endValue.getCurrencyCode()));

        long end = convertedEndValue.getAmount();
        long forexGain = 0L;
        TrailRecord forexGainTrail = TrailRecord.empty();

        if (!termCurrency.equals(getSecurity().getCurrencyCode()))
        {
            // calculate forex gains: use exchange rate of
            // each date of investment

            CurrencyConverter convert2Forex = converter.with(getSecurity().getCurrencyCode());

            Money forex = fifo.stream() //
                            .filter(item -> item.value != 0) //
                            .map(item -> convert2Forex.convert(item.date, Money.of(termCurrency, item.value)))
                            .collect(MoneyCollectors.sum(getSecurity().getCurrencyCode()));

            Money back = forex.with(converter.at(valuationAtEndDate));

            forexGain = back.getAmount() - start;

            // collect all start values and convert to forex
            // using the start date

            forexGainTrail = TrailRecord.of(fifo.stream() //
                            .filter(item -> item.value != 0) //
                            .map(item -> item.trail
                                            .fraction(Money.of(termCurrency, item.value), item.shares,
                                                            item.originalShares)
                                            .convert(convert2Forex.convert(item.date,
                                                            Money.of(termCurrency, item.value)),
                                                            convert2Forex.getRate(item.date, termCurrency)))
                            .collect(Collectors.toList()));

            // convert the forex amount back with the
            // exchange rate at the end (=snapshot end) and
            // substract start value

            forexGainTrail = forexGainTrail
                            .convert(back, converter.getRate(valuationAtEndDate, getSecurity().getCurrencyCode()))
                            .substract(startTrail);
        }

        unrealizedCapitalGains.addCapitalGains(Money.of(termCurrency, end - start));
        unrealizedCapitalGains.addCapitalGainsTrail(endTrail.substract(startTrail));
        unrealizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, forexGain));
        unrealizedCapitalGains.addForexCapitalGainsTrail(forexGainTrail);
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

        TrailRecord trail = TrailRecord.of(itemsToSquash.stream()
                        .map(item -> TrailRecord.ofPosition(valuationAtStartDate.toLocalDate(),
                                        (Portfolio) item.source.getOwner(),
                                        item.source.getSecurityPosition().orElseThrow(IllegalArgumentException::new)))
                        .collect(Collectors.toList()));

        trail = trail.convert(converted, converter.getRate(valuationAtStartDate, value.getCurrencyCode()));

        LineItem replacement = new LineItem(shares, valuationAtStartDate.toLocalDate(), converted.getAmount(), trail,
                        null);

        fifo.removeAll(itemsToSquash);
        fifo.add(0, replacement);
    }

    public CapitalGainsRecord getRealizedCapitalGains()
    {
        return realizedCapitalGains;
    }

    public CapitalGainsRecord getUnrealizedCapitalGains()
    {
        return unrealizedCapitalGains;
    }

}
