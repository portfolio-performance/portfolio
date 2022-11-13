package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Predicate;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractIndicatorWidget<D> extends WidgetDelegate<D>
{
    public static class DataSeriesConfigSetup
    {
        private String label;
        private boolean supportsBenchmark;
        private Predicate<DataSeries> predicate;
        private boolean supportsEmptyDataSeries;
        
        public DataSeriesConfigSetup(String label, boolean supportsBenchmark, Predicate<DataSeries> predicate,
                        boolean supportsEmptyDataSeries)
        {
            this.label = label;
            this.supportsBenchmark = supportsBenchmark;
            this.predicate = predicate;
            this.supportsEmptyDataSeries = supportsEmptyDataSeries;
        }
    }
    
    @Inject
    protected AbstractFinanceView view;

    protected Label title;
    protected ColoredLabel indicator;
    protected DataSeriesConfig[] dataSeriesConfigs;

    protected AbstractIndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks,
                    Predicate<DataSeries> predicate)
    {
        this(widget, dashboardData, new DataSeriesConfigSetup[] {new DataSeriesConfigSetup(Messages.LabelDataSeries,
                        supportsBenchmarks, predicate, false)});
    }

    protected AbstractIndicatorWidget(Widget widget, DashboardData dashboardData,
                    DataSeriesConfigSetup[] dataSeriesConfigSetups)
    {
        super(widget, dashboardData);

        dataSeriesConfigs = new DataSeriesConfig[dataSeriesConfigSetups.length];
        for(int i = 0; i < dataSeriesConfigSetups.length; i++)
        {
            dataSeriesConfigs[i] = new DataSeriesConfig(this, i, dataSeriesConfigSetups[i].supportsBenchmark,
                            dataSeriesConfigSetups[i].supportsEmptyDataSeries, dataSeriesConfigSetups[i].predicate, dataSeriesConfigSetups[i].label);
            addConfig(dataSeriesConfigs[i]);
        }
        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(Colors.theme().defaultBackground());
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        indicator = new ColoredLabel(container, SWT.NONE);
        indicator.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.KPI);
        indicator.setBackground(Colors.theme().defaultBackground());
        indicator.setText(""); //$NON-NLS-1$
        indicator.addMouseListener(MouseListener.mouseUpAdapter(e -> {
            DataSeries ds = get(DataSeriesConfig.class).getDataSeries();
            if (ds.getInstance() instanceof Security)
                view.setInformationPaneInput(ds.getInstance());
        }));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public void update(D data)
    {
        // construct label to indicate the data series (user can manually change
        // the label later)
        getWidget().setLabel(WidgetFactory.valueOf(getWidget().getType()).getLabel() + ", " //$NON-NLS-1$
                        + getAllDataSeriesLabels());
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));
    }
    
    private String getAllDataSeriesLabels() 
    {
        String dataSeriesLabel = ""; //$NON-NLS-1$
        for(int i = 0; i < dataSeriesConfigs.length; i++)
        {
            DataSeries series = dataSeriesConfigs[i].getDataSeries();
            if(series != null)
            {
                if(i > 0)
                {
                    dataSeriesLabel += ", "; //$NON-NLS-1$
                }
                dataSeriesLabel += series.getLabel();
            }
        }
        return dataSeriesLabel;
    }
}
