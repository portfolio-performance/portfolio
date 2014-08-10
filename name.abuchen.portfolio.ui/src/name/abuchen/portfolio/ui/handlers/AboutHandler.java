package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.ui.dialogs.AboutDialog;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;

public class AboutHandler
{
    @Execute
    public void execute(IEclipseContext context)
    {
        AboutDialog dialog = ContextInjectionFactory.make(AboutDialog.class, context);
        dialog.open();
    }
}
