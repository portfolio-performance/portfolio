package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

public class GroupEarningsByAccount
{
    public static class Item
    {
        private Account account;
        private long sum;

        public Item(Account account)
        {
            this.account = account;
        }

        public Account getAccount()
        {
            return account;
        }

        public long getSum()
        {
            return sum;
        }
    }

    private List<Item> items = new ArrayList<Item>();

    public GroupEarningsByAccount(ClientPerformanceSnapshot snapshot)
    {
        Client client = snapshot.getEndClientSnapshot().getClient();
        long startDate = snapshot.getStartClientSnapshot().getTime().getTime();
        long endDate = snapshot.getEndClientSnapshot().getTime().getTime();

        for (Account account : client.getAccounts())
        {
            Item item = new Item(account);

            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getDate().getTime() > startDate && t.getDate().getTime() <= endDate)
                {
                    switch (t.getType())
                    {
                        case DIVIDENDS:
                        case INTEREST:
                            item.sum += t.getAmount();
                            break;
                        case DEPOSIT:
                        case REMOVAL:
                        case FEES:
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

            if (item.getSum() != 0)
                items.add(item);
        }
    }

    public List<Item> getItems()
    {
        return items;
    }
}
