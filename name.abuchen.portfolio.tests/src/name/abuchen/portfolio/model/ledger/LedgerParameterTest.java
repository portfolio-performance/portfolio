package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;

/**
 * Tests ledger configuration and validation metadata.
 * These tests make sure entry definitions, posting rules, and parameter domains stay stable for Ledger-V6 transactions.
 */
@SuppressWarnings("nls")
public class LedgerParameterTest
{
    /**
     * Checks the ledger rule scenario: of code accepts matching domain.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testOfCodeAcceptsMatchingDomain()
    {
        var parameter = LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_LEG,
                        CorporateActionLeg.CASH_COMPENSATION);

        assertThat(parameter.getType(), is(LedgerParameterType.CORPORATE_ACTION_LEG));
        assertThat(parameter.getValueKind(), is(ValueKind.STRING));
        assertThat(parameter.getValue(), is(CorporateActionLeg.CASH_COMPENSATION.getCode()));
    }

    /**
     * Checks the ledger rule scenario: of code rejects wrong domain.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testOfCodeRejectsWrongDomain()
    {
        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_LEG,
                                        TaxReason.WITHHOLDING_TAX));

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_CORE_013.prefix()));
        assertThat(exception.getMessage(), containsString("expects code domain")); //$NON-NLS-1$
    }

    /**
     * Checks the ledger rule scenario: of code rejects non code domain parameter.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testOfCodeRejectsNonCodeDomainParameter()
    {
        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerParameter.ofCode(LedgerParameterType.EVENT_REFERENCE,
                                        CorporateActionLeg.CASH_COMPENSATION));

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_CORE_012.prefix()));
        assertThat(exception.getMessage(), containsString("does not define a controlled code domain")); //$NON-NLS-1$
    }

    /**
     * Checks the ledger rule scenario: of code rejects non string parameter.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testOfCodeRejectsNonStringParameter()
    {
        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerParameter.ofCode(LedgerParameterType.SOURCE_SECURITY,
                                        CorporateActionLeg.SOURCE_SECURITY));

        assertThat(exception.getMessage(), containsString(LedgerDiagnosticCode.LEDGER_CORE_021.prefix()));
        assertThat(exception.getMessage(), containsString("does not support STRING")); //$NON-NLS-1$
    }
}
