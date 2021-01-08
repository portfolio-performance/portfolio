package name.abuchen.portfolio.snapshot.security;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public interface CalculationLineItem
{
    public static class TransactionItem implements CalculationLineItem
    {
        private final TransactionPair<?> txPair;

        private TransactionItem(TransactionPair<?> transaction)
        {
            this.txPair = transaction;
        }

        @Override
        public TransactionOwner<?> getOwner()
        {
            return txPair.getOwner();
        }

        @Override
        public String getLabel()
        {
            if (txPair.getTransaction() instanceof AccountTransaction)
                return ((AccountTransaction) txPair.getTransaction()).getType().toString();
            else if (txPair.getTransaction() instanceof PortfolioTransaction)
                return ((PortfolioTransaction) txPair.getTransaction()).getType().toString();
            else
                return null;
        }

        @Override
        public LocalDateTime getDateTime()
        {
            return txPair.getTransaction().getDateTime();
        }

        @Override
        public Money getValue()
        {
            return txPair.getTransaction().getMonetaryAmount();
        }

        @Override
        public Optional<Transaction> getTransaction()
        {
            return Optional.of(txPair.getTransaction());
        }

        protected Transaction tx()
        {
            return txPair.getTransaction();
        }
    }

    public static class DividendPayment extends TransactionItem
    {
        private long totalShares;
        private Money fifoCost;
        private Money movingAverageCost;

        private DividendPayment(TransactionPair<?> transaction)
        {
            super(transaction);
        }

        public long getDividendPerShare()
        {
            return amountFractionPerShare(getGrossValueAmount(), tx().getShares());
        }

        /**
         * Returns the FIFO costs. It is the cost of the total position of the
         * given security. However, a dividend payment may only be about partial
         * holdings, for example if the security is held in multiple securities
         * accounts.
         */
        /* package */ Money getFifoCost()
        {
            return fifoCost;
        }

        /* package */ void setFifoCost(Money fifoCost)
        {
            this.fifoCost = fifoCost;
        }

        /**
         * Returns the costs based on moving average. It is the cost of the
         * total position of the given security. However, a dividend payment may
         * only be about partial holdings, for example if the security is held
         * in multiple securities accounts.
         */
        /* package */ Money getMovingAverageCost()
        {
            return movingAverageCost;
        }

        /* package */ void setMovingAverageCost(Money movingAverageCost)
        {
            this.movingAverageCost = movingAverageCost;
        }

        /* package */ void setTotalShares(long totalShares)
        {
            this.totalShares = totalShares;
        }

        public double getPersonalDividendYield()
        {
            if ((fifoCost == null) || (fifoCost.getAmount() <= 0))
                return 0;

            double cost = fifoCost.getAmount();

            if (tx().getShares() > 0)
                cost = fifoCost.getAmount() * (tx().getShares() / (double) totalShares);

            return getGrossValueAmount() / cost;
        }

        public double getPersonalDividendYieldMovingAverage()
        {
            if ((movingAverageCost == null) || (movingAverageCost.getAmount() <= 0))
                return 0;

            double cost = movingAverageCost.getAmount();

            if (tx().getShares() > 0)
                cost = movingAverageCost.getAmount() * (tx().getShares() / (double) totalShares);

            return getGrossValueAmount() / cost;
        }

        static long amountFractionPerShare(long amount, long shares)
        {
            if (shares == 0)
                return 0;

            return BigDecimal.valueOf(amount) //
                            .movePointLeft(Values.Amount.precision()) //
                            .movePointRight(Values.AmountFraction.precision()) //
                            .movePointRight(Values.Share.precision()) //
                            .divide(BigDecimal.valueOf(shares), Values.MC) //
                            .setScale(0, RoundingMode.HALF_EVEN).longValue();
        }

        public long getGrossValueAmount()
        {
            long taxes = tx().getUnits().filter(u -> u.getType() == Unit.Type.TAX)
                            .collect(MoneyCollectors.sum(tx().getCurrencyCode(), Unit::getAmount)).getAmount();

            return tx().getAmount() + taxes;
        }

        public Money getGrossValue()
        {
            return Money.of(tx().getCurrencyCode(), getGrossValueAmount());
        }
    }

    public abstract static class Valuation implements CalculationLineItem
    {
        private final Portfolio portfolio;
        private final SecurityPosition position;
        private final LocalDateTime date;

        protected Valuation(Portfolio portfolio, SecurityPosition position, LocalDateTime date)
        {
            this.portfolio = portfolio;
            this.position = position;
            this.date = date;
        }

        @Override
        public TransactionOwner<?> getOwner()
        {
            return portfolio;
        }

        @Override
        public String getLabel()
        {
            return Messages.LabelQuotation;
        }

        @Override
        public LocalDateTime getDateTime()
        {
            return date;
        }

        @Override
        public Money getValue()
        {
            return position.calculateValue();
        }

        @Override
        public Optional<SecurityPosition> getSecurityPosition()
        {
            return Optional.of(position);
        }
    }

    public static class ValuationAtStart extends Valuation
    {
        private ValuationAtStart(Portfolio portfolio, SecurityPosition position, LocalDateTime date)
        {
            super(portfolio, position, date);
        }
    }

    public static class ValuationAtEnd extends Valuation
    {
        private ValuationAtEnd(Portfolio portfolio, SecurityPosition position, LocalDateTime date)
        {
            super(portfolio, position, date);
        }
    }

    public static CalculationLineItem of(TransactionPair<?> transaction)
    {
        boolean isDividendPayment = transaction.getTransaction() instanceof AccountTransaction
                        && ((AccountTransaction) transaction.getTransaction())
                                        .getType() == AccountTransaction.Type.DIVIDENDS;

        return isDividendPayment ? new DividendPayment(transaction) : new TransactionItem(transaction);
    }

    public static CalculationLineItem of(Portfolio portfolio, PortfolioTransaction transaction)
    {
        return of(new TransactionPair<>(portfolio, transaction));
    }

    public static CalculationLineItem of(Account account, AccountTransaction transaction)
    {
        return of(new TransactionPair<>(account, transaction));
    }

    public static CalculationLineItem atStart(Portfolio portfolio, SecurityPosition position, LocalDateTime date)
    {
        return new ValuationAtStart(portfolio, position, date);
    }

    public static CalculationLineItem atEnd(Portfolio portfolio, SecurityPosition position, LocalDateTime date)
    {
        return new ValuationAtEnd(portfolio, position, date);
    }

    TransactionOwner<?> getOwner();

    String getLabel();

    LocalDateTime getDateTime();

    Money getValue();

    default Optional<Transaction> getTransaction()
    {
        return Optional.empty();
    }

    default Optional<SecurityPosition> getSecurityPosition()
    {
        return Optional.empty();
    }
}
