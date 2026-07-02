package name.abuchen.portfolio.model.ledger;

import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests structural validation for ledger entries.
 */
@SuppressWarnings("nls")
public class LedgerStructuralValidatorTest
{
    @Test
    public void testEmptyLedgerIsValid()
    {
        assertOK(LedgerStructuralValidator.validate(new Ledger()));
    }

    @Test
    public void testSimpleValidLedgerEntryPassesValidation()
    {
        assertOK(LedgerStructuralValidator.validate(createStandardLedger()));
    }

    private Ledger createStandardLedger()
    {
        var ledger = new Ledger();
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();

        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        posting.setType(LedgerPostingType.CASH);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(LedgerProjectionRole.ACCOUNT.name());

        entry.addPosting(posting);
        ledger.addEntry(entry);

        return ledger;
    }

    private void assertOK(LedgerStructuralValidator.ValidationResult result)
    {
        assertTrue(result.format(), result.isOK());
    }
}
