package name.abuchen.portfolio.ui.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

public class DialogTextUtils
{
    public static String addMarkdownLikeHyperlinks(String text, List<StyleRange> styles)
    {
        Pattern pattern = Pattern.compile("\\[(?<text>[^\\]]*)\\]\\((?<link>[^\\)]*)\\)"); //$NON-NLS-1$
        Matcher matcher = pattern.matcher( text);

        StringBuilder answer = new StringBuilder( text.length());
        int pointer = 0;

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            answer.append( text.substring(pointer, start));

            String linkText = matcher.group("text"); //$NON-NLS-1$
            String link = matcher.group("link"); //$NON-NLS-1$

            StyleRange styleRange = new StyleRange();
            styleRange.underline = true;
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underlineColor = Colors.theme().hyperlink();
            styleRange.foreground = Colors.theme().hyperlink();
            styleRange.data = link;
            styleRange.start = answer.length();
            styleRange.length = linkText.length();
            styles.add(styleRange);

            answer.append(linkText);

            pointer = end;
        }

        if (pointer <  text.length())
            answer.append( text.substring(pointer));

        return answer.toString();
    }

    public static void addBoldFirstLine(String text, List<StyleRange> styles)
    {
        StyleRange styleRange = new StyleRange();
        styleRange.fontStyle = SWT.BOLD;
        styleRange.start = 0;
        styleRange.length = text.indexOf('\n');
        styles.add(styleRange);
    }
}
