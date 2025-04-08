package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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

/**
 * @formatter:off
 * @implNote Zürcher Kantonalbank
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 *           If we have an exchange rate, then the taxes and fees are shown in the documents in foreign currency.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class ZuercherKantonalbankPDFExtractor extends AbstractPDFExtractor
{
    public ZuercherKantonalbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Zürcher Kantonalbank");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Zürcher Kantonalbank";
    }


    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Ihr (Kauf|Verkauf)|Abrechnung von Wertschriften)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Gesch.fts\\-Nr\\..*|Ausgabe|Zeichnungen)$");
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
                        .match("^Ihr (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Zeichnungen
                                        // Valor Bezeichnung Anzahl Kurs CHF Datum Total CHF
                                        // 51215778 SWC (CH) IPF III VF 95 Passiv NT 0.633 157.95 28.05.2021 99.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "wkn", "name") //
                                                        .match("^Valor Bezeichnung Anzahl Kurs (?<currency>[\\w]{3}) Datum Total [\\w]{3}$")
                                                        .match("^(?<wkn>[A-Z0-9]{5,9}) (?<name>.*) [\\.'\\d]+ [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.'\\d]+$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Anteile - Units -NT CHF- Swisscanto (CH) IPF III (IPF III) - Swc (CH) IPF III Vorsorge Fonds 95 Passiv
                                        // Valor 51215778 / ISIN CH0512157782
                                        // Stück GBP GBP
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "wkn", "isin") //
                                                        .match("^Anteile .* (?<currency>[\\w]{3})\\- (?<name>.*) \\-.*$")
                                                        .match("Valor (?<wkn>[A-Z0-9]{5,9}) \\/ ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Abwicklungs-Nr. 592473551 / Auftrags-Nr. ONBA-0005128022772021
                                        // Registered Shs Glencore PLC
                                        // Valor 12964057 / ISIN JE00B4T3BW64
                                        // Stück GBP GBP
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Abwicklungs\\-Nr\\..*")
                                                        .match("^(?<name>.*)$")
                                                        .match("Valor (?<wkn>[A-Z0-9]{5,9}) \\/ ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^St.ck (?<currency>[\\w]{3}) [\\w]{3}$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 51215778 SWC (CH) IPF III VF 95 Passiv NT 0.633 157.95 28.05.2021 99.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z0-9]{5,9} .* (?<shares>[\\.'\\d]+) [\\.'\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Stück GBP GBP
                                        // 1'000 zu 3.545 3'545.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("St.ck [\\w]{3} [\\w]{3}") //
                                                        .match("^(?<shares>[\\.'\\d]+) zu [\\.'\\d]+ [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschluss per: 04.10.2021 / Buchungstag: 04.10.2021 / Börsenplatz: LSE UK 1 CUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Abschluss per: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 51215778 SWC (CH) IPF III VF 95 Passiv NT 0.633 157.95 28.05.2021 99.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^[A-Z0-9]{5,9} .* [\\.'\\d]+ [\\.'\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // zum Kurs von GBP CHF 1.258775 CHF
                                        // Total zu Ihren Lasten Valuta 06.10.2021 4'469.94
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}von[\\s]{1,}[\\w]{3}([\\s|\\/]+)[\\w]{3}[\\s]{1,}[\\.'\\d]+[\\s]{1,}(?<currency>[\\w]{3})$") //
                                                        .match("^Total zu Ihren (Gunsten|Lasten) Valuta .* (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Valor Bezeichnung Anzahl Kurs CHF Datum Total CHF
                                        // Total Belastung Zeichnungen 99.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Valor Bezeichnung Anzahl Kurs [\\w]{3} Datum Total (?<currency>[\\w]{3})$")
                                                        .match("^Total Belastung Zeichnungen (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Stück CHF CHF
                                        // Total zu Ihren Lasten Valuta 10.06.2022 7'294.30
                                        // Total zu Ihren Gunsten Valuta 28.02.2023 8'512.11
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^St.ck [\\w]{3} (?<currency>[\\w]{3})$") //
                                                        .match("^Total zu Ihren (Gunsten|Lasten) Valuta .* (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            // @formatter:off
                                                            // Set currency for tax and fee handling
                                                            // @formatter:on
                                                            type.getCurrentContext().put("currency", asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // 1'000 zu 3.545 3'545.00
                        // zum Kurs von GBP CHF 1.258775 CHF
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^[\\.'\\d]+ zu [\\.'\\d]+ (?<fxGross>[\\.'\\d]+)$") //
                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}von[\\s]{1,}(?<baseCurrency>[\\w]{3})([\\s|\\/]+)(?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.'\\d]+)[\\s]{1,}[\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                            // @formatter:off
                            // Set foreign currency for tax and fee handling
                            // @formatter:on
                            type.getCurrentContext().put("fxCurrency", rate.getBaseCurrency());
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Abwicklungs-Nr. 592473551 / Auftrags-Nr. ONBA-0005128022772021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Abwicklungs\\-Nr\\..*) \\/.*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Abwicklungs-Nr. 123
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Abwicklungs\\-Nr\\..*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Abwicklungs-Nr. 592473551 / Auftrags-Nr. ONBA-0005128022772021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* \\/ (?<note>Auftrags\\-Nr\\..*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Abwicklungs-Nr. 123
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Auftrags\\-Nr\\..*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertragsabrechnung \\(Eingang vorbehalten\\)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Ertragsabrechnung \\(Eingang vorbehalten\\)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 2'000 Registered Shs Babcock International Group PLC
                        // Valor 1142141 / ISIN GB0009697037
                        // Dividendensatz GBP 0.017
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency")
                        .match("^St.ck (?<shares>[\\.'\\d]+) (?<name>.*)$")
                        .match("Valor (?<wkn>[A-Z0-9]{5,9}) \\/ ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                        .match("^Dividendensatz (?<currency>[\\w]{3}) [\\.'\\d]+$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 2'000 Registered Shs Babcock International Group PLC
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbar 19.01.2024
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbar (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // zum Kurs  von GBP/CHF 1.09551 CHF
                                        // Total zu Ihren Gunsten Valuta 19.01.2024 37.25
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}von[\\s]{1,}[\\w]{3}([\\s|\\/]+)[\\w]{3}[\\s]{1,}[\\.'\\d]+[\\s]{1,}(?<currency>[\\w]{3})$") //
                                                        .match("^Total zu Ihren Gunsten Valuta .* (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Zahlbar 24.01.2024
                                        // USD
                                        // Total zu Ihren Gunsten Valuta 24.01.2024 99.14
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Zahlbar [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}")
                                                        .match("^(?<currency>[\\w]{3})$") //
                                                        .match("^Total zu Ihren Gunsten Valuta .* (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Total zu Ihren Gunsten Valuta 06.02.2024 42.08
                                        // zum Kurs vom USD / CHF 0.855575
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Total zu Ihren (Gunsten|Lasten) Valuta .* (?<amount>[\\.'\\d]+)$") //
                                                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}vom[\\s]{1,}(?<currency>[\\w]{3})([\\s|\\/]+)[\\w]{3}[\\s]{1,}[\\.'\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            // @formatter:off
                                                            // Set currency for tax and fee handling
                                                            // @formatter:on
                                                            type.getCurrentContext().put("currency", asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttobetrag 34.00
                                        // zum Kurs  von GBP/CHF 1.09551 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag (?<fxGross>[\\.'\\d]+)$") //
                                                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}von[\\s]{1,}(?<baseCurrency>[\\w]{3})([\\s|\\/]+)(?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.'\\d]+)[\\s]{1,}[\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                                                            // @formatter:off
                                                            // Set foreign currency for tax and fee handling
                                                            // @formatter:on
                                                            type.getCurrentContext().put("fxCurrency", rate.getBaseCurrency());
                                                        }),
                                        // @formatter:off
                                        // Bruttobetrag 60.12
                                        // zum Kurs vom USD / CHF 0.855575
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag (?<gross>[\\.'\\d]+)$") //
                                                        .match("^zum[\\s]{1,}Kurs[\\s]{1,}vom[\\s]{1,}(?<baseCurrency>[\\w]{3})([\\s|\\/]+)(?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                                                            // @formatter:off
                                                            // Set foreign currency for tax and fee handling
                                                            // @formatter:on
                                                            type.getCurrentContext().put("fxCurrency", rate.getBaseCurrency());
                                                        }))

                        // @formatter:off
                        // Abwicklungs-Nr. 744484581 / Buchungstag 19.01.2024
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Abwicklungs\\-Nr\\..*) \\/.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // 15% Quellensteuer 17.19
                        // @formatter:on
                        .section("withHoldingTax").optional() //
                        .match("^[\\d]+% Quellensteuer (?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // 15% Zusätzlicher Steuerrückbehalt 17.19
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^[\\d]+% Zus.tzlicher Steuerr.ckbehalt (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Eidg. Abgaben 5.31
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Eidg\\. Abgaben (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // UK Stamp Tax 29.50
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^UK Stamp Tax (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // 0.3% Finanztransaktionssteuer Frankreich 17.67
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^.* Finanztransaktionssteuer .* (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Fremde Kommission 0.71
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^Fremde Kommission (?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Ausführungsgebühr 1.50
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^Ausf.hrungsgeb.hr (?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency",
                                            type.getCurrentContext().get("fxCurrency") != null
                                                ? type.getCurrentContext().get("fxCurrency")
                                                : type.getCurrentContext().get("currency"));

                            processFeeEntries(t, v, type);
                        });
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
