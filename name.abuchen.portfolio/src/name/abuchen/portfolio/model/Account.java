package name.abuchen.portfolio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import name.abuchen.portfolio.money.CurrencyUnit;

public class Account implements TransactionOwner<AccountTransaction>, InvestmentVehicle, Attributable
{
    private String uuid;
    private String name;
    private String currencyCode = CurrencyUnit.EUR;
    private String note;
    private boolean isRetired = false;

    private List<AccountTransaction> transactions = new ArrayList<>();

    private Attributes attributes;

    public Account()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Account(String name)
    {
        this();
        this.name = name;
    }

    @Override
    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    @Override
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
    }

    @Override
    public boolean isRetired()
    {
        return isRetired;
    }

    @Override
    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
    }

    @Override
    public Attributes getAttributes()
    {
        if (attributes == null)
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    @Override
    public List<AccountTransaction> getTransactions()
    {
        return transactions;
    }

    @Override
    public void addTransaction(AccountTransaction transaction)
    {
        if (!currencyCode.equals(transaction.getCurrencyCode()))
            throw new IllegalArgumentException();

        this.transactions.add(transaction);
    }

    @Override
    public void shallowDeleteTransaction(AccountTransaction transaction, Client client)
    {
        this.transactions.remove(transaction);
        client.getPlans().stream().forEach(plan -> plan.removeTransaction(transaction));
    }

    public long getCurrentAmount(LocalDateTime date)
    {
        return transactions.stream() //
                        .filter(t -> t.getDateTime().isBefore(date)) //
                        .mapToLong(t -> {
                            switch (t.getType())
                            {
                                case DEPOSIT:
                                case DIVIDENDS:
                                case INTEREST:
                                case SELL:
                                case TRANSFER_IN:
                                case TAX_REFUND:
                                case FEES_REFUND:
                                    return t.getAmount();
                                case FEES:
                                case INTEREST_CHARGE:
                                case TAXES:
                                case REMOVAL:
                                case BUY:
                                case TRANSFER_OUT:
                                    return -t.getAmount();
                                default:
                                    throw new UnsupportedOperationException();
                            }
                        }).sum();
    }

    @Override
    public String toString()
    {
        return getName();
    }

}
