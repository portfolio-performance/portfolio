package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyAccount;

public class AccountSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static AccountSnapshot create(Account account, CurrencyConverter converter, LocalDate date)
    {
        long funds = 0;
        
        LocalDateTime reference = date.atTime(LocalTime.MAX);

        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getDateTime().isBefore(reference))
            {
                if (t.getType().isDebit())
                    funds -= t.getAmount();
                else
                    funds += t.getAmount();
            }
        }

        return new AccountSnapshot(account, date, converter, Money.of(account.getCurrencyCode(), funds));
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Account account;
    private final LocalDate date;
    private final CurrencyConverter converter;
    private final Money funds;

    private AccountSnapshot(Account account, LocalDate date, CurrencyConverter converter, Money funds)
    {
        this.account = account;
        this.date = date;
        this.converter = converter;
        this.funds = funds;
    }
    
    /* package */ Account unwrapAccount()
    {
        return account instanceof ReadOnlyAccount ? ((ReadOnlyAccount) account).unwrap() : account;
    }

    public Account getAccount()
    {
        return account;
    }

    public LocalDate getTime()
    {
        return date;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public Money getFunds()
    {
        return funds.with(converter.at(date));
    }

    public Money getUnconvertedFunds()
    {
        return funds;
    }
}
