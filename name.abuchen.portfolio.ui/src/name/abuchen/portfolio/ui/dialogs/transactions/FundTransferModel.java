package name.abuchen.portfolio.ui.dialogs.transactions;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.FundTransferLotBuilder;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;

public class FundTransferModel extends AbstractModel
{
    public enum Properties
    {
        sourceSecurity, targetSecurity, sourceSecurityCurrencyCode, targetSecurityCurrencyCode, sourcePortfolio, //
        targetPortfolio, date, time, sourceShares, targetShares, sourceAmount, targetAmount, note, carriedLots, //
        calculationStatus;
    }

    private final Client client;

    private FundTransferEntry source;

    private Security sourceSecurity;
    private Security targetSecurity;
    private Portfolio sourcePortfolio;
    private Portfolio targetPortfolio;
    private LocalDate date = LocalDate.now();
    private LocalTime time = PresetValues.getTime();
    private long sourceShares;
    private long targetShares;
    private long sourceAmount;
    private long targetAmount;
    private String note;
    private List<FundTransferEntry.CarriedLot> carriedLots = List.of();
    private IStatus calculationStatus = ValidationStatus.ok();
    private boolean carriedLotsManuallyEdited;
    private boolean targetPortfolioExplicit;
    private boolean targetAmountExplicit;

    public FundTransferModel(Client client)
    {
        this.client = client;
    }

    @Override
    public String getHeading()
    {
        return Messages.LabelFundTransfer;
    }

    @Override
    public LocalDate getDate()
    {
        return date;
    }

    @Override
    public void applyChanges()
    {
        IStatus status = calculateStatus();
        if (status.getSeverity() != IStatus.OK)
            throw new UnsupportedOperationException(status.getMessage());

        FundTransferEntry entry;

        if (source != null && sourcePortfolio.equals(source.getSourcePortfolio())
                        && targetPortfolio.equals(source.getTargetPortfolio()))
        {
            entry = source;
        }
        else
        {
            entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
            entry.insert();

            if (source != null)
            {
                source.getSourcePortfolio().deleteTransaction(source.getSourceTransaction(), client);
                entry.setSource(source.getSource());
                source = null;
            }
        }

        entry.setDate(LocalDateTime.of(date, time));
        entry.setSourceSecurity(sourceSecurity);
        entry.setTargetSecurity(targetSecurity);
        entry.setSourceShares(sourceShares);
        entry.setTargetShares(targetShares);
        entry.setSourceMonetaryAmount(Money.of(getSourceSecurityCurrencyCode(), sourceAmount));
        entry.setTargetMonetaryAmount(Money.of(getTargetSecurityCurrencyCode(), targetAmount));
        // Carried lots preserve the historical acquisition basis; the transfer
        // amounts above remain the current market value at conversion time.
        entry.setCarriedLots(new ArrayList<>(carriedLots));
        entry.setNote(note);
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;

        targetAmountExplicit = false;
        targetPortfolioExplicit = false;
        carriedLotsManuallyEdited = false;
        setSourceShares(0);
        setTargetShares(0);
        setSourceAmount(0);
        setNote(null);
        setTime(PresetValues.getTime());
    }

    public void setSource(FundTransferEntry entry)
    {
        this.source = entry;
        fillFromSource(entry);
        recalculate();
    }

    public void presetFromSource(FundTransferEntry entry)
    {
        this.source = null;
        fillFromSource(entry);
        recalculate();
    }

