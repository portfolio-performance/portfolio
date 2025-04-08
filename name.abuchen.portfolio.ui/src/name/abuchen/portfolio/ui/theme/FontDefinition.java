package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.internal.css.swt.definition.IFontDefinitionOverridable;
import org.eclipse.swt.graphics.FontData;

@SuppressWarnings("restriction")
public class FontDefinition implements IFontDefinitionOverridable
{
    private final String id;
    private FontData[] value;

    public FontDefinition(String id)
    {
        this.id = id;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setValue(FontData[] data)
    {
        this.value = data;
    }

    @Override
    public FontData[] getValue()
    {
        return value;
    }

    @Override
    public boolean isOverridden()
    {
        return false;
    }

    @Override
    public void setCategoryId(String categoryId)
    {
    }

    @Override
    public void setName(String name)
    {
    }

    @Override
    public void setDescription(String description)
    {
    }
}
