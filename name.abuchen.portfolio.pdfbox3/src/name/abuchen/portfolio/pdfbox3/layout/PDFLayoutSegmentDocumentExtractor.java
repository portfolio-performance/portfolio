package name.abuchen.portfolio.pdfbox3.layout;

import java.io.File;
import java.io.IOException;

public final class PDFLayoutSegmentDocumentExtractor
{
    public PDFLayoutSegmentDocument extract(File file) throws IOException
    {
        return new PDFLayoutBcbcDebugTextExtractor().extractDocument(file);
    }
}