    private void fillFromSource(FundTransferEntry entry)
    {
        this.sourcePortfolio = entry.getSourcePortfolio();
        this.targetPortfolio = entry.getTargetPortfolio();
        this.sourceSecurity = entry.getSourceTransaction().getSecurity();
        this.targetSecurity = entry.getTargetTransaction().getSecurity();
        this.date = entry.getSourceTransaction().getDateTime().toLocalDate();
        this.time = entry.getSourceTransaction().getDateTime().toLocalTime();
        this.sourceShares = entry.getSourceTransaction().getShares();
        this.targetShares = entry.getTargetTransaction().getShares();
        this.sourceAmount = entry.getSourceTransaction().getAmount();
        this.targetAmount = entry.getTargetTransaction().getAmount();
        this.note = entry.getNote();
        this.carriedLots = new ArrayList<>(entry.getCarriedLots());
        this.carriedLotsManuallyEdited = true;
        this.targetAmountExplicit = true;
        this.targetPortfolioExplicit = true;
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    private void recalculate()
    {
        IStatus oldStatus = this.calculationStatus;
        List<FundTransferEntry.CarriedLot> oldLots = this.carriedLots;

        this.calculationStatus = calculateStatus();

        firePropertyChange(Properties.carriedLots.name(), oldLots, this.carriedLots);
        firePropertyChange(Properties.calculationStatus.name(), oldStatus, this.calculationStatus);
    }

    private IStatus calculateStatus()
    {
        if (!carriedLotsManuallyEdited)
            carriedLots = List.of();

        if (sourceSecurity == null || targetSecurity == null)
            return ValidationStatus.error(Messages.MsgMissingSecurity);

        if (sourceSecurity.equals(targetSecurity))
            return ValidationStatus.error(Messages.MsgFundTransferSecuritiesMustDiffer);

        if (sourcePortfolio == null)
            return ValidationStatus.error(Messages.MsgPortfolioFromMissing);

        if (targetPortfolio == null)
            return ValidationStatus.error(Messages.MsgPortfolioToMissing);

        if (sourceShares == 0L || targetShares == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares));

        if (sourceAmount == 0L || targetAmount == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnAmount));

        // Broker statements may split the destination-fund operation with
        // their own rounding. Preserve explicit lot edits and validate only
        // the invariants that must still match the transfer totals.
        if (carriedLotsManuallyEdited)
            return validateCarriedLots();

        try
        {
            carriedLots = FundTransferLotBuilder.build(client, sourcePortfolio, sourceSecurity,
                            LocalDateTime.of(date, time), sourceShares, targetShares, getSourceSecurityCurrencyCode(),
                            source);
        }
        catch (IllegalArgumentException e)
        {
            return ValidationStatus.error(Messages.MsgFundTransferNotEnoughShares);
        }

