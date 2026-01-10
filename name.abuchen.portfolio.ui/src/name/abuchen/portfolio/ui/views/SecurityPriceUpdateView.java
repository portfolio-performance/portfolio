package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.bootstrap.BundleMessages;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.handlers.UpdateQuotesHandler;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateProgress;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateSnapshot;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdatePricesJob;
import name.abuchen.portfolio.ui.jobs.priceupdate.UpdateStatus;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.BookmarkMenu;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.CommandAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ToolTipCustomProviderSupport;
import name.abuchen.portfolio.ui.views.columns.IsinColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.ui.views.columns.SymbolColumn;
import name.abuchen.portfolio.ui.views.columns.WknColumn;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesDataQualityPane;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;
import name.abuchen.portfolio.ui.wizards.security.FindQuoteProviderDialog;

public class SecurityPriceUpdateView extends AbstractFinanceView implements PriceUpdateProgress.Listener
{
    @Inject
    private SelectionService selectionService;

    private ShowHideColumnHelper columns;
    private TableViewer securities;

    /**
     * Timestamp of the ongoing price update job. Used to determine whether the
     * list of securities needs updating.
     */
    private long timestamp = 0;

    /**
     * Last status of the price update job.
     */
    private PriceUpdateSnapshot statuses = new PriceUpdateSnapshot(0, new HashMap<>());

