package name.abuchen.portfolio.model;

import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;

public final class ImageManager
{
    private HashMap<String, Image> imageCache = new HashMap<String, Image>();
    private static ImageManager instance = new ImageManager();
    public static ImageManager instance() 
    {
        return instance;
    }
    
    private ImageManager() 
    { 
        
    }
    
    public Image getImage(Attributable target, AttributeType attr) 
    {
        return getImage(target, attr, 32, 32);
    }
    
    public Image getImage(Attributable target, AttributeType attr, int width, int height) 
    {
        if(target != null && target.getAttributes().exists(attr)) 
        {
            if(attr.getConverter() instanceof ImageConverter) 
            {
                String imgString = target.getAttributes().get(attr).toString();
                synchronized (imageCache)
                {
                    Image img = imageCache.getOrDefault(imgString, null);
                    if(img != null) return img;
                    
                    ImageConverter imgConv = (ImageConverter)attr.getConverter();
                    Image fullImage = imgConv.toImage(imgString);
                    if(fullImage != null) 
                    {
                        Rectangle bounds = fullImage.getBounds();
                        if(bounds.height != height || bounds.width != width) 
                        {
                            img = ImageConverter.resize(fullImage, width, height);
                        }
                        else
                        {
                            img = fullImage;
                        }
                        imageCache.put(imgString, img);
                        return img;
                    }
                }
            }
        }
        return null;
    }
}
