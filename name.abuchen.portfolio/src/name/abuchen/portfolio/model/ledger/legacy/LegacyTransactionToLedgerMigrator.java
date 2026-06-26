package name.abuchen.portfolio.model.ledger.legacy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerDiagnosticMessageFormatter;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;

/**
 * Migrates legacy transaction structures into persisted Ledger entries.
 * This is compatibility loading infrastructure. It is not a general transaction editing API
 * and should not reconstruct business facts by guessing.
 */
public final class LegacyTransactionToLedgerMigrator
{
    public MigrationResult migrate(Client client)
    {
        Objects.requireNonNull(client);

        var plan = new MigrationPlan(client);
        var processedCrossEntries = new HashSet<CrossEntry>();

        migrateAccounts(client, plan, processedCrossEntries);
        migratePortfolios(client, plan, processedCrossEntries);

        if (validatePlan(client, plan) && plan.hasChanges())
        {
            applyPlan(client, plan);
            plan.markApplied();
        }

        return new MigrationResult(plan.getMigratedTransactionCount(), plan.getDiagnostics());
    }

    private void migrateAccounts(Client client, MigrationPlan plan, Set<CrossEntry> processedCrossEntries)
    {
        for (var account : client.getAccounts())
        {
            for (var transaction : List.copyOf(account.getTransactions()))
            {
                if (transaction instanceof LedgerBackedTransaction)
                    continue;

                if (transaction.getCrossEntry() != null)
                {
                    migrateAccountCrossEntry(transaction, plan, processedCrossEntries);
                }
                else
                {
                    migrateAccountOnly(account, transaction, plan);
                }
            }
        }
    }

    private void migratePortfolios(Client client, MigrationPlan plan, Set<CrossEntry> processedCrossEntries)
    {
        for (var portfolio : client.getPortfolios())
        {
            for (var transaction : List.copyOf(portfolio.getTransactions()))
            {
                if (transaction instanceof LedgerBackedTransaction)
                    continue;

                if (transaction.getCrossEntry() != null)
                {
                    migratePortfolioCrossEntry(transaction, plan, processedCrossEntries);
                }
                else
                {
                    migrateDelivery(portfolio, transaction, plan);
                }
            }
        }
    }

    private void migrateAccountOnly(Account account, AccountTransaction transaction, MigrationPlan plan)
    {
        var entryType = accountEntryType(transaction.getType());

        if (entryType == null)
        {
            plan.addDiagnostic("ACCOUNT", "UNSUPPORTED_TYPE", "owner=account type=" + transaction.getType(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            transaction);
            return;
        }

        var postingType = switch (entryType)
        {
            case FEES, FEES_REFUND -> LedgerPostingType.FEE;
            case TAXES, TAX_REFUND -> LedgerPostingType.TAX;
            default -> LedgerPostingType.CASH;
        };
        var entry = MigrationGraphBuilder.entry(transaction, entryType);
        var posting = MigrationGraphBuilder.cashPosting(postingType, account, transaction);

        if (transaction.getType() == AccountTransaction.Type.DIVIDENDS && transaction.getExDate() != null)
            MigrationGraphBuilder.addParameter(posting,
                            LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                                            transaction.getExDate()));

        MigrationGraphBuilder.addPosting(entry, posting);
        var unitPostings = MigrationGraphBuilder.addUnitPostings(entry, transaction);
        var projection = MigrationGraphBuilder.accountProjection(transaction.getUUID(), LedgerProjectionRole.ACCOUNT,
                        account, posting.getUUID());
        MigrationGraphBuilder.addUnitMemberships(projection, unitPostings);
        MigrationGraphBuilder.addProjectionRef(entry, projection);

        if (plan.handleAlreadyMigratedCompleteGroup("ACCOUNT", entry, transaction)) //$NON-NLS-1$
            return;

        plan.addEntry(entry, transaction);
    }

    private LedgerEntryType accountEntryType(AccountTransaction.Type type)
    {
        return switch (type)
        {
            case DEPOSIT -> LedgerEntryType.DEPOSIT;
            case REMOVAL -> LedgerEntryType.REMOVAL;
            case INTEREST -> LedgerEntryType.INTEREST;
            case INTEREST_CHARGE -> LedgerEntryType.INTEREST_CHARGE;
            case FEES -> LedgerEntryType.FEES;
            case FEES_REFUND -> LedgerEntryType.FEES_REFUND;
            case TAXES -> LedgerEntryType.TAXES;
            case TAX_REFUND -> LedgerEntryType.TAX_REFUND;
            case DIVIDENDS -> LedgerEntryType.DIVIDENDS;
            default -> null;
        };
    }

    private void migrateAccountCrossEntry(AccountTransaction transaction, MigrationPlan plan,
                    Set<CrossEntry> processedCrossEntries)
    {
        var crossEntry = transaction.getCrossEntry();

        if (!processedCrossEntries.add(crossEntry))
            return;

        if (crossEntry instanceof BuySellEntry buySellEntry)
            migrateBuySell(buySellEntry, plan);
        else if (crossEntry instanceof AccountTransferEntry transferEntry)
            migrateAccountTransfer(transferEntry, plan);
        else
            plan.addDiagnostic("ACCOUNT", "UNSUPPORTED_CROSS_ENTRY", //$NON-NLS-1$ //$NON-NLS-2$
                            "owner=account crossEntry=" + crossEntry.getClass().getName(), transaction); //$NON-NLS-1$
    }

