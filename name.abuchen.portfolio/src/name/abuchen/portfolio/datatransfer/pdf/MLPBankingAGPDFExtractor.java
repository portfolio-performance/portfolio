package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class MLPBankingAGPDFExtractor extends AbstractPDFExtractor
{
    public MLPBankingAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("MLP Banking AG"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MLP Banking AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung Kauf$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
                // INHABER-ANTEILE A O.N
                // Ausführungskurs 20,29 EUR
                .section("shares", "name", "isin", "wkn", "nameContinued", "currency")
                .match("St.ck (?<shares>[\\.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}) (\\((?<wkn>.*)\\).*)")
                .match("^(?<nameContinued>.*)$")
                .match("^Ausführungskurs [\\.,\\d]+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Schlusstag 14.01.2021
                .section("date")
                .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*")
                .assign((t, v) -> {
                    t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 100,01- EUR
                .section("amount", "currency")
                .match("^(Ausmachender Betrag) (?<amount>[\\.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // Ihr Ausgabeaufschlag betraegt:
                        // 0,00 EUR (0,000 Prozent)
                        .section("fee", "currency").optional()
                        .match("^(?<fee>[.,\\d]+) (?<currency>[\\w]{3}) \\([.,\\d]+ \\w+\\)$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
