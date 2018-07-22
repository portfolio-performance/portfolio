package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.sync.SyncMasterDataWizard;

public class SyncMasterDataHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        PortfolioPart p = (PortfolioPart) part.getObject();

        WizardDialog dialog = new WizardDialog(shell, new SyncMasterDataWizard(client, p.getPreferenceStore()));
        dialog.open();
    }

}