    private void migratePortfolioCrossEntry(PortfolioTransaction transaction, MigrationPlan plan,
                    Set<CrossEntry> processedCrossEntries)
    {
        var crossEntry = transaction.getCrossEntry();

        if (!processedCrossEntries.add(crossEntry))
            return;

        if (crossEntry instanceof BuySellEntry buySellEntry)
            migrateBuySell(buySellEntry, plan);
        else if (crossEntry instanceof PortfolioTransferEntry transferEntry)
            migratePortfolioTransfer(transferEntry, plan);
        else
            plan.addDiagnostic("PORTFOLIO", "UNSUPPORTED_CROSS_ENTRY", //$NON-NLS-1$ //$NON-NLS-2$
                            "owner=portfolio crossEntry=" + crossEntry.getClass().getName(), transaction); //$NON-NLS-1$
    }

    private void migrateBuySell(BuySellEntry buySellEntry, MigrationPlan plan)
    {
        var accountTransaction = buySellEntry.getAccountTransaction();
        var portfolioTransaction = buySellEntry.getPortfolioTransaction();
        var account = buySellEntry.getAccount();
        var portfolio = buySellEntry.getPortfolio();

        if (!isValidBuySellGroup(buySellEntry, account, accountTransaction, portfolio, portfolioTransaction, plan))
            return;

        var entryType = portfolioTransaction.getType() == PortfolioTransaction.Type.BUY ? LedgerEntryType.BUY
                        : LedgerEntryType.SELL;
        var entry = MigrationGraphBuilder.entry(portfolioTransaction, entryType);
        var cashPosting = MigrationGraphBuilder.cashPosting(LedgerPostingType.CASH, account, accountTransaction);
        var securityPosting = MigrationGraphBuilder.securityPosting(portfolio, portfolioTransaction);

        MigrationGraphBuilder.addPosting(entry, cashPosting);
        MigrationGraphBuilder.addPosting(entry, securityPosting);
        var unitPostings = MigrationGraphBuilder.addUnitPostings(entry, portfolioTransaction);
        var accountProjection = MigrationGraphBuilder.accountProjection(accountTransaction.getUUID(),
                        LedgerProjectionRole.ACCOUNT, account, cashPosting.getUUID());
        var portfolioProjection = MigrationGraphBuilder.portfolioProjection(portfolioTransaction.getUUID(),
                        LedgerProjectionRole.PORTFOLIO, portfolio, securityPosting.getUUID());
        MigrationGraphBuilder.addUnitMemberships(accountProjection, unitPostings);
        MigrationGraphBuilder.addUnitMemberships(portfolioProjection, unitPostings);
        MigrationGraphBuilder.addProjectionRef(entry, accountProjection);
        MigrationGraphBuilder.addProjectionRef(entry, portfolioProjection);

        if (plan.handleAlreadyMigratedCompleteGroup("BUY_SELL", entry, accountTransaction, portfolioTransaction)) //$NON-NLS-1$
            return;

        plan.addEntry(entry, accountTransaction, portfolioTransaction);
    }

    private boolean isValidBuySellGroup(BuySellEntry entry, Account account, AccountTransaction accountTransaction,
                    Portfolio portfolio, PortfolioTransaction portfolioTransaction, MigrationPlan plan)
    {
        if (account == null || portfolio == null || accountTransaction == null || portfolioTransaction == null)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_001, "BUY_SELL", "MALFORMED_CROSS_ENTRY", //$NON-NLS-1$ //$NON-NLS-2$
                            "incomplete", accountTransaction, portfolioTransaction); //$NON-NLS-1$

