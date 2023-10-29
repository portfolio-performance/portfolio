package name.abuchen.portfolio.model;

import java.util.HashMap;

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

    /**
     * Retrieves an image associated with the given Attributable and
     * AttributeType. If not found, a default image is returned.
     *
     * @return The retrieved image or null if not found.
     */
    public Image getImage(Attributable target, AttributeType attr)
    {
        return getImage(target, attr, 0, 16, 16);
    }

    /**
     * Retrieves an image associated with the given Attributable, AttributeType,
     * and additional parameters. If not found, a default image is returned.
     *
     * @return The retrieved image or null if not found.
     */
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
