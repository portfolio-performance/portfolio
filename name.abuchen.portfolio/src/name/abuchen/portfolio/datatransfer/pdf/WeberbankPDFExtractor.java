package name.abuchen.portfolio.datatransfer.pdf;

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
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class WeberbankPDFExtractor extends AbstractPDFExtractor
{
    public WeberbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BLZ 101 201 00");
        addBankIdentifier("BLZ 10120100");
        addBankIdentifier("BIC WELADED1WBB");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Weberbank Actiengesellschaft";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        var firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);

        var pdfTransaction = new Transaction<BuySellEntry>();
        firstRelevantLine.set(pdfTransaction);

        // Map for tax lost adjustment transaction
        var context = type.getCurrentContext();

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 4.440 NEL ASA NO0010081235 (A0B733)
                                        // NAVNE-AKSJER NK -,20
                                        // Kurswert 9.657,00- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurswert [\\.,\\d]+(\\-)? (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // NOK 1.425.000,00 1,375 % NORWEGEN, KÖNIGREICH NO0010875230 (A28TXS)
                                        // NK-ANL. 2020(30)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "nameContinued") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Stück 4.440 NEL ASA NO0010081235 (A0B733)
                        // NOK 1.425.000,00 1,375 % NORWEGEN, KÖNIGREICH NO0010875230 (A28TXS)
                        // @formatter:on
                        .section("notation", "shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<notation>(St.ck|[A-Z]{3})) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
                            {
                                var shares = asBigDecimal(v.get("shares"));
                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        // @formatter:off
                        // Schlusstag/-Zeit 26.03.2021 15:12:58 Auftraggeber XXXXXXXXXXXXX
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Handshake for tax lost adjustment transaction
                            // @formatter:on
                            context.put("time", v.get("time"));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                        })

                        // @formatter:off
                        // Ausmachender Betrag 9.978,18- EUR
                        // Ausmachender Betrag 2.335,30 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs (EUR/NOK) 11,207 vom 02.04.2026
                        // Kurswert 112.184,26- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                            // @formatter:off
                            // Handshake for tax lost adjustment transaction
                            // @formatter:on
                            context.put("baseCurrency", rate.getBaseCurrency());
                            context.put("termCurrency", rate.getTermCurrency());
                            context.put("exchangeRate", v.get("exchangeRate"));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftragsnummer 742198/43.00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Auftragsnummer .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit bestens
                        // Limit billigst
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap((t, ctx) -> {
                            var item = new BuySellEntryItem(t);

                            // @formatter:off
                            // Handshake for tax lost adjustment transaction
                            // @formatter:on
                            context.put("name", item.getSecurity().getName());
                            context.put("isin", item.getSecurity().getIsin());
                            context.put("wkn", item.getSecurity().getWkn());
                            context.put("shares", Long.toString(item.getShares()));

                            if (t.getNote() != null)
                                context.put("note", t.getNote());

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxLostAdjustmentTransaction(context, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("(Dividendengutschrift|Zinsgutschrift)");
        this.addDocumentTyp(type);

        var firstRelevantLine = new Block("^(Dividendengutschrift|Zinsgutschrift)$");
        type.addBlock(firstRelevantLine);

        var pdfTransaction = new Transaction<AccountTransaction>();
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 107 APPLE INC. US0378331005 (865985)
                                        // REGISTERED SHARES O.N.
                                        // Zahlbarkeitstag 13.08.2020 Dividende pro Stück 0,82 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "nameContinued", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Zahlbarkeitstag .* [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // NOK 1.425.000,00 NORWEGEN, KÖNIGREICH NO0010875230 (A28TXS)
                                        // NK-ANL. 2020(30)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "nameContinued") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Stück 107 APPLE INC. US0378331005 (865985)
                        // NOK 1.425.000,00 NORWEGEN, KÖNIGREICH NO0010875230 (A28TXS)
                        // @formatter:on
                        .section("notation", "shares") //
                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                        .match("^(?<notation>(St.ck|[A-Z]{3})) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"Stück".equalsIgnoreCase(v.get("notation")))
                            {
                                var shares = asBigDecimal(v.get("shares"));
                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        // @formatter:off
                        // Zahlbarkeitstag 13.08.2020 Dividende pro Stück 0,82 USD
                        // Zahlbarkeitstag 19.08.2026 Zinssatz p. a. 1,375 %
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ex-Tag 07.08.2020 Art der Dividende Quartalsdividende
                        // Ex-Tag 28.03.2025
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Ex\\-Tag (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Ausmachender Betrag 55,14+ EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR / USD 1,1848
                        // Dividendengutschrift 87,74 USD 74,05+ EUR
                        //
                        // Devisenkurs EUR / NOK 10,9341
                        // Zinsertrag 19.593,75 NOK 1.791,99+ EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^(Dividendengutschrift|Zinsertrag) (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+)\\+ [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Abrechnungsnr. 54861333620
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Abrechnungsnr\\. [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Ex-Tag 07.08.2020 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxLostAdjustmentTransaction(Map<String, String> context, DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // Ausmachender Betrag 558,03 EUR
                        // Den Gegenwert buchen wir mit Valuta 18.06.2026 zu Gunsten des Kontos xxxxxxxxxx
                        // @formatter:on
                        .section("amount", "currency", "date").optional() //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> {
                            if (context.get("time") != null)
                                t.setDateTime(asDate(v.get("date"), context.get("time")));
                            else
                                t.setDateTime(asDate(v.get("date")));

                            t.setShares(Long.parseLong(context.get("shares")));
                            t.setSecurity(getOrCreateSecurity(context));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            t.setNote(context.get("note"));
                        })

                        // @formatter:off
                        // Ausmachender Betrag 293,10 EUR
                        // @formatter:on
                        .section("gross", "currency").optional() //
                        .match("^Ausmachender Betrag (?<gross>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            if (context.get("exchangeRate") != null)
                            {
                                var rate = asExchangeRate(context);
                                type.getCurrentContext().putType(rate);

                                var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                            }
                        })

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
                        // Kapitalertragsteuer 25 % auf 29,61 EUR 7,40- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+[\\s]*% auf [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 7,40 EUR 0,40- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+[\\s]*% auf [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 9 % auf 7,40 EUR 0,66- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+[\\s]*% auf [\\.,\\d]+ [A-Z]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 87,74 USD 11,11- EUR
                        // Einbehaltene Quellensteuer 27 % auf 4.345,00 DKK 156,80- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+[\\s]*% auf [\\.,\\d]+ [A-Z]{3} (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 74,05 EUR 11,11 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+[\\s]*% auf [\\.,\\d]+ [A-Z]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // When purchasing bonds, the accrued interest ("Stückzinsen") is treated as a fee.
                        //
                        // Stückzinsen für 232 Tage per 07.04.2026 1.111,28- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^St.ckzinsen f.r [\\d]+ Tage per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<fee>[\\.,\\d]+)\\- (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
