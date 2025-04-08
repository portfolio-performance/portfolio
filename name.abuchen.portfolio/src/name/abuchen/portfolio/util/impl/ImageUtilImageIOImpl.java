package name.abuchen.portfolio.util.impl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.ImageLoader;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.util.ImageUtil;

public class ImageUtilImageIOImpl extends ImageUtil
{
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

    public ImageUtilImageIOImpl()
    {
        // check for ImageIO availability during instance creation
        ImageIO.getReaderFormatNames();
    }

    @Override
    public Image toImage(String value, int logicalWidth, int logicalHeight)
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
            if (imgData == null)
                return null;

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

    @Override
    public String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException
    {
        BufferedImage imgData = null;

        try (ImageInputStream input = ImageIO.createImageInputStream(new File(filename)))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext())
                return null;

            ImageReader reader = readers.next();

            try
            {
                reader.setInput(input);
                if (reader.getFormatName().equalsIgnoreCase("ico")) //$NON-NLS-1$
                {
                    // ico files have multiple resolutions. We pick the largest
                    // one with the best pixelSize that fits into maxWidth and
                    // maxHeight
                    var numImages = reader.getNumImages(true);
                    if (numImages <= 0)
                        return null;

                    int size = 0;
                    int pixelSize = 0;

                    for (int ii = 0; ii < numImages; ii++)
                    {
                        BufferedImage icon = reader.read(ii);
                        if ((icon.getWidth() > size
                                        || (icon.getWidth() == size && icon.getColorModel().getPixelSize() > pixelSize))
                                        && icon.getWidth() <= maxWidth)
                        {
                            imgData = icon;
                            size = icon.getWidth();
                            pixelSize = icon.getColorModel().getPixelSize();
                        }
                    }
                }
                else
                {
                    imgData = reader.read(0);
                }

            }
            finally
            {
                reader.dispose();
            }
        }

        if (imgData == null)
            return null;

        if (imgData.getWidth() > maxWidth || imgData.getHeight() > maxHeight)
        {
            BufferedImage scaledImage = scaleImage(imgData, maxWidth, maxHeight, true);
            imgData = scaledImage;
        }
        return BASE64PREFIX + Base64.getEncoder().encodeToString(encode(imgData));
    }

    private static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight, boolean preserveRatio)
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
