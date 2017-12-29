package name.abuchen.portfolio.online.impl;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Element;

import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.money.Monetary;
import name.abuchen.portfolio.util.Strings;

public class HTMLTableEventParser extends HTMLTableParser
{
    public HTMLTableEventParser()
    {        
        COLUMNS = new Column[] { new DateColumn(), new TypeColumn() , new ValueColumn()  , new RatioColumn()};
    }
    
    public Object newRowObject()
    {
        SecurityEvent event = new SecurityEvent(); 
        return (SecurityEvent) event;
        //return new SecurityEvent();
    }

    public List<SecurityElement> parseFromURL(String url, List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(super._parseFromURL(url, errors));
    }

    public List<SecurityElement> parseFromHTML(String html, List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(super._parseFromHTML(html, errors));
    }
    
    private static class DateColumn extends Column
    {
        private DateTimeFormatter[] formatters;

        @SuppressWarnings("nls")
        public DateColumn()
        {
            super(new String[] { "Datum", "Date" });

            formatters = new DateTimeFormatter[] { DateTimeFormatter.ofPattern("y-M-d"),
                            new DateTimeFormatterBuilder().appendPattern("d.M.").appendValueReduced(ChronoField.YEAR, 2, 2, Year.now().getValue() - 80).toFormatter(), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d.M.y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM. y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM dd, y", Locale.ENGLISH) //$NON-NLS-1$
            };
        }

        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            SecurityEvent event = (SecurityEvent) obj;
            String text = Strings.strip(value.text());
            for (int ii = 0; ii < formatters.length; ii++)
            {
                try
                {
                    LocalDate date = LocalDate.parse(text, formatters[ii]);
                    event.setDate(date);
                    return;
                }
                catch (DateTimeParseException e) // NOSONAR
                {
                    // continue with next pattern
                }
            }

            throw new ParseException(text, 0);
        }
    }


    private static class TypeColumn extends Column
    {
        @SuppressWarnings("nls")
        public TypeColumn()
        {
            super(new String[] { "Ereignis"});
        }
        
        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            SecurityEvent event = (SecurityEvent) obj;
            String type = value.text().trim();
            
            if (type.matches("Dividende") || type.matches("Ausschüttung"))
                event.setType(SecurityEvent.Type.STOCK_DIVIDEND);
            else if (type.matches("Split") || type.matches("Reverse Split")) 
                event.setType(SecurityEvent.Type.STOCK_SPLIT);                
            else if (type.matches("Bezugsrecht"))
                event.setType(SecurityEvent.Type.STOCK_RIGHT);
            else
            {
                event.setType(SecurityEvent.Type.STOCK_OTHER);                
                event.setTypeStr(type);
            }
        }
    }
    
    private static class RatioColumn extends Column
    {
        @SuppressWarnings("nls")
        public RatioColumn()
        {
            super(new String[] { "Verhältnis"});
        }
        
        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            SecurityEvent event = (SecurityEvent) obj;
            if (value.text().matches("^[0-9,.]+:[0-9,.]+$"))
            {
                String[] elements = value.text().trim().split(":");
                if (elements.length > 2)
                    throw new ParseException(value.toString(), 0);
                event.setRatio(asDouble(elements[0], languageHint), asDouble(elements[1], languageHint));
            }
            else if (value.text().matches("^[0-9,.]+$"))
                event.setRatio(asDouble(value.text().trim(), languageHint));
            else
                event.clearRatio();
        }
    }

    private static class ValueColumn extends Column
    {
        @SuppressWarnings("nls")
        public ValueColumn()
        {
            super(new String[] { "Betrag"});
        }
        
        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            SecurityEvent event = (SecurityEvent) obj;
            Monetary money = new Monetary().parse(value.text().trim(), languageHint);
            if (money.getValue() != null)
                event.setAmount(money);
            else
                event.clearAmount();
        }
    }

    
    protected final boolean isSpecValid(List<Spec> specs)
    {
        if (specs == null || specs.isEmpty())
            return false;

        boolean hasDate = false;
        boolean hasType = false;

        for (Spec spec : specs)
        {
            hasDate = hasDate || spec.getColumn() instanceof DateColumn;
            hasType = hasType || spec.getColumn() instanceof TypeColumn;
        }

        return hasDate && hasType;
    }

  
}