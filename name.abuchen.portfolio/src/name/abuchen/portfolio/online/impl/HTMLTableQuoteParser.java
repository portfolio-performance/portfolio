package name.abuchen.portfolio.online.impl;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Element;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.util.Strings;

public class HTMLTableQuoteParser extends HTMLTableParser
{
    public HTMLTableQuoteParser()
    {        
        COLUMNS = new Column[] { new DateColumn(), new CloseColumn(), new HighColumn(),
                        new LowColumn(), new VolumeColumn() };
    }
    
    public Object newRowObject()
    {
        LatestSecurityPrice price = new LatestSecurityPrice(); 
        return (LatestSecurityPrice) price;
    }

    private <T extends SecurityElement> List<T> castList(List<Object> Olist, Class<T> clazz, List<Exception> errors)
    {
        List<T> Tlist = new ArrayList<>();
        for (Object obj : Olist)
        {
            if (obj instanceof SecurityElement)
                Tlist.add((T) obj); // need to cast each object specifically
            else
                errors.add(new ClassCastException());
        }
        return Tlist;
    }
    
    public List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        return castList(super._parseFromURL(url, errors), LatestSecurityPrice.class, errors);
    }

    public List<LatestSecurityPrice> parseFromHTML(String html, List<Exception> errors)
    {
        return castList(super._parseFromHTML(html, errors), LatestSecurityPrice.class, errors);
    }
    
    private static class DateColumn extends Column
    {
        private DateTimeFormatter[] formatters;

        @SuppressWarnings("nls")
        public DateColumn()
        {
            super(new String[] { "Datum", "Date" });

            formatters = new DateTimeFormatter[] { DateTimeFormatter.ofPattern("y-M-d"),
                            DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
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
            LatestSecurityPrice price = (LatestSecurityPrice) obj;
            String text = Strings.strip(value.text());
            for (int ii = 0; ii < formatters.length; ii++)
            {
                try
                {
                    LocalDate date = LocalDate.parse(text, formatters[ii]);
                    price.setDate(date);
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

    private static class CloseColumn extends Column
    {
        @SuppressWarnings("nls")
        public CloseColumn()
        {
            super(new String[] { "Schluss.*", "Schluß.*", "Rücknahmepreis.*", "Close.*", "Zuletzt", "Price",
                            "akt. Kurs" });
        }

        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            LatestSecurityPrice price = (LatestSecurityPrice) obj;
            price.setValue(asQuote(value, languageHint));
        }
    }

    private static class HighColumn extends Column
    {
        @SuppressWarnings("nls")
        public HighColumn()
        {
            super(new String[] { "Hoch.*", "Tageshoch.*", "Max.*", "High.*" });
        }

        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            LatestSecurityPrice price = (LatestSecurityPrice) obj;
            if ("-".equals(value.text().replace("\u00a0", "").trim())) //$NON-NLS-1$
                price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            else
            {
                price.setHigh(asQuote(value, languageHint));
            }
        }
    }

    private static class LowColumn extends Column
    {
        @SuppressWarnings("nls")
        public LowColumn()
        {
            super(new String[] { "Tief.*", "Tagestief.*", "Low.*" });
        }

        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            LatestSecurityPrice price = (LatestSecurityPrice) obj;
            if ("-".equals(value.text().replace("\u00a0", "").trim())) //$NON-NLS-1$
                price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            else
            {
                price.setLow(asQuote(value, languageHint));
            }
        }
    }

    private static class VolumeColumn extends Column
    {
        @SuppressWarnings("nls")
        public VolumeColumn()
        {
            super(new String[] { "Volume.*", "Umsatz" , "Stücke" });
        }

        @Override
        public void setValue(Element value, Object obj, String languageHint) throws ParseException
        {
            LatestSecurityPrice price = (LatestSecurityPrice) obj;
            if ("-".equals(value.text().replace("\u00a0", "").trim())) //$NON-NLS-1$
                price.setVolume((int) LatestSecurityPrice.NOT_AVAILABLE);
            else
                price.setVolume(super.asInt(value, languageHint));
        }
    }
    
    @Override
    protected boolean isSpecValid(List<Spec> specs)
    {
        if (specs == null || specs.isEmpty())
            return false;

        boolean hasDate = false;
        boolean hasClose = false;

        for (Spec spec : specs)
        {
            hasDate = hasDate || spec.getColumn() instanceof DateColumn;
            hasClose = hasClose || spec.getColumn() instanceof CloseColumn;
        }

        return hasDate && hasClose;
    }

  
}