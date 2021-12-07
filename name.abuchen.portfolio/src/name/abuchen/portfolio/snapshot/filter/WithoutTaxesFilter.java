package name.abuchen.portfolio.snapshot.filter;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class WithoutTaxesFilter implements ClientFilter
{

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);
        client.getSecurities().forEach(pseudoClient::internalAddSecurity);

        // create all pseudo accounts
        Map<Account, ReadOnlyAccount> account2pseudo = new HashMap<>();
        client.getAccounts().stream().forEach(a -> {
            ReadOnlyAccount pa = new ReadOnlyAccount(a);
            pseudoClient.internalAddAccount(pa);
            account2pseudo.put(a, pa);
        });

        // create all pseudo portfolios
        Map<Portfolio, ReadOnlyPortfolio> portfolio2pseudo = new HashMap<>();
        client.getPortfolios().stream().forEach(p -> {
            ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(p);
            pseudoPortfolio.setReferenceAccount(account2pseudo.get(p.getReferenceAccount()));
            pseudoClient.internalAddPortfolio(pseudoPortfolio);
            portfolio2pseudo.put(p, pseudoPortfolio);
        });

        for (Portfolio portfolio : client.getPortfolios())
            adaptPortfolioTransactions(portfolio, portfolio2pseudo, account2pseudo);

        for (Account account : client.getAccounts())
            adaptAccountTransactions(account, account2pseudo);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, Map<Portfolio, ReadOnlyPortfolio> portfolio2pseudo,
                    Map<Account, ReadOnlyAccount> account2pseudo)
    {
        ReadOnlyPortfolio pseudoPortfolio = portfolio2pseudo.get(portfolio);
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                case SELL:
                    stripTaxes((BuySellEntry) t.getCrossEntry(), pseudoPortfolio,
                                    account2pseudo.get(t.getCrossEntry().getCrossOwner(t)));
                    break;
                case DELIVERY_INBOUND:
                case DELIVERY_OUTBOUND:
                    stripTaxes(t, pseudoPortfolio);
                    break;
                case TRANSFER_IN:
                    ClientFilterHelper.recreateTransfer((PortfolioTransferEntry) t.getCrossEntry(),
                                    portfolio2pseudo.get(t.getCrossEntry().getCrossOwner(t)), pseudoPortfolio);
                    break;
                case TRANSFER_OUT:
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void stripTaxes(BuySellEntry buySell, ReadOnlyPortfolio readOnlyPortfolio, ReadOnlyAccount readOnlyAccount)
    {
        PortfolioTransaction t = buySell.getPortfolioTransaction();

        boolean isBuy = t.getType() == PortfolioTransaction.Type.BUY;

        Money taxes = t.getUnitSum(Unit.Type.TAX);

        BuySellEntry copy = new BuySellEntry(readOnlyPortfolio, readOnlyAccount);

        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());
        copy.setShares(t.getShares());

        if (isBuy)
            copy.setAmount(t.getAmount() - taxes.getAmount());
        else
            copy.setAmount(t.getAmount() + taxes.getAmount());

        // without taxes
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(u -> copy.getPortfolioTransaction().addUnit(u));

        readOnlyPortfolio.internalAddTransaction(copy.getPortfolioTransaction());
        readOnlyAccount.internalAddTransaction(copy.getAccountTransaction());

        // correct the taxes on the account
        AccountTransaction at = new AccountTransaction();
        at.setType(AccountTransaction.Type.REMOVAL);
        at.setDateTime(t.getDateTime());
        at.setMonetaryAmount(taxes);
        readOnlyAccount.internalAddTransaction(at);
    }

    private void stripTaxes(PortfolioTransaction deliveryT, ReadOnlyPortfolio readOnlyPortfolio)
    {
        boolean isInbound = deliveryT.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;

        Money taxes = deliveryT.getUnitSum(Unit.Type.TAX);

        PortfolioTransaction copy = new PortfolioTransaction();
        copy.setType(deliveryT.getType());
        copy.setDateTime(deliveryT.getDateTime());
        copy.setCurrencyCode(deliveryT.getCurrencyCode());
        copy.setSecurity(deliveryT.getSecurity());
        copy.setNote(deliveryT.getNote());
        copy.setShares(deliveryT.getShares());

        if (isInbound)
            copy.setAmount(deliveryT.getAmount() - taxes.getAmount());
        else
            copy.setAmount(deliveryT.getAmount() + taxes.getAmount());

        readOnlyPortfolio.internalAddTransaction(copy);
    }

    private void adaptAccountTransactions(Account account, Map<Account, ReadOnlyAccount> account2pseudo)
    {
        ReadOnlyAccount pseudoAccount = account2pseudo.get(account);

        for (AccountTransaction t : account.getTransactions())
        {
            switch (t.getType())
            {
                case TAX_REFUND:
                    pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TAXES:
                    pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.REMOVAL));
                    break;
                case DIVIDENDS:
                    stripTaxes(t, pseudoAccount);
                    break;
                case BUY:
                case SELL:
                    // skip -> handled by portfolio transaction
                    break;
                case TRANSFER_IN:
                    ClientFilterHelper.recreateTransfer((AccountTransferEntry) t.getCrossEntry(),
                                    account2pseudo.get(t.getCrossEntry().getCrossOwner(t)), pseudoAccount);
                    break;
                case TRANSFER_OUT:
                    // skip -> handled by inbound transfer
                    break;
                case FEES_REFUND:
                case FEES:
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    pseudoAccount.internalAddTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void stripTaxes(AccountTransaction t, ReadOnlyAccount readOnlyAccount)
    {
        if (t.getType() != AccountTransaction.Type.DIVIDENDS)
            throw new UnsupportedOperationException();

        Money taxes = t.getUnitSum(Unit.Type.TAX);

        if (taxes.isZero())
        {
            readOnlyAccount.internalAddTransaction(t);
            return;
        }

        AccountTransaction copy = new AccountTransaction();
        copy.setType(t.getType());
        copy.setDateTime(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setAmount(t.getAmount() + taxes.getAmount());
        copy.setNote(t.getNote());
        copy.setShares(t.getShares());
        copy.setSecurity(t.getSecurity());

        // move fees over to dividend tx
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(copy::addUnit);

        readOnlyAccount.internalAddTransaction(copy);

        AccountTransaction removal = new AccountTransaction();
        removal.setType(AccountTransaction.Type.REMOVAL);
        removal.setDateTime(t.getDateTime());
        removal.setMonetaryAmount(taxes);
        readOnlyAccount.internalAddTransaction(removal);
    }

    private AccountTransaction convertTo(AccountTransaction t, AccountTransaction.Type type)
    {
        if (type != AccountTransaction.Type.DEPOSIT && type != AccountTransaction.Type.REMOVAL)
            throw new UnsupportedOperationException();

        AccountTransaction clone = new AccountTransaction();
        clone.setType(type);
        clone.setDateTime(t.getDateTime());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(null);
        clone.setAmount(t.getAmount());
        clone.setShares(t.getShares());

        // do *not* copy units as REMOVAL and DEPOSIT have never units
        return clone;
    }
}