    /**
     * Active filters for status types. Default: WAITING, LOADING and ERROR.
     */
    private Set<UpdateStatus> activeFilters = EnumSet.of(UpdateStatus.WAITING, UpdateStatus.LOADING,
                    UpdateStatus.ERROR);

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPriceUpdateProgress;
    }

    @Override
    public void notifyModelUpdated()
    {
        // ignore as we get frequent status update from the job progress monitor
    }

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) PriceUpdateSnapshot status)
    {
        this.statuses = status;
    }

    @Override
    public void onProgress(PriceUpdateSnapshot status)
    {
        getEditorActivationState().deferUntilNotEditing(() -> {
            var isNewRequest = this.timestamp != status.getTimestamp();

            this.timestamp = status.getTimestamp();
            this.statuses = status;

            if (!securities.getTable().isDisposed())
            {
                if (isNewRequest)
                {
                    columns.invalidateCache();
                    securities.setInput(status.getSecurities());

                    var dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

                    updateTitle(MessageFormat.format(Messages.LabelWithQualifier, getDefaultTitle(),
                                    Values.DateTime.format(dateTime)));
                }
                else
                {
                    securities.refresh(true);
                }
            }
        });
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(CommandAction.forCommand(getContext(), "\u25B6 " + Messages.CmdMigratePortfolioReport + " \u25C0", //$NON-NLS-1$ //$NON-NLS-2$
                        UIConstants.Command.MIGRATE_PORTFOLIO_REPORT));

        toolBar.add(new Separator());

        toolBar.add(new DropDown(Messages.JobLabelUpdateQuotes, manager -> {
            manager.add(CommandAction.forCommand(getContext(), Messages.JobLabelUpdateQuotes,
                            UIConstants.Command.UPDATE_QUOTES));

            manager.add(CommandAction.forCommand(getContext(),
                            BundleMessages.getString(BundleMessages.Label.Command.updateQuotesActiveSecurities),
                            UIConstants.Command.UPDATE_QUOTES, //
                            UIConstants.Parameter.FILTER, UpdateQuotesHandler.FilterType.ACTIVE.name()));

            manager.add(CommandAction.forCommand(getContext(),
                            BundleMessages.getString(BundleMessages.Label.Command.updateQuotesHoldings),
                            UIConstants.Command.UPDATE_QUOTES, //
                            UIConstants.Parameter.FILTER, UpdateQuotesHandler.FilterType.HOLDINGS.name()));

            var selection = selectionService.getSelection(getClient());
            var selectionMenu = CommandAction.forCommand(getContext(),
                            MessageFormat.format(Messages.MenuUpdatePricesForSelectedInstruments,
                                            selection.isPresent() ? selection.get().getSecurities().size() : 0),
                            UIConstants.Command.UPDATE_QUOTES, //
                            UIConstants.Parameter.FILTER, UpdateQuotesHandler.FilterType.SECURITY.name());
            selectionMenu.setEnabled(selection.isPresent() && !selection.get().getSecurities().isEmpty());
            manager.add(selectionMenu);

            var watchlistMenu = new MenuManager(
                            BundleMessages.getString(BundleMessages.Label.Command.updateQuotesWatchlist));
            for (var watchlist : getClient().getWatchlists())
            {
                watchlistMenu.add(CommandAction.forCommand(getContext(), watchlist.getName(),
                                UIConstants.Command.UPDATE_QUOTES, //
                                UIConstants.Parameter.FILTER, UpdateQuotesHandler.FilterType.WATCHLIST.name(),
                                UIConstants.Parameter.WATCHLIST, watchlist.getName()));
            }
            manager.add(watchlistMenu);
        }));

        toolBar.add(new Separator());

        var image = new Action(null, Images.FILTER_ON.descriptor())
        {
        };
        image.setEnabled(false);
        toolBar.add(image);

        var showInProgressAction = new SimpleAction(Messages.LabelInProgress,
                        a -> toggleFilter(UpdateStatus.WAITING, UpdateStatus.LOADING));
        showInProgressAction.setChecked(
                        activeFilters.contains(UpdateStatus.WAITING) || activeFilters.contains(UpdateStatus.LOADING));
        toolBar.add(showInProgressAction);

        var showCompletedAction = new SimpleAction(Messages.LabelCompleted,
                        a -> toggleFilter(UpdateStatus.MODIFIED, UpdateStatus.UNMODIFIED, UpdateStatus.SKIPPED));
        showCompletedAction.setChecked(
                        activeFilters.contains(UpdateStatus.MODIFIED) || activeFilters.contains(UpdateStatus.UNMODIFIED)
                                        || activeFilters.contains(UpdateStatus.SKIPPED));
        toolBar.add(showCompletedAction);

        var showErrorAction = new SimpleAction(Messages.LabelError, a -> toggleFilter(UpdateStatus.ERROR));
        showErrorAction.setChecked(activeFilters.contains(UpdateStatus.ERROR));
        toolBar.add(showErrorAction);

        toolBar.add(new Separator());

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> columns.menuAboutToShow(manager)));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        securities = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        columns = new ShowHideColumnHelper(SecurityPriceUpdateView.class.getSimpleName(), getPreferenceStore(),
                        securities, layout);

        ToolTipCustomProviderSupport.enableFor(securities, ToolTip.NO_RECREATE);
        ColumnEditingSupport.prepare(getEditorActivationState(), securities);
        CopyPasteSupport.enableFor(securities);

        createColumns();

        columns.createColumns(true);

        securities.getTable().setHeaderVisible(true);
        securities.getTable().setLinesVisible(true);

        securities.setContentProvider(ArrayContentProvider.getInstance());
        securities.addFilter(createStatusFilter());

        securities.addSelectionChangedListener(event -> {
            var selection = SecuritySelection.from(getClient(), event.getStructuredSelection());
            selectionService.setSelection(selection.isEmpty() ? null : selection);
            setInformationPaneInput(selection);
        });

        hookContextMenu(securities.getTable(), this::fillContextMenu);

        securities.setInput(statuses.getSecurities());

        PriceUpdateProgress.getInstance().register(getClient(), this);
        container.addDisposeListener(e -> PriceUpdateProgress.getInstance().unregister(getClient(), this));

        return container;
    }

    private ViewerFilter createStatusFilter()
    {
        return new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                Security security = (Security) element;

                boolean historicMatch = statuses.getHistoricStatus(security)
                                .map(s -> activeFilters.contains(s.getStatus())).orElse(false);

                boolean latestMatch = statuses.getLatestStatus(security).map(s -> activeFilters.contains(s.getStatus()))
                                .orElse(false);

                return historicMatch || latestMatch;
            }
        };
    }

    private void toggleFilter(UpdateStatus... statuses)
    {
        var hasAll = Arrays.asList(statuses).stream().allMatch(activeFilters::contains);

        if (hasAll)
        {
            Arrays.asList(statuses).forEach(activeFilters::remove);
        }
        else
        {
            Arrays.asList(statuses).forEach(activeFilters::add);
        }
        securities.refresh();
    }

    private void createColumns()
    {
        // security name
        Column column = new NameColumn(getClient());
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setSortDirction(SWT.UP);
        columns.addColumn(column);

        addColumnLatestPrice();
        addColumnDateOfLatestPrice();

        addStatusHistoricPrices();
        addStatusLatestPrices();

        // isin
        column = new IsinColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        columns.addColumn(column);

        // ticker
        column = new SymbolColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        columns.addColumn(column);

        // wkn
        column = new WknColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        columns.addColumn(column);

        // note
        column = new NoteColumn();
        column.getEditingSupport().addListener(new TouchClientListener(getClient()));
        column.setVisible(false);
        columns.addColumn(column);
    }

    private void addColumnLatestPrice()
    {
        Column column = new Column("last", Messages.ColumnLatest, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnLatest_MenuLabel);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Security security = (Security) e;
                SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                if (latest == null)
                    return null;

                if (security.getCurrencyCode() == null)
                    return Values.Quote.format(latest.getValue());
                else
                    return Values.Quote.format(security.getCurrencyCode(), latest.getValue(),
                                    getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            SecurityPrice p1 = ((Security) o1).getSecurityPrice(LocalDate.now());
            SecurityPrice p2 = ((Security) o2).getSecurityPrice(LocalDate.now());

            if (p1 == null)
                return p2 == null ? 0 : -1;
            if (p2 == null)
                return 1;

            return Long.compare(p1.getValue(), p2.getValue());
        }));
        columns.addColumn(column);
    }

    private void addColumnDateOfLatestPrice()
    {
        Column column;
        column = new Column("last-date", Messages.ColumnLatestDate, SWT.LEFT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnLatestDate_MenuLabel);

        Function<Object, LocalDate> dataProvider = element -> {
            SecurityPrice latest = ((Security) element).getSecurityPrice(LocalDate.now());
            return latest != null ? latest.getDate() : null;
        };

        column.setLabelProvider(new DateLabelProvider(dataProvider)
        {
            @Override
            public Color getBackground(Object element)
            {
                Security security = (Security) element;
                SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                if (latest == null)
                    return null;

                String feed = security.getLatestFeed() != null ? security.getLatestFeed() : security.getFeed();
                if (QuoteFeed.MANUAL.equals(feed))
                    return null;

                LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
                return latest.getDate().isBefore(sevenDaysAgo) ? Colors.theme().warningBackground() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(dataProvider::apply));
        columns.addColumn(column);
    }

    private void addStatusHistoricPrices()
    {
        var label = MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnStatus,
                        Messages.EditWizardQuoteFeedTitle);
        var column = new Column("historic-status", label, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardQuoteFeedTitle);
        column.setMenuLabel(Messages.ColumnStatus);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return statuses.getHistoricStatus((Security) element).map(s -> s.getStatus().toString()).orElse(""); //$NON-NLS-1$
            }

            @Override
            public Color getBackground(Object element)
            {
                return statuses.getHistoricStatus((Security) element).map(s -> getBackgroundFor(s.getStatus()))
                                .orElse(null);
            }
        });
        columns.addColumn(column);

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnMessage,
                        Messages.EditWizardQuoteFeedTitle);
        column = new Column("historic-message", label, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardQuoteFeedTitle);
        column.setMenuLabel(Messages.ColumnMessage);
        column.setLabelProvider(ColumnLabelProvider.createTextProvider(
                        element -> statuses.getHistoricStatus((Security) element).map(s -> s.getMessage()).orElse(""))); //$NON-NLS-1$
        columns.addColumn(column);

        Function<Object, String> quoteFeed = e -> {
            String feedId = ((Security) e).getFeed();
            if (feedId == null || feedId.isEmpty())
                return null;

            QuoteFeed feed = Factory.getQuoteFeedProvider(feedId);
            return feed != null ? feed.getName() : null;
        };

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.LabelQuoteFeed,
                        Messages.EditWizardQuoteFeedTitle);
        column = new Column("qf-historic", label, SWT.LEFT, 200); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardQuoteFeedTitle);
        column.setMenuLabel(Messages.LabelQuoteFeed);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return quoteFeed.apply(e);
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(quoteFeed::apply));
        columns.addColumn(column);

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.EditWizardQuoteFeedLabelFeedURL,
                        Messages.EditWizardQuoteFeedTitle);
        column = new Column("url-history", label, SWT.LEFT, 200); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardQuoteFeedTitle);
        column.setMenuLabel(Messages.EditWizardQuoteFeedLabelFeedURL);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Security security = (Security) e;
                return security.getFeedURL();
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(s -> ((Security) s).getFeedURL()));
        columns.addColumn(column);
    }

    private void addStatusLatestPrices()
    {
        var label = MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnStatus,
                        Messages.EditWizardLatestQuoteFeedTitle);
        var column = new Column("status-latest", label, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardLatestQuoteFeedTitle);
        column.setMenuLabel(Messages.ColumnStatus);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return statuses.getLatestStatus((Security) element).map(s -> s.getStatus().toString()).orElse(""); //$NON-NLS-1$
            }

            @Override
            public Color getBackground(Object element)
            {
                return statuses.getLatestStatus((Security) element).map(s -> getBackgroundFor(s.getStatus()))
                                .orElse(null);
            }
        });
        columns.addColumn(column);

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.ColumnMessage,
                        Messages.EditWizardLatestQuoteFeedTitle);
        column = new Column("message-latest", label, SWT.LEFT, 80); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardLatestQuoteFeedTitle);
        column.setMenuLabel(Messages.ColumnMessage);
        column.setLabelProvider(ColumnLabelProvider.createTextProvider(
                        element -> statuses.getLatestStatus((Security) element).map(s -> s.getMessage()).orElse(""))); //$NON-NLS-1$
        columns.addColumn(column);

        Function<Object, String> latestQuoteFeed = e -> {
            Security security = (Security) e;
            String feedId = security.getLatestFeed();
            if (feedId == null || feedId.isEmpty())
                return security.getFeed() != null ? Messages.EditWizardOptionSameAsHistoricalQuoteFeed : null;

            QuoteFeed feed = Factory.getQuoteFeedProvider(feedId);
            return feed != null ? feed.getName() : null;
        };

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.LabelQuoteFeed,
                        Messages.EditWizardLatestQuoteFeedTitle);
        column = new Column("qf-latest", label, SWT.LEFT, 200); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardLatestQuoteFeedTitle);
        column.setMenuLabel(Messages.LabelQuoteFeed);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return latestQuoteFeed.apply(e);
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(latestQuoteFeed::apply));
        columns.addColumn(column);

        label = MessageFormat.format(Messages.LabelWithQualifier, Messages.EditWizardQuoteFeedLabelFeedURL,
                        Messages.EditWizardLatestQuoteFeedTitle);
        column = new Column("url-latest", label, SWT.LEFT, 200); //$NON-NLS-1$
        column.setGroupLabel(Messages.EditWizardLatestQuoteFeedTitle);
        column.setMenuLabel(Messages.EditWizardQuoteFeedLabelFeedURL);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Security security = (Security) e;
                return security.getLatestFeedURL();
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(s -> ((Security) s).getLatestFeedURL()));
        columns.addColumn(column);
    }

    private Color getBackgroundFor(UpdateStatus status)
    {
        switch (status)
        {
            case UpdateStatus.ERROR:
                return Colors.theme().warningBackground();
            case UpdateStatus.MODIFIED, UpdateStatus.UNMODIFIED:
                return Colors.theme().greenBackground();
            case UpdateStatus.LOADING:
                return Colors.theme().chipBackground();
            case UpdateStatus.WAITING, UpdateStatus.SKIPPED:
                return null;
            default:
                return null;
        }
    }

    private void fillContextMenu(IMenuManager manager)
    {
        var selection = SecuritySelection.from(getClient(), securities.getStructuredSelection());

        if (selection.isEmpty())
            return;

        if (selection.size() == 1)
        {
            Security security = selection.getSecurities().getFirst();
            manager.add(new Action(Messages.SecurityMenuConfigureOnlineUpdate)
            {
                @Override
                public void run()
                {
                    EditSecurityDialog dialog = make(EditSecurityDialog.class, security);
                    dialog.setShowQuoteConfigurationInitially(true);

                    if (dialog.open() != Window.OK)
                        return;

                    markDirty();
                }
            });

            manager.add(new Separator());
            new QuotesContextMenu(this).menuAboutToShow(manager, security);
        }

        manager.add(new Separator());

        manager.add(new BookmarkMenu(getPart(), selection.getSecurities()));

        manager.add(new Separator());
        // update quotes for multiple securities
        if (selection.size() > 1)
        {
            manager.add(new SimpleAction(
                            MessageFormat.format(Messages.SecurityMenuUpdateQuotesMultipleSecurities, selection.size()),
                            a -> new UpdatePricesJob(getClient(), selection.getSecurities()).schedule()));

            manager.add(new SimpleAction(Messages.LabelSearchForQuoteFeeds + "...", //$NON-NLS-1$
                            a -> Display.getDefault().asyncExec(() -> {
                                FindQuoteProviderDialog dialog = new FindQuoteProviderDialog(
                                                securities.getTable().getShell(), getClient(),
                                                selection.getSecurities());
                                dialog.open();
                            })));

        }
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(HistoricalPricesDataQualityPane.class));
    }
}
