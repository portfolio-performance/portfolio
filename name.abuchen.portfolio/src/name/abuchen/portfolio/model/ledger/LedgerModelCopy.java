package name.abuchen.portfolio.model.ledger;

import java.util.Objects;

/**
 * Copies Ledger model objects for safe mutation and rollback paths.
 * This is internal Ledger infrastructure. Normal contributor code should rely on mutation
 * contexts rather than copying Ledger graph objects directly.
 */
public final class LedgerModelCopy
{
    private LedgerModelCopy()
    {
    }

    public static Ledger copyLedger(Ledger source)
    {
        var copy = new Ledger();

        Objects.requireNonNull(source).getEntries().stream().map(LedgerModelCopy::copyEntry).forEach(copy::addEntry);

        return copy;
    }

    static LedgerEntry copyEntry(LedgerEntry source)
    {
        var copy = new LedgerEntry(Objects.requireNonNull(source).getUUID());

        copy.setType(source.getType());
        copy.setDateTime(source.getDateTime());
        copy.setNote(source.getNote());
        copy.setSource(source.getSource());

        source.getParameters().stream().map(LedgerModelCopy::copyParameter).forEach(copy::addParameter);
        source.getPostings().stream().map(LedgerModelCopy::copyPosting).forEach(copy::addPosting);
        source.getProjectionRefs().stream().map(LedgerModelCopy::copyProjectionRef).forEach(copy::addProjectionRef);
        copy.setUpdatedAt(source.getUpdatedAt());

        return copy;
    }

    static LedgerPosting copyPosting(LedgerPosting source)
    {
        var copy = new LedgerPosting(Objects.requireNonNull(source).getUUID());

        copy.setType(source.getType());
        copy.setAmount(source.getAmount());
        copy.setCurrency(source.getCurrency());
        copy.setForexAmount(source.getForexAmount());
        copy.setForexCurrency(source.getForexCurrency());
        copy.setExchangeRate(source.getExchangeRate());
        copy.setSecurity(source.getSecurity());
        copy.setShares(source.getShares());
        copy.setAccount(source.getAccount());
        copy.setPortfolio(source.getPortfolio());
        source.getParameters().stream().map(LedgerModelCopy::copyParameter).forEach(copy::addParameter);

        return copy;
    }

    static LedgerProjectionRef copyProjectionRef(LedgerProjectionRef source)
    {
        var copy = new LedgerProjectionRef(Objects.requireNonNull(source).getUUID());

        copy.setRole(source.getRole());
        copy.setAccount(source.getAccount());
        copy.setPortfolio(source.getPortfolio());
        copy.setPrimaryPostingUUID(source.getPrimaryPostingUUID());
        copy.setPostingGroupUUID(source.getPostingGroupUUID());
        source.getMemberships().stream().map(LedgerModelCopy::copyProjectionMembership).forEach(copy::addMembership);

        return copy;
    }

    static ProjectionMembership copyProjectionMembership(ProjectionMembership source)
    {
        Objects.requireNonNull(source);
        return new ProjectionMembership(source.getPostingUUID(), source.getRole());
    }

    static LedgerParameter<?> copyParameter(LedgerParameter<?> source)
    {
        Objects.requireNonNull(source);
        return LedgerParameter.unchecked(source.getType(), source.getValueKind(), source.getValue());
    }
}
