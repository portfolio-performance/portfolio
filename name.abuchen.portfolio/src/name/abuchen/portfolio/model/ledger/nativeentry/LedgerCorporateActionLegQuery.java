package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;

/**
 * Business-attribute query over native Corporate Action postings.
 */
public final class LedgerCorporateActionLegQuery
{
    private final LedgerEntry entry;
    private final LedgerLegRole role;
    private final Predicate<LedgerPosting> predicate;

    LedgerCorporateActionLegQuery(LedgerEntry entry)
    {
        this(entry, null, posting -> true);
    }

    private LedgerCorporateActionLegQuery(LedgerEntry entry, LedgerLegRole role, Predicate<LedgerPosting> predicate)
    {
        this.entry = Objects.requireNonNull(entry);
        this.role = role;
        this.predicate = Objects.requireNonNull(predicate);
    }

    public LedgerCorporateActionLegQuery withRole(LedgerLegRole role)
    {
        Objects.requireNonNull(role);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> LedgerCorporateActionLegRoles.matches(posting, role)));
    }

    public LedgerCorporateActionLegQuery withLocalKey(String localKey)
    {
        Objects.requireNonNull(localKey);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> localKey.equals(posting.getLocalKey())));
    }

    public LedgerCorporateActionLegQuery withGroupKey(String groupKey)
    {
        Objects.requireNonNull(groupKey);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> groupKey.equals(posting.getGroupKey())));
    }

    public LedgerCorporateActionLegQuery withPortfolio(Portfolio portfolio)
    {
        Objects.requireNonNull(portfolio);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> sameModelObject(portfolio.getUUID(), posting.getPortfolio())));
    }

    public LedgerCorporateActionLegQuery withSecurity(Security security)
    {
        Objects.requireNonNull(security);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> sameModelObject(security.getUUID(), posting.getSecurity())));
    }

    public LedgerCorporateActionLegQuery withAccount(Account account)
    {
        Objects.requireNonNull(account);
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> sameModelObject(account.getUUID(), posting.getAccount())));
    }

    public LedgerCorporateActionLegQuery withCorporateActionLeg(CorporateActionLeg leg)
    {
        return new LedgerCorporateActionLegQuery(entry, role,
                        predicate.and(posting -> posting.getCorporateActionLeg() == leg));
    }

    public List<LedgerCorporateActionLegHandle> list()
    {
        return entry.getPostings().stream().filter(predicate)
                        .map(posting -> new LedgerCorporateActionLegHandle(entry, posting, role(posting))).toList();
    }

    public LedgerCorporateActionLegHandle single()
    {
        var matches = list();

        if (matches.size() != 1)
            throw new IllegalArgumentException("Corporate Action leg query expected one match but found " //$NON-NLS-1$
                            + matches.size());

        return matches.get(0);
    }

    public Optional<LedgerCorporateActionLegHandle> optional()
    {
        var matches = list();

        if (matches.size() > 1)
            throw new IllegalArgumentException("Corporate Action leg query is ambiguous: " + matches.size() //$NON-NLS-1$
                            + " matches"); //$NON-NLS-1$

        return matches.stream().findFirst();
    }

    public boolean isEmpty()
    {
        return list().isEmpty();
    }

    private LedgerLegRole role(LedgerPosting posting)
    {
        if (role != null)
            return role;

        return LedgerCorporateActionLegRoles.roleFor(posting)
                        .orElseThrow(() -> new IllegalArgumentException("Unsupported Corporate Action leg: " //$NON-NLS-1$
                                        + posting.getType()));
    }

    private static boolean sameModelObject(String uuid, Object actual)
    {
        if (actual instanceof Account account)
            return Objects.equals(uuid, account.getUUID());
        if (actual instanceof Portfolio portfolio)
            return Objects.equals(uuid, portfolio.getUUID());
        if (actual instanceof Security security)
            return Objects.equals(uuid, security.getUUID());

        return false;
    }
}
