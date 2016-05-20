package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;

public class HeadingWidget implements WidgetDelegate
{
    private final Widget widget;

    public HeadingWidget(Widget widget)
    {
        this.widget = widget;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite heading = new Composite(parent, SWT.NONE);
        heading.setBackground(parent.getBackground());
        FillLayout layout = new FillLayout();
        layout.marginWidth = 5;
        heading.setLayout(layout);

        Label lbl = new Label(heading, SWT.NONE);
        lbl.setFont(resources.getBoldFont());
        lbl.setForeground(resources.getHeadingColor());
        lbl.setText(widget.getLabel());

        return heading;
    }

    @Override
    public void update(DashboardData data)
    {
        // nothing to do for static heading
    }
}
