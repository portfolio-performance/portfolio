package name.abuchen.portfolio.ui.views.holdings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;
import name.abuchen.portfolio.ui.views.IPieChart;

public class HoldingsPieChartSWT implements IPieChart
{
    private PieChart chart;
    private ClientSnapshot snapshot;
    private AbstractFinanceView financeView;
    private List<String> lastLabels;
    private Map<String, NodeData> nodeDataMap;

    private class NodeData
    {
        AssetPosition position;
        Double percentage;
        String percentageString;
        String shares;
        String valueSingle;
        String value;
    }

    public HoldingsPieChartSWT(ClientSnapshot snapshot, AbstractFinanceView view)
    {
        this.snapshot = snapshot;
        this.financeView = view;
        nodeDataMap = new HashMap<>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent, IPieChart.ChartType.DONUT, this::getNodeLabel);

        // set customized tooltip builder
        chart.getToolTip().setToolTipBuilder((container, currentNode) -> {
            RowLayout layout = new RowLayout(SWT.VERTICAL);
            layout.center = true;
            container.setLayout(layout);
            Composite data = new Composite(container, SWT.NONE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);
            Label assetLabel = new Label(data, SWT.NONE);
            FontDescriptor boldDescriptor = FontDescriptor.createFrom(assetLabel.getFont()).setStyle(SWT.BOLD);
            assetLabel.setFont(boldDescriptor.createFont(assetLabel.getDisplay()));
            assetLabel.setText(currentNode.getId());
            NodeData nodeData = nodeDataMap.get(currentNode.getId());
            if (nodeData != null)
            {
                Label right = new Label(data, SWT.NONE);
                right.setText("(" + nodeData.percentageString + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                Label info = new Label(container, SWT.NONE);
                info.setText(String.format("%s x %s = %s", //$NON-NLS-1$
                                nodeData.shares, nodeData.valueSingle, nodeData.value));
            }
        });

        // Listen on mouse clicks to update information pane
        ((Composite) chart.getPlotArea()).addListener(SWT.MouseUp, event -> {
            Node node = chart.getNodeAt(event.x, event.y);
            if (node == null)
                return;
            NodeData nodeData = nodeDataMap.get(node.getId());
            if (nodeData != null)
            {
                financeView.setInformationPaneInput(nodeData.position.getInvestmentVehicle());
            }
        });

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.RIGHT);

        updateChart();

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
        nodeDataMap.clear();

        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation().getAmount())) //
                        .forEach(p -> {
                            String nodeId = JSONObject.escape(p.getDescription());
                            labels.add(nodeId);
                            values.add(p.getValuation().getAmount() / Values.Amount.divider());
                            NodeData data = new NodeData();
                            data.position = p;
                            data.percentage = p.getShare();
                            data.percentageString = Values.Percent2.format(p.getShare());
                            data.shares = Values.Share.format(p.getPosition().getShares());
                            data.value = Values.Money.format(p.getValuation());
                            data.valueSingle = Values.Money
                                            .format(p.getValuation().multiply((long) Values.Share.divider())
                                                            .divide(p.getPosition().getShares()));
                            nodeDataMap.put(nodeId, data);
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
        circularSeries.setHighlightColor(Colors.GREEN);
        circularSeries.setBorderColor(Colors.WHITE);
        lastLabels = new ArrayList<>(labels);
        Collections.sort(lastLabels, String.CASE_INSENSITIVE_ORDER);
        return circularSeries;
    }

    private void setColors(ICircularSeries<?> circularSeries, int colorCount)
    {
        PieChart.PieColors wheel = new PieChart.PieColors();
        Color[] colors = new Color[colorCount];
        for (int ii = 0; ii < colors.length; ii++)
            colors[ii] = wheel.next();
        circularSeries.setColor(colors);
    }

    private String getNodeLabel(Node node)
    {
        NodeData nodeData = nodeDataMap.get(node.getId());
        if (nodeData != null)
            return nodeData.percentage > 0.025 ? nodeData.percentageString : null;
        else
            return null;
    }
}
