package name.abuchen.portfolio.ui.selection;

import java.util.Optional;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

import name.abuchen.portfolio.model.Client;

@Creatable
@Singleton
public class SelectionService
{
    private SecuritySelection selection;

    public Optional<SecuritySelection> getSelection(Client client)
    {
        if (selection == null || client == null || !client.equals(selection.getClient()))
            return Optional.empty();

        return Optional.of(selection);
    }

    public void setSelection(SecuritySelection selection)
    {
        this.selection = selection;
    }
}
