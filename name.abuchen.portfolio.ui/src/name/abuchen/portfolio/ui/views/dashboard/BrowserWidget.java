package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.util.TextUtil;

public class BrowserWidget extends WidgetDelegate<Object>
{
    private Label title;
    private Browser description;
    private Composite composite;

    public BrowserWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ChartHeightConfig(this));
        addConfig(new UrlConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        title = new Label(composite, SWT.NONE);
        title.setBackground(composite.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        description = new Browser(composite, SWT.NONE);
        description.setJavascriptEnabled(false);
        String url = getWidget().getConfiguration().get(Dashboard.Config.URL.name());
        if (url != null)
        {
            description.setUrl(url);
        }
        description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        return composite;
    }

    @Override
    public Control getTitleControl()
    {
        return description;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> null;
    }

    @Override
    public void update(Object object)
    {
        String url = getWidget().getConfiguration().get(Dashboard.Config.URL.name());
        if (url != null)
        {
            description.setUrl(url);
        }

        GridData data = (GridData) composite.getLayoutData();

        int oldHeight = data.heightHint;
        int newHeight = get(ChartHeightConfig.class).getPixel();

        if (oldHeight != newHeight)
        {
            data.heightHint = newHeight;
            description.getParent().layout(true);
            description.getParent().getParent().layout(true);
        }
    }
}
