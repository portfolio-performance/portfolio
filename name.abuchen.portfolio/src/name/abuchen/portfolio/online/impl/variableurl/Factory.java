package name.abuchen.portfolio.online.impl.variableurl;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Factory
{
    private static final Pattern URL_MACRO_ABSTRACT = Pattern.compile("\\{(.*?)}"); //$NON-NLS-1$

    private Factory()
    {}

    public static VariableURL fromString(CharSequence input)
    {
        List<CharSequence> parts = new LinkedList<>();
        Matcher abstractMatcher = URL_MACRO_ABSTRACT.matcher(input);
        int lastEnd = 0;

        while (abstractMatcher.find())
        {
            parts.add(input.subSequence(lastEnd, abstractMatcher.start()));
            lastEnd = abstractMatcher.end();

            parts.add(abstractMatcher.group(1));
        }

        parts.add(input.subSequence(lastEnd, input.length()));

        if (parts.size() > 1)
        {
            try
            {
                return new FormattedDate(parts);
            }
            catch (IllegalArgumentException ignored)
            {
                // try next pattern
            }

            try
            {
                return new PageNumber(parts);
            }
            catch (IllegalArgumentException ignored)
            {
                // try next pattern
            }
        }

        return new ConstString(input);
    }
}
