package name.abuchen.portfolio.pdfbox3;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Keeps a single {@link PDDocument} and {@link PDFRenderer} open so that
 * repeated page renders (e.g. paging through a document in a viewer) do not
 * re-parse the file from disk on every call. Not thread-safe: PDFBox's document
 * and renderer must be used from a single thread; callers are responsible for
 * serializing access and for closing the renderer when done.
 */
public class PDFBox3Renderer implements Closeable
{
    private final PDDocument document;
    private final PDFRenderer renderer;

    public PDFBox3Renderer(File file) throws IOException
    {
        this.document = Loader.loadPDF(file);
        this.renderer = new PDFRenderer(document);
    }

    public int getPageCount()
    {
        return document.getNumberOfPages();
    }

    public byte[] renderPage(int pageIndex, float dpi) throws IOException
    {
        var image = renderer.renderImageWithDPI(pageIndex, dpi);
        var bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos); //$NON-NLS-1$
        return bos.toByteArray();
    }

    @Override
    public void close() throws IOException
    {
        document.close();
    }
}