        return ValidationStatus.ok();
    }

    private IStatus validateCarriedLots()
    {
        if (carriedLots.isEmpty())
            return ValidationStatus.error(Messages.MsgFundTransferCarriedLotsMissing);

        long carriedSourceShares = 0;
        long carriedTargetShares = 0;

        for (FundTransferEntry.CarriedLot lot : carriedLots)
        {
            Money acquisitionValue = lot.getAcquisitionValue();
            if (lot.getSourceShares() <= 0 || lot.getTargetShares() <= 0 || acquisitionValue == null
                            || acquisitionValue.getAmount() <= 0)
                return ValidationStatus.error(Messages.MsgFundTransferCarriedLotValuesMustBePositive);

            if (!getSourceSecurityCurrencyCode().equals(acquisitionValue.getCurrencyCode()))
                return ValidationStatus.error(Messages.MsgFundTransferCarriedLotCurrencyMismatch);

            carriedSourceShares += lot.getSourceShares();
            carriedTargetShares += lot.getTargetShares();
        }

        if (carriedSourceShares != sourceShares)
            return ValidationStatus.error(Messages.MsgFundTransferCarriedLotSourceSharesMismatch);

        if (carriedTargetShares != targetShares)
            return ValidationStatus.error(Messages.MsgFundTransferCarriedLotTargetSharesMismatch);

        return ValidationStatus.ok();
    }

    public Security getSourceSecurity()
    {
        return sourceSecurity;
    }

    public void setSourceSecurity(Security security)
    {
        String oldCurrencyCode = getSourceSecurityCurrencyCode();
        firePropertyChange(Properties.sourceSecurity.name(), this.sourceSecurity, this.sourceSecurity = security);
        firePropertyChange(Properties.sourceSecurityCurrencyCode.name(), oldCurrencyCode,
                        getSourceSecurityCurrencyCode());
        recalculate();
    }

    public Security getTargetSecurity()
    {
        return targetSecurity;
    }

    public void setTargetSecurity(Security security)
    {
        String oldCurrencyCode = getTargetSecurityCurrencyCode();
        firePropertyChange(Properties.targetSecurity.name(), this.targetSecurity, this.targetSecurity = security);
        firePropertyChange(Properties.targetSecurityCurrencyCode.name(), oldCurrencyCode,
                        getTargetSecurityCurrencyCode());
        recalculate();
    }

    public Portfolio getSourcePortfolio()
    {
        return sourcePortfolio;
    }

    public void setSourcePortfolio(Portfolio portfolio)
    {
        firePropertyChange(Properties.sourcePortfolio.name(), this.sourcePortfolio, this.sourcePortfolio = portfolio);
        if (!targetPortfolioExplicit)
            firePropertyChange(Properties.targetPortfolio.name(), this.targetPortfolio, this.targetPortfolio = portfolio);
        recalculate();
    }

    public Portfolio getTargetPortfolio()
    {
        return targetPortfolio;
    }

    public void setTargetPortfolio(Portfolio portfolio)
    {
        targetPortfolioExplicit = true;
        firePropertyChange(Properties.targetPortfolio.name(), this.targetPortfolio, this.targetPortfolio = portfolio);
        recalculate();
    }

    public LocalTime getTime()
    {
        return time;
    }

    public void setDate(LocalDate date)
    {
        firePropertyChange(Properties.date.name(), this.date, this.date = date);
        recalculate();
    }

    public void setTime(LocalTime time)
    {
        firePropertyChange(Properties.time.name(), this.time, this.time = time);
        recalculate();
    }

    public long getSourceShares()
    {
        return sourceShares;
    }

    public void setSourceShares(long shares)
    {
        firePropertyChange(Properties.sourceShares.name(), this.sourceShares, this.sourceShares = shares);
        recalculate();
    }

    public long getTargetShares()
    {
        return targetShares;
    }

    public void setTargetShares(long shares)
    {
        firePropertyChange(Properties.targetShares.name(), this.targetShares, this.targetShares = shares);
        recalculate();
    }

    public long getSourceAmount()
    {
        return sourceAmount;
    }

    public void setSourceAmount(long amount)
    {
        firePropertyChange(Properties.sourceAmount.name(), this.sourceAmount, this.sourceAmount = amount);
        // The target market value normally starts as the source redemption
        // value; once the user edits it, keep that explicit value.
        if (!targetAmountExplicit)
            firePropertyChange(Properties.targetAmount.name(), this.targetAmount, this.targetAmount = amount);
        recalculate();
    }

    public long getTargetAmount()
    {
        return targetAmount;
    }

    public void setTargetAmount(long amount)
    {
        targetAmountExplicit = true;
        firePropertyChange(Properties.targetAmount.name(), this.targetAmount, this.targetAmount = amount);
        recalculate();
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        firePropertyChange(Properties.note.name(), this.note, this.note = note);
    }

    public List<FundTransferEntry.CarriedLot> getCarriedLots()
    {
        return carriedLots;
    }

    public void setCarriedLotSourceShares(FundTransferEntry.CarriedLot lot, long shares)
    {
        updateCarriedLot(lot, l -> l.setSourceShares(shares));
    }

    public void setCarriedLotTargetShares(FundTransferEntry.CarriedLot lot, long shares)
    {
        updateCarriedLot(lot, l -> l.setTargetShares(shares));
    }

    public void setCarriedLotAcquisitionAmount(FundTransferEntry.CarriedLot lot, long amount)
    {
        updateCarriedLot(lot, l -> {
            Money currentValue = l.getAcquisitionValue();
            String currencyCode = currentValue != null ? currentValue.getCurrencyCode() : getSourceSecurityCurrencyCode();
            l.setAcquisitionValue(Money.of(currencyCode, amount));
        });
    }

    private void updateCarriedLot(FundTransferEntry.CarriedLot lot, Consumer<FundTransferEntry.CarriedLot> updater)
    {
        if (!carriedLots.contains(lot))
            throw new IllegalArgumentException("carried lot does not belong to this fund transfer"); //$NON-NLS-1$

        IStatus oldStatus = this.calculationStatus;

        updater.accept(lot);
        carriedLotsManuallyEdited = true;
        this.calculationStatus = calculateStatus();

        firePropertyChange(Properties.carriedLots.name(), null, this.carriedLots);
        firePropertyChange(Properties.calculationStatus.name(), oldStatus, this.calculationStatus);
    }

    public String getSourceSecurityCurrencyCode()
    {
        return sourceSecurity != null ? sourceSecurity.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getTargetSecurityCurrencyCode()
    {
        return targetSecurity != null ? targetSecurity.getCurrencyCode() : ""; //$NON-NLS-1$
    }
}
