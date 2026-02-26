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
 * @implNote Freiburger Kantonalbank
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class FreiburgerKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public FreiburgerKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Freiburger Kantonalbank");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Freiburger Kantonalbank";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("B.rsenabrechnung \\- Emission");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Auftragsnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Wir haben für Sie am 07.02.2023 gezeichnet.
                        // 100 Anteile BCF / FKB (CH) Funds - BCF / FKB (CH)
                        // Active Dynamic (CHF) -AP- Kapitalisation
                        // Valor: 116358461
                        // ISIN: CH1163584613
                        // Börsenplatz: Swiss Fund Data AG
                        // Menge Ausführung Preis Wrg Betrag
                        // 100 00:00:00 101.42 CHF -10'142.00
                        // @formatter:on
                        .section("name", "nameContinued", "wkn", "isin", "currency") //
                        .find("Wir haben für Sie am.*") //
                        .match("^[\\.'\\d]+ Anteile (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .find("Menge Ausf.hrung Preis Wrg Betrag") //
                        .match("^.*[\\d]{2}:[\\d]{2}:[\\d]{2} [\\.'\\d]+ (?<currency>[A-Z]{3}) .*$") //
                        .assign((t, v) -> {
                            v.put("wkn", v.get("wkn").replace(",", ""));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Wir haben für Sie am 07.02.2023 gezeichnet.
                        // Menge Ausführung Preis Wrg Betrag
                        // 100 00:00:00 101.42 CHF -10'142.00
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Wir haben für Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .find("Menge Ausf.hrung Preis Wrg Betrag") //
                        .match("^[\\d]+ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Menge Ausführung Preis Wrg Betrag
                        // 100 00:00:00 101.42 CHF -10'142.00
                        // @formatter:on
                        .section("shares") //
                        .find("Menge Ausf.hrung Preis Wrg Betrag") //
                        .match("^(?<shares>[\\.'\\d]+) [\\d]{2}:[\\d]{2}:[\\d]{2} .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Netto CHF -10'142.00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto (?<currency>[A-Z]{3}) (\\-)?(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Auftragsnummer AUF1191526
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Auftragsnummer .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

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
