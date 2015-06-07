package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;

public abstract class Values<E>
{
    public static final class MoneyValues extends Values<Money>
    {
        private MoneyValues()
        {
            super("#,##0.00", 100D, 100); //$NON-NLS-1$
        }

        @Override
        public String format(Money amount)
        {
            return String.format("%s %,.2f", amount.getCurrencyCode(), amount.getAmount() / divider()); //$NON-NLS-1$
        }

        public String format(Money amount, String skipCurrencyCode)
        {
            if (skipCurrencyCode.equals(amount.getCurrencyCode()))
                return String.format("%,.2f", amount.getAmount() / divider()); //$NON-NLS-1$
            else
                return format(amount);
        }

        public String formatNonZero(Money amount, String skipCurrencyCode)
        {
            return amount.isZero() ? null : format(amount, skipCurrencyCode);
        }
    }

    public static final Values<Long> Amount = new Values<Long>("#,##0.00", 100D, 100) //$NON-NLS-1$
    {
        @Override
        public String format(Long amount)
        {
            return String.format("%,.2f", amount / divider()); //$NON-NLS-1$
        }
    };

    public static final MoneyValues Money = new MoneyValues();

    public static final Values<Long> AmountFraction = new Values<Long>("#,##0.00###", 100000D, 100000) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long share)
        {
            return format.format(share / divider());
        }
    };

    public static final Values<Long> AmountPlain = new Values<Long>("#,##0.##", 100D, 100) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long amount)
        {
            return format.format(amount / divider());
        }
    };

    public static final Values<Long> Share = new Values<Long>("#,##0.######", 1000000D, 1000000) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long share)
        {
            return format.format(share / divider());
        }
    };

    public static final Values<Long> Quote = new Values<Long>("#,##0.00", 100D, 100) //$NON-NLS-1$
    {
        @Override
        public String format(Long quote)
        {
            return String.format("%,.2f", quote / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<BigDecimal> ExchangeRate = new Values<BigDecimal>("0.0000", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(BigDecimal exchangeRate)
        {
            return String.format("%,.4f", exchangeRate); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Index = new Values<Integer>("#,##0.00", 100D, 100) //$NON-NLS-1$
    {
        @Override
        public String format(Integer index)
        {
            return String.format("%,.2f", index / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Date> Date = new Values<Date>("yyyy-MM-dd", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Date date)
        {
            return String.format("%tF", date); //$NON-NLS-1$
        }
    };

    public static final Values<Double> Percent = new Values<Double>("0.00%", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f", percent * 100); //$NON-NLS-1$
        }
    };

    public static final Values<Double> PercentPlain = new Values<Double>("0.00", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f", percent); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Weight = new Values<Integer>("#,##0.00", 100D, 100) //$NON-NLS-1$
    {
        @Override
        public String format(Integer weight)
        {
            return String.format("%,.2f", weight / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Double> Percent2 = new Values<Double>("0.00%", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f%%", percent * 100); //$NON-NLS-1$ 
        }
    };

    public static final Values<Double> Percent5 = new Values<Double>("0.00000%", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.5f%%", percent * 100); //$NON-NLS-1$ 
        }
    };

    public static final Values<Integer> Id = new Values<Integer>("#,##0", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Integer amount)
        {
            return String.format("%,.0f", amount / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Year = new Values<Integer>("0", 1D, 1) //$NON-NLS-1$
    {
        @Override
        public String format(Integer amount)
        {
            return String.valueOf(amount);
        }
    };

    private final String pattern;
    private final double divider;
    private final int factor;

    private Values(String pattern, double divider, int factor)
    {
        this.pattern = pattern;
        this.divider = divider;
        this.factor = factor;
    }

    public String pattern()
    {
        return pattern;
    }

    public double divider()
    {
        return divider;
    }

    public int factor()
    {
        return factor;
    }
    
    public long factorize(double value)
    {
        return Double.valueOf(value * factor).longValue();
    }

    public abstract String format(E amount);

    public String formatNonZero(E amount)
    {
        if (amount instanceof Double)
        {
            Double d = (Double) amount;

            if (d.isNaN())
                return null;
            else if (d.doubleValue() == 0d)
                return null;
            else
                return format(amount);
        }
        else if (amount instanceof Number)
        {
            boolean isNotZero = ((Number) amount).longValue() != 0;
            return isNotZero ? format(amount) : null;
        }

        throw new UnsupportedOperationException();
    }

    public String formatNonZero(E amount, double threshold)
    {
        if (amount instanceof Double)
        {
            boolean isNotZero = Math.abs(((Double) amount).doubleValue()) >= threshold;
            return isNotZero ? format(amount) : null;
        }

        throw new UnsupportedOperationException();
    }

}
