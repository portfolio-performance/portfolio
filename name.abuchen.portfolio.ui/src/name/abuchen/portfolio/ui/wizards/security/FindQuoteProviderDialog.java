package name.abuchen.portfolio.ui.wizards.security;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.jface.viewers.IStructuredSelection;
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

import com.google.common.base.Objects;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.ManualQuoteFeed;
import name.abuchen.portfolio.online.impl.MarketIdentifierCodes;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceSearchProvider;
import name.abuchen.portfolio.online.impl.PortfolioReportCoins;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LoginButton;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

public class FindQuoteProviderDialog extends TitleAreaDialog
{
    private static final class UseBuiltInFeedAction extends SimpleAction
    {
        private final ResultItem resultItem;

        public UseBuiltInFeedAction(SecurityItem item, ResultItem onlineItem)
        {
            super(MessageFormat.format("{0} * {1} * {2}", //$NON-NLS-1$
                            onlineItem.getCurrencyCode(), //
                            onlineItem.getSymbol(), //
                            MarketIdentifierCodes.getLabel(onlineItem.getExchange())), a -> {
                                item.security.setFeed(PortfolioPerformanceFeed.ID);

                                item.security.setIsin(onlineItem.getIsin());
                                item.security.setTickerSymbol(onlineItem.getSymbol());

                                // if and only if the label includes the
                                // characters of a newly imported security (e.g.
                                // "Imported security: {0}"), then we also
                                // update the name of the instrument.
                                // Because we do not know which identifier was
                                // used to construct the name, we check with
                                // indexOf.

                                var labelOfNewlyImportedSecurity = MessageFormat
                                                .format(name.abuchen.portfolio.Messages.CSVImportedSecurityLabel, ""); //$NON-NLS-1$
                                if (item.security.getName().indexOf(labelOfNewlyImportedSecurity) >= 0)
                                {
                                    item.security.setName(onlineItem.getName());
                                }
                            });

            this.resultItem = onlineItem;
        }

        public String getExchange()
        {
            return resultItem.getExchange();
        }
    }

    private static class DeactivateAction extends SimpleAction
    {
        public DeactivateAction(SecurityItem item)
        {
            super(Factory.getQuoteFeed(ManualQuoteFeed.class).getName(), a -> {
                item.security.setFeed(QuoteFeed.MANUAL);
                item.security.setLatestFeed(null);
            });
        }
    }

    private static class LabelOnly extends Action
    {
        public LabelOnly(String text)
        {
            super(text);
            setEnabled(false);
        }
    }

    private static final class SecurityItem implements Adaptable
    {
        final Security security;
        final List<Action> actions = new CopyOnWriteArrayList<>();

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

    private static final class CheckPortfolioPerformanceAPIThread implements IRunnableWithProgress
    {
        private Consumer<SecurityItem> listener;
        private List<SecurityItem> items;

        public CheckPortfolioPerformanceAPIThread(Consumer<SecurityItem> listener, List<SecurityItem> securities)
        {
            this.listener = listener;
            this.items = securities;
        }

