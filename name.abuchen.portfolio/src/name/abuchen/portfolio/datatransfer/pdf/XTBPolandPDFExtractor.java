package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class XTBPolandPDFExtractor extends AbstractPDFExtractor
{
    public XTBPolandPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("XTB S.A. z siedzibą w Warszawie");

        addBuySellTransaction();
        // addDividendeTransaction();
        // addInterestTransaction();
        // addDepositTransaction();
        // addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "XTB Poland";
    }

    private void addBuySellTransaction()
    {
        
        final var type = new DocumentType("Potwierdzenie zleceń wykonanych w dniu");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wykonane zlecenia kupna OMI.*$");
        firstRelevantLine = new Block("^(\\d+) (\\d+).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "tickerSymbol", "name", "orderNum",
                                                                        "date",
                                                                        "amount", "currency", "time", "totalcost") //
                                                        .match("^(\\d+) (?<orderNum>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            if (v.get("orderNum") != null)
                                                                t.setNote("Order Number: " + v.get("orderNum"));

                                                            if (v.get("totalcost") != null)
                                                            {
                                                                var fees = Money.of(v.get("currency"),
                                                                            asAmount(v.get("totalcost")));

                                                                checkAndSetFee(fees, t, type.getCurrentContext());
                                                            }
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),

                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "tickerSymbol", "name", "tradeId", "date",
                                                                        "amount", "currency", "time", "totalcost") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            var fees = Money.of(v.get("currency"),
                                                                            asAmount(v.get("totalcost")));

                                                            checkAndSetFee(fees, t, type.getCurrentContext());

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        })
                        )

                        .conclude(ExtractorUtils.fixGrossValueBuySell())
                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asShares(String value)
    {
        var newNumberFormat = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
        // Ensure the parser is strict, just in case (though not strictly
        // necessary here)
        newNumberFormat.setParseIntegerOnly(false);
        double newNumberDouble = 0;
        try
        {
            newNumberDouble = newNumberFormat.parse(value).doubleValue();
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
        value = trim(value).replaceAll("\\s", "");
        
        return Math.abs(Math.round(newNumberDouble * Values.Share.factor()));

    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

}

