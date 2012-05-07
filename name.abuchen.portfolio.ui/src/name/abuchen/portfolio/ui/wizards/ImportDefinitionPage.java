package name.abuchen.portfolio.ui.wizards;

import java.io.File;

import name.abuchen.portfolio.model.Client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ImportDefinitionPage extends AbstractWizardPage
{

    public ImportDefinitionPage(Client client, File inputFile)
    {
        super("importdefintion"); //$NON-NLS-1$
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
    }

}
