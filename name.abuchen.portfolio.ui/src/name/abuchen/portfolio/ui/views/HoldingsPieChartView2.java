package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.json.simple.JSONObject;

import com.ibm.icu.text.MessageFormat;

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
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityEventsPane;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;

public class HoldingsPieChartView2 extends AbstractFinanceView
{
    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    private CurrencyConverter converter;
    private ClientFilterDropDown clientFilter;
    private ClientSnapshot snapshot;

    private Chart chart;

    @PostConstruct
    protected void contruct(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

        clientFilter = new ClientFilterDropDown(getClient(), getPreferenceStore(),
                        HoldingsPieChartView2.class.getSimpleName(), filter -> notifyModelUpdated());

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

        updateChart();
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
        chart = new Chart(parent, SWT.NONE);

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.BOTTOM);

        updateChart();

        return chart;
    }

    private void updateChart()
    {
        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation().getAmount())) //
                        .forEach(p -> {
                            labels.add(JSONObject.escape(p.getDescription()));
                            values.add(p.getValuation().getAmount() / Values.Amount.divider());
                        });

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        Messages.LabelStatementOfAssetsHoldings);
        circularSeries.setSeries(labels.toArray(new String[0]), values.stream().mapToDouble(d -> d).toArray());

        JSColors wheel = new JSColors();
        Color[] colors = new Color[values.size()];
        for (int ii = 0; ii < colors.length; ii++)
            colors[ii] = wheel.next();
        circularSeries.setColor(colors);

        circularSeries.setBorderColor(Colors.WHITE);
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

    private static final class JSColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / (float) SIZE;

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public Color next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / (float) SIZE)));
            return Colors.getColor(new RGB((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness));

        }
    }
}
