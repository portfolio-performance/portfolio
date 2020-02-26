package name.abuchen.portfolio.ui.views.trades;

import java.time.LocalDate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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

public class TradeView extends AbstractFinanceView
{
    private static final String KEY_TAB = TradeView.class.getSimpleName() + "-tab"; //$NON-NLS-1$
    private static final String KEY_YEAR = TradeView.class.getSimpleName() + "-year"; //$NON-NLS-1$

    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private ExchangeRateProviderFactory factory;

    private TradeViewModel model;

    private CTabFolder folder;

    @PostConstruct
    public void setupModel()
    {
        CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        model = new TradeViewModel(preferences, converter, client);

        int year = preferences.getInt(KEY_YEAR);
        LocalDate now = LocalDate.now();
        if (year < 1900 || year > now.getYear())
            year = now.getYear() - 2;

        model.configure(year);

        model.addUpdateListener(() -> {
            preferences.setValue(KEY_YEAR, model.getStartYear());
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
        return Messages.LabelTrades;
    }

    @Override
    protected void addViewButtons(ToolBarManager toolBarManager)
    {
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
                TradeTab tab = (TradeTab) folder.getItem(ii).getData();
                if (tab != null)
                    tab.addExportActions(manager);
            }
        }));

    }

    @Override
    protected Control createBody(Composite parent)
    {
        folder = new CTabFolder(parent, SWT.BORDER);

        createTab(folder, Images.VIEW_TABLE, TradePerMonthMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, TradePerQuarterMatrixTab.class);
        createTab(folder, Images.VIEW_TABLE, TradePerYearMatrixTab.class);
        createTab(folder, Images.VIEW_BARCHART, TradePerMonthChartTab.class);
        createTab(folder, Images.VIEW_BARCHART, TradePerQuarterChartTab.class);
        createTab(folder, Images.VIEW_BARCHART, TradePerYearChartTab.class);
        createTab(folder, Images.VIEW_TABLE, TradesClosedTab.class);
        createTab(folder, Images.VIEW_TABLE, TradesOpenTab.class);

        int tab = preferences.getInt(KEY_TAB);
        if (tab < 0 || tab > 7)
            tab = 0;
        folder.setSelection(tab);
        folder.addDisposeListener(e -> preferences.setValue(KEY_TAB, folder.getSelectionIndex()));

        return folder;
    }

    private void createTab(CTabFolder folder, Images image, Class<? extends TradeTab> tabClass)
    {
        TradeTab tab = this.make(tabClass, model);
        Control control = tab.createControl(folder);
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(tab.getLabel());
        item.setControl(control);
        item.setData(tab);
        item.setImage(image.image());
    }
}
