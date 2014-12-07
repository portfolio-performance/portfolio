package name.abuchen.portfolio.snapshot;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;

public class AccountSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static AccountSnapshot create(Account account, Date time)
    {
        long funds = 0;

        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getDate().getTime() <= time.getTime())
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

        return new AccountSnapshot(account, time, funds);
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private Account account;
    private Date time;
    private long funds;

    private AccountSnapshot(Account account, Date time, long funds)
    {
        this.account = account;
        this.time = time;
        this.funds = funds;
    }

    public Account getAccount()
    {
        return account;
    }

    public Date getTime()
    {
        return time;
    }

    public long getFunds()
    {
        return funds;
    }
}
