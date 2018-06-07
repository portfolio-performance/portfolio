package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.util.Iban;
import name.abuchen.portfolio.Messages;

public class Peer implements Named
{
    private String name;
    private String note;
    private String IBAN;
    private Account account;

    public Peer()
    {
        voidAccount();
    }

    public Peer(Account account)
    {
        setAccount(account);
    }

    public void voidAccount()
    {
        account = null;
        setName(Messages.LabelAnyPeer);
        setIban(Iban.IBANNUMBER_DUMMY);
    }

    public void setAccount(Account account)
    {
        this.account = account;
        this.IBAN = null;
        this.name = null;
    }

    public String setIban(String iban)
    {
        if (isAccount())
            account.setIban(iban);
        else
        {
            account = null;
            if (Iban.isValid(iban))
                IBAN = iban;
        }
        return this.getIban();
    }

    public String setIban(String blz, String konto)
    {
        return this.setIban(Iban.DEconvert(blz,konto));
    }

    public Account getAccount()
    {
        return account;
    }

    public boolean isAccount()
    {
        if (getAccount() == null)
            return false;
        else
            return true;
    }

    
    @Override
    public void setName(String name)
    {
        if (isAccount())
            getAccount().setName(name);
        else
            this.name = name;
    }

    @Override
    public void setNote(String note)
    {
        if (isAccount())
            getAccount().setNote(note);
        else
            this.note = note;
    }

    public String getIban()
    {
        if (isAccount())
            return getAccount().getIban();
        else
            return IBAN;
    }

    @Override
    public String getName()
    {
        if (isAccount())
            return "[" + this.getAccount().getName() + "]";
        else if (IBAN != null && IBAN.equals(Iban.IBANNUMBER_ANY))
            return Messages.LabelAnyPeer;
        else
            return name;
    }

    @Override
    public String getNote()
    {
        if (isAccount())
            return "<" + this.getAccount().getNote() + ">";
        else
        return this.note;
    }

    public Peer getPeer(String Iban)
    {
        if (isAccount())
        {
            if (this.getAccount().getIban().equals(Iban))
                return this;
        }       
        else if (IBAN.equals(Iban))
            return this;
        return null;
    }

    public Peer getPeer()
    {
        return this;
    }

//  TODO: #41 Is this an orphan method or needed?    
//    public List<DedicatedTransaction> getDedicatedTransactions()
//    {
//        if (this.account != null)
//            return this.account.getDedicatedTransactions();
//        return null;
//    }

    public List<TransactionPair<?>> getTransactions(Client client)
    {
        List<TransactionPair<?>> answer = new ArrayList<>();

        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream() //
                            .filter(t -> this.equals(t.getPeer()))
                            .filter(t -> t.getType() == AccountTransaction.Type.INTEREST
                                            || t.getType() == AccountTransaction.Type.DIVIDENDS
                                            || t.getType() == AccountTransaction.Type.DIVIDEND_CHARGE
                                            || t.getType() == AccountTransaction.Type.TAXES
                                            || t.getType() == AccountTransaction.Type.TAX_REFUND
                                            || t.getType() == AccountTransaction.Type.FEES
                                            || t.getType() == AccountTransaction.Type.FEES_REFUND)
                            .map(t -> new TransactionPair<AccountTransaction>(account, t)) //
                            .forEach(answer::add);
        }

        return answer;
    }

    @Override
    public String toString()
    {
        return (getName() != null?getName():"NULL") + " (" + (getIban() != null?getIban():"=/=") + ")" + (isAccount() ? " <Account: " + getAccount().getUUID() + ">" : "");
    }
}
