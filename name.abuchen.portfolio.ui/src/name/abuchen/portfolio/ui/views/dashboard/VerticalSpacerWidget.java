package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.TextUtil;

public class VerticalSpacerWidget extends WidgetDelegate<Object>
{
    public static class SpacerConfig implements WidgetConfig
    {
        private static final int MINIMUM_HEIGHT = 5;

        private final WidgetDelegate<?> delegate;

        private int height = 20;

        public SpacerConfig(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;

            String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.HEIGHT.name());

            if (code != null)
            {
                try
                {
                    this.height = Integer.parseInt(code);
                }
                catch (NumberFormatException e)
                {
                    // ignore -> use the default height
                }
            }
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

            IInputValidator validator = newText -> {
                try
                {
                    int newHeight = Integer.parseInt(newText);

                    if (newHeight < MINIMUM_HEIGHT)
                        return MessageFormat.format(Messages.MsgErrorMinimumHeightRequired, MINIMUM_HEIGHT);

                    return null;
                }
                catch (NumberFormatException e)
                {
                    return String.format(Messages.CellEditor_NotANumber, newText);
                }
            };

            manager.add(new SimpleAction(Messages.MenuChangeHeight, a -> {
                InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuChangeHeight,
                                Messages.ColumnHeight, String.valueOf(height), validator);

                if (dialog.open() != InputDialog.OK)
                    return;

                this.height = Integer.parseInt(dialog.getValue());

                delegate.getWidget().getConfiguration().put(Dashboard.Config.HEIGHT.name(), String.valueOf(height));

                delegate.update();
                delegate.getClient().touch();
            }));
        }

        @Override
        public String getLabel()
        {
            return Messages.ColumnHeight + ": " + Integer.valueOf(height); //$NON-NLS-1$
        }

        public int getHeight()
        {
            return height;
        }
    }

    private Label title;

    public VerticalSpacerWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new SpacerConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().applyTo(container);

        title = new Label(container, SWT.CENTER);
        title.setText(""); //$NON-NLS-1$

        title.addMouseTrackListener(new MouseTrackAdapter()
        {
            @Override
            public void mouseExit(MouseEvent e)
            {
                title.setText(""); //$NON-NLS-1$
            }

            @Override
            public void mouseEnter(MouseEvent e)
            {
                title.setText(TextUtil.tooltip(getWidget().getLabel()));
            }
        });

        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, get(SpacerConfig.class).getHeight())
                        .applyTo(title);

        return container;
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
    public void update(Object d)
    {
        GridData data = (GridData) title.getLayoutData();

        int oldHeight = data.heightHint;
        int newHeight = get(SpacerConfig.class).getHeight();

        data.heightHint = newHeight;
        title.setText(""); //$NON-NLS-1$

        if (oldHeight != newHeight)
        {
            title.getParent().layout(true);
            title.getParent().getParent().layout(true);
        }
    }
}
