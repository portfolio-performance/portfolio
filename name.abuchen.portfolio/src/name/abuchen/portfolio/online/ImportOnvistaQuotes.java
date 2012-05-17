package name.abuchen.portfolio.online;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.model.LatestSecurityPrice;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;

public class ImportOnvistaQuotes
{
    private List<LatestSecurityPrice> items;
    private LatestSecurityPrice item;

    private boolean hasOnlyClosingQuote;

    private boolean insideTable = false;
    private boolean insideColumn = false;
    private int tableIndex = -1;
    private int columnIndex = -1;
    private int rowIndex = -1;

    private void table(Tag tag) throws IOException
    {
        if (!tag.isEndTag())
        {
            tableIndex++;
            if (tableIndex == 1)
            {
                insideTable = true;
                rowIndex = -1;
            }
        }
    }

    private boolean tr(Tag tag) throws IOException
    {
        if (!insideTable)
            return true;

        if (!tag.isEndTag())
        {
            item = new LatestSecurityPrice();
            rowIndex++;
        }
        else
        {
            columnIndex = -1;

            if (item != null && item.getTime() != null)
                items.add(item);
            item = null;
        }
        return true;
    }

    private boolean td(Tag tag) throws IOException
    {
        if (rowIndex < 2) // skip first two lines
            return true;

        insideColumn = !tag.isEndTag();
        if (insideColumn)
            columnIndex++;

        return true;
    }

    private boolean text(Text text) throws IOException
    {
        if (!insideColumn)
            return true;

        String t = text.getText().trim();
        if (t.length() == 0)
            t = null;

        switch (columnIndex)
        {
            case 0:
                item.setTime(parseDate(t));
                break;
            case 1:
                if (hasOnlyClosingQuote)
                    item.setValue(parsePrice(t));
            case 2:
                if (!hasOnlyClosingQuote)
                    item.setLow(parsePrice(t));
                break;
            case 3:
                if (!hasOnlyClosingQuote)
                    item.setHigh(parsePrice(t));
                break;
            case 4:
                if (!hasOnlyClosingQuote)
                    item.setValue(parsePrice(t));
                break;
            default:
        }

        return true;
    }

    private long parsePrice(String text) throws IOException
    {
        try
        {
            DecimalFormat fmt = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
            Number q = fmt.parse(text);
            return (long) (q.doubleValue() * 100);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    private Date parseDate(String text) throws IOException
    {
        try
        {
            SimpleDateFormat fmtTradeDate = new SimpleDateFormat("dd.MM.yy"); //$NON-NLS-1$
            return fmtTradeDate.parse(text);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    public List<LatestSecurityPrice> extract(String htmlSource) throws IOException
    {
        if (htmlSource.indexOf("onvista.de") < 0) //$NON-NLS-1$
            return Collections.emptyList();

        hasOnlyClosingQuote = htmlSource.indexOf("R&uuml;cknahmepreis") >= 0; //$NON-NLS-1$

        this.items = new ArrayList<LatestSecurityPrice>();
        this.item = null;

        insideTable = insideColumn = false;
        columnIndex = tableIndex = rowIndex = -1;

        try
        {
            Lexer lexer = new Lexer(htmlSource);

            Node node = lexer.nextNode();
            while (node != null)
            {
                if (node instanceof Tag)
                {
                    Tag tag = (Tag) node;
                    String tagName = tag.getTagName();

                    if ("TABLE".equals(tagName)) //$NON-NLS-1$
                        table(tag);
                    else if ("TR".equals(tagName)) //$NON-NLS-1$
                        tr(tag);
                    else if ("TD".equals(tagName)) //$NON-NLS-1$
                        td(tag);
                }
                else if (node instanceof Text)
                {
                    text((Text) node);
                }
                node = lexer.nextNode();
            }
            return this.items;
        }
        catch (ParserException e)
        {
            throw new IOException(e);
        }
    }

}
