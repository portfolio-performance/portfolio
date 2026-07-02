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
        copy.setSemanticRole(source.getSemanticRole());
        copy.setDirection(source.getDirection());
        copy.setCorporateActionLeg(source.getCorporateActionLeg());
        copy.setUnitRole(source.getUnitRole());
        copy.setGroupKey(source.getGroupKey());
        copy.setLocalKey(source.getLocalKey());
        source.getParameters().stream().map(LedgerModelCopy::copyParameter).forEach(copy::addParameter);

        return copy;
    }

    static LedgerParameter<?> copyParameter(LedgerParameter<?> source)
    {
        Objects.requireNonNull(source);
        return LedgerParameter.unchecked(source.getType(), source.getValueKind(), source.getValue());
    }
}
