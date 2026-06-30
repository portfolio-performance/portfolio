package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

/**
 * Reverses ledger-backed buy and sell transactions.
 * This class is part of the Ledger compatibility layer for existing UI and action code. It
 * rewrites the Ledger entry while preserving consistent runtime projections.
 */
public final class LedgerBuySellReversalConverter
{
    private final Client client;

    public LedgerBuySellReversalConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public BuySellEntry reverse(BuySellEntry entry)
    {
        Objects.requireNonNull(entry);

        if (!(entry.getAccountTransaction() instanceof LedgerBackedAccountTransaction accountTransaction)
                        || !(entry.getPortfolioTransaction() instanceof LedgerBackedPortfolioTransaction portfolioTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_027.message("Only ledger-backed buy/sell transactions can be reversed")); //$NON-NLS-1$

        var ledgerEntry = accountTransaction.getLedgerEntry();
        var accountProjectionUUID = accountTransaction.getLedgerProjectionRef().getUUID();
        var portfolioProjectionUUID = portfolioTransaction.getLedgerProjectionRef().getUUID();
        var account = accountTransaction.getLedgerProjectionRef().getAccount();
        var portfolio = portfolioTransaction.getLedgerProjectionRef().getPortfolio();

        preflight(ledgerEntry, accountTransaction, portfolioTransaction);
        LedgerInvestmentPlanRefSupport.requireCurrentRefsResolveUniquely(client, ledgerEntry);

        new LedgerMutationContext(client).mutateEntry(ledgerEntry, this::reverse);

        return BuySellEntry.readOnly(portfolio, find(portfolio, portfolioProjectionUUID), account,
                        find(account, accountProjectionUUID));
    }

    public boolean canReverseSafely(BuySellEntry entry)
    {
        Objects.requireNonNull(entry);

        if (!(entry.getAccountTransaction() instanceof LedgerBackedAccountTransaction accountTransaction)
                        || !(entry.getPortfolioTransaction() instanceof LedgerBackedPortfolioTransaction portfolioTransaction)
                        || accountTransaction.getLedgerEntry() != portfolioTransaction.getLedgerEntry())
            return false;

        try
        {
            var ledgerEntry = accountTransaction.getLedgerEntry();
            preflight(ledgerEntry, accountTransaction, portfolioTransaction);
            LedgerInvestmentPlanRefSupport.requireCurrentRefsResolveUniquely(client, ledgerEntry);
            return true;
        }
        catch (IllegalArgumentException | UnsupportedOperationException e)
        {
            return false;
        }
    }

    private void preflight(LedgerEntry entry, LedgerBackedAccountTransaction accountTransaction,
                    LedgerBackedPortfolioTransaction portfolioTransaction)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_028.message("Only ledger-backed buy/sell entries can be reversed")); //$NON-NLS-1$

        if (accountTransaction.getLedgerEntry() != portfolioTransaction.getLedgerEntry())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_028.message("Buy/sell projections do not belong to the same ledger entry")); //$NON-NLS-1$

        if (accountTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.ACCOUNT
                        || portfolioTransaction.getLedgerProjectionRef().getRole() != LedgerProjectionRole.PORTFOLIO)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_029.message("Buy/sell projections are not account/portfolio projections")); //$NON-NLS-1$

        var accountProjection = uniqueProjection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = uniqueProjection(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPosting = requireOnePosting(entry, LedgerPostingType.CASH);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (LedgerProjectionSupport.primaryPosting(entry, accountProjection) != cashPosting
                        || LedgerProjectionSupport.primaryPosting(entry, portfolioProjection) != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_030.message("Buy/sell projection primary postings are ambiguous")); //$NON-NLS-1$

        if (cashPosting.getAccount() != accountProjection.getAccount()
                        || securityPosting.getPortfolio() != portfolioProjection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_031.message("Buy/sell projection and posting owners do not match")); //$NON-NLS-1$

        rejectPostingForex(cashPosting);
        rejectPostingForex(securityPosting);
    }

    private void reverse(LedgerEntry entry)
    {
        var currentType = entry.getType();
        var targetType = currentType == LedgerEntryType.BUY ? LedgerEntryType.SELL : LedgerEntryType.BUY;
        var cashPosting = requireOnePosting(entry, LedgerPostingType.CASH);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var amount = reversedAmount(entry, currentType);

        cashPosting.setAmount(amount.getAmount());
        cashPosting.setCurrency(amount.getCurrencyCode());
        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());
        entry.setType(targetType);
    }

    private Money reversedAmount(LedgerEntry entry, LedgerEntryType currentType)
    {
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var transactionCurrency = securityPosting.getCurrency();
        var grossAmount = entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.GROSS_VALUE) //
                        .findFirst() //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .orElseGet(() -> Money.of(transactionCurrency, grossValueAmount(entry, currentType)));
        var feesAndTaxes = entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.FEE
                                        || posting.getType() == LedgerPostingType.TAX) //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .collect(MoneyCollectors.sum(transactionCurrency));

        return currentType == LedgerEntryType.BUY ? grossAmount.subtract(feesAndTaxes)
                        : grossAmount.add(feesAndTaxes);
    }

    private long grossValueAmount(LedgerEntry entry, LedgerEntryType currentType)
    {
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var feesAndTaxes = entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.FEE
                                        || posting.getType() == LedgerPostingType.TAX) //
                        .map(posting -> Money.of(posting.getCurrency(), posting.getAmount())) //
                        .collect(MoneyCollectors.sum(securityPosting.getCurrency())).getAmount();

        return currentType == LedgerEntryType.BUY ? securityPosting.getAmount() - feesAndTaxes
                        : securityPosting.getAmount() + feesAndTaxes;
    }

    private void rejectPostingForex(LedgerPosting posting)
    {
        if (posting.getForexAmount() != null || posting.getForexCurrency() != null || posting.getExchangeRate() != null)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_001
                            .message("Ledger buy/sell posting forex metadata cannot be reversed")); //$NON-NLS-1$
    }

    private LedgerPosting requireOnePosting(LedgerEntry entry, LedgerPostingType type)
    {
        var postings = entry.getPostings().stream().filter(posting -> posting.getType() == type).toList();

        if (postings.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_029.message("Ledger buy/sell entry must have exactly one " + type + " posting")); //$NON-NLS-1$ //$NON-NLS-2$

        return postings.get(0);
    }

    private LedgerProjectionRef uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_032
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
                                        "Ledger buy/sell account projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }

    private PortfolioTransaction find(Portfolio portfolio, String projectionUUID)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .filter(transaction -> projectionUUID.equals(transaction.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Ledger buy/sell portfolio projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
