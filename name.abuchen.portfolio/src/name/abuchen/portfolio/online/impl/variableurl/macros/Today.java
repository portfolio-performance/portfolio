package name.abuchen.portfolio.online.impl.variableurl.macros;

import java.time.LocalDate;
import java.time.Period;
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

    public Today(CharSequence input)
    {
        Matcher matcher = MACRO.matcher(input);

        if (!matcher.matches())
            throw new IllegalArgumentException();

        String p = matcher.group(2);
        if (p == null || p.isEmpty())
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
        return formatter.format(LocalDate.now().plus(delta));
    }
}
