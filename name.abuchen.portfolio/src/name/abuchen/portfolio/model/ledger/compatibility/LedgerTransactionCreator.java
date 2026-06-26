package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.Money;

/**
 * Creates and updates ledger-backed  transactions.
 * This class is part of the Ledger compatibility layer for existing UI, import, and action
 * code. Contributor code should use it instead of mutating projected legacy transactions.
 */
public final class LedgerTransactionCreator
{
    private final Client client;

    public LedgerTransactionCreator(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public CreatedTransaction createDeposit(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg)
    {
        return createDeposit(metadata, cashLeg, LedgerCreationUnits.none());
    }

    public CreatedTransaction createDeposit(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.DEPOSIT, LedgerPostingType.CASH, cashLeg,
                        LedgerOptionalSecurity.none(), units));
    }

    public CreatedTransaction createRemoval(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg)
    {
        return createRemoval(metadata, cashLeg, LedgerCreationUnits.none());
    }

    public CreatedTransaction createRemoval(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.REMOVAL, LedgerPostingType.CASH, cashLeg,
                        LedgerOptionalSecurity.none(), units));
    }

    public CreatedTransaction createInterest(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.INTEREST, LedgerPostingType.CASH, cashLeg, security,
                        units));
    }

    public CreatedTransaction createInterestCharge(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.INTEREST_CHARGE, LedgerPostingType.CASH, cashLeg,
                        security, units));
    }

    public CreatedTransaction createFee(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.FEES, LedgerPostingType.FEE, cashLeg, security, units));
    }

    public CreatedTransaction createFeeRefund(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.FEES_REFUND, LedgerPostingType.FEE, cashLeg, security,
                        units));
    }

    public CreatedTransaction createTax(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.TAXES, LedgerPostingType.TAX, cashLeg, security, units));
    }

    public CreatedTransaction createTaxRefund(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerOptionalSecurity security, LedgerCreationUnits units)
    {
        return add(createAccountEntry(metadata, LedgerEntryType.TAX_REFUND, LedgerPostingType.TAX, cashLeg, security,
                        units));
    }

    public CreatedTransaction createDividend(LedgerTransactionMetadata metadata, LedgerDividend dividend)
    {
        Objects.requireNonNull(dividend);

        var entry = createBaseEntry(metadata, LedgerEntryType.DIVIDENDS);
        var cashPosting = createCashPosting(LedgerPostingType.CASH, dividend.getCashLeg());

        applySecurity(cashPosting, dividend.getSecurity());
        cashPosting.setShares(dividend.getShares());

        if (dividend.hasExDate())
            cashPosting.addParameter(
                            LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                                            dividend.getExDate()));

        entry.addPosting(cashPosting);
        var unitPostings = addUnitPostings(entry, dividend.getUnits());
        var projection = accountProjection(dividend.getCashLeg().getAccount(), cashPosting);
        addUnitMemberships(projection, unitPostings);
        entry.addProjectionRef(projection);

        return add(entry);
    }

    public CreatedTransaction createInboundDelivery(LedgerTransactionMetadata metadata, LedgerDeliveryLeg deliveryLeg)
    {
        return add(createDeliveryEntry(metadata, LedgerEntryType.DELIVERY_INBOUND, LedgerProjectionRole.DELIVERY_INBOUND,
                        deliveryLeg));
    }

    public CreatedTransaction createOutboundDelivery(LedgerTransactionMetadata metadata, LedgerDeliveryLeg deliveryLeg)
    {
        return add(createDeliveryEntry(metadata, LedgerEntryType.DELIVERY_OUTBOUND,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, deliveryLeg));
    }

    public CreatedTransaction createBuy(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerPortfolioSecurityLeg securityLeg, LedgerCreationUnits units)
    {
        return add(createBuySellEntry(metadata, LedgerEntryType.BUY, cashLeg, securityLeg, units));
    }

    public CreatedTransaction createSell(LedgerTransactionMetadata metadata, LedgerAccountCashLeg cashLeg,
                    LedgerPortfolioSecurityLeg securityLeg, LedgerCreationUnits units)
    {
        return add(createBuySellEntry(metadata, LedgerEntryType.SELL, cashLeg, securityLeg, units));
    }

    public CreatedTransaction createAccountTransfer(LedgerTransactionMetadata metadata, LedgerCashTransferLeg source,
                    LedgerCashTransferLeg target)
    {
        return add(createAccountTransferEntry(metadata, source, target));
    }

    LedgerEntry createAccountTransferEntry(LedgerTransactionMetadata metadata, LedgerCashTransferLeg source,
                    LedgerCashTransferLeg target)
    {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);

        var entry = createBaseEntry(metadata, LedgerEntryType.CASH_TRANSFER);

        var sourcePosting = createCashPosting(LedgerPostingType.CASH, source);
        var targetPosting = createCashPosting(LedgerPostingType.CASH, target);

        entry.addPosting(sourcePosting);
        entry.addPosting(targetPosting);
        entry.addProjectionRef(accountProjection(LedgerProjectionRole.SOURCE_ACCOUNT, source.getAccount(),
                        sourcePosting));
        entry.addProjectionRef(accountProjection(LedgerProjectionRole.TARGET_ACCOUNT, target.getAccount(),
                        targetPosting));

        return entry;
    }

    public CreatedTransaction createPortfolioTransfer(LedgerTransactionMetadata metadata,
                    LedgerPortfolioTransferSecurity security, LedgerPortfolioTransferLeg source,
                    LedgerPortfolioTransferLeg target)
    {
        return add(createPortfolioTransferEntry(metadata, security, source, target));
    }

    LedgerEntry createPortfolioTransferEntry(LedgerTransactionMetadata metadata,
                    LedgerPortfolioTransferSecurity security, LedgerPortfolioTransferLeg source,
                    LedgerPortfolioTransferLeg target)
    {
        Objects.requireNonNull(security);
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);

        var entry = createBaseEntry(metadata, LedgerEntryType.SECURITY_TRANSFER);

        var sourcePosting = createSecurityPosting(source, security);
        var targetPosting = createSecurityPosting(target, security);

        entry.addPosting(sourcePosting);
        entry.addPosting(targetPosting);
        entry.addProjectionRef(portfolioProjection(LedgerProjectionRole.SOURCE_PORTFOLIO, source.getPortfolio(),
                        sourcePosting));
        entry.addProjectionRef(portfolioProjection(LedgerProjectionRole.TARGET_PORTFOLIO, target.getPortfolio(),
                        targetPosting));

        return entry;
    }

    private LedgerEntry createAccountEntry(LedgerTransactionMetadata metadata, LedgerEntryType entryType,
                    LedgerPostingType postingType, LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security,
                    LedgerCreationUnits units)
    {
        Objects.requireNonNull(cashLeg);
        Objects.requireNonNull(security);
        Objects.requireNonNull(units);

        var entry = createBaseEntry(metadata, entryType);
        var posting = createCashPosting(postingType, cashLeg);

        applySecurity(posting, security);

        entry.addPosting(posting);
        var unitPostings = addUnitPostings(entry, units);
        var projection = accountProjection(cashLeg.getAccount(), posting);
        addUnitMemberships(projection, unitPostings);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry createDeliveryEntry(LedgerTransactionMetadata metadata, LedgerEntryType entryType,
                    LedgerProjectionRole role, LedgerDeliveryLeg deliveryLeg)
    {
        Objects.requireNonNull(deliveryLeg);

        var entry = createBaseEntry(metadata, entryType);
        var posting = new LedgerPosting();
        var securityQuantity = deliveryLeg.getSecurityQuantity();
        var value = deliveryLeg.getValue();

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(deliveryLeg.getPortfolio());
        posting.setSecurity(securityQuantity.getSecurity());
        posting.setShares(securityQuantity.getShares());
        applyMoney(posting, value, deliveryLeg.getForex());

        entry.addPosting(posting);
        var unitPostings = addUnitPostings(entry, deliveryLeg.getUnits());
        var projection = portfolioProjection(role, deliveryLeg.getPortfolio(), posting);
        addUnitMemberships(projection, unitPostings);
        entry.addProjectionRef(projection);

        return entry;
    }

    private LedgerEntry createBuySellEntry(LedgerTransactionMetadata metadata, LedgerEntryType entryType,
                    LedgerAccountCashLeg cashLeg, LedgerPortfolioSecurityLeg securityLeg, LedgerCreationUnits units)
    {
        Objects.requireNonNull(cashLeg);
        Objects.requireNonNull(securityLeg);
        Objects.requireNonNull(units);

        var entry = createBaseEntry(metadata, entryType);

        var cashPosting = createCashPosting(LedgerPostingType.CASH, cashLeg);
        var securityPosting = createSecurityPosting(securityLeg);

        entry.addPosting(cashPosting);
        entry.addPosting(securityPosting);
        var unitPostings = addUnitPostings(entry, units);
        var accountProjection = accountProjection(cashLeg.getAccount(), cashPosting);
        var portfolioProjection = portfolioProjection(LedgerProjectionRole.PORTFOLIO, securityLeg.getPortfolio(),
                        securityPosting);
        addUnitMemberships(accountProjection, unitPostings);
        addUnitMemberships(portfolioProjection, unitPostings);
        entry.addProjectionRef(accountProjection);
        entry.addProjectionRef(portfolioProjection);

        return entry;
    }

    private LedgerEntry createBaseEntry(LedgerTransactionMetadata metadata, LedgerEntryType type)
    {
        Objects.requireNonNull(metadata);

        var entry = new LedgerEntry();

        entry.setType(type);
        entry.setDateTime(metadata.getDateTime());
        entry.setNote(metadata.getNote());
        entry.setSource(metadata.getSource());

        return entry;
    }

    private LedgerPosting createCashPosting(LedgerPostingType postingType, LedgerAccountCashLeg cashLeg)
    {
        var posting = new LedgerPosting();

        posting.setType(postingType);
        posting.setAccount(cashLeg.getAccount());
        applyMoney(posting, cashLeg.getAmount(), cashLeg.getForex());

        return posting;
    }

    private LedgerPosting createCashPosting(LedgerPostingType postingType, LedgerCashTransferLeg cashLeg)
    {
        var posting = new LedgerPosting();

        posting.setType(postingType);
        posting.setAccount(cashLeg.getAccount());
        applyMoney(posting, cashLeg.getAmount(), cashLeg.getForex());

        return posting;
    }

    private LedgerPosting createSecurityPosting(LedgerPortfolioSecurityLeg securityLeg)
    {
        var posting = new LedgerPosting();
        var securityQuantity = securityLeg.getSecurityQuantity();

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(securityLeg.getPortfolio());
        posting.setSecurity(securityQuantity.getSecurity());
        posting.setShares(securityQuantity.getShares());
        applyMoney(posting, securityLeg.getValue(), securityLeg.getForex());

        return posting;
    }

    private LedgerPosting createSecurityPosting(LedgerPortfolioTransferLeg leg,
                    LedgerPortfolioTransferSecurity security)
    {
        var posting = new LedgerPosting();

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(leg.getPortfolio());
        posting.setSecurity(security.getSecurity());
        posting.setShares(security.getShares());
        applyMoney(posting, leg.getValue(), leg.getForex());

        return posting;
    }

    private List<LedgerPosting> addUnitPostings(LedgerEntry entry, LedgerCreationUnits units)
    {
        var postings = new ArrayList<LedgerPosting>();

        for (var unit : units.getUnits())
        {
            var posting = new LedgerPosting();

            posting.setType(unit.getPostingType());
            applyMoney(posting, unit.getAmount(), unit.getForex());
            entry.addPosting(posting);
            postings.add(posting);
        }

        return List.copyOf(postings);
    }

    private void addUnitMemberships(LedgerProjectionRef projection, List<LedgerPosting> unitPostings)
    {
        for (var posting : unitPostings)
        {
            switch (posting.getType())
            {
                case FEE -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.FEE_UNIT);
                case TAX -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.TAX_UNIT);
                case GROSS_VALUE -> projection.addMembership(posting.getUUID(), ProjectionMembershipRole.GROSS_VALUE_UNIT);
                default -> throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_062.message("Unsupported unit posting type: " + posting.getType())); //$NON-NLS-1$
            }
        }
    }

    private void applyMoney(LedgerPosting posting, Money amount, LedgerForexAmount forex)
    {
        posting.setAmount(amount.getAmount());
        posting.setCurrency(amount.getCurrencyCode());

        if (forex.isPresent())
        {
            posting.setForexAmount(forex.getForexAmount().getAmount());
            posting.setForexCurrency(forex.getForexAmount().getCurrencyCode());
            posting.setExchangeRate(forex.getExchangeRate());
        }
    }

    private void applySecurity(LedgerPosting posting, LedgerOptionalSecurity security)
    {
        if (security.isPresent())
            posting.setSecurity(security.getSecurity());
    }

    private LedgerProjectionRef accountProjection(Account account, LedgerPosting posting)
    {
        return accountProjection(LedgerProjectionRole.ACCOUNT, account, posting);
    }

    private LedgerProjectionRef accountProjection(LedgerProjectionRole role, Account account, LedgerPosting posting)
    {
        var projection = new LedgerProjectionRef();

        projection.setRole(role);
        projection.setAccount(account);
        projection.setPrimaryPosting(posting);

        return projection;
    }

    private LedgerProjectionRef portfolioProjection(LedgerProjectionRole role, Portfolio portfolio,
                    LedgerPosting posting)
    {
        var projection = new LedgerProjectionRef();

        projection.setRole(role);
        projection.setPortfolio(portfolio);
        projection.setPrimaryPosting(posting);

        return projection;
    }

    CreatedTransaction add(LedgerEntry entry)
    {
        var liveEntry = new LedgerMutationContext(client).attachEntry(entry);

        return new CreatedTransaction(liveEntry);
    }

    public static final class CreatedTransaction
    {
        private final LedgerEntry entry;

        private CreatedTransaction(LedgerEntry entry)
        {
            this.entry = entry;
        }

        public LedgerEntry getEntry()
        {
            return entry;
        }

        public List<LedgerProjectionRef> getProjectionRefs()
        {
            return entry.getProjectionRefs();
        }
    }
}
