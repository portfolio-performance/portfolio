package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class FinecoBankPDFExtractor extends AbstractPDFExtractor
{
    public FinecoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FinecoBank S.p.A.");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "FinecoBank S.p.A.";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Operazione[\\s]{1,}(ACQUISTO|VENDITA) TITOLI");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Descrizione titolo: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "VENDITA" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Operazione[\\s]{1,}(?<type>(ACQUISTO|VENDITA)) TITOLI.*$") //
                        .assign((t, v) -> {
                            if ("VENDITA".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Descrizione titolo: ISHS CR STX EUR-AC
                        // Simbolo: CSSX5E
                        // Isin: IE00B53L3W79
                        // Divisa: EUR Totale Commissioni e Spese: 0,00 EUR
                        // @formatter:on
                        .section("name", "tickerSymbol", "isin", "currency") //
                        .match("^Descrizione titolo: (?<name>.*)$") //
                        .match("^Simbolo: (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                        .match("^Isin: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Divisa: (?<currency>[A-Z]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Data/ora esecuzione: 22/05/2026 10:53:59 Disaggio di emissione: 0,00000 EUR
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Data\\/ora esecuzione: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Quantità: 4,000 Commissioni: 0,00 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantit.: (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Totale: 932,60 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Totale: (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Mercato: EURONEXT MILAN Bolli: 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.* Bolli: (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Quantità: 18,000 Commissioni: 19,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.* Commissioni: (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Prezzo eseguito: 250,00000 Spese: 0,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*(?<!Commissioni e) Spese: (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
