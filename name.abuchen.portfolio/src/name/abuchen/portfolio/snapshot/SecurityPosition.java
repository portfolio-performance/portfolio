package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class SecurityPosition
{
    private static class Record
    {
        private Money purchasePrice;
        private Money purchaseValue;

        public static Record calculate(CurrencyConverter converter, SecurityPosition position)
        {
            Record answer = new Record();
            answer.calculatePurchaseValuePrice(converter, answer.filter(position.transactions));
            return answer;
        }

        public Money getPurchasePrice()
        {
            return purchasePrice;
        }

        public Money getPurchaseValue()
        {
            return purchaseValue;
        }

        /**
         * Remove matching transfer_in / transfer_out transactions
         */
        private List<PortfolioTransaction> filter(List<PortfolioTransaction> input)
        {
            List<PortfolioTransaction> inbound = new ArrayList<>();
            for (PortfolioTransaction t : input)
                if (t.getType() == Type.TRANSFER_IN)
                    inbound.add(t);

            if (inbound.isEmpty())
                return input;

            List<PortfolioTransaction> output = new ArrayList<>(input.size());
            TransactionLoop: for (PortfolioTransaction t : input)
            {
                if (t.getType() == Type.TRANSFER_IN)
                {
                    continue;
                }
                else if (t.getType() == Type.TRANSFER_OUT)
                {
                    Iterator<PortfolioTransaction> iter = inbound.iterator();
                    while (iter.hasNext())
                    {
                        PortfolioTransaction t_inbound = iter.next();
                        if (t_inbound.getDate().equals(t.getDate()) && t_inbound.getShares() == t.getShares())
                        {
                            iter.remove();
                            continue TransactionLoop;
                        }
                    }
                    output.add(t);
                }
                else
                {
                    output.add(t);
                }
            }

            output.addAll(inbound);
            return output;
        }

        private void calculatePurchaseValuePrice(CurrencyConverter converter, List<PortfolioTransaction> input)
        {
            Collections.sort(input, new Transaction.ByDate());

            long sharesSold = 0;
            for (PortfolioTransaction t : input)
            {
                if (t.getType() == Type.TRANSFER_OUT || t.getType() == Type.SELL
                                || t.getType() == Type.DELIVERY_OUTBOUND)
                    sharesSold += t.getShares();
            }

            long sharesBought = 0;
            long grossInvestment = 0;
            long netInvestment = 0;
            for (PortfolioTransaction t : input)
            {
                if (t.getType() == Type.TRANSFER_IN || t.getType() == Type.BUY || t.getType() == Type.DELIVERY_INBOUND)
                {
                    long bought = t.getShares();

                    if (sharesSold > 0)
                    {
                        sharesSold -= bought;

                        if (sharesSold < 0)
                            bought = -sharesSold;
                        else
                            bought = 0;
                    }

                    if (bought > 0)
                    {
                        long grossAmount;
                        long netAmount;

                        grossAmount = t.getMonetaryAmount(converter).getAmount();
                        netAmount = t.getGrossValue(converter).getAmount();

                        sharesBought += bought;
                        grossInvestment += grossAmount * bought / (double) t.getShares();
                        netInvestment += netAmount * bought / (double) t.getShares();
                    }
                }
            }

            this.purchasePrice = Money.of(converter.getTermCurrency(), sharesBought > 0
                            ? Math.round((netInvestment * Values.Share.factor()) / (double) sharesBought) : 0);
            this.purchaseValue = Money.of(converter.getTermCurrency(), grossInvestment);
        }

    }

    private final InvestmentVehicle investment;
    private final CurrencyConverter converter;
    private final SecurityPrice price;
    private final long shares;
    private final List<PortfolioTransaction> transactions;

    private transient Map<String, Record> currency2record = new HashMap<String, Record>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        public Record get(Object key)
        {
            return super.computeIfAbsent((String) key,
                            currency -> Record.calculate(converter.with(currency), SecurityPosition.this));
        }
    };

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
        if (price == null)
            return Money.of(investment.getCurrencyCode(), 0);

        double marketValue = shares * price.getValue() / Values.Share.divider() / Values.Quote.dividerToMoney();
        return Money.of(investment.getCurrencyCode(), Math.round(marketValue));
    }

    public Money getFIFOPurchasePrice()
    {
        return currency2record.get(investment.getCurrencyCode()).getPurchasePrice();
    }

    public Money getFIFOPurchaseValue()
    {
        return currency2record.get(investment.getCurrencyCode()).getPurchaseValue();
    }

    public Money getFIFOPurchaseValue(String currencyCode)
    {
        return currency2record.get(currencyCode).getPurchaseValue();
    }

    public Money getProfitLoss()
    {
        if (!(investment instanceof Security))
            return Money.of(investment.getCurrencyCode(), 0);

        Record record = currency2record.get(investment.getCurrencyCode());
        return calculateValue().subtract(record.getPurchaseValue());
    }

    public static SecurityPosition split(SecurityPosition position, int weight)
    {
        List<PortfolioTransaction> splitTransactions = new ArrayList<>(position.transactions.size());

        for (PortfolioTransaction t : position.transactions)
        {
            PortfolioTransaction t2 = new PortfolioTransaction();
            t2.setDate(t.getDate());
            t2.setSecurity(t.getSecurity());
            t2.setType(t.getType());
            t2.setCurrencyCode(t.getCurrencyCode());
            t2.setAmount(Math.round(t.getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setShares(Math.round(t.getShares() * weight / (double) Classification.ONE_HUNDRED_PERCENT));

            t.getUnits().forEach(u -> {
                long splitAmount = Math.round(
                                u.getAmount().getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT);

                if (u.getForex() == null)
                {
                    t2.addUnit(new Unit(u.getType(), //
                                    Money.of(u.getAmount().getCurrencyCode(), splitAmount)));
                }
                else
                {
                    long splitForex = Math.round(
                                    u.getForex().getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT);

                    t2.addUnit(new Unit(u.getType(), //
                                    Money.of(u.getAmount().getCurrencyCode(), splitAmount),
                                    Money.of(u.getForex().getCurrencyCode(), splitForex), //
                                    u.getExchangeRate()));
                }
            });

            splitTransactions.add(t2);
        }

        return new SecurityPosition(position.investment, position.converter, position.price,
                        Math.round(position.shares * weight / (double) Classification.ONE_HUNDRED_PERCENT),
                        splitTransactions);
    }
}
