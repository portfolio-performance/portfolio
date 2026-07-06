package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache;
import name.abuchen.portfolio.ui.wizards.pdfdebug.CreatePDFDebugWizard;
import name.abuchen.portfolio.ui.wizards.pdfdebug.CreatePDFDebugWizardDialog;

public class CreateTextFromPDFHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, UnrecognizedPDFCache cache)
    {
        var wizard = new CreatePDFDebugWizard(cache);
        wizard.setWindowTitle(Messages.PDFImportDebugTextExtraction);
        new CreatePDFDebugWizardDialog(shell, wizard).open();
    }
}
