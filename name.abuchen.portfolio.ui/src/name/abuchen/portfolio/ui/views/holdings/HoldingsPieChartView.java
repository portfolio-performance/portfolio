package name.abuchen.portfolio.ui.views.holdings;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.IPieChart;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;

public class HoldingsPieChartView extends AbstractFinanceView
{
    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    private CurrencyConverter converter;
    private IPieChart chart;
    private ClientFilterDropDown clientFilter;
    private ClientSnapshot snapshot;

    @Inject
    @Preference(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS)
    boolean useSWTCharts;

    @PostConstruct
    protected void construct(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        HoldingsPieChartView.class.getSimpleName(), filter -> notifyModelUpdated());

        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);
        snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    public void notifyModelUpdated()
    {
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        setToContext(UIConstants.Context.FILTERED_CLIENT, filteredClient);
        snapshot = ClientSnapshot.create(filteredClient, converter, LocalDate.now());

        chart.refresh(snapshot);
        
        updateWarningInToolBar();

        setInformationPaneInput(null);
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(clientFilter);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        if (this.useSWTCharts)
        {
            chart = new HoldingsPieChartSWT(snapshot, this);
        }
        else
        {
            chart = new HoldingsPieChartBrowser(make(EmbeddedBrowser.class), snapshot, this);
        }

        updateWarningInToolBar();
        return chart.createControl(parent);
    }

    private void updateWarningInToolBar()
    {
        boolean hasNegativePositions = snapshot.getAssetPositions().anyMatch(p -> p.getValuation().isNegative());

        ToolBarManager toolBar = getToolBarManager();

        if (hasNegativePositions)
        {
            if (toolBar.find(ID_WARNING_TOOL_ITEM) == null)
            {
                Action warning = new SimpleAction(Messages.HoldingsWarningAssetsWithNegativeValuation,
                                Images.ERROR_NOTICE.descriptor(), a -> {

                                    String details = String.join("\n", snapshot.getAssetPositions() //$NON-NLS-1$
                                                    .filter(p -> p.getValuation().isNegative())
                                                    .map(p -> p.getDescription() + " " //$NON-NLS-1$
                                                                    + Values.Money.format(p.getValuation()))
                                                    .collect(Collectors.toList()));

                                    MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                                    MessageFormat.format(
                                                                    Messages.HoldingsWarningAssetsWithNegativeValuationDetails,
                                                                    Values.Money.format(snapshot.getMonetaryAssets()),
                                                                    details));
                                });
                warning.setId(ID_WARNING_TOOL_ITEM);
                toolBar.insert(0, new ActionContributionItem(warning));
                toolBar.update(true);
            }
        }
        else
        {
            if (toolBar.remove(ID_WARNING_TOOL_ITEM) != null)
                toolBar.update(true);
        }
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
