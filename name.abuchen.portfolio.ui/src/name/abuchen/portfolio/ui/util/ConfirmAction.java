package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class ConfirmAction extends Action
{
    @FunctionalInterface
    public interface Runnable
    {
        void run(Action action);
    }

    private final String message;
    private final Runnable runnable;

    public ConfirmAction(String title, String message, Runnable runnable)
    {
        super(title);
        this.message = message;
        this.runnable = runnable;
    }

    @Override
    public void run()
    {
        if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), getText(), message))
            runnable.run(this);
    }
}
