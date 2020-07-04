package name.abuchen.portfolio.model;

import java.time.LocalDate;
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

    public static class DividendPayment extends SecurityEvent
    {
        private LocalDate payDate;
        private Money amount;
        private String source;

        public DividendPayment()
        {
            super(null, Type.DIVIDEND_PAYMENT, null);
        }

        public DividendPayment(LocalDate exDate, LocalDate payDate, Money amount, String source)
        {
            super(exDate, Type.DIVIDEND_PAYMENT, null);
            this.payDate = payDate;
            this.amount = amount;
            this.source = source;
        }

        @Override
        public void setType(Type type)
        {
            if (type != Type.DIVIDEND_PAYMENT)
                throw new IllegalArgumentException();
        }

        public LocalDate getPayDate()
        {
            return payDate;
        }

        public void setPayDate(LocalDate payDate)
        {
            this.payDate = payDate;
        }

        public Money getAmount()
        {
            return amount;
        }

        public void setAmount(Money amount)
        {
            this.amount = amount;
        }

        public String getSource()
        {
            return source;
        }

        public void setSource(String source)
        {
            this.source = source;
        }
    }

    private LocalDate date;
    private Type type;
    private String details;

    public SecurityEvent()
    {
        // xstream
    }

    public SecurityEvent(LocalDate date, Type type, String details)
    {
        this.date = date;
        this.type = type;
        this.details = details;
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
}
