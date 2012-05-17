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

public class ImportFinanzenNetQuotes
{
    private List<LatestSecurityPrice> items;
    private LatestSecurityPrice item;

    private boolean insideDiv = false;
    private boolean insideTable = false;
    private boolean insideRow = false;
    private boolean insideColumn = false;
    private int columnIndex = -1;

    private void div(Tag tag) throws IOException
    {
        if (!tag.isEndTag())
        {
            if ("content_box table_quotes".equals(tag.getAttribute("CLASS"))) //$NON-NLS-1$ //$NON-NLS-2$
                insideDiv = true;
        }
    }

    private void table(Tag tag) throws IOException
    {
        if (insideDiv && !tag.isEndTag())
            insideTable = true;
    }

    private void tr(Tag tag) throws IOException
    {
        if (!insideTable)
            return;

        if (!tag.isEndTag())
        {
            insideRow = true;
            item = new LatestSecurityPrice();
        }
        else
        {
            insideRow = false;
            columnIndex = -1;

            if (item != null && item.getTime() != null)
                items.add(item);
            item = null;
        }
    }

    private void td(Tag tag) throws IOException
    {
        if (!insideRow)
            return;

        insideColumn = !tag.isEndTag();
        if (insideColumn)
            columnIndex++;
    }

    private void text(Text text) throws IOException
    {
        if (!insideColumn)
            return;

        String t = text.getText().trim();
        if (t.length() == 0)
            t = null;

        switch (columnIndex)
        {
            case 0:
                item.setTime(parseDate(t));
                break;
            case 2:
                item.setValue(parsePrice(t));
                break;
            case 3:
                item.setHigh(parsePrice(t));
                break;
            case 4:
                item.setLow(parsePrice(t));
                break;
            case 5:
                item.setVolume(parseVolumne(t));
                break;
            default:
        }
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
            SimpleDateFormat fmtTradeDate = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
            return fmtTradeDate.parse(text);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    private int parseVolumne(String text) throws IOException
    {
        if (text == null || text.trim().length() == 0)
            return 0;

        try
        {
            DecimalFormat fmt = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
            Number q = fmt.parse(text);
            return q.intValue();
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    public List<LatestSecurityPrice> extract(String htmlSource) throws IOException
    {
        if (htmlSource.indexOf("finanzen.net") < 0) //$NON-NLS-1$
            return Collections.emptyList();

        this.items = new ArrayList<LatestSecurityPrice>();
        this.item = null;

        insideDiv = insideTable = insideRow = insideColumn = false;
        columnIndex = -1;

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

                    if ("DIV".equals(tagName)) //$NON-NLS-1$
                        div(tag);
                    else if ("TABLE".equals(tagName)) //$NON-NLS-1$
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
