package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
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

@SuppressWarnings("nls")
public class UBSAGBankingAGPDFExtractor extends AbstractPDFExtractor
{
    public UBSAGBankingAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UBS");
        addBankIdentifier("UBS Switzerland AG");
        addBankIdentifier("www.ubs.com");

        addBuySellTransaction();
        addDividendeTransaction();
        addDepotAccountFeeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UBS AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(B.rse (Kauf|Verkauf) Komptant" //
                        + "|Ihr (Kauf|Verkauf)" //
                        + "|R.CKZAHLUNG RESERVEN AUS KAPITALEINLAGEN" //
                        + "|FUSION" //
                        + "|FRAKTIONS\\-ABRECHNUNG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Depot|Portfolio)\\-Nr\\..*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(.* B.rse|Ihr) (?<type>(Kauf|Verkauf)).*$") //
                                                        .assign((t, v) -> {
                                                            if ("Verkauf".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.SELL);
                                                        }),
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^(?<type>(R.CKZAHLUNG RESERVEN AUS KAPITALEINLAGEN" //
                                                                        + "|FUSION" //
                                                                        + "|FRAKTIONS\\-ABRECHNUNG))$") //
                                                        .assign((t, v) -> {
                                                            if ("RÜCKZAHLUNG RESERVEN AUS KAPITALEINLAGEN".equals(v.get("type")) //
                                                                            || "FUSION".equals(v.get("type")) //
                                                                            || "FRAKTIONS-ABRECHNUNG".equals(v.get("type"))) //
                                                                t.setType(PortfolioTransaction.Type.SELL);
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // USD 2'180 UBS (Lux) Fund Solutions - MSCI 21966836
                                        // Emerging Markets UCITS ETF LU0950674175
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "wkn", "nameContinued", "isin") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.'\\d\\s]+ (?<name>.*) (?<wkn>[A-Z0-9]{5,9})$") //
                                                        .match("^(?<nameContinued>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Stückzahl Valor 1203204 ISIN CH0012032048 Kurs
                                        // 10 Genussscheine CHF 376.3
                                        // Roche Holding AG (ROG)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name1", "currency", "name","tickerSymbol") //
                                                        .match("^St.ckzahl Valor (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .match("^(?<name1>.*) \\((?<tickerSymbol>.*)\\)$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stückzahl Valor 1253020 ISIN CH0012530207 Kurs
                                        // 15 N-Akt -B- Bachem Holding AG CHF 146
                                        // Kurswert in Handelswährung CHF 2 190.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "currency", "name1") //
                                                        .match("^St.ckzahl Valor (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .match("^(?<name1>.*) [\\w]{3} [\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurswert"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // STUECKZAHL VALOR 1057076 ISIN CH0010570767 ANSATZ
                                        // 1 PARTIZIPATIONSSCHEINE BRUTTO
                                        // CHOCOLADEFABRIKEN LINDT &
                                        // SPRUENGLI AG (LISP) CHF 36.90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name3", "name", "name1", "tickerSymbol", "currency") //
                                                        .match("^STUECKZAHL VALOR (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name3>.*) BRUTTO$") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^(?<name1>.*) \\((?<tickerSymbol>.*)\\) (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurswert"))
                                                                v.put("name", v.get("name") + " " + v.get("name1") + " " + v.get("name3"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stück Valor 58198423 ISIN US92556V1061 Preis
                                        // 0.167 N-AKT VIATRIS INC USD 18.21874
                                        // (VTRSV)
                                        // Zum Preis von USD 3.04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "currency", "tickerSymbol") //
                                                        .match("^St.ck Valor (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .match("^\\((?<tickerSymbol>.*)\\)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Stück Valor 58198423 ISIN US92556V1061 Preis
                                        // 0.167 N-AKT VIATRIS INC USD 18.21874
                                        // (VTRSV)
                                        // Zum Preis von USD 3.04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "currency", "name1") //
                                                        .match("^St.ck Valor (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zum Preis"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* Komptant (\\-)?(?<shares>[\\.'\\d\\s]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 15 N-Akt -B- Bachem Holding AG CHF 146
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.'\\d\\s]+) .* [\\w]{3} [\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 1 PARTIZIPATIONSSCHEINE BRUTTO
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.'\\d\\s]+) PARTIZIPATIONSSCHEINE BRUTTO$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                        // @formatter:on
                        .section("time").optional() //
                        .match("^(Abschluss|Abschlussdatum:) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                                        // Abschlussdatum: 27.12.2017 Abschlussort SIX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Abschluss|Abschlussdatum:) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"),
                                                                                type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // VERFALL 10.05.2021 EX-TAG 06.05.2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^VERFALL (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zugunsten Konto 292-123456.40R CHF Valuta 12.04.2021 CHF 8 030.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zugunsten Konto .* Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,'\\d\\s]+$")
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Abrechnungsbetrag USD -4'919.95
                                        // Abrechnungsbetrag USD 11'050.04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Abrechnungsbetrag (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Zulasten Konto 0292 00123456.M1Z CHF Valuta 29.12.2017 CHF 2 213.15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(Zulasten|Zugunsten) Konto .* Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // GUTSCHRIFT KONTO 292-123456.40R VALUTA 10.05.2021 CHF 36.90
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Buchung 10.03.2022 XXXXXXXXXXXXXXXX 0.9267
                                        // 15:02:13 450 USD 10.87
                                        // Abrechnungsdetails Bewertet in  CHF
                                        // Transaktionswert USD 4'890.60 4'532
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("exchangeRate", "baseCurrency", "termCurrency",
                                                                        "currency", "gross") //
                                                        .match("^Buchung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<exchangeRate>[\\.'\\d]+)$") //
                                                        .match("^Abrechnungsdetails Bewertet in .*(?<termCurrency>[\\w]{3})$") //
                                                        .match("^Transaktionswert (?<currency>[\\w]{3}) (?<gross>[\\.'\\d\\s]+) [\\.'\\d\\s]+$") //
                                                        .match("^Abrechnungsbetrag (?<baseCurrency>[\\w]{3}) (\\-)?[\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Kurswert in Handelswährung USD 3 851.00
                                        // USD / CHF zu 0.9987
                                        // USD 3 851.00
                                        // CHF 3 846.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^Kurswert in Handelsw.hrung [\\w]{3} (?<fxGross>[\\.'\\d\\s]+)$") //
                                                        .match("^(?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3}) zu (?<exchangeRate>[\\.'\\d]+)$") //
                                                        .match("^[\\w]{3} [\\.'\\d\\s]+$") //
                                                        .match("^[\\w]{3} (?<gross>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Kurswert in Handelswährung USD 3 851.00
                                        // USD / CHF zu 0.90231 USD 2 156.69
                                        // CHF 1 946.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^Kurswert in Handelsw.hrung [\\w]{3} (?<fxGross>[\\.'\\d\\s]+)$") //
                                                        .match("^(?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) zu (?<exchangeRate>[\\.'\\d]+) [\\w]{3} [\\.'\\d\\s]+$") //
                                                        .match("^[\\w]{3} (?<gross>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Zum Preis von USD 3.04
                                        // USD/CHF 0.86661 CHF 2.63
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^Zum Preis von [\\w]{3} (?<fxGross>[\\.'\\d\\s]+)$") //
                                                        .match("^(?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+) [\\w]{3} (?<gross>[\\.'\\d\\s]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Auftrags-Nr. 4083256
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(t -> {
                            // If we have multiple entries in the document, with
                            // fee, then the "noProvision" flag must be removed.
                            type.getCurrentContext().remove("noProvision");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("DIVIDENDENZAHLUNG");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Depot|Portfolio)\\-Nr\\..*$");
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
                                        // STUECKZAHL VALOR 957150 ISIN US6541061031 ANSATZ
                                        // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                                        // BRUTTO USD 6.10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "tickerSymbol", "currency") //
                                                        .match("^STUECKZAHL VALOR (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) \\((?<tickerSymbol>.*)\\) BRUTTO$") //
                                                        .match("^BRUTTO (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // STUECKZAHL VALOR 115606002 ISIN GB00BP6MXD84 ANSATZ
                                        // 546 N-AKT SHELL PLC BRUTTO
                                        // (VTRSV)
                                        // BRUTTO EUR 167.62
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "isin", "name", "tickerSymbol", "currency") //
                                                        .match("^STUECKZAHL VALOR (?<wkn>[A-Z0-9]{5,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\.'\\d\\s]+ (?<name>.*) BRUTTO$") //
                                                        .match("^\\((?<tickerSymbol>.*)\\)$")
                                                        .match("^BRUTTO (?<currency>[\\w]{3}) [\\.'\\d\\s]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                        // 546 N-AKT SHELL PLC BRUTTO
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.'\\d\\s]+) .* BRUTTO$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                        // @formatter:on
                        .section("date") //
                        .match("^GUTSCHRIFT KONTO .* VALUTA (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.'\\d\\s]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // BRUTTO USD 6.10
                        // UMRECHNUNGSKURS USD/CHF 0.901639
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^BRUTTO [\\w]{3} (?<fxGross>[\\.'\\d\\s]+)$") //
                        .match("^UMRECHNUNGSKURS (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d\\s]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftrags-Nr. 4083256
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // FX-Marge von CHF 2.62 inbegriffen
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^FX\\-Marge von (?<note1>[\\w]{3}) (?<note2>[\\.'\\d\\s]+) inbegriffen.*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note2") + " " + v.get("note1"), " | FX-Marge: ")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepotAccountFeeTransaction()
    {
        DocumentType type = new DocumentType("Depotf.hrungspreis");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnung vom .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Zu Lasten Konto 292-123456.40R, Valuta 30. Juni 2021
                        // @formatter:on
                        .section("date") //
                        .match("^Zu Lasten Konto .* Valuta (?<date>[\\d]{1,2}.\\ .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Position Basis Preis p.a. Betrag (CHF)
                        // Depotführungspreis inkl. Steuern 101.82
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Position Basis Preis p\\.a\\. Betrag \\((?<currency>[\\w]{3})\\)$") //
                        .match("^Depotf.hrungspreis inkl\\. Steuern (?<amount>[\\.,'\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abrechnung vom 01.04.2021 - 30.06.2021
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Abrechnung) vom (?<note2>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(v.get("note1"), v.get("note2"), " ")))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // STEUERABZUG 30.00% USD -1.83
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^STEUERABZUG [\\.'\\d]+% (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d\\s]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Eidgenössische Stempelabgabe CHF 1.65
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidgen.ssische Stempelabgabe (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.'\\d\\s]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage CHF 36.55
                        // Rabatt e-banking CHF -6.55
                        //
                        // Courtage CHF -45.15
                        // Rabatt e-banking CHF 5.15
                        // @formatter:on
                        .section("currency", "fee", "discountCurrency", "discount").optional() //
                        .match("^Courtage (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.'\\d\\s]+)$") //
                        .match("^Rabatt e\\-banking (?<discountCurrency>[\\w]{3}) (\\-)?(?<discount>[\\.'\\d\\s]+)$") //
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (fee.subtract(discount).isPositive())
                            {
                                fee = fee.subtract(discount);
                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }

                            type.getCurrentContext().putBoolean("noProvision", true);
                        })

                        // @formatter:off
                        // Courtage USD -22.01
                        // Courtage CHF 20.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Courtage (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noProvision"))
                                processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Diverse USD -7.34
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Diverse (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Externe Gebühren CHF 1.50
                        // Externe Gebühren USD -0.01
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Externe Geb.hren (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$") //
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
