package name.abuchen.portfolio.online;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.jsoup.nodes.Element;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.money.Values;


public abstract class Column
{
    static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_GERMAN = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMAN)); //$NON-NLS-1$
        }
    };

    static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_ENGLISH = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH)); //$NON-NLS-1$
        }
    };

    static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_APOSTROPHE = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols(Locale.US);
            unusualSymbols.setGroupingSeparator('\'');
            return new DecimalFormat("#,##0.##", unusualSymbols); //$NON-NLS-1$
        }
    };

    private final Pattern[] patterns;

    protected Column(String[] strings)
    {
        this.patterns = new Pattern[strings.length];
        for (int ii = 0; ii < strings.length; ii++)
            this.patterns[ii] = Pattern.compile(strings[ii]);
    }

    public boolean matches(Element header)
    {
        String text = header.text();
        for (Pattern pattern : patterns)
        {
            if (pattern.matcher(text).matches())
                return true;
        }
        return false;
    }

    public abstract void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException;

    protected long asQuote(Element value, String languageHint) throws ParseException
    {
        String text = value.text().trim();

        DecimalFormat format = null;

        if ("de".equals(languageHint)) //$NON-NLS-1$
            format = DECIMAL_FORMAT_GERMAN.get();
        else if ("en".equals(languageHint)) //$NON-NLS-1$
            format = DECIMAL_FORMAT_ENGLISH.get();

        if (format == null)
        {
            // check first for apostrophe

            int apostrophe = text.indexOf('\'');
            if (apostrophe >= 0)
                format = DECIMAL_FORMAT_APOSTROPHE.get();
        }

        if (format == null)
        {
            // determine format based on the relative location of the last
            // comma and dot, e.g. the last comma indicates a German number
            // format
            int lastDot = text.lastIndexOf('.');
            int lastComma = text.lastIndexOf(',');
            format = Math.max(lastDot, lastComma) == lastComma ? DECIMAL_FORMAT_GERMAN.get()
                            : DECIMAL_FORMAT_ENGLISH.get();
        }

        double quote = format.parse(text).doubleValue();
        return Math.round(quote * Values.Quote.factor());
    }
    
}
