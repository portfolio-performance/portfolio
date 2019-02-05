package name.abuchen.portfolio.ui.selection;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

@Creatable
@Singleton
public class SelectionService
{
    private SecuritySelection selection;

    public SecuritySelection getSelection()
    {
        return selection;
    }

    public void setSelection(SecuritySelection selection)
    {
        this.selection = selection;
    }
}
