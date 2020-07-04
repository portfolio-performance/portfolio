package name.abuchen.portfolio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;

public class ImageUtil
{
    public static Image toImage(String value)
    {
        if(value == null || value.length() == 0)
            return null;

        try 
        {
            int splitPos = value.indexOf(',');
            if(splitPos >= 0 && splitPos < value.length() - 1) value = value.substring(splitPos + 1);
            byte[] buff = Base64.getDecoder().decode(value);
            return toImage(buff);
        }
        catch (Exception ex) 
        {
            return null;
        }
    }
    
    public static Image toImage(byte[] value)
    {
        if(value == null || value.length == 0)
            return null;

        try 
        {
            ImageLoader loader = new ImageLoader();
            ByteArrayInputStream bis = new ByteArrayInputStream(value);
            ImageData[] imgArr = loader.load(bis);
            try
            {
                bis.close();
            }
            catch (IOException e) 
            { 
                
            }
            return new Image(null, imgArr[0]);
        }
        catch (Exception ex) 
        {
            return null;
        }
    }
    
    public static byte[] encode(Image image)
    {
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] {image.getImageData()};
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        loader.save(bos, 5); //org.eclipse.swt.SWT.IMAGE_PNG;
        
        return bos.toByteArray();
    }

    public static String loadAndPrepare(String filename, int maxWidth, int maxHeight) throws IOException
    {
        byte[] data = Files.readAllBytes(Paths.get(filename));
        Image img = ImageUtil.toImage(data);
        if(img.getBounds().width > maxWidth || img.getBounds().height > maxHeight) 
        {
            img = ImageUtil.resize(img, maxWidth, maxHeight);
            data = ImageUtil.encode(img);
        }
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static Image resize(Image image, int maxWidth, int maxHeight) 
    {
        Rectangle bounds = image.getBounds();
        int newHeight = maxHeight;
        int newWidth = (bounds.width * newHeight) / bounds.height;
        if (newWidth > maxWidth)
         {
           newWidth = maxWidth;
           newHeight = (bounds.height * newWidth) / bounds.width;
         }
        
        Image scaled = new Image(null, newWidth, newHeight);
        GC gc = new GC(scaled);
        gc.setAntialias(1); //org.eclipse.swt.SWT.ON
        gc.setInterpolation(2); //org.eclipse.swt.SWT.HIGH
        gc.setAlpha(image.getImageData().alpha);
        gc.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, newWidth, newHeight);
        gc.dispose();
        image.dispose(); // don't forget about me!
        return scaled;
    }
}
