package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.TextUtil;

public class LabelConfig implements WidgetConfig
{
    private static final class NotEmptyValidator implements IInputValidator
    {
        @Override
        public String isValid(String newText)
        {
            return newText == null || newText.trim().isEmpty()
                            ? MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnLabel)
                            : null;
        }
    }

    private final WidgetDelegate<?> delegate;

    private boolean isMultiLine = false;

    public LabelConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
    }

    public void setMultiLine(boolean isMultiLine)
    {
        this.isMultiLine = isMultiLine;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                        new LabelOnly(TextUtil.limit(delegate.getWidget().getLabel(), 60)));

        manager.add(new SimpleAction(Messages.MenuRenameLabel, a -> {

            InputDialog dialog = isMultiLine ? createMultiLine() : createSingleLine();

            if (dialog.open() != Window.OK)
                return;

            delegate.getWidget().setLabel(dialog.getValue());

            delegate.update();
            delegate.getClient().touch();
        }));
    }

    private InputDialog createSingleLine()
    {
        return new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel, Messages.ColumnLabel,
                        delegate.getWidget().getLabel(), new NotEmptyValidator());
    }

    private InputDialog createMultiLine()
    {
        return new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel, Messages.ColumnLabel,
                        delegate.getWidget().getLabel(), new NotEmptyValidator())
        {
            @Override
            protected Control createDialogArea(Composite parent)
            {
                Control area = super.createDialogArea(parent);
                Text text = getText();

                GridDataFactory.fillDefaults().grab(true, true)
                                .hint(SWTHelper.stringWidth(text, "X") * 60, SWTHelper.lineHeight(text) * 4) //$NON-NLS-1$
                                .applyTo(text);

                return area;
            }

            @Override
            protected int getInputTextStyle()
            {
                return SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP;
            }

        };
    }

    @Override
    public String getLabel()
    {
        return Messages.ColumnLabel + ": " + delegate.getWidget().getLabel(); //$NON-NLS-1$
    }
}
