package name.abuchen.portfolio.json;

import java.time.LocalDate;

import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.money.Values;

/**
 * JSON representation of an acquisition lot carried into a target fund.
 */
public class JFundTransferLot
{
    private LocalDate acquisitionDate;
    private double sourceShares;
    private double targetShares;
    private String acquisitionCurrency;
    private double acquisitionAmount;
    private String sourceTransaction;

    public LocalDate getAcquisitionDate()
    {
        return acquisitionDate;
    }

    public double getSourceShares()
    {
        return sourceShares;
    }

    public double getTargetShares()
    {
        return targetShares;
    }

    public String getAcquisitionCurrency()
    {
        return acquisitionCurrency;
    }

    public double getAcquisitionAmount()
    {
        return acquisitionAmount;
    }

    public String getSourceTransaction()
    {
        return sourceTransaction;
    }

    public static JFundTransferLot from(FundTransferEntry.CarriedLot lot)
    {
        JFundTransferLot value = new JFundTransferLot();
        value.acquisitionDate = lot.getAcquisitionDate();
        value.sourceShares = lot.getSourceShares() / Values.Share.divider();
        value.targetShares = lot.getTargetShares() / Values.Share.divider();
        value.acquisitionCurrency = lot.getAcquisitionValue().getCurrencyCode();
        value.acquisitionAmount = lot.getAcquisitionValue().getAmount() / Values.Amount.divider();
        value.sourceTransaction = lot.getSourceTransactionUUID();
        return value;
    }
}
