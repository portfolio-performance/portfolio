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

    public static <T> T byUuid(Collection<T> entities, Function<T, String> uuid, String wanted)
    {
        return entities.stream() //
                        .filter(entity -> uuid.apply(entity).equals(wanted)) //
                        .findFirst().orElseThrow(ApiException::notFound);
    }
}
