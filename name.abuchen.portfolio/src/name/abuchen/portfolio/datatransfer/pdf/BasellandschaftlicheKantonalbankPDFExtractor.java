package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Basellandschaftliche Kantonalbank (BLKB) / Radicant Bank AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class BasellandschaftlicheKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public BasellandschaftlicheKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("radicant bank ag");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Basellandschaftliche Kantonalbank (BLKB) / Radicant Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung \\-( Zeichnung| R.cknahme)? (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Auftragsnummer.*$");
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
                        .match("^B.rsenabrechnung \\-( Zeichnung| R.cknahme)? (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wir haben für Sie am 11.01.2024 gezeichnet.
                        // 10.467 Anteile -(CHF) M-
                        // radicant SDG Impact Solutions Fund - Global
                        // Sustainable Bonds
                        // Valor: 121220071
                        // ISIN: LI1212200714
                        // Total Kurswert CHF -105.51
                        // @formatter:on
                        .section("name", "nameContinued", "wkn", "isin", "currency") //
                        .find("Wir haben für Sie am .*") //
                        .match("^[\\.'\\d]+ .*$") //
                        .match("^(?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Total Kurswert (?<currency>[\\w]{3}) (\\-)?[\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Menge/Nominal Preis
                        // 10.467 10.08
                        //
                        // Menge/Nominal Eff. Börsenplatz Preis
                        // 8 11.311
                        // @formatter:on
                        .section("shares") //
                        .find("Menge\\/Nominal.*") //
                        .match("^(?<shares>[\\.'\\d]+) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wir haben für Sie am 11.01.2024 gezeichnet.
                        // @formatter:on
                        .section("date") //
                        .match("^Wir haben für Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto CHF -105.51
                        // Netto CHF 90.49
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftragsnummer AUF240111-
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }
}
