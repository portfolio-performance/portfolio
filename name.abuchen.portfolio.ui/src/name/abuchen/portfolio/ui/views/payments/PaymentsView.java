package name.abuchen.portfolio.ui.views.payments;

import java.util.EnumSet;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Mode;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsView extends AbstractFinanceView
{
    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private ExchangeRateProviderFactory factory;

    private PaymentsViewInput viewInput;
    private ClientFilterMenu clientFilterMenu;
    private PaymentsViewModel model;

    private CTabFolder folder;

    @Inject
    @Optional
    public void setup(@Named(UIConstants.Parameter.VIEW_PARAMETER) PaymentsViewInput viewInput)
    {
        this.viewInput = viewInput;
    }

    @PostConstruct
    public void setupModel()
    {
        if (viewInput == null)
            viewInput = PaymentsViewInput.fromPreferences(preferences, client);

        CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        model = new PaymentsViewModel(converter, client);
        setToContext(PaymentsViewModel.class.getName(), model);

        // setup filter
        clientFilterMenu = new ClientFilterMenu(client, preferences, filter -> {
            setFilteredClientToModel(filter);
            viewInput.setClientFilterId(clientFilterMenu.getSelectedItem().getId());
            model.recalculate();
        });

        // set initial Filter
        loadSavedFilterIdAndSetFilteredClientToModel();

        model.configure(viewInput.getYear(), viewInput.getMode(), viewInput.isUseGrossValue(),
                        viewInput.isUseConsolidateRetired());

        model.setHideTotalsAtTheTop(preferences.getBoolean(PaymentsViewInput.TOP));
        model.setHideTotalsAtTheBottom(preferences.getBoolean(PaymentsViewInput.BOTTOM));

        model.addUpdateListener(() -> {
            viewInput.setYear(model.getStartYear());
            viewInput.setMode(model.getMode());
            viewInput.setUseGrossValue(model.usesGrossValue());
            viewInput.setUseConsolidateRetired(model.usesConsolidateRetired());
        });
    }

    private void setFilteredClientToModel(ClientFilter filter)
    {
        Client filteredClient = filter.filter(client);
        model.setFilteredClient(filteredClient);
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);
    }

    private void loadSavedFilterIdAndSetFilteredClientToModel()
    {
        viewInput.getClientFilterId()
                        .ifPresent(clientFilterId -> clientFilterMenu.selectItemFromClientFilterId(clientFilterId)
                                        .ifPresent(item -> setFilteredClientToModel(item.getFilter())));
    }

    @Override
    public void notifyModelUpdated()
    {
        // reload client filter, in case its data is outdated
        loadSavedFilterIdAndSetFilteredClientToModel();

        model.recalculate();
    }

    @Override
    protected String getDefaultTitle()
    {
        return model.getMode().getLabel();
    }

    @Override
    protected void addViewButtons(ToolBarManager toolBarManager)
    {
        for (PaymentsViewModel.Mode mode : PaymentsViewModel.Mode.values())
        {
            ActionContributionItem item = new ActionContributionItem( //
                            new SimpleAction(TextUtil.tooltip(mode.getLabel()), a -> {
                                model.setMode(mode);
                                updateIcons(toolBarManager);
                                updateTitle(model.getMode().getLabel());
                            }));
            item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
            toolBarManager.add(item);
        }

        updateIcons(toolBarManager);
    }

    private void updateIcons(ToolBarManager toolBarManager)
    {
        int index = 0;
        for (IContributionItem item : toolBarManager.getItems())
        {
            Images image = index == model.getMode().ordinal() ? Images.VIEW_SELECTED : Images.VIEW;
            ((ActionContributionItem) item).getAction().setImageDescriptor(image.descriptor());
            index++;
        }
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new StartYearSelectionDropDown(model));

        DropDown dropDown = new DropDown(Messages.MenuChooseClientFilter,
                        clientFilterMenu.hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF, SWT.NONE,
                        clientFilterMenu::menuAboutToShow);
        clientFilterMenu.addListener(f -> dropDown
                        .setImage(clientFilterMenu.hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF));
        toolBar.add(dropDown);

        toolBar.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE, manager -> {
            final int itemCount = folder.getItemCount();
            for (int ii = 0; ii < itemCount; ii++)
            {
                PaymentsTab tab = (PaymentsTab) folder.getItem(ii).getData();
                if (tab != null)
                    tab.addExportActions(manager);
            }
        }));

        toolBar.add(new DropDown(Messages.MenuConfigureView, Images.CONFIG, SWT.NONE, manager -> {

            EnumSet<Mode> supportGrossValue = EnumSet.of(Mode.DIVIDENDS, Mode.INTEREST, Mode.EARNINGS);
            if (supportGrossValue.contains(model.getMode()))
            {
                Action action = new SimpleAction(Messages.LabelUseGrossValue,
                                a -> model.setUseGrossValue(!model.usesGrossValue()));
                action.setChecked(model.usesGrossValue());
                manager.add(action);
            }

            Action action = new SimpleAction(Messages.LabelPaymentsUseConsolidateRetired,
                            a -> model.setUseConsolidateRetired(!model.usesConsolidateRetired()));
            action.setChecked(model.usesConsolidateRetired());
            manager.add(action);

            PaymentsTab tab = (PaymentsTab) folder.getSelection().getData();
            if (tab != null)
            {
                manager.add(new Separator());
                tab.addConfigActions(manager);
            }
        }));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        folder = new CTabFolder(parent, SWT.BORDER);

        createTab(folder, Images.VIEW_TABLE, PaymentsPerMonthMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, PaymentsPerQuarterMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, PaymentsPerYearMatrixTab.class);
        createChartTab(folder, Images.VIEW_BARCHART, new PaymentsPerMonthChartBuilder());
        createChartTab(folder, Images.VIEW_BARCHART, new PaymentsPerQuarterChartBuilder());
        createChartTab(folder, Images.VIEW_BARCHART, new PaymentsPerYearChartBuilder());
        createChartTab(folder, Images.VIEW_LINECHART, new PaymentsAccumulatedChartBuilder());
        createTab(folder, Images.CALENDAR_OFF, PaymentsYearlyOverviewTab.class);
        createTab(folder, Images.VIEW_TABLE, TransactionsTab.class);

        int tab = viewInput.getTab();
        if (tab < 0 || tab > 7)
            tab = 0;
        folder.setSelection(tab);
        folder.addDisposeListener(e -> viewInput.setTab(folder.getSelectionIndex()));

        folder.addDisposeListener(e -> viewInput.writeToPreferences(preferences, client));

        return folder;
    }

    private void createTab(CTabFolder folder, Images image, Class<? extends PaymentsTab> tabClass)
    {
        PaymentsTab tab = this.make(tabClass, model);
        Control control = tab.createControl(folder);
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(tab.getLabel());
        item.setControl(control);
        item.setData(tab);
        item.setImage(image.image());
    }

    private void createChartTab(CTabFolder folder, Images image, PaymentsChartBuilder chartBuilder)
    {
        PaymentsChartTab tab = this.make(PaymentsChartTab.class, model);
        tab.setChartBuilder(chartBuilder);
        Control control = tab.createControl(folder);
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(tab.getLabel());
        item.setControl(control);
        item.setData(tab);
        item.setImage(image.image());
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        pages.add(make(PaymentsTooltipPane.class));
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
    }
}
