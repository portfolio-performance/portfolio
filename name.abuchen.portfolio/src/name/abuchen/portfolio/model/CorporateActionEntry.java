package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CorporateActionEntry implements CrossEntry, Annotated
{
    public enum Type
    {
        SPIN_OFF
    }

    public enum LegRole
    {
        SOURCE, TARGET, FRACTION_SALE, CASH_IN_LIEU
    }

    public record Leg(TransactionOwner<? extends Transaction> owner, Transaction transaction, LegRole role)
    {
    }

    public record DistributionRatio(int numerator, int denominator)
    {
    }

    private Type type;
    private final List<Leg> legs = new ArrayList<>();
    private String source;
    private LocalDate exDate;
    private BigDecimal basisRatio;
    private DistributionRatio distributionRatio;
    private long referencePrice;

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public LocalDate getExDate()
    {
        return exDate;
    }

    public void setExDate(LocalDate exDate)
    {
        this.exDate = exDate;
    }

    public BigDecimal getBasisRatio()
    {
        return basisRatio;
    }

    public void setBasisRatio(BigDecimal basisRatio)
    {
        this.basisRatio = basisRatio;
    }

    public DistributionRatio getDistributionRatio()
    {
        return distributionRatio;
    }

    public void setDistributionRatio(DistributionRatio distributionRatio)
    {
        this.distributionRatio = distributionRatio;
    }

    public long getReferencePrice()
    {
        return referencePrice;
    }

    public void setReferencePrice(long referencePrice)
    {
        this.referencePrice = referencePrice;
    }

    public void addLeg(TransactionOwner<? extends Transaction> owner, Transaction transaction, LegRole role)
    {
        transaction.setCrossEntry(this);
        legs.add(new Leg(owner, transaction, role));
    }

    public List<Leg> getLegs()
    {
        return List.copyOf(legs);
    }

    public Optional<Transaction> getLeg(LegRole role)
    {
        return legs.stream().filter(l -> l.role() == role).map(Leg::transaction).findFirst();
    }

    private Leg legOf(Transaction t)
    {
        return legs.stream().filter(l -> l.transaction() == t).findFirst()
                        .orElseThrow(() -> new UnsupportedOperationException("transaction not part of this entry"));
    }

    private LegRole counterpartRole(LegRole role)
    {
        return switch (role)
        {
            case SOURCE -> LegRole.TARGET;
            case TARGET -> LegRole.SOURCE;
            case FRACTION_SALE -> LegRole.CASH_IN_LIEU;
            case CASH_IN_LIEU -> LegRole.FRACTION_SALE;
        };
    }

    private Leg counterpart(Transaction t)
    {
        var wantRole = counterpartRole(legOf(t).role());
        return legs.stream().filter(l -> l.role() == wantRole).findFirst()
                        .orElseThrow(() -> new UnsupportedOperationException("no counterpart leg"));
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        return counterpart(t).transaction();
    }

    @Override
    public List<Transaction> getCrossTransactions(Transaction t)
    {
        legOf(t); // validate membership (throws if t is not a leg of this entry)
        return legs.stream().map(Leg::transaction).filter(other -> other != t).toList();
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        return counterpart(t).owner();
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        return legOf(t).owner();
    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        var leg = legOf(t);
        legs.set(legs.indexOf(leg), new Leg(owner, leg.transaction(), leg.role()));
    }

    @Override
    public void updateFrom(Transaction t)
    {
        legOf(t); // validate membership (throws if t is not a leg of this entry)

        // cascade only the group-shared attributes; the per-leg security
        // (parent vs spinco) and shares are authoritative facts (spec §11)
        for (Leg leg : legs)
        {
            if (leg.transaction() == t)
                continue;

            leg.transaction().setDateTime(t.getDateTime());
            leg.transaction().setNote(t.getNote());
        }
    }

    @Override
    public void insert()
    {
        for (Leg leg : legs)
            addToOwner(leg);
    }

    @SuppressWarnings("unchecked")
    private static void addToOwner(Leg leg)
    {
        ((TransactionOwner<Transaction>) leg.owner()).addTransaction(leg.transaction());
    }

    @Override
    public String getSource()
    {
        return source;
    }

    @Override
    public void setSource(String source)
    {
        this.source = source;
    }

    @Override
    public String getNote()
    {
        return legs.isEmpty() ? null : legs.get(0).transaction().getNote();
    }

    @Override
    public void setNote(String note)
    {
        legs.forEach(l -> l.transaction().setNote(note));
    }
}
