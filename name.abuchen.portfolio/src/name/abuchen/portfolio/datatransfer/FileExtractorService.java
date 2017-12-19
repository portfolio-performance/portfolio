package name.abuchen.portfolio.datatransfer;

import java.util.Map;

public interface FileExtractorService
{
    void registerExtractor(Extractor extractor);

    Map<String, Map<String, Extractor>> getAll();

    Map<String, Extractor> getExtractors(String type);
    
    Extractor getExtractor(String type, String extractorId);

}
