package name.abuchen.portfolio.ui.views.charts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.ColorConversion;

public class HoldingsSWTPieChart implements IPieChart
{
    private Chart chart;
    private ClientSnapshot snapshot;

    public HoldingsSWTPieChart(ClientSnapshot snapshot, AbstractFinanceView view)
    {
        this.snapshot = snapshot;
    }
    
    @Override
    public Control createControl(Composite parent)
    {
        chart = new Chart(parent, SWT.NONE);

        chart.getTitle().setVisible(false);
        chart.getLegend().setPosition(SWT.BOTTOM);

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
