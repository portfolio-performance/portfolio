package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDateTime;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.util.AdditionalLocales;

@SuppressWarnings("nls")
public class CetesDirectoPDFExtractor extends AbstractPDFExtractor
{

    public CetesDirectoPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CETES");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "CETES";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Movimientos del per");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block(
                        "^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z0-9]+.*$");
        //Block firstRelevantLine = new Block("^Saldo inicial+.*$", "^Saldo final+.*$");
        type.addBlock(firstRelevantLine);
        // firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })
                        
                        .section("type").optional() //
                        .match("^.*(?<type>AMORTIZACION) .*$") //
                        .assign((t, v) -> {
                            if ("AMORTIZACION".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })
                        
                        // @formatter:off
                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
                        // @formatter:on
                        .section("date", "name", "shares", "amount") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) [A-Z0-9]+(?<type>COMPRA|AMORTIZACION|ISR|COMPSI) (?<name>[A-Z]+) [\\dA-Z]+ (?<shares>[\\d,\\.]+) (?<price>[\\d,\\.]+) .* (?<amount>[\\d,\\.\\-]+)$")

                        // .section("date", "name", "shares", "debit", "credit")
                        // //
                        // .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}
                        // (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2})
                        // [A-Z0-9]+(?<type>COMPRA|AMORTIZACION|ISR|COMPSI)
                        // (?<name>[A-Z]+) [\\dA-Z]+ (?<shares>[\\d,\\.]+)
                        // (?<price>[\\d,\\.]+)( |.*) (?<debit>[\\d,\\.\\-]+)
                        // (?<credit>[\\d,\\.\\-]+) (?<amount>[\\d,\\.\\-]+)$")
                        // //
                         .assign((t, v) -> {
                            // v.put("currency", CurrencyUnit.MXN);
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(CurrencyUnit.MXN);
                            t.setAmount(asAmount(v.get("amount")));
                            // if ("AMORTIZACION".equals(v.get("type")))
                            // {
                            // t.setAmount(asAmount(v.get("credit"), "es",
                            // "MX"));
                            // }
                            // else
                            // t.setAmount(asAmount(v.get("debit"), "es",
                            // "MX"));
                         })

                        .wrap(BuySellEntryItem::new));

        // Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        // firstRelevantLine.set(pdfTransaction);
        //
        // pdfTransaction //
        //
        // .subject(() -> {
        // BuySellEntry portfolioTransaction = new BuySellEntry();
        // portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
        // return portfolioTransaction;
        // })
        //
        // // Is type --> "AMORTIZACION" change from BUY to SELL
        // /*
        // * .section("type").optional() //
        // * .match("^.*(?<type>AMORTIZACION) .*$") // .assign((t,
        // * v) -> { if ("AMORTIZACION".equals(v.get("type")))
        // * t.setType(PortfolioTransaction.Type.SELL); })
        // */
        //
//                        // @formatter:off
//                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
//                        // or
//                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
//                        // or
//                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
//                        // or
//                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
//                        // @formatter:on
        // // .section("name") //
        // // .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}
        // // [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z0-9]{12}[A-Z]*
        // // (?<name>.*) [A-Z0-9]{3,6} .*$") //
        // // .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}
        // // (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2})
        // // [A-Z0-9]{12}(?!AMORTIZACION)[A-Z]* (?<name>.*)
        // // [A-Z0-9]{3,6} (?<shares>[\\.,\\d]+) [\\.,\\d]+
        // // [\\.,\\d]+ [\\.,\\d]+ (?<amount>[\\d\\.,\\d\\.,\\d]+)
        // // .*$") //
        // // .match("^[\\.'\\d\\s]+ [\\.,'\\d\\s]+
        // // (?<currency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
        // /*
        // * .assign((t, v) -> { v.put("currency",
        // * CurrencyUnit.MXN);
        // * t.setSecurity(getOrCreateSecurity(v)); })
        // */
        //
//                        // @formatter:off
//                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
//                        // or
//                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
//                        // or
//                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
//                        // or
//                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
//                        // @formatter:on
        // .section("date", "type", "name", "shares", "amount") //
        // .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}
        // (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2})
        // [A-Z0-9]+(?<type>COMPRA|AMORTIZACION|ISR|COMPSI) (?<name>[A-Z]+)
        // [\\dA-Z]+ (?<shares>[\\d,\\.]+) (?<price>[\\d,\\.]+) .*
        // (?<amount>[\\d,\\.\\-]+)$") //
        // // .multipleTimes()
        // .assign((t, v) -> {
        // v.put("currency", CurrencyUnit.MXN);
        // t.setSecurity(getOrCreateSecurity(v));
        // t.setDate(asDate(v.get("date"), Locale.US));
        // t.setAmount(asAmount(v.get("amount")));
        // t.setShares(asShares(v.get("shares")));
        // t.setCurrencyCode(CurrencyUnit.MXN);
        // if ("AMORTIZACION".equals(v.get("type")))
        // t.setType(PortfolioTransaction.Type.SELL);
        // })
        //
        //
        //
        // // .conclude(ExtractorUtils.fixGrossValueBuySell())
        //
        // .wrap(BuySellEntryItem::new);

        // addTaxesSectionsTransaction(pdfTransaction, type);
        // addFeesSectionsTransaction(pdfTransaction, type);
        // firstRelevantLine.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

        // @formatter:off
                        // Kapitalertragsteuer EUR - 386,08
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag EUR - 21,23
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR - 11,11
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type));

    }

    @Override
    protected long asAmount(String value)
    {
        return asAmount(value, "es", "MX");
    }

    private LocalDateTime asDate(String date)
    {
        return asDate(date, AdditionalLocales.MEXICO);
    }

    @Override
    protected long asShares(String value)
    {
        return asShares(value, "es", "MX");
    }

}
