package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.ISeries;

import com.google.common.collect.Lists;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesChartLegend;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator;
import name.abuchen.portfolio.ui.views.dataseries.StatementOfAssetsSeriesBuilder;

public class StatementOfAssetsHistoryView extends AbstractHistoricView
{
    private TimelineChart chart;
    private DataSeriesConfigurator configurator;
    private StatementOfAssetsSeriesBuilder seriesBuilder;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelStatementOfAssetsHistory;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE,
                        manager -> configurator.configMenuAboutToShow(manager)));
    }

    private void addExportButton(ToolBarManager toolBar)
    {
        Action export = new Action()
        {
            private Menu menu;

            @Override
            public void run()
            {
                if (menu == null)
                    menu = createContextMenu(getActiveShell(),
                                    StatementOfAssetsHistoryView.this::exportMenuAboutToShow);

                menu.setVisible(true);
            }
        };
        export.setImageDescriptor(Images.EXPORT.descriptor());
        export.setToolTipText(Messages.MenuExportData);

        toolBar.add(export);
    }

    private void exportMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        manager.add(new SimpleAction(Messages.MenuExportChartData, a -> {
            TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
            exporter.addDiscontinousSeries(Messages.LabelTransferals);
            exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
        }));
        manager.add(new Separator());
        chart.exportMenuAboutToShow(manager, getTitle());
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        chart = new TimelineChart(composite);
        chart.getTitle().setVisible(false);
        chart.getToolTip().reverseLabels(true);

        DataSeriesCache cache = make(DataSeriesCache.class);
        seriesBuilder = new StatementOfAssetsSeriesBuilder(chart, cache);

        configurator = new DataSeriesConfigurator(this, DataSeries.UseCase.STATEMENT_OF_ASSETS);
        configurator.addListener(this::updateChart);
        configurator.setToolBarManager(getViewToolBarManager());

        DataSeriesChartLegend legend = new DataSeriesChartLegend(composite, configurator);

        updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        chart.getTitle().setText(getTitle());

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(legend);

        Lists.reverse(configurator.getSelectedDataSeries())
                        .forEach(series -> seriesBuilder.build(series, getReportingPeriod()));

        return composite;
    }

    @Override
    public void setFocus()
    {
        try
        {
            chart.setRedraw(false);
            chart.adjustRange();
        }
        finally
        {
            chart.setRedraw(true);
        }

        chart.setFocus();
    }

    @Override
    public void notifyModelUpdated()
    {
        seriesBuilder.getCache().clear();
        updateChart();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        notifyModelUpdated();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

            chart.suspendUpdate(true);
            chart.getTitle().setText(getTitle());
            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            Lists.reverse(configurator.getSelectedDataSeries())
                            .forEach(series -> seriesBuilder.build(series, getReportingPeriod()));

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
}
