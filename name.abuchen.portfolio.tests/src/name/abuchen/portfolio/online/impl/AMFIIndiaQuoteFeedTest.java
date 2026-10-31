package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AMFIIndiaQuoteFeedTest
{
    @Test
    public void testParseNAVAllFile()
    {
        // @formatter:off
        String content = """
                        Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Plan;Option;Net Asset Value;Date
                        119551;INF209KA12Z1;INF209KA13Z9;Aditya Birla Sun Life Banking & PSU Debt Fund;Direct Plan;IDCW-Re-investment;106.8821;21-Aug-2026
                        """;
        // @formatter:on

        var funds = new AMFIIndiaQuoteFeed().parse(content);

        // the header line has 8 columns as well, but must not be parsed as fund
        assertThat(funds.size(), is(1));

        var fund = funds.get(0);
        assertThat(fund.schemeCode(), is("119551"));
        assertThat(fund.isin1(), is("INF209KA12Z1"));
        assertThat(fund.isin2(), is("INF209KA13Z9"));
        assertThat(fund.price().getDate(), is(LocalDate.of(2026, 8, 21)));
        assertThat(fund.price().getValue(), is(Values.Quote.factorize(106.8821)));
    }
}
