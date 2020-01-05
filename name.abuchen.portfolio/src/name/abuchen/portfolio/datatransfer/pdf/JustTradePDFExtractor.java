package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class JustTradePDFExtractor extends AbstractPDFExtractor
{
    public JustTradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE"); //$NON-NLS-1$

        addBuyTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Sutor Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Stück .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
        pdfTransaction

                        .section("shares", "name", "isin", "wkn") //
                        .match("^Stück (?<shares>[\\d.,]+) (?<name>.*) (?<isin>\\S*) \\((?<wkn>\\S*)\\)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date") //
                        .match("Schlusstag\\/\\-Zeit (?<date>.{10}) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency")
                        .match("Kurswert (?<amount>[0-9.]+(\\,[0-9]{2}))- (?<currency>[A-Z]{3})") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    public String getLabel()
    {
        return "Sutor justTRADE"; //$NON-NLS-1$
    }
}
