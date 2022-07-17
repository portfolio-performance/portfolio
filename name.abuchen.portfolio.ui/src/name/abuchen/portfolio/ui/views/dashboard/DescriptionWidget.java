package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.util.TextUtil;

public class DescriptionWidget extends WidgetDelegate<Object>
{
    private StyledLabel description;

    public DescriptionWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        
        get(LabelConfig.class).setMultiLine(true);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        FillLayout layout = new FillLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 10;
        container.setLayout(layout);

        description = new StyledLabel(container, SWT.WRAP);
        description.setForeground(Colors.HEADINGS);
        description.setBackground(container.getBackground());
        description.setText(TextUtil.tooltip(getWidget().getLabel()));

        return container;
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
    public void update(Object data)
    {
        description.setText(TextUtil.tooltip(getWidget().getLabel()));
        description.requestLayout();
    }
}
