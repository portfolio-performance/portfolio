package name.abuchen.portfolio.ui.views.dividends;

import java.time.LocalDate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractDropDown;

public class DividendsView extends AbstractFinanceView
{
    private static final String KEY_TAB = DividendsView.class.getSimpleName() + "-tab"; //$NON-NLS-1$
    private static final String KEY_YEAR = DividendsView.class.getSimpleName() + "-year"; //$NON-NLS-1$

    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private ExchangeRateProviderFactory factory;

    private DividendsViewModel model;

    private CTabFolder folder;

    @PostConstruct
    public void setupModel()
    {
        model = new DividendsViewModel(new CurrencyConverterImpl(factory, client.getBaseCurrency()), client);

        int year = preferences.getInt(KEY_YEAR);
        LocalDate now = LocalDate.now();
        if (year <= now.getYear() - 10 || year > now.getYear())
            year = now.getYear() - 2;

        model.updateWith(year);
        model.addUpdateListener(() -> preferences.setValue(KEY_YEAR, model.getStartYear()));
    }

    @Override
    public void notifyModelUpdated()
    {
        model.recalculate();
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelDividends;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        new StartYearSelectionDropDown(toolBar, model);

        new AbstractDropDown(toolBar, Messages.MenuExportData, Images.EXPORT.image(), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                final int itemCount = folder.getItemCount();
                for (int ii = 0; ii < itemCount; ii++)
                {
                    DividendsTab tab = (DividendsTab) folder.getItem(ii).getData();
                    if (tab != null)
                        tab.addExportActions(manager);
                }
            }
        };

        new AbstractDropDown(toolBar, Messages.MenuConfigureChart, Images.CONFIG.image(), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                Action action = new Action(Messages.LabelUseGrossDividends)
                {
                    @Override
                    public void run()
                    {
                        model.setUseGrossValue(!model.usesGrossValue());
                        model.recalculate();
                    }
                };
                action.setChecked(model.usesGrossValue());
                manager.add(action);
            }
        };
    }

    @Override
    protected Control createBody(Composite parent)
    {
        folder = new CTabFolder(parent, SWT.BORDER);

        createTab(folder, DividendsMatrixTab.class);
        createTab(folder, DividendsChartTab.class);
        createTab(folder, AccumulatedDividendsChartTab.class);
        createTab(folder, TransactionsTab.class);

        int tab = preferences.getInt(KEY_TAB);
        if (tab <= 0 || tab >= 4)
            tab = 0;
        folder.setSelection(tab);
        folder.addDisposeListener(e -> preferences.setValue(KEY_TAB, folder.getSelectionIndex()));

        return folder;
    }

    private void createTab(CTabFolder folder, Class<? extends DividendsTab> tabClass)
    {
        DividendsTab tab = this.make(tabClass, model);
        Control control = tab.createControl(folder);
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(tab.getLabel());
        item.setControl(control);
        item.setData(tab);
    }
}
