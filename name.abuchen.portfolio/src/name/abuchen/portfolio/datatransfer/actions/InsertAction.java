package name.abuchen.portfolio.datatransfer.actions;

import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

public class InsertAction implements ImportAction
{
    private final Client client;
    private boolean convertBuySellToDelivery = false;

    public InsertAction(Client client)
    {
        this.client = client;
    }

    public void setConvertBuySellToDelivery(boolean flag)
    {
        this.convertBuySellToDelivery = flag;
    }

    @Override
    public Status process(Security security)
    {
        // might have been added via a transaction
        if (!client.getSecurities().contains(security))
            client.addSecurity(security);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(Security security, SecurityPrice price)
    {
        security.addPrice(price);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        if (transaction.getSecurity() != null)
            process(transaction.getSecurity());
        account.addTransaction(transaction);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(transaction.getSecurity());
        portfolio.addTransaction(transaction);
        return Status.OK_STATUS;
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(entry.getPortfolioTransaction().getSecurity());

        if (convertBuySellToDelivery)
        {
            PortfolioTransaction t = entry.getPortfolioTransaction();

            PortfolioTransaction delivery = new PortfolioTransaction();
            delivery.setType(t.getType() == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND);

            delivery.setDateTime(t.getDateTime());
            delivery.setSecurity(t.getSecurity());
            delivery.setMonetaryAmount(t.getMonetaryAmount());
            delivery.setNote(t.getNote());
            delivery.setShares(t.getShares());
            delivery.addUnits(t.getUnits());

            return process(delivery, portfolio);
        }
        else
        {
            entry.setPortfolio(portfolio);
            entry.setAccount(account);
            entry.insert();
            return Status.OK_STATUS;
        }
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        entry.setSourceAccount(source);
        entry.setTargetAccount(target);
        entry.insert();
        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        // ensure consistency (in case the user deleted the creation of the
        // security via the dialog)
        process(entry.getSourceTransaction().getSecurity());

        entry.setSourcePortfolio(source);
        entry.setTargetPortfolio(target);
        entry.insert();
        return Status.OK_STATUS;
    }
}
