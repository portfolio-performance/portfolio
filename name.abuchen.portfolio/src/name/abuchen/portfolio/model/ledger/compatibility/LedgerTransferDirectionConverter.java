package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.ExchangeRate;

/**
 * Reverses ledger-backed account and portfolio transfer directions.
 * This class is part of the Ledger compatibility layer for existing UI and action code. It
 * keeps both transfer sides derived from one consistent Ledger entry.
 */
public final class LedgerTransferDirectionConverter
{
    private final Client client;

    public LedgerTransferDirectionConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public AccountTransferEntry reverse(AccountTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        if (!(transfer.getSourceTransaction() instanceof LedgerBackedAccountTransaction sourceTransaction)
                        || !(transfer.getTargetTransaction() instanceof LedgerBackedAccountTransaction targetTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_063.message("Only ledger-backed account transfers can be reversed")); //$NON-NLS-1$

        var entry = sourceTransaction.getLedgerEntry();
        var sourceProjectionUUID = sourceTransaction.getLedgerProjectionRef().getUUID();
        var targetProjectionUUID = targetTransaction.getLedgerProjectionRef().getUUID();
        var sourceAccount = sourceTransaction.getLedgerProjectionRef().getAccount();
        var targetAccount = targetTransaction.getLedgerProjectionRef().getAccount();

        preflightAccountTransfer(entry, sourceTransaction, targetTransaction);
        var sourceRoleChange = LedgerInvestmentPlanRefSupport.roleChange(sourceProjectionUUID,
                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT);
        var targetRoleChange = LedgerInvestmentPlanRefSupport.roleChange(targetProjectionUUID,
                        LedgerProjectionRole.TARGET_ACCOUNT, LedgerProjectionRole.SOURCE_ACCOUNT);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, sourceRoleChange, targetRoleChange);

        new LedgerMutationContext(client).mutateEntry(entry, this::reverseAccountTransfer);
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, sourceRoleChange, targetRoleChange);

