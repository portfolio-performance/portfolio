package name.abuchen.portfolio.model.ledger.projection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Coordinates materialization and refresh of runtime projections for Ledger entries.
 * This is projection infrastructure. Contributor code should use it only when restoring or
 * refreshing views from Ledger truth.
 */
public final class LedgerProjectionService
{
    private LedgerProjectionService()
    {
    }

    public static void materialize(Client client)
    {
        new LedgerProjectionMaterializer().materialize(client);
    }

    public static Transaction createProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return new LedgerProjectionFactory().createProjection(entry, role);
    }

    public static List<Transaction> createProjections(LedgerEntry entry)
    {
        return new LedgerProjectionFactory().createProjections(entry);
    }

    public static LedgerStructuralValidator.ValidationResult restoreIfValid(Client client)
    {
        return new LedgerRuntimeProjectionRestorer().restoreIfValid(client);
    }

    public static void logSkipped(LedgerStructuralValidator.ValidationResult result)
    {
        LedgerRuntimeProjectionRestorer.logSkipped(result);
    }

    public static void logSkipped(Ledger ledger, LedgerStructuralValidator.ValidationResult result)
    {
        LedgerRuntimeProjectionRestorer.logSkipped(ledger, result);
    }
}
