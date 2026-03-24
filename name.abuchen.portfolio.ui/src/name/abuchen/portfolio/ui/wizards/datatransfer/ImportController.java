package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private static final long EX_DATE_PAYMENT_DATE_TOLERANCE_DAYS = 5;

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

            new UpdatePricesJob(client, newSecurities).schedule();

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

        var exDate = findMatchingExDate(transaction.getDateTime().toLocalDate(), events);

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

    private Optional<LocalDate> findMatchingExDate(LocalDate bookingDate, List<DividendEvent> dividendEvents)
    {
        if (bookingDate == null || dividendEvents == null || dividendEvents.isEmpty())
            return Optional.empty();

        return dividendEvents.stream().filter(event -> event.getDate() != null && event.getPaymentDate() != null)
                        .filter(event -> Math.abs(ChronoUnit.DAYS.between(bookingDate,
                                        event.getPaymentDate())) <= EX_DATE_PAYMENT_DATE_TOLERANCE_DAYS)
                        .min(Comparator.comparingLong((DividendEvent event) -> Math
                                        .abs(ChronoUnit.DAYS.between(bookingDate, event.getPaymentDate())))
                                        .thenComparing(DividendEvent::getPaymentDate))
                        .map(DividendEvent::getDate);
    }
}
