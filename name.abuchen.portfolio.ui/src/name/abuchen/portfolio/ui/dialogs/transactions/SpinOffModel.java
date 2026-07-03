package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;

/**
 * Calculation core for the spin-off create/edit dialog.
 * <p>
 * Holds the event-level fields (source security + portfolio + ex-date,
 * target security, distribution ratio, editable entitlement, basis ratio +
 * spinco FMV/share reference price, cash-in-lieu, exchange rate), the floor
 * split of the entitlement into whole shares vs. cashed fraction, an FMV
 * helper that derives the basis ratio, a live conservation summary and a
 * validation status.
 * <p>
 * {@link #applyChanges()} builds (create) or reconciles (edit, via
 * {@link #setSource(CorporateActionEntry)}) the four-leg
 * {@link name.abuchen.portfolio.model.CorporateActionEntry} and registers it
 * on the {@link Client}.
 */
public class SpinOffModel extends AbstractModel
{
    public enum Properties
    {
        sourceSecurity, portfolio, exDate, targetSecurity, //
        distributionNumerator, distributionDenominator, //
        entitlement, wholeShares, fractionShares, //
        basisRatio, spinOffPricePerShare, parentFmvPerShare, spinOffFmvPerShare, //
        cashInLieuAmount, exchangeRate, conservationSummary, entitlementWarning, calculationStatus;
    }

    private final Client client;

    private Security sourceSecurity;
    private Portfolio portfolio;
    private LocalDate exDate = LocalDate.now();
    private Security targetSecurity;
    private int distributionNumerator = 1;
    private int distributionDenominator = 1;
    private long entitlement;
    private BigDecimal basisRatio = BigDecimal.ZERO;
    private long spinOffPricePerShare;
    private long parentFmvPerShare;
    private long spinOffFmvPerShare;
    private long cashInLieuAmount;
    private BigDecimal exchangeRate = BigDecimal.ONE;

    private CorporateActionEntry source;
    private Account cashInLieuAccount;

    private boolean suppressSeed;

    private IStatus calculationStatus = ValidationStatus.ok();

