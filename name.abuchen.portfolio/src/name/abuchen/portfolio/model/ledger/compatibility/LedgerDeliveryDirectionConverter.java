package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

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
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

/**
 * Reverses ledger-backed delivery transactions between inbound and outbound deliveries.
 * This class is part of the Ledger compatibility layer for existing UI and action code. It
 * updates the Ledger entry instead of replaying legacy projection setters.
 */
public final class LedgerDeliveryDirectionConverter
{
    private final Client client;

    public LedgerDeliveryDirectionConverter(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public PortfolioTransaction reverse(TransactionPair<PortfolioTransaction> transaction)
    {
        Objects.requireNonNull(transaction);

        if (!(transaction.getTransaction() instanceof LedgerBackedPortfolioTransaction ledgerTransaction))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_034.message("Only ledger-backed delivery transactions can be reversed")); //$NON-NLS-1$

        var entry = ledgerTransaction.getLedgerEntry();
        var projectionRef = ledgerTransaction.getLedgerProjectionRef();
        var portfolio = projectionRef.getPortfolio();
        var projectionUUID = projectionRef.getUUID();

        preflight(entry, projectionRef, transaction, portfolio);
        var targetRole = role(entry.getType() == LedgerEntryType.DELIVERY_INBOUND
                        ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND);
        var roleChange = LedgerInvestmentPlanRefSupport.roleChange(projectionUUID, projectionRef.getRole(),
                        targetRole);
        LedgerInvestmentPlanRefSupport.requireRefsFollowRoleChanges(client, entry, roleChange);

        new LedgerMutationContext(client).mutateEntry(entry, editedEntry -> reverse(editedEntry, projectionUUID));
        LedgerInvestmentPlanRefSupport.updateProjectionRoles(client, entry, roleChange);

        return find(portfolio, projectionUUID);
    }

    private void preflight(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    TransactionPair<PortfolioTransaction> transaction, Portfolio portfolio)
    {
        if (entry.getType() != LedgerEntryType.DELIVERY_INBOUND && entry.getType() != LedgerEntryType.DELIVERY_OUTBOUND)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_033.message("Only ledger-backed delivery entries can be reversed")); //$NON-NLS-1$

        var expectedRole = role(entry.getType());

        if (projectionRef.getRole() != expectedRole)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_034.message("Only the delivery projection can be reversed")); //$NON-NLS-1$

        if (transaction.getOwner() != portfolio)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_035.message("Selected portfolio does not own the ledger projection")); //$NON-NLS-1$

        var projection = requireOneProjection(entry, expectedRole);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);

        if (projection != projectionRef)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_036.message("Selected projection is not the unique delivery projection")); //$NON-NLS-1$

        if (LedgerProjectionSupport.primaryPosting(entry, projection) != securityPosting)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_037.message("Delivery projection primary posting is ambiguous")); //$NON-NLS-1$

        if (securityPosting.getPortfolio() != projection.getPortfolio())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_038.message("Delivery projection and posting portfolio do not match")); //$NON-NLS-1$

        if (securityPosting.getForexAmount() != null || securityPosting.getForexCurrency() != null
                        || securityPosting.getExchangeRate() != null)
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_CONVERT_002
                            .message("Ledger delivery posting forex metadata cannot be reversed")); //$NON-NLS-1$

        reversedAmount(entry, entry.getType());
    }

    private void reverse(LedgerEntry entry, String projectionUUID)
    {
        var currentType = entry.getType();
        var targetType = currentType == LedgerEntryType.DELIVERY_INBOUND ? LedgerEntryType.DELIVERY_OUTBOUND
                        : LedgerEntryType.DELIVERY_INBOUND;
        var amount = reversedAmount(entry, currentType);
        var securityPosting = requireOnePosting(entry, LedgerPostingType.SECURITY);
        var projection = entry.getProjectionRefs().stream() //
                        .filter(item -> projectionUUID.equals(item.getUUID())) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Ledger delivery projection not found: " + projectionUUID)); //$NON-NLS-1$

        securityPosting.setAmount(amount.getAmount());
        securityPosting.setCurrency(amount.getCurrencyCode());
        projection.setRole(role(targetType));
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

        return currentType == LedgerEntryType.DELIVERY_INBOUND ? grossAmount.subtract(feesAndTaxes)
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

        return currentType == LedgerEntryType.DELIVERY_INBOUND ? securityPosting.getAmount() - feesAndTaxes
                        : securityPosting.getAmount() + feesAndTaxes;
    }

    private LedgerPosting requireOnePosting(LedgerEntry entry, LedgerPostingType type)
    {
        var postings = entry.getPostings().stream().filter(posting -> posting.getType() == type).toList();

        if (postings.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_035.message("Ledger delivery entry must have exactly one " + type + " posting")); //$NON-NLS-1$ //$NON-NLS-2$

        return postings.get(0);
    }

    private LedgerProjectionRef requireOneProjection(LedgerEntry entry, LedgerProjectionRole role)
    {
        var projections = entry.getProjectionRefs().stream().filter(projection -> projection.getRole() == role).toList();

        if (projections.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_PROJ_039
                            .message("Ledger delivery entry must have exactly one " + role + " projection")); //$NON-NLS-1$ //$NON-NLS-2$

        return projections.get(0);
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
                                        "Reversed ledger delivery projection was not materialized: " //$NON-NLS-1$
                                                        + projectionUUID));
    }
}
