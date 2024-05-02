package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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
public class DABPDFExtractor extends AbstractPDFExtractor
{
    public DABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DAB Bank");
        addBankIdentifier("DAB bank AG");
        addBankIdentifier("BNP Paribas S.A. Niederlassung Deutschland");

        addBuySellTransaction();
        addDividendTransaction();
        addDeliveryInOutbountInTransaction();
        addTaxAdjustmentTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank / BNP Paribas S.A.";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf|Gesamtf.lligkeit)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Kauf|Verkauf|Gesamtf.lligkeit) .*$", "Dieser Beleg wird .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Verkauf|Gesamtf.lligkeit)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Gesamtfälligkeit".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N. LU0392495700
                                        // STK 43,000 EUR 47,8310
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // 4,75% Ranft Invest GmbH Inh.-Schv. 01.07.2030 01.07.2021 DE000A2LQLH9
                                        // v.2018(2030)
                                        // Nominal Kurs
                                        // EUR 1.000,000 100,0000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Bundesrep.Deutschland Bundesschatzanw. 15.03.2024 DE0001104875
                                        // v.22(24)
                                        // Nominal Einlösung zu:
                                        // EUR 10.000,000 100,0000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // HSBC Trinkaus & Burkhardt AG DIZ 27.08.21 27.08.2021 DE000TT649A1
                                        // Siemens 140
                                        // Nominal Kurs
                                        // STK 15,000 EUR 133,5700
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // HSBC Trinkaus & Burkhardt AG DIZ 27.08.21 27.08.2021 DE000TT649A1
                                        // Siemens 140
                                        // Nominal Kurs
                                        // STK 15,000 EUR 133,5700
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // STK 15,000 EUR 133,5700
                        // EUR 1.000,000 100,0000 %
                        // @formatter:on
                        .section("notation", "shares") //
                        .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} )?[\\.,\\d]+( %)?$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"STK".equalsIgnoreCase(v.get("notation")))
                            {
                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        // @formatter:off
                        // Handelszeit 16:38* Provision USD 13,01-
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // 07.12.2021 0000000000 EUR 0,05
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 08.01.2015 8022574001 EUR 150,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                        // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                        // @formatter:on
                        .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional() //
                        .match("^.* Kurswert[\\s]{1,}(?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)([\\-\\s]+)?$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Vorname Nachname Depot-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // Straße 1b 1234567890 12345678 / 28.12.2015 ADRESSZEILE6=12345 Stadt BELEGNUMMER=5847
                        //
                        // Depot-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // Herr 1234567 4527275 / 0 7.07.2020
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^.* (?<note>(Abrechnungs|Ausf.hrungs)\\-Nr\\.) .*$") //
                        .match("^.* (?<note1>.*) \\/ [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

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

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertr.gnisgutschrift|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Postfach .*|Ertr.gnisgutschrift (aus|VERSANDARTENSCHLUESSEL).*)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Paychex Inc. Registered Shares DL -,01 US7043261079
                                        // STK 10,000 31.07.2020 27.08.2020 USD 0,620000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wertpapierbezeichnung WKN ISIN
                                        // HORMEL FOODS CORP. Registered Shares DL 0,01465 850875 US4404521001
                                        // Dividende pro Stück 0,1875 USD Schlusstag 10.01.2018
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wertpapierbezeichnung WKN ISIN") //
                                                        .match("^(?<name>.*) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(Dividende pro St.ck|Ertragsaussch.ttung je Anteil) [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 10,000 31.07.2020 27.08.2020 USD 0,620000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^STK (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 1.500 Stück
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("(?<shares>[\\.,\\d]+) St.ck") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Valuta 15.02.2018 BIC CSDBDE71XXX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 08.01.2015 8022574001 EUR 150,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 30.03.2015 0000000000 EUR/ZAR 13,195 EUR 586,80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3}\\/[\\w]{3} .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 08.01.2015 8022574001 EUR 150,00
                                        // 27.08.2020 123456789 USD 4,64
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 30.03.2015 0000000000 EUR/ZAR 13,195 EUR 586,80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Netto in USD zugunsten IBAN DE90 209,38 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Netto in [\\w]{3} zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Netto zugunsten IBAN DE11 7603 0080 0111 1111 14 437,22 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Netto zugunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // ausländische Dividende EUR 5,25
                                        // Devisenkurs: EUR/USD 1,1814
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "exchangeRate", "termCurrency", "baseCurrency") //
                                                        .match("^ausl.ndische Dividende [\\w]{3}[\\s]{1,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^Devisenkurs: (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 01.12.2021 1234567891 EUR/JPY 127,99 EUR 3,59
                                        // ausländische Dividende EUR 4,84
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^ausl.ndische Dividende [\\w]{3}[\\s]{1,}(?<gross>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 29.09.2023 1234567890 EUR/USD 1,0611 EUR 21,38
                                        // Steuerpflichtiger Ausschüttungsbetrag EUR 28,74
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Steuerpflichtiger Aussch.ttungsbetrag [\\w]{3}[\\s]{1,}(?<gross>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 07.02.2017 1234567 EUR/USD 1,0797 EUR 11,06
                                        // Ertrag für 2016 USD 14,24
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Ertrag f.r [\\d]{4} [\\w]{3}[\\s]{1,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Brutto in USD 281,25 USD
                                        // Devisenkurs 1,249900 USD / EUR
                                        // Brutto in EUR 225,02 EUR
                                        // Netto in USD zugunsten IBAN DE90 209,38 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "gross", "exchangeRate", "baseCurrency", "fxGross") //
                                                        .match("^Brutto in [\\w]{3} (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3})$") //
                                                        .match("^Brutto in [\\w]{3} (?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Brutto in USD 600,00 USD
                                        // Devisenkurs 0,990800 USD / EUR
                                        // Brutto in EUR 605,56 EUR
                                        // Netto zugunsten IBAN DE90 1234 5678 1234 5678 90 450,84 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency", "gross") //
                                                        .match("^Brutto in [\\w]{3} (?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3})$") //
                                                        .match("^Brutto in [\\w]{3} (?<gross>[\\.,\\d]+) [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 30.06.2021 5325658010 USD 20,49
                                        // Ertrag für 2020/21 USD 25,11
                                        // Devisenkurs: EUR/USD 1,1914
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Ertrag f.r [\\d]{4}\\/[\\d]{2} [\\w]{3}[\\s]{1,}(?<gross>[\\.,\\d]+).*$") //
                                                        .match("^Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Hans Mustermann Depot-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // Musterstr. 1 123456789 989898989 / 14.05.2015 ADRESSZEILE6=12345 Musterstadt BELEGNUMMER=4879
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^.* (?<note>(Abrechnungs|Ausf.hrungs)\\-Nr\\.) .*$") //
                        .match("^.* (?<note1>.*) \\/ [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(t -> {
                            // If we have multiple entries in the document, then
                            // the "negative" flag must be removed.
                            type.getCurrentContext().remove("negative");

                            return new TransactionItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDeliveryInOutbountInTransaction()
    {
        DocumentType type = new DocumentType("(Einbuchung|Split|Freie Lieferung)");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Umtausch\\/Bezug|Wertpapier.bertrag|Split).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Lasten Ihres Depots" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Wir lieferten zu (?<type>Lasten Ihres Depots).*$") //
                        .assign((t, v) -> {
                            if ("Lasten Ihres Depots".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Solutiance AG Inhaber-Aktien o.N. DE0006926504
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)") //
                                                        .match("STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // BNP P.Easy-MSCI Pac.x.Jap.x.CW Nam.-Ant.UCITS ETF CAP EUR LU1291106356
                                        // o.N
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "name1") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<currency>[\\w]{3}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Ceramic Fuel Cells Ltd. Registered Shares o.N. AU000000CFU6
                                        // Nominal Schlusstag Wert Verbuchung in
                                        // STK 1.000,000 04.02.2022 04.02.2022 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .find("Nominal .*") //
                                                        .match("STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nominal Ex-Tag
                                        // STK 0,0722 04.12.2018
                                        // Nominal
                                        // STK 1,5884
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Nominal") //
                                                        .match("STK (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // STK 20,000 EUR 1,3500
                                        // STK 1.000,000 04.02.2022 04.02.2022 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("STK (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 11.03.2021 3331111113 EUR 27,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // STK 0,0722 04.12.2018
                                        // STK 1.000,000 04.02.2022 04.02.2022 EU
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 11.03.2021 3331111113 EUR 27,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Freie Lieferung VERSANDARTENSCHLUESSEL=DRUCKADRESSZEILE1=Herr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Freie Lieferung) .*$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Wir haben Ihrem Depot im Verhältnis 1 : 22 folgende Stücke zugebucht (hinzugefügt):
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^Wir haben Ihrem Depot im (?<type>Verh.ltnis) [\\d]+ : [\\d]+ .*$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }))

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // 3331111113 84747170 / 19.03.2021 ADRESSZEILE6=
                        //
                        // Depot-Nr. Ausführungs-Nr. ADRESSZEILE5=
                        // 123456789 12345678 / 03.12.2018 ADRESSZEILE6=
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^.* (?<note>(Abrechnungs|Ausf.hrungs)\\-Nr\\.) .*$") //
                        .match("^.* (?<note1>.*) \\/ [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1"))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxAdjustmentTransaction()
    {
        DocumentType type = new DocumentType("Steuerausgleich f.r das Jahr [\\d]{4}");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Kunden\\-Nr\\. .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wert Betrag zu Ihren Gunsten
                        // 05.05.2023 EUR 143,90
                        // @formatter:on
                        .section("date") //
                        .find("Wert Betrag zu Ihren Gunsten") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Wert Betrag zu Ihren Gunsten
                        // 05.05.2023 EUR 143,90
                        // @formatter:on
                        .section("currency", "amount") //
                        .find("Wert Betrag zu Ihren Gunsten") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kunden-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // 1234567 12345678 / 05.05.2023 ADRESSZEILE6=
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^Kunden\\-Nr\\. (?<note>Abrechnungs\\-Nr\\.) .*$") //
                        .match("^[\\d]+ (?<note1>.*) \\/.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1"))))

                        // @formatter:off
                        // Steuerausgleich für das Jahr 2023
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^(?<note>Steuerausgleich) f.r das Jahr (?<note1>[\\d]{4})$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ") + " " + trim(v.get("note1"))))

                        .wrap(TransactionItem::new);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Steuerbelastung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Steuerbelastung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gattungsbezeichnung ISIN
                        // iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N. IE00B4L5Y983
                        // STK 50,000 02.01.2024 02.01.2024 EUR 1,2370
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .find("Gattungsbezeichnung ISIN")
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // STK 50,000 02.01.2024 02.01.2024 EUR 1,2370
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 15.01.2024 3354983003 EUR 11,43
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // 15.01.2024 3354983003 EUR 11,43
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr. ADRESSZEILE5=
                        // 0784962423 92956682 / 15.01.2024 ADRESSZEILE6=
                        // @formatter:on
                        .section("note", "note1").optional() //
                        .match("^Depot\\-Nr\\. (?<note>Abrechnungs\\-Nr\\.) .*$") //
                        .match("^[\\d]+ (?<note1>.*) \\/.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1"))))

                        .wrap(TransactionItem::new);
    }

    private void addBuySellTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Kauf|Verkauf|Gesamtf.lligkeit) .*$", "Dieser Beleg wird .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N. LU0392495700
                                        // STK 43,000 EUR 47,8310
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // 4,75% Ranft Invest GmbH Inh.-Schv. 01.07.2030 01.07.2021 DE000A2LQLH9
                                        // v.2018(2030)
                                        // Nominal Kurs
                                        // EUR 1.000,000 100,0000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Bundesrep.Deutschland Bundesschatzanw. 15.03.2024 DE0001104875
                                        // v.22(24)
                                        // Nominal Einlösung zu:
                                        // EUR 10.000,000 100,0000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // HSBC Trinkaus & Burkhardt AG DIZ 27.08.21 27.08.2021 DE000TT649A1
                                        // Siemens 140
                                        // Nominal Kurs
                                        // STK 15,000 EUR 133,5700
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // WisdomTree Multi Ass.Iss.PLC Gas 3x Sh. 05.12.2062 IE00BLRPRL42
                                        // ETP Secs 12(12/62)
                                        // Nominal Kurs
                                        // STK 1,000 EUR 111,1111
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch\\. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // STK 1,000 EUR 111,1111
                        // @formatter:on
                        .section("notation", "shares") //
                        .match("^(?<notation>[\\w]{3}) (?<shares>[\\.,\\d]+) ([\\w]{3} )?[\\.,\\d]+( %)?$") //
                        .assign((t, v) -> {
                            // Percentage quotation, workaround for bonds
                            if (v.get("notation") != null && !"STK".equalsIgnoreCase(v.get("notation")))
                            {
                                BigDecimal shares = asBigDecimal(v.get("shares"));
                                t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 07.12.2021 0000000000 EUR 0,05
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // 27.08.2015 0000000000 EUR/USD 1,162765 EUR 4.465,12
                        // zu versteuern (negativ) EUR 59,20
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                        // 07.07.2020 1234567 1234567 EUR 16,46
                        // @formatter:on
                        .section("currency", "amount").optional() //
                        .find("zu versteuern \\(negativ\\).*") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // 27.08.2015 0000000000 EUR/USD 1,162765 EUR 4.465,12
                        // zu versteuern (negativ) EUR 341,55
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                        // 24.08.2015 0000000000 00000000 EUR 90,09
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                        .find("zu versteuern \\(negativ\\) .*") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Verm.gensbericht|Verm.gensstatus)", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // Verrechnungskonto 0000000000 nach Buchungsdatum EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Verrechnungskonto .* nach Buchungsdatum (?<currency>[\\w]{3})$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                        // @formatter:off
                                                        // Referenzwährung Euro
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Referenzw.hrung (?<currency>.*)$") //
                                                                        .assign((ctx, v) -> {
                                                                            if ("Euro".equals(trim(v.get("currency")))) ctx.put("currency", "EUR");
                                                                        })));

        this.addDocumentTyp(type);

        // @formatter:off
        // SEPA-Gutschrift Max Mustermann 05.07.19 15.000,00
        // SEPA-Lastschrift Max Mustermann 15.07.19 300,00
        // @formatter:on
        Block depositBlock_Format01 = new Block("^(SEPA\\-Gutschrift|SEPA\\-Lastschrift) (?!.*Lastschrift).* [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositBlock_Format01);
        depositBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note>(SEPA\\-Gutschrift|SEPA\\-Lastschrift)) (?!.*Lastschrift).* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // SEPA-Überweisung MUSTERMANN, MAX 05.07.19 15.000,00  // no minus sign!
        // SEPA-Dauerauftrag MUSTERMANN, MAX 15.07.19 300,00    // no minus sign!
        // @formatter:on
        Block removalBlock_Format01 = new Block("^(SEPA\\-.berweisung|SEPA\\-Dauerauftrag) .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(removalBlock_Format01);
        removalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<note>(SEPA\\-.berweisung|SEPA\\-Dauerauftrag)) .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Belastung Porto 05.04.16 0,70  // no minus sign!
        // @formatter:on
        Block feesBlock_Format01 = new Block("^Belastung .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(feesBlock_Format01);
        feesBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^Belastung (?<note>.*) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 23.09.2022 23.09.2022 SEPA-Überweisung Anlage 2.000,00 EUR
        // @formatter:on
        Block depositBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} SEPA\\-.berweisung .* [\\.,\\d]+ [\\w]{3}$");
        type.addBlock(depositBlock_Format02);
        depositBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>SEPA\\-.berweisung) .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 23.09.2022 23.09.2022 SEPA-Überweisung Entsparen -2.000,00 EUR
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} SEPA\\-.berweisung .* -[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>SEPA\\-.berweisung) .* -(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 23.09.2022 23.09.2022 Sollzinsen -100,00 EUR
        // @formatter:on
        Block interestChargeBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Sollzinsen -[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(interestChargeBlock);
        interestChargeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Sollzinsen) -(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // SEPA-Lastschrift Lastschrift Managementgebühr 29.06.20 53,02
        // @formatter:on
        Block feesBlock = new Block("^SEPA\\-Lastschrift Lastschrift Managementgeb.hr .*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^SEPA\\-Lastschrift Lastschrift (?<note>Managementgeb.hr) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a tax refunds,
        // we set a flag and don't book tax below.
        transaction //

                        .section("n").optional() //
                        .match("^zu versteuern \\(negativ\\) (?<n>.*)$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("negative", true));

        transaction //

                        // @formatter:off
                        // ausländische Quellensteuer 15,315% JPY 95
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^ausl.ndische Quellensteuer .*[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // abzgl. Quellensteuer 15,00 % von 281,25 USD 42,19 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^abzgl\\. Quellensteuer .* [\\w]{3} (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // davon anrechenbare US-Quellensteuer 15% EUR 0,79
                        // davon anrechenbare Quellensteuer 15% ZAR 1.560,00
                        // davon anrechenbare Quellensteuer Fondseingangsseite EUR 1,62
                        // davon anrechenbare US-Quellensteuer  15% USD             2,430
                        // @formatter:on
                        .section("currency", "creditableWithHoldingTax").optional() //
                        .match("^.*davon anrechenbare (US\\-)?Quellensteuer .*[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<creditableWithHoldingTax>[\\.,\\d]+)(\\-|[\\s]+)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // Börse Sekunden-Handel Fonds L&S Kapitalertragsteuer EUR 45,88-
                        // @formatter:on
                        .section("label", "currency", "tax").optional() //
                        .match("^(?<label>.*) Kapitalertrags(s)?teuer[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer EUR 14,51
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Verwahrart Girosammelverwahrung Solidaritätszuschlag EUR 2,52-
                        // @formatter:on
                        .section("label", "currency", "tax").optional() //
                        .match("^(?<label>.*) Solidarit.tszuschlag[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltener"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag EUR 0,79
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltene Kirchensteuer EUR 0,15
                        // @formatter:on
                        .section("label", "currency", "tax").optional() //
                        .match("^(?<label>.*) Kirchensteuer[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer EUR 4,12-
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+)(\\-)?$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // abzgl. Kapitalertragsteuer 25,00 % von 90,02 EUR 22,51 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Kapitalertrags(s)?teuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // abzgl. Solidaritätszuschlag 5,50 % von 22,51 EUR 1,23 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Solidarit.tszuschlag [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // abzgl. Solidaritätszuschlag 9,00 % von 10,00 EUR 0,90 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Kirchensteuer [\\.,\\d]+ % von [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // abzgl. Kapitalertragsteuer 148,29 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Kapitalertrags(s)?teuer (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // abzgl. Solidaritätszuschlag 8,14 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Solidarit.tszuschlag (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // abzgl. Kirchensteuer 13,34 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abzgl\\. Kirchensteuer (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Handelszeit 14:15* Registrierungsspesen EUR 0,58-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Registrierungsspesen[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelszeit 16:38* Provision USD 18,78-
                        // Handelszeit 12:34* Provision                   EUR 10,00-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Provision[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börse USA/NAN Handelsplatzentgelt USD 17,44-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*[\\s]{1,}Handelsplatzentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Verwahrart Wertpapierrechnung Abwicklungskosten Ausland USD 0,10-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*[\\s]{1,}Abwicklungskosten .* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Verwahrart Girosammelverwahrung Gebühr EUR 0,50-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Verwahrart Girosammelverwahrung Geb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börse außerbörslich Bezugspreis EUR 27,00-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rse außerb.rslich Bezugspreis (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Verwahrart Girosammelverwahrung Variabl. Transaktionsentgelt EUR 0,75-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*[\\s]{1,}Transaktionsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}