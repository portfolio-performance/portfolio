package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Builds Ledger model objects while loading persisted files.
 * This is persistence infrastructure. Normal Ledger construction should use creators,
 * native assemblers, or mutation contexts instead of this load-only support.
 */
final class LedgerModelLoadSupport
{
    private LedgerModelLoadSupport()
    {
    }

    static LedgerEntry newEntry(String uuid, LedgerEntryType type, LocalDateTime dateTime)
    {
        var entry = new LedgerEntry(uuid);

        entry.setType(Objects.requireNonNull(type));
        entry.setDateTime(Objects.requireNonNull(dateTime));

        return entry;
    }

    static void setEntryNote(LedgerEntry entry, String note)
    {
        Objects.requireNonNull(entry).setNote(note);
    }

    static void setEntrySource(LedgerEntry entry, String source)
    {
        Objects.requireNonNull(entry).setSource(source);
    }

    static void setEntryUpdatedAt(LedgerEntry entry, Instant updatedAt)
    {
        Objects.requireNonNull(entry).setUpdatedAt(updatedAt);
    }

    static void addEntry(Ledger ledger, LedgerEntry entry)
    {
        Objects.requireNonNull(ledger).addEntry(entry);
    }

    static void addEntryParameter(LedgerEntry entry, LedgerParameter<?> parameter)
    {
        Objects.requireNonNull(entry).addParameter(parameter);
    }

    static void addPosting(LedgerEntry entry, LedgerPosting posting)
    {
        Objects.requireNonNull(entry).addPosting(posting);
    }

    static void addProjectionRef(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        Objects.requireNonNull(entry).addProjectionRef(projectionRef);
    }

    static LedgerPosting newPosting(String uuid, LedgerPostingType type)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(Objects.requireNonNull(type));

        return posting;
    }

    static void setPostingAmount(LedgerPosting posting, long amount)
    {
        Objects.requireNonNull(posting).setAmount(amount);
    }

    static void setPostingCurrency(LedgerPosting posting, String currency)
    {
        Objects.requireNonNull(posting).setCurrency(currency);
    }

    static void setPostingForexAmount(LedgerPosting posting, Long forexAmount)
    {
        Objects.requireNonNull(posting).setForexAmount(forexAmount);
    }

    static void setPostingForexCurrency(LedgerPosting posting, String forexCurrency)
    {
        Objects.requireNonNull(posting).setForexCurrency(forexCurrency);
    }

    static void setPostingExchangeRate(LedgerPosting posting, BigDecimal exchangeRate)
    {
        Objects.requireNonNull(posting).setExchangeRate(exchangeRate);
    }

    static void setPostingSecurity(LedgerPosting posting, Security security)
    {
        Objects.requireNonNull(posting).setSecurity(security);
    }

    static void setPostingShares(LedgerPosting posting, long shares)
    {
        Objects.requireNonNull(posting).setShares(shares);
    }

    static void setPostingAccount(LedgerPosting posting, Account account)
    {
        Objects.requireNonNull(posting).setAccount(account);
    }

    static void setPostingPortfolio(LedgerPosting posting, Portfolio portfolio)
    {
        Objects.requireNonNull(posting).setPortfolio(portfolio);
    }

    static void addPostingParameter(LedgerPosting posting, LedgerParameter<?> parameter)
    {
        Objects.requireNonNull(posting).addParameter(parameter);
    }

    static LedgerProjectionRef newProjectionRef(String uuid, LedgerProjectionRole role)
    {
        var projectionRef = new LedgerProjectionRef(uuid);

        projectionRef.setRole(Objects.requireNonNull(role));

        return projectionRef;
    }

    static void setProjectionRefAccount(LedgerProjectionRef projectionRef, Account account)
    {
        Objects.requireNonNull(projectionRef).setAccount(account);
    }

    static void setProjectionRefPortfolio(LedgerProjectionRef projectionRef, Portfolio portfolio)
    {
        Objects.requireNonNull(projectionRef).setPortfolio(portfolio);
    }

    static void setProjectionRefPrimaryPostingUUID(LedgerProjectionRef projectionRef, String primaryPostingUUID)
    {
        Objects.requireNonNull(projectionRef).setPrimaryPostingUUID(primaryPostingUUID);
    }

    static void setProjectionRefPostingGroupUUID(LedgerProjectionRef projectionRef, String postingGroupUUID)
    {
        Objects.requireNonNull(projectionRef).setPostingGroupUUID(postingGroupUUID);
    }

    static void addProjectionRefMembership(LedgerProjectionRef projectionRef, String postingUUID,
                    ProjectionMembershipRole role)
    {
        Objects.requireNonNull(projectionRef).addMembership(postingUUID, role);
    }
}
