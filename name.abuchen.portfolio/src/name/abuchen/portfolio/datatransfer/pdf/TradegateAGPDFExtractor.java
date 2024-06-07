package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class TradegateAGPDFExtractor extends AbstractPDFExtractor
{
    public TradegateAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Tradegate AG");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Tradegate AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Orderart (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // ISIN IE00BM67HN09 Gültigkeit 31.07.2024
                        // WKN A113FG
                        // Wertpapier Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N.
                        // Ausführungskurs 43,2500 EUR
                        // @formatter:on
                        .section("isin", "wkn", "name", "currency") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Wertpapier (?<name>.*)$")
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück ausgeführt 3
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck .* (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Handelstag/-zeit 04.06.2024 11:04:53
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelstag\\/\\-zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 129,75 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 12345 Investcity Order-/Ref.nr. 9876543
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-\\/Ref\\.nr\\. .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit 43,2500 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit [\\.,\\d]+ [\\w]{3})$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Abgeführte Kapitalertragsteuer -0,22 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgef.hrte Kapitalertrags(s)?teuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Abgeführter Solidaritätszuschlag -0,01 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgef.hrter Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
