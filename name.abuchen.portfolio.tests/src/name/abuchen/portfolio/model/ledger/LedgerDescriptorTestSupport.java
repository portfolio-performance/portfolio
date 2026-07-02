package name.abuchen.portfolio.model.ledger;

import java.util.List;

import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

public final class LedgerDescriptorTestSupport
{
    private LedgerDescriptorTestSupport()
    {
    }

    public static List<DerivedProjectionDescriptor> descriptors(LedgerEntry entry)
    {
        return LedgerProjectionSupport.descriptors(entry);
    }

    public static DerivedProjectionDescriptor descriptor(LedgerEntry entry, LedgerProjectionRole role)
    {
        return LedgerProjectionSupport.descriptor(entry, role);
    }

    public static String runtimeProjectionId(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getUUID() + ":" + role; //$NON-NLS-1$
    }
}
