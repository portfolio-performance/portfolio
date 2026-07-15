package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * St.Galler Kantonalbank AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 digits/characters.
 */

@SuppressWarnings("nls")
public class StGallerKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public StGallerKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("St.Galler Kantonalbank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "St.Galler Kantonalbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Abrechnung: Ihr (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Referenznummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Abrechnung: Ihr (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 10'000 N-Akt TUI AG Aus Konversion Valoren-Nr. 125205291
                        // ISIN DE000TUAG505
                        // Währung Betrag
                        // Kurs EUR 6.822613 EUR 68'226.13
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^[\\.'\\d]+ (?<name>.*) Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+ [A-Z]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 10'000 N-Akt TUI AG Aus Konversion Valoren-Nr. 125205291
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.'\\d]+) .* Valoren\\-Nr\\. [A-Z0-9]{5,9}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Menge Ausführungszeitpunkt Börsenplatz Währung Kurs
                                        // 10'000 20.02.2025 09:00:13 Xetra EUR 6.702
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find("Menge Ausf.hrungszeitpunkt B.rsenplatz W.hrung Kurs") //
                                                        .match("^[\\.'\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Wir haben für Sie am 24. Februar 2025 gekauft (Details siehe Folgeseite):
                                        // Wir haben für Sie am 20. Februar 2025 verkauft (Details siehe Folgeseite):
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Wir haben f.r Sie am (?<date>[\\d]{1,2}\\. .* [\\d]{4}) (gekauft|verkauft).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 26.02.2025 EUR 69'297.29
                        // Zu Ihren Gunsten Valuta 24.02.2025 EUR 65'967.79
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren (Lasten|Gunsten) Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Referenznummer 1603933135 DEUTSCHLAND
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Referenznummer [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Baraussch.ttung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Referenznummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // 10'000 N-Akt TUI AG Aus Konversion
                        // Namenaktien Valoren-Nr.: 125205291, ISIN: DE000TUAG505
                        // Ausschüttung: EUR 0.10
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .find("Ihr Depotbestand per Ex\\-Datum .*") //
                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                        .match("^.* Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Aussch.ttung: (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 10'000 N-Akt TUI AG Aus Konversion
                        // @formatter:on
                        .section("shares") //
                        .find("Ihr Depotbestand per Ex\\-Datum .*") //
                        .match("^(?<shares>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ihr Depotbestand per Ex-Datum 11. Februar 2026:
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ihr Depotbestand per Ex\\-Datum (?<exDate>[\\d]{1,2}\\. .* [\\d]{4}):$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 13.02.2026 EUR 736.25
                        // @formatter:on
                        .section("date") //
                        .match("^Zu Ihren Gunsten Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [A-Z]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 13.02.2026 EUR 736.25
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren Gunsten Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Referenznummer 1746312001 DEUTSCHLAND
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Referenznummer [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Stempelsteuer(CHF -96.20) EUR 102.34
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Eidg\\. Stempelsteuer ?(\\(.*\\))? (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 26.375% EUR 263.75
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer [\\.,'\\d]+% (?<currency>[A-Z]{3}) (\\-)?(?<withHoldingTax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage EUR 955.17
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Courtage EUR 13.65
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Courtage (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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
