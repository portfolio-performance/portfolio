package name.abuchen.portfolio.ui.views.settings;

import java.time.LocalDate;
import java.util.ResourceBundle;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.AmountConverter;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.Converter;
import name.abuchen.portfolio.model.AttributeType.DateConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.QuoteConverter;
import name.abuchen.portfolio.model.AttributeType.ShareConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;

public enum AttributeFieldType
{
    STRING(String.class, StringConverter.class), //
    AMOUNT(Long.class, AmountConverter.class), //
    AMOUNTPLAIN(Long.class, AmountPlainConverter.class), //
    PERCENT(Double.class, PercentConverter.class), //
    PERCENTPLAIN(Double.class, PercentPlainConverter.class), //
    QUOTE(Long.class, QuoteConverter.class), //
    SHARE(Long.class, ShareConverter.class), //
    DATE(LocalDate.class, DateConverter.class);

    private static final ResourceBundle RESOURCES = ResourceBundle
                    .getBundle("name.abuchen.portfolio.ui.views.settings.labels"); //$NON-NLS-1$

    private final Class<?> type;
    private final Class<? extends Converter> converterClass;

    private AttributeFieldType(Class<?> type, Class<? extends Converter> converterClass)
    {
        this.type = type;
        this.converterClass = converterClass;
    }

    public Class<?> getFieldClass()
    {
        return type;
    }

    public Class<? extends Converter> getConverterClass()
    {
        return converterClass;
    }

    private boolean isFieldType(AttributeType attribute)
    {
        return converterClass.isAssignableFrom(attribute.getConverter().getClass())
                        && type.isAssignableFrom(attribute.getType());
    }

    public String toString()
    {
        return RESOURCES.getString(name() + ".name"); //$NON-NLS-1$
    }

    public static AttributeFieldType of(AttributeType attribute)
    {
        for (AttributeFieldType t : values())
            if (t.isFieldType(attribute))
                return t;

        return null;
    }
}
