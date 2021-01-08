package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public abstract class Values<E>
{
    public static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public static final class MoneyValues extends Values<Money>
    {
        private MoneyValues()
        {
            super("#,##0.00", 2); //$NON-NLS-1$
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

        @Override
        public String formatNonZero(Money amount)
        {
            return amount == null || amount.isZero() ? null : format(amount);
        }

        @Override
        public String formatNonZero(Money amount, double threshold)
        {
            boolean isNotZero = amount != null && Math.abs(amount.getAmount()) >= threshold;
            return isNotZero ? format(amount) : null;
        }

        public String formatNonZero(Money amount, String skipCurrencyCode)
        {
            return amount == null || amount.isZero() ? null : format(amount, skipCurrencyCode);
        }
    }

    public static final class QuoteValues extends Values<Long>
    {
        private static final String QUOTE_PATTERN = "#,##0.00######"; //$NON-NLS-1$

        private static final ThreadLocal<DecimalFormat> QUOTE_FORMAT = ThreadLocal // NOSONAR
                        .withInitial(() -> new DecimalFormat(QUOTE_PATTERN));

        private final BigDecimal factorToMoney;
        private final int precisionDeltaToMoney;

        private QuoteValues()
        {
            super(QUOTE_PATTERN, 8);

            factorToMoney = BigDecimal.valueOf(factor() / Values.Money.factor());
            precisionDeltaToMoney = precision() - Values.Money.precision();
        }

        @Override
        public String format(Long quote)
        {
            return QUOTE_FORMAT.get().format(quote / divider());
        }

        public String format(String currencyCode, long quote, String skipCurrency)
        {
            if (currencyCode == null || skipCurrency.equals(currencyCode))
                return format(quote);
            else
                return format(currencyCode, quote);
        }

        public String format(String currencyCode, long quote)
        {
            return currencyCode + " " + format(quote); //$NON-NLS-1$
        }

        public String format(Quote quote)
        {
            return format(quote.getCurrencyCode(), quote.getAmount());
        }

        public String format(Quote quote, String skipCurrency)
        {
            return format(quote.getCurrencyCode(), quote.getAmount(), skipCurrency);
        }

        public String formatNonZero(Quote amount, String skipCurrencyCode)
        {
            return amount == null || amount.isZero() ? null : format(amount, skipCurrencyCode);
        }

        /**
         * Factor by which to multiply a monetary amount to convert it into a
         * quote amount. Monetary amounts have 2 decimal digits while quotes
         * have 4 digits.
         */
        public int factorToMoney()
        {
            return factor() / Values.Money.factor();
        }

        /**
         * Divider by which to divide a quote amount to convert it into a
         * monetary amount. Monetary amounts have 2 decimal digits while quotes
         * have 4 digits.
         */
        public double dividerToMoney()
        {
            return divider() / Values.Money.divider();
        }

        public BigDecimal getBigDecimalFactorToMoney()
        {
            return factorToMoney;
        }

        public int precisionDeltaToMoney()
        {
            return precisionDeltaToMoney;
        }
    }

    public static final Values<Long> Amount = new Values<Long>("#,##0.00", 2) //$NON-NLS-1$
    {
        @Override
        public String format(Long amount)
        {
            return String.format("%,.2f", amount / divider()); //$NON-NLS-1$
        }
    };

    public static final MoneyValues Money = new MoneyValues(); // NOSONAR

    public static final Values<Long> AmountFraction = new Values<Long>("#,##0.00###", 5) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long share)
        {
            return format.format(share / divider());
        }
    };

    /**
     * Optionally format values without decimal places. Currently used only for
     * attributes attached to the security.
     */
    public static final Values<Long> AmountPlain = new Values<Long>("#,##0.##", 2) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long amount)
        {
            return format.format(amount / divider());
        }
    };

    public static final Values<Long> AmountShort = new Values<Long>("#,##0", 2) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long amount)
        {
            return format.format(amount / divider());
        }
    };

    public static final Values<Long> Share = new Values<Long>("#,##0.######", 8) //$NON-NLS-1$
    {
        private final DecimalFormat format = new DecimalFormat(pattern());

        @Override
        public String format(Long share)
        {
            return format.format(share / divider());
        }
    };

    public static final QuoteValues Quote = new QuoteValues(); // NOSONAR

    public static final Values<BigDecimal> ExchangeRate = new Values<BigDecimal>("#,##0.0000", 0)//$NON-NLS-1$
    {
        @Override
        public String format(BigDecimal exchangeRate)
        {
            return String.format("%,.4f", exchangeRate); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Index = new Values<Integer>("#,##0.00", 2) //$NON-NLS-1$
    {
        @Override
        public String format(Integer index)
        {
            return String.format("%,.2f", index / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<LocalDate> Date = new Values<LocalDate>("yyyy-MM-dd", 0) //$NON-NLS-1$
    {
        DateTimeFormatter formatter = DateTimeFormatter
                        .ofLocalizedDate(new Locale("pt").getLanguage().equals(Locale.getDefault() //$NON-NLS-1$
                                        .getLanguage()) ? FormatStyle.SHORT : FormatStyle.MEDIUM);

        @Override
        public String format(LocalDate date)
        {
            return formatter.format(date);
        }
    };

    public static final Values<LocalDateTime> DateTime = new Values<LocalDateTime>("yyyy-MM-dd HH:mm", 0) //$NON-NLS-1$
    {
        DateTimeFormatter formatter = DateTimeFormatter
                        .ofLocalizedDateTime(new Locale("pt").getLanguage().equals(Locale.getDefault() //$NON-NLS-1$
                                        .getLanguage()) ? FormatStyle.SHORT : FormatStyle.MEDIUM, FormatStyle.SHORT);

        @Override
        public String format(LocalDateTime date)
        {
            if (date.toLocalTime().equals(LocalTime.MIDNIGHT))
                return Values.Date.format(date.toLocalDate());
            else
                return formatter.format(date);
        }
    };

    public static final Values<Double> Thousands = new Values<Double>("0.###k", 0) //$NON-NLS-1$
    {
        private ThreadLocal<DecimalFormat> numberFormatter = ThreadLocal // NOSONAR
                        .withInitial(() -> new DecimalFormat("#,##0.###")); //$NON-NLS-1$

        @Override
        public String format(Double value)
        {
            return numberFormatter.get().format(value / 1000) + "k"; //$NON-NLS-1$
        }
    };

    public static final Values<Double> Percent = new Values<Double>("0.00%", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f", percent * 100); //$NON-NLS-1$
        }
    };

    public static final Values<Double> PercentShort = new Values<Double>("0.00%", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.1f", percent * 100); //$NON-NLS-1$
        }
    };

    public static final Values<Double> PercentPlain = new Values<Double>("0.00", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f", percent); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Weight = new Values<Integer>("#,##0.00", 2) //$NON-NLS-1$
    {
        @Override
        public String format(Integer weight)
        {
            return String.format("%,.2f", weight / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> WeightPercent = new Values<Integer>("#,##0.00", 2) //$NON-NLS-1$
    {
        @Override
        public String format(Integer weight)
        {
            return String.format("%,.2f%%", weight / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Double> Percent2 = new Values<Double>("0.00%", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.2f%%", percent * 100); //$NON-NLS-1$
        }
    };

    public static final Values<Double> Percent5 = new Values<Double>("0.00000%", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Double percent)
        {
            return String.format("%,.5f%%", percent * 100); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Id = new Values<Integer>("#,##0", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Integer amount)
        {
            return String.format("%,.0f", amount / divider()); //$NON-NLS-1$
        }
    };

    public static final Values<Integer> Year = new Values<Integer>("0", 0) //$NON-NLS-1$
    {
        @Override
        public String format(Integer amount)
        {
            return String.valueOf(amount);
        }
    };

    private final String pattern;
    private final int factor;
    private final double divider;
    private final int precision;
    private final BigDecimal bdFactor;

    private Values(String pattern, int precision)
    {
        this.pattern = pattern;
        this.factor = BigInteger.TEN.pow(precision).intValue();
        this.divider = factor;
        this.precision = precision;
        this.bdFactor = BigDecimal.valueOf(factor);
    }

    public final String pattern()
    {
        return pattern;
    }

    public final double divider()
    {
        return divider;
    }

    public final int factor()
    {
        return factor;
    }

    /**
     * The number of decimal digits, for example the shares are stored in a long
     * with 8 decimal digits.
     */
    public final int precision()
    {
        return precision;
    }

    /**
     * Returns the factor as BigDecimal
     */
    public BigDecimal getBigDecimalFactor()
    {
        return bdFactor;
    }

    public long factorize(double value)
    {
        return Math.round(value * factor);
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
