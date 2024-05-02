package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.util.WebAccess;

@SuppressWarnings("nls")
public class DivvyDiaryDividendFeedTest
{

    @Test
    public void testInstantiationAndSetters()
    {
        DivvyDiaryDividendFeed dddf = new DivvyDiaryDividendFeed();

        assertThat("cache is null", dddf.cache, notNullValue());
    }

    @Test
    public void testGetDividendPayments() throws Exception
    {
        try (MockedStatic<FrameworkUtil> util = Mockito.mockStatic(FrameworkUtil.class))
        {
            Bundle bundle = createVersionReturningBundle();
            util.when(() -> FrameworkUtil.getBundle(DivvyDiaryDividendFeed.class)).thenReturn(bundle);

            Security emptyIsinSec = new Security("EmptyISIN Inc", "", "NOIS", "myfeed");
            Security noDivSec = new Security("Evotec", "DEEVOTECISIN", "EVOC", "myfeed");
            Security divSec = new Security("Xtrackers", "LUXTRACKERS", "XTGS", "myfeed");

            String[] retJson = new String[] { "json" };
            Security[] expectedSec = new Security[] { noDivSec };
            DivvyDiaryDividendFeed dddf = createCUT(expectedSec, retJson);
            List<DividendEvent> ret;

            ret = dddf.getDividendPayments(noDivSec);
            assertThat("list should be empty (no api key)", ret.isEmpty(), is(true));
            dddf.setApiKey("apikey");
            IOException ex = assertThrows(IOException.class, () -> dddf.getDividendPayments(noDivSec));
            assertThat(ex.getMessage(), is("server returned data that doesn't seem to be JSON"));

            // try the non-isin-sec now, which should lead to an empty list
            // again. This way we know that no actual web access has taken
            // place, otherwise the same exception as just checked would occur
            ret = dddf.getDividendPayments(emptyIsinSec);
            assertThat("list shouldn't be null", ret, notNullValue());
            assertThat("list should be empty (no api key)", ret.isEmpty(), is(true));
            assertThat(dddf.cache.lookup(emptyIsinSec.getIsin()), nullValue());

            retJson[0] = "{\"name\": \"some_other_json_content_with_missing_dividend_block\"}";
            ex = assertThrows(IOException.class, () -> dddf.getDividendPayments(noDivSec));
            assertThat(ex.getMessage(), is("server returned an unexpected JSON-format"));
            assertThat(dddf.cache.lookup(noDivSec.getIsin()), nullValue());

            retJson[0] = getJSON("divvy_diary_response_no_payments.json");
            ret = dddf.getDividendPayments(noDivSec);
            assertThat("list shouldn't be null", ret, notNullValue());
            assertThat("list should be empty", ret.isEmpty(), is(true));
            assertThat(dddf.cache.lookup(noDivSec.getIsin()), notNullValue());

            // check that the cached data is used - the new content would lead
            // to an exception
            retJson[0] = "{\"name\": \"some_other_json_content_with_missing_dividend_block\"}";
            ret = dddf.getDividendPayments(noDivSec);
            assertThat("list shouldn't be null", ret, notNullValue());
            assertThat("list should be empty", ret.isEmpty(), is(true));

            retJson[0] = getJSON("divvy_diary_response_with_payments.json");
            expectedSec[0] = divSec;
            ret = dddf.getDividendPayments(divSec);
            assertThat("list shouldn't be null", ret, notNullValue());
            assertThat("list shouldn't be empty", ret.isEmpty(), is(false));
            assertThat("list mismatch", getListAsString(ret), is("2024-02-21,2024-03-07,EUR 0.56\r\n"
                            + "2023-11-15,2023-11-30,EUR 0.57\r\n" + "2023-08-23,2023-09-07,EUR 0.56\r\n"
                            + "2023-05-24,2023-06-07,EUR 0.47\r\n" + "2023-02-08,2023-02-24,EUR 0.44\r\n"
                            + "2022-11-09,2022-11-25,EUR 0.43\r\n" + "2022-08-10,2022-08-25,EUR 0.43\r\n"
                            + "2022-06-08,2022-06-17,EUR 0.4\r\n" + "2022-04-27,2022-05-03,EUR 1.27\r\n"
                            + "2021-05-21,2021-05-28,EUR 1.43\r\n" + "2020-04-22,2020-04-27,EUR 1.43\r\n"
                            + "2019-04-11,2019-04-18,EUR 1.3\r\n" + "2018-04-09,2018-04-18,EUR 1.26\r\n"
                            + "2017-04-06,2017-04-19,EUR 1.09\r\n" + "2016-04-04,2016-04-13,EUR 0.87\r\n"
                            + "2015-04-02,2015-04-15,EUR 0.57\r\n" + "2014-07-24,2014-07-31,EUR 1.06\r\n"
                            + "2013-07-25,2013-07-31,EUR 1.02\r\n" + "2012-07-25,2012-07-30,EUR 0.95\r\n"
                            + "2011-07-22,2011-07-29,EUR 1.1\r\n" + "2010-07-23,2010-07-30,EUR 1.3\r\n"
                            + "2009-07-27,2009-07-31,EUR 0.53\r\n" + "2008-08-22,2008-08-29,EUR 0.84\r\n"));

            assertThat(dddf.cache.lookup(divSec.getIsin()), notNullValue());
        }
    }

    private String getListAsString(List<DividendEvent> ret)
    {
        StringBuilder sb = new StringBuilder();
        for (DividendEvent de : ret)
        {
            sb.append(de.getDate() + "," + de.getPaymentDate() + "," + de.getAmount().getCurrencyCode() + " "
                            + (de.getAmount().getAmount() / 100d) + "\r\n");
        }
        return sb.toString();
    }

    private String getJSON(String name) throws IOException
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(name), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
        }
    }

    private Bundle createVersionReturningBundle()
    {
        Bundle mock = Mockito.mock(Bundle.class);
        Mockito.when(mock.getVersion()).thenReturn(Version.valueOf("1.2.3"));
        return mock;
    }

    private DivvyDiaryDividendFeed createCUT(Security[] sec, String[] retjson)
    {
        return new DivvyDiaryDividendFeed()
        {
            @Override
            WebAccess createWebAccess(String host, String path)
            {
                assertThat("returned web access is not null", super.createWebAccess(host, path), notNullValue());
                assertThat("host mismatch", host, is("api.divvydiary.com"));
                assertThat("path mismatch", path, is("/symbols/" + sec[0].getIsin()));

                return new WebAccess(host, path)
                {
                    @Override
                    public WebAccess addParameter(String param, String value)
                    {
                        fail("shouldn't be called here");
                        return this;
                    }

                    @Override
                    public WebAccess addHeader(String param, String value)
                    {
                        assertThat("header param mismatch", param, is("X-API-Key"));
                        assertThat("header value mismatch", value, is("apikey"));
                        return super.addHeader(param, value);
                    }

                    @Override
                    public WebAccess addUserAgent(String userAgent)
                    {
                        assertThat(userAgent, is("PortfolioPerformance/1.2.3"));
                        return super.addUserAgent(userAgent);
                    }

                    @Override
                    public String get() throws IOException
                    {
                        return retjson[0];
                    }
                };
            }
        };
    }
}
