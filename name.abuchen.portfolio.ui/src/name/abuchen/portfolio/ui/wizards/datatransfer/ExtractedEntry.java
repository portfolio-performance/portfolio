package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Security;

public class ExtractedEntry
{
    private final Extractor.Item item;

    private Boolean isImported = null;
    private Status.Code maxCode = Status.Code.OK;
    private List<Status> status = new ArrayList<>();

    /**
     * If non-null, the security dependency tells which other extracted items
     * represents the security which this extracted items (typically a
     * transactions) requires.
     */
    private ExtractedEntry securityDependency;

    /**
     * If non-null, then the security dependency is overwritten by the given
     * security.
     */
    private Security securityOverride;

    /**
     * The entry this entry belongs to (e.g. the transfer of an additional fee
     * or tax entry). The entry is only imported together with its owner.
     */
    private ExtractedEntry owner;

    public ExtractedEntry(Item item)
    {
        this.item = item;
    }

    public Extractor.Item getItem()
    {
        return item;
    }

    /**
     * Sets the entry this entry belongs to, e.g. the transfer of the
     * additional fee and tax entries created when the user changes the type
     * of an entry. The entry is not imported if its owner is not imported.
     */
    public void setOwner(ExtractedEntry owner)
    {
        this.owner = owner;
    }

    /**
     * Creates an entry for the given item which takes over the choice of the
     * user (import or not), the security dependency and the security override
     * of this entry. Used when the user changes the type of an entry.
     */
    public ExtractedEntry copyWith(Extractor.Item newItem)
    {
        var copy = new ExtractedEntry(newItem);
        copy.isImported = isImported;
        copy.securityDependency = securityDependency;
        copy.securityOverride = securityOverride;
        return copy;
    }

    public void setImported(boolean isImported)
    {
        this.isImported = isImported;
    }

    public boolean isImported()
    {
        // do not import if explicitly excluded by the user
        if (isImported != null && !isImported.booleanValue())
            return false;

        // do not import if the entry belongs to an entry which is not imported
        if (owner != null && !owner.isImported())
            return false;

        // do not import if the entry has a dependency which is not imported
        // while there is no security override
        if (securityDependency != null && !securityDependency.isImported() && securityOverride == null)
            return false;

        // otherwise import if either the status is OK or the user explicitly
        // overrides warnings
        return maxCode == Status.Code.OK
                        || (maxCode == Status.Code.WARNING && isImported != null && isImported.booleanValue()
                                        || (maxCode == Status.Code.WARNING && item.isInvestmentPlanItem()));
    }

    public void addStatus(ImportAction.Status status)
    {
        // do not add the status if the message is already present
        if (this.status.stream().anyMatch(
                        s -> Objects.equals(s.getMessage(), status.getMessage()) && s.getCode() == status.getCode()))
            return;

        this.status.add(status);
        if (status.getCode().isHigherSeverityAs(maxCode))
            maxCode = status.getCode();
    }

    public Stream<Status> getStatus()
    {
        return this.status.stream();
    }

    public Status.Code getMaxCode()
    {
        return maxCode;
    }

    public void clearStatus()
    {
        this.status.clear();
        this.maxCode = Status.Code.OK;
    }

    public ExtractedEntry getSecurityDependency()
    {
        return securityDependency;
    }

    public void setSecurityDependency(ExtractedEntry securityDependency)
    {
        this.securityDependency = securityDependency;
    }

    public Security getSecurityOverride()
    {
        return securityOverride;
    }

    public void setSecurityOverride(Security securityOverride)
    {
        this.securityOverride = securityOverride;
    }
}
