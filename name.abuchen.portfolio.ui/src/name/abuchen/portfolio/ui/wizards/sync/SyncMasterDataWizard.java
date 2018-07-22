package name.abuchen.portfolio.ui.wizards.sync;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class SyncMasterDataWizard extends Wizard
{
    private final Client client;
    private final IPreferenceStore preferences;

    private SyncMasterDataPage syncPage;

    public SyncMasterDataWizard(Client client, IPreferenceStore preferences)
    {
        this.client = client;
        this.preferences = preferences;

        setNeedsProgressMonitor(true);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(new InfoSyncPage());
        addPage(syncPage = new SyncMasterDataPage(client, preferences));

        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performFinish()
    {
        syncPage.getSecurities().forEach(s -> {
            if (s.apply())
                client.markDirty();
        });

        return true;
    }

}
