package name.abuchen.portfolio.ui.wizards.security;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.online.impl.PortfolioReportNet.OnlineItem;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;

public class FindQuoteProviderDialog extends TitleAreaDialog
{
    private static final class SecurityItem implements Adaptable
    {
        final Security security;
        final List<Action> actions = new ArrayList<>();

        Action selectedAction;

        public SecurityItem(Security security)
        {
            this.security = security;
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            if (type == Named.class)
                return type.cast(security);
            else
                return null;
        }
    }

    private static final class CheckPortfolioReportThread implements IRunnableWithProgress
    {
        private Consumer<SecurityItem> listener;
        private List<SecurityItem> items;

        public CheckPortfolioReportThread(Consumer<SecurityItem> listener, List<SecurityItem> securities)
        {
            this.listener = listener;
            this.items = securities;
        }

        @Override
        public void run(IProgressMonitor monitor)
        {
            monitor.beginTask(Messages.LabelSearchForQuoteFeeds, items.size());

            for (SecurityItem item : items) // NOSONAR
            {
                try
                {
                    monitor.subTask(item.security.getName());

                    // if the security already has an online id, skip it
                    if (item.security.getOnlineId() != null)
                    {
                        monitor.worked(1);
                        continue;
                    }

                    // skip exchange rates and indices and well-known provider
                    var wellKnown = Set.of(EurostatHICPQuoteFeed.ID, CoinGeckoQuoteFeed.ID);
                    if (item.security.isExchangeRate() //
                                    || item.security.getCurrencyCode() == null //
                                    || (item.security.getFeed() != null && wellKnown.contains(item.security.getFeed())))
                    {
                        monitor.worked(1);
                        continue;
                    }

                    // search for ISIN
                    if (item.security.getIsin() != null && !item.security.getIsin().isEmpty()
                                    && searchByIdentifier(item, item.security.getIsin(), ResultItem::getIsin))
                    {
                        monitor.worked(1);
                        continue;
                    }

                    // search by WKN
                    if (item.security.getWkn() != null && !item.security.getWkn().isEmpty()
                                    && searchByIdentifier(item, item.security.getWkn(), ResultItem::getWkn))
                    {
                        monitor.worked(1);
                        continue;
                    }
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(item.security.getName(), e);
                }

                monitor.worked(1);

                if (monitor.isCanceled())
                {
                    monitor.done();
                    return;
                }

            }

            monitor.done();
        }

        private boolean searchByIdentifier(SecurityItem item, String identifier, Function<ResultItem, String> property)
                        throws IOException
        {
            var results = new PortfolioReportNet().search(identifier, SecuritySearchProvider.Type.ALL);

            // searching by WKN must be a direct match
            if (results.size() != 1)
                return false;

            if (!identifier.equals(property.apply(results.get(0))))
                return false;

            var onlineItem = (OnlineItem) results.get(0);

            addSecurityInfoAction(item, onlineItem);

            var useAction = addAction(item, onlineItem);

            if (onlineItem.hasPrices())
                item.selectedAction = useAction;

            Action linkOnlyAction = new SimpleAction(Messages.LabelOnlyLinkToPortfolioReport, a -> {
                item.security.setOnlineId(onlineItem.getOnlineId());
                PortfolioReportNet.updateWith(item.security, onlineItem);
            });

            if (item.selectedAction == null)
                item.selectedAction = linkOnlyAction;
            item.actions.add(linkOnlyAction);

            listener.accept(item);
            return true;
        }

        private void addSecurityInfoAction(SecurityItem item, OnlineItem onlineItem)
        {
            var label = MessageFormat.format("{0} * {1} * {2} * {3}", //$NON-NLS-1$
                            onlineItem.getName(), //
                            onlineItem.getIsin() != null ? onlineItem.getIsin() : "", //$NON-NLS-1$
                            onlineItem.getWkn() != null ? onlineItem.getWkn() : "", //$NON-NLS-1$
                            onlineItem.getSymbol() != null ? onlineItem.getSymbol() : ""); //$NON-NLS-1$

            item.actions.add(new LabelOnly(label));
        }

        private Action addAction(SecurityItem item, OnlineItem onlineItem)
        {
            var label = MessageFormat.format("{0}, {1}, {2}", //$NON-NLS-1$
                            new PortfolioReportQuoteFeed().getName(), //
                            onlineItem.getName(), //
                            item.security.getCurrencyCode());

            Action action = new SimpleAction(label, onlineItem.hasPrices() ? Images.VIEW_LINECHART : null, a -> {
                item.security.setOnlineId(onlineItem.getOnlineId());
                item.security.setFeed(PortfolioReportQuoteFeed.ID);

                // if and only if the label includes the characters of a newly
                // imported security (e.g. "Imported security: {0}"), then we
                // also update the name of the instrument. Because we do not
                // know which identifier was used to construct the name, we
                // check with indexOf.

                var labelOfNewlyImportedSecurity = MessageFormat
                                .format(name.abuchen.portfolio.Messages.CSVImportedSecurityLabel, ""); //$NON-NLS-1$
                if (item.security.getName().indexOf(labelOfNewlyImportedSecurity) >= 0)
                {
                    item.security.setName(onlineItem.getName());
                }

                PortfolioReportNet.updateWith(item.security, onlineItem);
            });

            item.actions.add(action);

            return action;
        }

    }

    private final Client client;
    private final List<SecurityItem> securities;

    public FindQuoteProviderDialog(Shell parentShell, Client client, List<Security> securities)
    {
        super(parentShell);

        this.client = client;
        this.securities = securities.stream().map(SecurityItem::new).toList();
    }

