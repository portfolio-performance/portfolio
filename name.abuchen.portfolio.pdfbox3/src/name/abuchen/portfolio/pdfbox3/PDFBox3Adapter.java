package name.abuchen.portfolio.pdfbox3;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.osgi.framework.FrameworkUtil;

public class PDFBox3Adapter
{
    public String convertToText(File file) throws IOException
    {
        try (PDDocument document = Loader.loadPDF(file))
        {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            return textStripper.getText(document);
        }
    }

    public String getPDFBoxVersion()
    {
        return FrameworkUtil.getBundle(PDDocument.class).getVersion().toString();
    }
}
