package name.abuchen.portfolio.cli.command;

import java.util.OptionalInt;

/**
 * Claims what an operation changes: one batch may change an entity only once.
 */
@FunctionalInterface
public interface Claims
{
    /**
     * Claims the entity with the UUID, or a composite key naming what the
     * operation changes, for the current operation.
     *
     * @return the index of the earlier operation that has claimed it, or empty
     *         if the current operation now holds the claim
     */
    OptionalInt claim(String uuid);
}
