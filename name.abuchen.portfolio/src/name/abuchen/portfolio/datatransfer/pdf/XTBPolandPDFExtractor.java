package name.abuchen.portfolio.datatransfer.pdf;

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

                        /*
                         * .attributes("baseCurrency", "fxGross",
                         * "exchangeRate", "termCurrency") //
                         * .match("^Gesamtbetrag: (?<baseCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$"
                         * ) //
                         * .match("^.* Devisenkurs: (?<exchangeRate>[\\.,\\d]+)$"
                         * ) //
                         * .match("^.*Gesamtbetrag.*: (?<termCurrency>[\\w]{3}) [\\.,\\d]+$"
                         * ) // .assign((t, v) -> { ExtrExchangeRate rate =
                         * asExchangeRate(v);
                         * type.getCurrentContext().putType(rate); Money fxGross
                         * = Money.of(rate.getBaseCurrency(),
                         * asAmount(v.get("fxGross"))); Money gross =
                         * rate.convert(rate.getTermCurrency(), fxGross);
                         * checkAndSetGrossUnit(gross, fxGross, t,
                         * type.getCurrentContext());
                         */
                        .oneOf(
                                //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),

                                        //
                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name", "currency") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                        
                                        )
                        .oneOf(
                                        //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),

                                        //
                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                        )

                        
                        .oneOf(
                                        //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time")))),

                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time"))))
                                        )
                        
                        .oneOf( 
                                        //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {
                                                                             t.setAmount(asAmount(v.get("amount")));
                                                                             t.setCurrencyCode(v.get("currency"));
                                                                        }),

                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {
                                                                             t.setAmount(asAmount(v.get("amount")));
                                                                             t.setCurrencyCode(v.get("currency"));
                                                                        }))
                        .oneOf( 
                                        //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {
                                                            if (v.get("note") != null)
                                                                t.setNote("Order Number: " + v.get("note"));

                                                        }),

                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {
                
                                                            if (v.get("note") != null)
                                                                t.setNote("Order Number: " + v.get("note"));
                
                                                        }))
                        .oneOf(
                                        //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("totalcost") //
                                                        .match("^(\\d+) (?<note>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(?<system>.*?)\\s+(?<shares>[.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {

                                                            if (v.get("totalcost") != null)
                                                            {
                                                                var fees = Money.of(v.get("currency"),
                                                                                asAmount(v.get("totalcost")));

                                                                // checkAndSetFee(fees,
                                                                // t,
                                                                // type.getCurrentContext());
                                                            }
                                                        }),

                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("totalcost") //
                                                        .match("^(\\d+) (?<tradeId>\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) (?<shares>[\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) (?<totalcost>[\\.,'\\d]+).*$") //
                                                        .match("^([A-Z]{3})\\, ([A-Z]{3}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
                                                        .assign((t, v) -> {

                                                            if (v.get("totalcost") != null)
                                                            {
                                                                var fees = Money.of(v.get("currency"),
                                                                                asAmount(v.get("totalcost")));

                                                                // checkAndSetFee(fees,
                                                                // t,
                                                                // type.getCurrentContext());
                                                            }
                                                        }))


                        .conclude(ExtractorUtils.fixGrossValueBuySell())
                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asShares(String value)
    {
        String language = "en";
        String country = "GB";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);


    }

    @Override
    protected long asAmount(String value)
    {
        String language = "en";
        String country = "GB";

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

}

