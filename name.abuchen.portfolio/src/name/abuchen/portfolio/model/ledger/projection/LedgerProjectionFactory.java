package name.abuchen.portfolio.model.ledger.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Builds runtime legacy projection objects for a Ledger entry.
 * This is projection infrastructure. The created objects are compatibility views, not
 * persisted transaction facts.
 */
final class LedgerProjectionFactory
{
    private final DerivedProjectionDescriptorService descriptorService;

    LedgerProjectionFactory()
    {
        this(new DerivedProjectionDescriptorService());
    }

    LedgerProjectionFactory(DerivedProjectionDescriptorService descriptorService)
    {
        this.descriptorService = Objects.requireNonNull(descriptorService);
    }

    Transaction createProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);

        var descriptors = createDescriptors(entry).stream() //
                        .filter(descriptor -> descriptor.getRole() == role) //
                        .toList();

        if (descriptors.size() == 1)
            return create(descriptors.get(0));

        if (descriptors.isEmpty())
            throw new IllegalArgumentException("Projection descriptor does not belong to entry role: " + role); //$NON-NLS-1$

        throw new IllegalArgumentException("Projection descriptor role is ambiguous: " + role); //$NON-NLS-1$
    }

    Transaction createProjection(LedgerEntry entry, LedgerProjectionRole role, String semanticInstanceKey)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);
        Objects.requireNonNull(semanticInstanceKey);

        return createDescriptors(entry).stream() //
                        .filter(descriptor -> descriptor.getRole() == role) //
                        .filter(descriptor -> descriptor.getSemanticInstanceKey()
                                        .filter(semanticInstanceKey::equals).isPresent()) //
                        .map(this::create) //
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                        "Projection descriptor does not belong to entry role and instance: " //$NON-NLS-1$
                                                        + role + "/" + semanticInstanceKey)); //$NON-NLS-1$
    }

    List<Transaction> createProjections(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var transactions = new ArrayList<Transaction>();

        for (var descriptor : createDescriptors(entry))
            transactions.add(create(descriptor));

        attachCrossEntry(entry, transactions);

        return List.copyOf(transactions);
    }

    private List<DerivedProjectionDescriptor> createDescriptors(LedgerEntry entry)
    {
        if (entry.getType() != null && !entry.getType().supportsDerivedDescriptors())
            return List.of();

        var result = descriptorService.derive(entry);

        if (!result.isOK())
            throw new IllegalArgumentException(result.formatDiagnostics());

        return result.getDescriptors();
    }

    private Transaction create(DerivedProjectionDescriptor descriptor)
    {
        if (descriptor.getViewKind() == DerivedProjectionViewKind.ACCOUNT)
            return new LedgerBackedAccountTransaction(descriptor);

        if (descriptor.getViewKind() == DerivedProjectionViewKind.PORTFOLIO)
            return new LedgerBackedPortfolioTransaction(descriptor);

        throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_002
                        .message("Unsupported ledger projection role " + descriptor.getRole())); //$NON-NLS-1$
    }

    private void attachCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        if (transactions.size() != 2)
            return;

        var crossEntry = new LedgerBackedCrossEntry(entry, transactions);

        for (var transaction : transactions)
        {
            if (transaction instanceof LedgerBackedAccountTransaction accountTransaction)
                accountTransaction.setLedgerCrossEntry(crossEntry);
            else if (transaction instanceof LedgerBackedPortfolioTransaction portfolioTransaction)
                portfolioTransaction.setLedgerCrossEntry(crossEntry);
        }
    }
}
