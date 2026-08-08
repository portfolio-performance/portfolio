package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Thurgauer Kantonalbank
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 digits/characters.
 */

@SuppressWarnings("nls")
public class ThurgauerKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public ThurgauerKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Thurgauer Kantonalbank");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Thurgauer Kantonalbank";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Abrechnung: Ihr Kauf");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Referenznummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // AUD 40'000.00 4.55% Obl Nestle Capital Corp 2025- Valoren-Nr. 142923838
                        // 13.03.30 ISIN AU3CB0319416
                        // Währung Betrag
                        // Kurs 98.594% AUD 39'437.60
                        // @formatter:on
                        .section("name", "wkn", "maturity", "isin", "currency") //
                        .match("^[A-Z]{3} [\\.'\\d]+ (?<name>.*) Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^(?<maturity>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Kurs [\\.,'\\d]+% (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // The security name is wrapped over two lines and continues
                            // with the maturity date directly after the trailing hyphen.
                            // @formatter:on
                            v.put("name", trim(v.get("name")) + trim(v.get("maturity")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // AUD 40'000.00 4.55% Obl Nestle Capital Corp 2025- Valoren-Nr. 142923838
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z]{3} (?<shares>[\\.'\\d]+) .* Valoren\\-Nr\\. [A-Z0-9]{5,9}$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Percentage quotation, workaround for bonds
                            // @formatter:on
                            var shares = asExchangeRate(v.get("shares"));
                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                        })

                        // @formatter:off
                        // Wir haben für Sie am 20. Juli 2026, 9:33:00 Uhr gekauft (Ausserbörslich Ausland):
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Wir haben f.r Sie am (?<date>[\\d]{1,2}\\. .* [\\d]{4}), (?<time>[\\d]{1,2}\\:[\\d]{2}\\:[\\d]{2}) Uhr gekauft.*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Zu Ihren Lasten Valuta 22.07.2026 CHF 23'185.11
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren Lasten Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kurswert AUD 40'085.60
                        // Wechselkurs AUD/CHF 0.5751
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^Kurswert [A-Z]{3} (?<fxGross>[\\.'\\d]+)$") //
                        .match("^Wechselkurs (?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Referenznummer 1234567890
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Referenznummer [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Marchzinsen (131 Tage) AUD 648.00
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>Marchzinsen \\([\\d]+ Tage\\)) (?<note3>[A-Z]{3}) (?<note2>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note2"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Umsatzabgabe AUD 60.13
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Umsatzabgabe (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage AUD 160.34
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Gebühren Gegenpartei AUD 8.85
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Geb.hren Gegenpartei (?<currency>[A-Z]{3}) (?<fee>[\\.'\\d]+)$") //
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
