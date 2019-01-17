package name.abuchen.portfolio.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

@XStreamAlias("property")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = { "value" })
public class SecurityProperty
{
    public enum Type
    {
        /**
         * Property type to store a market (e.g. exchange) and symbol pair.
         */
        MARKET
    }

    private final Type type;
    private final String name;
    private final String value;

    public SecurityProperty(Type type, String name, String value)
    {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public Type getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }
}