    @Override
    public void create()
    {
        super.create();

        setTitleImage(Images.BANNER.image());
        setTitle(Messages.LabelSearchForQuoteFeeds);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();

        List<Security> updatedSecurities = new ArrayList<>();

        for (SecurityItem item : securities)
        {
            if (item.selectedAction != null)
            {
                item.selectedAction.run();
                updatedSecurities.add(item.security);
            }
        }

        if (!updatedSecurities.isEmpty())
        {
            client.markDirty();
            new UpdateQuotesJob(client, updatedSecurities).schedule();
        }
    }

    @Override
    protected int getShellStyle()
    {
        return super.getShellStyle() | SWT.RESIZE;
    }

    @Override
    protected Point getInitialSize()
    {
        Point preferredSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        // create dialog with a minimum size
        preferredSize.x = Math.min(Math.max(preferredSize.x, 700), 1000);
        preferredSize.y = Math.min(Math.max(preferredSize.y, 500), 700);
        return preferredSize;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite tableArea = new Composite(area, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);
        tableArea.setLayout(new FillLayout());

        Composite compositeTable = new Composite(tableArea, SWT.NONE);

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        TableViewer tableViewer = new TableViewer(compositeTable,
                        SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
        tableViewer.setUseHashlookup(true);

        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);

        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumns(tableViewer, layout);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setInput(securities);

        hookContextMenu(tableViewer, table);

        ProgressMonitorPart progressMonitor = new ProgressMonitorPart(parent, new GridLayout());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(progressMonitor);

        setupDirtyListener(tableViewer);
        triggerJob(tableViewer, progressMonitor);

        return area;
    }

    private void setupDirtyListener(TableViewer tableViewer)
    {
        PropertyChangeListener listener = e -> Display.getDefault().asyncExec(() -> tableViewer.refresh(true));
        client.addPropertyChangeListener("dirty", listener); //$NON-NLS-1$
        tableViewer.getTable().addDisposeListener(e -> client.removePropertyChangeListener("dirty", listener)); //$NON-NLS-1$
    }

    private void triggerJob(TableViewer tableViewer, IProgressMonitor progressMonitor)
    {
        var job = new CheckPortfolioReportThread(item -> Display.getDefault().asyncExec(() -> {
            if (!tableViewer.getTable().isDisposed())
                tableViewer.refresh();
        }), securities);

        Display.getCurrent().asyncExec(() -> {
            try
            {
                ModalContext.run(job, true, progressMonitor, Display.getDefault());
            }
            catch (InvocationTargetException | InterruptedException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            }
        });

        tableViewer.getTable().addDisposeListener(e -> progressMonitor.setCanceled(true));
    }

    private void addColumns(TableViewer tableViewer, TableColumnLayout layout)
    {
        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnName);
        column.setLabelProvider(new NameColumn.NameColumnLabelProvider(client));
        layout.setColumnData(column.getColumn(), new ColumnPixelData(250, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnCurrency);
        column.setLabelProvider(
                        ColumnLabelProvider.createTextProvider(e -> ((SecurityItem) e).security.getCurrencyCode()));
        layout.setColumnData(column.getColumn(), new ColumnPixelData(60, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.LabelCurrentConfiguration);
        column.setLabelProvider(ColumnLabelProvider.createTextProvider(e -> {
            var security = ((SecurityItem) e).security;
            var feedLabel = Factory.getQuoteFeedProvider(security.getFeed());
            return MessageFormat.format(Messages.LabelQuoteFeedConfiguration,
                            feedLabel != null ? feedLabel.getName() : Messages.LabelNotAvailable,
                            security.getPrices().size());
        }));
        layout.setColumnData(column.getColumn(), new ColumnPixelData(200, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.LabelUpdatedConfiguration);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                var selectedAction = ((SecurityItem) e).selectedAction;

                return selectedAction != null ? selectedAction.getText()
                                : MessageFormat.format("{0,choice,0#|1#1 candidate|1<{0} candidates}", //$NON-NLS-1$
                                                ((SecurityItem) e).actions.stream().filter(a -> !a.isEnabled())
                                                                .count());
            }

            @Override
            public Color getBackground(Object e)
            {
                return ((SecurityItem) e).selectedAction != null ? Colors.theme().warningBackground() : null;
            }
        });

        layout.setColumnData(column.getColumn(), new ColumnPixelData(300, true));
    }

    private void hookContextMenu(TableViewer tableViewer, Table table)
    {
        new ContextMenu(table, menuManager -> {
            var selection = tableViewer.getStructuredSelection();
            var element = selection.getFirstElement();

            if (selection.size() == 1 && element instanceof SecurityItem item)
            {
                SimpleAction noop = new SimpleAction(Messages.MenuDoNotChange, a -> {
                    item.selectedAction = null;
                    tableViewer.refresh();
                });

                noop.setChecked(item.selectedAction == null);
                menuManager.add(noop);

                for (Action action : item.actions)
                {
                    SimpleAction menuItem = new SimpleAction(action.getText(), a -> {
                        item.selectedAction = action;
                        tableViewer.refresh(item);
                    });
                    menuItem.setChecked(action == item.selectedAction);
                    menuItem.setEnabled(action.isEnabled());
                    menuManager.add(menuItem);
                }
            }
            else if (selection.size() > 1)
            {
                SimpleAction noop = new SimpleAction(Messages.MenuDoNotChange, a -> {
                    for (Object e : selection)
                        ((SecurityItem) e).selectedAction = null;
                    tableViewer.refresh();
                });
                menuManager.add(noop);
            }

        }).hook();
    }
}
