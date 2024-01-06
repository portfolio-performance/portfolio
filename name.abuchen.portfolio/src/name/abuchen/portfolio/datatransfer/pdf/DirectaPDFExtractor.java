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

        Block firstRelevantLine = new Block("^.*(acquisto di).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                        //
                        .section("shares", "name", "isin")
                        .match("^.*:\s*(?<shares>\\d+)\s+(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                        .assign((t, v) -> { 
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                        })

                        .section("date", "amount")
                        .match("^\\s(?<date>.*)(\\s{2}.*\\s{2}Eseguito\\s+([0-9]+))\\s+(?<amount>.\\S+)\\s+.*$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // // Auftrags-Nummer: 20220106123456789000000612345
                        // .section("note").optional().match("^(?<note>Auftrags-Nummer:
                        // [\\d]+)$")
                        // .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
                        
    }
}
