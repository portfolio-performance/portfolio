package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.util.TextUtil.trim;

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


                        .oneOf(
                                //
                                        // @formatter:off
                                        // Executed Buy Orders for Financial Instruments (OMI) 
                                        // 1 2004419557 FWIA.DE Invesco, UCITS, BATE 30.0000 03.09.2025 rynkowe 6.67500 200.25000 ETFs EUR 1.0000 0.00 0.00 0.00 
                                        // ACC, EUR 11:18:09
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name", "currency") //
                                                        .match("^(\\d+) (\\d+) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*?),(.*?) (?<currency>[A-Z]{3}) .*$") //
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
                                                        .match("^(\\d+) (\\d+) ([A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (.*?),(.*?)\\s+(?<shares>[.,\\d]+).*$") //
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
                                                        .match("^(\\d+) (\\d+) .*? ([.,\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .match("^[A-Z]{3}\\, [A-Z]{3} (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*")
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
                                                        .match("^(\\d+) (\\d+) .*? ([.,\\d]+) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\w+ (?<amount>[\\.,\\d]+) [\\.,\\d]+ .* (?<currency>[A-Z]{3}) .*$") //
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
                                                        .match("^(\\d+) (?<note>\\d+) ([A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?) .*$") //

                                                        .assign((t, v) -> {
                                                            if (v.get("note") != null)
                                                                t.setNote("Order Number: " + trim(v.get("note")));

                                                        }))
                        


                        .conclude(ExtractorUtils.fixGrossValueBuySell())
                        .wrap(BuySellEntryItem::new);
        addFeesSectionsTransaction(pdfTransaction, type);
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
                        .match("^.*(?<currency>[A-Z]{3}) ([\\.,\\d]+) (?<currencyConversionFee>[\\.,\\d]+) (?<fee>[\\.,\\d]+) (?<totalFees>[\\.,\\d]+).*$") //

        .assign((t, v) -> {
                            var fees = Money.of(v.get("currency"), asAmount(v.get("fee")));
                            var currencyConversionFee = Money.of(v.get("currency"),
                                            asAmount(v.get("currencyConversionFee")));

                            // Add currency conversion fee from fees
                            fees = fees.add(currencyConversionFee);

                            checkAndSetFee(fees, t, type.getCurrentContext());
                        });
    }


    @Override
    protected long asShares(String value)
    {
        String language = "en";
        String country = "US";

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
        String country = "US";

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }
}

