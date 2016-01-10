package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private Extractor extractor;
    private IPreferenceStore preferences;
    private List<File> files;

    private ReviewExtractedItemsPage page;

    public ImportExtractedItemsWizard(Client client, Extractor extractor, IPreferenceStore preferences,
                    List<File> files)
    {
        this.client = client;
        this.extractor = extractor;
        this.preferences = preferences;
        this.files = files;

        setWindowTitle(Messages.PDFImportWizardTitle);
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
        page = new ReviewExtractedItemsPage(client, extractor, preferences, files);
        addPage(page);
        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performFinish()
    {
        page.afterPage();

        InsertAction action = new InsertAction(client);
        action.setConvertBuySellToDelivery(page.doConvertToDelivery());

        boolean isDirty = false;
        for (ExtractedEntry entry : page.getEntries())
        {
            if (entry.isImported())
            {
                entry.getItem().apply(action, page);
                isDirty = true;
            }
        }

        if (isDirty)
        {
            client.markDirty();

            // run consistency checks in case bogus transactions have been
            // created (say: an outbound delivery of a security where there no
            // held shares)
            new ConsistencyChecksJob(client, false).schedule();
        }

        return true;
    }

}
