package name.abuchen.portfolio;

import java.time.LocalDateTime;
import java.util.UUID;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class PortfolioBuilder
{
    private Portfolio portfolio;
    private Account account;

    public PortfolioBuilder()
    {
        this.portfolio = new Portfolio();
        this.portfolio.setName(UUID.randomUUID().toString());
    }

    public PortfolioBuilder(Account referenceAccount)
    {
        this();
        this.portfolio.setReferenceAccount(referenceAccount);
    }

    public PortfolioBuilder inbound_delivery(Security security, String date, long shares, long amount)
    {
        return inbound_delivery(security, AccountBuilder.asDateTime(date), shares, amount);
    }

    public PortfolioBuilder inbound_delivery(Security security, LocalDateTime date, long shares, long amount)
    {
        portfolio.addTransaction(new PortfolioTransaction(date, CurrencyUnit.EUR, amount, security, shares,
                        Type.DELIVERY_INBOUND, 0, 0));
        return this;
    }

    public Portfolio addTo(Client client)
    {
        client.addPortfolio(portfolio);
        if (account != null)
            client.addAccount(account);
        return portfolio;
    }

    public PortfolioBuilder buy(Security security, String date, long shares, long amount)
    {
        return buysell(Type.BUY, security, date, shares, amount, 0);
    }

    public PortfolioBuilder sell(Security security, String date, long shares, long amount)
    {
        return buysell(Type.SELL, security, date, shares, amount, 0);
    }

    public PortfolioBuilder sell(Security security, String date, long shares, long amount, int fees)
    {
        return buysell(Type.SELL, security, date, shares, amount, fees);
    }

    private PortfolioBuilder buysell(Type type, Security security, String date, long shares, long amount, int fees)
    {
        if (portfolio.getReferenceAccount() == null)
        {
            account = new Account(UUID.randomUUID().toString());
            portfolio.setReferenceAccount(account);
        }

        BuySellEntry entry = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
        entry.setType(type);
        entry.setDate(AccountBuilder.asDateTime(date));
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(amount);
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, fees)));

        entry.insert();

        return this;
    }
}
