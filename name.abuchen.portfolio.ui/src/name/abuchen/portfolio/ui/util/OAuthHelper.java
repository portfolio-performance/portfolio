package name.abuchen.portfolio.ui.util;

import java.util.function.Consumer;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.ui.Messages;

public class OAuthHelper
{
    @FunctionalInterface
    public interface AccessTokenRunnable<T>
    {
        T get() throws AuthenticationException;
    }

    public static <T> void run(AccessTokenRunnable<T> supplier, Consumer<T> consumer)
    {
        Job.createSystem("Asynchronously retrieve token", monitor -> { //$NON-NLS-1$
            try
            {
                var result = supplier.get();
                Display.getDefault().asyncExec(() -> consumer.accept(result));
            }
            catch (AuthenticationException e)
            {
                Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
                                Messages.LabelError, e.getMessage()));
            }
        }).schedule();
    }

}
