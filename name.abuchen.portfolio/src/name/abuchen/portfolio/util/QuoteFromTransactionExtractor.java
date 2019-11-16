package name.abuchen.portfolio.util;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Quote;

/**
 * A helper for extracting historic quotes from the transactions of a security.
 * Aims to provide at least quotes from buying or selling to allow calculating
 * the correct performance.
 */
public class QuoteFromTransactionExtractor
{
    private final Client client;
    private final CurrencyConverter converter;

    /**
     * Constructs an instance.
     *
     * @param client
     *            {@link Client}
     */
    public QuoteFromTransactionExtractor(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;
    }

    /**
     * Extracts the quotes for the given {@link Security}.
     *
     * @param security
     *            {@link Security}
     * @return true if quotes were found, else false
     */
    public boolean extractQuotes(Security security)
    {
        if (security.getCurrencyCode() == null)
            return false;

        boolean bChanges = false;
        SecurityPrice pLatest = null;
        // walk through all all transactions for security
        for (TransactionPair<?> p : security.getTransactions(client))
        {
            Transaction t = p.getTransaction();
            // check the type of the transaction
            if (t instanceof PortfolioTransaction)
            {
                PortfolioTransaction pt = (PortfolioTransaction) t;
                // get date and quote and build a price from it
                Quote q = pt.getGrossPricePerShare();
                LocalDate d = pt.getDateTime().toLocalDate();
                
                // check if currency conversion is needed
                if (!q.getCurrencyCode().equals(security.getCurrencyCode()))
                    q = converter.with(security.getCurrencyCode()).convert(d, q);
                
                SecurityPrice price = new SecurityPrice(d, q.getAmount());
                bChanges |= security.addPrice(price);
                // remember the latest price
                if ((pLatest == null) || d.isAfter(pLatest.getDate()))
                {
                    pLatest = price;
                }
            }
        }
        // set the latest price (if at least one price was found)
        if (pLatest != null)
        {
            LatestSecurityPrice lsp = new LatestSecurityPrice(pLatest.getDate(), pLatest.getValue());
            bChanges |= security.setLatest(lsp);
        }
        return bChanges;
    }
}
