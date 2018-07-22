package name.abuchen.portfolio.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import name.abuchen.portfolio.Messages;

public class OnlineState
{
    public enum Property
    {
        NAME(Messages.CSVColumn_SecurityName, Security::getName), //
        ISIN(Messages.CSVColumn_ISIN, Security::getIsin), //
        WKN(Messages.CSVColumn_WKN, Security::getWkn), //
        TICKER(Messages.CSVColumn_TickerSymbol, Security::getTickerSymbol);

        private String label;
        private Function<Security, String> securityReadMethod;

        private Property(String label, Function<Security, String> securityReadMethod)
        {
            this.label = label;
            this.securityReadMethod = securityReadMethod;
        }

        public String getLabel()
        {
            return label;
        }

        public String getValue(Security security)
        {
            return securityReadMethod.apply(security);
        }
    }

    public enum State
    {
        BLANK, SYNCED, CUSTOM, EDITED;
    }

    private Map<Property, State> state = new EnumMap<>(Property.class);

    public OnlineState()
    {}

    public OnlineState(OnlineState template)
    {
        Objects.requireNonNull(template);
        this.state.putAll(template.state);
    }

    public State getState(Property property)
    {
        State answer = state.get(property);
        return answer == null ? State.BLANK : answer;
    }

    public State setState(Property property, State state)
    {
        return this.state.put(property, state);
    }

    @Override
    public String toString()
    {
        return state.toString();
    }

    public void setAll(OnlineState other)
    {
        this.state.clear();
        this.state.putAll(other.state);
    }
}