        return AccountTransferEntry.readOnly(targetAccount, find(targetAccount, targetProjectionUUID), sourceAccount,
                        find(sourceAccount, sourceProjectionUUID));
    }

    public PortfolioTransferEntry reverse(PortfolioTransferEntry transfer)
    {
        Objects.requireNonNull(transfer);

        if (!(transfer.getSourceTransaction() instanceof LedgerBackedPortfolioTransaction sourceTransaction)
                        || !(transfer.getTargetTransaction() instanceof LedgerBackedPortfolioTransaction targetTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_064.message("Only ledger-backed portfolio transfers can be reversed")); //$NON-NLS-1$

        var entry = sourceTransaction.getLedgerEntry();
        var sourceProjectionUUID = sourceTransaction.getLedgerProjectionRef().getUUID();
        var targetProjectionUUID = targetTransaction.getLedgerProjectionRef().getUUID();
        var sourcePortfolio = sourceTransaction.getLedgerProjectionRef().getPortfolio();
        var targetPortfolio = targetTransaction.getLedgerProjectionRef().getPortfolio();

        preflightPortfolioTransfer(entry, sourceTransaction, targetTransaction);
        var sourceRoleChange = LedgerInvestmentPlanRefSupport.roleChange(sourceProjectionUUID,
                        LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO);
        var targetRoleChange = LedgerInvestmentPlanRefSupport.roleChange(targetProjectionUUID,
                        LedgerProjectionRole.TARGET_PORTFOLIO, LedgerProjectionRole.SOURCE_PORTFOLIO);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, sourceRoleChange, targetRoleChange);

        new LedgerMutationContext(client).mutateEntry(entry, this::reversePortfolioTransfer);
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, sourceRoleChange, targetRoleChange);

        return PortfolioTransferEntry.readOnly(targetPortfolio, find(targetPortfolio, targetProjectionUUID),
                        sourcePortfolio, find(sourcePortfolio, sourceProjectionUUID));
    }

    private void preflightAccountTransfer(LedgerEntry entry, LedgerBackedAccountTransaction sourceTransaction,
                    LedgerBackedAccountTransaction targetTransaction)
    {
        if (entry.getType() != LedgerEntryType.CASH_TRANSFER)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_065.message("Only ledger-backed account transfer entries can be reversed")); //$NON-NLS-1$

        if (sourceTransaction.getLedgerEntry() != targetTransaction.getLedgerEntry())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_059.message("Transfer projections do not belong to the same ledger entry")); //$NON-NLS-1$

        if (sourceTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.SOURCE_ACCOUNT
                        || targetTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.TARGET_ACCOUNT)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_060.message("Account transfer projections are not in source/target order")); //$NON-NLS-1$

        var sourceProjection = uniqueProjection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = uniqueProjection(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePosting = LedgerProjectionSupport.primaryPosting(entry, sourceProjection);
        var targetPosting = LedgerProjectionSupport.primaryPosting(entry, targetProjection);

        if (sourcePosting == targetPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_066.message("Account transfer source and target postings are ambiguous")); //$NON-NLS-1$

        if (sourcePosting.getAccount() != sourceProjection.getAccount()
                        || targetPosting.getAccount() != targetProjection.getAccount())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_061.message("Account transfer projection and posting owners do not match")); //$NON-NLS-1$
    }

    private void preflightPortfolioTransfer(LedgerEntry entry, LedgerBackedPortfolioTransaction sourceTransaction,
                    LedgerBackedPortfolioTransaction targetTransaction)
    {
        if (entry.getType() != LedgerEntryType.SECURITY_TRANSFER)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_067.message("Only ledger-backed portfolio transfer entries can be reversed")); //$NON-NLS-1$

        if (sourceTransaction.getLedgerEntry() != targetTransaction.getLedgerEntry())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_062.message("Transfer projections do not belong to the same ledger entry")); //$NON-NLS-1$

        if (sourceTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.SOURCE_PORTFOLIO
                        || targetTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.TARGET_PORTFOLIO)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_063.message("Portfolio transfer projections are not in source/target order")); //$NON-NLS-1$

        var sourceProjection = uniqueProjection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = uniqueProjection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);
        var sourcePosting = LedgerProjectionSupport.primaryPosting(entry, sourceProjection);
        var targetPosting = LedgerProjectionSupport.primaryPosting(entry, targetProjection);

        if (sourcePosting == targetPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_068.message("Portfolio transfer source and target postings are ambiguous")); //$NON-NLS-1$

        if (sourcePosting.getPortfolio() != sourceProjection.getPortfolio()
                        || targetPosting.getPortfolio() != targetProjection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_064.message("Portfolio transfer projection and posting owners do not match")); //$NON-NLS-1$
    }

    private void reverseAccountTransfer(LedgerEntry entry)
    {
        var sourceProjection = uniqueProjection(entry, LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetProjection = uniqueProjection(entry, LedgerProjectionRole.TARGET_ACCOUNT);
        var sourcePosting = LedgerProjectionSupport.primaryPosting(entry, sourceProjection);
        var targetPosting = LedgerProjectionSupport.primaryPosting(entry, targetProjection);

        reverseAccountTransferForex(sourcePosting, targetPosting);

        sourceProjection.setRole(LedgerProjectionRole.TARGET_ACCOUNT);
        targetProjection.setRole(LedgerProjectionRole.SOURCE_ACCOUNT);
    }

    private void reverseAccountTransferForex(LedgerPosting oldSourcePosting, LedgerPosting oldTargetPosting)
    {
        if (oldSourcePosting.getCurrency() == null || oldTargetPosting.getCurrency() == null
                        || oldSourcePosting.getCurrency().equals(oldTargetPosting.getCurrency()))
            return;

        if (hasForexFor(oldTargetPosting, oldSourcePosting))
        {
            clearForex(oldSourcePosting);
            return;
        }

        if (!hasForexFor(oldSourcePosting, oldTargetPosting))
            return;

        var reversedExchangeRate = inverse(oldSourcePosting.getExchangeRate());

        oldTargetPosting.setForexAmount(oldSourcePosting.getAmount());
        oldTargetPosting.setForexCurrency(oldSourcePosting.getCurrency());
        oldTargetPosting.setExchangeRate(reversedExchangeRate);
        clearForex(oldSourcePosting);
    }

    private boolean hasForexFor(LedgerPosting posting, LedgerPosting oppositePosting)
    {
        return posting.getForexAmount() != null && posting.getForexCurrency() != null
                        && posting.getExchangeRate() != null
                        && posting.getForexCurrency().equals(oppositePosting.getCurrency());
    }

    private BigDecimal inverse(BigDecimal exchangeRate)
    {
        if (exchangeRate.signum() <= 0)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_FOREX_001
                            .message("Account transfer exchange rate must be positive")); //$NON-NLS-1$

        return ExchangeRate.inverse(exchangeRate);
    }

    private void clearForex(LedgerPosting posting)
    {
        posting.setForexAmount(null);
        posting.setForexCurrency(null);
        posting.setExchangeRate(null);
    }

    private void reversePortfolioTransfer(LedgerEntry entry)
    {
        var sourceProjection = uniqueProjection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetProjection = uniqueProjection(entry, LedgerProjectionRole.TARGET_PORTFOLIO);

        sourceProjection.setRole(LedgerProjectionRole.TARGET_PORTFOLIO);
        targetProjection.setRole(LedgerProjectionRole.SOURCE_PORTFOLIO);
    }

    private LedgerProjectionRef uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_065
                            .message("Expected one projection for role " + role + " but found " //$NON-NLS-1$ //$NON-NLS-2$
                                            + projections.size()));

        return projections.get(0);
    }

    private AccountTransaction find(Account account, String projectionUUID)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger account transfer projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger portfolio transfer projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
