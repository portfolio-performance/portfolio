package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

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

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Movimientos del per.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "AMORTIZACION" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.*(?<type>AMORTIZACIONASASA) .*$") //
                        .assign((t, v) -> {
                            if ("AMORTIZACION".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
                        // or
                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
                        // or
                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
                        // or
                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
                        // @formatter:on
                        .section("name") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [A-Z0-9]{12}(?!AMORTIZACION)[A-Z]* (?<name>.*) [A-Z0-9]{3,6} .*$") //
                        //.match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) [A-Z0-9]{12}(?!AMORTIZACION)[A-Z]* (?<name>.*) [A-Z0-9]{3,6} (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<amount>[\\d\\.,\\d\\.,\\d]+) .*$") //
                        // .match("^[\\.'\\d\\s]+ [\\.,'\\d\\s]+
                        // (?<currency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.MXN);
                            t.setSecurity(getOrCreateSecurity(v));
                            })

                        // @formatter:off
                        // 04/01/22 06/01/22 SVD147529623COMPRA CETES 220203 6,080 9.95730000 2 5.51 60,540.38 0.00 -60,540.10
                        // or
                        // 06/01/22 06/01/22 SVD147779466AMORTIZACION CETES 220106 6,055 0 0.00 60,550.00 9.90
                        // or
                        // 06/01/22 06/01/22 SVD147779466ISR CETES 220106 0 3.70 0.00 6.20
                        // or
                        // 06/01/22 06/01/22 SVD148097667COMPSI BONDDIA PF2 3 1.57377100 0 0.00 4.72 0.00 1.48
                        // @formatter:on
                        .section("date", "shares", "amount") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) [A-Z0-9]{12}(?!AMORTIZACION)[A-Z]* (?<name>.*) [A-Z0-9]{3,6} (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<amount>[\\d\\.,\\d\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(CurrencyUnit.MXN);
                        })

                        

                        // .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        // addTaxesSectionsTransaction(pdfTransaction, type);
        // addFeesSectionsTransaction(pdfTransaction, type);
    }

}
