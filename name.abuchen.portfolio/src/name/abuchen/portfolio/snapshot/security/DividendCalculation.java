package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Periodicity;
import name.abuchen.portfolio.snapshot.security.CostCalculation.DividendCostContext;
import name.abuchen.portfolio.util.Dates;

/* package */class DividendCalculation extends Calculation
{
    public record DividendCalculationResult(Money sum, LocalDate lastDividendPayment, int numOfEvents,
                    Periodicity periodicity)
    {
    }

    /**
     * A dividend payment.
     */
    private static class Payment
    {
        /**
         * Amount of the payment.
         */
        public final Money amount;
        /**
         * Date of the payment.
         */
        public final LocalDate date;
        /**
         * Year of the payment.
         */
        public final int year;
        /**
         * Constructs an instance.
         *
         * @param converter
         *            currency converter
         * @param t
         *            {@link DividendTransaction}
         */
        public Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
        {
            this.amount = t.getGrossValue().with(converter.at(t.getDateTime()));
            LocalDateTime time = t.getDateTime();
            this.year = time.getYear();
            this.date = time.toLocalDate();
        }
    }

    private final List<Payment> payments = new ArrayList<>();
    private Periodicity periodicity;
    private MutableMoney sum;

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        // no payments result in no periodicity
        if (payments.isEmpty())
        {
            this.periodicity = Periodicity.NONE;
            return;
        }

        // default is unknown periodicity
        this.periodicity = Periodicity.UNKNOWN;

        // first sort
        Collections.sort(payments, (r, l) -> r.date.compareTo(l.date));

        // get first and last payment
        LocalDate firstPayment = payments.get(0).date;
        LocalDate lastPayment = payments.get(payments.size() - 1).date;

        int significantCount = 0;
        int insignificantYears = 0;

        // first calc total sum of all payments
        for (Payment p : payments)
        {
            // add to total sum
            sum.add(p.amount);
        }

        // now walk through individual years
        for (int year = firstPayment.getYear(); year <= lastPayment.getYear(); year++)
        {
            int countPerYear = 0;
            long sumPerYear = 0;
            LocalDate lastDate = null;

            // first calc sum only for this year
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    countPerYear++;
                    sumPerYear += p.amount.getAmount();
                }
            }

            // skip years with no dividend payments
            if (countPerYear == 0)
            {
                insignificantYears++;
                continue;
            }

            // calc expected amount for this year
            double expectedAmount = sumPerYear / (double) countPerYear;

            // then calc significance
            for (Payment p : payments)
            {
                if (p.year == year)
                {
                    // check if dividend contributes the expected amount (if
                    // it is not a very small extraordinary payment below 30% of
                    // the expected one)
                    double significance = p.amount.getAmount() / expectedAmount;
                    if (significance > 0.3)
                    {
                        // check, if dividends were recorded for multiple
                        // accounts at the same date
                        if (lastDate == null || !p.date.equals(lastDate))
                        {
                            significantCount++;
                        }
                    }
                    lastDate = p.date;
                }
            }
        }

        // determine periodicity?
        if (significantCount > 0)
        {
            // days in current time range
            int days = Dates.daysBetween(firstPayment, lastPayment) - (insignificantYears * 365);
            long daysBetweenPayments = Math.round(days / (double) (significantCount - 1));

            // just check payments inbetween one year
            if (daysBetweenPayments < 430)
            {
                if (daysBetweenPayments > 270)
                {
                    this.periodicity = Periodicity.ANNUAL;
                }
                else if (daysBetweenPayments > 130)
                {
                    this.periodicity = Periodicity.SEMIANNUAL;
                }
                else if (daysBetweenPayments > 60)
                {
                    this.periodicity = Periodicity.QUARTERLY;
                }
                else if (daysBetweenPayments > 20)
                {
                    this.periodicity = Periodicity.MONTHLY;
                }
            }
        }
    }

    public DividendCalculationResult getResult()
    {
        return new DividendCalculationResult(sum.toMoney(), getLastDividendPayment(), payments.size(), periodicity);
    }

    public LocalDate getLastDividendPayment()
    {
        return payments.isEmpty() ? null : payments.get(payments.size() - 1).date;
    }

    public int getNumOfEvents()
    {
        return payments.size();
    }

    public List<Payment> getPayments()
    {
        return payments;
    }

    public Periodicity getPeriodicity()
    {
        return periodicity;
    }

    public Money getSum()
    {
        return sum.toMoney();
    }

    @Override
    public void setTermCurrency(String termCurrency)
    {
        super.setTermCurrency(termCurrency);
        this.sum = MutableMoney.of(termCurrency);
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment t)
    {
        // construct new payment and add it to the list
        payments.add(new Payment(converter, t));
    }
}

/* package */class DividendRateOfReturnCalculation extends Calculation
{
    private static final class Payment
    {
        private final LocalDate date;
        private final double rateOfReturn;

        private Payment(CurrencyConverter converter, CalculationLineItem.DividendPayment payment, Security security,
                        Optional<DividendCostContext> context)
        {
            Money amount = payment.getGrossValue().with(converter.at(payment.getDateTime()));
            this.date = payment.getDateTime().toLocalDate();

            // Keep the existing fallback and NaN behavior unchanged.
            double rr = Double.NaN;
            if (security != null)
            {
                if (context.isPresent() && !context.get().cost().isZero())
                    rr = amount.getAmount() / (double) context.get().cost().getAmount();

                if (rr == 0)
                {
                    SecurityPrice price = security.getSecurityPrice(date);
                    long priceValue = price.getValue();
                    if (priceValue != 0)
                    {
                        double sharePriceAmount = ((double) priceValue) / Values.Quote.factor()
                                        * Values.AmountFraction.factor();
                        rr = payment.getDividendPerShare() / sharePriceAmount;
                    }
                }
            }
            this.rateOfReturn = rr;
        }
    }

    private final Function<CalculationLineItem.DividendPayment, Optional<DividendCostContext>> costProvider;
    private final List<Payment> payments = new ArrayList<>();
    private double rateOfReturnPerYear;

    public DividendRateOfReturnCalculation(
                    Function<CalculationLineItem.DividendPayment, Optional<DividendCostContext>> costProvider)
    {
        this.costProvider = costProvider;
    }

    @Override
    public void visit(CurrencyConverter converter, CalculationLineItem.DividendPayment payment)
    {
        payments.add(new Payment(converter, payment, getSecurity(), costProvider.apply(payment)));
    }

    @Override
    public void finish(CurrencyConverter converter, List<CalculationLineItem> lineItems)
    {
        if (payments.isEmpty())
            return;

        Collections.sort(payments, (r, l) -> r.date.compareTo(l.date));
        int firstYear = payments.get(0).date.getYear();
        int lastYear = payments.get(payments.size() - 1).date.getYear();
        int years = lastYear - firstYear + 1;

        rateOfReturnPerYear = payments.stream().mapToDouble(payment -> payment.rateOfReturn).sum() / years;
    }

    public double getRateOfReturnPerYear()
    {
        return rateOfReturnPerYear;
    }
}
