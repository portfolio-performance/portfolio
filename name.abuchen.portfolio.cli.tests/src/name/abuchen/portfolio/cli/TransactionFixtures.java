package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;

/**
 * Builds the joined entries that the shared JUnit builders don't cover:
 * records in other currencies, with a time of day, and transfers.
 */
public final class TransactionFixtures
{
    private TransactionFixtures()
    {
    }

    public static LocalDateTime at(String dateTime)
    {
        return dateTime.indexOf('T') < 0 ? LocalDateTime.parse(dateTime + "T00:00") : LocalDateTime.parse(dateTime);
    }

    /** Amounts are in minor units: 100_500 is 1005.00. */
    public static BuySellEntry buy(Portfolio portfolio, Account account, Security security, String dateTime,
                    long shares, long amount)
    {
        return buySell(PortfolioTransaction.Type.BUY, portfolio, account, security, dateTime, shares, amount);
    }

    public static BuySellEntry sell(Portfolio portfolio, Account account, Security security, String dateTime,
                    long shares, long amount)
    {
        return buySell(PortfolioTransaction.Type.SELL, portfolio, account, security, dateTime, shares, amount);
    }

    private static BuySellEntry buySell(PortfolioTransaction.Type type, Portfolio portfolio, Account account,
                    Security security, String dateTime, long shares, long amount)
    {
        var entry = new BuySellEntry(portfolio, account);
        entry.setType(type);
        entry.setDate(at(dateTime));
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setMonetaryAmount(Money.of(account.getCurrencyCode(), amount));
        entry.insert();
        return entry;
    }

    public static AccountTransaction cash(Account account, AccountTransaction.Type type, String dateTime, long amount)
    {
        var transaction = new AccountTransaction(at(dateTime), account.getCurrencyCode(), amount, null, type);
        account.addTransaction(transaction);
        return transaction;
    }

    public static AccountTransferEntry cashTransfer(Account from, Account to, String dateTime, long amount)
    {
        var entry = new AccountTransferEntry(from, to);
        entry.setDate(at(dateTime));
        entry.setCurrencyCode(from.getCurrencyCode());
        entry.setAmount(amount);
        entry.insert();
        return entry;
    }

    /**
     * A transfer between accounts of different currencies. Core keeps the rate
     * as a gross value unit on the outgoing side: its amount is the outgoing
     * amount, its forex the incoming one.
     */
    public static AccountTransferEntry crossCurrencyTransfer(Account from, Account to, String dateTime, long amount,
                    long targetAmount, String rate)
    {
        var entry = new AccountTransferEntry(from, to);
        entry.setDate(at(dateTime));
        entry.getSourceTransaction().setMonetaryAmount(Money.of(from.getCurrencyCode(), amount));
        entry.getTargetTransaction().setMonetaryAmount(Money.of(to.getCurrencyCode(), targetAmount));
        entry.getSourceTransaction().addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE,
                        Money.of(from.getCurrencyCode(), amount), Money.of(to.getCurrencyCode(), targetAmount),
                        new BigDecimal(rate)));
        entry.insert();
        return entry;
    }

    public static PortfolioTransferEntry securityTransfer(Portfolio from, Portfolio to, Security security,
                    String dateTime, long shares, long amount)
    {
        var entry = new PortfolioTransferEntry(from, to);
        entry.setDate(at(dateTime));
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setCurrencyCode(security.getCurrencyCode());
        entry.setAmount(amount);
        entry.insert();
        return entry;
    }
}
