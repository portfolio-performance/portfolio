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
public class OnvistaPDFExtractor extends AbstractPDFExtractor
{
    public OnvistaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("onvista bank");
        addBankIdentifier("OnVista Bank");
        addBankIdentifier("onvist a bank");
        addBankIdentifier("service@onvista-bank.de");
        addBankIdentifier("Frankfurt am Main,");

        addBuySellTransaction();
        addReinvestTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addDeliveryInOutBoundTransaction();
        addRegistrationFeeTransaction();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "OnVista Bank GmbH";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Kauf" //
                        + "|Verkauf" //
                        + "|Scheme of Arrangement" //
                        + "|Gesamtf.lligkeit" //
                        + "|Zwangsabfindung" //
                        + "|Dividende)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Spitze )?" //
                        + "(Kauf" //
                        + "|Verkauf" //
                        + "|Gesamtf.lligkeit" //
                        + "|Abrechnung)" //
                        + "( .*)?$");
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
                        .match("(^|Wir haben für Sie )" //
                                        + "(?<type>verkauft" //
                                        + "|Gesamtf.lligkeit" //
                                        + "|Ausbuchung:)" //
                                        + "( .*)?$") //
                        .assign((t, v) -> {
                            if ("verkauft".equals(v.get("type")) //
                                            || "Gesamtfälligkeit".equals(v.get("type")) //
                                            || "Ausbuchung:".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Sky Deutschland AG Namens-Aktien o.N. DE000SKYD000
                                        // Nominal Ex-Tag
                                        // Abfindung zu:
                                        // EUR 6,680000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)") //
                                                        .find("Abfindung zu:") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // DWS Deutschland Inhaber-Anteile LC DE0008490962
                                        // STK 0,7445 EUR 200,1500
                                        // Nominal Ex-Tag
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                                        // Fres. SE
                                        // STK 65,000 EUR 39,4400
                                        // Nominal Ex-Tag
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Société Générale Effekten GmbH DISC.Z 13.05.2021 DE000SE8F9E8
                                        // 13.05.21 NVIDIA 498
                                        // Nominal Kurs
                                        // STK 4,000 EUR 480,6300
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // 11,5% UniCredit Bank AG HVB Aktienanleihe 24.11.2023 24.11.2023 DE000HB4GEE2
                                        // v.22(23)SDF
                                        // Nominal Kurs
                                        // EUR 1.000,000 90,1800 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 65,000 EUR 39,4400
                                        // STK 25,000 22.09.2015
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^STK (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominal Kurs
                                        // EUR 1.000,000 90,1800 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                        // @formatter:off
                                                        // Percentage quotation, workaround for bonds
                                                        // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Handelszeit 15:30 Orderprovision USD 11,03-
                        // Handelszeit 12:00
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelstag 15.08.2019 Kurswert EUR 149,01-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // 25.09.2020 123456789 EUR 2.563,60
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 19.08.2019 123450042 EUR 150,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 21.08.2019 372650044 EUR/USD 1,1026 EUR 1.536,13
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Handelstag 19.08.2019 Kurswert USD 1.677,20-
                        // 21.08.2019 372650044 EUR/USD 1,1026 EUR 1.536,13
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^.* Kurswert [\\w]{3} (?<fxGross>[\\.,\\d]+).*$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr.
                        // 12345 Musterstadt 123456000 10283354 / 24.09.2020 SEITENNUMMER=1STEUERERSTATTUNG=N
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.* (?<note1>(Abrechnungs|Ausf.hrungs)\\-Nr\\.).*$") //
                        .match("^.* (?<note2>[\\d]+) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Zwangsabfindung gemäß Hauptversammlungsbeschluss vom 22. Juli 2015. Der Übertragungsbeschluss wurde am 15.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Zwangsabfindung gem.ß Hauptversammlungsbeschluss .*) Der .bertragungsbeschluss .*$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Stückzinsaufwand EUR 82,55
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>St.ckzinsaufwand [\\w]{3} [\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(t -> {
                            // If we have multiple entries in the document, with
                            // taxes and tax refunds, then the "negative" flag
                            // must be removed.
                            type.getCurrentContext().remove("negative");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void addReinvestTransaction()
    {
        final DocumentType type = new DocumentType("Reinvestierung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Reinvestierung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // If we have a reinvest we pick the second
                        //
                        // Gattungsbezeichnung ISIN
                        // Deutsche Telekom AG Namens-Aktien o.N. DE0005557508
                        // Die Dividende wurde wie folgt in neue Aktien reinvestiert:
                        // Gattungsbezeichnung ISIN
                        // Deutsche Telekom AG Dividend in Kind-Cash Line DE000A1TNRX5
                        // Nominal Reinvestierungspreis
                        // STK 25,000 EUR 0,700000
                        // @formatter:on
                        .section("name", "isin", "name1", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)") //
                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominal"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 75,000 16.01.2020 13.03.2020 GBP 0,240000
                        // STK 1,000 GBP 14,910000
                        // @formatter:on
                        .section("shares") //
                        .find("^STK [\\.,\\d]+ .*$") //
                        .match("^STK (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zahlungstag 17.05.2013
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zahlungstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 13.03.2020 123456789 EUR/GBP 0,88295 EUR 6,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Leistungen aus dem steuerlichen Einlagenkonto (§27 KStG) EUR 17,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Leistungen aus dem steuerlichen Einlagenkonto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // ausländische Dividende EUR 20,39
                                        // Barausgleich GBP 3,09
                                        // Gebühren GBP 8,83-
                                        // 13.03.2020 123456789 EUR/GBP 0,88295 EUR 6,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("dividendCurrency", "dividendAmount", //
                                                                        "currencyAmountReturn", "amountReturn", //
                                                                        "feeCurrency", "feeAmount", //
                                                                        "baseCurrency", "termCurrency",  //
                                                                        "exchangeRate", "currency") //
                                                        .match("^ausl.ndische Dividende (?<dividendCurrency>[\\w]{3}) (?<dividendAmount>[\\.,\\d]+)$") //
                                                        .match("^Barausgleich (?<currencyAmountReturn>[\\w]{3}) (?<amountReturn>[\\.,\\d]+)$") //
                                                        .match("^Geb.hren (?<feeCurrency>[\\w]{3}) (?<feeAmount>[\\.,\\d]+)\\-$") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            Money dividend = Money.of(asCurrencyCode(v.get("dividendCurrency")), asAmount(v.get("dividendAmount")));
                                                            Money amountReturn = Money.of(asCurrencyCode(v.get("currencyAmountReturn")), asAmount(v.get("amountReturn")));
                                                            Money fee = Money.of(asCurrencyCode(v.get("currencyAmountReturn")), asAmount(v.get("feeAmount")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            if ("currency".equals(dividend.getCurrencyCode()))
                                                                dividend = rate.convert(rate.getBaseCurrency(), dividend);

                                                            if (!"currency".equals(amountReturn.getCurrencyCode()))
                                                                amountReturn = rate.convert(rate.getBaseCurrency(), amountReturn);

                                                            if (!"currency".equals(fee.getCurrencyCode()))
                                                                fee = rate.convert(rate.getBaseCurrency(), fee);

                                                            t.setMonetaryAmount(dividend.subtract(amountReturn).add(fee));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // ausländische Dividende EUR 20,39
                                        // Barausgleich GBP 3,09
                                        // 13.03.2020 123456789 EUR/GBP 0,88295 EUR 6,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("dividendCurrency", "dividendAmount", //
                                                                        "currencyAmountReturn", "amountReturn", //
                                                                        "feeCurrency", "feeAmount",  //
                                                                        "baseCurrency", "termCurrency",  //
                                                                        "exchangeRate", "currency") //
                                                        .match("^ausl.ndische Dividende (?<dividendCurrency>[\\w]{3}) (?<dividendAmount>[\\.,\\d]+)$") //
                                                        .match("^Barausgleich (?<currencyAmountReturn>[\\w]{3}) (?<amountReturn>[\\.,\\d]+)$") //
                                                        .match("^Geb.hren (?<feeCurrency>[\\w]{3}) (?<feeAmount>[\\.,\\d]+)\\-$") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            Money dividend = Money.of(asCurrencyCode(v.get("dividendCurrency")), asAmount(v.get("dividendAmount")));
                                                            Money amountReturn = Money.of(asCurrencyCode(v.get("currencyAmountReturn")), asAmount(v.get("amountReturn")));
                                                            Money fees = Money.of(asCurrencyCode(v.get("currencyAmountReturn")), asAmount(v.get("feeAmount")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            if ("currency".equals(dividend.getCurrencyCode()))
                                                                dividend = rate.convert(rate.getBaseCurrency(), dividend);

                                                            if (!"currency".equals(amountReturn.getCurrencyCode()))
                                                                amountReturn = rate.convert(rate.getBaseCurrency(), amountReturn);

                                                            if (!"currency".equals(fees.getCurrencyCode()))
                                                                fees = rate.convert(rate.getBaseCurrency(), fees);

                                                            Money gross = dividend.subtract(amountReturn).add(fees);
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr.
                        // 54321 Musterhausen 173458000 17299829 / 17.05.2013 SEITENNUMMER=1STEUERERSTATTUNG=N
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.* (?<note1>(Abrechnungs|Ausf.hrungs)\\-Nr\\.).*$") //
                        .match("^.* (?<note2>[\\d]+) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        // @formatter:off
                        // Reinvestierung ADRESSZEILE4=54321 Musterhausen
                        // Gattungsbezeichnung ISIN
                        // Deutsche Telekom AG Namens-Aktien o.N. DE0005557508
                        // @formatter:on
                        .section("note", "isin").optional() //
                        .match("^(?<note>Reinvestierung) .*$") //
                        .find("Gattungsbezeichnung .*") //
                        .match("^.* (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note") + ": " + v.get("isin"), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Dividendengutschrift" //
                        + "|Ertr.gnisgutschrift" //
                        + "|Kupongutschrift" //
                        + "|Kapitalr.ckzahlung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Ertr.gnisgutschrift aus Wertpapieren" //
                        + "|aus Wertpapieren" //
                        + "|Kapitalr.ckzahlung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Ertragsthesaurierung" change from DIVIDENDS to TAXES
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Ertragsthesaurierung) .*$") //
                        .match("^Steuerliquidit.t [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setType(AccountTransaction.Type.TAXES))

                        // @formatter:off
                        // Storno - Dividendengutschrift
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Storno) \\- Dividendengutschrift$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                                        // Nominal Ex-Tag Zahltag Dividenden-Betrag pro Stück
                                        // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Garmin Ltd. Namens-Aktien SF 0,10 CH0114405324
                                        // Ausschüttungsbetrag pro Stück
                                        // USD 0,730000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .find("Aussch.ttungsbetrag pro St.ck") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit Zinstermin ISIN
                                        // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014 17.11.2010 DE000TUAG117
                                        // Nominal Zahltag Zinssatz
                                        // STK 1,000 17.11.2010 EUR 1,548250
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // 5,875% Telefónica Europe B.V. 14.02.2033 14.02.2020 XS0162869076
                                        // EO-Medium-Term Notes 2003(33)
                                        // Nominal Zahltag Zinssatz
                                        // EUR 5.000,000 14.02.2020 5,875000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 15,000
                                        // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                                        // STK 1,000 17.11.2010 EUR 1,548250
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^STK (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares((asShares(v.get("shares"))))),
                                        // @formatter:off
                                        // EUR 5.000,000 14.02.2020 5,875000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                                        // STK 1,000 17.11.2010 EUR 1,548250
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^STK [\\.,\\d]+( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // EUR 5.000,000 14.02.2020 5,875000 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 21.04.2016 172306238 EUR 10,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 30.09.2022 111111111 EUR/USD 0,98128 EUR 8,21
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        //
                                        // Reinvestierung ADRESSZEILE2=Max Mustermann
                                        // ausländische Dividende EUR 20,39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Reinvestierung .*") //
                                                        .match("^ausl.ndische Dividende (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        //
                                        // Reinvestierung ADRESSZEILE4=54321 Musterhausen
                                        // Leistungen aus dem steuerlichen Einlagenkonto (§27 KStG) EUR 17,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Reinvestierung .*") //
                                                        .match("^Leistungen aus dem steuerlichen Einlagenkonto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // This is for the "Ertragsthesaurierung"
                                        //
                                        // Ertragsthesaurierung Frankfurt am Main, 19.10.2017
                                        // Steuerliquidität EUR 0,02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Ertragsthesaurierung .*") //
                                                        .match("^Steuerliquidit.t (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // This is for the "Ertragsthesaurierung" without importable amount,
                                        // then we skip the transaction
                                        //
                                        // Ertragsthesaurierung Frankfurt am Main, 17.12.2015
                                        // Steuerpflichtiger Betrag gem.§ 2 Abs. 1 InvStG EUR 3,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Ertragsthesaurierung .*") //
                                                        .match("^Steuerpflichtiger Betrag gem\\..*InvStG (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            type.getCurrentContext().putBoolean("skipTransaction", true);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 21.04.2016 172306238 EUR 10,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 15.07.2019 123456789 EUR/USD 1,1327 EUR 13,47
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\w]{3}\\/[\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // This is for the reinvestment of dividends
                                        //
                                        // Reinvestierung ADRESSZEILE2=Max Mustermann
                                        // ausländische Dividende EUR 20,39
                                        // 13.03.2020 123456789 EUR/GBP 0,88295 EUR 6,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .find("Reinvestierung .*") //
                                                        .match("^ausl.ndische Dividende [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 05.02.2019 000000000 EUR/USD 1,1474 EUR 39,60
                                        // Ertrag für 2018 USD 45,44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Ertrag f.r [\\d]{4}(\\/[\\d]{2})? [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 15.07.2019 123456789 EUR/USD 1,1327 EUR 13,47
                                        // ausländische Dividende EUR 18,10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^ausl.ndische Dividende [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Barausgleich USD 10,95
                                        // 30.09.2022 111111111 EUR/USD 0,98128 EUR 8,21
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Barausgleich [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 01.06.2021 30000000 EUR/USD 1,08985 EUR 13,73
                                        // Steuerpflichtiger Ausschüttungsbetrag EUR 16,14
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Steuerpflichtiger Aussch.ttungsbetrag [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr.
                        // 54321 Musterhausen 173458000 17299829 / 17.05.2013 SEITENNUMMER=1STEUERERSTATTUNG=N
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.* (?<note1>(Abrechnungs|Ausf.hrungs)\\-Nr\\.).*$") //
                        .match("^.* (?<note2>[\\d]+) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Ertrag für 2018 USD 45,44
                                        // Ertrag für 2016/17 EUR 1,80
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Ertrag f.r [\\d]{4}(\\/[\\d]{2,4})?).*$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Kapitalrückzahlung:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Kapitalr.ckzahlung):.*$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Storno unserer Dividendengutschrift Nr. 67390000 vom 15.05.2020.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Storno unserer Dividendengutschrift Nr\\. .*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            // If we have multiple entries in the document, with
                            // taxes and tax refunds, then the "negative" flag
                            // must be removed.
                            type.getCurrentContext().remove("negative");

                            // If we have a gross reinvestment, then the "noTax"
                            // flag must be removed.
                            type.getCurrentContext().remove("noTax");

                            if (type.getCurrentContext().getBoolean("skipTransaction"))
                                return null;

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        final DocumentType type = new DocumentType("Steuerpflichtige Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Steuerbelastung .*$");
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
                        // Deka DAX UCITS ETF Inhaber-Anteile DE000ETFL011
                        // Nominal Ex-Tag Zahltag Jahreswert Vorabpauschale pro Stück
                        // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                        // @formatter:on
                        .section("name", "isin", "name1", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)$") //
                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominal"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                        // @formatter:on
                        .section("date") //
                        .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // 10.01.2020 123456789 EUR 0,02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // If all taxes are covered by Freistellungsauftrag/Verlusttopf,
                                        // section "Wert Konto-Nr. Betrag zu Ihren Lasten" is not present,
                                        // then extract currency here
                                        //
                                        // STK 0,4298 02.01.2020 02.01.2020 EUR 0,3477
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency") //
                                                        .match("^STK [\\.,\\d]+ .* (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(0L);
                                                        }))

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr.
                        // XXXXXX XXXXXX 35XXXXXXX 21408694 / 10.01.2020 SEITENNUMMER=1STEUERERSTATTUNG=N
                        //
                        // Depot-Nr. Ausführungs-Nr.
                        // 12345 Musterstadt 35XXXXXXX 82128903 / 09.01.2020 SEITENNUMMER=1STEUERERSTATTUNG=N
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.* (?<note1>(Abrechnungs|Ausf.hrungs)\\-Nr\\.).*$") //
                        .match("^.* (?<note2>[\\d]+) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addDeliveryInOutBoundTransaction()
    {
        final DocumentType type = new DocumentType("Wertlose Ausbuchung");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertlose Ausbuchung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Wertlose Ausbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Wertlose Ausbuchung).*$") //
                        .assign((t, v) -> {
                            if ("Wertlose Ausbuchung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        // @formatter:off
                        // Ausbuchung:
                        // Gattungsbezeichnung ISIN
                        // Deutsche Telekom AG Dividend in Kind-Cash Line DE000A2AA2C3
                        // Nominal Ex-Tag
                        // @formatter:on
                        .section("name", "isin", "name1") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominal"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 25,000 21.06.2016
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // STK 25,000 21.06.2016
                        // @formatter:on
                        .section("date") //
                        .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Wertlose Ausbuchung ADRESSZEILE1=Herr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Wertlose Ausbuchung).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }))

                        .wrap(TransactionItem::new);
    }

    private void addRegistrationFeeTransaction()
    {
        final DocumentType type = new DocumentType("Registrierungsgeb.hr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Registrierung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gattungsbezeichnung ISIN
                        // Vonovia SE Namens-Aktien o.N. DE000A1ML7J1
                        // @formatter:on
                        .section("name", "isin", "name1") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominal"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 6,000 22.07.2017
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 24.07.2017 172406048 EUR 0,8
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // 24.07.2017 172406048 EUR 0,89
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Depot-Nr. Abrechnungs-Nr.
                        // 54321 Musterhausen 58205100 63550522 / 24.07.2017 SEITENNUMMER=1STEUERERSTATTUNG=N
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.* (?<note1>(Abrechnungs|Ausf.hrungs)\\-Nr\\.).*$") //
                        .match("^.* (?<note2>[\\d]+) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        // @formatter:off
                        // Für die Registrierung der Namens-Aktien (auf Ihren Namen) im Aktionärs-Register belasten wir Ihrem Konto vorstehenden
                        // @formatter:on
                        .section("note").optional() //
                        .match("^F.r die (?<note>Registrierung der Namens\\-Aktien) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(t -> {
                            // If we have multiple entries in the document, with
                            // taxes and tax refunds, then the "negative" flag
                            // must be removed.
                            type.getCurrentContext().remove("negative");

                            return new TransactionItem(t);
                        });
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Kontoauszug|KONTOAUSZUG) Nr\\.", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // EUR - Verrechnungskonto: 0 111111 222
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^(?<currency>[\\w]{3}) - Verrechnungskonto: .*$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                        // @formatter:off
                                                        // Ihre Kontonummer : 172306238 : Customer Cash Account  EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^.* Customer Cash Account ([\\s]+)?(?<currency>[\\w]{3})$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))))
                                        .oneOf( //
                                                        // @formatter:off
                                                        // Kontoauszug Nr. 2017 / 2 und Rechnungsabschluss zum 30.06.2017
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("year") //
                                                                        .match("^(Kontoauszug|KONTOAUSZUG) Nr\\. (?<year>[\\d]{4}) / [\\d]+ .*$") //
                                                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))),
                                                        // @formatter:off
                                                        // KONTOAUSZUG Nr. 2 per 30.06.2015
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("year") //
                                                                        .match("^(Kontoauszug|KONTOAUSZUG) Nr\\. [\\d]+ per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4})$") //
                                                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))))
                                        );

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 04.04. 04.04. REF: 000045862247 200,00+
                        // Überweisungseingang SEPA Max Mustermann
                        //
                        // 22.05. 22.05. REF: 912315845658 60.000,00-
                        // Übertrag Referenzkonto
                        // @formatter:on
                        .section("date", "amount", "note", "type").optional() //
                        .documentContext("year", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+)(?<type>([\\+|\\-]))$") //
                        .match("^(?<note>(.berweisung(seingang|ausgang) SEPA|.bertrag Referenzkonto)).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 31.10. 31.10. REF: 000017304356 37,66
                        // Saldenübernahme Nordnet
                        //
                        // 19.07. 19.07. REF: 000040720216 5,00
                        // Überweisungseingang SEPA Max Muster
                        // @formatter:on
                        .section("date", "amount", "note").optional() //
                        .documentContext("year", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. .* (?<amount>[\\.,\\d]+)$") //
                        .match("^(?<note>(Salden.bernahme|Überweisungseingang SEPA)).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 07.04. 03.04. REF: 000033640646 0,62-
                        // Portogebühren
                        // Portogebuehren 03/15
                        //
                        // 24.03. 23.03. REF: 000137060674 42,42+
                        // Erst. BGH-Urteil Sonstige
                        //
                        // 11.05. 03.05. REF: 000057140531 0,70+
                        // Storno: Portogebühren
                        // Portogebuehren 04/18
                        // @formatter:on
                        .section("date", "amount", "type", "note1", "note2").optional() //
                        .documentContext("year", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. REF: [\\d]+ (?<amount>[\\.,\\d]+)(?<type>([\\+|\\-]))$") //
                        .match("^(?<note1>((Storno: )?Portogeb.hren|Erst\\. BGH\\-Urteil Sonstige))$") //
                        .match("^(Portogebuehren )?(?<note2>([\\d]{2}\\/[\\d]{2}|[\\d]{1}\\. Quartal [\\d]{4}))$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from FEES to FEES_REFUND
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES);
                            else
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note1") + " " + v.get("note2"));
                        })

                        // @formatter:off
                        // 02.05. 02.05. REF: 337909771410 7,50-
                        // Geb. Back Office extern
                        // @formatter:on
                        .section("date", "amount", "type", "note").optional() //
                        .documentContext("year", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. REF: [\\d]+ (?<amount>[\\.,\\d]+)(?<type>([\\+|\\-]))$") //
                        .match("^(?<note>Geb\\. Back Office extern)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from FEES to FEES_REFUND
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES);
                            else
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 31.03. 30.03. REF: 000137802265 0,77-
                        // Überziehungszinsen
                        // @formatter:on
                        .section("date", "amount", "type", "note").optional() //
                        .documentContext("year", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. REF: [\\d]+ (?<amount>[\\.,\\d]+)(?<type>([\\+|\\-]))$") //
                        .match("^(?<note>.berziehungszinsen)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            else
                                t.setType(AccountTransaction.Type.INTEREST);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(Freier Erhalt" //
                        + "|Freie Lieferung" //
                        + "|Einbuchung von Rechten" //
                        + "|Fusion" //
                        + "|Kapitalerh.hung" //
                        + "|Kapitalherabsetzung" //
                        + "|Umtausch" //
                        + "|Im Zuge der Geldzahlung erfolgt die Ausbuchung der Rechte" //
                        + "|Reverse Split)", //
                        "(Kontoauszug|KONTOAUSZUG) Nr\\.");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Freier Erhalt" //
                        + "|Freie Lieferung" //
                        + "|Einbuchung von Rechten" //
                        + "|Fusion" //
                        + "|Kapitalerh.hung" //
                        + "|Kapitalherabsetzung" //
                        + "|Umtausch" //
                        + "|Dividendengutschrift" //
                        + "|Reverse Split)(?! im Verh.ltnis).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Is type --> "Freie Lieferung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        // Is type --> "Fusion" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        // Is type --> "Kapitalherabsetzung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        // Is type --> "Ausbuchung:" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>(Freier Erhalt" //
                                                                        + "|Freie Lieferung" //
                                                                        + "|Einbuchung von Rechten" //
                                                                        + "|Fusion" //
                                                                        + "|Kapitalerh.hung" //
                                                                        + "|Kapitalherabsetzung" //
                                                                        + "|Ausbuchung:)).*$") //
                                                        .assign((t, v) -> {
                                                            if ("Freie Lieferung".equals(v.get("type")) //
                                                                            || "Fusion".equals(v.get("type")) //
                                                                            || "Kapitalherabsetzung".equals(v.get("type")) //
                                                                            || "Ausbuchung:".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                                                        }),
                                        // @formatter:off
                                        // Im Zuge der Geldzahlung erfolgt die Ausbuchung der Rechte. Ein separater Beleg wird nicht erstellt.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^.*(?<type>Ausbuchung der Rechte).*$") //
                                                        .assign((t, v) -> {
                                                            if ("Ausbuchung der Rechte".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                                                        }))

                        // @formatter:off
                        // Gattungsbezeichnung ISIN
                        // Allianz PIMCO Euro Bd Tot.Ret. Inhaber-Anteile A (EUR) o.N. LU0140355917
                        // Nominal Schlusstag Wert
                        // @formatter:on
                        .section("name", "isin", "name1") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(?<name1>.*)") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Nominal"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 50,000 07.02.2024 08.02.2024
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 6,000 23.05.2022 25.05.2022 EUR 1,660000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^STK [\\.,\\d]+( [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // STK 50,000 07.02.2024 08.02.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^STK [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Frankfurt am Main, 25.05.2016
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.*, (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Freier Erhalt ADRESSZEILE4=54321 Musterhausen
                                        // Freie Lieferung ADRESSZEILE2=Vorname NachnameADRESSZEILE3=Straße 1
                                        // Einbuchung von Rechten für die ADRESSZEILE1=Herr
                                        // Kapitalerhöhung ADRESSZEILE2=Max MustermannADRESSZEILE3=Musterstr. 2
                                        // Umtausch ADRESSZEILE2=Max MustermannADRESSZEILE3=Musterstr. 2
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>(Freier Erhalt" //
                                                                        + "|Freie Lieferung" //
                                                                        + "|Einbuchung von Rechten" //
                                                                        + "|Fusion" //
                                                                        + "|Kapitalerh.hung" //
                                                                        + "|Umtausch)).*$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Dividendengutschrift
                                        // Im Zuge der Geldzahlung erfolgt die Ausbuchung der Rechte. Ein separater Beleg wird nicht erstellt.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Dividendengutschrift).*$") //
                                                        .find(".*Ausbuchung der Rechte.*") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Kapitalherabsetzung ADRESSZEILE1=Herr
                                        // Durchführungsanzeige ADRESSZEILE2=Max Mustermann
                                        // Kapitalherabsetzung im Verhältnis 10:1. Weitere Informationen finden Sie im elektronischen Bundesanzeiger
                                        //
                                        // Reverse Split ADRESSZEILE1=Herr
                                        // Durchführungsanzeige ADRESSZEILE2=Max Muster
                                        // Wir haben Ihrem Depot im Verhältnis 2 : 1 folgende Stücke zugebucht (hinzugefügt):
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Kapitalherabsetzung|Reverse Split).*$") //
                                                        .find("Durchf.hrungsanzeige.*") //
                                                        .find(".* im Verhältnis.*") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);

                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Kapitalherabsetzung ADRESSZEILE1=Herr
                                        // Umbuchung der Teil- in Vollrechte. Für die eventuell verbleibenden Bruchteile (Nachkommastellen) in den Teilrechten
                                        //
                                        // Kapitalherabsetzung ADRESSZEILE1=Herr
                                        // Durchführungsanzeige ADRESSZEILE2=Max MUSTER
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>Kapitalherabsetzung).*$") //
                                                        .find("(Umbuchung der Teil\\- in Vollrechte|Durchf.hrungsanzeige).*") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                                                            t.setAmount(0L);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Spitze )?(Kauf" //
                        + "|Verkauf" //
                        + "|Gesamtf.lligkeit" //
                        + "|Umtausch) .*$");
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
                                        // Gattungsbezeichnung ISIN
                                        // Sky Deutschland AG Namens-Aktien o.N. DE000SKYD000
                                        // Nominal Ex-Tag
                                        // Abfindung zu:
                                        // EUR 6,680000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)") //
                                                        .find("Abfindung zu:") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // DWS Deutschland Inhaber-Anteile LC DE0008490962
                                        // STK 0,7445 EUR 200,1500
                                        // Nominal Ex-Tag
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Morgan Stanley & Co. Intl PLC DIZ 25.09.20 25.09.2020 DE000MC55366
                                        // Fres. SE
                                        // STK 65,000 EUR 39,4400
                                        // Nominal Ex-Tag
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // Société Générale Effekten GmbH DISC.Z 13.05.2021 DE000SE8F9E8
                                        // 13.05.21 NVIDIA 498
                                        // Nominal Kurs
                                        // STK 4,000 EUR 480,6300
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung Fälligkeit näch. Zinstermin ISIN
                                        // 11,5% UniCredit Bank AG HVB Aktienanleihe 24.11.2023 24.11.2023 DE000HB4GEE2
                                        // v.22(23)SDF
                                        // Nominal Kurs
                                        // EUR 1.000,000 90,1800 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1", "currency") //
                                                        .find("Gattungsbezeichnung F.lligkeit n.ch. Zinstermin ISIN") //
                                                        .match("^(?<name>.*) [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2,4} )?(?<name1>.*)$") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // Gagfah S.A. Actions nom. EO 1,25 LU0269583422
                                        // Nominal Ex-Tag
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "name1") //
                                                        .find("(Einbuchung:|Ausbuchung:|Wir erhielten zu Gunsten Ihres Depots|Dividendengutschrift).*") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name1>.*)") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Nominal"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 6,840
                                        // STK 65,000 EUR 39,4400
                                        // STK 25,000 22.09.2015
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^STK (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominal Kurs
                                        // EUR 1.000,000 90,1800 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Handelszeit 15:30 Orderprovision USD 11,03-
                        // Handelszeit 12:00
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelstag 15.08.2019 Kurswert EUR 149,01-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDateTime(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDateTime(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // 25.09.2020 123456789 EUR 2.563,60
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 26.11.2015 172306238 68366911 EUR 7,90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]+ [\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // STK 33,000 06.06.2011
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Frankfurt am Main, 26.02.2019
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("(^|^[\\s]+).*, (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // zu versteuern (negativ) EUR 4,49
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                        // 14.09.2016 172306238 47883712 EUR 1,18
                        // @formatter:on
                        .section("currency", "amount").optional() //
                        .find("zu versteuern \\(negativ\\) .*$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 17.12.2020 241462046 EUR/USD 1,2239 EUR 3.965,72
                        // zu versteuern (negativ) EUR 53,43
                        // 16.12.2020 241462046 59592727 EUR 14,10
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "amount").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                        .find("zu versteuern \\(negativ\\) .*$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ [\\d]+ [\\w]{3} (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("amount")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // zu versteuern (negativ) EUR 4,49
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                        // 14.09.2016 172306238 47883712 EUR 1,18
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .find("zu versteuern \\(negativ\\) .*$") //
                        .match("^.* (?<note1>Abrechnungs\\-Nr\\.).*$") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]+ (?<note2>[\\d]+) [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a tax refunds, we set a flag and don't book tax below.
        transaction //

                        .section("negative").optional() //
                        .match("^zu versteuern \\(negativ\\) (?<negative>.*)$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("negative", true));

        // If we have a gross reinvestment, we set a flag and don't book tax
        // below.
        transaction //

                        .section("noTax").optional() //
                        .match("^Ertragsthesaurierung (?<noTax>.*)$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true));

        transaction //

                        // @formatter:off
                        // Handelsplatz außerbörslich Lang & Schwarz Frz. Finanztrans. Steuer EUR 11,19-
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.*Finanztrans\\. Steuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Börse Xetra/EDE Kapitalertragsteuer EUR 1,43-
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.*Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Verwahrart Girosammelverwahrung Solidaritätszuschlag EUR 0,08-
                        // Solidaritätszuschlag EUR 1,43-
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.*Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer EUR 1,01-
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^.*Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // ausländische Quellensteuer 27% DKK 8,34
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("ausl.ndische Quellensteuer.* (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // davon anrechenbare US-Quellensteuer EUR 4,74
                        // davon anrechenbare US-Quellensteuer 15% EUR 2,72
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^.*anrechenbare US\\-Quellensteuer.* (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // anrechenbare Quellensteuer EUR 0,03
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^anrechenbare Quellensteuer (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // einbehaltene Kapitalertragsteuer EUR 1,81
                        // einbehaltene Kapitalertragsteuer EUR              0,39
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^einbehaltene Kapitalertrags(s)?teuer[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltener Solidaritätszuschlag EUR 0,10
                        // einbehaltener Solidaritätszuschlag  EUR              0,02
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^einbehaltener Solidarit.tszuschlag[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltene Kirchensteuer EUR 5,86
                        // einbehaltene Kirchensteuer  EUR              0,02
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^einbehaltene Kirchensteuer[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltene Kirchensteuer Ehegatte/Lebenspartner EUR 1,09
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^einbehaltene Kirchensteuer Ehegatte\\/Lebenspartner[\\s]{1,}(?<currency>[\\w]{3})[\\s]{1,}(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative") && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Handelsplatz außerbörslich Orderprovision EUR 1,00-
                        // Handelszeit 15:30 Orderprovision USD 11,03-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Orderprovision[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsplatz Börse NASDAQ/NAN Handelsplatzgebühr USD 5,51-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Handelsplatzgeb.hr[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börse Xetra/EDE Börsengebühr EUR 1,50-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Börsengeb.hr[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelszeit 12:30 Maklercourtage              EUR 0,75-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Maklercourtage[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Gebühren GBP 8,83-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Geb.hren[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelszeit 16:04 Fremdspesen EUR 1,50-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Fremdspesen[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
