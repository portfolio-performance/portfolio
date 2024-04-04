package name.abuchen.portfolio.online.impl.variableurl.macros;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class Today implements Macro
{
    private static final Pattern MACRO = Pattern.compile("TODAY(:([^:]*)(:([PYMWDpymwd0123456789-]*))?)?"); //$NON-NLS-1$

    private final DateTimeFormatter formatter;
    private final TemporalAmount delta;
    private final boolean useUnixTime;

    public Today(CharSequence input)
    {
        Matcher matcher = MACRO.matcher(input);

        if (!matcher.matches())
            throw new IllegalArgumentException(input.toString());

        String p = matcher.group(2);

        useUnixTime = "unixtime".equals(p); //$NON-NLS-1$

        if (useUnixTime)
            formatter = null;
        else if (p == null || p.isEmpty())
            formatter = DateTimeFormatter.ISO_DATE;
        else
            formatter = DateTimeFormatter.ofPattern(p);

        String d = matcher.group(4);
        if (d == null || d.isEmpty())
            delta = Period.ZERO;
        else
            delta = Period.parse(d);
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }

    @Override
    public CharSequence resolve(Security security)
    {
        LocalDate ld = LocalDate.now().plus(delta);

        if (useUnixTime)
            return String.valueOf(ld.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC));
        else
            return formatter.format(ld);
    }
}
