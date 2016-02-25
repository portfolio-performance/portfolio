package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.swtchart.IAxis;
import org.swtchart.ICustomPaintListener;
import org.swtchart.ILineSeries;
import org.swtchart.IPlotArea;
import org.swtchart.ISeries;

import name.abuchen.portfolio.math.Risk.Volatility;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.chart.ScatterChart;
import name.abuchen.portfolio.ui.util.chart.ScatterChartCSVExporter;
import name.abuchen.portfolio.ui.views.ChartConfigurator.ClientDataSeries;
import name.abuchen.portfolio.ui.views.ChartConfigurator.DataSeries;

public class ReturnsVolatilityChartView extends AbstractHistoricView
{
    private CurrencyConverter converter;

    private ScatterChart chart;
    private ChartConfigurator picker;

    private Map<Object, PerformanceIndex> dataCache = new HashMap<Object, PerformanceIndex>();

    @PostConstruct
    private void setupCurrencyConverter(ExchangeRateProviderFactory factory, Client client)
    {
        converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
    }

    @Override
    protected String getTitle()
    {
        return Messages.LabelHistoricalReturnsAndVolatiltity;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        new ExportDropDown(toolBar);
        addConfigButton(toolBar);
    }

    private void addConfigButton(ToolBar toolBar)
    {
        Action save = new Action()
        {
            @Override
            public void run()
            {
                picker.showSaveMenu(getActiveShell());
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
                picker.showMenu(getActiveShell());
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

        chart = new ScatterChart(composite);
        chart.getTitle().setText(getTitle());
        chart.getTitle().setVisible(false);

        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(Messages.LabelVolatility);
        xAxis.getTick().setFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setText(Messages.LabelPeformanceTTWROR);
        yAxis.getTick().setFormat(new DecimalFormat("0.##%")); //$NON-NLS-1$

        ((IPlotArea) chart.getPlotArea()).addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                int y = xAxis.getPixelCoordinate(0);
                e.gc.drawLine(y, 0, y, e.height);

                int x = yAxis.getPixelCoordinate(0);
                e.gc.drawLine(0, x, e.width, x);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return true;
            }
        });

        picker = new ChartConfigurator(composite, this, ChartConfigurator.Mode.RETURN_VOLATILITY);
        picker.setListener(() -> updateChart());
        
        updateTitle(Messages.LabelHistoricalReturnsAndVolatiltity + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$);

        GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.FILL).applyTo(picker);

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
        dataCache.clear();
        updateChart();
    }

    @Override
    public void notifyModelUpdated()
    {
        dataCache.clear();
        updateChart();
    }

    private void updateChart()
    {
        try
        {
            updateTitle(Messages.LabelHistoricalReturnsAndVolatiltity + " (" + picker.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$);
            
            chart.suspendUpdate(true);
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
        List<Exception> warnings = new ArrayList<Exception>();

        for (DataSeries item : picker.getSelectedDataSeries())
        {
            if (item.getType() == Client.class)
                addClient(item, (ClientDataSeries) item.getInstance(), warnings);
            else if (item.getType() == Security.class)
                addSecurity(item, (Security) item.getInstance(), warnings);
            else if (item.getType() == Portfolio.class)
                addPortfolio(item, (Portfolio) item.getInstance(), warnings);
            else if (item.getType() == Account.class)
                addAccount(item, (Account) item.getInstance(), warnings);
            else if (item.getType() == Classification.class)
                addClassification(item, (Classification) item.getInstance(), warnings);
        }

        PortfolioPlugin.log(warnings);
    }

    private void addScatterSeries(DataSeries item, PerformanceIndex index)
    {
        Volatility volatility = index.getVolatility();
        ILineSeries series = chart.addScatterSeries(new double[] { volatility.getStandardDeviation() },
                        new double[] { index.getFinalAccumulatedPercentage() }, item.getLabel());
        item.configure(series);
    }

    private PerformanceIndex getClientIndex(List<Exception> warnings)
    {
        PerformanceIndex index = dataCache.get(Client.class);
        if (index == null)
        {
            ReportingPeriod interval = getReportingPeriod();
            index = PerformanceIndex.forClient(getClient(), converter, interval, warnings);
            dataCache.put(Client.class, index);
        }
        return index;
    }

    private void addClient(DataSeries item, ClientDataSeries type, List<Exception> warnings)
    {
        PerformanceIndex clientIndex = getClientIndex(warnings);

        if (type == ClientDataSeries.TOTALS)
            addScatterSeries(item, clientIndex);
    }

    private void addSecurity(DataSeries item, Security security, List<Exception> warnings)
    {
        if (item.isBenchmark())
            addSecurityBenchmark(item, security, warnings);
        else
            addSecurityPerformance(item, security, warnings);
    }

    private void addSecurityBenchmark(DataSeries item, Security security, List<Exception> warnings)
    {
        PerformanceIndex securityIndex = dataCache.get(security);

        if (securityIndex == null)
        {
            PerformanceIndex clientIndex = getClientIndex(warnings);
            securityIndex = PerformanceIndex.forSecurity(clientIndex, security, warnings);
            dataCache.put(security, securityIndex);
        }

        addScatterSeries(item, securityIndex);
    }

    private void addSecurityPerformance(DataSeries item, Security security, List<Exception> warnings)
    {
        PerformanceIndex securityIndex = dataCache.get(security.getUUID());

        if (securityIndex == null)
        {
            securityIndex = PerformanceIndex.forInvestment(getClient(), converter, security, getReportingPeriod(),
                            warnings);
            dataCache.put(security.getUUID(), securityIndex);
        }

        addScatterSeries(item, securityIndex);
    }

    private void addPortfolio(DataSeries item, Portfolio portfolio, List<Exception> warnings)
    {
        Object cacheKey = item.isPortfolioPlus() ? portfolio.getUUID() : portfolio;
        PerformanceIndex portfolioIndex = dataCache.get(cacheKey);

        if (portfolioIndex == null)
        {
            portfolioIndex = item.isPortfolioPlus() ? PerformanceIndex //
                            .forPortfolioPlusAccount(getClient(), converter, portfolio, getReportingPeriod(), warnings)
                            : PerformanceIndex.forPortfolio(getClient(), converter, portfolio, getReportingPeriod(),
                                            warnings);
            dataCache.put(cacheKey, portfolioIndex);
        }

        addScatterSeries(item, portfolioIndex);
    }

    private void addAccount(DataSeries item, Account account, List<Exception> warnings)
    {
        PerformanceIndex accountIndex = dataCache.get(account);

        if (accountIndex == null)
        {
            accountIndex = PerformanceIndex.forAccount(getClient(), converter, account, getReportingPeriod(), warnings);
            dataCache.put(account, accountIndex);
        }

        addScatterSeries(item, accountIndex);
    }

    private void addClassification(DataSeries item, Classification classification, List<Exception> warnings)
    {
        PerformanceIndex index = dataCache.get(classification);

        if (index == null)
        {
            index = PerformanceIndex.forClassification(getClient(), converter, classification, getReportingPeriod(),
                            warnings);
            dataCache.put(classification, index);
        }

        addScatterSeries(item, index);
    }

    private final class ExportDropDown extends AbstractDropDown
    {
        private ExportDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.MenuExportData, Images.EXPORT.image(), SWT.NONE);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new Action(Messages.MenuExportChartData)
            {
                @Override
                public void run()
                {
                    ScatterChartCSVExporter exporter = new ScatterChartCSVExporter(chart);
                    exporter.setValueFormat(new DecimalFormat("0.##########%")); //$NON-NLS-1$
                    exporter.export(getTitle() + ".csv"); //$NON-NLS-1$
                }
            });

            Set<Class<?>> exportTypes = new HashSet<Class<?>>(Arrays.asList(new Class<?>[] { //
                            Client.class, Security.class, Portfolio.class, Account.class, Classification.class }));

            for (DataSeries series : picker.getSelectedDataSeries())
            {
                if (exportTypes.contains(series.getType()))
                    addMenu(manager, series);
            }

            manager.add(new Separator());
            chart.exportMenuAboutToShow(manager, getTitle());
        }

        private void addMenu(IMenuManager manager, final DataSeries series)
        {
            manager.add(new Action(MessageFormat.format(Messages.LabelExport, series.getLabel()))
            {
                @Override
                public void run()
                {
                    exportDataSeries(series);
                }
            });
        }

        private void exportDataSeries(DataSeries series)
        {
            AbstractCSVExporter exporter = new AbstractCSVExporter()
            {
                @Override
                protected void writeToFile(File file) throws IOException
                {
                    PerformanceIndex index = null;

                    if (series.getType() == Client.class)
                        index = dataCache.get(Client.class);
                    else if (series.isPortfolioPlus())
                        index = dataCache.get(((Portfolio) series.getInstance()).getUUID());
                    else if (series.getType() == Security.class && !series.isBenchmark())
                        index = dataCache.get(((Security) series.getInstance()).getUUID());
                    else
                        index = dataCache.get(series.getInstance());

                    index.exportVolatilityData(file);
                }

                @Override
                protected Control getControl()
                {
                    return ExportDropDown.this.getToolBar();
                }
            };
            exporter.export(getTitle() + "_" + series.getLabel() + ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
