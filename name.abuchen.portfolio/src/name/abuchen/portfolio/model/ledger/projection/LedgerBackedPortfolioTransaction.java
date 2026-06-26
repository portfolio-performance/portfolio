package name.abuchen.portfolio.model.ledger.projection;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatchHelper;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

/**
 * Represents a runtime legacy transaction view backed by a Ledger entry.
 * This class belongs to projection support. It must not be treated as persisted transaction
 * truth or mutated through legacy setters.
 */
public final class LedgerBackedPortfolioTransaction extends PortfolioTransaction implements LedgerBackedTransaction
{
    private final LedgerEntry entry;
    private final LedgerProjectionRef projectionRef;
    private final LedgerPosting primaryPosting;
    private CrossEntry crossEntry;

    LedgerBackedPortfolioTransaction(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        this.entry = entry;
        this.projectionRef = projectionRef;
        this.primaryPosting = LedgerProjectionSupport.primaryPosting(entry, projectionRef);
    }

    @Override
    public LedgerEntry getLedgerEntry()
    {
        return entry;
    }

    @Override
    public LedgerProjectionRef getLedgerProjectionRef()
    {
        return projectionRef;
    }

    void setLedgerCrossEntry(CrossEntry crossEntry)
    {
        this.crossEntry = crossEntry;
    }

    @Override
    public String getUUID()
    {
        return projectionRef.getUUID();
    }

    @Override
    public Type getType()
    {
        if (entry.getType().isLedgerNativeTargeted())
            return LedgerProjectionSupport.targetedPortfolioType(projectionRef);

        return switch (entry.getType())
        {
            case BUY -> Type.BUY;
            case SELL -> Type.SELL;
            case SECURITY_TRANSFER -> transferType();
            case DELIVERY_INBOUND -> Type.DELIVERY_INBOUND;
            case DELIVERY_OUTBOUND -> Type.DELIVERY_OUTBOUND;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_069
                            .message("Unsupported portfolio projection for " + entry.getType())); //$NON-NLS-1$
        };
    }

    @Override
    public LocalDateTime getDateTime()
    {
        return entry.getDateTime();
    }

    @Override
    public String getNote()
    {
        return entry.getNote();
    }

    @Override
    public String getSource()
    {
        return entry.getSource();
    }

    @Override
    public Instant getUpdatedAt()
    {
        return entry.getUpdatedAt();
    }

    @Override
    public long getAmount()
    {
        return primaryPosting.getAmount();
    }

    @Override
    public String getCurrencyCode()
    {
        return primaryPosting.getCurrency();
    }

    @Override
    public Money getMonetaryAmount()
    {
        return Money.of(getCurrencyCode(), getAmount());
    }

    @Override
    public Security getSecurity()
    {
        return primaryPosting.getSecurity();
    }

    @Override
    public long getShares()
    {
        return primaryPosting.getShares();
    }

    @Override
    public CrossEntry getCrossEntry()
    {
        return crossEntry;
    }

    @Override
    public Stream<Unit> getUnits()
    {
        return LedgerProjectionSupport.units(entry, projectionRef, primaryPosting);
    }

    @Override
    public long getGrossValueAmount()
    {
        long taxAndFees = getUnitSum(Unit.Type.FEE, Unit.Type.TAX).getAmount();

        if (getType().isPurchase())
            return getAmount() - taxAndFees;
        else
            return getAmount() + taxAndFees;
    }

    @Override
    public Money getUnitSum(Unit.Type type, CurrencyConverter converter)
    {
        return getUnits().filter(u -> u.getType() == type)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency(), unit -> {
                            if (converter.getTermCurrency().equals(unit.getAmount().getCurrencyCode()))
                                return unit.getAmount();
                            else if (unit.getForex() != null
                                            && converter.getTermCurrency().equals(unit.getForex().getCurrencyCode()))
                                return unit.getForex();
                            else
                                return unit.getAmount().with(converter.at(getDateTime()));
                        }));
    }

    @Override
    public String toString()
    {
        return String.format("%s %-17s %s %9s %s", Values.DateTime.format(this.getDateTime()), getType().name(), //$NON-NLS-1$
                        getCurrencyCode(), Values.Amount.format(getAmount()), getSecurity().getName());
    }

    @Override
    public void setType(Type type)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setDateTime(LocalDateTime date)
    {
        LedgerEntryMetadataPatchHelper.setDateTime(entry, date);
    }

    @Override
    public void setNote(String note)
    {
        LedgerEntryMetadataPatchHelper.setNote(entry, note);
    }

    @Override
    public void setSource(String source)
    {
        LedgerEntryMetadataPatchHelper.setSource(entry, source);
    }

    @Override
    public void setUpdatedAt(Instant updatedAt)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setAmount(long amount)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setCurrencyCode(String currencyCode)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setMonetaryAmount(Money value)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setSecurity(Security security)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void setShares(long shares)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void clearUnits()
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void addUnit(Unit unit)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void addUnits(Stream<Unit> items)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void removeUnit(Unit unit)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    public void removeUnits(Unit.Type type)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    @Override
    protected void setCrossEntry(CrossEntry crossEntry)
    {
        throw LedgerProjectionSupport.unsupportedMutation();
    }

    private Type transferType()
    {
        return switch (projectionRef.getRole())
        {
            case SOURCE_PORTFOLIO -> Type.TRANSFER_OUT;
            case TARGET_PORTFOLIO -> Type.TRANSFER_IN;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_070
                            .message("Unsupported security transfer role " + projectionRef.getRole())); //$NON-NLS-1$
        };
    }
}
