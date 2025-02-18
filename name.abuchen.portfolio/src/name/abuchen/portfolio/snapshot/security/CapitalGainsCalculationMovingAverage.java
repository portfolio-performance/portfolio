package name.abuchen.portfolio.snapshot.security;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private long movingRelativeNetCost = 0;

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

        movingRelativeNetCost += converted.getAmount();
        heldShares += position.getShares();
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.TransactionItem transactionItem,
                    PortfolioTransaction t)
    {
        String termCurrency = getTermCurrency();
        long netAmount = t.getGrossValue(converter).getAmount();

        switch (t.getType())
        {
            case BUY, DELIVERY_INBOUND:
                movingRelativeNetCost += netAmount;
                heldShares += t.getShares();
                break;

            case SELL, DELIVERY_OUTBOUND:
                long sold = t.getShares();
                long remaining = heldShares - sold;
                if (remaining < 0)
                {
                    movingRelativeNetCost = 0;
                    heldShares = 0;
                    // FIXME Oops. More sold than bought.
                    PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                    Values.Share.format(sold), t.getSecurity().getName(),
                                    Values.DateTime.format(t.getDateTime())));
                }
                else if (remaining == 0)
                {
                    long gain = netAmount - movingRelativeNetCost;
                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
                    movingRelativeNetCost = 0;
                    heldShares = 0;
                }
                else
                {
                    long gain = Math.round((netAmount - movingRelativeNetCost / (double) heldShares * sold));
                    realizedCapitalGains.addCapitalGains(Money.of(termCurrency, gain));
                    movingRelativeNetCost = Math.round(movingRelativeNetCost / (double) heldShares * remaining);
                    heldShares = remaining;
                }
                break;
            case TRANSFER_IN:
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
            return;
        }

        // starting valuation (based on open line items)
        long start = movingRelativeNetCost;
        // end value (based on the security positions)
        LocalDateTime valuationAtEndDate = valuationsAtEnd.get(0).getDateTime();

        Money endValue = valuationsAtEnd.stream().map(
                        item -> item.getSecurityPosition().orElseThrow(IllegalArgumentException::new).calculateValue())
                        .collect(MoneyCollectors.sum(getSecurity().getCurrencyCode()));

        Money convertedEndValue = endValue.with(converter.at(valuationAtEndDate));

        long end = convertedEndValue.getAmount();
        unrealizedCapitalGains.addCapitalGains(Money.of(termCurrency, end - start));
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
