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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
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

    public static LedgerPosting primaryPosting(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(projectionRef);

        var primaryMembership = projectionRef.getPrimaryMembership();
        if (primaryMembership.isPresent())
            return requirePostingInEntry(entry, primaryMembership.get().getPostingUUID());

        if (projectionRef.getPrimaryPostingUUID() != null)
            return requirePostingInEntry(entry, projectionRef.getPrimaryPostingUUID());

        return switch (projectionRef.getRole())
        {
            case SOURCE_ACCOUNT -> firstAccountPosting(entry, projectionRef);
            case TARGET_ACCOUNT -> lastAccountPosting(entry, projectionRef);
            case SOURCE_PORTFOLIO -> firstPortfolioPosting(entry, projectionRef);
            case TARGET_PORTFOLIO -> lastPortfolioPosting(entry, projectionRef);
            case ACCOUNT, CASH_COMPENSATION -> firstAccountPosting(entry, projectionRef);
            case PORTFOLIO, DELIVERY, DELIVERY_INBOUND, DELIVERY_OUTBOUND, OLD_SECURITY_LEG, NEW_SECURITY_LEG ->
                firstPortfolioPosting(entry, projectionRef);
        };
    }

    static Optional<PostingForex> primaryPostingForex(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return postingForex(primaryPosting(entry, projectionRef));
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

    static Stream<Unit> units(LedgerEntry entry, LedgerProjectionRef projectionRef, LedgerPosting primaryPosting)
    {
        if (entry.getType().isLedgerNativeTargeted())
            return targetedUnits(entry, projectionRef, primaryPosting).map(LedgerProjectionSupport::unit);

        return entry.getPostings().stream() //
                        .filter(posting -> posting != primaryPosting) //
                        .filter(LedgerProjectionSupport::isUnitPosting) //
                        .map(LedgerProjectionSupport::unit);
    }

    static AccountTransaction.Type targetedAccountType(LedgerProjectionRef projectionRef)
    {
        return switch (projectionRef.getRole())
        {
            case CASH_COMPENSATION -> AccountTransaction.Type.DEPOSIT;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_072
                            .message("Unsupported targeted account role " + projectionRef.getRole())); //$NON-NLS-1$
        };
    }

    static PortfolioTransaction.Type targetedPortfolioType(LedgerProjectionRef projectionRef)
    {
        return switch (projectionRef.getRole())
        {
            case DELIVERY_OUTBOUND -> PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            case DELIVERY_INBOUND -> PortfolioTransaction.Type.DELIVERY_INBOUND;
            case OLD_SECURITY_LEG -> PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            case NEW_SECURITY_LEG -> PortfolioTransaction.Type.DELIVERY_INBOUND;
            default -> throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_073
                            .message("Unsupported targeted portfolio role " + projectionRef.getRole())); //$NON-NLS-1$
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

    static boolean isAccountProjection(LedgerProjectionRef projectionRef)
    {
        return switch (projectionRef.getRole())
        {
            case ACCOUNT, SOURCE_ACCOUNT, TARGET_ACCOUNT, CASH_COMPENSATION -> true;
            default -> false;
        };
    }

    static boolean isPortfolioProjection(LedgerProjectionRef projectionRef)
    {
        return switch (projectionRef.getRole())
        {
            case PORTFOLIO, SOURCE_PORTFOLIO, TARGET_PORTFOLIO, DELIVERY, DELIVERY_INBOUND, DELIVERY_OUTBOUND,
                            OLD_SECURITY_LEG, NEW_SECURITY_LEG -> true;
            default -> false;
        };
    }

    private static LedgerPosting firstAccountPosting(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return accountPostings(entry, projectionRef).findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "No account posting for projection " + projectionRef.getUUID())); //$NON-NLS-1$
    }

    private static LedgerPosting requirePostingInEntry(LedgerEntry entry, String uuid)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> uuid.equals(posting.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_001
                                        .message("Primary posting does not exist in entry: " + uuid))); //$NON-NLS-1$
    }

    private static LedgerPosting lastAccountPosting(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        var postings = accountPostings(entry, projectionRef).toList();

        if (postings.isEmpty())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_074
                            .message("No account posting for projection " + projectionRef.getUUID())); //$NON-NLS-1$

        return postings.get(postings.size() - 1);
    }

    private static Stream<LedgerPosting> accountPostings(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getAccount() == projectionRef.getAccount());
    }

    private static LedgerPosting firstPortfolioPosting(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return portfolioPostings(entry, projectionRef).findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "No portfolio posting for projection " + projectionRef.getUUID())); //$NON-NLS-1$
    }

    private static LedgerPosting lastPortfolioPosting(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        List<LedgerPosting> postings = portfolioPostings(entry, projectionRef).toList();

        if (postings.isEmpty())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_075
                            .message("No portfolio posting for projection " + projectionRef.getUUID())); //$NON-NLS-1$

        return postings.get(postings.size() - 1);
    }

    private static Stream<LedgerPosting> portfolioPostings(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getPortfolio() == projectionRef.getPortfolio());
    }

    private static boolean isUnitPosting(LedgerPosting posting)
    {
        return switch (posting.getType())
        {
            case FEE, TAX, GROSS_VALUE -> true;
            default -> false;
        };
    }

    private static Stream<LedgerPosting> targetedUnits(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    LedgerPosting primaryPosting)
    {
        var unitMemberships = projectionRef.getMemberships().stream() //
                        .filter(membership -> isUnitMembershipRole(membership.getRole())) //
                        .toList();

        if (!unitMemberships.isEmpty())
            return unitMemberships.stream() //
                            .map(membership -> requirePostingInEntry(entry, membership.getPostingUUID())) //
                            .filter(posting -> posting != primaryPosting) //
                            .filter(LedgerProjectionSupport::isUnitPosting);

        var groupAnchorUUID = projectionRef.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream()
                        .findFirst() //
                        .map(membership -> membership.getPostingUUID()) //
                        .orElse(projectionRef.getPostingGroupUUID());

        if (groupAnchorUUID == null || !groupAnchorUUID.equals(primaryPosting.getUUID()))
            return Stream.empty();

        return entry.getPostings().stream() //
                        .filter(posting -> posting != primaryPosting) //
                        .filter(LedgerProjectionSupport::isUnitPosting) //
                        .filter(posting -> hasSameProjectionOwner(primaryPosting, posting));
    }

    private static boolean isUnitMembershipRole(ProjectionMembershipRole role)
    {
        return switch (role)
        {
            case FEE_UNIT, TAX_UNIT, GROSS_VALUE_UNIT -> true;
            default -> false;
        };
    }

    private static boolean hasSameProjectionOwner(LedgerPosting primaryPosting, LedgerPosting posting)
    {
        if (primaryPosting.getAccount() != null)
            return primaryPosting.getAccount() == posting.getAccount();

        if (primaryPosting.getPortfolio() != null)
            return primaryPosting.getPortfolio() == posting.getPortfolio();

        return false;
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
