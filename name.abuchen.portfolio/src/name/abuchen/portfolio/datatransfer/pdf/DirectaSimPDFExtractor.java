package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * @formatter:off
 * @implNote Directa SIM is a pioneer in Italian online trading
 *           The currency is EUR --> €.
 *
 * @implSpec All security currencies are EUR --> €.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class DirectaSimPDFExtractor extends AbstractPDFExtractor
{
    public DirectaSimPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DIRECTA SIM");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Directa SIM";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("acquisto di");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Nota Informativa per l'ordine.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // per l'acquisto di: 29  VANGUARD FTSE ALL-WORLD UCITS ISIN IE00BK5BQT80
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^.*: [\\.,\\d]+[\\s]{1,}(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(CurrencyUnit.EUR);
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // per l'acquisto di: 29  VANGUARD FTSE ALL-WORLD UCITS ISIN IE00BK5BQT80
                        // @formatter:on
                        .section("shares") //
                        .match("^.*: (?<shares>[\\.,\\d]+).* ISIN [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        //  5.01.2024  14:02:36  Eseguito                           29             3.074,29             106,0100  09.01.2024
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(\\s)*(?<date>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})[\\s]{1,}(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})[\\s]{1,}Eseguito.*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        //                       Totale a Vs. Debito                               3.079,29
                        // @formatter:on
                        .section("amount") //
                        .match("^.*Totale a Vs\\. Debito[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        // @formatter:off
                        // Nota Informativa per l'ordine T1673620593440
                        // @formatter:on
                        .section("note") //
                        .match("^Nota Informativa per l.ordine (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ordine " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        //                       Commissioni:                                          5,00
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Commissioni:[\\s]{1,}(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.EUR);
                            processFeeEntries(t, v, type);
                        });
    }
}
