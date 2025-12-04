package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;

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

        // No data available for these sections yet
        // addDividendeTransaction();
        // addInterestTransaction();
        // addDepositTransaction();
        addAccountStatementTransaction();
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


                        .oneOf(
                                //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name") //
                                                        .match("^(\\d+) (\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(?<name>.*?),(.*?)\\s+([.,'\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) ([\\.,'\\d]+) .* ([A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //

                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),

                                        //
                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name") //
                                                        .match("^(\\d+) (\\d+) (?<tickerSymbol>[A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (?<name>.*?) ([\\.,'\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) Prawa \\w+ ([A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //

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
                                                        .match("^(\\d+) (\\d+) ([A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(.*?),(.*?)\\s+(?<shares>[.,'\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //

                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),

                                        //
                                        // @formatter:off 
                                        // Executed Buy Orders for Fractional Rights 
                                        //1 2004419557 FWIA.DE Invesco, UCITS, XTB 0.2636 03.09.2025 rynkowe 6.67600 1.75979 Prawa ułamkowe EUR 1.0000 0.00 0.00 0.00
                                        //ACC, EUR 11:18:08 dot. ETF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(\\d+) (\\d+) ([A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (.*?) (?<shares>[\\.,'\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) Prawa \\w+ (?<currency>[A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //

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
                                                        .match("^(\\d+) (\\d+) ([A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(.*?),(.*?)\\s+([.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) ([\\.,'\\d]+) .* ([A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //
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
                                                        .match("^(\\d+) (\\d+) ([A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?) (.*?) ([\\.,'\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) Prawa \\w+ ([A-Z]{3}) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+) ([\\.,'\\d]+).*$") //
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

                                                        .assign((t, v) -> {
                
                                                            if (v.get("note") != null)
                                                                t.setNote("Order Number: " + v.get("note"));
                
                                                        }))
                        


                        .conclude(ExtractorUtils.fixGrossValueBuySell())
                        .wrap(BuySellEntryItem::new);
        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }



    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // not yet implemented. No examples
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
    
        transaction
        //
        // @formatter:off
        // Executed Buy Orders for Financial Instruments (OMI) 
        //                                                              
        // EUR - Share Currency
        // 1.0 - Exchange rate to account Currency
        // 0.00 - Currency Exchange Costs
        // 0.00 - Commission Rate
        // 0.00 - Total Costs
        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
        // ACC, EUR 11:18:09
        // @formatter:on
                        .section("currencyConversionFee", "fee", "totalFees", "currency") //
                        .match("^(\\d+) (\\d+) ([A-Z0-9\\\\._-]{1,10}(?:\\\\.[A-Z]{1,4})?)\\s+(.*?),(.*?)\\s+([.,'\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ ([\\.,'\\d]+) ([\\.,'\\d]+) .* (?<currency>[A-Z]{3}) ([\\.,'\\d]+) (?<currencyConversionFee>[\\.,'\\d]+) (?<fee>[\\.,'\\d]+) (?<totalFees>[\\.,'\\d]+).*$") //

        .assign((t, v) -> {
                            var fees = Money.of(v.get("currency"), asAmount(v.get("fee")));
                            var currencyConversionFee = Money.of(v.get("currency"),
                                            asAmount(v.get("currencyConversionFee")));

                            // Add currency conversion fee from fees
                            fees = fees.add(currencyConversionFee);

                            checkAndSetFee(fees, t, type.getCurrentContext());
                        });
    }

    private void addAccountStatementTransaction()
    {
        // Not implemented yet. No examples
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

