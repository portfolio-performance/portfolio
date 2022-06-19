package name.abuchen.portfolio.ui.dnd;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;

public class ImportFromURLDropAdapter extends AbstractDropAdapter
{
    public static final Pattern URL_PATTERN = Pattern.compile("^https://www.portfolio-report.net/securities/([^/]+)/?"); //$NON-NLS-1$

    private final PortfolioPart part;

    private ImportFromURLDropAdapter(Transfer transfer, Control control, PortfolioPart part)
    {
        super(transfer, control);
        this.part = part;
    }

    public static void attach(Control control, PortfolioPart part)
    {
        new ImportFromURLDropAdapter(URLTransfer.getInstance(), control, part);
    }

    @Override
    protected boolean isValidEvent(DropTargetEvent e)
    {
        if (!super.isValidEvent(e))
            return false;

        if (Util.isWindows())
        {
            if (e.data == null && !extractEventData(e))
                return false;
            Optional<String> url = getUrlFromEvent(e);

            return url.isPresent() && URL_PATTERN.matcher(url.get()).matches();
        }

        return true;
    }

    private boolean extractEventData(DropTargetEvent e)
    {
        TransferData transferData = e.currentDataType;
        if (transferData != null)
        {
            Object data = URLTransfer.getInstance().nativeToJava(transferData);
            if (data != null)
            {
                e.data = data;
                return true;
            }
        }
        return false;
    }

    @Override
    public void doDrop(DropTargetEvent event)
    {
        Optional<String> url = getUrlFromEvent(event);
        if (!url.isPresent())
        {
            event.detail = DND.DROP_NONE;
            return;
        }

        Matcher matcher = URL_PATTERN.matcher(url.get());
        if (!matcher.matches())
        {
            event.detail = DND.DROP_NONE;
            return;
        }

        String onlineId = matcher.group(1);

        DropTarget source = (DropTarget) event.getSource();
        Display display = source.getDisplay();
        display.asyncExec(() -> createNewSecurity(url.get(), onlineId));
    }

    private Optional<String> getUrlFromEvent(DropTargetEvent event)
    {
        Object eventData = event.data;
        if (!(eventData instanceof String))
            return Optional.empty();
        return Optional.of(((String) eventData).split(System.getProperty("line.separator"))[0]); //$NON-NLS-1$
    }

    private void createNewSecurity(String url, String onlineId)
    {
        try
        {
            Optional<ResultItem> result = new PortfolioReportNet().getUpdatedValues(onlineId);
            if (!result.isPresent())
            {
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                MessageFormat.format(Messages.MsgErrorNoInvestmentVehicleFoundAtURL, url));
                return;
            }

            Security newSecurity = new Security();
            newSecurity.setName(result.get().getName());
            newSecurity.setOnlineId(onlineId);
            PortfolioReportNet.updateWith(newSecurity, result.get());

            QuoteFeed feed = Factory.getQuoteFeedProvider(PortfolioReportQuoteFeed.ID);
            List<Exchange> exchanges = feed.getExchanges(newSecurity, new ArrayList<>());

            if (!exchanges.isEmpty())
            {
                newSecurity.setFeed(feed.getId());
                newSecurity.setPropertyValue(SecurityProperty.Type.FEED, PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME,
                                exchanges.get(0).getId());
            }

            Dialog dialog = part.make(EditSecurityDialog.class, newSecurity);

            if (dialog.open() == Window.OK)
            {
                part.getClient().addSecurity(newSecurity);

                new UpdateQuotesJob(part.getClient(), newSecurity).schedule();
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }
}
