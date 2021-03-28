package name.abuchen.portfolio.datatransfer.pdf;


import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class KeytradeBankPDFExtractor extends AbstractPDFExtractor
{
    public KeytradeBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Keytrade Bank"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Keytrade Bank"; //$NON-NLS-1$
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

        Block firstRelevantLine = new Block("Kauf .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction

                        // Kauf 168 LYXOR CORE WORLD (LU1781541179) für 11,7824 EUR
                        .section("isin", "name", "shares")
                        .match("^Kauf (?<shares>[\\d.,]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .*")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // Ausführungsdatum und -zeit : 15/03/2021 12:31:50 CET
                        .section("date", "time")
                        .match("^Ausführungsdatum und -zeit : (?<date>\\d+/\\d+/\\d{4}) (?<time>\\d+:\\d+:\\d+) .*")
                        .assign((t, v) -> {
                            if (v.get("time") != null)
                                t.setDate(asDate(v.get("date").replaceAll("/", "."), v.get("time")));
                            else
                                t.setDate(asDate(v.get("date").replaceAll("/", ".")));
                        })

                        // Lastschrift 1.994,39 EUR Valutadatum 17/03/2021
                        .section("currency", "amount")
                        .match("^Lastschrift (?<amount>[.,\\d]+ (?<currency>\\w{3})) .*")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // Transaktionskosten 14,95 EUR
                        .section("fee", "currency").optional()
                        .match("^(Transaktionskosten) (?<fee>[.,\\d]+) (?<currency>\\w{3})")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new);
    }
}
