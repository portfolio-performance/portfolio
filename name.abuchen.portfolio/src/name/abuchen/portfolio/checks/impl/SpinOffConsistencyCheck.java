package name.abuchen.portfolio.checks.impl;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.Leg;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;

/**
 * Load-time consistency check for spin-off {@link CorporateActionEntry} groups
 * (spec §13.3). Catches externally-edited / structurally-broken groups and
 * offers to delete the whole group. Runtime engine guards are deliberately NOT
 * used (spec §13.4); this is the load-time net.
 */
public class SpinOffConsistencyCheck implements Check
{
    enum Defect
    {
        MISSING_LEG, DUPLICATE_LEG, UNPAIRED_FRACTION, SEVERED_LEG, BASIS_RATIO_OUT_OF_RANGE, CURRENCY_MISMATCH
    }

    /* package */ static class SpinOffIssue implements Issue
    {
        private final Client client;
        private final CorporateActionEntry entry;
        private final Defect defect;

        SpinOffIssue(Client client, CorporateActionEntry entry, Defect defect)
        {
            this.client = client;
            this.entry = entry;
            this.defect = defect;
        }

        Defect getDefect()
        {
            return defect;
        }

        CorporateActionEntry getEntry()
        {
            return entry;
        }

        @Override
        public LocalDate getDate()
        {
            return entry.getExDate();
        }

        @Override
        public Object getEntity()
        {
            return entry;
        }

        @Override
        public Long getAmount()
        {
            return null;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.IssueSpinOffInconsistent, entry.getExDate());
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            return List.of(new DeleteCorporateActionFix(client, entry));
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> issues = new ArrayList<>();

        for (CorporateActionEntry entry : client.getCorporateActions())
        {
            if (entry.getType() != CorporateActionEntry.Type.SPIN_OFF)
                continue; // no other event type exists yet; do not throw (spec §13.4)

            detect(entry).ifPresent(defect -> issues.add(new SpinOffIssue(client, entry, defect)));
        }

        return issues;
    }

    /**
     * Returns the first structural defect of a SPIN_OFF entry, if any. Task 3
     * appends the SEVERED_LEG, BASIS_RATIO_OUT_OF_RANGE and CURRENCY_MISMATCH
     * cases here.
     */
    private Optional<Defect> detect(CorporateActionEntry entry)
    {
        Map<LegRole, Integer> counts = new EnumMap<>(LegRole.class);
        for (Leg leg : entry.getLegs())
            counts.merge(leg.role(), 1, Integer::sum);

        // 1. mandatory legs present
        if (counts.getOrDefault(LegRole.SOURCE, 0) == 0 || counts.getOrDefault(LegRole.TARGET, 0) == 0)
            return Optional.of(Defect.MISSING_LEG);

        // 2. no duplicate role
        for (int count : counts.values())
            if (count > 1)
                return Optional.of(Defect.DUPLICATE_LEG);

        // 3. the fractional legs are paired (both or neither)
        boolean hasFractionSale = counts.getOrDefault(LegRole.FRACTION_SALE, 0) == 1;
        boolean hasCashInLieu = counts.getOrDefault(LegRole.CASH_IN_LIEU, 0) == 1;
        if (hasFractionSale != hasCashInLieu)
            return Optional.of(Defect.UNPAIRED_FRACTION);

        // 4. every leg is still owned by its owner and back-points to this entry
        for (Leg leg : entry.getLegs())
        {
            boolean ownedHere = leg.owner().getTransactions().contains(leg.transaction());
            boolean backPointsHere = leg.transaction().getCrossEntry() == entry;
            if (!ownedHere || !backPointsHere)
                return Optional.of(Defect.SEVERED_LEG);
        }

        // 5. basis ratio in the open interval (0, 1) (spec §4)
        BigDecimal basisRatio = entry.getBasisRatio();
        if (basisRatio == null || basisRatio.signum() <= 0 || basisRatio.compareTo(BigDecimal.ONE) >= 0)
            return Optional.of(Defect.BASIS_RATIO_OUT_OF_RANGE);

        // 6. currency: event currency = source security currency, and both
        // mandatory legs share it (spec §6). The distribution ratio is NOT
        // consulted -- it is a 1/1 provenance placeholder, not authoritative.
        Transaction source = entry.getLeg(LegRole.SOURCE).orElseThrow();
        Transaction target = entry.getLeg(LegRole.TARGET).orElseThrow();
        Security sourceSecurity = source.getSecurity();
        if (sourceSecurity != null && !source.getCurrencyCode().equals(sourceSecurity.getCurrencyCode()))
            return Optional.of(Defect.CURRENCY_MISMATCH);
        if (!source.getCurrencyCode().equals(target.getCurrencyCode()))
            return Optional.of(Defect.CURRENCY_MISMATCH);

        return Optional.empty();
    }

    /* package */ static class DeleteCorporateActionFix implements QuickFix
    {
        private final Client client;
        private final CorporateActionEntry entry;

        DeleteCorporateActionFix(Client client, CorporateActionEntry entry)
        {
            this.client = client;
            this.entry = entry;
        }

        @Override
        public String getLabel()
        {
            return Messages.FixDeleteSpinOff;
        }

        @Override
        public String getDoneLabel()
        {
            return Messages.FixDeleteTransactionDone;
        }

        @Override
        public void execute()
        {
            // remove every leg from its owner (best-effort; shallowDelete is a
            // no-op if a leg was already severed) and drop the registry entry.
            // Uses shallowDeleteTransaction, not the Phase-5 N-ary
            // deleteTransaction cascade, so it is robust to broken back-pointers
            // -- which is exactly when this fix is offered.
            for (Leg leg : entry.getLegs())
                shallowDelete(leg, client);
            client.removeCorporateAction(entry);
        }

        @SuppressWarnings("unchecked")
        private static void shallowDelete(Leg leg, Client client)
        {
            TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) leg.owner();
            owner.shallowDeleteTransaction(leg.transaction(), client);
        }
    }
}
