package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.jobs.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;

public class ImportController
{

    private final Client client;

    public ImportController(Client client)
    {
        this.client = client;
    }

    public void perform(List<ReviewExtractedItemsPage> pages)
    {
        if (pages.isEmpty())
            return;

        var newSecurities = new ArrayList<Security>();

        var isDirty = false;
        for (ReviewExtractedItemsPage page : pages)
        {
            page.afterPage();
            var isPageDirty = importPage(page, newSecurities);
            isDirty |= isPageDirty;
        }

        if (isDirty)
        {
            client.markDirty();

            // run consistency checks in case bogus transactions have been
            // created (say: an outbound delivery of a security where there
            // no held shares)
            new ConsistencyChecksJob(client, false).schedule();
        }

        if (!newSecurities.isEmpty())
        {
            // updated prices for newly created securities (for example
            // crypto currencies already have a working configuration)

            new UpdateQuotesJob(client, newSecurities).schedule();

            // run async to allow the other dialog to close

            Display.getDefault().asyncExec(() -> {
                FindQuoteProviderDialog dialog = new FindQuoteProviderDialog(ActiveShell.get(), client, newSecurities);
                dialog.open();
            });
        }
    }

    private boolean importPage(ReviewExtractedItemsPage page, ArrayList<Security> newSecurities)
    {
        var isDirty = false;

        InsertAction action = new InsertAction(client);
        action.setConvertBuySellToDelivery(page.doConvertToDelivery());
        action.setRemoveDividends(page.doRemoveDividends());

        for (ExtractedEntry entry : page.getEntries())
        {
            if (entry.isImported())
            {
                if (!page.doImportNotesFromSource())
                    entry.getItem().setNote(null);

                action.setInvestmentPlanItem(entry.getItem().isInvestmentPlanItem());

                // apply security override
                if (entry.getSecurityOverride() != null)
                    entry.getItem().setSecurity(entry.getSecurityOverride());
                
                entry.getItem().apply(action, page);
                isDirty = true;

                if (entry.getItem() instanceof Extractor.SecurityItem)
                {
                    newSecurities.add(entry.getItem().getSecurity());
                }
            }
        }
        return isDirty;
    }
}
