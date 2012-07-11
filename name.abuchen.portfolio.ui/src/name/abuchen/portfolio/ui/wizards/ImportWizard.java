package name.abuchen.portfolio.ui.wizards;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.CSVImporter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;

public class ImportWizard extends Wizard
{
    private CSVImporter importer;

    public ImportWizard(Client client, File inputFile)
    {
        setWindowTitle(Messages.CSVImportWizardTitle);
        this.importer = new CSVImporter(client, inputFile);
    }

    @Override
    public void addPages()
    {
        addPage(new ImportDefinitionPage(importer));
    }

    @Override
    public boolean performFinish()
    {
        List<Exception> errors = new ArrayList<Exception>();
        importer.createObjects(errors);

        if (!errors.isEmpty())
        {
            IStatus[] status = new IStatus[errors.size()];
            int ii = 0;
            for (Exception e : errors)
                status[ii++] = new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage());

            String message = MessageFormat.format(Messages.CSVImportErrorsDuringImport, errors.size(), importer
                            .getRawValues().size());
            ErrorDialog.openError(getShell(), Messages.LabelError, null, new MultiStatus(PortfolioPlugin.PLUGIN_ID, -1,
                            status, message, null));
        }

        return true;
    }

}
