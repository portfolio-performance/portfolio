package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.datatransfer.pdf.AssistantPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.HelloBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public final class ImportExtractedItemsWizard extends Wizard
{
    private Client client;
    private List<Extractor> extractors = new ArrayList<>();
    private IPreferenceStore preferences;
    private List<Extractor.InputFile> files;

    private List<ReviewExtractedItemsPage> pages = new ArrayList<>();

    /**
     * in legacy mode, the PDF import does not try to assign extractors
     * automatically. It is just a fallback in case the automatic assignment has
     * problems. This mode is only available via keyboard shortcut.
     */
    private boolean isLegacyMode = false;

    public ImportExtractedItemsWizard(Client client, Extractor extractor, IPreferenceStore preferences,
                    List<Extractor.InputFile> files) throws IOException
    {
        this.client = client;
        this.preferences = preferences;
        this.files = files;

        if (extractor != null)
            extractors.add(extractor);
        else
            addDefaultExtractors();

        setWindowTitle(Messages.PDFImportWizardTitle);
        setNeedsProgressMonitor(false);
    }

    private void addDefaultExtractors() throws IOException
    {
        extractors.add(new BaaderBankPDFExtractor(client));
        extractors.add(new BankSLMPDFExtractor(client));
        extractors.add(new ComdirectPDFExtractor(client));
        extractors.add(new CommerzbankPDFExtractor(client));
        extractors.add(new ConsorsbankPDFExtractor(client));
        extractors.add(new DABPDFExtractor(client));
        extractors.add(new DegiroPDFExtractor(client));
        extractors.add(new DeutscheBankPDFExtractor(client));
        extractors.add(new DkbPDFExtractor(client));
        extractors.add(new FinTechGroupBankPDFExtractor(client));
        extractors.add(new INGDiBaExtractor(client));
        extractors.add(new OnvistaPDFExtractor(client));
        extractors.add(new SBrokerPDFExtractor(client));
        extractors.add(new UnicreditPDFExtractor(client));
        extractors.add(new HelloBankPDFExtractor(client));

        Collections.sort(extractors, (r, l) -> r.getLabel().compareToIgnoreCase(l.getLabel()));
    }

    public void setLegacyMode(boolean isLegacyMode)
    {
        this.isLegacyMode = isLegacyMode;
    }

    @Override
    public boolean canFinish()
    {
        // allow "Finish" only on the last page
        return getContainer().getCurrentPage() == pages.get(pages.size() - 1);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        // assign files to extractors and create a page for each extractor that
        // has a file

        Map<Extractor, List<Extractor.InputFile>> extractor2files = new HashMap<>();

        if (extractors.size() == 1)
        {
            extractor2files.put(extractors.get(0), files);
        }
        else if (isLegacyMode)
        {
            Extractor e = new AssistantPDFExtractor(client, new ArrayList<>(extractors));
            extractors.add(e);
            extractor2files.put(e, files);
        }
        else
        {
            assignFilesToExtractors(extractor2files);
        }

        for (Extractor extractor : extractors)
        {
            List<Extractor.InputFile> files4extractor = extractor2files.get(extractor);
            if (files4extractor == null || files4extractor.isEmpty())
                continue;

            ReviewExtractedItemsPage page = new ReviewExtractedItemsPage(client, extractor, preferences,
                            files4extractor);
            pages.add(page);
            addPage(page);
        }

        AbstractWizardPage.attachPageListenerTo(getContainer());
    }

    private void assignFilesToExtractors(Map<Extractor, List<Extractor.InputFile>> extractor2files)
    {
        List<Extractor.InputFile> unknown = new ArrayList<>();

        for (Extractor.InputFile file : files)
        {
            PDFInputFile inputFile = (PDFInputFile) file;

            Extractor extractor = PDFImportAssistant.detectBankIdentifier(inputFile, extractors);

            if (extractor != null)
                extractor2files.computeIfAbsent(extractor, k -> new ArrayList<>()).add(inputFile);
            else
                unknown.add(inputFile);
        }

        if (!unknown.isEmpty())
        {
            Extractor e = new AssistantPDFExtractor(client, new ArrayList<>(extractors));
            extractors.add(e);
            extractor2files.put(e, unknown);
        }
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
