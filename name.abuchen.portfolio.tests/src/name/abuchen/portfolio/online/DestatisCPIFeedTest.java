package name.abuchen.portfolio.online;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.model.ConsumerPriceIndex;

import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;
import org.junit.Test;

@SuppressWarnings("nls")
public class DestatisCPIFeedTest
{

    @Test
    public void testParsingHtml() throws IOException, ParserException
    {
        String html = new Scanner(getClass().getResourceAsStream("response_destatis.txt"), "UTF-8") //
                        .useDelimiter("\\A").next();

        Lexer lexer = new Lexer(html);
        List<ConsumerPriceIndex> prices = new DestatisCPIFeed.Visitor().visit(lexer);

        assertThat(prices.size(), equalTo(19 /* years in file */* 12 + 6));

        ConsumerPriceIndex p = prices.get(5);
        assertThat(p.getYear(), equalTo(2012));
        assertThat(p.getMonth(), equalTo(Calendar.JANUARY));
        assertThat(p.getIndex(), equalTo(11150));
    }
}
