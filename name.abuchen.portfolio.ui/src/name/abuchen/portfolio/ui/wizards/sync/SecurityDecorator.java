package name.abuchen.portfolio.ui.wizards.sync;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.model.OnlineState.Property;
import name.abuchen.portfolio.model.OnlineState.State;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.sync.PortfolioReportNet;
import name.abuchen.portfolio.online.sync.PortfolioReportNet.OnlineItem;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class SecurityDecorator implements Adaptable
{
    private Security security;

    private String onlineId;

    private Map<OnlineState.Property, OnlineProperty> properties = new EnumMap<>(Property.class);

    public SecurityDecorator(Security security)
    {
        this.security = security;

        this.onlineId = security.getOnlineId();

        OnlineState state = security.getOnlineState();

        properties.put(Property.NAME, new OnlineProperty(security.getName(), state.getState(Property.NAME)));
        properties.put(Property.ISIN, new OnlineProperty(security.getIsin(), state.getState(Property.ISIN)));
        properties.put(Property.WKN, new OnlineProperty(security.getWkn(), state.getState(Property.WKN)));
        properties.put(Property.TICKER,
                        new OnlineProperty(security.getTickerSymbol(), state.getState(Property.TICKER)));
    }

    public OnlineProperty getProperty(Property property)
    {
        return properties.get(property);
    }

    public Security getSecurity()
    {
        return security;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Named.class)
            return type.cast(security);
        else
            return null;
    }

    public void checkOnline()
    {
        try
        {
            Optional<OnlineItem> onlineItem = onlineId != null ? new PortfolioReportNet().getUpdatedValues(security) : new PortfolioReportNet().findMatch(security);

            onlineItem.ifPresent(o -> {
                
                this.onlineId = o.getId();
                
                properties.get(Property.NAME).setSuggestedValue(o.getName());
                properties.get(Property.ISIN).setSuggestedValue(o.getIsin());
                properties.get(Property.WKN).setSuggestedValue(o.getWkn());
                properties.get(Property.TICKER).setSuggestedValue(o.getTicker());
            });
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    /**
     * Applies selected changes to the original security.
     * 
     * @return true if any changes were made.
     */
    public boolean apply()
    {
        if (onlineId == null)
            return false;

        boolean[] isDirty = new boolean[1];

        if (!onlineId.equals(security.getOnlineId()))
        {
            security.setOnlineId(onlineId);
            isDirty[0] = true;
        }

        properties.entrySet().stream().forEach(entry -> {
            if (entry.getValue().isModified())
            {
                switch (entry.getKey())
                {
                    case NAME:
                        security.setName(entry.getValue().getSuggestedValue());
                        break;
                    case ISIN:
                        security.setIsin(entry.getValue().getSuggestedValue());
                        break;
                    case WKN:
                        security.setWkn(entry.getValue().getSuggestedValue());
                        break;
                    case TICKER:
                        security.setTickerSymbol(entry.getValue().getSuggestedValue());
                        break;
                    default:
                }

                isDirty[0] = true;
            }

            State newState = entry.getValue().getSuggestedState();
            State oldState = security.getOnlineState().setState(entry.getKey(), newState);

            isDirty[0] = isDirty[0] || !newState.equals(oldState);
        });

        return isDirty[0];
    }

}
