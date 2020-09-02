package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class ConsorsbankPre2009PDFExtractor extends AbstractPDFExtractor
{
    public ConsorsbankPre2009PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction2001();
        addSellTransaction2001();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank (before 2009)"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction2001()
    {
        DocumentType type = new DocumentType("WERTPAPIERABRECHNUNG");
        this.addDocumentTyp(type);

        Block block = new Block("^ *(KAUF|Kauf|BEZUG|Bezug) +AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        block.set(pdfTransaction);
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // note: PDF/txt contains no ISIN
        pdfTransaction.section("wkn", "name", "currency") //
                        // .find(" WERTPAPIERABRECHNUNG") //
                        .match("^.+WKN: (?<wkn>[^ ]{6}) *$") //
                        .match("^ *(?<name>.*)$").match("^ *(KURSWERT|Kurswert|) *(?<currency>\\w{3}+) .*$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        // .find(" WERTPAPIERABRECHNUNG") //
                        .match("^ *ST *(?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date").match("^ *(KAUF|Kauf|BEZUG|Bezug) +AM (?<date>\\d+\\.\\d+\\.\\d{4}+)\\s+.*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), "05:00:00")))

                        .section("amount", "currency")
                        .match("^ *(KURSWERT|Kurswert|) *(?<currency>\\w{3}+) +(?<amount>[\\d.]+,\\d+).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction2001(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addFeesSectionsTransaction2001(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("currency", "stockfees").optional()
                        .match("^ *(GRUNDGEBUEHR|COURTAGE) +(?<currency>\\w{3}+) +(?<stockfees>[\\d.]+,\\d+).*$") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("stockfees"))))))

                        .section("currency", "brokerage").optional()
                        .match("^ *PROVISION +(?<currency>\\w{3}+) +(?<brokerage>[\\d.]+,\\d+).*$") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("brokerage"))))))

                        .section("currency", "expenses").optional()
                        .match("^ *EIG\\.SPESEN +(?<currency>\\w{3}+) +(?<expenses>[\\d.]+,\\d+).*$") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("expenses"))))));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction2001()
    {
        DocumentType type = new DocumentType("WERTPAPIERABRECHNUNG");
        this.addDocumentTyp(type);

        Block block = new Block("^ *(VERKAUF|Verkauf) +AM .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        block.set(pdfTransaction);
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        pdfTransaction.section("wkn", "name", "currency") //
                        .match("^.*WKN: (?<wkn>[^ ]{6}) *$") //
                        .match("^(?<name>.*)$") //
                        .match("^ *(KURSWERT|Kurswert|) *(?<currency>\\w{3}+) .*$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^ *ST *(?<shares>[\\d.]+(,\\d+)?).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date").match("^ *(VERKAUF|Verkauf) +AM (?<date>\\d+\\.\\d+\\.\\d{4}+)\\s+.*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), "05:00:00")))

                        .section("amount", "currency")
                        .match("^ *(KURSWERT|Kurswert|) *(?<currency>\\w{3}+) +(?<amount>[\\d.]+,\\d+).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction2001(pdfTransaction);
    }
}
