package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.TextUtil;

public class HeadingWidget extends WidgetDelegate<Object>
{
    private Label title;

    public HeadingWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite heading = new Composite(parent, SWT.NONE);
        heading.setBackground(parent.getBackground());
        FillLayout layout = new FillLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 10;
        heading.setLayout(layout);

        title = new Label(heading, SWT.NONE);
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING1);
        title.setForeground(Colors.HEADINGS);
        title.setBackground(heading.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));

        return heading;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> null;
    }

    @Override
    public void update(Object data)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
    }
}
