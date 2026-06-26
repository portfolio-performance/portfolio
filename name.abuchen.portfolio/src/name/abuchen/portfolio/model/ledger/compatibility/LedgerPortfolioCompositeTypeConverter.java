package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
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
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var type = entry.getType();

        if (type == LedgerEntryType.BUY || type == LedgerEntryType.SELL)
            return prepareBuySellToOppositeDelivery(entry, projectionRef, transaction);

        if (type == LedgerEntryType.DELIVERY_INBOUND || type == LedgerEntryType.DELIVERY_OUTBOUND)
            return prepareDeliveryToOppositeBuySell(entry, projectionRef, transaction);

        throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_052.message("Unsupported composite portfolio conversion: " + type)); //$NON-NLS-1$
    }

    private Operation prepareBuySellToOppositeDelivery(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction)
    {
        var portfolio = projectionRef.getPortfolio();
        var projectionUUID = projectionRef.getUUID();

        preflightBuySell(entry, projectionRef, transaction, portfolio);

        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND;
        var targetRole = role(targetType);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(projectionUUID, LedgerProjectionRole.PORTFOLIO,
                        targetRole);

        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);
        LedgerEntryEditSupport.validatePatch(entry, editedEntry -> applyBuySellToOppositeDelivery(editedEntry,
                        projectionUUID));

        return new Operation(entry, portfolio, projectionUUID, roleChange,
                        editedEntry -> applyBuySellToOppositeDelivery(editedEntry, projectionUUID));
    }

    private Operation prepareDeliveryToOppositeBuySell(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction)
    {
        var portfolio = projectionRef.getPortfolio();
        var account = requireReferenceAccount(portfolio);
        var projectionUUID = projectionRef.getUUID();
        var accountProjectionUUID = UUID.randomUUID().toString();
        var cashPostingUUID = UUID.randomUUID().toString();

        preflightDelivery(entry, projectionRef, transaction, portfolio);

        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(projectionUUID, projectionRef.getRole(),
                        LedgerProjectionRole.PORTFOLIO);

        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);
        LedgerEntryEditSupport.validatePatch(entry,
                        editedEntry -> applyDeliveryToOppositeBuySell(editedEntry, projectionUUID, account,
                                        accountProjectionUUID, cashPostingUUID));

        return new Operation(entry, portfolio, projectionUUID, roleChange,
                        editedEntry -> applyDeliveryToOppositeBuySell(editedEntry, projectionUUID, account,
                                        accountProjectionUUID, cashPostingUUID));
    }

    private void preflightBuySell(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.BUY && entry.getType() != LedgerEntryType.SELL)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_043.message("Only ledger-backed buy/sell entries can be converted")); //$NON-NLS-1$

        if (projectionRef.getRole() != LedgerProjectionRole.PORTFOLIO)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_044.message("Only the portfolio side of a buy/sell entry can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_045.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        var accountProjection = uniqueProjection(entry, LedgerProjectionRole.ACCOUNT);
        var portfolioProjection = uniqueProjection(entry, LedgerProjectionRole.PORTFOLIO);
        var cashPosting = requireOnePosting(entry, LedgerPostingType.CASH);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (portfolioProjection != projectionRef)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_046.message("Selected projection is not the unique portfolio projection")); //$NON-NLS-1$

        if (LedgerProjectionSupport.primaryPosting(entry, accountProjection) != cashPosting
                        || LedgerProjectionSupport.primaryPosting(entry, portfolioProjection) != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_047.message("Buy/sell projection primary postings are ambiguous")); //$NON-NLS-1$

        if (cashPosting.getAccount() != accountProjection.getAccount()
                        || securityPosting.getPortfolio() != portfolioProjection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_048.message("Buy/sell projection and posting owners do not match")); //$NON-NLS-1$

        rejectPostingForex(cashPosting);
        rejectPostingForex(securityPosting);
        reversedBuySellAmount(entry, entry.getType());
    }

    private void preflightDelivery(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_049.message("Only ledger-backed delivery entries can be converted")); //$NON-NLS-1$

        var expectedRole = role(entry.getType());

        if (projectionRef.getRole() != expectedRole)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_050.message("Only the delivery projection can be converted")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_051.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        if (entry.getPostings().stream().anyMatch(posting -> posting.getType() == LedgerPostingType.CASH))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_053.message("Ledger delivery entry must not already have a cash posting")); //$NON-NLS-1$

        if (entry.getProjectionRefs().stream().anyMatch(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT))
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_052.message("Ledger delivery entry must not already have an account projection")); //$NON-NLS-1$

        var projection = uniqueProjection(entry, expectedRole);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (projection != projectionRef)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_053.message("Selected projection is not the unique delivery projection")); //$NON-NLS-1$

        if (LedgerProjectionSupport.primaryPosting(entry, projection) != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_054.message("Delivery projection primary posting is ambiguous")); //$NON-NLS-1$

        if (securityPosting.getPortfolio() != projection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_055.message("Delivery projection and posting portfolio do not match")); //$NON-NLS-1$

        rejectPostingForex(securityPosting);
        reversedDeliveryAmount(entry, entry.getType());
    }

    private void applyBuySellToOppositeDelivery(LedgerEntry entry, String projectionUUID)
    {
        var targetType = entry.getType() == LedgerEntryType.BUY ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND;
        var targetRole = role(targetType);
        var amount = reversedBuySellAmount(entry, entry.getType());
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var portfolioProjection = projection(entry, projectionUUID);

        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());

        List.copyOf(entry.getPostings()).stream() //
                        .filter(posting -> posting.getType() == LedgerPostingType.CASH) //
                        .forEach(entry::removePosting);

        List.copyOf(entry.getProjectionRefs()).stream() //
                        .filter(projection -> projection.getRole() == LedgerProjectionRole.ACCOUNT) //
                        .forEach(entry::removeProjectionRef);

        portfolioProjection.setRole(targetRole);
        entry.setType(targetType);
    }

    private void applyDeliveryToOppositeBuySell(LedgerEntry entry, String portfolioProjectionUUID, Account account,
                    String accountProjectionUUID, String cashPostingUUID)
    {
        var targetType = entry.getType() == LedgerEntryType.DELIVERY_INBOUND ? LedgerEntryType.SELL
                        : LedgerEntryType.BUY;
        var amount = reversedDeliveryAmount(entry, entry.getType());
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var cashPosting = new LedgerPosting(cashPostingUUID);
        var accountProjection = new LedgerProjectionRef(accountProjectionUUID);
        var portfolioProjection = projection(entry, portfolioProjectionUUID);
        var unitPostings = entry.getPostings().stream() //
                        .filter(posting -> posting != securityPosting) //
                        .toList();

        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());

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

    private LedgerProjectionRef uniqueProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_056
                            .message("Expected one projection for role " + role + " but found " //$NON-NLS-1$ //$NON-NLS-2$
                                            + projections.size()));

        return projections.get(0);
    }

    private LedgerProjectionRef projection(LedgerEntry entry, String projectionUUID)
    {
        return entry.getProjectionRefs().stream() //
                        .filter(projection -> projectionUUID.equals(projection.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Ledger portfolio projection not found: " + projectionUUID)); //$NON-NLS-1$
    }

    private LedgerProjectionRole role(LedgerEntryType entryType)
    {
        return entryType == LedgerEntryType.DELIVERY_INBOUND ? LedgerProjectionRole.DELIVERY_INBOUND
                        : LedgerProjectionRole.DELIVERY_OUTBOUND;
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
