package name.abuchen.portfolio.util;

import java.io.IOException;

import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.util.impl.ImageUtilImageIOImpl;
import name.abuchen.portfolio.util.impl.ImageUtilSWTImpl;

public abstract class ImageUtil
{
    public static final String BASE64PREFIX = "data:image/png;base64,"; //$NON-NLS-1$

    private static ImageUtil instance;

    public static ImageUtil instance()
    {
        if (instance == null)
        {
            try
            {
                instance = new ImageUtilImageIOImpl();
            }
            catch (NoClassDefFoundError | UnsatisfiedLinkError e)
            {
                // if the ImageIO library is not available, then fallback to the
                // SWT

                // On Linux, the ImageIO library might not be available if the
                // application runs with a headless JDK installation.

                // The SWT image libraries do not handle transparency very well.
                // That is the reason to prefer the ImageIO library from Java.

                PortfolioLog.warning(
                                "ImageIO library not available. Are you running against a headless JDK installation?"); //$NON-NLS-1$

                instance = new ImageUtilSWTImpl();
            }
        }

        return instance;
    }

    public abstract Image toImage(String value, int logicalWidth, int logicalHeight);

    public abstract String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException;
}
