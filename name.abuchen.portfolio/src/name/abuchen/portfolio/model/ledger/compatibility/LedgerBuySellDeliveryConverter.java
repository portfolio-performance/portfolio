package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * Converts ledger-backed buy/sell transactions to deliveries and back.
 * This class is part of the Ledger compatibility layer for existing UI and action code. It
 * validates the conversion before replacing the Ledger entry shape.
 */
public final class LedgerBuySellDeliveryConverter
{
    private static final BigDecimal DEFAULT_EXCHANGE_RATE = BigDecimal.ONE;

    private final Client client;

    public LedgerBuySellDeliveryConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public PortfolioTransaction convertBuySellToDelivery(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_020.message("Only ledger-backed buy/sell transactions can be converted")); //$NON-NLS-1$

        var entry = ledgerTransaction.getLedgerEntry();
        var descriptor = ledgerTransaction.getLedgerProjectionDescriptor();
        var portfolio = descriptor.getPortfolio();
        var oldRuntimeProjectionId = ledgerTransaction.getRuntimeProjectionId();

        preflightBuySell(entry, ledgerTransaction.getLedgerProjectionRole(), transaction, portfolio);
        var targetRole = entry.getType() == LedgerEntryType.BUY ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;
        var targetRuntimeProjectionId = runtimeProjectionId(entry, targetRole);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(oldRuntimeProjectionId,
                        LedgerProjectionRole.PORTFOLIO, targetRole);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);

        new LedgerMutationContext(client).mutateEntry(entry, this::convert);
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, roleChange);

        return find(portfolio, targetRuntimeProjectionId);
    }

    public BuySellEntry convertDeliveryToBuySell(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_021.message("Only ledger-backed deliveries can be converted")); //$NON-NLS-1$

        var entry = ledgerTransaction.getLedgerEntry();
        var descriptor = ledgerTransaction.getLedgerProjectionDescriptor();
        var portfolio = descriptor.getPortfolio();
        var account = requireReferenceAccount(portfolio);
        var oldRuntimeProjectionId = ledgerTransaction.getRuntimeProjectionId();
        var portfolioRuntimeProjectionId = runtimeProjectionId(entry, LedgerProjectionRole.PORTFOLIO);
        var accountRuntimeProjectionId = runtimeProjectionId(entry, LedgerProjectionRole.ACCOUNT);

        preflightDelivery(entry, ledgerTransaction.getLedgerProjectionRole(), transaction, portfolio, account);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(oldRuntimeProjectionId,
                        ledgerTransaction.getLedgerProjectionRole(), LedgerProjectionRole.PORTFOLIO);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> convertDeliveryToBuySell(editedEntry,
                        account));
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, roleChange);

        var portfolioTransaction = find(portfolio, portfolioRuntimeProjectionId);
        var accountTransaction = find(account, accountRuntimeProjectionId);

        return BuySellEntry.readOnly(portfolio, portfolioTransaction, account, accountTransaction);
    }

    private void preflightBuySell(LedgerEntry entry, LedgerProjectionRole role,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_019.message("Only ledger-backed buy/sell entries can be converted")); //$NON-NLS-1$

        if (role != LedgerProjectionRole.PORTFOLIO)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_020.message("Only the portfolio side of a buy/sell entry can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_021.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        requireOnePosting(entry, LedgerPostingType.CASH);
        requireOnePosting(entry, LedgerPostingType.SECURITY);
        requireOneProjection(entry, LedgerProjectionRole.ACCOUNT);
        requireOneProjection(entry, LedgerProjectionRole.PORTFOLIO);
    }

    private void preflightDelivery(LedgerEntry entry, LedgerProjectionRole role,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio, Account account)
    {
        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_022.message("Only ledger-backed delivery entries can be converted")); //$NON-NLS-1$

        var expectedRole = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;

        if (role != expectedRole)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_023.message("Only the delivery projection can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_024.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        if (entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.CASH))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_022.message("Ledger delivery entry must not already have a cash posting")); //$NON-NLS-1$

        if (hasProjection(entry, LedgerProjectionRole.ACCOUNT))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_025.message("Ledger delivery entry must not already have an account projection")); //$NON-NLS-1$

        requireOnePosting(entry, LedgerPostingType.SECURITY);
        requireOneProjection(entry, expectedRole);
    }

    private Account requireReferenceAccount(Portfolio portfolio)
    {
        var account = portfolio.getReferenceAccount();

        if (account == null)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_023.message("Delivery portfolio has no reference account")); //$NON-NLS-1$

        return account;
    }

    private void convert(LedgerEntry entry)
    {
        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_INBOUND
                        : LedgerEntryType.DELIVERY_OUTBOUND;
        var targetRole = targetType == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;

        List.copyOf(entry.getPostings()).stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.CASH) //
                        .forEach(entry::removePosting);

        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        entry.setType(targetType);
        markPrimary(securityPosting, LedgerPostingSemanticRole.SECURITY, direction(targetRole), targetRole);
    }

    private void convertDeliveryToBuySell(LedgerEntry entry, Account account)
    {
        var targetType = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerEntryType.BUY
                        : LedgerEntryType.SELL;
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var cashPosting = new LedgerPosting();
        var unitPostings = entry.getPostings().stream() //
                        .filter(posting -> posting != securityPosting) //
                        .toList();

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
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_FOREX_003
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

    private LedgerPosting requireOnePosting(LedgerEntry entry, LedgerPostingType type)
    {
        var postings = entry.getPostings().stream().filter(posting -> posting.getType() == type).toList();

        if (postings.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_024.message("Ledger buy/sell entry must have exactly one " + type + " posting")); //$NON-NLS-1$ //$NON-NLS-2$

        return postings.get(0);
    }

    private void requireOneProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var count = LedgerProjectionSupport.descriptors(entry).stream().filter(projection -> projection.getRole() == role)
                        .count();

        if (count != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_026
                            .message("Ledger buy/sell entry must have exactly one " + role + " projection")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private boolean hasProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return LedgerProjectionSupport.descriptors(entry).stream().anyMatch(projection -> projection.getRole() == role);
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
                                        "Converted ledger delivery projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Converted ledger account projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
