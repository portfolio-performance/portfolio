package name.abuchen.portfolio.model;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.util.ImageUtil;

public final class ImageManager // NOSONAR
{
    public static final int RETIRED_ALPHA = 125;

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
        return getImage(target, attr, false);
    }

    /**
     * Retrieves an image associated with the given Attributable and
     * AttributeType. If not found, a default image is returned.
     *
     * @return The retrieved image or null if not found.
     */
    public Image getImage(Attributable target, AttributeType attr, boolean disabled)
    {
        return getImage(target, attr, 16, 16, disabled);
    }

    /**
     * Retrieves an image associated with the given Attributable, AttributeType,
     * and additional parameters. If not found, a default image is returned.
     *
     * @return The retrieved image or null if not found.
     */
    public Image getImage(Attributable target, AttributeType attr, int width, int height)
    {
        return getImage(target, attr, width, height, false);
    }

    /**
     * Retrieves an image associated with the given Attributable, AttributeType,
     * and additional parameters. If not found, a default image is returned.
     *
     * @return The retrieved image or null if not found.
     */
    public Image getImage(Attributable target, AttributeType attr, int width, int height, boolean disabled)
    {
        return getImage(target, attr, width, height, disabled, null);
    }

    public Image getImageWithAlpha(Attributable target, AttributeType attr, int alpha)
    {
        return getImageWithAlpha(target, attr, 16, 16, alpha);
    }

    public Image getImageWithAlpha(Attributable target, AttributeType attr, int width, int height, int alpha)
    {
        return getImage(target, attr, width, height, false, Integer.valueOf(alpha));
    }

    private Image getImage(Attributable target, AttributeType attr, int width, int height, boolean disabled, Integer alpha)
    {
        if (target != null && target.getAttributes().exists(attr) && attr.getConverter() instanceof ImageConverter)
        {
            Object imgObject = target.getAttributes().get(attr);
            if (imgObject == null)
                return null;

            String imgString = String.valueOf(imgObject);
            String sourceKey = createImageKey(imgString, width, height, "normal"); //$NON-NLS-1$
            String imgKey = alpha != null ? createImageKey(imgString, width, height, "alpha:" + alpha) //$NON-NLS-1$
                            : disabled ? createImageKey(imgString, width, height, "disabled") : sourceKey; //$NON-NLS-1$

            synchronized (imageCache)
            {
                Image img = imageCache.getOrDefault(imgKey, null);
                if (img != null)
                    return img;

                Image source = getOrCreateSourceImage(imgString, sourceKey, width, height);
                if (source == null)
                    return null;

                if (alpha != null)
                    img = getAlphaVersion(source, alpha.intValue());
                else if (disabled)
                    img = getDisabledVersion(source);
                else
                    img = source;

                imageCache.put(imgKey, img);
                return img;
            }
        }
        return null;
    }

    private String createImageKey(String image, int width, int height, String effect)
    {
        return image + "|" + width + "x" + height + "|" + effect; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private Image getOrCreateSourceImage(String image, String sourceKey, int width, int height)
    {
        Image source = imageCache.getOrDefault(sourceKey, null);
        if (source != null)
            return source;

        source = ImageUtil.instance().toImage(image, width, height);
        if (source != null)
            imageCache.put(sourceKey, source);

        return source;
    }

    public static Image getDisabledVersion(Image img)
    {
        return new Image(null, img, SWT.IMAGE_DISABLE);
    }

    public static Image getAlphaVersion(Image image, int alpha)
    {
        return new Image(null, getAlphaVersion(image.getImageData(), alpha));
    }

    public static ImageData getAlphaVersion(ImageData source, int alpha)
    {
        if (alpha < 0 || alpha > 255)
            throw new IllegalArgumentException("Alpha must be between 0 and 255"); //$NON-NLS-1$

        ImageData imageData = (ImageData) source.clone();
        imageData.alpha = -1;
        imageData.alphaData = new byte[source.width * source.height];

        float factor = alpha / 255f;
        for (int y = 0; y < source.height; y++)
        {
            for (int x = 0; x < source.width; x++)
            {
                int index = y * source.width + x;
                int originalAlpha = getAlpha(source, x, y, index);
                imageData.alphaData[index] = (byte) Math.round(originalAlpha * factor);
            }
        }

        return imageData;
    }

    private static int getAlpha(ImageData source, int x, int y, int index)
    {
        if (source.alphaData != null)
            return source.alphaData[index] & 0xFF;

        if (source.alpha != -1)
            return source.alpha;

        if (source.transparentPixel != -1 && source.getPixel(x, y) == source.transparentPixel)
            return 0;

        return 255;
    }
}
