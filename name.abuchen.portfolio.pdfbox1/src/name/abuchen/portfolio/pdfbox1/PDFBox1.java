package name.abuchen.portfolio.pdfbox1;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.osgi.framework.FrameworkUtil;

public class PDFBox1
{
    public String convertToText(File file) throws IOException
    {
        try (PDDocument document = PDDocument.load(file))
        {
            boolean isProtected = document.isEncrypted();
            if (isProtected)
            {
                document.decrypt(""); //$NON-NLS-1$
                document.setAllSecurityToBeRemoved(true);
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            var text = textStripper.getText(document);

            // replace horizontal whitespace characters by normal whitespace
            text = text.replaceAll("\\h", " "); //$NON-NLS-1$ //$NON-NLS-2$

            // without carriage returns
            return text.replace("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$

        }
        catch (CryptographyException e)
        {
            throw new IOException(e);
        }
    }

    public String getPDFBoxVersion()
    {
        return FrameworkUtil.getBundle(PDDocument.class).getVersion().toString();
    }
}
