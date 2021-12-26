package name.abuchen.portfolio.ui.views.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.model.Node;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.PieChart;

public class HoldingsSWTPieChart implements IPieChart
{
    private Chart chart;
    private ClientSnapshot snapshot;
    private List<String> lastLabels;

    public HoldingsSWTPieChart(ClientSnapshot snapshot, AbstractFinanceView view)
    {
        this.snapshot = snapshot;
    }
    
    @Override
    public Control createControl(Composite parent)
    {
        chart = new PieChart(parent);

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

        snapshot.getAssetPositions() //
                        .filter(p -> p.getValuation().getAmount() > 0) //
                        .sorted((l, r) -> Long.compare(r.getValuation().getAmount(), l.getValuation().getAmount())) //
                        .forEach(p -> {
                            labels.add(JSONObject.escape(p.getDescription()));
                            values.add(p.getValuation().getAmount() / Values.Amount.divider());
                        });

        ICircularSeries<?> circularSeries = (ICircularSeries<?>) chart.getSeriesSet().getSeries(Messages.LabelStatementOfAssetsHoldings);
        if (circularSeries == null) {
            circularSeries = createSeries(values, labels);
        }
        else {
            if (hasDataSetChanged(labels)) {
                // refresh values only for smoother update
                ListIterator<String> iter = labels.listIterator();
                while(iter.hasNext()) {
                    int idx = iter.nextIndex();
                    String label = iter.next();
                    Node node = circularSeries.getNodeById(label);
                    if (node != null) {
                        node.setValue(values.get(idx));
                    }
                }
            }
            else {
                circularSeries = createSeries(values, labels);
            }

        }
        setColors(circularSeries, values.size());
        chart.redraw();
    }

    /**
     * Check whether or not the same labels in dataset
     * @param labels
     * @return
     */
    private boolean hasDataSetChanged(List<String> labels)
    {
        List<String> newLabels = new ArrayList<String>(labels);
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
        lastLabels = new ArrayList<String>(labels);
        Collections.sort(lastLabels, String.CASE_INSENSITIVE_ORDER);
        return circularSeries;
    }

    private void setColors(ICircularSeries<?> circularSeries, int colorCount)
    {
        PieColors wheel = new PieColors();
        Color[] colors = new Color[colorCount];
        for (int ii = 0; ii < colors.length; ii++)
            colors[ii] = wheel.next();
        circularSeries.setColor(colors);
    }

    private static final class PieColors
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
