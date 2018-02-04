package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.Monetary;
import name.abuchen.portfolio.money.Values;

public class SecurityEvent extends SecurityElement
{
    public enum Type
    {
        STOCK_SPLIT, STOCK_DIVIDEND, STOCK_RIGHT, STOCK_OTHER, NONE;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("event." + name()); //$NON-NLS-1$
        }
    }

    private Type type = Type.NONE;

    private Monetary amount = null;

    private LocalDate exDate = null;

    private double[] ratio = null;

    private String typeStr = null;

    private boolean isVisible = true;

    @Deprecated
    protected String details = null;
    @Deprecated
    protected long value;

    public SecurityEvent()
    {
    }

    public SecurityEvent(LocalDate date, Type type)
    {
        super.setDate(date);
        setType(type);
    }
    
    public SecurityEvent setAmount(Monetary money)
    {
        this.amount = money;
        return this;
    }

    public SecurityEvent setAmount(String currencyCode, double value)
    {
        this.amount = new Monetary().valueOf(currencyCode, BigDecimal.valueOf(value));
        return this;
    }

    public SecurityEvent setAmount(String currencyCode, BigDecimal value)
    {
        this.amount = new Monetary().valueOf(currencyCode, value);
        return this;
    }

    public Monetary getAmount()
    {
        if (amount == null)
            throw new NoSuchElementException();
        return amount;
    }

    public SecurityEvent clearAmount()
    {
        if (amount != null && amount.getCurrency().equals(Messages.LabelNoCurrencyCode))
        {
            amount = null;
        }
        return this;
    }

    public SecurityEvent setExDate(LocalDate date)
    {
        this.exDate = date;
        return this;
    }

    public LocalDate getExDate()
    {
        if (exDate == null)
            return this.date;
        return this.exDate;
    }

    public SecurityEvent setRatio(double enumerator, double denumerator)
    {
        ratio = new double[]{enumerator, denumerator};
        return this;
    }

    public SecurityEvent setRatio(double enumerator)
    {
        ratio = new double[]{enumerator, (double) 1.0};
        return this;
    }

    public String getRatioString()
    {
        if (ratio == null || ratio.length != 2)
        {
            System.err.println("SecurityEvent.getRatioString() - event: " + String.format("[%s] EVENT %tF (ex: %tF): [%s-%s] amount: <%s> ratio: <-/->", 
                                            (isVisible ? "+"                                           : "o" ),
                                            date,
                                            (exDate == null  ? LocalDate.of(1900, Month.JANUARY, 1)  : exDate),
                                             type.toString(),
                                            (typeStr == null ? ""                                    : typeStr),
                                            (amount == null  ? new Monetary()                        :  amount).toString()
                                            ));
            throw new NoSuchElementException();
        }
        // taken from http://stackoverflow.com/questions/8741107/format-a-double-to-omit-unnecessary-0-and-never-round-off
        double e = ratio[0];
        String eStr = (long) e == e ? "" + (long) e : "" + e; 
        double d = ratio[1];
        String dStr = (long) d == d ? "" + (long) d : "" + d; 
        return  eStr  + ":" + dStr;
    }

    public double[] getRatio()
    {
        if (ratio == null)
            throw new NoSuchElementException();
        return ratio;
    }
    
    public SecurityEvent clearRatio()
    {
        ratio = null;
        return this;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }

    public SecurityEvent setTypeStr(String str)
    {
        this.typeStr = str;
        return this;
    }

    public String getTypeStr()
    {
        return (typeStr == null ? type.toString() : typeStr);
    }

    public SecurityEvent clearTypeStr()
    {
        if (typeStr != null && typeStr.equals(type.toString()))
        {
            typeStr = null;
        }
        return this;
    }

    @Deprecated
    public String getDetails()
    {
          if (details != null)
              return details;
          else
              return "";
    }
    
    public String getExplaination()
    {
        if (type.equals(Type.STOCK_DIVIDEND))
        {
            return getAmount().toString();
        }
        else if (type.equals(Type.STOCK_SPLIT))
        {
            return getRatioString();
        }
        else if (type.equals(Type.STOCK_RIGHT))
        {
            return (ratio == null ? "" : getRatioString()) + (amount == null? "" : " " + Messages.LabelStockRightReference + " - " + getAmount().toString());
        }
        else if (type.equals(Type.STOCK_OTHER))
        {
            return getTypeStr() + ": " + (ratio == null ? "" : getRatioString()) + (amount == null? "" : " @ " + getAmount().toString());
        }
        else
        {
            return Type.NONE.toString();
        }
    }

    public SecurityEvent clearDetails()
    {
        this.details = null;
        return this;
    }

    public SecurityEvent hide()
    {
        isVisible = false;
        return this;
    }

    public SecurityEvent unhide()
    {
        isVisible = true;
        return this;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public String toString()
    {
        return String.format("[%s] EVENT %tF (ex: %tF): [%s-%s] amount: <%s> ratio: <%s> => [%08x] deprecated: value %,10.2f details: <%s>", 
                        (isVisible ? "+"                                           : "o" ),
                        date,
                        (exDate == null  ? LocalDate.of(1900, Month.JANUARY, 1)  : exDate),
                         type.toString(),
                        (typeStr == null ? ""                                    : typeStr),
                        (amount == null  ? new Monetary()                        :  amount).toString(),
                        (ratio  == null ? Messages.LabelNoRatio                  :  getRatioString()),
                        this.hashCode(),
                        value / Values.Quote.divider(),
                        (details == null ? "?"                                   : details.toString())
                        );
    }

    public static List<SecurityEvent> castElement2EventList(List<SecurityElement> iList)
    {
        if (iList != null)
        {
            List<SecurityEvent> oList = new ArrayList<>();
            for (SecurityElement e : iList)
            {
                    if (e instanceof SecurityEvent)
                        oList.add((SecurityEvent) e); // need to cast each object specifically
            }
            return oList;
        }
        else
        {
            return null;
        }
    }

      @Override
      public int hashCode()
      {
          final int prime = 31;
          int result = 1;
          result = prime * result + ((date    == null) ? 0                     : date.hashCode());
          result = prime * result + ((exDate  == null) ? 0                     : exDate.hashCode());
          result = prime * result + ((details == null) ? "?"                   : details).hashCode();
          result = prime * result + ((ratio == null)   ? Messages.LabelNoRatio : getRatioString()).hashCode();
          result = prime * result + type.hashCode();
          result = prime * result + getTypeStr().hashCode();
          result = prime * result + ((amount == null)  ? new Monetary()        : amount).toString().hashCode();
          return result;
      }

//    @Override
//    public int compareTo(SecurityEvent o)
//    {
//        return super.date.compareTo(o.date);
//    }


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
        if (date == null)
        {
            if (other.date != null)
                return false;
        }
        else if (!date.equals(other.date))
            return false;
        if (hashCode() != other.hashCode())
            return false;
        return true;
    }

}