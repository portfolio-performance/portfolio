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
        
        // @formatter:off
        // Wertpapier Abrechnung Kauf 
        // @formatter:on
        Block firstRelevantLine = new Block("^Wertpapier Abrechnung Kauf.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
        
        pdfTransaction
                        // @formatter:off
                        // Stück 125,3258 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // REGISTERED SHARES 1C O.N.          
                        // @formatter:on
                        .section("name", "name1", "isin", "wkn")
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                        .match("^(?<name1>.*)$")
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        
                        // @formatter:off
                        // Stück 125,3258 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                        // @formatter:on
                        .section("shares")
                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        
                        // @formatter:off
                        // Schlusstag/-Zeit 02.05.2023 09:34:40 Auftraggeber Max Mustermann
                        // @formatter:on
                        .section("date", "time")
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 10.002,50- EUR
                        // @formatter:on
                        .section("amount", "currency")
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        // @formatter:off
                        // Ausf. erfolgte über Quotrix
                        // Ihr ETF-Sparplan Nr. 1
                        // @formatter:on
                        .section("note").optional()
                        .match("^Ausf. erfolgte über .*$")
                        .match("^(?<note>.*)$")
                        .match("^Für das Gesch.ft wurde keine Anlageberatung erbracht.")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                        
                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);

    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Provision 2,50- EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));

    }
}
