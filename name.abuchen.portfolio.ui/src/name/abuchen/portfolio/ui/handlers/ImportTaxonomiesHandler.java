package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.MultiTaxonomyImportWizard;

public class ImportTaxonomiesHandler
{

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, IStylingEngine stylingEngine)
    {
        MenuHelper.getActiveClientInput(part).ifPresent(input -> {
            var wizard = new MultiTaxonomyImportWizard(input.getClient(), input.getPreferenceStore(), stylingEngine);
            new WizardDialog(shell, wizard).open();
        });
    }
}
