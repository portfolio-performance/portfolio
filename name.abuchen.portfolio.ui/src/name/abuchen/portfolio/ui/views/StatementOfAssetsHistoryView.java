package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.ISeries;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.chart.TimelineChartCSVExporter;
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
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHistory;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addExportButton(ToolBar toolBar)
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

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    private void exportMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        manager.add(new Action(Messages.MenuExportChartData)
        {
            @Override
            public void run()
            {
                TimelineChartCSVExporter exporter = new TimelineChartCSVExporter(chart);
                exporter.addDiscontinousSeries(Messages.LabelTransferals);
                exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
            }
        });
        manager.add(new Separator());
        chart.exportMenuAboutToShow(manager, getTitle());
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action save = new Action()
        {
            @Override
            public void run()
            {
                configurator.showSaveMenu(getActiveShell());
            }
        };
        save.setImageDescriptor(Images.SAVE.descriptor());
        save.setToolTipText(Messages.MenuSaveChart);
        new ActionContributionItem(save).fill(toolBar, -1);

        Action config = new Action()
        {
            @Override
            public void run()
            {
                configurator.showMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(Images.CONFIG.descriptor());
        config.setToolTipText(Messages.MenuConfigureChart);
        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected Composite createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        chart = new TimelineChart(composite);
        chart.getTitle().setText(getTitle());
        chart.getTitle().setVisible(false);

        DataSeriesCache cache = make(DataSeriesCache.class);
        seriesBuilder = new StatementOfAssetsSeriesBuilder(chart, cache);

        configurator = new DataSeriesConfigurator(this, DataSeriesConfigurator.Mode.STATEMENT_OF_ASSETS);
        configurator.addListener(() -> updateChart());

        DataSeriesChartLegend legend = new DataSeriesChartLegend(composite, configurator);

        updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(legend);

        configurator.getSelectedDataSeries().forEach(series -> seriesBuilder.build(series, getReportingPeriod()));

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
        seriesBuilder.getCache().clear();
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelStatementOfAssetsHistory + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

            chart.suspendUpdate(true);

            for (ISeries s : chart.getSeriesSet().getSeries())
                chart.getSeriesSet().deleteSeries(s.getId());

            configurator.getSelectedDataSeries().forEach(series -> seriesBuilder.build(series, getReportingPeriod()));

            chart.adjustRange();
        }
        finally
        {
            chart.suspendUpdate(false);
        }
        chart.redraw();
    }
}
