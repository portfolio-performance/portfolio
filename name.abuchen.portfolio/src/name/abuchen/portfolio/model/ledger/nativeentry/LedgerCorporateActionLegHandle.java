package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;

/**
 * In-memory handle for one native Corporate Action leg.
 */
public final class LedgerCorporateActionLegHandle
{
    private final LedgerEntry entry;
    private final LedgerPosting posting;
    private final LedgerLegRole role;

    LedgerCorporateActionLegHandle(LedgerEntry entry, LedgerPosting posting, LedgerLegRole role)
    {
        this.entry = Objects.requireNonNull(entry);
        this.posting = Objects.requireNonNull(posting);
        this.role = Objects.requireNonNull(role);
    }

    public LedgerEntry entry()
    {
        return entry;
    }

    public LedgerPosting posting()
    {
        return posting;
    }

    public LedgerLegRole role()
    {
        return role;
    }

    public String localKey()
    {
        return posting.getLocalKey();
    }

    public String groupKey()
    {
        return posting.getGroupKey();
    }

    public Portfolio portfolio()
    {
        return posting.getPortfolio();
    }

    public Security security()
    {
        return posting.getSecurity();
    }

    public Account account()
    {
        return posting.getAccount();
    }

    public boolean hasGroupKey()
    {
        return groupKey() != null && !groupKey().isBlank();
    }

    public SemanticKey toSemanticKey()
    {
        return new SemanticKey(role, localKey(), groupKey());
    }

    public record SemanticKey(LedgerLegRole role, String localKey, String groupKey)
    {
        public SemanticKey
        {
            Objects.requireNonNull(role);
            Objects.requireNonNull(localKey);
        }
    }
}
