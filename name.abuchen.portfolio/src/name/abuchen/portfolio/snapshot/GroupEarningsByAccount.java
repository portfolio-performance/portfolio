package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class GroupEarningsByAccount
{
    public static class Item
    {
        private final Account account;
        private final Money sum;

        public Item(Account account, Money sum)
        {
            this.account = account;
            this.sum = sum;
        }

        public Account getAccount()
        {
            return account;
        }

        public Money getSum()
        {
            return sum;
        }
    }

    private List<Item> items = new ArrayList<>();

    public GroupEarningsByAccount(ClientPerformanceSnapshot snapshot)
    {
        Client client = snapshot.getClient();
        LocalDate startDate = snapshot.getStartClientSnapshot().getTime();
        LocalDate endDate = snapshot.getEndClientSnapshot().getTime();

        for (Account account : client.getAccounts())
        {
            MutableMoney sum = MutableMoney.of(account.getCurrencyCode());

            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getDate().isAfter(startDate) && !t.getDate().isAfter(endDate))
                {
                    switch (t.getType())
                    {
                        case DIVIDENDS:
                        case INTEREST:
                            sum.add(t.getGrossValue());
                            break;
                        case INTEREST_CHARGE:
                            sum.subtract(t.getMonetaryAmount());
                            break;
                        case DEPOSIT:
                        case REMOVAL:
                        case FEES:
                        case FEES_REFUND:
                        case TAXES:
                        case TAX_REFUND:
                        case BUY:
                        case SELL:
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            // no operation
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }

            if (!sum.isZero())
            {
                Item item = new Item(account, sum.toMoney());
                items.add(item);
            }
        }
    }

    public List<Item> getItems()
    {
        return items;
    }
}
