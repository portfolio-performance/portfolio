package name.abuchen.portfolio.ui.handlers;

import java.lang.reflect.Method;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Select all handler. As E4 does not come with a default handler, this is a
 * simplified implementation (without Swing support) based on
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=426773
 */
public class SelectAllHandler
{
    @Execute
    public void execute()
    {
        Display display = Display.getCurrent();
        if (display == null)
            return;

        Control focusControl = display.getFocusControl();
        if (focusControl == null)
            return;

        try
        {
            final Class<?> clazz = focusControl.getClass();
            Method method = clazz.getMethod("selectAll"); //$NON-NLS-1$

            method.invoke(focusControl);
            focusControl.notifyListeners(SWT.Selection, null);
        }
        catch (ReflectiveOperationException | IllegalArgumentException e)
        {
            // ignore
        }
    }

}
