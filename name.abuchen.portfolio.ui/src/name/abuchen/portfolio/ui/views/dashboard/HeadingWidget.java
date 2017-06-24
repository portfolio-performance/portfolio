package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.util.Colors;

public class HeadingWidget extends WidgetDelegate
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
        title.setFont(resources.getBoldFont());
        title.setForeground(Colors.HEADINGS);
        title.setText(getWidget().getLabel());

        return heading;
    }

    @Override
    Control getTitleControl()
    {
        return title;
    }

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel());
    }
}
