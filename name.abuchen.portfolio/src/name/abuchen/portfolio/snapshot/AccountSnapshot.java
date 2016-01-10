package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

public class AccountSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static AccountSnapshot create(Account account, CurrencyConverter converter, LocalDate date)
    {
        long funds = 0;

        for (AccountTransaction t : account.getTransactions())
        {
            if (!t.getDate().isAfter(date))
            {
                switch (t.getType())
                {
                    case DEPOSIT:
                    case DIVIDENDS:
                    case INTEREST:
                    case SELL:
                    case TRANSFER_IN:
                    case TAX_REFUND:
                        funds += t.getAmount();
                        break;
                    case FEES:
                    case TAXES:
                    case REMOVAL:
                    case BUY:
                    case TRANSFER_OUT:
                        funds -= t.getAmount();
                        break;
                    default:
                        throw new RuntimeException("Unknown Account Transaction type: " + t.getType()); //$NON-NLS-1$
                }
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
