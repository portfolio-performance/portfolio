package name.abuchen.portfolio.snapshot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class GroupEarningsByAccount
{
    public static class Item
    {
        private final Account account;
        private final Money dividends;
        private final Money fees;
        private final Money interest;
        private final Money sum;
        private final Money taxes;

        public Item(Account account, Money dividends, Money fees, Money interest, Money sum, Money taxes)
        {
            this.account = account;
            this.dividends = dividends;
            this.fees = fees;
            this.interest = interest;
            this.sum = sum;
            this.taxes = taxes;
        }

        public Account getAccount()
        {
            return account;
        }

        public Money getDividends()
        {
            return dividends;
        }

        public Money getFees()
        {
            return fees;
        }

        public Money getInterest()
        {
            return interest;
        }

        public Money getSum()
        {
            return sum;
        }

        public Money getTaxes()
        {
            return taxes;
        }
    }

    private List<Item> items = new ArrayList<>();

    public GroupEarningsByAccount(ClientPerformanceSnapshot snapshot)
    {
        Client client = snapshot.getClient();
        LocalDateTime startDate = snapshot.getStartClientSnapshot().getTime().atTime(LocalTime.MAX);
        LocalDateTime endDate = snapshot.getEndClientSnapshot().getTime().atTime(LocalTime.MAX);

        for (Account account : client.getAccounts())
        {
            MutableMoney dividends = MutableMoney.of(account.getCurrencyCode());
            MutableMoney fees = MutableMoney.of(account.getCurrencyCode());
            MutableMoney interest = MutableMoney.of(account.getCurrencyCode());
            MutableMoney sum = MutableMoney.of(account.getCurrencyCode());
            MutableMoney taxes = MutableMoney.of(account.getCurrencyCode());

            for (AccountTransaction at : account.getTransactions())
            {
                if (at.getDateTime().isAfter(startDate) && !at.getDateTime().isAfter(endDate))
                {
                    switch (at.getType())
                    {
                        case DIVIDENDS:
                            dividends.add(at.getGrossValue());
                            sum.add(at.getGrossValue());
                            taxes.add(at.getUnitSum(Unit.Type.TAX));
                            fees.add(at.getUnitSum(Unit.Type.FEE));
                            break;
                        case INTEREST:
                            interest.add(at.getGrossValue());
                            sum.add(at.getGrossValue());
                            taxes.add(at.getUnitSum(Unit.Type.TAX));
                            fees.add(at.getUnitSum(Unit.Type.FEE));
                            break;
                        case INTEREST_CHARGE:
                            interest.subtract(at.getGrossValue());
                            sum.subtract(at.getGrossValue());
                            taxes.add(at.getUnitSum(Unit.Type.TAX));
                            fees.add(at.getUnitSum(Unit.Type.FEE));
                            break;
                        case FEES:
                            fees.add(at.getMonetaryAmount());
                            break;
                        case FEES_REFUND:
                            fees.subtract(at.getMonetaryAmount());
                            break;
                        case TAXES:
                            taxes.add(at.getMonetaryAmount());
                            break;
                        case TAX_REFUND:
                            taxes.subtract(at.getMonetaryAmount());
                            break;
                        case BUY:
                        case SELL:
                            CrossEntry crossEntry = at.getCrossEntry();
                            if (crossEntry instanceof BuySellEntry)
                            {
                                PortfolioTransaction pt = ((BuySellEntry) crossEntry).getPortfolioTransaction();
                                taxes.add(pt.getUnitSum(Unit.Type.TAX));
                                fees.add(pt.getUnitSum(Unit.Type.FEE));
                            }
                            else
                            {
                                throw new UnsupportedOperationException();
                            }
                            break;
                        case DEPOSIT:
                        case REMOVAL:
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            // no operation
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }

            if (!dividends.isZero() || !fees.isZero() || !interest.isZero() || !sum.isZero() || !taxes.isZero())
            {
                Item item = new Item(account, dividends.toMoney(), fees.toMoney(), interest.toMoney(), sum.toMoney(),
                                taxes.toMoney());
                items.add(item);
            }
        }
    }

    public List<Item> getItems()
    {
        return items;
    }
}
