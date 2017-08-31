package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;  
import java.util.regex.Matcher;  

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Client;

import org.apache.pdfbox.pdmodel.PDDocument;  
import org.apache.pdfbox.pdmodel.PDDocumentInformation;  
import org.apache.pdfbox.util.PDFTextStripper;  

import name.abuchen.portfolio.datatransfer.pdf.AssistantPDFExtractor;
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

public class PDFImportAssistant
{

    public PDFImportAssistant()
    {

    }

    public static Extractor createExtractor(String type, Client client) throws IOException, IllegalArgumentException
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
                return new AssistantPDFExtractor(client);
        }
    }

    public static String DetectBankIdentifier(File PDFfilename) throws IOException  
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
            case "Consorsbank": //$NON-NLS-1$  
                return "consorsbank";  
            case "Computershare Communication Services GmbH": //$NON-NLS-1$  
                return "dab";  
            case "DKB AG": //$NON-NLS-1$  
                return "dkb";  
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

}
