package name.abuchen.portfolio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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
        private ImageData fullSize;
        private HashMap<Integer, ImageData> zoomLevels = new HashMap<Integer, ImageData>();

        public ZoomingImageDataProvider(byte[] data, int logicalWidth, int logicalHeight)
        {
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
            this.fullSize = ImageUtil.toImageData(data);
        }

        @Override
        public ImageData getImageData(int zoom)
        {
            ImageData imageData = zoomLevels.get(zoom);
            if (imageData != null)
                return imageData;

            float scaleW = 1f / fullSize.width * logicalWidth * (zoom / 100f);
            float scaleH = 1f / fullSize.height * logicalHeight * (zoom / 100f);

            imageData = ImageUtil.resize(fullSize, (int) (fullSize.width * scaleW), (int) (fullSize.height * scaleH),
                            false);

            zoomLevels.put(zoom, imageData);

            return imageData;
        }
    }

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

            return new Image(null, new ZoomingImageDataProvider(buff, logicalWidth, logicalHeight));
        }
        catch (Exception ex)
        {
            PortfolioLog.error(ex);
            return null;
        }
    }

    private static ImageData toImageData(byte[] value)
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

    private static byte[] encode(ImageData image)
    {
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { image };
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        loader.save(bos, SWT.IMAGE_PNG);

        return bos.toByteArray();
    }

    public static String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException
    {
        byte[] data = Files.readAllBytes(Paths.get(filename));
        ImageData imgData = ImageUtil.toImageData(data);
        if (imgData.width > maxWidth || imgData.height > maxHeight)
        {
            imgData = ImageUtil.resize(imgData, maxWidth, maxHeight, true);
            data = ImageUtil.encode(imgData);
        }
        return BASE64PREFIX + Base64.getEncoder().encodeToString(data);
    }

    private static ImageData resize(ImageData image, int maxWidth, int maxHeight, boolean preserveRatio)
    {
        if (image.width == maxWidth && image.height == maxHeight)
            return image;

        int newHeight = maxHeight;
        int newWidth = (image.width * newHeight) / image.height;
        if (newWidth > maxWidth)
        {
            newWidth = maxWidth;
            newHeight = (image.height * newWidth) / image.width;
        }

        int imageWidth = preserveRatio ? newWidth : maxWidth;
        int imageHeight = preserveRatio ? newHeight : maxHeight;
        int posX = preserveRatio ? 0 : (maxWidth - newWidth) / 2;
        int posY = preserveRatio ? 0 : (maxHeight - newHeight) / 2;

        Image background = new Image(null, imageWidth, imageHeight);
        ImageData imageData = background.getImageData();
        imageData.transparentPixel = imageData.getPixel(0, 0);
        background.dispose();

        Image canvas = new Image(null, imageData);

        GC gc = new GC(canvas);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);

        Image source = new Image(null, image);
        gc.drawImage(source, 0, 0, image.width, image.height, posX, posY, newWidth, newHeight);
        gc.dispose();
        source.dispose();

        ImageData answer = canvas.getImageData();
        canvas.dispose();

        return answer;
    }
}
