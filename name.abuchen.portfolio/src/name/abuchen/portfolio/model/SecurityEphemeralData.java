package name.abuchen.portfolio.model;

import java.time.Instant;
import java.util.Optional;

public class SecurityEphemeralData
{

    private Instant feedConfigurationChanged;
    private Instant feedLastUpdate;

    /**
     * Returns the last time the feed configuration was changed by the user.
     * This can be used by feed implementations to determine caching behavior.
     * For example, some feeds return the latest price as part of the historical
     * prices, some do not. There there is a price for today, the latter can
     * assume no update is needed anymore.
     * 
     * @return the last time the feed configuration was changed or empty if the
     *         feed was never edited in this session
     */
    public Optional<Instant> getFeedConfigurationChanged()
    {
        return Optional.ofNullable(feedConfigurationChanged);
    }

    public void touchFeedConfigurationChanged()
    {
        this.feedConfigurationChanged = Instant.now();
    }

    /**
     * Returns the last time the feed updated the historical prices.
     */
    public Optional<Instant> getFeedLastUpdate()
    {
        return Optional.ofNullable(feedLastUpdate);
    }

    public void touchFeedLastUpdate()
    {
        this.feedLastUpdate = Instant.now();
    }
}
