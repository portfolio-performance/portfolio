package name.abuchen.portfolio.snapshot.trades;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

public class Trade implements Adaptable
{
    private final Security security;
    private final Portfolio portfolio;
    private LocalDateTime start;
    private LocalDateTime end;
    private final long shares;

    private List<TransactionPair<PortfolioTransaction>> transactions = new ArrayList<>();

    private Money entryValue;
    private Money exitValue;
    private long holdingPeriod;
    private double irr;

    public Trade(Security security, Portfolio portfolio, long shares)
    {
        this.security = security;
        this.shares = shares;
        this.portfolio = portfolio;
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

        calculateIRR(converter);
    }

    private void calculateIRR(CurrencyConverter converter)
    {
        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        transactions.stream().forEach(t -> {
            dates.add(t.getTransaction().getDateTime().toLocalDate());

            double amount = t.getTransaction().getMonetaryAmount().with(converter.at(t.getTransaction().getDateTime()))
                            .getAmount() / Values.Amount.divider();

            if (t.getTransaction().getType().isPurchase())
                amount = -amount;

            values.add(amount);
        });

        if (end == null)
        {
            dates.add(LocalDate.now());
            values.add(exitValue.getAmount() / Values.Amount.divider());
        }

        this.irr = IRR.calculate(dates, values);
    }

    public Security getSecurity()
    {
        return security;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
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

    public double getIRR()
    {
        return irr;
    }

    public double getReturn()
    {
        return (exitValue.getAmount() / (double) entryValue.getAmount()) - 1;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Named.class)
            return type.cast(security);
        else
            return null;
    }
}
