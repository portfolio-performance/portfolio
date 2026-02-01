package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
 * @implNote Hypothekarbank Lenzburg AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractor extends AbstractPDFExtractor
{
    public HypothekarbankLenzburgAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hypothekarbank Lenzburg AG");

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Hypothekarbank Lenzburg AG";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("(Wir haben am .* f.r Sie (gekauft|verkauft)|Titelr.ckzahlung)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Ihr Betreuerteam:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Rücknahme" change from BUY to SELL
                        .section("type").optional() //
                        .match("^B.rse \\/ " //
                                        + "(?<type>(Kauf" //
                                        + "|Verkauf" //
                                        + "|R.cknahme)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "Sale".equals(v.get("type")) //
                                            || "Rücknahme".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // Is type --> "Titelrückzahlung" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>Titelr.ckzahlung).*$") //
                        .assign((t, v) -> {
                            if ("Titelrückzahlung".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  5 Underlying Tracker Asset Segregated SPV Depotstelle: 1
                                        // Valor: 110867626 / CH1108676268
                                        // Brutto zu USD 920.67 USD  4'603.35
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Aufgrund Ihres Bestandes schreiben wir Ihnen gut") //)
                                                        .match("^[\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Brutto zu (?<currency>[A-Z]{3})[\\s]{1,}[\\.'\\d]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                                        //  720 Accum Shs USD Inve FTSE All Depotstelle  3500
                                        // Valor: 125615212 / IE000716YHJ7
                                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie (gekauft|verkauft)") //)
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.,'\\d]+ Kurs (?<currency>[A-Z]{3})[\\s]{1,}[\\.,'\\d]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 04.02.2022 an der SIX für Sie gekauft
                                        //  19 Accum Shs Unhedged USD iSh MSCI USA Depotstelle  1
                                        // Valor: 43695283 / IE00BFNM3G45
                                        // Menge  19 Kurs USD 8.705 USD  165.40
                                        //
                                        // Wir haben am 29.07.2024 an der SIX für Sie verkauft
                                        //  5 Accum Shs Unhedged USD iShs Jap ESG Depotstelle  1
                                        // Valor: 43671001 / IE00BFNM3L97
                                        // Menge  5 Kurs USD 6.68 USD  33.40
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie (gekauft|verkauft)") //)
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.,'\\d]+ Kurs (?<currency>[A-Z]{3})[\\s]{1,}[\\.,'\\d]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 03.09.2024 an der BX Swiss für Sie gekauft
                                        //  8 Ptg.Shs Van FTSE All Wr Depotstelle  3500
                                        // Valor: 18575459 / IE00B3RBWM25
                                        // Menge  8 Kurs CHF 115.471 CHF  923.77
                                        //
                                        // Wir haben am 03.09.2024 an der BX Swiss für Sie gekauft
                                        //  7 Shs SPDR S&P US Di Depotstelle  3500
                                        // Valor: 13976063 / IE00B6YX5D40
                                        // Menge  7 Kurs CHF 65.555 CHF  458.89
                                        //
                                        // Wir haben am 27.12.2024 an der BX Swiss für Sie verkauft
                                        // 10 Namen-Akt Vontobel Holding AG Nom. CHF Depotstelle  3500
                                        // 1.00
                                        // Valor: 1233554 / CH0012335540
                                        // Menge  10 Kurs CHF 63.788 CHF  637.88
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie (gekauft|verkauft)") //)
                                                        .match("^[\\s]*[\\.,'\\d]+ ([A-Za-z]{3}\\.)?[A-Za-z]{3} (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.,'\\d]+ Kurs (?<currency>[A-Z]{3})[\\s]{1,}[\\.,'\\d]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 09.10.2024 an der BX Swiss für Sie gekauft
                                        // 15 Registered Shs Microsoft Corp Depotstelle  3500
                                        // Valor: 951692 / US5949181045
                                        // Menge  15 Kurs CHF 355.865 CHF  5'337.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie (gekauft|verkauft)") //)
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.,'\\d]+ Kurs (?<currency>[A-Z]{3})[\\s]{1,}[\\.,'\\d]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Menge[\\s]{1,}(?<shares>[\\.,'\\d]+) Kurs.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        //  5 Underlying Tracker Asset Segregated SPV Depotstelle: 1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\s]*(?<shares>[\\.,'\\d]+) .* (?<name>.*) Depotstelle.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Wir haben am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // 162 IC Zahlbar Datum: 11.08.2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.* Zahlbar Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Belastung 314.391.304 Valuta 18.03.2024 CHF  3'974.14
                        // Gutschrift 351.413.308 Valuta 31.12.2024 CHF  634.21
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Belastung|Gutschrift) .* (?<currency>[A-Z]{3})[\\s]{1,}(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                            // @formatter:off
                                            // Brutto zu USD 920.67 USD  4'603.35
                                            // USD  4'603.35
                                            // Devisenkurs 0.8099 CHF  3'728.25
                                            // @formatter:on
                                            section -> section //
                                                            .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency", "gross") //
                                                            .find("Brutto zu .*") //
                                                            .match("^(?<termCurrency>[A-Z]{3})[\\s]+(?<fxGross>[\\.,'\\d]+)$") //
                                                            .match("^Devisenkurs[\\s]+(?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3})[\\s]+(?<gross>[\\.,'\\d]+)$") //
                                                            .assign((t, v) -> {
                                                                var exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                                var inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                                var rate = new ExtrExchangeRate(inverseRate, v.get("baseCurrency"), v.get("termCurrency"));
                                                                type.getCurrentContext().putType(rate);

                                                                var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                                var fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxGross")));

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }),
                                            // @formatter:off
                                            // Menge  19 Kurs USD 8.705 USD  165.40
                                            // USD  165.40
                                            // Devisenkurs  0.9206 CHF  152.25
                                            // @formatter:on
                                            section -> section //
                                                            .attributes("termCurrency", "fxGross", "exchangeRate", "baseCurrency", "gross") //
                                                            .find("Menge.*Kurs.*") //
                                                            .match("^(?<termCurrency>[A-Z]{3})[\\s]+(?<fxGross>[\\.,'\\d]+)$") //
                                                            .match("^Devisenkurs[\\s]+(?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3})[\\s]+(?<gross>[\\.,'\\d]+)$") //
                                                            .assign((t, v) -> {
                                                                var exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                                var inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                                var rate = new ExtrExchangeRate(inverseRate, v.get("baseCurrency"), v.get("termCurrency"));
                                                                type.getCurrentContext().putType(rate);

                                                                var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                                var fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxGross")));

                                                                checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            }))

                        // @formatter:off
                        // Transaktion 61327806-0002
                        // Endverfall Transaktion  82230675
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Transaktion .*)$") //
                        .assign((t, v) -> t.setNote(trim(replaceMultipleBlanks(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        var type = new DocumentType("(Ertragsaussch.ttung|Dividendenzahlung)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Ertragsaussch.ttung|Dividendenzahlung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  9 Registered Shs Ashtead Group PLC Nom. Depotstelle: 1117
                                        // GBP 0.10 Zahlbar Datum: 12.09.2023
                                        // Valor: 18575459 / IE00B3RBWM25 Ex Datum: 13.06.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^(?<currency>[A-Z]{3}) [\\.,'\\d]+ Zahlbar Datum.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        // 168 Ptg.Shs Van FTSE All Wr Depotstelle: 3500
                                        // USD Zahlbar Datum: 26.06.2024
                                        // Valor: 18575459 / IE00B3RBWM25 Ex Datum: 13.06.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^[\\s]*[\\.,'\\d]+ ([A-Za-z]{3}\\.)?[A-Za-z]{3} (?<name>.*) Depotstelle.*$") //
                                                        .match("^(?<currency>[A-Z]{3}) Zahlbar Datum.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  25 Namen-Akt Allreal Holding AG Nom. CHF Depotstelle: 3500
                                        // 1.00 Zahlbar Datum: 25.04.2024
                                        // Valor: 883756 / CH0008837566 Ex Datum: 23.04.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) (?<currency>[A-Z]{3}) Depotstelle.*$") //
                                                        .match("^[\\.'\\d]+ Zahlbar Datum.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  10 Namen-Akt Swisscom AG Nom. CHF 1.00 Depotstelle: 3500
                                        // Valor: 874251 / CH0008742519 Zahlbar Datum: 04.04.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) (?<currency>[A-Z]{3}) [\\.'\\d]+ Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  2 Namen-Akt Talanx AG Depotstelle: 1117
                                        // Valor: 19625225 / DE000TLX1005 Zahlbar Datum: 13.05.2025
                                        // Brutto zu EUR 2.70 EUR  5.40
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^[\\s]*[\\.,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .match("^Brutto zu (?<currency>[A-Z]{3}) [\\.,'\\d]+.*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // 168 Ptg.Shs Van FTSE All Wr Depotstelle: 3500
                        //  10 Namen-Akt Swisscom AG Nom. CHF 1.00 Depotstelle: 3500
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\s]*(?<shares>[\\.,'\\d]+) .* Depotstelle.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))


                        // @formatter:off
                        // USD Zahlbar Datum: 26.06.2024
                        // @formatter:on
                        .section("date") //
                        .match("^.* Zahlbar Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift 351.413.308 Valuta 26.06.2024 CHF  116.96
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift .* Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3})[\\s]{1,}(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Brutto zu USD 0.788854 USD  132.53
                        // USD  132.53
                        // Devisenkurs 0.88255 CHF  116.96
                        // @formatter:on
                        .section("termCurrency", "exchangeRate", "fxGross", "baseCurrency", "gross").optional() //
                        .match("^Brutto zu (?<baseCurrency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}[\\s]{1,}(?<fxGross>[\\.,'\\d]+)$") //
                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})[\\s]{1,}(?<gross>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType((rate));

                            var fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Quartalsdividende Transaktion  65062408
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>.*) Transaktion.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Quartalsdividende Transaktion  65062408
                        // Transaktion  62847906
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Transaktion.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(replaceMultipleBlanks(v.get("note"))), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }


    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Verrechnungssteuer 35 % CHF -77.00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer .* (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<tax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Eidg. Umsatzabgabe CHF  5.92
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidg\\. Umsatzabgabe (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<tax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 26.375 % EUR -1.42
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Quellensteuer.* (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<withHoldingTax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Steuerrückbehalt USA 15 % CHF -0.05
                        // @formatter:on
                        .section("currency", "creditableWithHoldingTax").optional() //
                        .match("^Steuerr.ckbehalt.* (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<creditableWithHoldingTax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eigene Kommission (NEON) CHF  19.74
                        // Eigene Kommission (NEON) CHF -3.19
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Eigene Kommission.* (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<fee>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Externe Ausführungsgebühr CHF  0.15
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Externe Ausf.hrungsgeb.hr.* (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<fee>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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
