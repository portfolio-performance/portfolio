package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
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
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var portfolio = projectionRef.getPortfolio();
        var projectionUUID = projectionRef.getUUID();

        preflightBuySell(entry, projectionRef, transaction, portfolio);
        var targetRole = entry.getType() == LedgerEntryType.BUY ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(projectionUUID, LedgerProjectionRole.PORTFOLIO,
                        targetRole);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> convert(editedEntry, projectionUUID));
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, roleChange);

        return find(portfolio, projectionUUID);
    }

    public BuySellEntry convertDeliveryToBuySell(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_021.message("Only ledger-backed deliveries can be converted")); //$NON-NLS-1$

        var entry = ledgerTransaction.getLedgerEntry();
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var portfolio = projectionRef.getPortfolio();
        var account = requireReferenceAccount(portfolio);
        var portfolioProjectionUUID = projectionRef.getUUID();
        var accountProjectionUUID = UUID.randomUUID().toString();
        var cashPostingUUID = UUID.randomUUID().toString();

        preflightDelivery(entry, projectionRef, transaction, portfolio, account);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(portfolioProjectionUUID, projectionRef.getRole(),
                        LedgerProjectionRole.PORTFOLIO);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> convertDeliveryToBuySell(editedEntry,
                        portfolioProjectionUUID, account, accountProjectionUUID, cashPostingUUID));
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, roleChange);

        var portfolioTransaction = find(portfolio, portfolioProjectionUUID);
        var accountTransaction = find(account, accountProjectionUUID);

        return BuySellEntry.readOnly(portfolio, portfolioTransaction, account, accountTransaction);
    }

    private void preflightBuySell(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_019.message("Only ledger-backed buy/sell entries can be converted")); //$NON-NLS-1$

        if (projectionRef.getRole() != LedgerProjectionRole.PORTFOLIO)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_020.message("Only the portfolio side of a buy/sell entry can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_021.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        requireOnePosting(entry, LedgerPostingType.CASH);
        requireOnePosting(entry, LedgerPostingType.SECURITY);
        requireOneProjection(entry, LedgerProjectionRole.ACCOUNT);
        requireOneProjection(entry, LedgerProjectionRole.PORTFOLIO);
    }

    private void preflightDelivery(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio, Account account)
    {
        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_022.message("Only ledger-backed delivery entries can be converted")); //$NON-NLS-1$

        var expectedRole = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;

        if (projectionRef.getRole() != expectedRole)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_023.message("Only the delivery projection can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_024.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        if (entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.CASH))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_022.message("Ledger delivery entry must not already have a cash posting")); //$NON-NLS-1$

        if (entry.getProjectionRefs().stream().anyMatch(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT))
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

    private void convert(LedgerEntry entry, String projectionUUID)
    {
        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_INBOUND
                        : LedgerEntryType.DELIVERY_OUTBOUND;
        var targetRole = targetType == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;

        List.copyOf(entry.getPostings()).stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.CASH) //
                        .forEach(entry::removePosting);

        List.copyOf(entry.getProjectionRefs()).stream() //
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT) //
                        .forEach(entry::removeProjectionRef);

        var portfolioProjection = entry.getProjectionRefs().stream() //
                        .filter(projection -> projectionUUID.equals(projection.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Ledger portfolio projection not found: " + projectionUUID)); //$NON-NLS-1$

        entry.setType(targetType);
        portfolioProjection.setRole(targetRole);
    }

    private void convertDeliveryToBuySell(LedgerEntry entry, String portfolioProjectionUUID, Account account,
                    String accountProjectionUUID, String cashPostingUUID)
    {
        var targetType = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerEntryType.BUY
                        : LedgerEntryType.SELL;
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var cashPosting = new LedgerPosting(cashPostingUUID);
        var accountProjection = new LedgerProjectionRef(accountProjectionUUID);
        var portfolioProjection = entry.getProjectionRefs().stream() //
                        .filter(projection -> portfolioProjectionUUID.equals(projection.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Ledger delivery projection not found: " + portfolioProjectionUUID)); //$NON-NLS-1$
        var unitPostings = entry.getPostings().stream() //
                        .filter(posting -> posting != securityPosting) //
                        .toList();

        cashPosting.setType(LedgerPostingType.CASH);
        cashPosting.setAccount(account);
        applyDeliveryCashPosting(securityPosting, cashPosting, account);

        accountProjection.setRole(LedgerProjectionRole.ACCOUNT);
        accountProjection.setAccount(account);
        accountProjection.setPrimaryPosting(cashPosting);

        portfolioProjection.setRole(LedgerProjectionRole.PORTFOLIO);

        List.copyOf(entry.getPostings()).forEach(entry::removePosting);
        entry.addPosting(cashPosting);
        entry.addPosting(securityPosting);
        unitPostings.forEach(entry::addPosting);

        List.copyOf(entry.getProjectionRefs()).forEach(entry::removeProjectionRef);
        entry.addProjectionRef(accountProjection);
        entry.addProjectionRef(portfolioProjection);

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
        var count = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).count();

        if (count != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_026
                            .message("Ledger buy/sell entry must have exactly one " + role + " projection")); //$NON-NLS-1$ //$NON-NLS-2$
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
