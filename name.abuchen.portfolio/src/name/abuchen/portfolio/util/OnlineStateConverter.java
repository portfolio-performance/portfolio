package name.abuchen.portfolio.util;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

import name.abuchen.portfolio.model.OnlineState;

public class OnlineStateConverter extends AbstractSingleValueConverter
{
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.equals(OnlineState.class);
    }

    @Override
    public String toString(Object source)
    {
        OnlineState state = (OnlineState) source;

        StringBuilder builder = new StringBuilder();

        for (OnlineState.Property p : OnlineState.Property.values())
            builder.append(state.getState(p).ordinal());

        return builder.toString();
    }

    public Object fromString(String s)
    {
        OnlineState state = new OnlineState();

        for (int index = 0; index < s.length() && index < OnlineState.Property.values().length; index++)
        {
            int ordinal = "0123456789".indexOf(s.charAt(index));
            if (ordinal >= 0 && ordinal < OnlineState.State.values().length)
                state.setState(OnlineState.Property.values()[index], OnlineState.State.values()[ordinal]);
        }

        return state;
    }
}
