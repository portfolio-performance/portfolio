package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class SecurityPosition
{
    private final InvestmentVehicle investment;
    private final CurrencyConverter converter;
    private final SecurityPrice price;
    private final long shares;
    private final List<PortfolioTransaction> transactions;

    private SecurityPosition(InvestmentVehicle investment, CurrencyConverter converter, SecurityPrice price,
                    long shares, List<PortfolioTransaction> transactions)
    {
        this.investment = investment;
        this.converter = converter;
        this.price = price;
        this.shares = shares;
        this.transactions = transactions;
    }

    public SecurityPosition(AccountSnapshot snapshot)
    {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot.getAccount());

        this.investment = snapshot.getAccount();
        this.converter = snapshot.getCurrencyConverter().with(investment.getCurrencyCode());
        this.price = new SecurityPrice(snapshot.getTime(),
                        snapshot.getUnconvertedFunds().getAmount() * Values.Quote.factorToMoney());
        this.shares = Values.Share.factor();
        this.transactions = new ArrayList<>();
    }

    public SecurityPosition(Security security, CurrencyConverter converter, SecurityPrice price,
                    List<PortfolioTransaction> transactions)
    {
        Objects.requireNonNull(security);
        Objects.requireNonNull(converter);
        Objects.requireNonNull(price);

        this.investment = security;
        this.converter = converter.with(investment.getCurrencyCode());
        this.price = price;
        this.shares = transactions.stream().mapToLong(t -> {
            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                case DELIVERY_INBOUND:
                    return t.getShares();
                case SELL:
                case TRANSFER_OUT:
                case DELIVERY_OUTBOUND:
                    return -t.getShares();
                default:
                    throw new UnsupportedOperationException();
            }
        }).sum();
        this.transactions = new ArrayList<>(transactions);
    }

    public Security getSecurity()
    {
        return investment instanceof Security ? (Security) investment : null;
    }

    public InvestmentVehicle getInvestmentVehicle()
    {
        return investment;
    }

    public SecurityPrice getPrice()
    {
        return price;
    }

    public long getShares()
    {
        return shares;
    }

    public Money calculateValue()
    {
        double marketValue = shares / Values.Share.divider() * price.getValue() / Values.Quote.dividerToMoney();
        return Money.of(investment.getCurrencyCode(), Math.round(marketValue));
    }

    public static SecurityPosition split(SecurityPosition position, int weight)
    {
        List<PortfolioTransaction> splitTransactions = new ArrayList<>(position.transactions.size());

        for (PortfolioTransaction t : position.transactions)
        {
            PortfolioTransaction t2 = new PortfolioTransaction();
            t2.setDateTime(t.getDateTime());
            t2.setSecurity(t.getSecurity());
            t2.setType(t.getType());
            t2.setCurrencyCode(t.getCurrencyCode());
            t2.setAmount(Math.round(t.getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setShares(Math.round(t.getShares() * weight / (double) Classification.ONE_HUNDRED_PERCENT));

            t.getUnits().forEach(u -> t2.addUnit(u.split(weight / (double) Classification.ONE_HUNDRED_PERCENT)));

            splitTransactions.add(t2);
        }

        return new SecurityPosition(position.investment, position.converter, position.price,
                        Math.round(position.shares * weight / (double) Classification.ONE_HUNDRED_PERCENT),
                        splitTransactions);
    }
}
