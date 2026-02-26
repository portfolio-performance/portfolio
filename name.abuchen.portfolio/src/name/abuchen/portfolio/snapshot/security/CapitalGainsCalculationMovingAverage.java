package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;

/* package */class CapitalGainsCalculationMovingAverage extends Calculation
{
    private long heldShares = 0;
    private long movingAverageNetCost = 0;
    private long movingAverageNetCostForex = 0;

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

        movingAverageNetCost += converted.getAmount();
        movingAverageNetCostForex += value.getAmount();
        heldShares += position.getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem transactionItem,
                    PortfolioTransaction t)
    {
        String termCurrency = getTermCurrency();
        String securityCurrency = getSecurity().getCurrencyCode();

        long netAmountForex = t.getGrossValue(converter.with(securityCurrency)).getAmount();
        long netAmount = t.getGrossValue(converter).getAmount();

        switch (t.getType())
        {
            case BUY, DELIVERY_INBOUND:
                movingAverageNetCost += netAmount;
                movingAverageNetCostForex += netAmountForex;
                heldShares += t.getShares();
                break;

            case SELL, DELIVERY_OUTBOUND:
                long sold = t.getShares();
                long remaining = heldShares - sold;
                if (remaining < 0)
                {
                    movingAverageNetCost = 0;
                    movingAverageNetCostForex = 0;
                    heldShares = 0;
                    // FIXME Oops. More sold than bought.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }
                else
                {
                    var averageCosts = Math.round(movingAverageNetCost / (double) heldShares * sold);
                    var averageCostsForex = Math.round(movingAverageNetCostForex / (double) heldShares * sold);

                    long gain = netAmount - averageCosts;
                    long gainForex = 0L;

                    // netAmountForex can be zero because an outbound delivery
                    // can be zero value
                    if (!termCurrency.equals(securityCurrency) && netAmountForex != 0)
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

                        // calculate the exchange rate out of netAmount /
                        // netAmountForex because
                        // a) we want to take the actual exchange rate of the
                        // transaction and
                        // b) the account currency might be different that the
                        // reporting currency

                        var exchangeRate = BigDecimal.valueOf(netAmount / (double) netAmountForex)
                                        .setScale(Values.MC.getPrecision(), Values.MC.getRoundingMode());

                        gainForex = BigDecimal.valueOf(averageCostsForex) //
                                        .multiply(exchangeRate) //
                                        .subtract(BigDecimal.valueOf(averageCosts)) //
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();
                    }
                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
                    realizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, gainForex));

                    movingAverageNetCost -= averageCosts;
                    movingAverageNetCostForex -= averageCostsForex;
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

        var netAmount = convertedEndValue.getAmount();
        var netAmountForex = endValue.getAmount();

        long gain = netAmount - movingAverageNetCost;
        long gainForex = 0L;

        // netAmountForex can be zero because an outbound delivery can be zero
        // value
        if (!termCurrency.equals(securityCurrency) && netAmountForex != 0)
        {
            var exchangeRate = BigDecimal.valueOf(netAmount / (double) netAmountForex)
                            .setScale(Values.MC.getPrecision(), Values.MC.getRoundingMode());

            gainForex = BigDecimal.valueOf(movingAverageNetCostForex) //
                            .multiply(exchangeRate) //
                            .subtract(BigDecimal.valueOf(movingAverageNetCost)) //
                            .setScale(0, RoundingMode.HALF_DOWN).longValue();
        }

        unrealizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
        unrealizedCapitalGains.addForexCaptialGains(Money.of(termCurrency, gainForex));
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
