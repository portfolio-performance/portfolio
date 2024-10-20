package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.CircularChart;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsCenteredInPie;
import name.abuchen.portfolio.ui.util.chart.CircularChart.RenderLabelsOutsidePie;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.Type;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.UseCase;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;
import name.abuchen.portfolio.util.Interval;

public class AbsolutePerformancePieChart implements IPieChart
{
    private CircularChart chart;
    private ClientSnapshot snapshot;
    private Map<String, NodeData> id2nodeData;
    private Client client;
    private IPreferenceStore preferences;
    private DataSeriesCache cache;
    private String position;

    private class NodeData
    {
        String percentageString;
        String value;
        String label;
    }

    private class InvestedWrapper
    {
        long invested = 0;
        long delta = 0;
        int counter = 0;
    }


    public AbsolutePerformancePieChart(Client client, IPreferenceStore preferences, DataSeriesCache cache)
    {
        this.client = client;
        this.preferences = preferences;
        this.cache = cache;

        id2nodeData = new HashMap<>();
    }

    @Override
    public Control createControl(Composite parent)
    {
        chart = new CircularChart(parent, SeriesType.DOUGHNUT, this::getNodeLabel);
        chart.addLabelPainter(new RenderLabelsCenteredInPie(chart, this::getNodeLabel));
        chart.addLabelPainter(
                        new RenderLabelsOutsidePie(chart, n -> id2nodeData.get(n.getId()).label));

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

                if (position != null)
                {
                    assetLabel.setText(position);
                }
                else
                {
                    assetLabel.setText(snapshot.getJointPortfolio().getPortfolio().getName()); // $NON-NLS-1$
                }

                Label info = new Label(data, SWT.NONE);
                GridDataFactory.fillDefaults().span(2, 1).applyTo(info);
                info.setText(Values.Money.format(snapshot.getMonetaryAssets()));
            }
            else
            {
                Label assetLabel = new Label(data, SWT.NONE);
                assetLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
                assetLabel.setText(nodeData.label);

                Label right = new Label(data, SWT.NONE);
                right.setText(nodeData.percentageString);

                Label info = new Label(data, SWT.NONE);
                GridDataFactory.fillDefaults().span(2, 1).applyTo(info);
                info.setText(nodeData.value);
            }
        });

        chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT, Messages.ColumnAbsolutePerformance_MenuLabel);

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
        id2nodeData.clear();

        position = null;

        var series = chart.getSeriesSet().getSeries();
        for (var s : series)
            chart.getSeriesSet().deleteSeries(s.getId());
        
        chart.redraw();

        if (snapshot == null)
            return;

        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        var wrapper = new InvestedWrapper();
        var dataSeriesSet = new DataSeriesSet(client, preferences, UseCase.STATEMENT_OF_ASSETS);

        countAssets(wrapper, dataSeriesSet);

        countAccounts(wrapper, dataSeriesSet);

        if (wrapper.invested <= 0)
            return;
        
        if (wrapper.counter > 1)
        {
            position = null;
        }

        wrapper.delta = Math.max(0, wrapper.delta);

        var total = wrapper.invested + wrapper.delta;


        values.add(wrapper.invested / Values.Amount.divider());
        labels.add(Messages.LabelAbsoluteInvestedCapital);

        NodeData data = new NodeData();
        data.label = Messages.LabelAbsoluteInvestedCapital;
        data.percentageString = Values.Percent2.format((wrapper.invested / (double) total));
        data.value = Values.Money.format(Money.of(snapshot.getCurrencyCode(), wrapper.invested));
        id2nodeData.put(Messages.LabelAbsoluteInvestedCapital, data);


        values.add(wrapper.delta / Values.Amount.divider());
        labels.add(Messages.LabelAbsoluteDelta);

        data = new NodeData();
        data.label = Messages.LabelAbsoluteDelta;
        data.percentageString = Values.Percent2.format((wrapper.delta / (double) total));
        data.value = Values.Money.format(Money.of(snapshot.getCurrencyCode(), wrapper.delta));
        id2nodeData.put(Messages.LabelAbsoluteDelta, data);


        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet()
                        .getSeries(Messages.ColumnAbsolutePerformance_MenuLabel);
        if (circularSeries == null)
        {
            circularSeries = createSeries(values, labels);
        }
        else
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

        setColors(circularSeries, values.size(), wrapper.delta > 0);

        chart.updateAngleBounds();
        chart.redraw();
    }

    private void countAssets(InvestedWrapper wrapper, DataSeriesSet dataSeriesSet)
    {
        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .forEach(p -> {
                            String nodeId = p.getInvestmentVehicle().getUUID();
                            
                            var dataSeries = dataSeriesSet.getAvailableSeries().stream()
                                            .filter(ds -> ds.getType() != Type.ACCOUNT && ds.getUUID().contains(nodeId))
                                            .findAny().orElse(null);
    
                            if (dataSeries != null)
                            {
                                var perf = cache.lookup(dataSeries,
                                                Interval.of(LocalDate.ofYearDay(1900, 1), LocalDate.now()));
    
                                long[] d = perf.calculateAbsoluteInvestedCapital();
                                wrapper.invested += d.length > 0 ? d[d.length - 1] : 0L;
    
                                long[] d2 = perf.calculateAbsoluteDelta();
                                wrapper.delta += d2.length > 0 ? d2[d2.length - 1] : 0L;
    
                                position = p.getDescription();
                                wrapper.counter++;
                            }
                        });
    }

    private void countAccounts(InvestedWrapper wrapper, DataSeriesSet dataSeriesSet)
    {
        snapshot.getAccounts().stream() //
                        .filter(p -> p.getAccount().getCurrentAmount(LocalDateTime.now()) > 0) //
                        .forEach(p -> {
                            Account account = p.getAccount();

                            var dataSeries = dataSeriesSet.getAvailableSeries().stream()
                                            .filter(ds -> ds.getType() == Type.ACCOUNT
                                                            && ds.getLabel().contains(account.getName()))
                                            .findAny().orElse(null);

                            if (dataSeries != null)
                            {
                                var perf = cache.lookup(dataSeries,
                                                Interval.of(LocalDate.ofYearDay(1900, 1), LocalDate.now()));

                                long[] d = perf.calculateAbsoluteInvestedCapital();
                                wrapper.invested += d.length > 0 ? d[d.length - 1] : 0L;

                                long[] d2 = perf.calculateAbsoluteDelta();
                                wrapper.delta += d2.length > 0 ? d2[d2.length - 1] : 0L;

                                position = account.getName();
                                wrapper.counter++;
                            }
                        });
    }

    private ICircularSeries<?> createSeries(List<Double> values, List<String> labels)
    {
        ICircularSeries<?> circularSeries;
        circularSeries = (ICircularSeries<?>) chart.getSeriesSet().createSeries(SeriesType.DOUGHNUT,
                        Messages.ColumnAbsolutePerformance_MenuLabel);
        circularSeries.setSeries(labels.toArray(String[]::new), values.stream().mapToDouble(d -> d).toArray());
        circularSeries.setSliceColor(Colors.WHITE);

        return circularSeries;
    }

    private void setColors(ICircularSeries<?> circularSeries, int colorCount, boolean isPositive)
    {
        CircularChart.PieColors wheel = new CircularChart.PieColors();
        Color[] colors = new Color[colorCount];

        if (isPositive)
        {
            for (int ii = 0; ii < colors.length; ii++)
            {
                colors[ii] = wheel.next();

                for (int j = 0; j < 6; j++)
                {
                    wheel.next();
                }
            }
        }
        else
        {
            for (int j = 0; j < 3; j++)
            {
                wheel.next();
            }

            for (int ii = 0; ii < colors.length; ii++)
            {
                colors[ii] = wheel.next();
            }
        }

        circularSeries.setColor(colors);
    }

    private String getNodeLabel(Node node)
    {
        NodeData nodeData = id2nodeData.get(node.getId());
        return nodeData != null ? nodeData.percentageString : null;
    }
}
