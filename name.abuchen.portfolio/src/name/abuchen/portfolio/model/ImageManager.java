package name.abuchen.portfolio.model;

import java.util.HashMap;

import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.util.ImageUtil;

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
        return getImage(target, attr, 16, 16);
    }

    public Image getImage(Attributable target, AttributeType attr, int width, int height)
    {
        if (target != null && target.getAttributes().exists(attr))
        {
            if (attr.getConverter() instanceof ImageConverter)
            {
                String imgString = target.getAttributes().get(attr).toString();
                String imgKey = imgString + width + height;
                synchronized (imageCache)
                {
                    Image img = imageCache.getOrDefault(imgKey, null);
                    if (img != null)
                        return img;

                    img = ImageUtil.toImage(imgString, width, height);
                    if (img != null)
                    {
                        imageCache.put(imgKey, img);
                        return img;
                    }
                }
            }
        }
        return null;
    }
}
