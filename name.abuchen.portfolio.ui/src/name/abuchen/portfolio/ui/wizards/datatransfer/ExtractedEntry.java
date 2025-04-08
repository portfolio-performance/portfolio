package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.List;
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
     * If non null, the security dependency tells which other extracted items
     * represents the security which this extracted items (typically a
     * transactions) requires.
     */
    private ExtractedEntry securityDependency;

    /**
     * If non null, then the security dependency is overwritten by the given
     * security.
     */
    private Security securityOverride;

    public ExtractedEntry(Item item)
    {
        this.item = item;
    }

    public Extractor.Item getItem()
    {
        return item;
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
