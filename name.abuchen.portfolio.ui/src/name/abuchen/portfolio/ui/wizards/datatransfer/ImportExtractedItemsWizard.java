package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException; 
import java.util.List;
import java.util.ArrayList;   
import java.util.Map;   
import java.util.HashMap; 

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

    ArrayList <ReviewExtractedItemsPage> ImportAssistantPages = new ArrayList <ReviewExtractedItemsPage>();

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
    public boolean canFinish()
    {
        if(getContainer().getCurrentPage() != ImportAssistantPages.get(ImportAssistantPages.size()-1))
            return false;
        else
            return true;
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        try
        {
            switch (extractor.getLabel())
            {

                // PDF auto import assistant page generation
                case "pdfimportassistant": //$NON-NLS-1$

                    new PDFImportAssistant();
                    // Read words from file and put into a simulated multimap
                    Map<String, List<String>> PDFasistantBankIdentifierFiles = new HashMap<String, List<String>>();
                    String BankIdentifier;
                    for (File filename : files)
                    {
                        BankIdentifier = PDFImportAssistant.DetectBankIdentifier(filename);
                        List<String> l = PDFasistantBankIdentifierFiles.get(BankIdentifier);
                        if (l == null) PDFasistantBankIdentifierFiles.put(BankIdentifier, l=new ArrayList<String>());
                        l.add(filename.getAbsolutePath());
                    }

                    for (String BankIdentifierKey : PDFasistantBankIdentifierFiles.keySet())
                    {
                        Extractor extractor = PDFImportAssistant.createExtractor(BankIdentifierKey, client);
                        ImportAssistantPages.add(new ReviewExtractedItemsPage(client, extractor, preferences, ImportDetectedBankIdentierFiles(PDFasistantBankIdentifierFiles.get(BankIdentifierKey)), BankIdentifierKey));
                    }

                    break;

                // fall back if auto importer is not used
                default:
                    ImportAssistantPages.add(new ReviewExtractedItemsPage(client, extractor, preferences, files, "extracted"));
                    break;
            }
            if (ImportAssistantPages.size() > 0) {
                for (int PDFAssistantPageID = 0; PDFAssistantPageID < ImportAssistantPages.size(); PDFAssistantPageID++) addPage(ImportAssistantPages.get(PDFAssistantPageID));
            }
            AbstractWizardPage.attachPageListenerTo(getContainer());
        }
        catch (IOException ex)
        {
        }

    }

    @Override
    public boolean performFinish()
    {

        if (ImportAssistantPages.size() > 0)
        {
            boolean isDirty = false;
            for (int PDFAssistantPageID = 0; PDFAssistantPageID < ImportAssistantPages.size(); PDFAssistantPageID++)
            {

                ImportAssistantPages.get(PDFAssistantPageID).afterPage();
                InsertAction action = new InsertAction(client);
                action.setConvertBuySellToDelivery(ImportAssistantPages.get(PDFAssistantPageID).doConvertToDelivery());

                for (ExtractedEntry entry : ImportAssistantPages.get(PDFAssistantPageID).getEntries())
                {
                    if (entry.isImported())
                    {
                        entry.getItem().apply(action, ImportAssistantPages.get(PDFAssistantPageID));
                        isDirty = true;
                    }
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
        }

        return true;
    }

    private List<File> ImportDetectedBankIdentierFiles(List<String> Dateinamen)
    {
        List<File> files = new ArrayList<>();
        if (Dateinamen.isEmpty()) return(files);
        for (String file : Dateinamen)
            files.add(new File(file));
        return(files);
    }

}
