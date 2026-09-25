package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;

import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

/**
 * A {@link TransactionFixture} with some history: 1000 deposited on "Cash",
 * 500 on "Cash 2", 10 "EUR Share" bought for 500 through "Broker" (on
 * "Cash"), quoted at 60 since 2026-01-01; a 30 dividend (5 taxes) and a 2
 * interest on "Cash".
 */
@SuppressWarnings("nls")
public class ReportFixture extends TransactionFixture
{
    public static final String DATE = "2026-07-20";

    public ReportFixture()
    {
        eurSecurity.addPrice(new SecurityPrice(LocalDate.parse("2026-01-01"), Values.Quote.factorize(60)));

        create("{'type':'deposit','cashAccount':'" + cash.getUUID() + "','date':'2026-01-02','amount':'1000'}");
        create("{'type':'deposit','cashAccount':'" + cash2.getUUID() + "','date':'2026-01-02','amount':'500'}");
        create("{'type':'buy','investmentAccount':'" + broker.getUUID() + "','cashAccount':'" + cash.getUUID()
                        + "','instrument':'" + eurSecurity.getUUID()
                        + "','date':'2026-01-10','shares':'10','quote':'50'}");
    }

    /** adds the dividend and the interest, which change the balance of "Cash" by +27 */
    public void addEarnings()
    {
        create("{'type':'dividends','cashAccount':'" + cash.getUUID() + "','instrument':'" + eurSecurity.getUUID()
                        + "','date':'2026-03-01','shares':'10','grossValue':'30','taxes':'5'}");
        create("{'type':'interest','cashAccount':'" + cash.getUUID() + "','date':'2026-04-01','amount':'2'}");
    }
}
