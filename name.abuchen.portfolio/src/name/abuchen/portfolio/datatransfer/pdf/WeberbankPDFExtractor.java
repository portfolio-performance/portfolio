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
                                                        .attributes("currency", "name", "isin", "wkn", "name1") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " "
                                                                                + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

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
                                t.setShares(asBondNominal(v.get("shares")));
                            else
                                t.setShares(asShares(v.get("shares")));
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
                            context.put("date", v.get("date"));
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
                        // When purchasing bonds, the accrued interest ("Stückzinsen") is part of the
                        // purchase price, so the gross value is the total amount.
                        //
                        // Devisenkurs (EUR/NOK) 11,207 vom 02.04.2026
                        // Ausmachender Betrag 113.295,54- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^Ausmachender Betrag (?<gross>[\\.,\\d]+)(\\-)? [A-Z]{3}$") //
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

                        // @formatter:off
                        // When purchasing bonds, the accrued interest is part of the purchase price.
                        // It is kept in the note because it is what triggers the tax adjustment.
                        //
                        // Stückzinsen für 232 Tage per 07.04.2026 1.111,28- EUR
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>St.ckzinsen f.r [\\d]+ Tage) per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note2>[\\.,\\d]+)\\- (?<note3>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note2"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(t -> {
                            // @formatter:off
                            // Handshake for tax lost adjustment transaction
                            // @formatter:on
                            context.put("name", t.getPortfolioTransaction().getSecurity().getName());
                            context.put("isin", t.getPortfolioTransaction().getSecurity().getIsin());
                            context.put("wkn", t.getPortfolioTransaction().getSecurity().getWkn());
                            context.put("shares", Long.toString(t.getPortfolioTransaction().getShares()));

                            if (t.getNote() != null)
                                context.put("note", t.getNote());

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
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
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zahlbarkeitstag .* [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " "
                                                                                + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // NOK 1.425.000,00 NORWEGEN, KÖNIGREICH NO0010875230 (A28TXS)
                                        // NK-ANL. 2020(30)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "isin", "wkn", "name1") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " "
                                                                                + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

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
                                t.setShares(asBondNominal(v.get("shares")));
                            else
                                t.setShares(asShares(v.get("shares")));
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
                        // Only a credit ("zu Gunsten") is a tax refund. A charge ("zu Lasten") is a
                        // different transaction type and is deliberately not imported.
                        //
                        // Ausmachender Betrag 558,03 EUR
                        // Den Gegenwert buchen wir mit Valuta 18.06.2026 zu Gunsten des Kontos xxxxxxxxxx
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .match("^Den Gegenwert buchen wir mit Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} zu Gunsten .*$") //
                        .assign((t, v) -> {
                            // Date of the trade, not of the settlement a couple
                            // of days later
                            if (context.get("date") != null && context.get("time") != null)
                                t.setDateTime(asDate(context.get("date"), context.get("time")));

                            if (context.get("shares") != null)
                                t.setShares(Long.parseLong(context.get("shares")));

                            if (context.get("isin") != null)
                                t.setSecurity(getOrCreateSecurity(context));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            t.setNote(context.get("note"));

                            if (t.getSecurity() != null && context.get("exchangeRate") != null)
                            {
                                var rate = asExchangeRate(context);
                                type.getCurrentContext().putType(rate);

                                var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("amount")));
                                var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                            }
                        })

                        .wrap(t -> {
                            if (t.getDateTime() != null && t.getCurrencyCode() != null && t.getAmount() != 0)
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

                        // No test document available. The line shape follows the Kapitalertragsteuer
                        // and Solidaritätszuschlag lines of the same statement family; it is not
                        // copied from a statement.
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
}
