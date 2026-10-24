package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.views.AbstractHistoricView;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;
import name.abuchen.portfolio.snapshot.IRRSeries;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.util.ColorConversion;

public class IRRChartView extends AbstractHistoricView
{
    private TimelineChart chart;

    @Override
    protected String getDefaultTitle()
    {
        return "IRR";
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Colors.theme().defaultBackground());

        chart = new TimelineChart(composite);
        chart.getTitle().setText("IRR");
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new AxisTickPercentNumberFormat("0.#%")); //$NON-NLS-1$
        chart.getToolTip().setDefaultValueFormat(new DecimalFormat("0.##%"));
        chart.getToolTip().reverseLabels(true);

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

        updateChart();
        return composite;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        toolBar.add(new ExportDropDown());
    }

    @Override
    public void setFocus()
    {
        chart.adjustRange();
        chart.setFocus();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        notifyModelUpdated();
    }

    @Override
    public void notifyModelUpdated()
    {
        updateChart();
    }

    private void updateChart()
    {
        chart.suspendUpdate(true);
        try
        {
            chart.getTitle().setText("IRR");
            for (var s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            chart.clearHorizontalMarkerLines();

            ExchangeRateProviderFactory factory = make(ExchangeRateProviderFactory.class);
            CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
            java.util.List<Exception> warnings = new java.util.ArrayList<>();
            PerformanceIndex clientIndex = PerformanceIndex.forClient(getClient(), converter, getReportingPeriod().toInterval(LocalDate.now()), warnings);

            var irrValues = IRRSeries.calculate(clientIndex);
            var allDates = clientIndex.getDates();

            double maxTotal = 0.1;
            double minTotal = -0.1;
            double[] totals = clientIndex.getAccumulatedPercentage();
            if (totals != null) {
                for (double v : totals) {
                    if (Double.isFinite(v)) {
                        if (v > maxTotal) maxTotal = v;
                        if (v < minTotal) minTotal = v;
                    }
                }
            }

            double capMax = Math.max(0.2, maxTotal * 1.5);
            double capMin = Math.min(-0.2, minTotal * 1.5);

            int validCount = 0;
            for (double v : irrValues) {
                if (Double.isFinite(v)) validCount++;
            }

            LocalDate[] validDates = new LocalDate[validCount];
            double[] validValues = new double[validCount];
            int idx = 0;
            for (int i = 0; i < irrValues.length; i++) {
                if (Double.isFinite(irrValues[i])) {
                    validDates[idx] = allDates[i];
                    validValues[idx] = Math.max(capMin, Math.min(capMax, irrValues[i]));
                    idx++;
                }
            }

            if (validCount > 0) {
                var irrLineSeries = chart.addDateSeries("irr-main-series",
                                validDates, validValues, Messages.LabelIRR);
                irrLineSeries.setLineColor(Colors.getColor(ColorConversion.hex2RGB("#034EFF")));
                irrLineSeries.setLineWidth(2);

                double[] sortedValues = Arrays.copyOf(validValues, validCount);
                Arrays.sort(sortedValues);

                double minV = sortedValues[0];
                double p5 = sortedValues[Math.max(0, (int)(validCount * 0.05))];
                double median = sortedValues[Math.max(0, (int)(validCount * 0.50))];
                double p95 = sortedValues[Math.min(validCount - 1, (int)(validCount * 0.95))];
                double maxV = sortedValues[validCount - 1];

                var colorMin = Colors.getColor(ColorConversion.hex2RGB("#D90429"));
                var colorP5 = Colors.getColor(ColorConversion.hex2RGB("#F27438"));
                var colorMed = Colors.getColor(ColorConversion.hex2RGB("#E6AF2E"));
                var colorP95 = Colors.getColor(ColorConversion.hex2RGB("#8CD373"));
                var colorMax = Colors.getColor(ColorConversion.hex2RGB("#2B9348"));

                chart.addHorizontalMarkerLine(minV, colorMin, String.format("Mínimo (%.1f%%)", minV * 100));
                chart.addHorizontalMarkerLine(p5, colorP5, String.format("Percentil 5%% (%.1f%%)", p5 * 100));
                chart.addHorizontalMarkerLine(median, colorMed, String.format("Mediana (%.1f%%)", median * 100));
                chart.addHorizontalMarkerLine(p95, colorP95, String.format("Percentil 95%% (%.1f%%)", p95 * 100));
                chart.addHorizontalMarkerLine(maxV, colorMax, String.format("Máximo (%.1f%%)", maxV * 100));
            }

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private final class ExportDropDown extends DropDown implements IMenuListener
    {
        private ExportDropDown()
        {
            super(Messages.MenuExportData, Images.EXPORT, SWT.NONE);
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(Messages.MenuExportChartData)
            {
                @Override
                public void run()
                {
                    TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                    exporter.setValueFormat(new DecimalFormat("0.##########")); //$NON-NLS-1$
                    exporter.export("IRR.csv"); //$NON-NLS-1$
                }
            });

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, "IRR");
        }
    }
}