    public SpinOffModel(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public String getHeading()
    {
        return Messages.LabelSpinOff;
    }

    @Override
    public LocalDate getDate()
    {
        return exDate;
    }

    public Security getSourceSecurity()
    {
        return sourceSecurity;
    }

    public void setSourceSecurity(Security sourceSecurity)
    {
        firePropertyChange(Properties.sourceSecurity.name(), this.sourceSecurity, this.sourceSecurity = sourceSecurity);
        recalculate();
        if (source == null)
            seedEntitlement();

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio);

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
        if (source == null)
            seedEntitlement();

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    public LocalDate getExDate()
    {
        return exDate;
    }

    public void setExDate(LocalDate exDate)
    {
        firePropertyChange(Properties.exDate.name(), this.exDate, this.exDate = exDate);
        if (source == null)
            seedEntitlement();

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    public Security getTargetSecurity()
    {
        return targetSecurity;
    }

    public void setTargetSecurity(Security targetSecurity)
    {
        firePropertyChange(Properties.targetSecurity.name(), this.targetSecurity, this.targetSecurity = targetSecurity);
        recalculate();
    }

    public int getDistributionNumerator()
    {
        return distributionNumerator;
    }

    public void setDistributionNumerator(int distributionNumerator)
    {
        firePropertyChange(Properties.distributionNumerator.name(), this.distributionNumerator,
                        this.distributionNumerator = distributionNumerator);
        seedEntitlement();

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    public int getDistributionDenominator()
    {
        return distributionDenominator;
    }

    public void setDistributionDenominator(int distributionDenominator)
    {
        firePropertyChange(Properties.distributionDenominator.name(), this.distributionDenominator,
                        this.distributionDenominator = distributionDenominator);
        seedEntitlement();

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    // --- entitlement + whole/fraction split (floor; §12) ---

    public long getEntitlement()
    {
        return entitlement;
    }

    public void setEntitlement(long entitlement)
    {
        long oldWholeShares = getWholeShares();
        long oldFractionShares = getFractionShares();
        String oldConservationSummary = getConservationSummary();

        firePropertyChange(Properties.entitlement.name(), this.entitlement, this.entitlement = entitlement);

        firePropertyChange(Properties.wholeShares.name(), oldWholeShares, getWholeShares());
        firePropertyChange(Properties.fractionShares.name(), oldFractionShares, getFractionShares());
        firePropertyChange(Properties.conservationSummary.name(), oldConservationSummary, getConservationSummary());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());

        firePropertyChange(Properties.entitlementWarning.name(), null, getEntitlementWarning());
    }

    public long getWholeShares()
    {
        // floor to a whole share (share values scale by Values.Share.factor())
        return (entitlement / Values.Share.factor()) * Values.Share.factor();
    }

    public long getFractionShares()
    {
        return entitlement - getWholeShares();
    }

    /**
     * Seeds the entitlement from the distribution ratio and the source
     * security's holdings at the ex-date (spec §10): {@code entitlement =
     * round(holdings × numerator / denominator)}. No-op unless all inputs are
     * present and the denominator is positive, and while {@link #suppressSeed}
     * is set (edit prefill). The entitlement stays editable — a later manual
     * {@link #setEntitlement} is not overwritten until a trigger fires again.
     */
    private void seedEntitlement()
    {
        if (suppressSeed || sourceSecurity == null || portfolio == null || exDate == null
                        || distributionDenominator <= 0)
            return;

        long holdings = sharesHeldAtExDate();
        setEntitlement(Math.round(holdings * (double) distributionNumerator / distributionDenominator));
    }

    /**
     * The source security's share count in the selected portfolio at the
     * ex-date. Package-private so headless tests can stub it (the live path
     * builds a {@link PortfolioSnapshot}; a {@link SecurityPosition} is always
     * in the instrument's currency, so the converter currency is arbitrary).
     */
    /* package */ long sharesHeldAtExDate()
    {
        if (portfolio == null || sourceSecurity == null || exDate == null || getExchangeRateProviderFactory() == null)
            return 0;

        CurrencyConverter converter = new CurrencyConverterImpl(getExchangeRateProviderFactory(), CurrencyUnit.EUR);
        SecurityPosition position = PortfolioSnapshot.create(portfolio, converter, exDate) //
                        .getPositionsBySecurity().get(sourceSecurity);
        return position != null ? position.getShares() : 0;
    }

    // --- basis ratio + spinco FMV/share reference price ---

    public BigDecimal getBasisRatio()
    {
        return basisRatio;
    }

    public void setBasisRatio(BigDecimal basisRatio)
    {
        var newValue = basisRatio == null ? BigDecimal.ZERO : basisRatio;

        String oldConservationSummary = getConservationSummary();

        firePropertyChange(Properties.basisRatio.name(), this.basisRatio, this.basisRatio = newValue);
        firePropertyChange(Properties.conservationSummary.name(), oldConservationSummary, getConservationSummary());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getSpinOffPricePerShare()
    {
        return spinOffPricePerShare;
    }

    public void setSpinOffPricePerShare(long spinOffPricePerShare)
    {
        firePropertyChange(Properties.spinOffPricePerShare.name(), this.spinOffPricePerShare,
                        this.spinOffPricePerShare = spinOffPricePerShare);
    }

    public long getParentFmvPerShare()
    {
        return parentFmvPerShare;
    }

    public void setParentFmvPerShare(long parentFmvPerShare)
    {
        firePropertyChange(Properties.parentFmvPerShare.name(), this.parentFmvPerShare,
                        this.parentFmvPerShare = parentFmvPerShare);
    }

    public long getSpinOffFmvPerShare()
    {
        return spinOffFmvPerShare;
    }

    public void setSpinOffFmvPerShare(long spinOffFmvPerShare)
    {
        firePropertyChange(Properties.spinOffFmvPerShare.name(), this.spinOffFmvPerShare,
                        this.spinOffFmvPerShare = spinOffFmvPerShare);
    }

    /**
     * Derives the basis ratio from the parent and spinco fair-market values
     * per share, i.e. spinco FMV / (parent FMV + spinco FMV). Only the ratio
     * and the spinco FMV/share (as the persisted reference price, §10) are
     * kept; the FMVs themselves are display-only inputs to this helper.
     */
    public void applyFmvHelper()
    {
        long denom = parentFmvPerShare + spinOffFmvPerShare;
        if (denom <= 0)
            return;

        var ratio = BigDecimal.valueOf(spinOffFmvPerShare).divide(BigDecimal.valueOf(denom), 10,
                        RoundingMode.HALF_UP);
        setBasisRatio(ratio);
        setSpinOffPricePerShare(spinOffFmvPerShare); // spinco FMV/share is the reference price
    }

    public long getCashInLieuAmount()
    {
        return cashInLieuAmount;
    }

    public void setCashInLieuAmount(long cashInLieuAmount)
    {
        firePropertyChange(Properties.cashInLieuAmount.name(), this.cashInLieuAmount,
                        this.cashInLieuAmount = cashInLieuAmount);
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate)
    {
        var newValue = exchangeRate == null ? BigDecimal.ZERO : exchangeRate;
        firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newValue);

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    private void recalculate()
    {
        String oldConservationSummary = getConservationSummary();
        firePropertyChange(Properties.conservationSummary.name(), oldConservationSummary, getConservationSummary());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    private IStatus calculateStatus()
    {
        if (sourceSecurity == null)
            return ValidationStatus
                            .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSecurity));
        if (targetSecurity == null)
            return ValidationStatus
                            .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSecurity));
        if (targetSecurity.equals(sourceSecurity))
            return ValidationStatus.error(Messages.MsgSpinOffTargetMustDifferFromSource);
        if (distributionNumerator <= 0 || distributionDenominator <= 0)
            return ValidationStatus.error(Messages.MsgSpinOffRatioMustBePositive);
        if (!sourceSecurity.getCurrencyCode().equals(targetSecurity.getCurrencyCode())
                        && exchangeRate.signum() <= 0)
            return ValidationStatus.error(Messages.MsgSpinOffExchangeRateRequired);
        if (getFractionShares() > 0)
        {
            Account cashAccount = resolveCashAccount();
            if (cashAccount != null)
            {
                String cashCurrency = cashAccount.getCurrencyCode();
                if (!cashCurrency.equals(sourceSecurity.getCurrencyCode())
                                && !cashCurrency.equals(targetSecurity.getCurrencyCode()))
                    return ValidationStatus.error(Messages.MsgSpinOffUnsupportedCashCurrency);
            }
        }
        if (entitlement <= 0)
            return ValidationStatus
                            .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares));
        if (getFractionShares() > 0 && resolveCashAccount() == null)
            return ValidationStatus.error(Messages.MsgMissingReferenceAccount);
        if (basisRatio == null || basisRatio.signum() <= 0 || basisRatio.compareTo(BigDecimal.ONE) >= 0)
            return ValidationStatus.error(Messages.MsgSpinOffBasisRatioOutOfRange);
        return ValidationStatus.ok();
    }

    /**
     * §13.2 entry-time soft-warn: non-null when the entered
     * {@link #entitlement} diverges from {@code round(holdings-at-ex-date ×
     * numerator / denominator)}. Deliberately kept out of
     * {@link #calculationStatus} -- OK is gated on severity == OK
     * ({@code AbstractTransactionDialog}), so surfacing this as a validation
     * warning would wrongly disable OK. Consumed by a dedicated,
     * non-gating dialog {@code Label} instead.
     */
    public String getEntitlementWarning()
    {
        if (sourceSecurity == null || portfolio == null || exDate == null || distributionDenominator <= 0)
            return null;
        long holdings = sharesHeldAtExDate();
        if (holdings <= 0)
            return null;
        long implied = Math.round(holdings * (double) distributionNumerator / distributionDenominator);
        return entitlement != implied ? Messages.MsgSpinOffEntitlementDiffersFromRatio : null;
    }

    public String getConservationSummary()
    {
        // human-readable: entitlement -> whole shares delivered + fraction
        // cashed; basis ratio moved to spinco. Display-only.
        return MessageFormat.format(Messages.MsgSpinOffConservationSummary, //
                        Values.Share.format(getWholeShares()), Values.Share.format(getFractionShares()),
                        Values.Percent2.format(basisRatio.doubleValue()));
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Puts the model into edit mode: prefills the fields from an existing
     * four-leg group so {@link #applyChanges()} reconciles (deletes +
     * rebuilds) it instead of creating a new one.
     */
    public void setSource(CorporateActionEntry entry)
    {
        this.source = entry;
        suppressSeed = true;
        try
        {
            setSourceSecurity(entry.getLeg(CorporateActionEntry.LegRole.SOURCE) //
                            .map(Transaction::getSecurity).orElse(null));
            entry.getLeg(CorporateActionEntry.LegRole.TARGET).ifPresent(t -> {
                setTargetSecurity(t.getSecurity());
                setEntitlement(t.getShares());
            });
            // owning portfolio of the source leg
            setPortfolio((Portfolio) entry.getLegs().stream()
                            .filter(l -> l.role() == CorporateActionEntry.LegRole.SOURCE).findFirst()
                            .map(CorporateActionEntry.Leg::owner).orElse(null));
            if (entry.getExDate() != null)
                setExDate(entry.getExDate());
            setBasisRatio(entry.getBasisRatio());
            setSpinOffPricePerShare(entry.getReferencePrice());
            if (entry.getDistributionRatio() != null)
            {
                setDistributionNumerator(entry.getDistributionRatio().numerator());
                setDistributionDenominator(entry.getDistributionRatio().denominator());
            }
            entry.getLeg(CorporateActionEntry.LegRole.CASH_IN_LIEU) //
                            .ifPresent(t -> setCashInLieuAmount(t.getAmount()));
            this.cashInLieuAccount = (Account) entry.getLegs().stream()
                            .filter(l -> l.role() == CorporateActionEntry.LegRole.CASH_IN_LIEU).findFirst()
                            .map(CorporateActionEntry.Leg::owner).orElse(null);
            // restore the exchange rate used to build the target leg's GROSS_VALUE
            // forex unit (multi-currency events); single-currency entries have no
            // such unit, so the rate stays at the default ONE
            entry.getLeg(CorporateActionEntry.LegRole.TARGET)
                            .flatMap(t -> t.getUnit(Transaction.Unit.Type.GROSS_VALUE))
                            .map(Transaction.Unit::getExchangeRate).ifPresent(this::setExchangeRate);
        }
        finally
        {
            suppressSeed = false;
        }
    }

    public boolean hasSource()
    {
        return source != null;
    }

    /** The account that owns the cash-in-lieu leg: the one captured on edit,
     *  else the portfolio's reference account (create). */
    private Account resolveCashAccount()
    {
        if (cashInLieuAccount != null)
            return cashInLieuAccount;
        return portfolio != null ? portfolio.getReferenceAccount() : null;
    }

    @Override
    public void applyChanges()
    {
        // guard before any destructive delete/rebuild: a fractional
        // entitlement needs a cash-in-lieu leg on the portfolio's reference
        // account (§12); without one, bail out before touching existing state
        if (getFractionShares() > 0 && resolveCashAccount() == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        // ensure the target security is part of the client (create-new flow)
        if (targetSecurity != null && !client.getSecurities().contains(targetSecurity))
            client.addSecurity(targetSecurity);

        // edit: delete the old group first (Phase 5 makes this atomic + drops
        // the registry entry), then rebuild
        if (source != null)
        {
            var anyLeg = source.getLegs().get(0);
            @SuppressWarnings("unchecked")
            var owner = (TransactionOwner<Transaction>) anyLeg.owner();
            owner.deleteTransaction(anyLeg.transaction(), client);
            source = null;
        }

        var exDateTime = exDate.atStartOfDay();
        String sourceCurrency = sourceSecurity.getCurrencyCode();

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setExDate(exDate);
        entry.setBasisRatio(basisRatio);
        entry.setDistributionRatio(
                        new CorporateActionEntry.DistributionRatio(distributionNumerator, distributionDenominator));
        entry.setReferencePrice(spinOffPricePerShare);

        // leg 1: source basis reduction (shares 0, event currency)
        var sourceLeg = new PortfolioTransaction();
        sourceLeg.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        sourceLeg.setDateTime(exDateTime);
        sourceLeg.setSecurity(sourceSecurity);
        sourceLeg.setShares(0);
        sourceLeg.setCurrencyCode(sourceCurrency);
        sourceLeg.setAmount(marketValue(entitlement)); // §5 amount = MV at ex-date
        entry.addLeg(portfolio, sourceLeg, CorporateActionEntry.LegRole.SOURCE);

        // leg 2: target receipt (full entitlement; GROSS_VALUE forex if target
        // currency differs from the event currency, §6)
        var targetLeg = new PortfolioTransaction();
        targetLeg.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        targetLeg.setDateTime(exDateTime);
        targetLeg.setSecurity(targetSecurity);
        targetLeg.setShares(entitlement);
        targetLeg.setCurrencyCode(sourceCurrency);
        targetLeg.setAmount(marketValue(entitlement));
        addForexIfNeeded(targetLeg, sourceCurrency, targetSecurity.getCurrencyCode());
        entry.addLeg(portfolio, targetLeg, CorporateActionEntry.LegRole.TARGET);

        // legs 3-4: fractional disposal, only when a fraction remains (§12)
        if (getFractionShares() > 0)
        {
            Account cashAccount = resolveCashAccount();
            String cashCurrency = cashAccount.getCurrencyCode();

            var fractionSale = new PortfolioTransaction();
            fractionSale.setType(PortfolioTransaction.Type.SELL);
            fractionSale.setDateTime(exDateTime);
            fractionSale.setSecurity(targetSecurity);
            fractionSale.setShares(getFractionShares());
            fractionSale.setCurrencyCode(cashCurrency);
            fractionSale.setAmount(cashInLieuAmount);
            addForexIfNeeded(fractionSale, cashCurrency, targetSecurity.getCurrencyCode());
            entry.addLeg(portfolio, fractionSale, CorporateActionEntry.LegRole.FRACTION_SALE);

            var cashLeg = new AccountTransaction();
            cashLeg.setType(AccountTransaction.Type.SELL);
            cashLeg.setDateTime(exDateTime);
            cashLeg.setSecurity(targetSecurity);
            cashLeg.setCurrencyCode(cashCurrency);
            cashLeg.setAmount(cashInLieuAmount);
            entry.addLeg(cashAccount, cashLeg, CorporateActionEntry.LegRole.CASH_IN_LIEU);
        }

        entry.insert();
        client.addCorporateAction(entry);
    }

    /**
     * Market value of the given number of shares at the spinco reference
     * price/share (§5), rounded to the amount precision (2dp).
     */
    private long marketValue(long shares)
    {
        // shares (8dp) x reference price/share (quote precision) -> amount (2dp)
        return Math.round(shares * (spinOffPricePerShare / (double) Values.Quote.factor()) / Values.Share.factor()
                        * Values.Amount.factor());
    }

    /**
     * Adds a {@code GROSS_VALUE} forex unit (§6) when the leg's transaction
     * currency differs from the security's currency; leaves the leg
     * untouched (no unit) when they match, per
     * {@code CheckCurrenciesAction}'s invariant.
     */
    private void addForexIfNeeded(PortfolioTransaction leg, String transactionCurrency, String securityCurrency)
    {
        if (transactionCurrency.equals(securityCurrency))
            return;

        // GROSS_VALUE: amount in transaction currency, forex in security currency
        long amount = leg.getAmount();
        long forex = Math.round(amount / exchangeRate.doubleValue());
        leg.addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                        Money.of(transactionCurrency, amount), //
                        Money.of(securityCurrency, forex), //
                        exchangeRate));
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;
        this.cashInLieuAccount = null;
        setEntitlement(0);
        setBasisRatio(BigDecimal.ZERO);
        setSpinOffPricePerShare(0);
        setCashInLieuAmount(0);
        setTargetSecurity(null);
    }
}
