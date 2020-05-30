package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractPage
{
    private Control control;

    private String title;

    public abstract void createControl(Composite parent);

    public void setControl(Control control)
    {
        this.control = control;
    }

    public Control getControl()
    {
        return control;
    }

    public void beforePage()
    {
    }

    public void afterPage()
    {
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    protected Shell getShell()
    {
        return control.getShell();
    }
}
