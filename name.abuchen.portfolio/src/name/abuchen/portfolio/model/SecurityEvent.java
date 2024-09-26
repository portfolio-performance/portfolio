package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;

import name.abuchen.portfolio.money.Money;

public class SecurityEvent
{
    public enum Type
    {
        STOCK_SPLIT(false), NOTE(true), DIVIDEND_PAYMENT(false);

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        private boolean isUserEditable;

        private Type(boolean isUserEditable)
        {
            this.isUserEditable = isUserEditable;
        }

        public boolean isUserEditable()
        {
            return isUserEditable;
        }

        @Override
        public String toString()
        {
            return RESOURCES.getString("event." + name()); //$NON-NLS-1$
        }
    }

    public static class DividendEvent extends SecurityEvent
    {
        private LocalDate paymentDate;
        private Money amount;

        public DividendEvent()
        {
            super(null, Type.DIVIDEND_PAYMENT, null);
        }

        public DividendEvent(LocalDate exDate, LocalDate payDate, Money amount, String source)
        {
            super(exDate, Type.DIVIDEND_PAYMENT, null, source);
            this.paymentDate = payDate;
            this.amount = amount;
        }

        @Override
        public void setType(Type type)
        {
            if (type != Type.DIVIDEND_PAYMENT)
                throw new IllegalArgumentException("type must be DIVIDEND_PAYMENT but was " + type.name()); //$NON-NLS-1$
        }

        public LocalDate getPaymentDate()
        {
            return paymentDate;
        }

        public void setPaymentDate(LocalDate payDate)
        {
            this.paymentDate = payDate;
        }

        public Money getAmount()
        {
            return amount;
        }

        public void setAmount(Money amount)
        {
            this.amount = amount;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(amount, paymentDate);
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            DividendEvent other = (DividendEvent) obj;
            return Objects.equals(amount, other.amount) && Objects.equals(paymentDate, other.paymentDate);
        }
    }

    private LocalDate date;
    private Type type;
    private String details;

    /**
     * Machine-readable identifier of external source used to create (and
     * manage) this event.
     */
    private String source;

    public SecurityEvent()
    {
        // xstream
    }

    public SecurityEvent(LocalDate date, Type type, String details)
    {
        this(date, type, details, null);
    }

    public SecurityEvent(LocalDate date, Type type, String details, String source)
    {
        this.date = date;
        this.type = type;
        this.details = details;
        this.source = source;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(date, details, type, source);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SecurityEvent other = (SecurityEvent) obj;
        return Objects.equals(date, other.date) && Objects.equals(details, other.details)
                        && Objects.equals(source, other.source) && type == other.type;
    }
}
