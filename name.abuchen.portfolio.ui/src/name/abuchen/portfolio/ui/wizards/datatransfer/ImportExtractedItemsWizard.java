package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList; 
import java.util.Map; 
import java.util.HashMap; 

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFExtractorImportAssistant;
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

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

                 // Read words from file and put into a simulated multimap
                    Map<String, List<String>> PDFasistantBankIdentifierFiles = new HashMap<String, List<String>>();
                    String BankIdentifier;
                    for (File filename : files)
                    {
                        BankIdentifier = PDFassistantDetectBankIdentifier(filename);
                        List<String> l = PDFasistantBankIdentifierFiles.get(BankIdentifier);
                        if (l == null)
                            PDFasistantBankIdentifierFiles.put(BankIdentifier, l=new ArrayList<String>());
                        l.add(filename.getAbsolutePath());
                    }

                    for (String BankIdentifierKey : PDFasistantBankIdentifierFiles.keySet())
                    {
                        Extractor extractor = createExtractor(BankIdentifierKey, client);
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
    public boolean canFinish()
    {
        if(getContainer().getCurrentPage() != ImportAssistantPages.get(ImportAssistantPages.size()-1))
            return false;
        else 
            return true;
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

    private String PDFassistantDetectBankIdentifier(File PDFfilename) throws IOException
    {

            // Loading an existing document 
            PDDocument document = PDDocument.load(PDFfilename);

            // Getting the PDDocumentInformation object
            PDDocumentInformation pdd = document.getDocumentInformation();

            // Getting the PDFTextStripper object
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            String text = textStripper.getText(document);
            document.close();
            
            // PDF import assistant - Level 1 - Allocate by PDF Author
            String PDFauthor;
            if (pdd.getAuthor() == null) PDFauthor = "";
            else PDFauthor = pdd.getAuthor();

            switch (PDFauthor)
            {

                case "Scalable Capital Vermögensverwaltung GmbH": //$NON-NLS-1$
                    return "baaderbank";
                case "consorsbank": //$NON-NLS-1$
                    return "";
                case "dab": //$NON-NLS-1$
                    return "";
                case "dkb": //$NON-NLS-1$
                    return "";
                case "ING-DiBa": //$NON-NLS-1$
                    return "ingdiba";

                default:


                    // PDF import assistant - Precheck if specific securities could cause faulty detection
                    // ISIN DE0005088108 = Baader Bank Aktie detect the bank identifier "Baader Bank" 
                    Matcher matcherISIN = Pattern.compile("DE0005088108|DE0005428007|DE000CBK1001|FR0000131104|INE007B01023|DE000FTG1111|CH0001351862|CH0001350328|CH0001354296|CH0001352720|CH0001350112|CH0318681860|CH0001343885|FR0000131104|INE007B01023").matcher( text );
                    int matcherISIN_count = 0;
                    while (matcherISIN.find())
                        matcherISIN_count++;
                    if (matcherISIN_count != 0) return ("others");

                    // PDF import assistant - Search for the bank identifier
                    Matcher matcherBankIdentifier = Pattern.compile("Baader Bank|Bank SLM AG|biw AG|BNP Paribas|C O M M E R Z B A N K|comdirect bank|Consorsbank|Cortal Consors|DAB Bank|Deutsche Bank Privat- und Geschäftskunden AG|FinTech Group Bank AG|ING-DiBa AG|S Broker AG & Co. KG|Scalable Capital|Spar + Leihkasse|UniCredit Bank AG").matcher( text );
                    String BankIdentifier = "";
                    while ( matcherBankIdentifier.find() )
                    {
                        BankIdentifier = matcherBankIdentifier.group();
                        break;
                    }
                    if (BankIdentifier == "") return ("others");
                    else
                    {
                        switch (BankIdentifier)
                        {

                            case "Baader Bank": return("baaderbank");
                            case "Bank SLM AG": return("bankslm");
                            case "biw AG": return("fintechgroupbank");
                            case "BNP Paribas": return("dab");
                            case "C O M M E R Z B A N K": return("commerzbank");
                            case "comdirect bank": return("comdirect");
                            case "Consorsbank": return("consorsbank");
                            case "Cortal Consors": return("consorsbank");
                            case "DAB Bank": return("dab");
                            case "Deutsche Bank Privat- und Geschäftskunden AG": return("db");
                            case "FinTech Group Bank AG": return("fintechgroupbank");
                            case "ING-DiBa AG": return("ingdiba");
                            case "S Broker AG & Co. KG": return("sbroker");
                            case "Scalable Capital": return("baaderbank");
                            case "Spar + Leihkasse": return("bankslm");
                            case "UniCredit Bank AG": return("unicredit");
                        }
                    }
            }
            return ("others");
    }

    private List<File> ImportDetectedBankIdentierFiles(List<String> Dateinamen)
    {
        List<File> files = new ArrayList<>();
        if (Dateinamen.isEmpty()) return(files);
        for (String file : Dateinamen)
            files.add(new File(file));
        return(files);
    }

    private Extractor createExtractor(String type, Client client) throws IOException, IllegalArgumentException
    {
        switch (type)
        {
            case "baaderbank": //$NON-NLS-1$
                return new BaaderBankPDFExtractor(client);
            case "bankslm": //$NON-NLS-1$
                return new BankSLMPDFExctractor(client);
            case "comdirect": //$NON-NLS-1$
                return new ComdirectPDFExtractor(client);
            case "commerzbank": //$NON-NLS-1$
                return new CommerzbankPDFExctractor(client);
            case "consorsbank": //$NON-NLS-1$
                return new ConsorsbankPDFExctractor(client);
            case "dab": //$NON-NLS-1$
                return new DABPDFExctractor(client);
            case "db": //$NON-NLS-1$
                return new DeutscheBankPDFExctractor(client);
            case "dkb": //$NON-NLS-1$
                return new DkbPDFExtractor(client);
            case "fintechgroupbank": //$NON-NLS-1$
                return new FinTechGroupBankPDFExtractor(client);
            case "ingdiba": //$NON-NLS-1$
                return new INGDiBaExtractor(client);
            case "onvista": //$NON-NLS-1$
                return new OnvistaPDFExtractor(client);
            case "sbroker": //$NON-NLS-1$
                return new SBrokerPDFExtractor(client);
            case "unicredit": //$NON-NLS-1$
                return new UnicreditPDFExtractor(client);
            default:
                return new PDFExtractorImportAssistant(client);
        }
    }

 }
