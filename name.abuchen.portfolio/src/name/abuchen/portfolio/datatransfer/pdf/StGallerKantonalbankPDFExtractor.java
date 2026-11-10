package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben für Sie am 2. Juni 2026 gekauft (Details siehe Folgeseite):
                                        // 30 N-Akt Partners Group Holding AG CHF Valoren-Nr. 2460882
                                        // 0.01 nom ISIN CH0024608827
                                        // Währung Betrag
                                        // Kurs CHF 820.40 CHF 24'612.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "wkn", "nameContinued", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am .* (gekauft|verkauft).*") //
                                                        .match("^(?<shares>[\\.'\\d]+) (?<name>.*?) Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^(?<nameContinued>.+) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^W.hrung Betrag$") //
                                                        .match("^Kurs (?<currency>[A-Z]{3}) [\\.'\\d]+( [A-Z]{3} [\\.'\\d]+)?$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // Wir haben für Sie am 24. Februar 2025 gekauft (Details siehe Folgeseite):
                                        // 10'000 N-Akt TUI AG Aus Konversion Valoren-Nr. 125205291
                                        // ISIN DE000TUAG505
                                        // Währung Betrag
                                        // Kurs EUR 6.822613 EUR 68'226.13
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "name", "wkn", "isin", "currency") //
                                                        .find("Wir haben f.r Sie am .* (gekauft|verkauft).*") //
                                                        .match("^(?<shares>[\\.'\\d]+) (?<name>.*?) Valoren\\-Nr\\. (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
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
                                        // 10'000 20.02.2025 09:00:13 Xetra EUR 6.702
                                        // 500 28.05.2026 14:04:36 SIX Swiss Exchange - EBBO Book CHF 79.39
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
                        //
                        // Total CHF 39'728.85 Zu Ihren Lasten
                        // Valuta 01.06.2026 CHF 39'728.85
                        //
                        // Total EUR 59'403.62 Zu Ihren Gunsten
                        // Valuta 01.06.2026 EUR 59'403.62
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
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))) //
                                        , //
                                        // @formatter:off
                                        // 10'000 N-Akt TUI AG Aus Konversion
                                        // Namenaktien Valoren-Nr.: 125205291, ISIN: DE000TUAG505
                                        // Ausschüttung: EUR 0.10
                                        //
                                        // 750 N-Akt Nike Inc -B- Namenaktien
                                        // Valoren-Nr.: 957150, ISIN: US6541061031
                                        // Ausschüttung: USD 0.41
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Ihr Depotbestand per Ex\\-Datum .*") //
                                                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                                                        .match("^(.*\\s)?Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Aussch.ttung: (?<currency>[A-Z]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))) //
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
                        // Zu Ihren Gunsten Valuta 13.02.2026 EUR 736.25
                        //
                        // Total USD 215.24 Zu Ihren Gunsten
                        // Valuta 01.07.2026 USD 215.24
                        // @formatter:on
                        .section("date") //
                        .match("^(Zu Ihren Gunsten )?Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [A-Z]{3} [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Gunsten Valuta 13.02.2026 EUR 736.25
                        //
                        // Total USD 215.24 Zu Ihren Gunsten
                        // Valuta 01.07.2026 USD 215.24
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
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Zus.tzlicher Steuerr.ckbehalt ?(\\(.*\\))? (?<currency>[A-Z]{3}) (\\-)?(?<tax>[\\.'\\d]+)$") //
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
                                        // Quellensteuer 15% USD 46.13
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
                        .section("fee", "currency").optional() //
                        .match("^Ausf.hrungsgeb.hr( [A-Za-z0-9]+)? (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

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
