package name.abuchen.portfolio.model.ledger.projection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Runtime-only description of a compatibility projection derived from Ledger
 * entry and posting semantics.
 */
public final class DerivedProjectionDescriptor
{
    private final LedgerEntry entry;
    private final LedgerProjectionRole role;
    private final DerivedProjectionViewKind viewKind;
    private final Account account;
    private final Portfolio portfolio;
    private final LedgerPosting primaryPosting;
    private final List<LedgerPosting> unitPostings;
    private final String semanticInstanceKey;
    private final String primarySelector;
    private final String unitSelector;

    DerivedProjectionDescriptor(LedgerEntry entry, LedgerProjectionRole role, DerivedProjectionViewKind viewKind,
                    Account account, Portfolio portfolio, LedgerPosting primaryPosting, List<LedgerPosting> unitPostings,
                    String primarySelector, String unitSelector)
    {
        this(entry, role, viewKind, account, portfolio, primaryPosting, unitPostings, null, primarySelector,
                        unitSelector);
    }

    DerivedProjectionDescriptor(LedgerEntry entry, LedgerProjectionRole role, DerivedProjectionViewKind viewKind,
                    Account account, Portfolio portfolio, LedgerPosting primaryPosting, List<LedgerPosting> unitPostings,
                    String semanticInstanceKey, String primarySelector, String unitSelector)
    {
        this.entry = Objects.requireNonNull(entry);
        this.role = Objects.requireNonNull(role);
        this.viewKind = Objects.requireNonNull(viewKind);
        this.account = account;
        this.portfolio = portfolio;
        this.primaryPosting = Objects.requireNonNull(primaryPosting);
        this.unitPostings = List.copyOf(unitPostings);
        this.semanticInstanceKey = normalize(semanticInstanceKey);
        this.primarySelector = Objects.requireNonNull(primarySelector);
        this.unitSelector = Objects.requireNonNull(unitSelector);
    }

    public LedgerEntry getEntry()
    {
        return entry;
    }

    public String getRuntimeProjectionId()
    {
        var projectionId = entry.getUUID() + ":" + role; //$NON-NLS-1$

        if (semanticInstanceKey == null)
            return projectionId;

        return projectionId + ":" + semanticInstanceKey; //$NON-NLS-1$
    }

    public LedgerProjectionRole getRole()
    {
        return role;
    }

    public DerivedProjectionViewKind getViewKind()
    {
        return viewKind;
    }

    public Account getAccount()
    {
        return account;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public LedgerPosting getPrimaryPosting()
    {
        return primaryPosting;
    }

    public List<LedgerPosting> getUnitPostings()
    {
        return Collections.unmodifiableList(unitPostings);
    }

    public Optional<String> getSemanticInstanceKey()
    {
        return Optional.ofNullable(semanticInstanceKey);
    }

    public boolean hasSemanticInstanceKey()
    {
        return semanticInstanceKey != null;
    }

    public String getPrimarySelector()
    {
        return primarySelector;
    }

    public String getUnitSelector()
    {
        return unitSelector;
    }

    private static String normalize(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }
}
