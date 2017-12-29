package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.online.impl.HTMLTableEventParser;

@SuppressWarnings("nls")
public class HTMLTableEventFeedTest
{

    @Test
    public void testParsingHtml() throws IOException
    {
        // search: http://www.ariva.de/commerzbank-aktie/historische_ereignisse

        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("response_html_events.txt"), "UTF-8"))
        {
            String html = scanner.useDelimiter("\\A").next();

            List<Exception> errors = new ArrayList<Exception>();

            List<SecurityElement> elements = new HTMLTableEventParser().parseFromHTML(html, errors);

            if (!errors.isEmpty())
                System.err.println("HTMLTableEventTest.testParsingHtml - errors: " + errors.toString());

            assertThat(errors.size(), equalTo(0));

            //System.err.println("HTMLTableEventTest.testParsingHtml - elements: " + elements.toString());
            assertThat(elements.size(), equalTo(28));

            assertThat(elements.get( 2), //
                            is(new SecurityEvent(LocalDate.of(2013, Month.APRIL  , 24), SecurityEvent.Type.STOCK_SPLIT   )
                                    .setRatio((double) 10.0, (double) 1.0)));
            assertThat(elements.get( 3), //
                            is(new SecurityEvent(LocalDate.of(2011, Month.MAY    , 24), SecurityEvent.Type.STOCK_RIGHT   )
                                    .setRatio((double) 11.0, (double) 10.0)
                                    .setAmount("EUR", (double) 21.80)));
            assertThat(elements.get( 8), //
                            is(new SecurityEvent(LocalDate.of(2004, Month.JUNE   ,  2), SecurityEvent.Type.STOCK_DIVIDEND)
                                    .setAmount("EUR", (double) 1.00)));
            assertThat(elements.get(14), //
                            is(new SecurityEvent(LocalDate.of(1999, Month.JANUARY,  4), SecurityEvent.Type.STOCK_OTHER   )
                                .setTypeStr("Euro-Umstellung")    
                                .setRatio((double) 0.51129, (double) 1.0)));
            assertThat(elements.get(18), //
                            is(new SecurityEvent(LocalDate.of(1996, Month.OCTOBER,  1), SecurityEvent.Type.STOCK_SPLIT   )
                                    .setRatio((double) 1.0, (double) 10.0)));

        }
    }
}
