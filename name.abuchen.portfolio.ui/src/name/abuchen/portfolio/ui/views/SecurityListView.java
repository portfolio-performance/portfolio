package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.events.ChangeEventConstants;
import name.abuchen.portfolio.events.SecurityCreatedEvent;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.DomainElement;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.CommandAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesDataQualityPane;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.ui.wizards.datatransfer.CSVImportWizard;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;

public class SecurityListView extends AbstractFinanceView
{
    private class CreateSecurityDropDown extends DropDown implements IMenuListener
    {
        public CreateSecurityDropDown()
        {
            super(Messages.SecurityMenuAddNewSecurity, Images.PLUS, SWT.NONE);
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(CommandAction.forCommand(getContext(), DomainElement.INVESTMENT_VEHICLE.getPaletteLabel(),
                            UIConstants.Command.NEW_DOMAIN_ELEMENT, UIConstants.Parameter.TYPE,
                            DomainElement.INVESTMENT_VEHICLE.name()));

            manager.add(CommandAction.forCommand(getContext(), DomainElement.CRYPTO_CURRENCY.getPaletteLabel(),
                            UIConstants.Command.NEW_DOMAIN_ELEMENT, UIConstants.Parameter.TYPE,
                            DomainElement.CRYPTO_CURRENCY.name()));

            manager.add(CommandAction.forCommand(getContext(), DomainElement.EXCHANGE_RATE.getPaletteLabel(),
                            UIConstants.Command.NEW_DOMAIN_ELEMENT, UIConstants.Parameter.TYPE,
                            DomainElement.EXCHANGE_RATE.name()));

            manager.add(CommandAction.forCommand(getContext(), DomainElement.CONSUMER_PRICE_INDEX.getPaletteLabel(),
                            UIConstants.Command.NEW_DOMAIN_ELEMENT, UIConstants.Parameter.TYPE,
                            DomainElement.CONSUMER_PRICE_INDEX.name()));

            manager.add(new Separator());

            manager.add(new Action(Messages.SecurityMenuImportCSV)
            {
                @Override
                public void run()
                {
                    FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
                    fileDialog.setFilterNames(
                                    new String[] { Messages.CSVImportLabelFileCSV, Messages.CSVImportLabelFileAll });
                    fileDialog.setFilterExtensions(new String[] { "*.csv;*.CSV", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
                    String fileName = fileDialog.open();

                    if (fileName == null)
                        return;

                    CSVImportWizard wizard = new CSVImportWizard(getClient(), getPreferenceStore(), new File(fileName));
                    inject(wizard);

                    // pre-select import of securities
                    wizard.setExtractor("investment-vehicle"); //$NON-NLS-1$

                    Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);

                    if (dialog.open() != Window.OK)
                        return;

                    markDirty();
                    notifyModelUpdated();
                }
            });

            manager.add(new Separator());

            manager.add(new SimpleAction(Messages.SecurityMenuEmptyInstrument + "...", a -> { //$NON-NLS-1$
                Security newSecurity = new Security(null, getClient().getBaseCurrency());
                newSecurity.setFeed(QuoteFeed.MANUAL);
                newSecurity.setCurrencyCode(getClient().getBaseCurrency());
                openEditDialog(newSecurity);
            }));

        }

        private void openEditDialog(Security newSecurity)
        {
            Dialog dialog = make(EditSecurityDialog.class, newSecurity);

            if (dialog.open() == Window.OK)
                addNewSecurity(newSecurity);
        }

        private void addNewSecurity(Security newSecurity)
        {
            getClient().addSecurity(newSecurity);

            if (watchlist != null)
                watchlist.addSecurity(newSecurity);

            markDirty();

            setSecurityTableInput();
            securities.getTableViewer().setSelection(new StructuredSelection(newSecurity), true);

            securities.updateQuotes(newSecurity);
        }
    }

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private final Predicate<Security> securityIsNotInactive = record -> !record.isRetired();
        private final Predicate<Security> securityIsInactive = record -> record.isRetired();
        private final Predicate<Security> onlySecurities = record -> !record.isExchangeRate();
        private final Predicate<Security> onlyExchangeRates = record -> record.isExchangeRate();
        private final Predicate<Security> sharesNotZero = record -> getSharesHeld(getClient(), record) != 0;
        private final Predicate<Security> sharesEqualZero = record -> getSharesHeld(getClient(), record) == 0;
        private final Predicate<Security> limitPriceExceeded = record -> isLimitPriceExceeded(record);

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityListFilter, Images.FILTER_OFF, SWT.NONE);
            setMenuListener(this);

            int savedFilters = 0;

            if (adhocFilter != null)
                filter.add(adhocFilter);
            else if (watchlist != null)
                savedFilters = preferenceStore.getInt(
                                this.getClass().getSimpleName() + "-filterSettings" + "-" + watchlist.getName()); //$NON-NLS-1$ //$NON-NLS-2$
            else
                savedFilters = preferenceStore.getInt(this.getClass().getSimpleName() + "-filterSettings"); //$NON-NLS-1$

