package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.online.SecuritySearchProvider;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;

public class YahooSearchProvider implements SecuritySearchProvider
{
    private static final String SEARCH_URL = "http://de.finance.yahoo.com/lookup?s=%s&t=A&b=0&m=ALL"; //$NON-NLS-1$

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        try
        {
            URL url = new URL(String.format(SEARCH_URL, URLEncoder.encode(query + "*", "UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$
            Lexer lexer = new Lexer(url.openConnection());

            List<ResultItem> answer = new Visitor().visit(lexer);

            if (answer.isEmpty())
            {
                ResultItem item = new ResultItem();
                item.setName(String.format(Messages.MsgNoResults, query));
                answer.add(item);
            }
            else if (answer.size() == 20)
            {
                ResultItem item = new ResultItem();
                item.setName(Messages.MsgMoreResulstsAvailable);
                answer.add(item);
            }

            return answer;
        }
        catch (ParserException e)
        {
            throw new IOException(e);
        }
    }

    static class Visitor
    {
        private List<ResultItem> items;
        private ResultItem item;

        private boolean insideTable = false;
        private boolean insideTBody = false;
        private boolean insideRow = false;
        private boolean insideColumn = false;
        private int columnIndex = -1;

        public boolean tag(Tag tag) throws IOException
        {
            return true;
        }

        public boolean table(Tag tag) throws IOException
        {
            if (!insideTable)
            {
                if ("YFT_SL_TABLE_SUMMARY".equals(tag.getAttribute("SUMMARY"))) //$NON-NLS-1$ //$NON-NLS-2$
                    insideTable = true;
                return true;
            }

            insideTable = false;
            if (tag.isEndTag())
                return false;

            throw new IOException(MessageFormat.format(Messages.MsgUnexpectedTag, tag));
        }

        public boolean tbody(Tag tag) throws IOException
        {
            if (!insideTable)
                return true;

            if (!tag.isEndTag())
            {
                insideTBody = true;
            }
            else
            {
                insideTBody = false;
            }

            return true;
        }

        public boolean tr(Tag tag) throws IOException
        {
            if (!insideTBody)
                return true;

            if (!tag.isEndTag())
            {
                insideRow = true;
                item = new ResultItem();
            }
            else
            {
                insideRow = false;
                columnIndex = -1;

                if (item != null)
                    items.add(item);
                item = null;
            }
            return true;
        }

        public boolean td(Tag tag) throws IOException
        {
            if (!insideRow)
                return true;

            insideColumn = !tag.isEndTag();
            if (insideColumn)
                columnIndex++;

            return true;
        }

        public boolean text(Text text) throws IOException
        {
            if (!insideColumn)
                return true;

            String t = text.getText().trim();
            if (t.length() == 0)
                t = null;

            switch (columnIndex)
            {
                case 0:
                    item.setSymbol(t);
                    break;
                case 1:
                    item.setName(t);
                    break;
                case 2:
                    item.setIsin(t);
                    break;
                case 3:
                    if (!"NaN".equals(t)) //$NON-NLS-1$
                        item.setLastTrade(parseIndex(t));
                    break;
                case 4:
                    item.setType(t);
                    break;
                case 5:
                    item.setExchange(t);
                    break;
                default:
            }

            return true;
        }

        private int parseIndex(String text) throws IOException
        {
            try
            {
                DecimalFormat fmt = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
                Number q = fmt.parse(text);
                return (int) (q.doubleValue() * 100);
            }
            catch (ParseException e)
            {
                throw new IOException(e);
            }
        }

        public List<ResultItem> visit(Lexer lexer) throws ParserException, IOException
        {
            this.items = new ArrayList<ResultItem>();

            boolean doContinue = true;

            Node node = lexer.nextNode();
            while (node != null && doContinue)
            {
                if (node instanceof Tag)
                {
                    Tag tag = (Tag) node;
                    String tagName = tag.getTagName();

                    if ("TABLE".equals(tagName)) //$NON-NLS-1$
                        doContinue = table(tag);
                    else if ("TBODY".equals(tagName)) //$NON-NLS-1$
                        doContinue = tbody(tag);
                    else if ("TR".equals(tagName)) //$NON-NLS-1$
                        doContinue = tr(tag);
                    else if ("TD".equals(tagName)) //$NON-NLS-1$
                        doContinue = td(tag);
                    else
                        doContinue = tag(tag);
                }
                else if (node instanceof Text)
                {
                    doContinue = text((Text) node);
                }
                node = lexer.nextNode();
            }
            return this.items;
        }
    }
}
