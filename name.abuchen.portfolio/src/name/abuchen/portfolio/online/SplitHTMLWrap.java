package name.abuchen.portfolio.online;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

/* package */ public final class SplitHTMLWrap
{

    /* package */ public static String getSplitHTML(String text, char seperator)
    {
        // split by line break
        String lines[] = text.split("\\r?\\n"); //$NON-NLS-1$

        // split each line by separator
        StringBuilder sb = new StringBuilder();
        sb = sb.append("<table>"); //$NON-NLS-1$
        for (String line : lines)
        {
            ArrayList<String> lineSplit = customSplitSpecific(line, seperator);
            sb = sb.append("<tr>"); //$NON-NLS-1$
            for (int i = 0; i < lineSplit.size(); i++)
                sb = sb.append("<td>" + escapeChars(lineSplit.get(i)) + "</td>"); //$NON-NLS-1$//$NON-NLS-2$
            sb = sb.append("</tr>"); //$NON-NLS-1$
        }
        sb = sb.append("</tr></table>"); //$NON-NLS-1$
        return (sb.toString());
    }

    public static ArrayList<String> customSplitSpecific(String input, char seperator)
    {
        ArrayList<String> words = new ArrayList<String>();
        boolean notInsideSeperator = true;
        int start = 0;
        for (int i = 0; i < input.length() - 1; i++)
        {
            if (input.charAt(i) == seperator && notInsideSeperator)
            {
                String word = input.substring(start, i);
                word = StringUtils.removeStart(word, "\""); //$NON-NLS-1$
                word = StringUtils.removeEnd(word, "\""); //$NON-NLS-1$
                words.add(word);
                start = i + 1;
            }
            else if (input.charAt(i) == '"')
                notInsideSeperator = !notInsideSeperator;
        }
        String word = input.substring(start);
        word = StringUtils.removeStart(word, "\""); //$NON-NLS-1$
        word = StringUtils.removeEnd(word, "\""); //$NON-NLS-1$
        words.add(word);
        return words;
    }

    public static String escapeChars(String lineIn)
    {
        StringBuilder sb = new StringBuilder();
        int lineLength = lineIn.length();
        for (int i = 0; i < lineLength; i++)
        {
            char c = lineIn.charAt(i);
            switch (c)
            {
                case '"':
                    sb.append("&quot;"); //$NON-NLS-1$
                    break;
                case '&':
                    sb.append("&amp;"); //$NON-NLS-1$
                    break;
                case '\'':
                    sb.append("&apos;"); //$NON-NLS-1$
                    break;
                case '<':
                    sb.append("&lt;"); //$NON-NLS-1$
                    break;
                case '>':
                    sb.append("&gt;"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
