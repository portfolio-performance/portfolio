package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;
import name.abuchen.portfolio.online.impl.variableurl.urls.DateURL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormattedDate implements Macro
{
    private static final Pattern MACRO = Pattern.compile("DATE:(.*?)"); //$NON-NLS-1$

    private DateTimeFormatter formatter;

    public FormattedDate(CharSequence input) throws IllegalArgumentException
    {
        Matcher matcher = MACRO.matcher(input);

        if (!matcher.matches())
            throw new IllegalArgumentException("Bad date macro: " + input); //$NON-NLS-1$

        // throws IllegalArgumentException
        formatter = DateTimeFormatter.ofPattern(matcher.group(1));
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return DateURL::new;
    }

    @Override
    public CharSequence resolve(Security security) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    public String resolve(LocalDate date)
    {
        return date.format(formatter);
    }
}
