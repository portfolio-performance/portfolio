package name.abuchen.portfolio.checks.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;

/**
 * Search for transactions of type TRANSFER_IN where the crossEntry is an
 * instance of BuySellEntry instead of PortfolioTransferEntry. This indicates
 * that this malformed transaction was created through PDF import and needs to
 * be corrected. Issue #1931 relates to this fix.
 */
public class FixWrongTransfersCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {

        List<PortfolioTransaction> transactions = client.getPortfolios().stream()
                        .flatMap(p -> p.getTransactions().stream())
                        .filter(t -> PortfolioTransaction.Type.TRANSFER_IN.equals(t.getType()))
                        .collect(Collectors.toList());

        for (PortfolioTransaction t : transactions)
        {
            if (t.getCrossEntry() instanceof BuySellEntry)
            {
                // create new transaction because crossEntry cannot be removed
                PortfolioTransaction copy = new PortfolioTransaction(t.getDateTime(), t.getCurrencyCode(),
                                t.getAmount(), t.getSecurity(), t.getShares(),
                                PortfolioTransaction.Type.DELIVERY_INBOUND, t.getUnitSum(Unit.Type.FEE).getAmount(),
                                t.getUnitSum(Unit.Type.TAX).getAmount());
                // copy note text
                copy.setNote(t.getNote());

                // replace transaction in portfolio
                ((Portfolio) t.getCrossEntry().getOwner(t)).addTransaction(copy);
                ((Portfolio) t.getCrossEntry().getOwner(t)).deleteTransaction(t, client);
            }
        }

        return Collections.emptyList();
    }
}
