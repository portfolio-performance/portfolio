package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.BarChart;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartShowYAxisConfig;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.TaxonomyConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyModel;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNode;
import name.abuchen.portfolio.util.TextUtil;

public class RebalancingChartWidget extends WidgetDelegate<TaxonomyModel>
{
    private BarChart chart;
    private Label title;

    public RebalancingChartWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ClientFilterConfig(this));
        addConfig(new TaxonomyConfig(this));
        addConfig(new IncludeUnassignedCategoryConfig(this, true));

        addConfig(new ChartShowYAxisConfig(this, true));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setBackground(container.getBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new BarChart(container, title.getText());

        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(chart);

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

            int index = 0;
            for (TaxonomyNode node : nodes)
            {
                categories[index] = node.getName();
                diffs[index] = node.getTarget().subtract(node.getActual()).getAmount() / Values.Amount.divider();
                actualValues[index] = node.getActual().getAmount() / Values.Amount.divider();
                targetValues[index] = node.getTarget().getAmount() / Values.Amount.divider();
                index++;
            }

            chart.setCategories(Arrays.asList(categories));

            chart.addSeries("actual", Messages.ColumnActualValue, actualValues, Colors.getColor(60, 151, 218), true); //$NON-NLS-1$
            chart.addSeries("target", Messages.ColumnTargetValue, targetValues, Colors.getColor(113, 173, 70), true); //$NON-NLS-1$
            chart.addSeries("diff", Messages.ColumnDeltaValue, diffs, Colors.theme().redForeground(), false); //$NON-NLS-1$

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
}
