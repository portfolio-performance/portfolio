package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote St.Galler Kantonalbank AG
 *
 * @implSpec The Valoren-Nr. is the WKN and, in all observed documents, is 5 to 9 digits/characters.
 *
 *           The trade date is preferably taken from the per-lot execution table on the second page
 *           ("Menge Ausführungszeitpunkt Börsenplatz Währung Kurs"), which provides the most precise
 *           timestamp (including seconds). For collective orders ("Sammelaufträge" - an order bundled
 *           together with orders from other clients), this table is not always present on the second
 *           page. In that case, the extractor falls back to "Zeitpunkt letzte Ausführung" on the first
 *           page (minute precision) and, if that is missing too, to the date in "Wir haben für Sie am
 *           ... (gekauft|verkauft)" on the first page, which is always present but only has day
 *           precision. The trade date must never be required from the second page alone, since it is
 *           not always available for collective orders.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class StGallerKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public StGallerKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("St.Galler Kantonalbank AG");
        addBankIdentifier("St. Galler Kantonalbank");

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
                        .match("^Abrechnung: Ihr (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben für Sie am 16. Juli 2026 gekauft (Details siehe Folgeseite):
                                        // 500 N-Akt Georg Fischer AG CHF 0.05 nom Valoren-Nr. 116915100
                                        // ISIN CH1169151003
                                        // Währung Betrag
                                        // Kurs CHF 45.52164 CHF 22'760.82
                                        //
                                        // Short security names have the Valoren-Nr. inline on the same line as
                                        // shares/name instead of on its own line.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "wkn", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am .* (gekauft|verkauft).*") //
                                                        .match("^(?<shares>[\\.'\\d]+) (?<name>.*?) Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^.*ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^W.hrung Betrag$") //
                                                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+( [A-Z]{3} [\\.'\\d]+)?$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // Wir haben für Sie am 28. Mai 2026 verkauft (Details siehe Folgeseite):
                                        // 5'500 Ant UBS (Irl) ETF plc - UBS MSCI World
                                        // Valoren-Nr. 110951056
                                        // Small Cap SR UCITS ETF Accum Shs USD
                                        // ISIN IE00BKSCBX74
                                        // Währung Betrag
                                        // Kurs EUR 10.81905
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am .* (gekauft|verkauft).*") //
                                                        .match("^(?<shares>[\\.'\\d]+) (?<name>.*)$") //
                                                        .match("^Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^.*ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^W.hrung Betrag$") //
                                                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+( [A-Z]{3} [\\.'\\d]+)?$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // Wir haben für Sie am 28. Mai 2026 gekauft (Details siehe Folgeseite):
                                        // 500 N-Akt Nestle AG CHF 0.1 nom
                                        // Valoren-Nr. 3886335
                                        // ISIN CH0038863350
                                        // Währung Betrag
                                        // Kurs CHF 79.39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "wkn", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am .* (gekauft|verkauft).*") //
                                                        .match("^(?<shares>[\\.'\\d]+) (?<name>.*)$") //
                                                        .match("^Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^.*ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^W.hrung Betrag$") //
                                                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+( [A-Z]{3} [\\.'\\d]+)?$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Menge Ausführungszeitpunkt Börsenplatz Währung Kurs
                                        // 500 28.05.2026 14:04:36 SIX Swiss Exchange - EBBO Book CHF 79.39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find("Menge Ausf.hrungszeitpunkt B.rsenplatz W.hrung Kurs") //
                                                        .match("^[\\.'\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Diese Transaktion war Teil eines Sammelauftrages für mehrere Kundenbeziehungen.
                                        // In this case, the per-lot execution table above is not present. If the order was
                                        // executed as a single fill, "Zeitpunkt letzte Ausführung" gives the exact time instead.
                                        //
                                        // Zeitpunkt letzte Ausführung 13.07.2026 18:30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Zeitpunkt letzte Ausf.hrung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Wir haben für Sie am 24. Februar 2025 gekauft (Details siehe Folgeseite):
                                        // Wir haben für Sie am 18. Juni 2026 gekauft (Details siehe Folgeseite):
                                        //
                                        // Fallback for collective orders ("Sammelaufträge") where neither the per-lot
                                        // execution table nor "Zeitpunkt letzte Ausführung" is present on the document.
                                        // This line is always present, but only provides day precision.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Wir haben f.r Sie am (?<date>[\\d]{1,2}\\. .* [\\d]{4}) (gekauft|verkauft).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Total CHF 39'728.85 Zu Ihren Lasten
                        // Valuta 01.06.2026 CHF 39'728.85
                        //
                        // Total EUR 59'403.62 Zu Ihren Gunsten
                        // Valuta 01.06.2026 EUR 59'403.62
                        //
                        // Total CHF 22'780.87
                        // Zu Ihren Lasten Valuta 20.07.2026 CHF 22'780.87
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Zu Ihren (Lasten|Gunsten) )?Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
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

    // @formatter:off
    // Matches the nominal-value suffix embedded in a security name, e.g.
    // "N-Akt Linde PLC EUR 0.001 nom" or "N-Akt Apple Inc USD 0.00001 nom".
    // This denotes the security's own (home) currency and can differ from
    // the currency a particular dividend happens to be paid in (e.g. a
    // EUR-denominated stock paying a USD distribution).
    // @formatter:on
    private static final Pattern NOMINAL_CURRENCY_PATTERN = Pattern.compile("\\b([A-Z]{3})\\s+[\\.'\\d]+\\s+nom\\b");

    /**
     * If the security name carries an explicit nominal-value currency (see
     * {@link #NOMINAL_CURRENCY_PATTERN}), it takes precedence over the
     * currency inferred from the distribution/payment amount, since the
     * latter need not match the security's own trading currency.
     */
    private Map<String, String> overrideWithNominalCurrency(Map<String, String> v, String name, String nameContinued)
    {
        var combined = nameContinued != null ? name + " " + nameContinued : name;

        var m = NOMINAL_CURRENCY_PATTERN.matcher(combined);
        if (m.find())
            v.put("currency", asCurrencyCode(m.group(1)));

        return v;
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

                        .oneOf( //

                                        // @formatter:off
                                        // 10'000 N-Akt Sibanye Stillwater Limited
                                        // Sponsored ADR Repr 4 Shs ADR/ADS
                                        // Übrige Aktien Valoren-Nr.: 52619625, ISIN: US82575P1075
                                        // Ausschüttung: USD 0.310942
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "nameContinued", "wkn", "isin", "currency") //
                                                        .find("Ihr Depotbestand per Ex\\-Datum .*") //
                                                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^(.*\\s)?Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Aussch.ttung: (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(overrideWithNominalCurrency(v, v.get("name"), v.get("nameContinued"))))) //
                                        , //
                                        // @formatter:off
                                        // 10'000 N-Akt TUI AG Aus Konversion
                                        // Namenaktien Valoren-Nr.: 125205291, ISIN: DE000TUAG505
                                        // Ausschüttung: EUR 0.10
                                        //
                                        // 250 N-Akt Apple Inc USD 0.00001 nom
                                        // Namenaktien Valoren-Nr.: 908440, ISIN: US0378331005
                                        // Ausschüttung: USD 0.27
                                        //
                                        // 750 N-Akt Nike Inc -B- Namenaktien
                                        // Valoren-Nr.: 957150, ISIN: US6541061031
                                        // Ausschüttung: USD 0.41
                                        //
                                        // 100 N-Akt Linde PLC EUR 0.001 nom
                                        // Namenaktien Valoren-Nr.: 124625792, ISIN: IE000S9YS762
                                        // Ausschüttung: USD 1.60
                                        //
                                        // The "Valoren-Nr.:" line may or may not have prefix text (e.g. a share
                                        // class continuation like "Namenaktien") before it.
                                        //
                                        // The security's home/nominal currency (embedded in the name as
                                        // "<currency> <nominal value> nom", e.g. "EUR 0.001 nom") does not always
                                        // match the currency the distribution is paid in - e.g. a EUR-denominated
                                        // stock (Linde PLC) can pay a USD dividend. In that case the security must
                                        // keep its own EUR currency rather than being tainted by the USD payment
                                        // currency picked up from the "Ausschüttung:" line.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Ihr Depotbestand per Ex\\-Datum .*") //
                                                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                                                        .match("^(.*\\s)?Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Aussch.ttung: (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(overrideWithNominalCurrency(v, v.get("name"), null)))) //
                        )

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
                        // Total USD 215.24 Zu Ihren Gunsten
                        // Valuta 01.07.2026 USD 215.24
                        //
                        // Zu Ihren Gunsten Valuta 13.08.2026 USD 47.24
                        // @formatter:on
                        .section("date") //
                        .match("^(Zu Ihren Gunsten )?Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [A-Z]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Total USD 215.24 Zu Ihren Gunsten
                        // Valuta 01.07.2026 USD 215.24
                        //
                        // Zu Ihren Gunsten Valuta 13.08.2026 USD 47.24
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Zu Ihren Gunsten )?Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Betrag USD 6'218.84
                        // Umrechnungskurs EUR/USD 1.175393
                        // @formatter:on
                        .section("termCurrency", "baseCurrency", "exchangeRate", "fxGross").optional() //
                        .match("^Betrag\\s+[A-Z]{3}\\s+(?<fxGross>['\\.,\\d]+)\\s*$") //
                        .match("^Umrechnungskurs\\s+(?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3})\\s+(?<exchangeRate>[\\.'\\d]+)\\s*$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
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
                        // Zusätzlicher Steuerrückbehalt(CHF -36.03) USD 46.13
                        //
                        // Additional US withholding tax retained on top of the ordinary "Quellensteuer"
                        // (e.g. under the Swiss/US relief-at-source mechanism) - only seen on USD dividends.
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Zus.tzlicher Steuerr.ckbehalt\\(.*\\) (?<currency>[A-Z]{3}) (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Quellensteuer 20% USD 1'243.77
                                        // Umrechnungskurs EUR/USD 1.175393
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("withHoldingTax", "currency", "termCurrency",
                                                                        "baseCurrency", "exchangeRate")
                                                        .match("^Quellensteuer [\\.,'\\d]+% (?<currency>[A-Z]{3}) (\\-)?(?<withHoldingTax>[\\.'\\d]+)$") //
                                                        .match("^Umrechnungskurs\\s+(?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3})\\s+(?<exchangeRate>[\\.'\\d]+)\\s*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);
                                                            processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                                                        }) //
                                        ,
                                        // @formatter:off
                                        // Quellensteuer 26.375% EUR 263.75
                                        // Quellensteuer 15% USD 10.13
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("withHoldingTax", "currency") //
                                                        .match("^Quellensteuer [\\.,'\\d]+% (?<currency>[A-Z]{3}) (\\-)?(?<withHoldingTax>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> processWithHoldingTaxEntries(t, v,
                                                                        "withHoldingTax", type)) //
                        )
        ;
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ausführungsgebühr SSX CHF 4.08
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Ausf.hrungsgeb.hr( [A-Za-z0-9]+)? (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage EUR 955.17
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Courtage EUR 11.90
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Fremde Courtage (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Auftragsausführung Anlagefonds USD 2.46
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Auftragsausf.hrung Anlagefonds (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SEC Börsengebühr USD 1.26
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^SEC B.rsengeb.hr (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
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
