package name.abuchen.portfolio.online.portfolioreport;

public class PRTransactionUnit
{
    String type;
    String amount;
    String currencyCode;
    String originalAmount;
    String originalCurrencyCode;
    String exchangeRate;

    public PRTransactionUnit(String type, long amount, String currencyCode)
    {
        this.type = type;
        this.amount = Double.toString(amount / 100.);
        this.currencyCode = currencyCode;
    }
}
