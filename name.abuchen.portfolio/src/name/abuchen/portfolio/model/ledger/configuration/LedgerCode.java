package name.abuchen.portfolio.model.ledger.configuration;

/**
 * Defines the code Ledger code domain used by configuration or validation.
 * This is configuration metadata. Normal transaction-editing code should not treat these
 * values as a direct mutation API.
 *
 * <p>
 * Implementations provide stable string codes for controlled {@link LedgerParameterType}
 * values. The interface is not persisted as a standalone object, but its returned code can
 * be written as a Ledger parameter string value.
 * </p>
 */
public interface LedgerCode
{
    LedgerParameterCodeDomain getDomain();

    String getCode();
}
