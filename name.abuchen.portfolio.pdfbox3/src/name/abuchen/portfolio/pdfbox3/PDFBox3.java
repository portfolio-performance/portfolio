package name.abuchen.portfolio.pdfbox3;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.osgi.framework.FrameworkUtil;

public class PDFBox3
{
    public String convertToText(File file) throws IOException
    {
        try (PDDocument document = Loader.loadPDF(file))
        {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            var text = textStripper.getText(document);

            // replace horizontal whitespace characters by normal whitespace
            text = text.replaceAll("\\h", " "); //$NON-NLS-1$ //$NON-NLS-2$

            // without carriage returns
            return text.replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$

        }
    }

    public String getPDFBoxVersion()
    {
        return FrameworkUtil.getBundle(PDDocument.class).getVersion().toString();
    }

}
