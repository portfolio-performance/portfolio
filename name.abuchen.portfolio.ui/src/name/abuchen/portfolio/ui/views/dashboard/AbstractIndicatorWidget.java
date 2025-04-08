package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractIndicatorWidget<D> extends WidgetDelegate<D>
{
    @Inject
    protected AbstractFinanceView view;

    protected Label title;
    protected ColoredLabel indicator;

    protected AbstractIndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks,
                    Predicate<DataSeries> predicate)
    {
        super(widget, dashboardData);

        addConfig(new DataSeriesConfig(this, supportsBenchmarks, predicate));
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
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));
    }
}
