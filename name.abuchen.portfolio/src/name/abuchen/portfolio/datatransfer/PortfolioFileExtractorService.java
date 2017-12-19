package name.abuchen.portfolio.datatransfer;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;

import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.BankSLMPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.CommerzbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.ConsorsbankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExctractor;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FinTechGroupBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.HelloBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaExtractor;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.datatransfer.xml.IBFlexStatementExtractor;

@Component(service = FileExtractorService.class)
public class PortfolioFileExtractorService implements FileExtractorService
{
    private SortedMap<String, Map<String, Extractor>> extractors = new TreeMap<>();

    public PortfolioFileExtractorService()
    {
        registerExtractor(new BaaderBankPDFExtractor());
        registerExtractor(new BankSLMPDFExctractor());
        registerExtractor(new ComdirectPDFExtractor());
        registerExtractor(new CommerzbankPDFExctractor());
        registerExtractor(new ConsorsbankPDFExctractor());
        registerExtractor(new DABPDFExctractor());
        registerExtractor(new DegiroPDFExtractor());
        registerExtractor(new DeutscheBankPDFExctractor());
        registerExtractor(new DkbPDFExtractor());
        registerExtractor(new FinTechGroupBankPDFExtractor());
        registerExtractor(new INGDiBaExtractor());
        registerExtractor(new OnvistaPDFExtractor());
        registerExtractor(new SBrokerPDFExtractor());
        registerExtractor(new UnicreditPDFExtractor());
        registerExtractor(new HelloBankPDFExtractor());
        
        registerExtractor(new IBFlexStatementExtractor());
    }

    public void registerExtractor(Extractor extractor)
    {
        String extractorType = extractor.getFileExtension().toLowerCase();
        Map<String, Extractor> map = extractors.get(extractorType);
        if (map == null)
        {
            map = new TreeMap<>();
            extractors.put(extractorType, map);
        }
        map.put(extractor.getLabel(), extractor);
    }

    @Override
    public Map<String, Map<String, Extractor>> getAll()
    {
        return extractors;
    }

    public Map<String, Extractor> getExtractors(String type)
    {
        Map<String, Extractor> map = extractors.get(type);
        if (map == null)
            return Collections.emptyMap();
        return map;
    }

    @Override
    public Extractor getExtractor(String type, String extractorId)
    {
        Map<String, Extractor> map = extractors.get(type);
        if (map != null)
            return map.get(extractorId);
        return null;
    }
}
