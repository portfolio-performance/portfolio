package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class FondsdepotBankPDFExtractor extends AbstractPDFExtractor
{
    public FondsdepotBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Fondsdepot Bank");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Fondsdepot Bank";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Depotabrechnung Auszug Nr\\.", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Fondsbezeichnung: Robeco GlobConsTrend D EUR
                                        // ISIN/WKN: LU0187079347/A0CA0W Ertragsverwendung: thesaurierend
                                        // @formatter:on
                                        .section("name", "isin", "wkn") //
                                        .match("^Fondsbezeichnung: (?<name>.*)$") //
                                        .match("^ISIN\\/WKN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6}) .*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("name", trim(v.get("name")));
                                            ctx.put("isin", v.get("isin"));
                                            ctx.put("wkn", v.get("wkn"));
                                        }));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(Kauf|Wiederanlage) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\+[\\.,\\d]+$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(3);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Kauf 50,00 EUR 15.09.2025 400,2271 EUR +0,125
                        // Kauf 50,00 EUR 18.02.2025 1.031,1630 USD +0,050
                        // Wiederanlage 19,81 EUR 11.11.2025 25,5500 EUR +0,775
                        // @formatter:on
                        .section("currency") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^(Kauf|Wiederanlage) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ (?<currency>[A-Z]{3}) \\+[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf 50,00 EUR 15.09.2025 400,2271 EUR +0,125
                        // @formatter:on
                        .section("shares") //
                        .match("^(Kauf|Wiederanlage) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\+(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Kauf 50,00 EUR 15.09.2025 400,2271 EUR +0,125
                        // @formatter:on
                        .section("date") //
                        .match("^(Kauf|Wiederanlage) [\\.,\\d]+ [A-Z]{3} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [A-Z]{3} \\+[\\.,\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // The transaction fee (Entgelt) is by definition a cost,
                        // the sign in the document is ignored.
                        // The front-end load (Ausgabeaufschlag) is booked as a fee.
                        .oneOf( //
                                        // @formatter:off
                                        // Transaktion Anlagebetrag Wertermittlungstag Preis je Anteil 2) Abgerechnete Anteile
                                        // Entgelt Währungsbetrag Ausgabeaufschlag Bestand alt
                                        // Einzahlbetrag Devisenkurs 1) Bestand neu
                                        // Kauf 50,00 EUR 18.02.2025 1.031,1630 USD +0,050
                                        // 0,00 EUR 51,78 USD 2,59 EUR 7,952
                                        // 50,00 EUR 1,035637 USD 8,002
                                        //
                                        // Kauf 4.995,00 EUR 02.09.2025 277,5990 USD +20,803
                                        // aus Tausch -5,00 EUR 5.774,83 USD 237,80 EUR 0,000
                                        // 5.000,00 EUR 1,156122 USD 20,803
                                        //
                                        // Kauf 24,86 EUR 16.07.2025 11,1035 USD +2,590
                                        // -0,15 EUR 28,76 USD 0,00 EUR 10,933
                                        // 25,00 EUR 1,157098 USD 100 % 13,523
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "fee", "feeCurrency", "fxGross", "termCurrency", "frontEndLoad", "frontEndLoadCurrency", "amount", "baseCurrency", "exchangeRate") //
                                                        .match("^(Kauf|Wiederanlage) (?<gross>[\\.,\\d]+) [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\+[\\.,\\d]+$") //
                                                        .match("^(.* )?(\\-)?(?<fee>[\\.,\\d]+) (?<feeCurrency>[A-Z]{3}) (?<fxGross>[\\.,\\d]+) (?<termCurrency>[A-Z]{3}) (?<frontEndLoad>[\\.,\\d]+) (?<frontEndLoadCurrency>[A-Z]{3}) [\\.,\\d]+$") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+) [A-Z]{3}( [\\.,\\d]+ %)? [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("baseCurrency")));

                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                                                            var fee = Money.of(asCurrencyCode(v.get("feeCurrency")), asAmount(v.get("fee")));
                                                            checkAndSetFee(fee, t, type.getCurrentContext());

                                                            var frontEndLoad = Money.of(asCurrencyCode(v.get("frontEndLoadCurrency")), asAmount(v.get("frontEndLoad")));
                                                            checkAndSetFee(frontEndLoad, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Transaktion Anlagebetrag Wertermittlungstag Preis je Anteil 1) Abgerechnete Anteile
                                        // Entgelt Währungsbetrag Ausgabeaufschlag Bestand alt
                                        // Einzahlbetrag Devisenkurs Rabatt 1) Bestand neu
                                        // Kauf 50,00 EUR 15.09.2025 400,2271 EUR +0,125
                                        // 0,00 EUR 1,43 EUR 0,000
                                        // 50,00 EUR 40 % 0,125
                                        //
                                        // Wiederanlage 19,81 EUR 11.11.2025 25,5500 EUR +0,775
                                        // Ertrag 0,00 EUR 0,00 EUR 157,501
                                        // 19,81 EUR 100 % 158,276
                                        //
                                        // Kauf 4.718,15 EUR 05.09.2025 772,9944 EUR +6,104
                                        // aus Tausch -5,00 EUR 137,22 EUR 6,449
                                        // 4.723,15 EUR 12,553
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fee", "feeCurrency", "frontEndLoad", "frontEndLoadCurrency", "amount", "currency") //
                                                        .match("^(.* )?(\\-)?(?<fee>[\\.,\\d]+) (?<feeCurrency>[A-Z]{3}) (?<frontEndLoad>[\\.,\\d]+) (?<frontEndLoadCurrency>[A-Z]{3}) [\\.,\\d]+$") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})( [\\.,\\d]+ %)? [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var fee = Money.of(asCurrencyCode(v.get("feeCurrency")), asAmount(v.get("fee")));
                                                            checkAndSetFee(fee, t, type.getCurrentContext());

                                                            var frontEndLoad = Money.of(asCurrencyCode(v.get("frontEndLoadCurrency")), asAmount(v.get("frontEndLoad")));
                                                            checkAndSetFee(frontEndLoad, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // aus Tausch -5,00 EUR 137,22 EUR 6,449
                        // @formatter:on
                        .section("note").optional() //
                        .match("^aus (?<note>Tausch) .*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        // @formatter:off
                        // Wiederanlage 19,81 EUR 11.11.2025 25,5500 EUR +0,775
                        // Ertrag 0,00 EUR 0,00 EUR 157,501
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Wiederanlage) .*$") //
                        .match("^(?<note2>Ertrag) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(v.get("note1"), v.get("note2"), " ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addSellTransaction(type);
        addSellForCustodyFeeTransaction(type);
        addSellTaxReturnBlock(type);
    }

    private void addSellTransaction(DocumentType type)
    {
        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\-[\\.,\\d]+$", //
                        "^Verbleibende Gutschrift .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // Verkauf 4.859,43 EUR 02.09.2025 379,9100 EUR -12,791
                        // Verkauf 5.471,33 EUR 02.09.2025 1.013,1600 USD -6,340
                        // Gebührenverkauf 45,83 EUR 01.04.2020 228,2300 EUR -0,201
                        // @formatter:on
                        .section("currency") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ (?<currency>[A-Z]{3}) \\-[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Verkauf 4.859,43 EUR 02.09.2025 379,9100 EUR -12,791
                        // @formatter:on
                        .section("shares") //
                        .match("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\-(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Verkauf 4.859,43 EUR 02.09.2025 379,9100 EUR -12,791
                        // @formatter:on
                        .section("date") //
                        .match("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ [A-Z]{3} \\-[\\.,\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Verbleibende Gutschrift 4.723,15 EUR
                        // Verbleibende Gutschrift 5.870,41 USD 5.000,00 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Verbleibende Gutschrift ([\\.,\\d]+ [A-Z]{3} )?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // If the sale was made to pay the custody fee, the fee is
                        // booked as a separate fee transaction. Therefore the sale
                        // amount is increased by the custody fee.
                        //
                        // ant. Depotführungsentgelt lfd. Jahr inkl. 19% USt. -45,83 EUR
                        // @formatter:on
                        .section("amount").optional() //
                        .match("^(ant\\. )?Depotf.hrungsentgelt .* (\\-)?(?<amount>[\\.,\\d]+) [A-Z]{3}$") //
                        .assign((t, v) -> t.setAmount(t.getPortfolioTransaction().getAmount() + asAmount(v.get("amount"))))

                        // @formatter:off
                        // Verkauf 5.471,33 EUR 02.09.2025 1.013,1600 USD -6,340
                        // für Tausch 6.423,78 USD 8,362
                        // 1,174081 USD 2,022
                        // Abrechnungsbetrag 6.423,78 USD 5.471,33 EUR
                        // @formatter:on
                        .section("exchangeRate", "fxGross", "termCurrency", "gross", "baseCurrency").optional() //
                        .match("^(?<exchangeRate>[\\.,\\d]+) [A-Z]{3} [\\.,\\d]+$") //
                        .match("^Abrechnungsbetrag (?<fxGross>[\\.,\\d]+) (?<termCurrency>[A-Z]{3}) (?<gross>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // für Tausch 12,791
                        // @formatter:on
                        .section("note").optional() //
                        .match("^f.r (?<note>Tausch)( .*)?$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        // @formatter:off
                        // Gebührenverkauf 45,83 EUR 01.04.2020 228,2300 EUR -0,201
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Geb.hrenverkauf) .*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellForCustodyFeeTransaction(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Geb.hrenverkauf [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\-[\\.,\\d]+$", //
                        "^Verbleibende Gutschrift .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        // @formatter:off
                        // Gebührenverkauf 45,83 EUR 01.04.2020 228,2300 EUR -0,201
                        // @formatter:on
                        .section("date", "currency", "shares") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^Geb.hrenverkauf [\\.,\\d]+ [A-Z]{3} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ (?<currency>[A-Z]{3}) \\-(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // ant. Depotführungsentgelt lfd. Jahr inkl. 19% USt. -45,83 EUR
                        // @formatter:on
                        .section("note", "amount", "currency") //
                        .match("^(ant\\. )?(?<note>Depotf.hrungsentgelt.*) inkl\\. .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addSellTaxReturnBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ [A-Z]{3} \\-[\\.,\\d]+$", //
                        "^Verbleibende Gutschrift .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // Verkauf 4.859,43 EUR 02.09.2025 379,9100 EUR -12,791
                        // @formatter:on
                        .section("date", "currency", "shares") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^(Verkauf|Geb.hrenverkauf) [\\.,\\d]+ [A-Z]{3} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ (?<currency>[A-Z]{3}) \\-(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Verbleibende Gutschrift 4.723,15 EUR
                        // Verbleibende Gutschrift 5.870,41 USD 5.000,00 EUR
                        // @formatter:on
                        .section("currency") //
                        .match("^Verbleibende Gutschrift ([\\.,\\d]+ [A-Z]{3} )?[\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                        // @formatter:off
                        // Only positive taxes are refunds, withheld taxes have a negative sign.
                        //
                        // Kapitalertragsteuer 124,44 EUR
                        // Kapitalertragsteuer 518,96 USD 442,01 EUR
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kapitalertrags(s)?teuer ([\\.,\\d]+ [A-Z]{3} )?(?<tax>[\\.,\\d]+) [A-Z]{3}$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // Solidaritätszuschlag 6,84 EUR
                        // Solidaritätszuschlag 28,54 USD 24,31 EUR
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Solidarit.tszuschlag ([\\.,\\d]+ [A-Z]{3} )?(?<tax>[\\.,\\d]+) [A-Z]{3}$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // Kirchensteuer 8,0000% 9,95 EUR
                        // Kirchensteuer 8,0000% 41,51 USD 35,36 EUR
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+% ([\\.,\\d]+ [A-Z]{3} )?(?<tax>[\\.,\\d]+) [A-Z]{3}$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            return null;
                        });
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Aussch.ttung per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 51473 VOKyx Datum: Hof, 12. November 2025
                                        // @formatter:on
                                        .section("date") //
                                        .match("^.*Datum: .*, (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date")))

                                        // @formatter:off
                                        // Fondsbezeichnung: FF-GloDivFd A-QINCOME(G)Eur
                                        // ISIN/WKN: LU0731782404/A1JSY0 Ertragsverwendung: ausschüttend
                                        // @formatter:on
                                        .section("name", "isin", "wkn") //
                                        .match("^Fondsbezeichnung: (?<name>.*)$") //
                                        .match("^ISIN\\/WKN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\/(?<wkn>[A-Z0-9]{6}) .*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("name", trim(v.get("name")));
                                            ctx.put("isin", v.get("isin"));
                                            ctx.put("wkn", v.get("wkn"));
                                        })

                                        // @formatter:off
                                        // Ermittlung der Gutschrift/Wiederanlage: EUR gesamt
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Ermittlung der Gutschrift\\/Wiederanlage: (?<currency>[A-Z]{3}) gesamt$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Aussch.ttung per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$", "^Wiederanlagebetrag .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // EUR je Anteil EUR gesamt
                        // @formatter:on
                        .section("currency") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^(?<currency>[A-Z]{3}) je Anteil [A-Z]{3} gesamt$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // für das Geschäftsjahr vom 01.05.2025 bis 30.04.2026 Anteile 157,501
                        // @formatter:on
                        .section("shares") //
                        .match("^.* Anteile (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // The booking date is the document date,
                        // the date of the distribution is the ex-date.
                        //
                        // 51473 VOKyx Datum: Hof, 12. November 2025
                        // Ausschüttung per 03.11.2025
                        // @formatter:on
                        .section("exDate") //
                        .documentContext("date") //
                        .match("^Aussch.ttung per (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setExDate(asDate(v.get("exDate")));
                        })

                        // @formatter:off
                        // Wiederanlagebetrag 19,81
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^Wiederanlagebetrag (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        addDividendeTaxReturnBlock(type);
    }

    private void addDividendeTaxReturnBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Aussch.ttung per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$", "^Wiederanlagebetrag .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // EUR je Anteil EUR gesamt
                        // @formatter:on
                        .section("currency") //
                        .documentContext("name", "isin", "wkn") //
                        .match("^(?<currency>[A-Z]{3}) je Anteil [A-Z]{3} gesamt$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // für das Geschäftsjahr vom 01.05.2025 bis 30.04.2026 Anteile 157,501
                        // @formatter:on
                        .section("shares") //
                        .documentContext("date", "currency") //
                        .match("^.* Anteile (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // Only positive taxes are refunds, withheld taxes have a negative sign.
                        //
                        // Kapitalertragsteuer 4,25
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // Solidaritätszuschlag 0,23
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Solidarit.tszuschlag (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // Kirchensteuer 8,0000% 0,34
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+% (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            return null;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer -124,44 EUR
                        // Kapitalertragsteuer -518,96 USD -442,01 EUR
                        // @formatter:on
                        .section("sign", "tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer ((\\-)?[\\.,\\d]+ [A-Z]{3} )?(?<sign>(\\-)?)(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer -4,25
                        // @formatter:on
                        .section("sign", "tax").optional() //
                        .documentContext("currency") //
                        .match("^Kapitalertrags(s)?teuer (?<sign>(\\-)?)(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag -6,84 EUR
                        // Solidaritätszuschlag -28,54 USD -24,31 EUR
                        // @formatter:on
                        .section("sign", "tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag ((\\-)?[\\.,\\d]+ [A-Z]{3} )?(?<sign>(\\-)?)(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag -0,23
                        // @formatter:on
                        .section("sign", "tax").optional() //
                        .documentContext("currency") //
                        .match("^Solidarit.tszuschlag (?<sign>(\\-)?)(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 0,0000% -0,00 EUR
                        // Kirchensteuer 0,0000% -0,00 USD -0,00 EUR
                        // @formatter:on
                        .section("sign", "tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+% ((\\-)?[\\.,\\d]+ [A-Z]{3} )?(?<sign>(\\-)?)(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 0,0000% -0,00
                        // @formatter:on
                        .section("sign", "tax").optional() //
                        .documentContext("currency") //
                        .match("^Kirchensteuer [\\.,\\d]+% (?<sign>(\\-)?)(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processSignedTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // The transaction fee is by definition a cost,
                        // the sign in the document is ignored.
                        //
                        // Transaktionsentgelt inkl. gesetzl. USt -5,00 EUR
                        // Transaktionsentgelt inkl. gesetzl. USt -5,87 USD -5,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt .* (\\-)?(?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    /**
     * Withheld taxes are shown with a negative sign. A positive, non-zero
     * value is a tax refund which is already included in the credited amount.
     * The refund is booked as a separate tax refund transaction, therefore
     * the amount of the transaction is reduced by the refund.
     */
    private void processSignedTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if ("-".equals(v.get("sign")))
        {
            processTaxEntries(t, v, type);
            return;
        }

        var refund = asAmount(v.get("tax"));
        if (refund == 0)
            return;

        if (t instanceof BuySellEntry entry)
            entry.setAmount(entry.getPortfolioTransaction().getAmount() - refund);
        else if (t instanceof AccountTransaction tx)
            tx.setAmount(tx.getAmount() - refund);
    }
}
