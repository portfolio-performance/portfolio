package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.jobs.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public final class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private IPreferenceStore preferences;
    private Map<Extractor, List<Item>> result;
    private Map<File, List<Exception>> errors;

    private List<ReviewExtractedItemsPage> pages = new ArrayList<>();

    public ImportExtractedItemsWizard(Client client, IPreferenceStore preferences, Map<Extractor, List<Item>> result,
                    Map<File, List<Exception>> errors)
    {
        this.client = client;
        this.preferences = preferences;
        this.result = result;
        this.errors = errors;

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(false);
    }

    @Override
    public boolean canFinish()
    {
        // allow "Finish" only on the last page
        return !pages.isEmpty() && getContainer().getCurrentPage() == pages.get(pages.size() - 1);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        if (!errors.isEmpty())
        {
            addPage(new ErroneousImportFilesPage(errors));
        }

        result.entrySet().stream() //
                        .sorted((r, l) -> r.getKey().getLabel().compareTo(l.getKey().getLabel())) //
                        .map(entry -> new Extractor()
                        {
                            @Override
                            public String getLabel()
                            {
                                return entry.getKey().getLabel();
                            }

                            @Override
                            public List<Item> extract(SecurityCache securityCache, InputFile file,
                                            List<Exception> errors)
                            {
                                return entry.getValue();
                            }

                            @Override
                            public List<Item> extract(List<InputFile> file, List<Exception> errors)
                            {
                                return entry.getValue();
                            }
                        }) //
                        .forEach(extractor -> {
                            ReviewExtractedItemsPage page = new ReviewExtractedItemsPage(client, extractor, preferences,
                                            Collections.emptyList());
                            pages.add(page);
                            addPage(page);
                        });

        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    @Override
    public boolean performFinish()
    {
        if (!pages.isEmpty())
        {
            boolean isDirty = false;
            for (int index = 0; index < pages.size(); index++)
            {
                ReviewExtractedItemsPage page = pages.get(index);
                page.afterPage();

                InsertAction action = new InsertAction(client);
                action.setConvertBuySellToDelivery(page.doConvertToDelivery());

                for (ExtractedEntry entry : page.getEntries())
                {
                    if (entry.isImported())
                    {
                        entry.getItem().apply(action, page);
                        isDirty = true;
                    }
                }
            }

            if (isDirty)
            {
                client.markDirty();

                // run consistency checks in case bogus transactions have been
                // created (say: an outbound delivery of a security where there
                // no held shares)
                new ConsistencyChecksJob(client, false).schedule();
            }
        }

        return true;
    }
}
