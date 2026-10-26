package name.abuchen.portfolio.cli.apply;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import name.abuchen.portfolio.cli.query.TransactionRecord;
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
record EntityKind<T>(String noun, Function<Client, ? extends Collection<T>> all, BiPredicate<T, String> hasUuid,
                Function<T, Object> stored)
{
    static final EntityKind<Account> CASH_ACCOUNT = of("cash account", Client::getAccounts, Account::getUUID);
    static final EntityKind<Portfolio> INVESTMENT_ACCOUNT = of("investment account", Client::getPortfolios,
                    Portfolio::getUUID);
    static final EntityKind<Security> INSTRUMENT = of("instrument", Client::getSecurities, Security::getUUID);
    static final EntityKind<Taxonomy> TAXONOMY = of("taxonomy", Client::getTaxonomies, Taxonomy::getId);

    /** A record is named by the UUID of any of its sides. */
    static final EntityKind<TransactionRecord> TRANSACTION_RECORD = new EntityKind<>("transaction",
                    TransactionRecord::allOf, (record, uuid) -> record.sides().contains(uuid),
                    TransactionRecord::primary);

    static <T> EntityKind<T> of(String noun, Function<Client, ? extends Collection<T>> all, Function<T, String> uuidOf)
    {
        return new EntityKind<>(noun, all, (entity, uuid) -> uuid.equals(uuidOf.apply(entity)), entity -> entity);
    }

    /** The existing entity of this kind with the UUID, if there is one. */
    Optional<T> find(OperationContext context, String uuid)
    {
        return all.apply(context.client()).stream()
                        .filter(e -> hasUuid.test(e, uuid) && context.existedBeforeBatch(stored.apply(e)))
                        .findFirst();
    }
}
