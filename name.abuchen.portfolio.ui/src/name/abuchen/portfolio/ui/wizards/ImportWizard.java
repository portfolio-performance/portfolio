package name.abuchen.portfolio.ui.wizards;

import java.io.File;

import name.abuchen.portfolio.model.Client;

import org.eclipse.jface.wizard.Wizard;

public class ImportWizard extends Wizard
{
    private Client client;
    private File inputFile;

    public ImportWizard(Client client, File inputFile)
    {
        this.client = client;
        this.inputFile = inputFile;
    }

    @Override
    public void addPages()
    {
        addPage(new ImportDefinitionPage(client, inputFile));
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }

}
