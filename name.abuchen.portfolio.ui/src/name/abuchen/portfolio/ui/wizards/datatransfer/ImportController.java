package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;

public class ImportController
{
    private final Client client;

    public ImportController(Client client)
    {
        this.client = client;
    }

    /**
     * Imports the given review pages and returns the securities that were newly
     * created during the import. Configuring price feeds for these securities is
     * left to the caller via {@link #configureNewSecurities(Client, List)} so
     * that securities from other import paths (e.g. manually created
     * transactions) can be configured in the same step.
     */
    public List<Security> perform(List<ReviewExtractedItemsPage> pages)
    {
        if (pages.isEmpty())
            return List.of();

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

        return newSecurities;
    }

    /**
     * Returns those of the given candidate securities that are now part of the
     * client. During an import a candidate ends up in the client either because
     * it was imported directly or because a transaction referencing it was
     * inserted (see {@code InsertAction}); in both cases it is a newly created
     * security that needs price-feed configuration.
     */
    public static List<Security> newlyImportedSecurities(Collection<Security> candidates, Client client)
    {
        return candidates.stream().filter(client.getSecurities()::contains).toList();
    }

    /**
     * Schedules a price update and opens the {@link FindQuoteProviderDialog} to
     * let the user configure historical prices for securities that were newly
     * created during an import. No-op if the list is empty.
     */
    public static void configureNewSecurities(Client client, List<Security> newSecurities)
    {
        if (newSecurities.isEmpty())
            return;

        // updated prices for newly created securities (for example crypto
        // currencies already have a working configuration)

        new UpdatePricesJob(client, newSecurities).schedule();

        // run async to allow the other dialog to close

        Display.getDefault().asyncExec(() -> {
            FindQuoteProviderDialog dialog = new FindQuoteProviderDialog(ActiveShell.get(), client, newSecurities);
            dialog.open();
        });
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

                enrichMissingExDate(entry.getItem());

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

    @VisibleForTesting
    /* package */ boolean enrichMissingExDate(Item item)
    {
        if (!(item.getSubject() instanceof AccountTransaction transaction))
            return false;

        if (transaction.getType() != AccountTransaction.Type.DIVIDENDS || transaction.getExDate() != null)
            return false;

        var security = transaction.getSecurity();

        var events = security.getEvents().stream()
                        .filter(event -> event.getType() == SecurityEvent.Type.DIVIDEND_PAYMENT)
                        .map(DividendEvent.class::cast) //
                        .toList();

        if (events.isEmpty())
        {
            try
            {
                var feed = Factory.getDividendFeed(DivvyDiaryDividendFeed.class);
                events = feed.getDividendPayments(security);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(security.getName(), e);
                events = List.of();
            }
        }

        var exDate = DividendEvent.findExDateByPaymentDate(transaction.getDateTime().toLocalDate(), events);

        if (exDate.isPresent())
        {
            transaction.setExDate(exDate.get().atStartOfDay());
            return true;
        }
        else
        {
            return false;
        }
    }
}
