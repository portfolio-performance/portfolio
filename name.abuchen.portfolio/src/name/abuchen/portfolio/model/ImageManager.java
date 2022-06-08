package name.abuchen.portfolio.model;

import java.util.HashMap;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.util.ImageUtil;

public final class ImageManager
{
    private HashMap<String, Image> imageCache = new HashMap<>();
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
        int xOffset = Platform.OS_WIN32.equals(Platform.getOS()) ? 1 : 0;
        return getImage(target, attr, xOffset, 16, 16);
    }

    public Image getImage(Attributable target, AttributeType attr, int xOffset, int width, int height)
    {
        if (target != null && target.getAttributes().exists(attr) && attr.getConverter() instanceof ImageConverter)
        {
            Object imgObject = target.getAttributes().get(attr);
            if (imgObject == null)
                return null;

            String imgString = String.valueOf(imgObject);
            String imgKey = imgString + width + height + xOffset;
            synchronized (imageCache)
            {
                Image img = imageCache.getOrDefault(imgKey, null);
                if (img != null)
                    return img;

                img = ImageUtil.toImage(imgString, xOffset, width, height);
                if (img != null)
                {
                    imageCache.put(imgKey, img);
                    return img;
                }
            }
        }
        return null;
    }
}
