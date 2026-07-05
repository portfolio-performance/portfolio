package name.abuchen.portfolio.model.ledger.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.money.Money;

/**
 * Supports runtime projection behavior for ledger-backed legacy transactions.
 * This is projection infrastructure. Projections are views rebuilt from Ledger entries, not
 * independent transaction truth.
 */
public final class LedgerProjectionSupport
{
    record PostingForex(Money amount, BigDecimal exchangeRate)
    {
        PostingForex
        {
            Objects.requireNonNull(amount);
            Objects.requireNonNull(exchangeRate);
        }
    }

    private LedgerProjectionSupport()
    {
    }

    public static LedgerPosting primaryPosting(LedgerEntry entry, LedgerProjectionRole role)
    {
        return descriptor(entry, role).getPrimaryPosting();
    }

    public static DerivedProjectionDescriptor descriptor(LedgerEntry entry, LedgerProjectionRole role)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);

        var matches = descriptors(entry).stream() //
                        .filter(descriptor -> descriptor.getRole() == role) //
                        .toList();

        if (matches.size() == 1)
            return matches.get(0);

        if (matches.isEmpty())
            throw new IllegalArgumentException("Projection descriptor not found: " + role); //$NON-NLS-1$

        throw new IllegalArgumentException("Projection descriptor role is ambiguous: " + role); //$NON-NLS-1$
    }

    public static DerivedProjectionDescriptor descriptor(LedgerEntry entry, LedgerProjectionRole role,
                    String semanticInstanceKey)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(role);
        Objects.requireNonNull(semanticInstanceKey);

        return descriptors(entry).stream() //
                        .filter(descriptor -> descriptor.getRole() == role) //
                        .filter(descriptor -> descriptor.getSemanticInstanceKey()
                                        .filter(semanticInstanceKey::equals).isPresent()) //
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                        "Projection descriptor not found: " + role + "/" + semanticInstanceKey)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static List<DerivedProjectionDescriptor> descriptors(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var result = new DerivedProjectionDescriptorService().derive(entry);

        if (!result.isOK())
            throw new IllegalArgumentException(result.formatDiagnostics());

        return result.getDescriptors();
    }

    static Optional<PostingForex> primaryPostingForex(DerivedProjectionDescriptor descriptor)
    {
        return postingForex(descriptor.getPrimaryPosting());
    }

    private static Optional<PostingForex> postingForex(LedgerPosting posting)
    {
        Objects.requireNonNull(posting);

        if (posting.getForexAmount() == null || posting.getForexCurrency() == null
                        || posting.getExchangeRate() == null)
            return Optional.empty();

        return Optional.of(new PostingForex(Money.of(posting.getForexCurrency(), posting.getForexAmount()),
                        posting.getExchangeRate()));
    }

    static Stream<Unit> units(DerivedProjectionDescriptor descriptor)
    {
        return descriptor.getUnitPostings().stream().map(LedgerProjectionSupport::unit);
    }

    static AccountTransaction.Type targetedAccountType(DerivedProjectionDescriptor descriptor)
    {
        return switch (descriptor.getRole())
        {
            case CASH_COMPENSATION -> AccountTransaction.Type.DEPOSIT;
            case ACCOUNT -> corporateActionAccountType(descriptor);
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_072
                            .message("Unsupported targeted account role " + descriptor.getRole())); //$NON-NLS-1$
        };
    }

    private static AccountTransaction.Type corporateActionAccountType(DerivedProjectionDescriptor descriptor)
    {
        var kind = CorporateActionKind.fromEntry(descriptor.getEntry()).orElse(null);

        if (kind == null)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_072
                            .message("Unsupported targeted account kind " + kind)); //$NON-NLS-1$

        return switch (kind)
        {
            case CASH_DISTRIBUTION -> AccountTransaction.Type.DIVIDENDS;
            case COUPON_PAYMENT -> AccountTransaction.Type.INTEREST;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_072
                            .message("Unsupported targeted account kind " + kind)); //$NON-NLS-1$
        };
    }

    static Optional<Security> securityContext(LedgerEntry entry)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getCorporateActionLeg() == CorporateActionLeg.SECURITY_CONTEXT) //
                        .filter(posting -> posting.getSecurity() != null) //
                        .map(LedgerPosting::getSecurity) //
                        .findFirst();
    }

    static PortfolioTransaction.Type targetedPortfolioType(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case DELIVERY_OUTBOUND -> PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            case DELIVERY_INBOUND -> PortfolioTransaction.Type.DELIVERY_INBOUND;
            case OLD_SECURITY_LEG -> PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            case NEW_SECURITY_LEG -> PortfolioTransaction.Type.DELIVERY_INBOUND;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_073
                            .message("Unsupported targeted portfolio role " + role)); //$NON-NLS-1$
        };
    }

    static Optional<LocalDateTime> exDate(LedgerPosting posting)
    {
        return posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE) //
                        .filter(parameter -> parameter.getValueKind() == LedgerParameter.ValueKind.LOCAL_DATE_TIME) //
                        .map(LedgerParameter::getValue) //
                        .filter(LocalDateTime.class::isInstance) //
                        .map(LocalDateTime.class::cast) //
                        .findFirst();
    }

    static UnsupportedOperationException unsupportedMutation()
    {
        return new UnsupportedOperationException("Ledger-backed projections are read-only"); //$NON-NLS-1$
    }

    static boolean isAccountProjection(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case ACCOUNT, SOURCE_ACCOUNT, TARGET_ACCOUNT, CASH_COMPENSATION -> true;
            default -> false;
        };
    }

    static boolean isPortfolioProjection(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case PORTFOLIO, SOURCE_PORTFOLIO, TARGET_PORTFOLIO, DELIVERY, DELIVERY_INBOUND, DELIVERY_OUTBOUND,
                            OLD_SECURITY_LEG, NEW_SECURITY_LEG -> true;
            default -> false;
        };
    }

    private static Unit unit(LedgerPosting posting)
    {
        var type = switch (posting.getType())
        {
            case FEE -> Unit.Type.FEE;
            case TAX -> Unit.Type.TAX;
            case GROSS_VALUE -> Unit.Type.GROSS_VALUE;
            default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_076
                            .message("Posting is not a unit posting: " + posting.getType())); //$NON-NLS-1$
        };
        var amount = Money.of(posting.getCurrency(), posting.getAmount());

        if (posting.getForexAmount() != null && posting.getForexCurrency() != null && posting.getExchangeRate() != null)
            return new Unit(type, amount, Money.of(posting.getForexCurrency(), posting.getForexAmount()),
                            posting.getExchangeRate());

        return new Unit(type, amount);
    }
}
