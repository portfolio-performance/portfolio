package name.abuchen.portfolio.model;

import java.util.Objects;

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
        MARKET,

        /**
         * Properties related to loading data from a quote feed.
         */
        FEED;
    }

    private final Type type;
    private final String name;
    private final String value;

    public SecurityProperty(Type type, String name, String value)
    {
        this.type = Objects.requireNonNull(type);
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
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

    @Override
    public String toString()
    {
        return String.join(",", type.toString(), name, value); //$NON-NLS-1$
    }
}
