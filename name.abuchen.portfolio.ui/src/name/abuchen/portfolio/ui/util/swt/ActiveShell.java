package name.abuchen.portfolio.ui.util.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Keep a reference to the last active shell in case the RCP application has no
 * focus at the moment. Use case: user drag and drops files to PP which
 * currently has not the focus (instead Finder or Windows Explorer have the
 * focus). See // https://stackoverflow.com/a/28986616/1158146
 */
public class ActiveShell
{
    private static final ActiveShell activeShell = new ActiveShell();

    private Shell shell;

    public ActiveShell()
    {
        Display display = Display.getCurrent();

        shell = display.getActiveShell();

        display.addFilter(SWT.Activate, e -> {
            if (e.widget instanceof Shell)
                shell = (Shell) e.widget;
        });
    }

    public static Shell get()
    {
        return activeShell.getShell();
    }

    private Shell getShell()
    {
        return shell;
    }
}
