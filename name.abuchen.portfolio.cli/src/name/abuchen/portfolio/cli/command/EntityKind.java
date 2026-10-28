package name.abuchen.portfolio.cli.command;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * A kind of entity that a write operation names by UUID. Only an existing
 * entity, one that was in the file before the batch started, can be named.
 *
 * @param noun
 *            how failures name the kind, such as <code>cash account</code>
 * @param stored
 *            the object of the model that was in the file before the batch,
 *            if the entity is a view of one
 */
public record EntityKind<T>(String noun, Function<Client, ? extends Collection<T>> all, BiPredicate<T, String> hasUuid,
                Function<T, Object> stored)
{
    public static final EntityKind<Account> CASH_ACCOUNT = of("cash account", Client::getAccounts, Account::getUUID);
    public static final EntityKind<Portfolio> INVESTMENT_ACCOUNT = of("investment account", Client::getPortfolios,
                    Portfolio::getUUID);
    public static final EntityKind<Security> INSTRUMENT = of("instrument", Client::getSecurities, Security::getUUID);
    public static final EntityKind<Taxonomy> TAXONOMY = of("taxonomy", Client::getTaxonomies, Taxonomy::getId);

    public static <T> EntityKind<T> of(String noun, Function<Client, ? extends Collection<T>> all,
                    Function<T, String> uuidOf)
    {
        return new EntityKind<>(noun, all, (entity, uuid) -> uuid.equals(uuidOf.apply(entity)), entity -> entity);
    }

    /** The existing entity of this kind with the UUID, if there is one. */
    public Optional<T> find(Client client, Predicate<Object> existedBeforeBatch, String uuid)
    {
        return all.apply(client).stream()
                        .filter(e -> hasUuid.test(e, uuid) && existedBeforeBatch.test(stored.apply(e))).findFirst();
    }
}