            if ((savedFilters & (1 << 1)) != 0)
                filter.add(securityIsNotInactive);
            if ((savedFilters & (1 << 2)) != 0)
                filter.add(onlySecurities);
            if ((savedFilters & (1 << 3)) != 0)
                filter.add(onlyExchangeRates);
            if ((savedFilters & (1 << 4)) != 0)
                filter.add(sharesNotZero);
            if ((savedFilters & (1 << 5)) != 0)
                filter.add(sharesEqualZero);
            if ((savedFilters & (1 << 6)) != 0)
                filter.add(limitPriceExceeded);
            if ((savedFilters & (1 << 7)) != 0)
                filter.add(securityIsInactive);

            if (!filter.isEmpty())
                setImage(Images.FILTER_ON);

            addDisposeListener(e -> {

                // save filter configuration only if it is not an ad hoc filter
                if (adhocFilter != null)
                    return;

                int savedFilter = 0;
                if (filter.contains(securityIsNotInactive))
                    savedFilter += (1 << 1);
                if (filter.contains(onlySecurities))
                    savedFilter += (1 << 2);
                if (filter.contains(onlyExchangeRates))
                    savedFilter += (1 << 3);
                if (filter.contains(sharesNotZero))
                    savedFilter += (1 << 4);
                if (filter.contains(sharesEqualZero))
                    savedFilter += (1 << 5);
                if (filter.contains(limitPriceExceeded))
                    savedFilter += (1 << 6);
                if (filter.contains(securityIsInactive))
                    savedFilter += (1 << 7);
                if (watchlist != null)
                    preferenceStore.setValue(
                                    this.getClass().getSimpleName() + "-filterSettings" + "-" + watchlist.getName(), //$NON-NLS-1$ //$NON-NLS-2$
                                    savedFilter);
                else
                    preferenceStore.setValue(this.getClass().getSimpleName() + "-filterSettings", savedFilter); //$NON-NLS-1$
            });
        }

        /**
         * Collects all shares held for the given security.
         *
         * @param client
         *            {@link Client}
         * @param security
         *            {@link Security}
         * @return shares held on success, else 0
         */
        private long getSharesHeld(Client client, Security security)
        {
            // collect all shares and return a value greater 0
            return Math.max(security.getTransactions(client).stream()
                            .filter(t -> t.getTransaction() instanceof PortfolioTransaction) //
                            .map(t -> (PortfolioTransaction) t.getTransaction()) //
                            .mapToLong(t -> {
                                switch (t.getType())
                                {
                                    case BUY:
                                    case DELIVERY_INBOUND:
                                        return t.getShares();
                                    case SELL:
                                    case DELIVERY_OUTBOUND:
                                        return -t.getShares();
                                    default:
                                        return 0L;
                                }
                            }).sum(), 0);
        }

        private boolean isLimitPriceExceeded(Security security)
        {
            for (Object attribute : security.getAttributes().getMap().values())
            {
                if (!(attribute instanceof LimitPrice limit))
                    continue;

                SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                if (latest != null && limit.isExceeded(latest))
                    return true;
            }

            return false;
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            if (adhocFilter != null)
            {
                manager.add(createAction(Messages.LabelAdhocFilter, adhocFilter));
                manager.add(new Separator());
            }

            manager.add(createAction(Messages.SecurityListFilterHideInactive, securityIsNotInactive));
            manager.add(createAction(Messages.SecurityListFilterOnlyInactive, securityIsInactive));
            manager.add(new Separator());
            manager.add(createAction(Messages.SecurityListFilterOnlySecurities, onlySecurities));
            manager.add(createAction(Messages.SecurityListFilterOnlyExchangeRates, onlyExchangeRates));
            manager.add(new Separator());
            manager.add(createAction(Messages.SecurityFilterSharesHeldNotZero, sharesNotZero));
            manager.add(createAction(Messages.SecurityFilterSharesHeldEqualZero, sharesEqualZero));
            manager.add(new Separator());
            manager.add(createAction(Messages.SecurityListFilterLimitPriceExceeded, limitPriceExceeded));
        }

        private Action createAction(String label, Predicate<Security> predicate)
        {
            Action action = new Action(label, IAction.AS_CHECK_BOX)
            {
                @Override
                public void run()
                {
                    boolean isChecked = filter.contains(predicate);

                    if (isChecked)
                        filter.remove(predicate);
                    else
                        filter.add(predicate);

                    // uncheck mutually exclusive actions if new filter is added
                    if (!isChecked)
                    {
                        if (predicate == onlySecurities)
                            filter.remove(onlyExchangeRates);
                        else if (predicate == onlyExchangeRates)
                            filter.remove(onlySecurities);
                        else if (predicate == sharesEqualZero)
                            filter.remove(sharesNotZero);
                        else if (predicate == sharesNotZero)
                            filter.remove(sharesEqualZero);
                        else if (predicate == securityIsNotInactive)
                            filter.remove(securityIsInactive);
                        else if (predicate == securityIsInactive)
                            filter.remove(securityIsNotInactive);
                    }

                    setImage(filter.isEmpty() ? Images.FILTER_OFF : Images.FILTER_ON);
                    securities.refresh(false);
                }
            };
            action.setChecked(filter.contains(predicate));
            return action;
        }
    }

    @Inject
    private SelectionService selectionService;

    private SecuritiesTable securities;

    private List<Predicate<Security>> filter = new ArrayList<>();

    private Watchlist watchlist;

    private Pattern filterPattern;

    private Predicate<Security> adhocFilter;

    @Override
    protected String getDefaultTitle()
    {
        StringBuilder title = new StringBuilder();
        if (watchlist == null)
            title.append(Messages.LabelSecurities);
        else
            title.append(Messages.LabelSecurities).append(' ').append(watchlist.getName());

        if (securities != null)
            title.append(" (").append(securities.getColumnHelper().getConfigurationName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

        return title.toString();
    }

    @Override
    public void notifyModelUpdated()
    {
        if (securities != null && !securities.getTableViewer().getTable().isDisposed())
        {
            updateTitle(getDefaultTitle());
            securities.refresh(true);
        }
    }

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) Watchlist parameter)
    {
        this.watchlist = parameter;
    }

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) Predicate<Security> adhocFilter)
    {
        this.adhocFilter = adhocFilter;
    }

    @Inject
    @Optional
    public void onSecurityCreated(@UIEventTopic(ChangeEventConstants.Security.CREATED) SecurityCreatedEvent event)
    {
        if (!event.appliesTo(getClient()))
            return; // if security was created by other client, ignore event

        if (watchlist != null)
        {
            watchlist.addSecurity(event.getSecurity());
            getClient().touch();
        }

        setSecurityTableInput();
        securities.getTableViewer().setSelection(new StructuredSelection(event.getSecurity()), true);
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addSearchButton(toolBar);

        toolBar.add(new Separator());

        toolBar.add(new CreateSecurityDropDown());
        toolBar.add(new FilterDropDown(getPreferenceStore()));
        addExportButton(toolBar);

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> securities.getColumnHelper().menuAboutToShow(manager)));
    }

    private void addSearchButton(ToolBarManager toolBar)
    {
        toolBar.add(new ControlContribution("searchbox") //$NON-NLS-1$
        {
            @Override
            protected Control createControl(Composite parent)
            {
                final Text search = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
                search.setMessage(Messages.LabelSearch);
                search.setSize(300, SWT.DEFAULT);

                search.addModifyListener(e -> {
                    String filterText = Pattern.quote(search.getText().trim());
                    if (filterText.length() == 0)
                    {
                        filterPattern = null;
                        securities.refresh(false);
                    }
                    else
                    {
                        filterPattern = Pattern.compile(".*" + filterText + ".*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL); //$NON-NLS-1$ //$NON-NLS-2$
                        securities.refresh(false);
                    }
                });

                return search;
            }

            @Override
            protected int computeWidth(Control control)
            {
                return control.computeSize(100, SWT.DEFAULT, true).x;
            }
        });
    }

    private void addExportButton(ToolBarManager toolBar)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(securities.getTableViewer()) //
                                .export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        toolBar.add(export);
    }

    // //////////////////////////////////////////////////////////////
    // top table: securities
    // //////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        securities = new SecuritiesTable(container, this);
        updateTitle(getDefaultTitle());
        securities.getColumnHelper().addListener(() -> updateTitle(getDefaultTitle()));
        securities.getColumnHelper().setToolBarManager(getViewToolBarManager());

        securities.addSelectionChangedListener(event -> setInformationPaneInput(
                        ((IStructuredSelection) event.getSelection()).getFirstElement()));

        securities.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();

            if (!selection.isEmpty())
                selectionService.setSelection(new SecuritySelection(getClient(), selection.toList()));
            else
                selectionService.setSelection(null);
        });

        securities.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (filterPattern == null)
                    return true;

                Security security = (Security) element;

                String[] properties = new String[] { security.getName(), //
                                security.getIsin(), //
                                security.getTickerSymbol(), //
                                security.getWkn(), //
                                security.getNote() //
                };

                for (String property : properties)
                {
                    if (property != null && filterPattern.matcher(property).matches())
                        return true;
                }

                return false;
            }
        });

        securities.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                for (Predicate<Security> predicate : filter)
                {
                    if (!predicate.test((Security) element))
                        return false;
                }

                return true;
            }
        });

        setSecurityTableInput();

        return container;
    }

    private void setSecurityTableInput()
    {
        if (watchlist != null)
            securities.setInput(watchlist);
        else
            securities.setInput(getClient().getSecurities());
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
        pages.add(make(HistoricalPricesDataQualityPane.class));
    }
}
