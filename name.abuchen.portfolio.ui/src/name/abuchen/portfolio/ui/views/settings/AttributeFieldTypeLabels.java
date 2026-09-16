package name.abuchen.portfolio.ui.views.settings;

import java.util.ResourceBundle;

import name.abuchen.portfolio.model.AttributeFieldType;

public final class AttributeFieldTypeLabels
{
    private static final ResourceBundle RESOURCES = ResourceBundle
                    .getBundle("name.abuchen.portfolio.ui.views.settings.labels"); //$NON-NLS-1$

    private AttributeFieldTypeLabels()
    {
    }

    public static String label(AttributeFieldType type)
    {
        return RESOURCES.getString(type.name() + ".name"); //$NON-NLS-1$
    }
}