        @Override
        public void run(IProgressMonitor monitor)
        {
            monitor.beginTask(Messages.LabelSearchForQuoteFeeds, items.size());

            var coins = new PortfolioReportCoins();

            for (SecurityItem item : items) // NOSONAR
            {
                try
                {
                    monitor.subTask(item.security.getName());

                    // Add deactivate action for all instruments, including
                    // exchange rates. This allows users to disable automatic
                    // updates for any instrument.
                    item.actions.add(new DeactivateAction(item));

                    // skip exchange rates and indices and well-known provider
                    var wellKnown = Set.of(EurostatHICPQuoteFeed.ID, CoinGeckoQuoteFeed.ID);
                    if (item.security.isExchangeRate() //
                                    || item.security.getCurrencyCode() == null //
                                    || (item.security.getFeed() != null && wellKnown.contains(item.security.getFeed())))
                    {
                        monitor.worked(1);
                        continue;
                    }

                    // check for crypto currencies on Portfolio Report
                    if (PortfolioReportQuoteFeed.ID.equals(item.security.getFeed())
                                    && coins.contains(item.security.getOnlineId()))
                    {
                        setupCoinMigration(coins, item);

                        monitor.worked(1);
                        continue;
                    }

                    // search for ISIN
                    if (item.security.getIsin() != null && !item.security.getIsin().isEmpty() && searchByIdentifier(
                                    item, item.security.getIsin(), PortfolioPerformanceSearchProvider.Parameter.ISIN))
                    {
                        monitor.worked(1);
                        continue;
                    }

                    // search for ticker symbol
                    if (item.security.getTickerSymbol() != null && !item.security.getTickerSymbol().isEmpty()
                                    && searchByIdentifier(item, item.security.getTickerSymbol(),
                                                    PortfolioPerformanceSearchProvider.Parameter.SYMBOL))
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

        private void setupCoinMigration(PortfolioReportCoins coins, SecurityItem item)
        {
            var coin = coins.getCoinByOnlineId(item.security.getOnlineId());

            var heading = MessageFormat.format("{0} * {1}", //$NON-NLS-1$
                            coin.getName(), coin.getCoinGeckoCoindId());
            item.actions.add(new LabelOnly(heading));

            var label = MessageFormat.format("CoinGecko * {0} * {1} * {2}", //$NON-NLS-1$
                            item.security.getCurrencyCode(), //
                            coin.getTickerSymbol().toUpperCase(), //
                            coin.getCoinGeckoCoindId());

            var action = new SimpleAction(label, a -> {
                item.security.setFeed(CoinGeckoQuoteFeed.ID);
                item.security.setTickerSymbol(coin.getTickerSymbol().toUpperCase());
                item.security.setPropertyValue(SecurityProperty.Type.FEED, CoinGeckoQuoteFeed.COINGECKO_COIN_ID,
                                coin.getCoinGeckoCoindId());
            });

            item.actions.add(action);
            item.selectedAction = action;

            listener.accept(item);
        }

        private boolean searchByIdentifier(SecurityItem item, String identifier,
                        PortfolioPerformanceSearchProvider.Parameter parameter) throws IOException
        {
            var results = new PortfolioPerformanceSearchProvider().search(parameter, identifier);

            if (results.isEmpty())
                return false;

            for (var result : results)
            {
                var markets = (result.getMarkets().isEmpty() ? List.of(result) : result.getMarkets()) //
                                .stream()
                                .filter(r -> Objects.equal(item.security.getCurrencyCode(), r.getCurrencyCode()))
                                .toList();

                if (markets.isEmpty())
                    continue;

                var proposedMarket = !QuoteFeed.MANUAL.equals(item.security.getFeed())
                                && !PortfolioPerformanceFeed.ID.equals(item.security.getFeed())
                                                ? markets.stream()
                                                                .filter(m -> Objects.equal(m.getSymbol(),
                                                                                item.security.getTickerSymbol()))
                                                                .findFirst().orElse(markets.getFirst())
                                                : null;

                addSecurityInfoAction(item, result);

                for (var market : markets)
                {
                    var action = new UseBuiltInFeedAction(item, market);
                    item.actions.add(action);

                    if (proposedMarket != null && market == proposedMarket)
                    {
                        item.selectedAction = action;
                    }
                }
            }

            listener.accept(item);
            return true;
        }

        private void addSecurityInfoAction(SecurityItem item, ResultItem onlineItem)
        {
            var label = MessageFormat.format("{0} * {1}", //$NON-NLS-1$
                            onlineItem.getName(), //
                            onlineItem.getIsin() != null ? onlineItem.getIsin() : ""); //$NON-NLS-1$

            item.actions.add(new LabelOnly(label));
        }
    }

    private static final int DO_NOT_CHANGE_ID = 5712;

    private final Client client;
    private final List<SecurityItem> securities;

    private TableViewer tableViewer;

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

        var oauthClient = OAuthClient.INSTANCE;

        if (!oauthClient.isAuthenticated())
            setErrorMessage(Messages.MsgHistoricalPricesRequireSignIn);

        Runnable updateListener = () -> Display.getDefault().asyncExec(() -> setErrorMessage(
                        oauthClient.isAuthenticated() ? null : Messages.MsgHistoricalPricesRequireSignIn));

        oauthClient.addStatusListener(updateListener);
        getContents().addDisposeListener(e -> oauthClient.removeStatusListener(updateListener));
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
            new UpdatePricesJob(client, updatedSecurities).schedule();
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
        preferredSize.x = Math.clamp(preferredSize.x, 700, 1000);
        preferredSize.y = Math.clamp(preferredSize.y, 500, 700);
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

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
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

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        ((GridLayout) parent.getLayout()).numColumns++;
        var button = LoginButton.create(parent);
        setButtonLayoutData(button);

        createButton(parent, DO_NOT_CHANGE_ID, Messages.MenuDoNotChange, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == DO_NOT_CHANGE_ID)
        {
            securities.forEach(s -> s.selectedAction = null);
            tableViewer.refresh();
        }
        else
        {
            super.buttonPressed(buttonId);
        }
    }

    private void setupDirtyListener(TableViewer tableViewer)
    {
        PropertyChangeListener listener = e -> Display.getDefault().asyncExec(() -> tableViewer.refresh(true));
        client.addPropertyChangeListener("dirty", listener); //$NON-NLS-1$
        tableViewer.getTable().addDisposeListener(e -> client.removePropertyChangeListener("dirty", listener)); //$NON-NLS-1$
    }

    private void triggerJob(TableViewer tableViewer, IProgressMonitor progressMonitor)
    {
        var job = new CheckPortfolioPerformanceAPIThread(item -> Display.getDefault().asyncExec(() -> {
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
        layout.setColumnData(column.getColumn(), new ColumnPixelData(50, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnISIN);
        column.setLabelProvider(ColumnLabelProvider.createTextProvider(e -> ((SecurityItem) e).security.getIsin()));
        layout.setColumnData(column.getColumn(), new ColumnPixelData(100, true));

        column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnTicker);
        column.setLabelProvider(
                        ColumnLabelProvider.createTextProvider(e -> ((SecurityItem) e).security.getTickerSymbol()));
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
                                : MessageFormat.format(Messages.LabelNumberOfCandidates, ((SecurityItem) e).actions
                                                .stream().filter(a -> !a.isEnabled()).count());
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
                    if (action instanceof LabelOnly)
                        menuManager.add(new Separator());

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

                // Find and select the DeactivateAction for this security
                // The action will be executed when the dialog is confirmed
                var deactivate = new SimpleAction(Factory.getQuoteFeed(ManualQuoteFeed.class).getName(), a -> {
                    for (Object e : selection)
                    {
                        var securityItem = (SecurityItem) e;
                        securityItem.selectedAction = securityItem.actions.stream()
                                        .filter(DeactivateAction.class::isInstance).findFirst().orElse(null);
                    }
                    tableViewer.refresh();
                });
                menuManager.add(deactivate);

                menuManager.add(new Separator());

                addAvailableExchanges(tableViewer, menuManager, selection);
            }

        }).hook();
    }

    private void addAvailableExchanges(TableViewer tableViewer, IMenuManager menuManager,
                    IStructuredSelection selection)
    {
        var exchanges = new HashSet<Pair<String, String>>();
        for (Object item : selection)
        {
            var actions = ((SecurityItem) item).actions;
            for (var action : actions)
            {
                if (action instanceof UseBuiltInFeedAction builtIn)
                {
                    var exchange = builtIn.getExchange();
                    if (exchange != null)
                        exchanges.add(new Pair<>(exchange, MarketIdentifierCodes.getLabel(exchange)));
                }
            }
        }

        if (!exchanges.isEmpty())
        {
            menuManager.add(new Separator());
            menuManager.add(new LabelOnly(Messages.LabelExchange));

            exchanges.stream() //
                            .sorted((l, r) -> TextUtil.compare(l.getValue(), r.getValue())) //
                            .forEach(exchange -> menuManager.add(new SimpleAction(exchange.getValue(), a -> {
                                for (var item : selection)
                                {
                                    var securityItem = (SecurityItem) item;
                                    for (var action : securityItem.actions)
                                    {
                                        if (action instanceof UseBuiltInFeedAction builtIn
                                                        && exchange.getKey().equals(builtIn.getExchange()))
                                        {
                                            securityItem.selectedAction = action;
                                            break;
                                        }
                                    }
                                }
                                tableViewer.refresh();
                            })));
        }
    }
}
