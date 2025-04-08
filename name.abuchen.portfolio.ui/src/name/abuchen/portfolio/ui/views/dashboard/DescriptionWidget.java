package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

public class DescriptionWidget extends WidgetDelegate<Object>
{
    private StyledLabel description;

    public DescriptionWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        
        get(LabelConfig.class).setMultiLine(true);
        get(LabelConfig.class).setContainsTags(true);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());

        FillLayout layout = new FillLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 10;
        container.setLayout(layout);

        description = new StyledLabel(container, SWT.WRAP);
        description.setForeground(Colors.HEADINGS);
        description.setBackground(container.getBackground());
        description.setText(getWidget().getLabel());
        description.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);

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
        description.setText(getWidget().getLabel());
        description.requestLayout();
    }
}
