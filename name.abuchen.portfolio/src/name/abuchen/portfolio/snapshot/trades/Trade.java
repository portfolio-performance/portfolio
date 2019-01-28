package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

public class Trade
{
    private Security security;
    private LocalDateTime start;
    private LocalDateTime end;
    private long shares;

    private List<TransactionPair<PortfolioTransaction>> transactions = new ArrayList<>();

    private Money entryValue;
    private Money exitValue;
    private long holdingPeriod;

    public Trade(Security security, long shares)
    {
        this.security = security;
        this.shares = shares;
    }

    /* package */ void calculate(CurrencyConverter converter)
    {
        this.entryValue = transactions.stream() //
                        .filter(t -> t.getTransaction().getType().isPurchase())
                        .map(t -> t.getTransaction().getMonetaryAmount()
                                        .with(converter.at(t.getTransaction().getDateTime())))
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));

        if (end != null)
        {
            this.exitValue = transactions.stream() //
                            .filter(t -> t.getTransaction().getType().isLiquidation())
                            .map(t -> t.getTransaction().getMonetaryAmount()
                                            .with(converter.at(t.getTransaction().getDateTime())))
                            .collect(MoneyCollectors.sum(converter.getTermCurrency()));

            this.holdingPeriod = Math.round(transactions.stream() //
                            .filter(t -> t.getTransaction().getType().isPurchase())
                            .mapToLong(t -> t.getTransaction().getShares() * Dates.daysBetween(
                                            t.getTransaction().getDateTime().toLocalDate(), end.toLocalDate()))
                            .sum() / (double) shares);
        }
        else
        {
            LocalDate now = LocalDate.now();
            double marketValue = shares / Values.Share.divider() * security.getSecurityPrice(now).getValue()
                            / Values.Quote.dividerToMoney();
            this.exitValue = converter.at(now).apply(Money.of(security.getCurrencyCode(), Math.round(marketValue)));

            this.holdingPeriod = Math.round(transactions.stream() //
                            .filter(t -> t.getTransaction().getType().isPurchase())
                            .mapToLong(t -> t.getTransaction().getShares()
                                            * Dates.daysBetween(t.getTransaction().getDateTime().toLocalDate(), now))
                            .sum() / (double) shares);
        }

    }

    public Security getSecurity()
    {
        return security;
    }

    public Optional<LocalDateTime> getEnd()
    {
        return Optional.ofNullable(end);
    }

    /* package */ void setEnd(LocalDateTime end)
    {
        this.end = end;
    }

    public LocalDateTime getStart()
    {
        return start;
    }

    /* package */ void setStart(LocalDateTime start)
    {
        this.start = start;
    }

    public long getShares()
    {
        return shares;
    }

    public List<TransactionPair<PortfolioTransaction>> getTransactions()
    {
        return transactions;
    }

    public Money getEntryValue()
    {
        return entryValue;
    }

    public Money getExitValue()
    {
        return exitValue;
    }

    public Money getProfitLoss()
    {
        return exitValue.subtract(entryValue);
    }

    public long getHoldingPeriod()
    {
        return holdingPeriod;
    }
}
