package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.internal.css.swt.definition.IColorDefinitionOverridable;
import org.eclipse.swt.graphics.RGB;

@SuppressWarnings("restriction")
public class ColorDefinition implements IColorDefinitionOverridable
{
    private final String id;
    private RGB value;

    public ColorDefinition(String id)
    {
        this.id = id;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setValue(RGB data)
    {
        this.value = data;
    }

    @Override
    public RGB getValue()
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
