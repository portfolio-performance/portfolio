package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;

/* package */class CapitalGainsCalculationMovingAverage extends AbstractCapitalGainsCalculation
{
    private long heldShares = 0;
    private long movingAverageCost = 0;
    private long movingAverageCostForex = 0;

    private final TaxesAndFees taxesAndFees;

    /**
     * Measures gains against the cost basis with or without the taxes and fees
     * embedded in a trade. {@link TaxesAndFees#NOT_INCLUDED} is the basis
     * {@link name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot} needs,
     * because it carries fees and taxes as separate terms of its breakdown and
     * would otherwise subtract them twice.
     */
    public CapitalGainsCalculationMovingAverage(TaxesAndFees taxesAndFees)
    {
        this.taxesAndFees = taxesAndFees;
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.ValuationAtStart valuation)
    {
        SecurityPosition position = valuation.getSecurityPosition().orElseThrow(IllegalArgumentException::new);

        Money value = valuation.getValue();
        Money converted = value.with(converter.at(valuation.getDateTime()));

        movingAverageCost += converted.getAmount();
        movingAverageCostForex += value.getAmount();
        heldShares += position.getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem transactionItem,
                    PortfolioTransaction t)
    {
        String termCurrency = getTermCurrency();
        String securityCurrency = getSecurity().getCurrencyCode();

        // the basis: the gross value leaves out the taxes and fees embedded in
        // the trade, the monetary amount takes them in - on both legs, as a
        // sale's gross value is the proceeds before the charges are deducted
        var forexBasis = taxesAndFees.isIncluded()
                        ? t.getMonetaryAmount(converter.with(securityCurrency)).getAmount()
                        : t.getGrossValue(converter.with(securityCurrency)).getAmount();
        var termBasis = taxesAndFees.isIncluded() ? t.getMonetaryAmount(converter).getAmount()
                        : t.getGrossValue(converter).getAmount();

        switch (t.getType())
        {
            case BUY, DELIVERY_INBOUND:
                movingAverageCost += termBasis;
                movingAverageCostForex += forexBasis;
                heldShares += t.getShares();
                break;

            case SELL, DELIVERY_OUTBOUND:
                long sold = t.getShares();
                long remaining = heldShares - sold;
                if (remaining < 0)
                {
                    movingAverageCost = 0;
                    movingAverageCostForex = 0;
                    heldShares = 0;
                    // FIXME Oops. More sold than bought.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }
                else
                {
                    var averageCosts = Math.round(movingAverageCost / (double) heldShares * sold);
                    var averageCostsForex = Math.round(movingAverageCostForex / (double) heldShares * sold);

                    long gain = termBasis - averageCosts;
                    long gainForex = 0L;

                    // forexBasis can be zero because an outbound delivery can
                    // be zero value
                    if (!termCurrency.equals(securityCurrency) && forexBasis != 0)
                    {
                        // Calculate currency gains as the difference between
                        // the average costs in the security currency with the
                        // current exchange rate and the average costs in the
                        // term currency.

                        // Essentially the delta if the user held the amount in
                        // a foreign account converted with the average exchange
                        // rate

                        // [average costs in USD] * [sale transaction exchange
                        // rate] - [average costs in EUR]

                        // calculate the exchange rate out of termBasis /
                        // forexBasis because
                        // a) we want to take the actual exchange rate of the
                        // transaction and
                        // b) the account currency might be different that the
                        // reporting currency

                        var exchangeRate = BigDecimal.valueOf(termBasis / (double) forexBasis)
                                        .setScale(Values.MC.getPrecision(), Values.MC.getRoundingMode());

                        gainForex = BigDecimal.valueOf(averageCostsForex) //
                                        .multiply(exchangeRate) //
                                        .subtract(BigDecimal.valueOf(averageCosts)) //
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();
                    }
                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
                    realizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, gainForex));

                    movingAverageCost -= averageCosts;
                    movingAverageCostForex -= averageCostsForex;
                    heldShares = remaining;
                }
                break;
            case TRANSFER_IN, TRANSFER_OUT:
                // ignore --> not relevant for moving average
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

        var valuationsAtEnd = lineItems.stream() //
                        .filter(item -> item instanceof CalculationLineItem.ValuationAtEnd)
                        .map(item -> (CalculationLineItem.ValuationAtEnd) item) //
                        .toList();

        if (valuationsAtEnd.isEmpty())
        {
            // no holdings at the end of the period -> no unrealized capital
            // gains -> nothing to do
            return;
        }

        LocalDateTime valuationAtEndDate = valuationsAtEnd.get(0).getDateTime();

        Money endValue = valuationsAtEnd.stream().map(
                        item -> item.getSecurityPosition().orElseThrow(IllegalArgumentException::new).calculateValue())
                        .collect(MoneyCollectors.sum(getSecurity().getCurrencyCode()));
        Money convertedEndValue = endValue.with(converter.at(valuationAtEndDate));

        var endAmount = convertedEndValue.getAmount();
        var endAmountForex = endValue.getAmount();

        long gain = endAmount - movingAverageCost;
        long gainForex = 0L;

        // endAmountForex can be zero if there is no holding value at the end
        if (!termCurrency.equals(securityCurrency) && endAmountForex != 0)
        {
            var exchangeRate = BigDecimal.valueOf(endAmount / (double) endAmountForex)
                            .setScale(Values.MC.getPrecision(), Values.MC.getRoundingMode());

            gainForex = BigDecimal.valueOf(movingAverageCostForex) //
                            .multiply(exchangeRate) //
                            .subtract(BigDecimal.valueOf(movingAverageCost)) //
                            .setScale(0, RoundingMode.HALF_DOWN).longValue();
        }

        unrealizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
        unrealizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, gainForex));
    }
}
