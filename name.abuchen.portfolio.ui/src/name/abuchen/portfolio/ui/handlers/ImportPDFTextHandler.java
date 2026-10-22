package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.preferences.Experiments;

/**
 * Imports text files previously extracted from PDF documents (see "Create text
 * from PDF") through the regular PDF import. Used to debug the PDF extractors
 * without having the original document. Hidden behind an experimental flag.
 */
public class ImportPDFTextHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return isEnabled() && MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        MenuHelper.getActiveClient(part)
                        .ifPresent(client -> ImportPDFHandler.runImport((PortfolioPart) part.getObject(), shell,
                                        client, null, null, UIConstants.Preferences.PDF_TEXT_IMPORT_PATH,
                                        "Text extracted from PDF document (*.txt)", "*.txt;*.TXT")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /* package */ static boolean isEnabled()
    {
        return new Experiments().isEnabled(Experiments.Feature.SEP26_IMPORT_PDF_TEXT_FILES);
    }
}
