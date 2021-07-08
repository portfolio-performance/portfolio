package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.earnings.EarningsViewModel.Mode;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsView extends AbstractFinanceView
{
    private static final String KEY_TAB = EarningsView.class.getSimpleName() + "-tab"; //$NON-NLS-1$
    private static final String KEY_YEAR = EarningsView.class.getSimpleName() + "-year"; //$NON-NLS-1$
    private static final String KEY_MODE = EarningsView.class.getSimpleName() + "-mode"; //$NON-NLS-1$
    private static final String KEY_USE_GROSS_VALUE = EarningsView.class.getSimpleName() + "-use-gross-value"; //$NON-NLS-1$
    private static final String KEY_USE_CONSOLIDATE_RETIRED = EarningsView.class.getSimpleName()
                    + "-use-consolidate-retired"; //$NON-NLS-1$

    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private ExchangeRateProviderFactory factory;

    private EarningsViewModel model;

    private CTabFolder folder;

    @PostConstruct
    public void setupModel()
    {
        CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        model = new EarningsViewModel(this, preferences, converter, client);

        int year = preferences.getInt(KEY_YEAR);
        LocalDate now = LocalDate.now();
        if (year < 1900 || year > now.getYear())
            year = now.getYear() - 2;

        EarningsViewModel.Mode mode = EarningsViewModel.Mode.ALL;
        String prefMode = preferences.getString(KEY_MODE);

        if (prefMode != null && !prefMode.isEmpty())
        {
            try
            {
                mode = EarningsViewModel.Mode.valueOf(prefMode);
            }
            catch (Exception ignore)
            {
                // use default mode
            }
        }

        boolean useGrossValue = preferences.getBoolean(KEY_USE_GROSS_VALUE);
        boolean useConsolidateRetired = preferences.getBoolean(KEY_USE_CONSOLIDATE_RETIRED);

        model.configure(year, mode, useGrossValue, useConsolidateRetired);

        model.addUpdateListener(() -> {
            preferences.setValue(KEY_YEAR, model.getStartYear());
            preferences.setValue(KEY_MODE, model.getMode().name());
            preferences.setValue(KEY_USE_GROSS_VALUE, model.usesGrossValue());
            preferences.setValue(KEY_USE_CONSOLIDATE_RETIRED, model.usesConsolidateRetired());
        });
    }

    @Override
    public void notifyModelUpdated()
    {
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
        for (EarningsViewModel.Mode mode : EarningsViewModel.Mode.values())
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
                        model.getClientFilterMenu().hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF, SWT.NONE,
                        model.getClientFilterMenu()::menuAboutToShow);
        model.getClientFilterMenu().addListener(f -> dropDown.setImage(
                        model.getClientFilterMenu().hasActiveFilter() ? Images.FILTER_ON : Images.FILTER_OFF));
        toolBar.add(dropDown);

        toolBar.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE, manager -> {
            final int itemCount = folder.getItemCount();
            for (int ii = 0; ii < itemCount; ii++)
            {
                EarningsTab tab = (EarningsTab) folder.getItem(ii).getData();
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

            Action action = new SimpleAction(Messages.LabelEarningsUseConsolidateRetired,
                            a -> model.setUseConsolidateRetired(!model.usesConsolidateRetired()));
            action.setChecked(model.usesConsolidateRetired());
            manager.add(action);

            EarningsTab tab = (EarningsTab) folder.getSelection().getData();
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

        createTab(folder, Images.VIEW_TABLE, EarningsPerMonthMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, EarningsPerQuarterMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, EarningsPerYearMatrixTab.class);
        createTab(folder, Images.VIEW_BARCHART, EarningsPerMonthChartTab.class);
        createTab(folder, Images.VIEW_BARCHART, EarningsPerQuarterChartTab.class);
        createTab(folder, Images.VIEW_BARCHART, EarningsPerYearChartTab.class);
        createTab(folder, Images.VIEW_LINECHART, EarningsAccumulatedChartTab.class);
        createTab(folder, Images.VIEW_TABLE, TransactionsTab.class);

        int tab = preferences.getInt(KEY_TAB);
        if (tab < 0 || tab > 7)
            tab = 0;
        folder.setSelection(tab);
        folder.addDisposeListener(e -> preferences.setValue(KEY_TAB, folder.getSelectionIndex()));

        return folder;
    }

    private void createTab(CTabFolder folder, Images image, Class<? extends EarningsTab> tabClass)
    {
        EarningsTab tab = this.make(tabClass, model);
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
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
        pages.add(make(SecurityEventsPane.class));
    }
}
