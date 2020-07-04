package name.abuchen.portfolio.model;

import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.util.ImageUtil;

public final class ImageManager
{
    private static final float DPI_DEFAULT = 96.0f;
    
    private HashMap<String, Image> imageCache = new HashMap<String, Image>();
    private static ImageManager instance = new ImageManager();
    public static ImageManager instance() 
    {
        return instance;
    }
    
    private ImageManager() 
    { 
        
    }
    
    public Image getImage(Attributable target, AttributeType attr, float dpi_current) 
    {
        return getImage(target, attr, 16, 16, dpi_current);
    }
    
    public Image getImage(Attributable target, AttributeType attr, int width, int height, float dpi_current) 
    {
        if(target != null && target.getAttributes().exists(attr)) 
        {
            if(attr.getConverter() instanceof ImageConverter) 
            {
                float dpi_sclale = getDPIScale(dpi_current);
                height*=dpi_sclale;
                width*=dpi_sclale;
                String imgString = target.getAttributes().get(attr).toString();
                String imgKey = imgString + width + height;
                synchronized (imageCache)
                {
                    Image img = imageCache.getOrDefault(imgKey, null);
                    if(img != null) return img;
                    
                    Image fullImage = ImageUtil.toImage(imgString);
                    if(fullImage != null) 
                    {
                        Rectangle bounds = fullImage.getBounds();
                        if(bounds.height != height || bounds.width != width) 
                        {
                            img = ImageUtil.resize(fullImage, width, height);
                        }
                        else
                        {
                            img = fullImage;
                        }
                        imageCache.put(imgKey, img);
                        return img;
                    }
                }
            }
        }
        return null;
    }
    
    private float getDPIScale(float dpi_current)
    {
        return dpi_current / DPI_DEFAULT;
    }
}
