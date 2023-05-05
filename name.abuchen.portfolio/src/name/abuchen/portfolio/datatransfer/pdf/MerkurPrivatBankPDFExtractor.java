package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class MerkurPrivatBankPDFExtractor extends AbstractPDFExtractor
{

    public MerkurPrivatBankPDFExtractor(Client client)
    {
        super(client);
        
        // @formatter:off
        // Am Marktplatz 10 · 97762 Hammelburg
        // @formatter:on
        addBankIdentifier("Am Marktplatz 10 · 97762 Hammelburg"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "MERKUR PRIVATBANK KGaA"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        
        // Start from: Wertpapier Abrechnung Kauf
        Block firstRelevantLine = new Block("^Wertpapier Abrechnung Kauf.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
        
        pdfTransaction
                        // @formatter:off
                        // Type (Optional)
                        // Wertpapier Abrechnung Kauf
                        // @formatter:on
                        .section("type").optional().match("^Wertpapier Abrechnung (?<type>Kauf).*$").assign((t, v) -> {
                            if (v.get("type").equals("Kauf"))
                                t.setType(PortfolioTransaction.Type.BUY);
                        })
                        // @formatter:off
                        // Security identification:
                        // Stück 125,3258 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // REGISTERED SHARES 1C O.N.
                        // @formatter:on
                        .section("name", "ln2", "isin", "wkn")
                        .match("^St.ck [\\,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                        .match("^(?<ln2>.*)$").assign((t, v) -> {
                            // Assemble name from both lines
                            if (!v.get("ln2").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("ln2")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        // @formatter:off
                        // Shares of the transaction
                        // Stück 125,3258
                        // @formatter:on
                        .section("shares").match("^St.ck (?<shares>[\\,\\d]+) .*$").assign((t, v) -> 
                            t.setShares(asShares(v.get("shares")))
                        )
                        // @formatter:off
                        // Date and time
                        // Schlusstag/-Zeit 02.05.2023 09:34:40
                        // @formatter:on
                        .section("date", "time")
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total amount (With fees and taxes)
                        // Ausmachender Betrag 10.002,50- EUR
                        // @formatter:on
                        .section("amount", "totalcurrency")
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<totalcurrency>[\\w]{3})$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("totalcurrency")));
                        }).wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);

    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Fee section
                        // Provision 2,50- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));

    }
}