        if (accountTransaction.getCrossEntry() != entry || portfolioTransaction.getCrossEntry() != entry)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_002, "BUY_SELL", "MALFORMED_CROSS_ENTRY", //$NON-NLS-1$ //$NON-NLS-2$
                            "backReference", accountTransaction, portfolioTransaction); //$NON-NLS-1$

        if (!account.getTransactions().contains(accountTransaction)
                        || !portfolio.getTransactions().contains(portfolioTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_003, "BUY_SELL", "MALFORMED_CROSS_ENTRY", //$NON-NLS-1$ //$NON-NLS-2$
                            "ownerMembership", accountTransaction, portfolioTransaction); //$NON-NLS-1$

        if (!isBuySellAccountType(accountTransaction.getType()) || !isBuySellPortfolioType(portfolioTransaction.getType())
                        || !sameBuySellDirection(accountTransaction, portfolioTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_004, "BUY_SELL", "TYPE_MISMATCH", //$NON-NLS-1$ //$NON-NLS-2$
                            "accountType=" + accountTransaction.getType() + " portfolioType=" //$NON-NLS-1$ //$NON-NLS-2$
                                            + portfolioTransaction.getType(),
                            accountTransaction, portfolioTransaction);

        if (!distinctUUIDs("BUY_SELL", accountTransaction, portfolioTransaction, plan)) //$NON-NLS-1$
            return false;

        if (!sameMetadata("BUY_SELL", accountTransaction, portfolioTransaction, plan)) //$NON-NLS-1$
            return false;

        if (hasUnits(accountTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_005, "BUY_SELL", "UNSUPPORTED_UNITS", //$NON-NLS-1$ //$NON-NLS-2$
                            "accountSideUnits", accountTransaction, portfolioTransaction); //$NON-NLS-1$

        return true;
    }

    private boolean isBuySellAccountType(AccountTransaction.Type type)
    {
        return type == AccountTransaction.Type.BUY || type == AccountTransaction.Type.SELL;
    }

    private boolean isBuySellPortfolioType(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
    }

    private boolean sameBuySellDirection(AccountTransaction accountTransaction,
                    PortfolioTransaction portfolioTransaction)
    {
        return accountTransaction.getType() == AccountTransaction.Type.BUY
                        && portfolioTransaction.getType() == PortfolioTransaction.Type.BUY
                        || accountTransaction.getType() == AccountTransaction.Type.SELL
                                        && portfolioTransaction.getType() == PortfolioTransaction.Type.SELL;
    }

    private void migrateAccountTransfer(AccountTransferEntry transferEntry, MigrationPlan plan)
    {
        var sourceTransaction = transferEntry.getSourceTransaction();
        var targetTransaction = transferEntry.getTargetTransaction();
        var sourceAccount = transferEntry.getSourceAccount();
        var targetAccount = transferEntry.getTargetAccount();

        if (!isValidAccountTransferGroup(transferEntry, sourceAccount, sourceTransaction, targetAccount,
                        targetTransaction, plan))
            return;

        var entry = MigrationGraphBuilder.entry(sourceTransaction, LedgerEntryType.CASH_TRANSFER);
        var sourcePosting = MigrationGraphBuilder.cashPosting(LedgerPostingType.CASH, sourceAccount,
                        sourceTransaction);
        var targetPosting = MigrationGraphBuilder.cashPosting(LedgerPostingType.CASH, targetAccount,
                        targetTransaction);

        MigrationGraphBuilder.addPosting(entry, sourcePosting);
        MigrationGraphBuilder.addPosting(entry, targetPosting);
        MigrationGraphBuilder.addProjectionRef(entry, MigrationGraphBuilder.accountProjection(
                        sourceTransaction.getUUID(), LedgerProjectionRole.SOURCE_ACCOUNT, sourceAccount,
                        sourcePosting.getUUID()));
        MigrationGraphBuilder.addProjectionRef(entry, MigrationGraphBuilder.accountProjection(
                        targetTransaction.getUUID(), LedgerProjectionRole.TARGET_ACCOUNT, targetAccount,
                        targetPosting.getUUID()));

        if (plan.handleAlreadyMigratedCompleteGroup("ACCOUNT_TRANSFER", entry, sourceTransaction, targetTransaction)) //$NON-NLS-1$
            return;

        plan.addEntry(entry, sourceTransaction, targetTransaction);
    }

    private boolean isValidAccountTransferGroup(AccountTransferEntry entry, Account sourceAccount,
                    AccountTransaction sourceTransaction, Account targetAccount, AccountTransaction targetTransaction,
                    MigrationPlan plan)
    {
        if (sourceAccount == null || targetAccount == null || sourceTransaction == null || targetTransaction == null)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_006, "ACCOUNT_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "incomplete", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (sourceTransaction.getCrossEntry() != entry || targetTransaction.getCrossEntry() != entry)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_007, "ACCOUNT_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "backReference", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (!sourceAccount.getTransactions().contains(sourceTransaction)
                        || !targetAccount.getTransactions().contains(targetTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_008, "ACCOUNT_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "ownerMembership", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (sourceTransaction.getType() != AccountTransaction.Type.TRANSFER_OUT
                        || targetTransaction.getType() != AccountTransaction.Type.TRANSFER_IN)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_009, "ACCOUNT_TRANSFER", "TYPE_MISMATCH", //$NON-NLS-1$ //$NON-NLS-2$
                            "direction", sourceTransaction, targetTransaction); //$NON-NLS-1$

        if (!distinctUUIDs("ACCOUNT_TRANSFER", sourceTransaction, targetTransaction, plan)) //$NON-NLS-1$
            return false;

        if (!sameMetadata("ACCOUNT_TRANSFER", sourceTransaction, targetTransaction, plan)) //$NON-NLS-1$
            return false;

        if (hasUnits(sourceTransaction) || hasUnits(targetTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_010, "ACCOUNT_TRANSFER", //$NON-NLS-1$
                            "UNSUPPORTED_UNITS", "transferUnits", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        return true;
    }

    private void migratePortfolioTransfer(PortfolioTransferEntry transferEntry, MigrationPlan plan)
    {
        var sourceTransaction = transferEntry.getSourceTransaction();
        var targetTransaction = transferEntry.getTargetTransaction();
        var sourcePortfolio = transferEntry.getSourcePortfolio();
        var targetPortfolio = transferEntry.getTargetPortfolio();

        if (!isValidPortfolioTransferGroup(transferEntry, sourcePortfolio, sourceTransaction, targetPortfolio,
                        targetTransaction, plan))
            return;

        var entry = MigrationGraphBuilder.entry(sourceTransaction, LedgerEntryType.SECURITY_TRANSFER);
        var sourcePosting = MigrationGraphBuilder.securityPosting(sourcePortfolio, sourceTransaction);
        var targetPosting = MigrationGraphBuilder.securityPosting(targetPortfolio, targetTransaction);

        MigrationGraphBuilder.addPosting(entry, sourcePosting);
        MigrationGraphBuilder.addPosting(entry, targetPosting);
        MigrationGraphBuilder.addProjectionRef(entry, MigrationGraphBuilder.portfolioProjection(
                        sourceTransaction.getUUID(), LedgerProjectionRole.SOURCE_PORTFOLIO, sourcePortfolio,
                        sourcePosting.getUUID()));
        MigrationGraphBuilder.addProjectionRef(entry, MigrationGraphBuilder.portfolioProjection(
                        targetTransaction.getUUID(), LedgerProjectionRole.TARGET_PORTFOLIO, targetPortfolio,
                        targetPosting.getUUID()));

        if (plan.handleAlreadyMigratedCompleteGroup("PORTFOLIO_TRANSFER", entry, sourceTransaction, targetTransaction)) //$NON-NLS-1$
            return;

        plan.addEntry(entry, sourceTransaction, targetTransaction);
    }

    private boolean isValidPortfolioTransferGroup(PortfolioTransferEntry entry, Portfolio sourcePortfolio,
                    PortfolioTransaction sourceTransaction, Portfolio targetPortfolio,
                    PortfolioTransaction targetTransaction, MigrationPlan plan)
    {
        if (sourcePortfolio == null || targetPortfolio == null || sourceTransaction == null || targetTransaction == null)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_011, "PORTFOLIO_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "incomplete", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (sourceTransaction.getCrossEntry() != entry || targetTransaction.getCrossEntry() != entry)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_012, "PORTFOLIO_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "backReference", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (!sourcePortfolio.getTransactions().contains(sourceTransaction)
                        || !targetPortfolio.getTransactions().contains(targetTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_013, "PORTFOLIO_TRANSFER", //$NON-NLS-1$
                            "MALFORMED_CROSS_ENTRY", "ownerMembership", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (sourceTransaction.getType() != PortfolioTransaction.Type.TRANSFER_OUT
                        || targetTransaction.getType() != PortfolioTransaction.Type.TRANSFER_IN)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_014, "PORTFOLIO_TRANSFER", //$NON-NLS-1$
                            "TYPE_MISMATCH", "direction", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        if (!distinctUUIDs("PORTFOLIO_TRANSFER", sourceTransaction, targetTransaction, plan)) //$NON-NLS-1$
            return false;

        if (!sameMetadata("PORTFOLIO_TRANSFER", sourceTransaction, targetTransaction, plan)) //$NON-NLS-1$
            return false;

        if (hasUnits(sourceTransaction) || hasUnits(targetTransaction))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_015, "PORTFOLIO_TRANSFER", //$NON-NLS-1$
                            "UNSUPPORTED_UNITS", "transferUnits", sourceTransaction, targetTransaction); //$NON-NLS-1$ //$NON-NLS-2$

        return true;
    }

    private boolean distinctUUIDs(String family, Transaction first, Transaction second, MigrationPlan plan)
    {
        if (first.getUUID() == null || second.getUUID() == null)
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_016, family, "MALFORMED_CROSS_ENTRY", //$NON-NLS-1$
                            "missingUUID", first, second); //$NON-NLS-1$

        if (first.getUUID().equals(second.getUUID()))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_017, family, "MALFORMED_CROSS_ENTRY", //$NON-NLS-1$
                            "duplicateUUID", first, second); //$NON-NLS-1$

        return true;
    }

    private boolean sameMetadata(String family, Transaction first, Transaction second, MigrationPlan plan)
    {
        if (!Objects.equals(first.getDateTime(), second.getDateTime()))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_018, family, "METADATA_MISMATCH", //$NON-NLS-1$
                            "field=dateTime", first, second); //$NON-NLS-1$

        if (!Objects.equals(first.getNote(), second.getNote()))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_019, family, "METADATA_MISMATCH", //$NON-NLS-1$
                            "field=note", first, second); //$NON-NLS-1$

        if (!Objects.equals(first.getSource(), second.getSource()))
            return malformed(plan, LedgerDiagnosticCode.LEDGER_IMPORT_020, family, "METADATA_MISMATCH", //$NON-NLS-1$
                            "field=source", first, second); //$NON-NLS-1$

        return true;
    }

    private boolean malformed(MigrationPlan plan, LedgerDiagnosticCode code, String family, String reason, String context,
                    Transaction... transactions)
    {
        plan.addDiagnostic(code, family, reason, context, transactions);
        return false;
    }

    private void migrateDelivery(Portfolio portfolio, PortfolioTransaction transaction, MigrationPlan plan)
    {
        var entryType = switch (transaction.getType())
        {
            case DELIVERY_INBOUND -> LedgerEntryType.DELIVERY_INBOUND;
            case DELIVERY_OUTBOUND -> LedgerEntryType.DELIVERY_OUTBOUND;
            default -> null;
        };

        if (entryType == null)
        {
            plan.addDiagnostic("DELIVERY", "UNSUPPORTED_TYPE", //$NON-NLS-1$ //$NON-NLS-2$
                            "owner=portfolio type=" + transaction.getType(), transaction); //$NON-NLS-1$
            return;
        }

        var role = entryType == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;
        var entry = MigrationGraphBuilder.entry(transaction, entryType);
        var posting = MigrationGraphBuilder.securityPosting(portfolio, transaction);

        MigrationGraphBuilder.addPosting(entry, posting);
        var unitPostings = MigrationGraphBuilder.addUnitPostings(entry, transaction);
        var projection = MigrationGraphBuilder.portfolioProjection(transaction.getUUID(), role, portfolio,
                        posting.getUUID());
        MigrationGraphBuilder.addUnitMemberships(projection, unitPostings);
        MigrationGraphBuilder.addProjectionRef(entry, projection);

        if (plan.handleAlreadyMigratedCompleteGroup("DELIVERY", entry, transaction)) //$NON-NLS-1$
            return;

        plan.addEntry(entry, transaction);
    }

    private boolean hasUnits(Transaction transaction)
    {
        return transaction.getUnits().findAny().isPresent();
    }

    private boolean validatePlan(Client client, MigrationPlan plan)
    {
        if (plan.getEntries().isEmpty())
            return true;

        var candidate = new Ledger();

        client.getLedger().getEntries().forEach(entry -> MigrationGraphBuilder.addEntry(candidate, entry));
        plan.getEntries().forEach(entry -> MigrationGraphBuilder.addEntry(candidate, entry));

        var result = LedgerStructuralValidator.validate(candidate);

        if (result.isOK())
            return true;

        plan.addDiagnostic("MIGRATION", "FAILED_VALIDATION", "issues=" + result.format()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return false;
    }

    private void applyPlan(Client client, MigrationPlan plan)
    {
        plan.getEntries().forEach(entry -> MigrationGraphBuilder.addEntry(client.getLedger(), entry));
        convertInvestmentPlanTransactionsToLedgerRefs(client, plan);
        removeMigratedLegacyTransactions(client, plan.getProjectionUUIDsToRemove(), plan.getLegacyTransactionsToRemove());
        LedgerProjectionService.materialize(client);
    }

    private void convertInvestmentPlanTransactionsToLedgerRefs(Client client, MigrationPlan plan)
    {
        for (var investmentPlan : client.getPlans())
        {
            for (var index = investmentPlan.getTransactions().size() - 1; index >= 0; index--)
            {
                var transaction = investmentPlan.getTransactions().get(index);

                if (!shouldRemove(transaction, plan.getProjectionUUIDsToRemove(), plan.getLegacyTransactionsToRemove()))
                    continue;

                var ref = ledgerExecutionRef(client, transaction.getUUID());

                if (ref == null)
                    continue;

                if (investmentPlan.getLedgerExecutionRefs().stream()
                                .noneMatch(existing -> sameExecutionRef(existing, ref)))
                    investmentPlan.addLedgerExecutionRef(ref);

                investmentPlan.getTransactions().remove(index);
            }
        }
    }

    private InvestmentPlan.LedgerExecutionRef ledgerExecutionRef(Client client, String projectionUUID)
    {
        for (var entry : client.getLedger().getEntries())
        {
            for (var projection : entry.getProjectionRefs())
            {
                if (projectionUUID.equals(projection.getUUID()))
                    return new InvestmentPlan.LedgerExecutionRef(entry.getUUID(), projection.getUUID(),
                                    projection.getRole());
            }
        }

        return null;
    }

    private boolean sameExecutionRef(InvestmentPlan.LedgerExecutionRef left, InvestmentPlan.LedgerExecutionRef right)
    {
        return Objects.equals(left.getLedgerEntryUUID(), right.getLedgerEntryUUID())
                        && Objects.equals(left.getProjectionUUID(), right.getProjectionUUID())
                        && left.getProjectionRole() == right.getProjectionRole();
    }

    private void removeMigratedLegacyTransactions(Client client, Set<String> migratedProjectionUUIDs,
                    List<Transaction> migratedTransactions)
    {
        for (var account : client.getAccounts())
            account.getTransactions().removeIf(transaction -> shouldRemove(transaction, migratedProjectionUUIDs,
                            migratedTransactions));

        for (var portfolio : client.getPortfolios())
            portfolio.getTransactions().removeIf(transaction -> shouldRemove(transaction, migratedProjectionUUIDs,
                            migratedTransactions));
    }

    private boolean shouldRemove(Transaction transaction, Set<String> migratedProjectionUUIDs,
                    List<Transaction> migratedTransactions)
    {
        return !(transaction instanceof LedgerBackedTransaction) && (migratedTransactions.contains(transaction)
                        || migratedProjectionUUIDs.contains(transaction.getUUID()));
    }

    private static final class MigrationGraphBuilder
    {
        private MigrationGraphBuilder()
        {
        }

        private static LedgerEntry entry(Transaction transaction, LedgerEntryType type)
        {
            var entry = new LedgerEntry(migratedEntryUUID(type, transaction.getUUID()));

            entry.setType(type);
            entry.setDateTime(transaction.getDateTime());
            entry.setNote(transaction.getNote());
            entry.setSource(transaction.getSource());
            entry.setUpdatedAt(transaction.getUpdatedAt());

            return entry;
        }

        private static LedgerPosting cashPosting(LedgerPostingType type, Account account,
                        AccountTransaction transaction)
        {
            var posting = new LedgerPosting();

            posting.setUUID(migratedPostingUUID(transaction.getUUID(), type, "primary")); //$NON-NLS-1$
            posting.setType(type);
            posting.setAccount(account);
            posting.setAmount(transaction.getAmount());
            posting.setCurrency(transaction.getCurrencyCode());
            posting.setSecurity(transaction.getSecurity());
            posting.setShares(transaction.getShares());

            return posting;
        }

        private static LedgerPosting securityPosting(Portfolio portfolio, PortfolioTransaction transaction)
        {
            var posting = new LedgerPosting();

            posting.setUUID(migratedPostingUUID(transaction.getUUID(), LedgerPostingType.SECURITY, "primary")); //$NON-NLS-1$
            posting.setType(LedgerPostingType.SECURITY);
            posting.setPortfolio(portfolio);
            posting.setAmount(transaction.getAmount());
            posting.setCurrency(transaction.getCurrencyCode());
            posting.setSecurity(transaction.getSecurity());
            posting.setShares(transaction.getShares());

            return posting;
        }

        private static List<LedgerPosting> addUnitPostings(LedgerEntry entry, Transaction transaction)
        {
            var units = transaction.getUnits().toList();
            var postings = new ArrayList<LedgerPosting>();

            for (var index = 0; index < units.size(); index++)
            {
                var posting = unitPosting(units.get(index));

                posting.setUUID(migratedPostingUUID(transaction.getUUID(), posting.getType(), "unit-" + index)); //$NON-NLS-1$
                addPosting(entry, posting);
                postings.add(posting);
            }

            return List.copyOf(postings);
        }

        private static void addUnitMemberships(LedgerProjectionRef projection, List<LedgerPosting> unitPostings)
        {
            for (var posting : unitPostings)
            {
                switch (posting.getType())
                {
                    case FEE -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.FEE_UNIT);
                    case TAX -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.TAX_UNIT);
                    case GROSS_VALUE -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.GROSS_VALUE_UNIT);
                    default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_IMPORT_021
                                    .message("Unsupported unit posting type: " + posting.getType())); //$NON-NLS-1$
                }
            }
        }

        private static LedgerProjectionRef accountProjection(String uuid, LedgerProjectionRole role, Account account,
                        String primaryPostingUUID)
        {
            var projection = new LedgerProjectionRef(uuid);

            projection.setRole(role);
            projection.setAccount(account);
            projection.setPrimaryPostingTargetUUID(primaryPostingUUID);

            return projection;
        }

        private static LedgerProjectionRef portfolioProjection(String uuid, LedgerProjectionRole role,
                        Portfolio portfolio, String primaryPostingUUID)
        {
            var projection = new LedgerProjectionRef(uuid);

            projection.setRole(role);
            projection.setPortfolio(portfolio);
            projection.setPrimaryPostingTargetUUID(primaryPostingUUID);

            return projection;
        }

        private static void addEntry(Ledger ledger, LedgerEntry entry)
        {
            ledger.addEntry(entry);
        }

        private static void setUpdatedAt(LedgerEntry entry, Transaction transaction)
        {
            entry.setUpdatedAt(transaction.getUpdatedAt());
        }

        private static void addPosting(LedgerEntry entry, LedgerPosting posting)
        {
            entry.addPosting(posting);
        }

        private static void addProjectionRef(LedgerEntry entry, LedgerProjectionRef projection)
        {
            entry.addProjectionRef(projection);
        }

        private static void addParameter(LedgerPosting posting, LedgerParameter<?> parameter)
        {
            posting.addParameter(parameter);
        }

        private static LedgerPosting unitPosting(Unit unit)
        {
            var posting = new LedgerPosting();
            var amount = unit.getAmount();

            posting.setType(switch (unit.getType())
            {
                case FEE -> LedgerPostingType.FEE;
                case TAX -> LedgerPostingType.TAX;
                case GROSS_VALUE -> LedgerPostingType.GROSS_VALUE;
            });
            posting.setAmount(amount.getAmount());
            posting.setCurrency(amount.getCurrencyCode());

            if (unit.getForex() != null)
            {
                posting.setForexAmount(unit.getForex().getAmount());
                posting.setForexCurrency(unit.getForex().getCurrencyCode());
                posting.setExchangeRate(unit.getExchangeRate());
            }

            return posting;
        }

        private static String migratedEntryUUID(LedgerEntryType type, String primaryProjectionUUID)
        {
            var key = "ledger-v6:migrated-entry:" + type + ":" + primaryProjectionUUID; //$NON-NLS-1$ //$NON-NLS-2$
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        }

        private static String migratedPostingUUID(String projectionUUID, LedgerPostingType type, String discriminator)
        {
            var key = "ledger-v6:migrated-posting:" + projectionUUID + ":" + type + ":" + discriminator; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private static final class MigrationPlan
    {
        private final Client client;
        private final List<LedgerEntry> entries = new ArrayList<>();
        private final List<Transaction> legacyTransactionsToRemove = new ArrayList<>();
        private final Set<String> projectionUUIDsToRemove = new HashSet<>();
        private final Set<String> plannedProjectionUUIDs = new HashSet<>();
        private final List<String> diagnostics = new ArrayList<>();
        private int migratedTransactionCount;
        private boolean applied;

        private MigrationPlan(Client client)
        {
            this.client = client;
        }

        private void addEntry(LedgerEntry entry, Transaction... transactions)
        {
            entries.add(entry);

            for (var transaction : transactions)
                markMigrated(transaction);

            for (var projection : entry.getProjectionRefs())
                plannedProjectionUUIDs.add(projection.getUUID());

            MigrationGraphBuilder.setUpdatedAt(entry, transactions[0]);
        }

        private boolean handleAlreadyMigratedCompleteGroup(String family, LedgerEntry expectedEntry,
                        Transaction... transactions)
        {
            var projectionUUIDs = projectionUUIDs(expectedEntry);

            if (projectionUUIDs.stream().anyMatch(plannedProjectionUUIDs::contains))
            {
                addDiagnostic(family, "DUPLICATE_CONFLICT", "plannedProjectionConflict", transactions); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }

            var matchingEntries = existingEntriesContainingAny(projectionUUIDs);

            if (matchingEntries.isEmpty())
                return false;

            if (matchingEntries.size() == 1 && existingEntryContainsAll(matchingEntries.get(0), projectionUUIDs)
                            && isStructurallyValidExistingEntry(matchingEntries.get(0)))
            {
                var mismatch = semanticMismatch(matchingEntries.get(0), expectedEntry);

                if (mismatch == SemanticMismatch.NONE)
                {
                    for (var transaction : transactions)
                        markAlreadyMigrated(transaction);

                    addDiagnostic(family, "SKIPPED_ALREADY_MIGRATED", "completeExistingLedgerEntry", transactions); //$NON-NLS-1$ //$NON-NLS-2$
                    return true;
                }

                addDiagnostic(family, "DUPLICATE_CONFLICT", "existingProjectionConflict mismatch=" + mismatch, //$NON-NLS-1$ //$NON-NLS-2$
                                transactions);
                return true;
            }

            addDiagnostic(family, "DUPLICATE_CONFLICT", //$NON-NLS-1$
                            "existingProjectionConflict mismatch=" + duplicateMismatch(matchingEntries, //$NON-NLS-1$
                                            projectionUUIDs),
                            transactions);
            return true;
        }

        private void markMigrated(Transaction transaction)
        {
            legacyTransactionsToRemove.add(transaction);
            projectionUUIDsToRemove.add(transaction.getUUID());
            migratedTransactionCount++;
        }

        private void markAlreadyMigrated(Transaction transaction)
        {
            legacyTransactionsToRemove.add(transaction);
            projectionUUIDsToRemove.add(transaction.getUUID());
        }

        private List<LedgerEntry> existingEntriesContainingAny(List<String> projectionUUIDs)
        {
            var result = new ArrayList<LedgerEntry>();

            for (var entry : client.getLedger().getEntries())
            {
                if (entry.getProjectionRefs().stream().anyMatch(ref -> projectionUUIDs.contains(ref.getUUID())))
                    result.add(entry);
            }

            return result;
        }

        private boolean existingEntryContainsAll(LedgerEntry entry, List<String> projectionUUIDs)
        {
            var existingProjectionUUIDs = new HashSet<String>();

            for (var projection : entry.getProjectionRefs())
                existingProjectionUUIDs.add(projection.getUUID());

            return existingProjectionUUIDs.containsAll(projectionUUIDs);
        }

        private boolean isStructurallyValidExistingEntry(LedgerEntry entry)
        {
            var ledger = new Ledger();

            MigrationGraphBuilder.addEntry(ledger, entry);

            return LedgerStructuralValidator.validate(ledger).isOK();
        }

        private SemanticMismatch duplicateMismatch(List<LedgerEntry> matchingEntries, List<String> projectionUUIDs)
        {
            if (matchingEntries.size() != 1)
                return SemanticMismatch.PROJECTION_UUID;

            if (!existingEntryContainsAll(matchingEntries.get(0), projectionUUIDs))
                return SemanticMismatch.PROJECTION_UUID;

            if (!isStructurallyValidExistingEntry(matchingEntries.get(0)))
                return SemanticMismatch.STRUCTURAL_VALIDATION;

            return SemanticMismatch.PROJECTION_UUID;
        }

        private SemanticMismatch semanticMismatch(LedgerEntry existingEntry, LedgerEntry expectedEntry)
        {
            if (existingEntry.getType() != expectedEntry.getType())
                return SemanticMismatch.ENTRY_TYPE;

            if (existingEntry.getProjectionRefs().size() != expectedEntry.getProjectionRefs().size())
                return SemanticMismatch.PROJECTION_UUID;

            if (!postingTypeCounts(existingEntry).equals(postingTypeCounts(expectedEntry)))
                return SemanticMismatch.POSTING_TYPE_SHAPE;

            if (!sameUnitPostingFacts(existingEntry, expectedEntry))
                return SemanticMismatch.UNIT_POSTINGS;

            for (var expectedProjection : expectedEntry.getProjectionRefs())
            {
                var existingProjection = projectionByUUID(existingEntry, expectedProjection.getUUID());

                if (existingProjection == null)
                    return SemanticMismatch.PROJECTION_UUID;

                if (existingProjection.getRole() != expectedProjection.getRole())
                    return SemanticMismatch.PROJECTION_ROLE;

                if (existingProjection.getAccount() != expectedProjection.getAccount()
                                || existingProjection.getPortfolio() != expectedProjection.getPortfolio())
                    return SemanticMismatch.PROJECTION_OWNER;

                var expectedPosting = postingByUUID(expectedEntry, expectedProjection.getPrimaryPostingUUID());
                var existingPosting = postingByUUID(existingEntry, existingProjection.getPrimaryPostingUUID());

                if (expectedPosting == null || existingPosting == null)
                    return SemanticMismatch.PRIMARY_POSTING;

                var postingMismatch = postingOwnerMismatch(existingPosting, expectedPosting, expectedEntry.getType());

                if (postingMismatch != SemanticMismatch.NONE)
                    return postingMismatch;
            }

            return SemanticMismatch.NONE;
        }

        private List<String> projectionUUIDs(LedgerEntry entry)
        {
            var result = new ArrayList<String>();

            for (var projection : entry.getProjectionRefs())
                result.add(projection.getUUID());

            return result;
        }

        private java.util.Map<LedgerPostingType, Integer> postingTypeCounts(LedgerEntry entry)
        {
            var result = new java.util.EnumMap<LedgerPostingType, Integer>(LedgerPostingType.class);

            for (var posting : entry.getPostings())
                result.merge(posting.getType(), 1, Integer::sum);

            return result;
        }

        private List<UnitPostingFact> unitPostingFacts(LedgerEntry entry)
        {
            var result = new ArrayList<UnitPostingFact>();

            for (var posting : entry.getPostings())
                if (isUnitPosting(posting.getType()))
                    result.add(new UnitPostingFact(posting.getType(), posting.getAmount(), posting.getCurrency(),
                                    posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                                    posting.getAccount(), posting.getPortfolio(), posting.getSecurity(),
                                    posting.getShares()));

            return result;
        }

        private boolean sameUnitPostingFacts(LedgerEntry existingEntry, LedgerEntry expectedEntry)
        {
            var unmatchedExpectedFacts = new ArrayList<>(unitPostingFacts(expectedEntry));

            for (var existingFact : unitPostingFacts(existingEntry))
                if (!unmatchedExpectedFacts.remove(existingFact))
                    return false;

            return unmatchedExpectedFacts.isEmpty();
        }

        private boolean isUnitPosting(LedgerPostingType type)
        {
            return type == LedgerPostingType.FEE || type == LedgerPostingType.TAX
                            || type == LedgerPostingType.GROSS_VALUE;
        }

        private LedgerProjectionRef projectionByUUID(LedgerEntry entry, String uuid)
        {
            for (var projection : entry.getProjectionRefs())
                if (Objects.equals(projection.getUUID(), uuid))
                    return projection;

            return null;
        }

        private LedgerPosting postingByUUID(LedgerEntry entry, String uuid)
        {
            for (var posting : entry.getPostings())
                if (Objects.equals(posting.getUUID(), uuid))
                    return posting;

            return null;
        }

        private SemanticMismatch postingOwnerMismatch(LedgerPosting existingPosting, LedgerPosting expectedPosting,
                        LedgerEntryType expectedEntryType)
        {
            if (existingPosting.getType() != expectedPosting.getType())
                return SemanticMismatch.POSTING_TYPE_SHAPE;

            if (existingPosting.getAccount() != expectedPosting.getAccount()
                            || existingPosting.getPortfolio() != expectedPosting.getPortfolio())
                return SemanticMismatch.POSTING_OWNER;

            if (expectedEntryType == LedgerEntryType.DIVIDENDS
                            && existingPosting.getSecurity() != expectedPosting.getSecurity())
                return SemanticMismatch.DIVIDEND_SECURITY;

            return SemanticMismatch.NONE;
        }

        private void addDiagnostic(String family, String reason, String context, Transaction... transactions)
        {
            diagnostics.add(LedgerDiagnosticMessageFormatter.formatMigrationDiagnostic(client,
                            "family=" + family + " reason=" + reason + " uuids=" + uuids(transactions) + " " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                            + context,
                            transactions));
        }

        private void addDiagnostic(LedgerDiagnosticCode code, String family, String reason, String context,
                        Transaction... transactions)
        {
            diagnostics.add(LedgerDiagnosticMessageFormatter.formatMigrationDiagnostic(client,
                            code.prefix() + " family=" + family + " reason=" + reason + " uuids=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                            + uuids(transactions) + " " + context, //$NON-NLS-1$
                            transactions));
        }

        private void addDiagnostic(String family, String reason, String context)
        {
            diagnostics.add("family=" + family + " reason=" + reason + " " + context); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        private String uuids(Transaction... transactions)
        {
            var result = new ArrayList<String>();

            for (var transaction : transactions)
                result.add(transaction != null ? transaction.getUUID() : "<null>"); //$NON-NLS-1$

            return result.toString();
        }

        private List<LedgerEntry> getEntries()
        {
            return entries;
        }

        private List<Transaction> getLegacyTransactionsToRemove()
        {
            return legacyTransactionsToRemove;
        }

        private Set<String> getProjectionUUIDsToRemove()
        {
            return projectionUUIDsToRemove;
        }

        private List<String> getDiagnostics()
        {
            return diagnostics;
        }

        private int getMigratedTransactionCount()
        {
            return applied ? migratedTransactionCount : 0;
        }

        private boolean hasChanges()
        {
            return !entries.isEmpty() || !legacyTransactionsToRemove.isEmpty();
        }

        private void markApplied()
        {
            applied = true;
        }

        private enum SemanticMismatch
        {
            NONE,
            ENTRY_TYPE,
            PROJECTION_UUID,
            PROJECTION_ROLE,
            PROJECTION_OWNER,
            PRIMARY_POSTING,
            POSTING_OWNER,
            POSTING_TYPE_SHAPE,
            UNIT_POSTINGS,
            DIVIDEND_SECURITY,
            STRUCTURAL_VALIDATION
        }

        private record UnitPostingFact(LedgerPostingType type, long amount, String currency, Long forexAmount,
                        String forexCurrency, BigDecimal exchangeRate, Account account, Portfolio portfolio,
                        Security security, long shares)
        {}
    }

    public static final class MigrationResult
    {
        private final int migratedTransactionCount;
        private final List<String> diagnostics;

        private MigrationResult(int migratedTransactionCount, List<String> diagnostics)
        {
            this.migratedTransactionCount = migratedTransactionCount;
            this.diagnostics = List.copyOf(diagnostics);
        }

        public int getMigratedTransactionCount()
        {
            return migratedTransactionCount;
        }

        public List<String> getDiagnostics()
        {
            return diagnostics;
        }

        public boolean hasDiagnostics()
        {
            return !diagnostics.isEmpty();
        }
    }
}
