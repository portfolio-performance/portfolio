package name.abuchen.portfolio.model;

import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.AttributeType.ImageConverter;

public abstract class AttributableBase implements Attributable {

    public Image getImage(AttributeType attr, int width, int height)
    {   
        if(this.getAttributes().exists(attr)) {
            if(attr.getConverter() instanceof ImageConverter) {
                ImageConverter imgConv = (ImageConverter)attr.getConverter();
                Image fullImage = imgConv.toImage(this.getAttributes().get(attr).toString());
                if(fullImage != null) {
                    return ImageConverter.resize(fullImage, width, height);
                }
            }
        }
        return null;
    }
}
