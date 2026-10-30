package name.abuchen.portfolio.rest.internal;

import java.util.Collection;
import java.util.function.Function;

/**
 * Looks up model entities by their UUID. Security, Account, and Portfolio do
 * not share a supertype that exposes the UUID, hence the extractor function.
 */
public final class Entities
{
    private Entities()
    {
    }

    /**
     * A UUID is case-insensitive on input (RFC 9562), so an id a client
     * upper-cased on its way through still finds its entity.
     */
    public static <T> T byUuid(Collection<T> entities, Function<T, String> uuid, String wanted)
    {
        if (wanted == null)
            throw ApiException.notFound();

        return entities.stream() //
                        .filter(entity -> wanted.equalsIgnoreCase(uuid.apply(entity))) //
                        .findFirst().orElseThrow(ApiException::notFound);
    }
}
