package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Importer for "Directa" purchases.
 */
@SuppressWarnings("nls")
public class DirectaPDFExtractor extends AbstractPDFExtractor
{
    public DirectaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DIRECTA SIM");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Directa";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("acquisto di");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Nota Informativa per l'ordine.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                        //
                        // @formatter:off
                        // Nota Informativa per l'ordine T1673620593440
                        // @formatter:on
                        .section("orderNo") //
                        .match("^Nota Informativa per l'ordine (?<orderNo>[A-Z0-9]+)$")
                        .assign((t, v) -> t.setNote("Ordine " + v.get("orderNo")))

                        // @formatter:off
                        // per l'acquisto di: 29  VANGUARD FTSE ALL-WORLD UCITS ISIN IE00BK5BQT80
                        // @formatter:on
                        .section("shares", "name", "isin")
                        .match("^.*:\\s*(?<shares>\\d+)\\s+(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                        })

                        // @formatter:off
                        //  5.01.2024  14:02:36  Eseguito                           29             3.074,29             106,0100  09.01.2024
                        // @formatter:on
                        .section("date", "time") //
                        .match("^\\s?(?<date>[0-9.]+)\\s{2}(?<time>[0-9:]+)\\s{2}Eseguito.*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // per l'acquisto di: 29  VANGUARD FTSE ALL-WORLD UCITS ISIN IE00BK5BQT80
                        // @formatter:on
                        .section("amount") //
                        .match("^.*Totale a Vs. Debito\\s*(?<amount>[0-9.,]+)$")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        // @formatter:off
                        //                       Commissioni:                                          5,00
                        // @formatter:on
                        .section("fee") //
                        .match("^.*Commissioni:\s*(?<fee>[0-9,]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        .wrap(BuySellEntryItem::new);

    }
}
