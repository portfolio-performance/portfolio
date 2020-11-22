package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.function.ToDoubleFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.IAxis;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;

import com.google.common.collect.Lists;

import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.ScatterChart;
import name.abuchen.portfolio.ui.util.chart.ScatterChartCSVExporter;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesChartLegend;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator;
import name.abuchen.portfolio.util.Interval;

public class ReturnsVolatilityChartView extends AbstractHistoricView
{
    private enum RiskMetric
    {
        VOLATILITY(Messages.LabelVolatility, index -> index.getVolatility().getStandardDeviation()), //
        SEMIVOLATILITY(Messages.LabelSemiVolatility, index -> index.getVolatility().getSemiDeviation());

        private String label;
        private ToDoubleFunction<PerformanceIndex> riskFunction;

        private RiskMetric(String label, ToDoubleFunction<PerformanceIndex> riskFunction)
        {
            this.label = label;
            this.riskFunction = riskFunction;
        }

        public double getRisk(PerformanceIndex index)
        {
            return riskFunction.applyAsDouble(index);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private static final String KEY_USE_IRR = ReturnsVolatilityChartView.class.getSimpleName() + "-use-irr"; //$NON-NLS-1$
    private static final String KEY_RISK_METRIC = ReturnsVolatilityChartView.class.getSimpleName() + "-risk-metric"; //$NON-NLS-1$

    private boolean useIRR = false;
    private RiskMetric riskMetric = RiskMetric.VOLATILITY;

    private ScatterChart chart;
    private LocalResourceManager resources;
    private DataSeriesConfigurator configurator;

    private DataSeriesCache cache;

    @PostConstruct
    public void construct()
    {
        this.useIRR = getPreferenceStore().getBoolean(KEY_USE_IRR);

        String riskMetricKey = getPreferenceStore().getString(KEY_RISK_METRIC);
        if (riskMetricKey != null && !riskMetricKey.isEmpty())
        {
            try
            {
                this.riskMetric = RiskMetric.valueOf(riskMetricKey);
            }
            catch (IllegalArgumentException e)
            {
                // unknown risk metric type; continue to use the default one
            }
        }
    }

    @PreDestroy
    public void destroy()
    {
        getPreferenceStore().setValue(KEY_USE_IRR, this.useIRR);
        getPreferenceStore().setValue(KEY_RISK_METRIC, riskMetric.name());
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelHistoricalReturnsAndVolatiltity;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        toolBar.add(new ExportDropDown());
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE, manager -> {

            manager.add(new LabelOnly(Messages.LabelPerformanceMetric));

            Action ttwror = new SimpleAction(Messages.ColumnTWROR, a -> {
                this.useIRR = false;

                IAxis yAxis = chart.getAxisSet().getYAxis(0);
                yAxis.getTitle().setText(Messages.LabelPerformanceTTWROR);

                reportingPeriodUpdated();
            });
            ttwror.setChecked(!this.useIRR);
            manager.add(ttwror);

            Action irr = new SimpleAction(Messages.ColumnIRR, a -> {
                this.useIRR = true;

                IAxis yAxis = chart.getAxisSet().getYAxis(0);
                yAxis.getTitle().setText(Messages.LabelPerformanceIRR);

                reportingPeriodUpdated();
            });
            irr.setChecked(this.useIRR);
            manager.add(irr);

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.LabelRiskMetric));

            for (RiskMetric metric : RiskMetric.values())
            {
                Action action = new SimpleAction(metric.toString(), a -> {
                    this.riskMetric = metric;

                    IAxis yAxis = chart.getAxisSet().getXAxis(0);
                    yAxis.getTitle().setText(metric.toString());

                    reportingPeriodUpdated();
                });
                action.setChecked(this.riskMetric == metric);
                manager.add(action);
            }

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.LabelDataSeries));
            configurator.configMenuAboutToShow(manager);
        }));
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        cache = make(DataSeriesCache.class);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        resources = new LocalResourceManager(JFaceResources.getResources(), composite);

        chart = new ScatterChart(composite);
        chart.getTitle().setVisible(false);

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(this.riskMetric.toString());
        xAxis.getTick().setFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setText(useIRR ? Messages.LabelPerformanceIRR : Messages.LabelPerformanceTTWROR);
        yAxis.getTick().setFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        configurator = new DataSeriesConfigurator(this, DataSeries.UseCase.RETURN_VOLATILITY);
        configurator.addListener(this::updateChart);
        configurator.setToolBarManager(getViewToolBarManager());

        DataSeriesChartLegend legend = new DataSeriesChartLegend(composite, configurator);

        updateTitle(Messages.LabelHistoricalReturnsAndVolatiltity + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        chart.getTitle().setText(getTitle());

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(legend);

        setChartSeries();

        return composite;
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
        cache.clear();
        updateChart();
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelHistoricalReturnsAndVolatiltity + " (" + configurator.getConfigurationName() //$NON-NLS-1$
                            + ")"); //$NON-NLS-1$

            chart.suspendUpdate(true);
            chart.getTitle().setText(getTitle());
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            setChartSeries();

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }

    private void setChartSeries()
    {
        Interval interval = getReportingPeriod().toInterval(LocalDate.now());

        Lists.reverse(configurator.getSelectedDataSeries()).forEach(series -> {
            PerformanceIndex index = cache.lookup(series, interval);

            double risk = this.riskMetric.getRisk(index);
            double retrn = this.useIRR ? index.getPerformanceIRR() : index.getFinalAccumulatedPercentage();

            ILineSeries lineSeries = chart.addScatterSeries(new double[] { risk }, new double[] { retrn },
                            series.getLabel());

            Color color = resources.createColor(series.getColor());
            lineSeries.setLineColor(color);
            lineSeries.setSymbolColor(color);
            lineSeries.enableArea(series.isShowArea());
            lineSeries.setLineStyle(series.getLineStyle());
        });
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
            manager.add(new SimpleAction(Messages.MenuExportChartData, a -> {
                ScatterChartCSVExporter exporter = new ScatterChartCSVExporter(chart);
                exporter.setValueFormat(new DecimalFormat("0.##########%")); //$NON-NLS-1$
                exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
            }));

            for (DataSeries series : configurator.getSelectedDataSeries())
                manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, series.getLabel()),
                                a -> exportDataSeries(series)));

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, getTitle());
        }

        private void exportDataSeries(DataSeries series)
        {
            AbstractCSVExporter exporter = new AbstractCSVExporter()
            {
                @Override
                protected void writeToFile(File file) throws IOException
                {
                    PerformanceIndex index = cache.lookup(series, getReportingPeriod().toInterval(LocalDate.now()));
                    index.exportVolatilityData(file);
                }

                @Override
                protected Shell getShell()
                {
                    return chart.getShell();
                }
            };
            exporter.export(getTitle() + "_" + series.getLabel() + ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
