package name.abuchen.portfolio.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.ImageLoader;

import name.abuchen.portfolio.PortfolioLog;

public class ImageUtil
{
    private static final String BASE64PREFIX = "data:image/png;base64,"; //$NON-NLS-1$

    private static class ZoomingImageDataProvider implements ImageDataProvider
    {
        private int logicalWidth;
        private int logicalHeight;
        private BufferedImage fullSize;
        private HashMap<Integer, ImageData> zoomLevels = new HashMap<>();

        public ZoomingImageDataProvider(BufferedImage fullSize, int logicalWidth, int logicalHeight)
        {
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
            this.fullSize = fullSize;
        }

        @Override
        public ImageData getImageData(int zoom)
        {
            try
            {
                ImageData imageData = zoomLevels.get(zoom);
                if (imageData != null)
                    return imageData;

                int zoomedWidth = Math.round(logicalWidth * (zoom / 100f));
                int zoomedHeight = Math.round(logicalHeight * (zoom / 100f));

                var scaledBufferedImage = scaleImage(fullSize, zoomedWidth, zoomedHeight, false);
                imageData = toImageData(encode(scaledBufferedImage));

                zoomLevels.put(zoom, imageData);

                return imageData;
            }
            catch (IOException e)
            {
                PortfolioLog.error(e);
                return null;
            }
        }

        private ImageData toImageData(byte[] value)
        {
            if (value == null || value.length == 0)
                return null;

            try
            {
                ImageLoader loader = new ImageLoader();
                ByteArrayInputStream bis = new ByteArrayInputStream(value);
                ImageData[] imgArr = loader.load(bis);
                return imgArr[0];
            }
            catch (Exception ex)
            {
                return null;
            }
        }

    }

    private ImageUtil()
    {
    }

    /**
     * @param value
     * @param xOffset
     *            Additional transparent offset. Width of the resulting image is
     *            (xOffset + maxWidth)
     * @param logicalWidth
     * @param logicalHeight
     * @return
     */
    public static Image toImage(String value, int logicalWidth, int logicalHeight)
    {
        if (value == null || value.length() == 0)
            return null;

        if (!value.startsWith(BASE64PREFIX))
            return null;

        try
        {
            int splitPos = value.indexOf(',');
            if (splitPos >= 0 && splitPos < value.length() - 1)
                value = value.substring(splitPos + 1);
            byte[] buff = Base64.getDecoder().decode(value);
            if (buff == null || buff.length == 0)
                return null;

            BufferedImage imgData = ImageIO.read(new ByteArrayInputStream(buff));

            return new Image(null, new ZoomingImageDataProvider(imgData, logicalWidth, logicalHeight));
        }
        catch (Exception ex)
        {
            PortfolioLog.error(ex);
            return null;
        }
    }

    private static byte[] encode(BufferedImage image) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", bos); //$NON-NLS-1$
        bos.close();
        return bos.toByteArray();
    }

    public static String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException
    {
        BufferedImage imgData = ImageIO.read(new File(filename));

        if (imgData.getWidth() > maxWidth || imgData.getHeight() > maxHeight)
        {
            BufferedImage scaledImage = scaleImage(imgData, maxWidth, maxHeight, true);
            imgData = scaledImage;
        }
        return BASE64PREFIX + Base64.getEncoder().encodeToString(ImageUtil.encode(imgData));
    }

    public static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight, boolean preserveRatio)
    {
        if (image.getWidth() == maxWidth && image.getHeight() == maxHeight)
            return image;

        int newHeight = maxHeight;
        int newWidth = (image.getWidth() * newHeight) / image.getHeight();
        if (newWidth > maxWidth)
        {
            newWidth = maxWidth;
            newHeight = (image.getHeight() * newWidth) / image.getWidth();
        }

        int imageWidth = preserveRatio ? newWidth : maxWidth;
        int imageHeight = preserveRatio ? newHeight : maxHeight;
        int posX = preserveRatio ? 0 : (maxWidth - newWidth) / 2;
        int posY = preserveRatio ? 0 : (maxHeight - newHeight) / 2;

        if (posX + newWidth > imageWidth)
            newWidth = imageWidth - posX;
        if (posY + newHeight > imageHeight)
            newWidth = imageHeight - posY;

        BufferedImage scaledImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, posX, posY, newWidth, newHeight, null);
        g2d.dispose();
        return scaledImage;
    }
}
