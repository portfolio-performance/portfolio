package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentContext;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class PostbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String isJointAccount = "isJointAccount"; //$NON-NLS-1$

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("Anteilige Berechnungsgrundlage .* \\(50,00 %\\).*"); //$NON-NLS-1$
        Boolean bJointAccount = false;

        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(isJointAccount, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(isJointAccount, Boolean.FALSE.toString());

    };

    public PostbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Postbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Postbank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf|Verkauf\\-Festpreisgesch.ft|Ausgabe Investmentfonds|R.cknahme Investmentfonds)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung "
                        + "(Kauf"
                        + "|Verkauf"
                        + "|Verkauf\\-Festpreisgesch.ft"
                        + "|Ausgabe Investmentfonds"
                        + "|R.cknahme Investmentfonds)"
                        + "([\\s]+)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf|Ausgabe Investmentfonds|R.cknahme Investmentfonds)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Rücknahme Investmentfonds"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Stück 158 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                // REGISTERED SHARES 1C O.N.
                // Ausführungskurs 62,821 EUR
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 158 XTR.(IE) - MSCI WORLD IE00BJ0KDQ92 (A1XB5U)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // Schlusstag 05.02.2020
                                section -> section
                                        .attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Schlusstag/-Zeit 04.02.2020 08:00:04
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Den Gegenwert buchen wir mit Valuta 14.01.2020 zu Gunsten des Kontos 012345678
                                section -> section
                                        .attributes("date")
                                        .match("^Den Gegenwert buchen wir mit Valuta ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // Ausmachender Betrag 9.978,18- EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Abrech.-Preis 196,00 USD
                // Devisenkurs (EUR/USD) 1,06386 vom 12.12.2016
                // Kurswert 1.289,64 EUR
                .section( "fxCurrency", "baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional()
                .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$")
                .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Limit 43,00 EUR 
                .section("note").optional()
                .match("^(?<note>Limit .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .conclude(PDFExtractorUtils.fixGrossValueBuySell())

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift"
                        + "|Aussch.ttung Investmentfonds"
                        + "|Gutschrift von Investmentertr.gen"
                        + "|Ertragsgutschrift"
                        + "|Zinsgutschrift)", jointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift"
                        + "|Aussch.ttung Investmentfonds"
                        + "|Gutschrift von Investmentertr.gen"
                        + "|Ertragsgutschrift .*"
                        + "|Zinsgutschrift)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 12 JOHNSON & JOHNSON SHARES US4781601046 (853260)
                // REGISTERED SHARES DL 1
                // Zahlbarkeitstag 14.01.2022 Ausschüttung pro St. 1,390000000 USD
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$")
                .match("(?<name1>.*)")
                .match("^.* (Aussch.ttung|Dividende|Ertrag) ([\\s]+)?pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // EUR 15.000,00 ENEL FINANCE INTL N.V. XS0177089298 (908043)
                // EO-MEDIUM-TERM NOTES 2003(23)
                .section("currency", "name", "isin", "wkn", "name1").optional()
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>.*)\\)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // Stück 12 JOHNSON & JOHNSON SHARES US4781601046
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // EUR 15.000,00 ENEL FINANCE INTL N.V. XS0177089298 (908043)
                                section -> section
                                        .attributes("shares")
                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\(.*\\)$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            t.setShares((asShares(v.get("shares")) / 100));
                                        })
                        )

                // Zahlbarkeitstag 08.04.2021 Ertrag  pro Stück 0,60 EUR
                .section("date")
                .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag 8,64+ EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>\\w{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .optionalOneOf(
                                // Devisenkurs EUR / USD 1,1920
                                // Dividendengutschrift 12,12 USD 10,17+ EUR
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().putType(asExchangeRate(v));

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type);
                                        })
                                ,
                                // Devisenkurs EUR / USD  0,9997  
                                // Zinsertrag 166,25 USD 166,30+ EUR
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^Zinsertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().putType(asExchangeRate(v));

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type);
                                        })
                        )

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // Bestandsstichtag 13.09.2022 Laufzeit Zinsschein 180 Tag(e)     
                .section("note").optional()
                .match("^.* (?<note>Zinsschein .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .conclude(PDFExtractorUtils.fixGrossValueA())

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Einbehaltene Quellensteuer 15 % auf 12,12 USD 1,53- EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltende Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 10,17 EUR 1,53 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // Anrechenbare Quellensteuer pro Stück 0,0144878 EUR 0,29 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer pro St.ck [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // Kapitalertragsteuer (Account)
                // Kapitalertragsteuer 24,51% auf 0,71 EUR 0,17- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kapitalerstragsteuer (Joint Account)
                // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag (Account)
                // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Solitaritätszuschlag (Joint Account)
                // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer (Joint Account)
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 39,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Abwicklungskosten Börse 0,04- EUR
                .section("fee", "currency").optional()
                .match("^Abwicklungskosten B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 11,82- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,65- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Auslagen 16,86- EUR
                .section("fee", "currency").optional()
                .match("^Fremde Auslagen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
