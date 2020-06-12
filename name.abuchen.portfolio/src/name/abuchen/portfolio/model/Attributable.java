package name.abuchen.portfolio.model;

import org.eclipse.swt.graphics.Image;

public interface Attributable
{
    Attributes getAttributes();

    void setAttributes(Attributes attributes);
    
    Image getImage(AttributeType attr, int width, int height);
}
