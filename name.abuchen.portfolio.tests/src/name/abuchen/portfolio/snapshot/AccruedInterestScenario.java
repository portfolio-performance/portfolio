package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * A bond bought and sold with accrued interest:
 * <ul>
 * <li>2024-01-02 deposit EUR 2,000.00</li>
 * <li>2024-03-01 buy 1,000 nominal at 100 %, accrued interest 10.00, fees 5.00
 * = EUR 1,015.00</li>
 * <li>2024-06-28 coupon EUR 30.00</li>
 * <li>2024-09-02 sell 1,000 nominal at 101 %, accrued interest 5.00, fees 5.00
 * = EUR 1,010.00</li>
 * </ul>
 * The interest income is 30.00 - 10.00 + 5.00 = 25.00, the realized capital
 * gain (without fees) 1,010.00 - 1,000.00 = 10.00.
 */
@SuppressWarnings("nls")
public final class AccruedInterestScenario
{
    public final Client client = new Client();
    public final Account account;
    public final Portfolio portfolio;
    public final Security bond;

    public AccruedInterestScenario()
    {
        account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setPercentageQuoted(true);
        bond.addPrice(new SecurityPrice(LocalDate.parse("2024-03-01"), Values.Quote.factorize(100)));
        bond.addPrice(new SecurityPrice(LocalDate.parse("2024-06-28"), Values.Quote.factorize(100)));
        bond.addPrice(new SecurityPrice(LocalDate.parse("2024-09-02"), Values.Quote.factorize(101)));
        client.addSecurity(bond);

        AccountTransaction deposit = new AccountTransaction();
        deposit.setType(AccountTransaction.Type.DEPOSIT);
        deposit.setDateTime(LocalDateTime.parse("2024-01-02T00:00"));
        deposit.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000)));
        account.addTransaction(deposit);

        buySell(PortfolioTransaction.Type.BUY, "2024-03-01T00:00", 1015, 10);

        AccountTransaction coupon = new AccountTransaction();
        coupon.setType(AccountTransaction.Type.DIVIDENDS);
        coupon.setDateTime(LocalDateTime.parse("2024-06-28T00:00"));
        coupon.setSecurity(bond);
        coupon.setShares(Values.Share.factorize(1000));
        coupon.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30)));
        account.addTransaction(coupon);

        buySell(PortfolioTransaction.Type.SELL, "2024-09-02T00:00", 1010, 5);
    }

    private void buySell(PortfolioTransaction.Type type, String date, double amount, double accruedInterest)
    {
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(type);
        entry.setDate(LocalDateTime.parse(date));
        entry.setSecurity(bond);
        entry.setShares(Values.Share.factorize(1000));
        entry.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.ACCRUED_INTEREST,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(accruedInterest))));
        entry.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5))));
        entry.insert();
    }

    public static Money eur(double amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    /**
     * Returns the (first) portfolio transaction of the given type, e.g. of a
     * filtered client.
     */
    public static PortfolioTransaction transaction(Client client, PortfolioTransaction.Type type)
    {
        return client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getType() == type).findFirst().orElseThrow();
    }
}
