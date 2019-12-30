package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import javax.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.ISeries;

import com.google.common.collect.Lists;

import name.abuchen.portfolio.snapshot.Aggregation;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesChartLegend;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator;
import name.abuchen.portfolio.ui.views.dataseries.PerformanceChartSeriesBuilder;
import name.abuchen.portfolio.util.Interval;

public class PerformanceChartView extends AbstractHistoricView
{
    private static final String KEY_AGGREGATION_PERIOD = "performance-chart-aggregation-period"; //$NON-NLS-1$

    private TimelineChart chart;
    private DataSeriesConfigurator picker;

    private Aggregation.Period aggregationPeriod;

    private PerformanceChartSeriesBuilder seriesBuilder;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPerformanceChart;
    }

    @PostConstruct
    public void setup()
    {
        String key = getPreferenceStore().getString(KEY_AGGREGATION_PERIOD);
        if (key != null && key.length() > 0)
        {
            try
            {
                aggregationPeriod = Aggregation.Period.valueOf(key);
            }
            catch (IllegalArgumentException ignore)
            {
                // ignore if key is not a valid aggregation period
                PortfolioPlugin.log(ignore);
            }
        }
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        toolBar.add(new AggregationPeriodDropDown());
        toolBar.add(new ExportDropDown());
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE,
                        manager -> picker.configMenuAboutToShow(manager)));
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        chart = new TimelineChart(composite);
        chart.getTitle().setText(getTitle());
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getYAxis(0).getTick().setFormat(new DecimalFormat("0.#%")); //$NON-NLS-1$
        chart.getToolTip().setValueFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$
        chart.getToolTip().reverseLabels(true);

        DataSeriesCache cache = make(DataSeriesCache.class);
        seriesBuilder = new PerformanceChartSeriesBuilder(chart, cache);

        picker = new DataSeriesConfigurator(this, DataSeries.UseCase.PERFORMANCE);
        picker.addListener(this::updateChart);
        picker.setToolBarManager(getViewToolBarManager());

        DataSeriesChartLegend legend = new DataSeriesChartLegend(composite, picker);

        updateTitle(Messages.LabelPerformanceChart + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
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
        notifyModelUpdated();
    }

    @Override
    public void notifyModelUpdated()
    {
        seriesBuilder.getCache().clear();
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelPerformanceChart + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

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

        // re-layout in case chart legend changed
        chart.getParent().layout(true);
    }

    private void setChartSeries()
    {
        Interval interval = getReportingPeriod().toInterval(LocalDate.now());
        Lists.reverse(picker.getSelectedDataSeries())
                        .forEach(series -> seriesBuilder.build(series, interval, aggregationPeriod));
    }

    private final class AggregationPeriodDropDown extends DropDown implements IMenuListener
    {
        private AggregationPeriodDropDown()
        {
            super(aggregationPeriod == null ? Messages.LabelAggregationDaily : aggregationPeriod.toString());
            setMenuListener(this);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            Action daily = new Action(Messages.LabelAggregationDaily)
            {
                @Override
                public void run()
                {
                    setLabel(Messages.LabelAggregationDaily);
                    aggregationPeriod = null;
                    getPart().getPreferenceStore().setValue(KEY_AGGREGATION_PERIOD, ""); //$NON-NLS-1$
                    updateChart();
                }
            };
            daily.setChecked(aggregationPeriod == null);
            manager.add(daily);

            for (final Aggregation.Period period : Aggregation.Period.values())
            {
                Action action = new Action(period.toString())
                {
                    @Override
                    public void run()
                    {
                        setLabel(period.toString());
                        aggregationPeriod = period;
                        getPart().getPreferenceStore().setValue(KEY_AGGREGATION_PERIOD, period.name());
                        updateChart();
                    }
                };
                action.setChecked(aggregationPeriod == period);
                manager.add(action);
            }
        }
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
                    exporter.addDiscontinousSeries(Messages.PerformanceChartLabelCPI);
                    exporter.setDateFormat(new SimpleDateFormat("yyyy-MM-dd")); //$NON-NLS-1$
                    exporter.setValueFormat(new DecimalFormat("0.##########")); //$NON-NLS-1$
                    exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
                }
            });

            picker.getSelectedDataSeries().stream().forEach(ds -> addMenu(manager, ds));

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, getTitle());
        }

        private void addMenu(IMenuManager manager, final DataSeries series)
        {
            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, series.getLabel()), a -> {
                AbstractCSVExporter exporter = new AbstractCSVExporter()
                {
                    @Override
                    protected void writeToFile(File file) throws IOException
                    {
                        PerformanceIndex index = seriesBuilder.getCache().lookup(series,
                                        getReportingPeriod().toInterval(LocalDate.now()));
                        if (aggregationPeriod != null)
                            index = Aggregation.aggregate(index, aggregationPeriod);
                        index.exportTo(file);
                    }

                    @Override
                    protected Shell getShell()
                    {
                        return chart.getShell();
                    }
                };
                exporter.export(getTitle() + "_" + series.getLabel() + ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
            }));
        }
    }
}
