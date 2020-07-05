package name.abuchen.portfolio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.ImageLoader;

public class ImageUtil
{
    private static class ZoomingImageDataProvider implements ImageDataProvider
    {
        private int logicalWidth;
        private int logicalHeight;
        private byte[] data;
        private ImageData fullSize;
        private HashMap<Integer, ImageData> zoomLevels = new HashMap<Integer, ImageData>();
        public ZoomingImageDataProvider(byte[] data, int logicalWidth, int logicalHeight)
        {
            this.data = data;
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
        }
        
        @Override
        public ImageData getImageData (int zoom)
        {
            if(fullSize == null)
            {
                fullSize = ImageUtil.toImageData(data);
            }
            
            ImageData zoomed = zoomLevels.getOrDefault(zoomLevels, null);
            if(zoomed == null)
            {
                float scaleW = 1f / fullSize.width * logicalWidth * (zoom / 100f);
                float scaleH = 1f / fullSize.height * logicalHeight * (zoom / 100f);
                zoomed = ImageUtil.resize(fullSize, (int)(fullSize.width * scaleW), (int)(fullSize.height * scaleH));
                
                zoomLevels.put(zoom, zoomed);
            }
            
            return zoomed;
        }
    }
    
    public static Image toImage(String value, int logicalWidth, int logicalHeight)
    {
        if(value == null || value.length() == 0)
            return null;

        try 
        {
            int splitPos = value.indexOf(',');
            if(splitPos >= 0 && splitPos < value.length() - 1) value = value.substring(splitPos + 1);
            byte[] buff = Base64.getDecoder().decode(value);
            return toImage(buff, logicalWidth, logicalHeight);
        }
        catch (Exception ex) 
        {
            return null;
        }
    }
    
    public static Image toImage(byte[] value, int logicalWidth, int logicalHeight)
    {
        if(value == null || value.length == 0)
            return null;

        return new Image(null, new ZoomingImageDataProvider(value, logicalWidth, logicalHeight));
    }
    
    public static ImageData toImageData(byte[] value)
    {
        if(value == null || value.length == 0)
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
    
    public static byte[] encode(ImageData image)
    {
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] {image};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        loader.save(bos, 5); //org.eclipse.swt.SWT.IMAGE_PNG;
        
        return bos.toByteArray();
    }

    public static String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException
    {
        byte[] data = Files.readAllBytes(Paths.get(filename));
        ImageData imgData = ImageUtil.toImageData(data);
        if(imgData.width > maxWidth || imgData.height > maxHeight) 
        {
            imgData = ImageUtil.resize(imgData, maxWidth, maxHeight);
            data = ImageUtil.encode(imgData);
        }
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static ImageData resize(ImageData image, int maxWidth, int maxHeight) 
    {
        int newHeight = maxHeight;
        int newWidth = (image.width * newHeight) / image.height;
        if (newWidth > maxWidth)
         {
           newWidth = maxWidth;
           newHeight = (image.height * newWidth) / image.width;
         }
        
        Image scaled = new Image(null, newWidth, newHeight);
        GC gc = new GC(scaled);
        gc.setAntialias(1); //org.eclipse.swt.SWT.ON
        gc.setInterpolation(2); //org.eclipse.swt.SWT.HIGH
        gc.setAlpha(image.alpha);
        Image tmp = new Image(null, image);
        gc.drawImage(tmp, 0, 0, image.width, image.height, 0, 0, newWidth, newHeight);
        gc.dispose();
        tmp.dispose();
        return scaled.getImageData();
    }
}
