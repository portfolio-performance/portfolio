package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.wizard.Wizard;

public class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private Extractor extractor;
    private List<File> files;
    private List<Exception> errors;

    private ReviewExtractedItemsPage page;

    public ImportExtractedItemsWizard(Client client, Extractor extractor, List<File> files, List<Exception> errors)
    {
        this.client = client;
        this.extractor = extractor;
        this.files = files;
        this.errors = errors;

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages()
    {
        page = new ReviewExtractedItemsPage(client, extractor, files, errors);
        addPage(page);
        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performFinish()
    {
        Portfolio portfolio = page.getPortfolio();
        Account account = page.getAccount();

        boolean isDirty = false;

        for (Extractor.Item item : page.getItems())
        {
            item.insert(client, portfolio, account);
            isDirty = true;
        }

        if (isDirty)
            client.markDirty();

        return true;
    }

}
