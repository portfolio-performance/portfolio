package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

public class DABPDFExtractor extends AbstractPDFExtractor
{
    public DABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DAB Bank"); //$NON-NLS-1$
        addBankIdentifier("DAB bank AG"); //$NON-NLS-1$
        addBankIdentifier("BNP Paribas S.A. Niederlassung Deutschland"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
        addTransferOutInTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank / BNP Paribas S.A."; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf|Gesamtf.lligkeit)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf|Verkauf|Gesamtf.lligkeit) .*$", "Dieser Beleg wird .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Kauf|Verkauf|Gesamtf.lligkeit)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Gesamtfälligkeit"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N. LU0392495700
                // STK 43,000 EUR 47,8310
                .section("isin", "name", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // 4,75% Ranft Invest GmbH Inh.-Schv. 01.07.2030 01.07.2021 DE000A2LQLH9
                // v.2018(2030)
                // Nominal Kurs
                // EUR 1.000,000 100,0000 %
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // HSBC Trinkaus & Burkhardt AG DIZ 27.08.21 27.08.2021 DE000TT649A1
                // Siemens 140
                // Nominal Kurs
                // STK 15,000 EUR 133,5700
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // WisdomTree Multi Ass.Iss.PLC Gas 3x Sh. 05.12.2062 IE00BLRPRL42
                // ETP Secs 12(12/62)
                // Nominal Kurs
                // STK 1,000 EUR 111,1111
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 15,000 EUR 133,5700
                // EUR 1.000,000 100,0000 %
                .section("notation", "shares")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} )?[\\.,\\d]+( %)?$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // Handelszeit 16:38* Provision USD 13,01-
                .section("time").optional()
                .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                                section -> section
                                        .attributes("date")
                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                                ,
                                // 07.12.2021 0000000000 EUR 0,05
                                section -> section
                                        .attributes("date")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                .oneOf(
                                // 08.01.2015 8022574001 EUR 150,00
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional()
                .match("^.* Kurswert ([\\s]+)?(?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)([\\-\\s]+)?$")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);
                    
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueBuySell())

                .wrap(t -> {
                    // If we have multiple entries in the document,
                    // then the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addBuySellTaxReturnBlock(type);
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertr.gnisgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertr.gnisgutschrift(?! (aus|VERSANDARTENSCHLUESSEL))).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Paychex Inc. Registered Shares DL -,01 US7043261079
                // STK 10,000 31.07.2020 27.08.2020 USD 0,620000
                .section("name", "isin", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Wertpapierbezeichnung WKN ISIN
                // HORMEL FOODS CORP. Registered Shares DL 0,01465 850875 US4404521001
                // Dividende pro Stück 0,1875 USD Schlusstag 10.01.2018
                .section("name", "wkn", "isin", "currency").optional()
                .find("Wertpapierbezeichnung WKN ISIN")
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^Dividende pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // STK 10,000 31.07.2020 27.08.2020 USD 0,620000
                                section -> section
                                        .attributes("shares")
                                        .match("^STK (?<shares>[\\.,\\d]+) .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // 1.500 Stück
                                section -> section
                                        .attributes("shares")
                                        .match("(?<shares>[\\.,\\d]+) St.ck")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // Valuta 15.02.2018 BIC CSDBDE71XXX
                                section -> section
                                        .attributes("date")
                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // 08.01.2015 8022574001 EUR 150,00
                                section -> section
                                        .attributes("date")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // 30.03.2015 0000000000 EUR/ZAR 13,195 EUR 586,80
                                section -> section
                                        .attributes("date")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3}\\/[\\w]{3} .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // 08.01.2015 8022574001 EUR 150,00
                                // 27.08.2020 123456789 USD 4,64
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // 30.03.2015 0000000000 EUR/ZAR 13,195 EUR 586,80
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Netto in USD zugunsten IBAN DE90 209,38 USD
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Netto in [\\w]{3} zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Netto zugunsten IBAN DE11 7603 0080 0111 1111 14 437,22 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Netto zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // 27.08.2020 123456789 USD 4,64
                // ausländische Dividende EUR 5,25
                // Devisenkurs: EUR/USD 1,1814
                .section("currency", "fxCurrency", "fxGross", "exchangeRate", "termCurrency", "baseCurrency").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .match("^ausl.ndische Dividende (?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .match("^Devisenkurs: (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);
                    
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // 01.12.2021 1234567891 EUR/JPY 127,99 EUR 3,59
                // ausländische Dividende EUR 4,84
                .section("baseCurrency", "termCurrency", "exchangeRate", "currency", "gross").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                .match("^ausl.ndische Dividende (?<currency>[\\w]{3}) ([\\s]+)?(?<gross>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);
                    
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // 07.02.2017 1234567 EUR/USD 1,0797 EUR 11,06
                // Ertrag für 2016 USD 14,24
                .section("baseCurrency", "termCurrency", "exchangeRate", "currency", "fxCurrency", "fxGross").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .match("^Ertrag f.r [\\d]{4} (?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);
                    
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Brutto in USD 281,25 USD
                // Devisenkurs 1,249900 USD / EUR
                // Brutto in EUR 225,02 EUR
                // Netto in USD zugunsten IBAN DE90 209,38 USD
                .section("termCurrency", "gross", "currency", "exchangeRate", "baseCurrency", "fxGross", "fxCurrency").optional()
                .match("^Brutto in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3})$")
                .match("^Brutto in [\\w]{3} (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Netto in [\\w]{3} zugunsten .*$")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // 30.06.2021 5325658010 USD 20,49
                // Ertrag für 2020/21 USD 25,11
                // Devisenkurs: EUR/USD 1,1914
                .section("currency", "gross", "termCurrency", "baseCurrency", "exchangeRate").optional()
                .match("^Ertrag f.r [\\d]{4}\\/[\\d]{2} (?<currency>[\\w]{3}) ([\\s]+)?(?<gross>[\\.,\\d]+).*$")
                .match("^Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("baseCurrency")), gross);
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueA())

                .wrap(t -> {
                    // If we have multiple entries in the document, then
                    // the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new TransactionItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addTransferOutInTransaction()
    {
        DocumentType type = new DocumentType("(Einbuchung|Split|Freie Lieferung)");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Einbuchung|Split .*|Freie Lieferung .*)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Solutiance AG Inhaber-Aktien o.N. DE0006926504
                .section("isin", "name", "name1", "shares", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("STK (?<shares>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung ISIN
                // BNP P.Easy-MSCI Pac.x.Jap.x.CW Nam.-Ant.UCITS ETF CAP EUR LU1291106356
                // o.N
                .section("isin", "name", "name1", "shares", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<currency>[\\w]{3}) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("STK (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung ISIN
                // Ceramic Fuel Cells Ltd. Registered Shares o.N. AU000000CFU6
                // Nominal Schlusstag Wert Verbuchung in
                // STK 1.000,000 04.02.2022 04.02.2022 EUR
                .section("name", "isin", "shares", "date", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^Nominal Schlusstag Wert Verbuchung in")
                .match("STK (?<shares>[\\.,\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    
                    t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    
                    t.setShares(asShares(v.get("shares")));
                    t.setDateTime(asDate(v.get("date")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                    t.setAmount(0L);
                })

                // 11.03.2021 3331111113 EUR 27,50
                .section("date", "amount", "currency").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // STK 0,0722 04.12.2018
                .section("date").optional()
                .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Wir haben Ihrem Depot im Verhältnis 1 : 22 folgende Stücke zugebucht (hinzugefügt):
                .section("note").optional()
                .match("^Wir haben Ihrem Depot im (?<note>Verh.ltnis [\\d]+ : [\\d]+) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                    t.setAmount(0L);
                })

                .wrap(t -> {
                    // If we have multiple entries in the document, then
                    // the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new TransactionItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addBuySellTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^(Kauf|Verkauf|Gesamtf.lligkeit) .*$", "Dieser Beleg wird .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N. LU0392495700
                // STK 43,000 EUR 47,8310
                .section("isin", "name", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // 4,75% Ranft Invest GmbH Inh.-Schv. 01.07.2030 01.07.2021 DE000A2LQLH9
                // v.2018(2030)
                // Nominal Kurs
                // EUR 1.000,000 100,0000 %
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // HSBC Trinkaus & Burkhardt AG DIZ 27.08.21 27.08.2021 DE000TT649A1
                // Siemens 140
                // Nominal Kurs
                // STK 15,000 EUR 133,5700
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                // WisdomTree Multi Ass.Iss.PLC Gas 3x Sh. 05.12.2062 IE00BLRPRL42
                // ETP Secs 12(12/62)
                // Nominal Kurs
                // STK 1,000 EUR 111,1111
                .section("isin", "name", "name1", "currency").optional()
                .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN")
                .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 1,000 EUR 111,1111
                .section("notation", "shares")
                .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} )?[\\.,\\d]+( %)?$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("STK"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                .oneOf(
                                // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                                section -> section
                                        .attributes("date")
                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // 07.12.2021 0000000000 EUR 0,05
                                section -> section
                                        .attributes("date")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // 27.08.2015 0000000000 EUR/USD 1,162765 EUR 4.465,12
                // zu versteuern (negativ) EUR 59,20
                // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                // 07.07.2020 1234567 1234567 EUR 16,46
                .section("currency", "amount").optional()
                .match("^zu versteuern \\(negativ\\).*$")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // 27.08.2015 0000000000 EUR/USD 1,162765 EUR 4.465,12
                // zu versteuern (negativ) EUR 341,55
                // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                // 24.08.2015 0000000000 00000000 EUR 90,09
                .section("baseCurrency", "termCurrency", "exchangeRate", "currency", "gross").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                .match("^zu versteuern \\(negativ\\) .*$")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    @SuppressWarnings("nls")
    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Konto.bersicht", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^Verrechnungskonto .* nach Buchungsdatum (?<currency>[\\w]{3})$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group(1));
            }
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^(SEPA-Gutschrift|SEPA-Lastschrift) [^Lastschrift].*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.DEPOSIT);
                    return transaction;
                })

                // SEPA-Gutschrift Max Mustermann 05.07.19 15.000,00
                // SEPA-Lastschrift Max Mustermann 15.07.19 300,00
                .section("note", "date", "amount")
                .match("^(?<note>(SEPA\\-Gutschrift|SEPA-Lastschrift)) [^Lastschrift].* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));

        Block feesBlock = new Block("^SEPA-Lastschrift Lastschrift Managementgeb.hr .*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.FEES);
                    return transaction;
                })

                // SEPA-Lastschrift Lastschrift Managementgebühr 29.06.20 53,02
                .section("note", "date", "amount")
                .match("^SEPA\\-Lastschrift Lastschrift (?<note>Managementgeb.hr) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a tax refunds,
        // we set a flag and don't book tax below.
        transaction
                .section("n").optional()
                .match("^zu versteuern \\(negativ\\) (?<n>.*)$")
                .assign((t, v) -> type.getCurrentContext().put("negative", "X"));

        transaction
                // ausländische Quellensteuer 15,315% JPY 95
                .section("withHoldingTax", "currency").optional()
                .match("^ausländische Quellensteuer .* ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<withHoldingTax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                })

                // abzgl. Quellensteuer 15,00 % von 281,25 USD 42,19 USD
                .section("withHoldingTax", "currency").optional()
                .match("^abzgl\\. Quellensteuer .* [\\w]{3} (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // davon anrechenbare US-Quellensteuer 15% EUR 0,79
                // davon anrechenbare Quellensteuer 15% ZAR 1.560,00
                // davon anrechenbare Quellensteuer Fondseingangsseite EUR 1,62
                // davon anrechenbare US-Quellensteuer  15% USD             2,430     
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^.*davon anrechenbare (US\\-)?Quellensteuer .* ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<creditableWithHoldingTax>[\\.,\\d]+)(\\-|[\\s]+)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                })

                // Börse Sekunden-Handel Fonds L&S Kapitalertragsteuer EUR 45,88-
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Kapitalertragsteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) 
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                        processTaxEntries(t, v, type);
                })

                // Kapitalertragsteuer EUR 14,51
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processTaxEntries(t, v, type);
                })

                // Verwahrart Girosammelverwahrung Solidaritätszuschlag EUR 2,52-
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Solidarit.tszuschlag ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) 
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltener"))
                        processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag EUR 0,79
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processTaxEntries(t, v, type);
                })

                // einbehaltene Kirchensteuer EUR 0,15
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Kirchensteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative"))
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer EUR 4,12-
                .section("tax", "currency").optional()
                .match("^Kirchensteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                        processTaxEntries(t, v, type);
                })

                // abzgl. Kapitalertragsteuer 25,00 % von 90,02 EUR 22,51 EUR
                .section("tax", "currency").optional()
                .match("^abzgl\\. Kapitalertrags(s)?teuer .* (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // abzgl. Solidaritätszuschlag 5,50 % von 22,51 EUR 1,23 EUR
                .section("tax", "currency").optional()
                .match("^abzgl\\. Solidarit.tszuschlag .* (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                .section("tax", "currency").optional()
                .match("^abzgl\\. Kirchensteuer .* (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelszeit 14:15* Registrierungsspesen EUR 0,58-
                .section("fee", "currency").optional()
                .match("^.* Registrierungsspesen ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelszeit 16:38* Provision USD 18,78-
                // Handelszeit 12:34* Provision                   EUR 10,00-
                .section("fee", "currency").optional()
                .match("^.* Provision ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börse USA/NAN Handelsplatzentgelt USD 17,44-
                .section("fee", "currency").optional()
                .match("^.* ([\\s]+)?Handelsplatzentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart Wertpapierrechnung Abwicklungskosten Ausland USD 0,10-
                .section("fee", "currency").optional()
                .match("^.* ([\\s]+)?Abwicklungskosten .* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart Girosammelverwahrung Gebühr EUR 0,50-
                .section("fee", "currency").optional()
                .match("^Verwahrart Girosammelverwahrung Geb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börse außerbörslich Bezugspreis EUR 27,00-
                .section("fee", "currency").optional()
                .match("^B.rse außerb.rslich Bezugspreis (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart Girosammelverwahrung Variabl. Transaktionsentgelt EUR 0,75-
                .section("fee", "currency").optional()
                .match("^.* ([\\s]+)?Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}