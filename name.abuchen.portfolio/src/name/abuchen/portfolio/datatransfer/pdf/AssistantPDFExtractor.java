package name.abuchen.portfolio.datatransfer.pdf;

import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Client;

public class AssistantPDFExtractor extends AbstractPDFExtractor
{
    private List<Extractor> availableExtractors;

    public AssistantPDFExtractor(Client client, List<Extractor> availableExtractors)
    {
        super(client);
        this.availableExtractors = availableExtractors;
    }

    @Override
    public String getLabel()
    {
        return "pdfimportassistant"; //$NON-NLS-1$
    }

    public List<Extractor> getAvailableExtractors()
    {
        return availableExtractors;
    }

}
