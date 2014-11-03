package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.util.Dates;

import org.apache.pdfbox.util.PDFTextStripper;

public class INGDibaPDFExtractor implements Extractor
{
    private Client client;

    public INGDibaPDFExtractor(Client client)
    {
        this.client = client;
    }

    @Override
    public String getLabel()
    {
        return Messages.PDFLabel_ingdiba;
    }

    @Override
    public String getFilterExtension()
    {
        return "*.pdf"; //$NON-NLS-1$
    }

    @Override
    public List<Item> extract(List<File> files)
    {
        PDFTextStripper stripper;

        List<Security> allSecurities = new ArrayList<Security>(client.getSecurities());

        List<Item> answer = new ArrayList<Item>();

        // new interest payment
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.INTEREST);
        t.setDate(Dates.today());
        t.setAmount(100); // 1 euro = 100 cents
        t.setNote(files.get(0).getName()); // just to test

        // do not add to client directly -> allow user to accept/approve
        answer.add(new TransactionItem(t));

        // create a new security if not found in client.getSecurities()
        Security security = new Security("BASF", null, "BAS.DE", YahooFinanceQuoteFeed.ID); //$NON-NLS-1$ //$NON-NLS-2$
        answer.add(new SecurityItem(security));

        // do not add security to client directly, but keep it for next
        // transactions
        allSecurities.add(security);

        // new dividend payment
        t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setDate(Dates.today());
        t.setAmount(100);
        t.setSecurity(security);
        answer.add(new TransactionItem(t));

        // new security purchase
        BuySellEntry purchase = new BuySellEntry();
        purchase.setType(PortfolioTransaction.Type.BUY);
        purchase.setDate(Dates.today());
        purchase.setSecurity(security);
        purchase.setShares(1 * Values.Share.factor()); // 1 share
        purchase.setAmount(100 * Values.Amount.factor()); // 100 euros
        purchase.setFees(100);
        answer.add(new BuySellEntryItem(purchase));

        return answer;
    }

}
