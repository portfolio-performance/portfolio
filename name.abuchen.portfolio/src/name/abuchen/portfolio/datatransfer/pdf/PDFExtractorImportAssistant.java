package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.model.Client;

public class PDFExtractorImportAssistant extends AbstractPDFExtractor
{

    public PDFExtractorImportAssistant(Client client) throws IOException
    {

        super(client);

    }

    @Override
    public String getLabel()
    {
        return "pdfimportassistant"; //$NON-NLS-1$
    }

}