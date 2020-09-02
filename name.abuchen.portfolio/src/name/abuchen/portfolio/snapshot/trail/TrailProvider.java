package name.abuchen.portfolio.snapshot.trail;

import java.util.Optional;

/**
 * Interface to be implemented by table elements to provide access to the trail
 * records. Because the tool tip viewer has only access to a string, we pass the
 * key to the trail as text of the tool tip (which then can be looked up by this
 * interface).
 */
public interface TrailProvider
{
    Optional<Trail> explain(String key);
}
