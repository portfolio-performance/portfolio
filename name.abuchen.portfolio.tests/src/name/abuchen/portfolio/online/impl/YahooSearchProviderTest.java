package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;
import org.junit.Test;

@SuppressWarnings("nls")
public class YahooSearchProviderTest
{

    @Test
    public void testParsingHtml() throws IOException, ParserException
    {
        String html = new Scanner(getClass().getResourceAsStream("response_yahoo_search.txt"), "UTF-8") //
                        .useDelimiter("\\A").next();

        Lexer lexer = new Lexer(html);
        List<ResultItem> items = new YahooSearchProvider.Visitor().visit(lexer);

        assertThat(items.size(), equalTo(20));

        ResultItem p = items.get(0);
        assertThat(p.getSymbol(), equalTo("SAP.DE"));
        assertThat(p.getName(), equalTo("SAP AG"));
        assertThat(p.getIsin(), equalTo("DE0007164600"));
        assertThat(p.getLastTrade(), equalTo(5081));
        assertThat(p.getType(), equalTo("Stock"));
        assertThat(p.getExchange(), equalTo("GER"));
    }

    @Test
    public void testParsingHtmlWithSpecialNumbers() throws IOException, ParserException
    {
        String html = new Scanner(getClass().getResourceAsStream("response_yahoo_search2.txt"), "UTF-8") //
                        .useDelimiter("\\A").next();

        Lexer lexer = new Lexer(html);
        List<ResultItem> items = new YahooSearchProvider.Visitor().visit(lexer);

        assertThat(items.size(), equalTo(20));
        ResultItem p = items.get(5);
        assertThat(p.getSymbol(), equalTo("2815.HK"));
        assertNull(p.getName());
        assertThat(p.getIsin(), equalTo("FR0010372185"));
        assertThat(p.getLastTrade(), equalTo(0));
        assertNull(p.getType());
        assertNull(p.getExchange());
    }

}
