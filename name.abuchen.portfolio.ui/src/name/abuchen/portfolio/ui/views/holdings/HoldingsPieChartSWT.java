package name.abuchen.portfolio.ui.views.holdings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsOutsidePie;
import name.abuchen.portfolio.ui.views.IPieChart;

public class HoldingsPieChartSWT implements IPieChart
{
    private CircularChart chart;
    private ClientSnapshot snapshot;
    private AbstractFinanceView financeView;
    private List<String> lastLabels;
    private Map<String, NodeData> id2nodeData;

    private class NodeData
    {
        AssetPosition position;
        String percentageString;
        String shares;
        String valueSingle;
        String value;
    }

    public HoldingsPieChartSWT(ClientSnapshot snapshot, AbstractFinanceView view)
    {
        this.snapshot = snapshot;
        this.financeView = view;
        id2nodeData = new HashMap<>();
    }

    public HoldingsPieChartSWT()
    {
        id2nodeData = new HashMap<>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new CircularChart(parent, SeriesType.DOUGHNUT, this::getNodeLabel);
        chart.addLabelPainter(new RenderLabelsCenteredInPie(chart, this::getNodeLabel));
        chart.addLabelPainter(
                        new RenderLabelsOutsidePie(chart, n -> id2nodeData.get(n.getId()).position.getDescription()));

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder((container, currentNode) -> {

            final Composite data = new Composite(container, SWT.NONE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);

            NodeData nodeData = id2nodeData.get(currentNode.getId());

            if (nodeData == null) // center of the Pie Chart
            {
                Label assetLabel = new Label(data, SWT.NONE);
                GridDataFactory.fillDefaults().span(2, 1).applyTo(assetLabel);
                assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                if (financeView != null) // from view : Statement of Assets
                {
                    assetLabel.setText(currentNode.getId());
                }
                else // from pane = single portfolio information pane
                {
                    assetLabel.setText(snapshot.getPortfolios().get(0).getPortfolio().getName());
                }

                Label info = new Label(data, SWT.NONE);
                GridDataFactory.fillDefaults().span(2, 1).applyTo(info);
                info.setText(Values.Money.format(snapshot.getMonetaryAssets()));

            }
            else
            {
                Label assetLabel = new Label(data, SWT.NONE);
                assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                assetLabel.setText(nodeData.position.getDescription());

                Label right = new Label(data, SWT.NONE);
                right.setText(nodeData.percentageString);

                Label info = new Label(data, SWT.NONE);
                GridDataFactory.fillDefaults().span(2, 1).applyTo(info);
                info.setText(String.format("%s x %s = %s", //$NON-NLS-1$
                                nodeData.shares, nodeData.valueSingle, nodeData.value));
            }
        });

        // Listen on mouse clicks to update information pane
        if (financeView != null)
        {
            ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp,
                            event -> chart.getNodeAt(event.x, event.y).ifPresent(node -> {
                                NodeData nodeData = id2nodeData.get(node.getId());
                                if (nodeData != null)
                                    financeView.setInformationPaneInput(nodeData.position.getInvestmentVehicle());
                            }));
        }

        if (snapshot != null)
        {
            updateChart();
        }

        return chart;
    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        this.snapshot = snapshot;
        updateChart();
    }

    private void updateChart()
    {
        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        id2nodeData.clear();

        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation().getAmount())) //
                        .forEach(p -> {
                            String nodeId = p.getInvestmentVehicle().getUUID();
                            labels.add(nodeId);
                            values.add(p.getValuation().getAmount() / Values.Amount.divider());

                            NodeData data = new NodeData();
                            data.position = p;
                            data.percentageString = Values.Percent2.format(p.getShare());
                            data.shares = Values.Share.format(p.getPosition().getShares());
                            data.value = Values.Money.format(p.getValuation());
                            data.valueSingle = Values.Money
                                            .format(p.getValuation().multiply((long) Values.Share.divider())
                                                            .divide(p.getPosition().getShares()));
                            id2nodeData.put(nodeId, data);
                        });

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet()
                        .getSeries(Messages.LabelStatementOfAssetsHoldings);
        if (circularSeries == null)
        {
            circularSeries = createSeries(values, labels);
        }
        else
        {
            if (hasDataSetChanged(labels))
            {
                // refresh values only for smoother update
                ListIterator<String> iter = labels.listIterator();
                while (iter.hasNext())
                {
                    int idx = iter.nextIndex();
                    String label = iter.next();
                    Node node = circularSeries.getNodeById(label);
                    if (node != null)
                    {
                        node.setValue(values.get(idx));
                    }
                }
            }
            else
            {
                circularSeries = createSeries(values, labels);
            }

        }
        setColors(circularSeries, values.size());
        chart.updateAngleBounds();
        chart.redraw();
    }

    /**
     * Check whether or not the same labels in dataset
     */
    private boolean hasDataSetChanged(List<String> labels)
    {
        List<String> newLabels = new ArrayList<>(labels);
        Collections.sort(newLabels, String.CASE_INSENSITIVE_ORDER);
        return newLabels.equals(lastLabels);
    }

    private ICircularSeries<?> createSeries(List<Double> values, List<String> labels)
    {
        ICircularSeries<?> circularSeries;
        circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        Messages.LabelStatementOfAssetsHoldings);
        circularSeries.setSeries(labels.toArray(new String[0]), values.stream().mapToDouble(d -> d).toArray());
        circularSeries.setSliceColor(chart.getPlotArea().getBackground());
        lastLabels = new ArrayList<>(labels);
        Collections.sort(lastLabels, String.CASE_INSENSITIVE_ORDER);
        return circularSeries;
    }

    private void setColors(ICircularSeries<?> circularSeries, int colorCount)
    {
        CircularChart.PieColors wheel = new CircularChart.PieColors();
        Color[] colors = new Color[colorCount];
        for (int ii = 0; ii < colors.length; ii++)
            colors[ii] = wheel.next();
        circularSeries.setColor(colors);
    }

    private String getNodeLabel(Node node)
    {
        NodeData nodeData = id2nodeData.get(node.getId());
        return nodeData != null ? nodeData.percentageString : null;
    }
}
