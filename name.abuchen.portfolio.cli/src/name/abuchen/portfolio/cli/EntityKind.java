package name.abuchen.portfolio.cli;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

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
 */
record EntityKind<T>(String noun, Function<Client, ? extends Collection<T>> all, Function<T, String> uuidOf)
{
    static final EntityKind<Account> CASH_ACCOUNT = new EntityKind<>("cash account", Client::getAccounts,
                    Account::getUUID);
    static final EntityKind<Portfolio> INVESTMENT_ACCOUNT = new EntityKind<>("investment account",
                    Client::getPortfolios, Portfolio::getUUID);
    static final EntityKind<Security> INSTRUMENT = new EntityKind<>("instrument", Client::getSecurities,
                    Security::getUUID);
    static final EntityKind<Taxonomy> TAXONOMY = new EntityKind<>("taxonomy", Client::getTaxonomies, Taxonomy::getId);

    /** The existing entity of this kind with the UUID, if there is one. */
    Optional<T> find(OperationContext context, String uuid)
    {
        return all.apply(context.client()).stream()
                        .filter(e -> uuid.equals(uuidOf.apply(e)) && context.existedBeforeBatch(e)).findFirst();
    }
}
