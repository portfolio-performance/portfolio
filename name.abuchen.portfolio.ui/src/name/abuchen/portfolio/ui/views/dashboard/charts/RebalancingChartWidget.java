package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.util.List;
import name.abuchen.portfolio.ui.views.dashboard.TaxonomyConfig;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Client;

import java.util.function.Supplier;
import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.util.chart.StackedChart;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.DataSeriesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.money.Values;

public class RebalancingChartWidget extends WidgetDelegate<TaxonomyModel>
{
    private StackedChart chart;
    private Label title;
    private static final Color COLOR_ACTUAL = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    private static final Color COLOR_TARGET = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
    private static final Color COLOR_DIFF = Display.getDefault().getSystemColor(SWT.COLOR_RED);

    public RebalancingChartWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, true));
        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ChartShowYAxisConfig(this, true));
        addConfig(new ChartHeightConfig(this));
        addConfig(new ClientFilterConfig(this));
        addConfig(new ChartHeightConfig(this));
        addConfigAfter(ReportingPeriodConfig.class, new TaxonomyConfig(this));
        addConfigAfter(ClientFilterConfig.class, new IncludeUnassignedCategoryConfig(this, true));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new StackedChart(container, title.getText());
        
        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).span(2, 1).applyTo(chart);

        getDashboardData().getStylingEngine().style(chart);

        container.layout();

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<TaxonomyModel> getUpdateTask()
    {
        return () -> {
            Taxonomy taxonomy = get(TaxonomyConfig.class).getTaxonomy();

            if (taxonomy != null)
            {
                TaxonomyModel model = new TaxonomyModel(getDashboardData().getExchangeRateProviderFactory(),
                                getClient(), taxonomy);

                // apply filter if applicable
                Client filteredClient = get(ClientFilterConfig.class).getSelectedFilter().filter(getClient());
                if (filteredClient != getClient())
                    model.updateClientSnapshot(filteredClient);

                model.setExcludeUnassignedCategoryInCharts(
                                !get(IncludeUnassignedCategoryConfig.class).isUnassignedCategoryIncluded());

                return model;
            }
            else
            {
                return null;
            }
        };
    }

    @Override
    public void update(TaxonomyModel model)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        try
        {
            chart.suspendUpdate(true);

            get(ChartHeightConfig.class).updateGridData(chart, title.getParent());

            chart.getTitle().setText(title.getText());

            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());


            if (model == null)
                return;
            
            TaxonomyNode root = model.getClassificationRootNode();
            List<TaxonomyNode> nodes = root.getChildren();
            double[] actualValues = new double[nodes.size()];
            double[] targetValues = new double[nodes.size()];
            double[] diffs = new double[nodes.size()];
            String[] categories = new String[nodes.size()];

            PortfolioLog.error(String.format("Amount of nodes: %s", nodes.size()));

            int index = 0;
            for (TaxonomyNode node : nodes)
            {
                categories[index] = node.getName();
                diffs[index] = node.getTarget().subtract(node.getActual()).getAmount() / Values.Amount.divider();
                actualValues[index] = node.getActual().isZero() ? 0 : node.getActual().getAmount() / Values.Amount.divider();
                targetValues[index] = node.getTarget().getAmount() / Values.Amount.divider();
                index++;
            }

            PortfolioLog.error(String.format("Actual values: %s, Target values: %s, Categories: %s", Arrays.toString(actualValues), Arrays.toString(targetValues), Arrays.toString(categories)));

            chart.setCategories(Arrays.asList(categories));

            chart.addSeries("diff", "Diff", diffs, COLOR_DIFF, false);
            chart.addSeries("actual", "Actual", actualValues, COLOR_ACTUAL, true);
            chart.addSeries("target", "Target", targetValues, COLOR_TARGET, true);

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
} 