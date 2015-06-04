package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.wizard.Wizard;

public class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private Extractor extractor;
    private List<File> files;

    private ReviewExtractedItemsPage page;

    public ImportExtractedItemsWizard(Client client, Extractor extractor, List<File> files)
    {
        this.client = client;
        this.extractor = extractor;
        this.files = files;

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages()
    {
        page = new ReviewExtractedItemsPage(client, extractor, files);
        addPage(page);
        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performFinish()
    {
        Portfolio primaryPortfolio = page.getPrimaryPortfolio();
        Account primaryAccount = page.getPrimaryAccount();
        Portfolio secondaryPortfolio = page.getSecondaryPortfolio();
        Account secondaryAccount = page.getSecondaryAccount();

        boolean isDirty = false;

        for (Extractor.Item item : page.getItems())
        {
            if (item.isImported())
            {
                item.insert(client, primaryPortfolio, primaryAccount, secondaryPortfolio, secondaryAccount);
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
