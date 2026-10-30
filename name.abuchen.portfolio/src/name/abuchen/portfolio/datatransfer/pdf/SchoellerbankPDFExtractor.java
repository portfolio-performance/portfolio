package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

/**
 * @formatter:off
 * Schoellerbank AG (Vienna) sends two kinds of statements:
 *
 * "Abrechnung Handel"   - Kauf, Verkauf
 * "Abrechnung Ereignis" - Ertrag (dividends and bond coupons), Tilgung,
 *                         Kapitalmaßnahme (capital reduction with cash
 *                         compensation)
 *
 * All of them share the same layout and the same line
 * "Wir haben für Sie am ... unten angeführtes Geschäft abgerechnet:".
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SchoellerbankPDFExtractor extends AbstractPDFExtractor
{
    public SchoellerbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Schoellerbank AG");

        addBuySellTransaction();
        addDividendTransaction();
        addCapitalReductionTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Schoellerbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: (Kauf|Verkauf|Tilgung)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Tilgung" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Gesch.ftsart: (?<type>(Kauf|Verkauf|Tilgung))[\\s]*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Tilgung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: US02079K3059  A l p h a b e t  I n c.
                                        // Reg. Shs Cap.Stk Cl. A DL-,001
                                        // Kurs: 326,24 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\.,\\d]+[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: CH0005362097  K O N .  L U C H T V AART MIJ. N.V.
                                        // SF-ANL. 1986(96/PERPETUAL)
                                        // Kurs: 100,5 %
                                        // Verwahrart: WR
                                        // Positionsdaten: Loco: Zürich
                                        // Kurswert: 35.175,-- CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\.,\\d]+[\\s]+%[\\s]*$") //
                                                        .match("^Kurswert: (\\-)?[\\.,\\d-]+[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Zugang: 730 Stk
                                        // Abgang: 900 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk[\\s]*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Abgang: 35.000 CHF
                                        // Kurs: 100,5 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) [A-Z]{3}[\\s]*$") //
                                                        .find("Kurs: [\\.,\\d]+[\\s]+%[\\s]*") //
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                            t.setShares(asBondNominal(v.get("shares")));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelszeit: 18.12.2025 um 11:00:41 Uhr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Valuta 15.5.2026
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Zu Lasten IBAN no35 1920 1899 2043 4021 -120.970,92 USD
                        // Zu Gunsten IBAN Mp73 0023 3905 2666 4048 35.122,24 CHF
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Kurswert: -94.553,05 CHF
                        // Devisenkurs: 1,270881 (19.12.2025) -120.165,64 USD
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "exchangeRate", "gross", "termCurrency").optional() //
                        .match("^Kurswert: (\\-)?(?<fxGross>[\\.,\\d-]+)[\\s]+(?<baseCurrency>[A-Z]{3})[\\s]*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) (\\-)?(?<gross>[\\.,\\d-]+)[\\s]+(?<termCurrency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftrags-Nr.: 23477527-18.12.2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: AT0000A3FA05  v o e s t a l p i n e AG
                                        // Schuldverschr.2024-2029
                                        // Kup. 3.10.2026/GZJ Endfälligkeit 3.10.2029
                                        // Zinsertrag für 365 Tage: 5.625,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zinsertrag f.r [\\d]{1,3} Tage: (\\-)?[\\.,\\d-]+[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kup."))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE0002635299  i S h . S T . E u r . Sel.Div.30 U.ETF DE
                                        // Inhaber-Anteile
                                        // Fondsgesellschaft: BlackRock Asset Management Deutschl
                                        // and AG
                                        // Ertrag: 0,418646 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Fondsgesellschaft"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // 3.000 Stk
                        // Titel: DE0002635299  i S h . S T . E u r . Sel.Div.30 U.ETF DE
                        //
                        // 150.000 EUR
                        // Titel: AT0000A3FA05  v o e s t a l p i n e AG
                        // @formatter:on
                        .section("shares", "notation") //
                        .match("^(?<shares>[\\.,\\d]+) (?<notation>(Stk|[A-Z]{3}))[\\s]*$") //
                        .find("Titel:.*") //
                        .assign((t, v) -> {
                        // @formatter:off
                        // Percentage quotation, workaround for bonds
                        // @formatter:on
                            if ("Stk".equalsIgnoreCase(v.get("notation")))
                                t.setShares(asShares(v.get("shares")));
                            else
                                t.setShares(asBondNominal(v.get("shares")));
                        })

                        // @formatter:off
                        // Valuta 15.10.2026
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Extag: 15.10.2026
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Extag: (?<exDate>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Zu Gunsten IBAN ST25 3050 6552 3668 4008 973,47 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addCapitalReductionTransaction()
    {
        // A capital reduction pays out a cash compensation without removing the
        // shares. It is booked as a dividend of the security; the bank adjusts
        // the tax acquisition values on its own.
        final var type = new DocumentType("Gesch.ftsart: Kapitalma.nahme");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Titel: CH0244767585  U B S  G r o u p  A G
                        // Namens-Aktien SF -,10
                        // Verwahrart: WR
                        // Positionsdaten: Loco: Zürich
                        // Record-Tag: 22.4.2026
                        // Geschäftsart: Einbuchung
                        // wegen Kapitalreduktion
                        // Titel: CH0244767585  U B S  G r o u p  A G
                        // Namens-Aktien SF -,10
                        // Barausgleich: 0,428285 CHF je 1 Stk
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Barausgleich: [\\.,\\d]+[\\s]+(?<currency>[A-Z]{3}) je 1 Stk[\\s]*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Barausgleich"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 10.883 Stk
                        // Titel: CH0244767585  U B S  G r o u p  A G
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) Stk[\\s]*$") //
                        .find("Titel:.*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 23.4.2026
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Extag: 21.4.2026
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Extag: (?<exDate>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Zu Gunsten IBAN aH14 0978 5226 7552 4008 5.027,81 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Barausgleich: 4.661,03 CHF
                        // 4.661,03 CHF
                        // Devisenkurs: 0,9245 (22.4.2026) 5.041,67 EUR
                        // @formatter:on
                        .section("fxGross", "termCurrency", "exchangeRate", "gross", "baseCurrency") //
                        .match("^Barausgleich: (?<fxGross>[\\.,\\d-]+)[\\s]+(?<termCurrency>[A-Z]{3})[\\s]*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) (\\-)?(?<gross>[\\.,\\d-]+)[\\s]+(?<baseCurrency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Geschäftsart: Kapitalmaßnahme
                        // wegen Kapitalreduktion
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^Gesch.ftsart: (?<note1>Kapitalma.nahme)[\\s]*$") //
                        .match("^wegen (?<note2>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " wegen " + trim(v.get("note2"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // KESt-Neu: -63.118,48 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt\\-Neu: \\-(?<tax>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KESt-Alt: -1.546,88 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt\\-Alt: \\-(?<tax>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KESt Ausländische Dividende: -278,70 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Transaktionsgebühr: -3,14 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsgeb.hr: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Umsatzsteuer: -0,63 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Umsatzsteuer: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen: -1.174,46 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Fremde Spesen: -120,95 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Spesen: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Devisenprovision: -330,75 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Devisenprovision: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SWIFT-Gebühr: -5,88 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^SWIFT\\-Geb.hr: \\-(?<fee>[\\.,\\d-]+)[\\s]+(?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
