package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntryEditSupport;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

/**
 * Applies composite ledger-backed portfolio type conversions as one atomic Ledger mutation.
 * This covers the inline transitions that need both a direction reversal and a shape conversion.
 */
public final class LedgerPortfolioCompositeTypeConverter
{
    private static final BigDecimal DEFAULT_EXCHANGE_RATE = BigDecimal.ONE;

    private final Client client;

    public LedgerPortfolioCompositeTypeConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public boolean canConvertSafely(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        try
        {
            if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
                return false;

            prepare(ledgerTransaction, transaction);
            return true;
        }
        catch (RuntimeException e)
        {
            return false;
        }
    }

    public PortfolioTransaction convert(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_CONVERT_051.message("Only ledger-backed portfolio transactions can use composite conversion")); //$NON-NLS-1$

        var operation = prepare(ledgerTransaction, transaction);

        new LedgerMutationContext(client).mutateEntry(operation.entry(), operation::apply);
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, operation.entry(), operation.roleChange());

        return find(operation.portfolio(), operation.portfolioProjectionUUID());
    }

    private Operation prepare(LedgerBackedPortfolioTransaction ledgerTransaction,
                    TransactionPair<PortfolioTransaction> transaction)
    {
        var entry = ledgerTransaction.getLedgerEntry();
        var descriptor = ledgerTransaction.getLedgerProjectionDescriptor();
        var type = entry.getType();

        if (type == LedgerEntryType.BUY || type == LedgerEntryType.SELL)
            return prepareBuySellToOppositeDelivery(entry, descriptor, transaction);

        if (type == LedgerEntryType.DELIVERY_INBOUND || type == LedgerEntryType.DELIVERY_OUTBOUND)
            return prepareDeliveryToOppositeBuySell(entry, descriptor, transaction);

        throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_052.message("Unsupported composite portfolio conversion: " + type)); //$NON-NLS-1$
    }

    private Operation prepareBuySellToOppositeDelivery(LedgerEntry entry, DerivedProjectionDescriptor descriptor,
                    TransactionPair<PortfolioTransaction> transaction)
    {
        var portfolio = descriptor.getPortfolio();
        var oldRuntimeProjectionId = descriptor.getRuntimeProjectionId();

        preflightBuySell(entry, descriptor, transaction, portfolio);

        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND;
        var targetRole = role(targetType);
        var targetRuntimeProjectionId = runtimeProjectionId(entry, targetRole);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(oldRuntimeProjectionId,
                        LedgerProjectionRole.PORTFOLIO, targetRole);

        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);
        LedgerEntryEditSupport.validatePatch(entry, this::applyBuySellToOppositeDelivery);

        return new Operation(entry, portfolio, targetRuntimeProjectionId, roleChange,
                        this::applyBuySellToOppositeDelivery);
    }

    private Operation prepareDeliveryToOppositeBuySell(LedgerEntry entry, DerivedProjectionDescriptor descriptor,
                    TransactionPair<PortfolioTransaction> transaction)
    {
        var portfolio = descriptor.getPortfolio();
        var account = requireReferenceAccount(portfolio);
        var oldRuntimeProjectionId = descriptor.getRuntimeProjectionId();
        var targetRuntimeProjectionId = runtimeProjectionId(entry, LedgerProjectionRole.PORTFOLIO);

        preflightDelivery(entry, descriptor, transaction, portfolio);

        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(oldRuntimeProjectionId, descriptor.getRole(),
                        LedgerProjectionRole.PORTFOLIO);

        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);
        LedgerEntryEditSupport.validatePatch(entry,
                        editedEntry -> applyDeliveryToOppositeBuySell(editedEntry, account));

        return new Operation(entry, portfolio, targetRuntimeProjectionId, roleChange,
                        editedEntry -> applyDeliveryToOppositeBuySell(editedEntry, account));
    }

    private void preflightBuySell(LedgerEntry entry, DerivedProjectionDescriptor descriptor,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_043.message("Only ledger-backed buy/sell entries can be converted")); //$NON-NLS-1$

        if (descriptor.getRole() != LedgerProjectionRole.PORTFOLIO)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_044.message("Only the portfolio side of a buy/sell entry can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_045.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        var accountProjection = uniqueProjection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = uniqueProjection(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPosting = requireOnePosting(entry, LedgerPostingType.CASH);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (!portfolioProjection.getRuntimeProjectionId().equals(descriptor.getRuntimeProjectionId()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_046.message("Selected projection is not the unique portfolio projection")); //$NON-NLS-1$

        if (accountProjection.getPrimaryPosting() != cashPosting
                        || portfolioProjection.getPrimaryPosting() != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_047.message("Buy/sell projection primary postings are ambiguous")); //$NON-NLS-1$

        if (cashPosting.getAccount() != accountProjection.getAccount()
                        || securityPosting.getPortfolio() != portfolioProjection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_048.message("Buy/sell projection and posting owners do not match")); //$NON-NLS-1$

        rejectPostingForex(cashPosting);
        rejectPostingForex(securityPosting);
        reversedBuySellAmount(entry, entry.getType());
    }

    private void preflightDelivery(LedgerEntry entry, DerivedProjectionDescriptor descriptor,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_049.message("Only ledger-backed delivery entries can be converted")); //$NON-NLS-1$

        var expectedRole = role(entry.getType());

        if (descriptor.getRole() != expectedRole)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_050.message("Only the delivery projection can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_051.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        if (entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.CASH))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_053.message("Ledger delivery entry must not already have a cash posting")); //$NON-NLS-1$

        if (hasProjection(entry, LedgerProjectionRole.ACCOUNT))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_052.message("Ledger delivery entry must not already have an account projection")); //$NON-NLS-1$

        var projection = uniqueProjection(entry, expectedRole);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (!projection.getRuntimeProjectionId().equals(descriptor.getRuntimeProjectionId()))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_053.message("Selected projection is not the unique delivery projection")); //$NON-NLS-1$

        if (projection.getPrimaryPosting() != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_054.message("Delivery projection primary posting is ambiguous")); //$NON-NLS-1$

        if (securityPosting.getPortfolio() != projection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_055.message("Delivery projection and posting portfolio do not match")); //$NON-NLS-1$

        rejectPostingForex(securityPosting);
        reversedDeliveryAmount(entry, entry.getType());
    }

    private void applyBuySellToOppositeDelivery(LedgerEntry entry)
    {
        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND;
        var targetRole = role(targetType);
        var amount = reversedBuySellAmount(entry, entry.getType());
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());

        List.copyOf(entry.getPostings()).stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.CASH) //
                        .forEach(entry::removePosting);

        markPrimary(securityPosting, LedgerPostingSemanticRole.SECURITY, direction(targetRole), targetRole);
        entry.setType(targetType);
    }

    private void applyDeliveryToOppositeBuySell(LedgerEntry entry, Account account)
    {
        var targetType = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerEntryType.SELL
                        : LedgerEntryType.BUY;
        var amount = reversedDeliveryAmount(entry, entry.getType());
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var cashPosting = new LedgerPosting();
        var unitPostings = entry.getPostings().stream() //
                        .filter(posting -> posting != securityPosting) //
                        .toList();

        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());

        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(account);
        applyDeliveryCashPosting(securityPosting, cashPosting, account);

        markPrimary(cashPosting, LedgerPostingSemanticRole.CASH, cashDirection(targetType),
                        LedgerProjectionRole.ACCOUNT);
        markPrimary(securityPosting, LedgerPostingSemanticRole.SECURITY, securityDirection(targetType),
                        LedgerProjectionRole.PORTFOLIO);

        List.copyOf(entry.getPostings()).forEach(entry::removePosting);
        entry.addPosting(cashPosting);
        entry.addPosting(securityPosting);
        unitPostings.forEach(entry::addPosting);

        entry.setType(targetType);
    }

    private Money reversedBuySellAmount(LedgerEntry entry, LedgerEntryType currentType)
    {
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var transactionCurrency = securityPosting.getCurrency();
        var grossAmount = entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE) //
                        .findFirst() //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .orElseGet(() -> Money.of(transactionCurrency, grossValueAmount(entry, currentType)));
        var feesAndTaxes = feesAndTaxes(entry, transactionCurrency);

        return currentType == LedgerEntryType.BUY ? grossAmount.subtract(feesAndTaxes)
                        : grossAmount.add(feesAndTaxes);
    }

    private Money reversedDeliveryAmount(LedgerEntry entry, LedgerEntryType currentType)
    {
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var transactionCurrency = securityPosting.getCurrency();
        var grossAmount = entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE) //
                        .findFirst() //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .orElseGet(() -> Money.of(transactionCurrency, grossValueAmount(entry, currentType)));
        var feesAndTaxes = feesAndTaxes(entry, transactionCurrency);

        return currentType == LedgerEntryType.DELIVERY_INBOUND ? grossAmount.subtract(feesAndTaxes)
                        : grossAmount.add(feesAndTaxes);
    }

    private long grossValueAmount(LedgerEntry entry, LedgerEntryType currentType)
    {
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var feesAndTaxes = feesAndTaxes(entry, securityPosting.getCurrency()).getAmount();

        return switch (currentType)
        {
            case BUY, DELIVERY_INBOUND -> securityPosting.getAmount() - feesAndTaxes;
            case SELL, DELIVERY_OUTBOUND -> securityPosting.getAmount() + feesAndTaxes;
            default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_054.message("Unsupported reversal type " + currentType)); //$NON-NLS-1$
        };
    }

    private Money feesAndTaxes(LedgerEntry entry, String currency)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.FEE
                                        || posting.getType() == LedgerPostingType.TAX) //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .collect(MoneyCollectors.sum(currency));
    }

    private void applyDeliveryCashPosting(LedgerPosting securityPosting, LedgerPosting cashPosting, Account account)
    {
        var accountCurrency = account.getCurrencyCode();

        if (Objects.equals(securityPosting.getCurrency(), accountCurrency))
        {
            cashPosting.setAmount(securityPosting.getAmount());
            cashPosting.setCurrency(accountCurrency);
            return;
        }

        if (hasCompleteForex(securityPosting) && Objects.equals(securityPosting.getForexCurrency(), accountCurrency))
        {
            if (securityPosting.getExchangeRate().signum() <= 0)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_FOREX_004
                                .message("Delivery forex exchange rate is not positive")); //$NON-NLS-1$

            cashPosting.setAmount(securityPosting.getForexAmount());
            cashPosting.setCurrency(accountCurrency);
            cashPosting.setForexAmount(securityPosting.getAmount());
            cashPosting.setForexCurrency(securityPosting.getCurrency());
            cashPosting.setExchangeRate(ExchangeRate.inverse(securityPosting.getExchangeRate()));
            return;
        }

        cashPosting.setAmount(securityPosting.getAmount());
        cashPosting.setCurrency(accountCurrency);
        cashPosting.setForexAmount(securityPosting.getAmount());
        cashPosting.setForexCurrency(securityPosting.getCurrency());
        cashPosting.setExchangeRate(DEFAULT_EXCHANGE_RATE);
    }

    private boolean hasCompleteForex(LedgerPosting posting)
    {
        return posting.getForexAmount() != null && posting.getForexCurrency() != null
                        && posting.getExchangeRate() != null;
    }

    private void rejectPostingForex(LedgerPosting posting)
    {
        if (posting.getForexAmount() != null || posting.getForexCurrency() != null || posting.getExchangeRate() != null)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_FOREX_005
                            .message("Ledger posting forex metadata cannot be reversed")); //$NON-NLS-1$
    }

    private Account requireReferenceAccount(Portfolio portfolio)
    {
        var account = portfolio.getReferenceAccount();

        if (account == null)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_055.message("Delivery portfolio has no reference account")); //$NON-NLS-1$

        return account;
    }

    private LedgerPosting requireOnePosting(LedgerEntry entry, LedgerPostingType type)
    {
        var postings = entry.getPostings().stream().filter(posting -> posting.getType() == type).toList();

        if (postings.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_056.message("Ledger entry must have exactly one " + type + " posting")); //$NON-NLS-1$ //$NON-NLS-2$

        return postings.get(0);
    }

    private DerivedProjectionDescriptor uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = LedgerProjectionSupport.descriptors(entry).stream()
                        .filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_056
                            .message("Expected one projection for role " + role + " but found " //$NON-NLS-1$ //$NON-NLS-2$
                                            + projections.size()));

        return projections.get(0);
    }

    private boolean hasProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return LedgerProjectionSupport.descriptors(entry).stream().anyMatch(projection -> projection.getRole() == role);
    }

    private LedgerProjectionRole role(LedgerEntryType entryType)
    {
        return entryType == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;
    }

    private void markPrimary(LedgerPosting posting, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingDirection direction, LedgerProjectionRole role)
    {
        posting.setSemanticRole(semanticRole);
        posting.setDirection(direction);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setLocalKey(role.name());
    }

    private LedgerPostingDirection cashDirection(LedgerEntryType entryType)
    {
        return switch (entryType)
        {
            case BUY -> LedgerPostingDirection.OUTBOUND;
            case SELL -> LedgerPostingDirection.INBOUND;
            default -> LedgerPostingDirection.NEUTRAL;
        };
    }

    private LedgerPostingDirection securityDirection(LedgerEntryType entryType)
    {
        return switch (entryType)
        {
            case BUY -> LedgerPostingDirection.INBOUND;
            case SELL -> LedgerPostingDirection.OUTBOUND;
            default -> LedgerPostingDirection.NEUTRAL;
        };
    }

    private LedgerPostingDirection direction(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case DELIVERY_OUTBOUND -> LedgerPostingDirection.OUTBOUND;
            case DELIVERY_INBOUND -> LedgerPostingDirection.INBOUND;
            default -> LedgerPostingDirection.NEUTRAL;
        };
    }

    private String runtimeProjectionId(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getUUID() + ":" + role; //$NON-NLS-1$
    }

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Composite ledger portfolio projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private record Operation(LedgerEntry entry, Portfolio portfolio, String portfolioProjectionUUID,
                    LedgerInvestmentPlanRefSupport.RoleChange roleChange, LedgerEntryEditSupport.EntryPatch mutation)
    {
        void apply(LedgerEntry entry)
        {
            mutation.apply(entry);
        }
    }
}
