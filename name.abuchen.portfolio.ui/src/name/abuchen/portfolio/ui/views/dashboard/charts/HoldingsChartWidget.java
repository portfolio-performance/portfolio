package name.abuchen.portfolio.ui.views.dashboard.charts;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChartToolTip;
import name.abuchen.portfolio.ui.views.dashboard.ClientFilterConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;

public class HoldingsChartWidget extends CircularChartWidget<ClientSnapshot>
{

    public HoldingsChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    @Override
    protected void configureTooltip(CircularChartToolTip toolTip)
    {
        // set customized tooltip builder
        toolTip.setToolTipBuilder((composite, currentNode) -> {

            final Composite data = new Composite(composite, SWT.NONE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

            AssetPosition position = (AssetPosition) currentNode.getData();
            Label assetLabel = new Label(data, SWT.NONE);
            assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            assetLabel.setText(position != null ? position.getDescription() : Messages.ClientEditorLabelHoldings);

            if (position != null)
            {
                Label right = new Label(data, SWT.NONE);
                right.setText(Values.Percent2.format(currentNode.getValue() / currentNode.getParent().getValue()));
            }
        });
    }

    @Override
    public Supplier<ClientSnapshot> getUpdateTask()
    {
        return () -> {
            ClientFilter clientFilter = get(ClientFilterConfig.class).getSelectedFilter();
            return ClientSnapshot.create(clientFilter.filter(getClient()), getDashboardData().getCurrencyConverter(),
                            LocalDate.now());
        };
    }

    @Override
    protected void createCircularSeries(ClientSnapshot snapshot)
    {
        Map<String, Color> id2color = new HashMap<>();

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) getChart().getSeriesSet()
                        .createSeries(SeriesType.DOUGHNUT, Messages.ClientEditorLabelHoldings);
        circularSeries.setSliceColor(getChart().getPlotArea().getBackground());
        Node rootNode = circularSeries.getRootNode();

        CircularChart.PieColors colorWheel = new CircularChart.PieColors();

        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation().getAmount())) //
                        .forEach(p -> {
                            String nodeId = p.getInvestmentVehicle().getUUID();
                            Node node = rootNode.addChild(nodeId,
                                            p.getValuation().getAmount() / Values.Amount.divider());
                            node.setData(p);
                            id2color.put(nodeId, colorWheel.next());
                        });

        // no assets found; set an error message
        if (id2color.isEmpty())
        {
            circularSeries.setSeries(new String[] { Messages.LabelErrorNoHoldings }, new double[] { 100 });
            circularSeries.setColor(Messages.LabelErrorNoHoldings, Colors.LIGHT_GRAY);
        }

        id2color.forEach(circularSeries::setColor);
    }

}
