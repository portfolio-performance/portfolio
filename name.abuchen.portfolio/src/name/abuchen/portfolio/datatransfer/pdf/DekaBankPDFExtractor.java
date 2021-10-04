package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class DekaBankPDFExtractor extends AbstractPDFExtractor
{
    public DekaBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DekaBank"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DekaBank Deutsche Girozentrale"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("LASTSCHRIFTEINZUG*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^LASTSCHRIFTEINZUG .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Bezeichnung: Deka-UmweltInvest TF
                // ISIN: DE000DK0ECT0 Unterdepot: 00 Auftragsnummer: 8103 1017
                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 187,770000 Anteilumsatz: 21,303
                .section("name", "isin", "currency", "shares").match("^Bezeichnung: (?<name>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12}) .*$")
                .match("(^|^=)Abrechnungsbetrag [\\w]{3} [.,\\d]+ [\\w]{3} [.,\\d]+ (?<currency>[\\w]{3}) [.,\\d]+ .*: (?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Verwahrart: GiroSammel Abrechnungstag: 18.05.2021
                .section("date")
                .match("^.* Abrechnungstag: (?<date>[\\d]+.[\\d]+.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR
                .section("currency", "amount")
                .match("(^|^=)Abrechnungsbetrag (?<currency>[\\w]{3}) (?<amount>[.,\\d]+) [\\w]{3} [.,\\d]+ [\\w]{3} [.,\\d]+ .*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);
    }
}
