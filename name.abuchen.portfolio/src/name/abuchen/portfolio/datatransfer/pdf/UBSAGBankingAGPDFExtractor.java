package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

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

        addBankIdentifier("UBS"); //$NON-NLS-1$
        addBankIdentifier("UBS Switzerland AG"); //$NON-NLS-1$
        addBankIdentifier("www.ubs.com"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addDepotAccountFeeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UBS AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(B.rse (Kauf|Verkauf) Komptant"
                        + "|Ihr (Kauf|Verkauf)"
                        + "|R.CKZAHLUNG RESERVEN AUS KAPITALEINLAGEN"
                        + "|FUSION"
                        + "|FRAKTIONS\\-ABRECHNUNG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Bewertet in: .*"
                        + "|Ihr (Kauf|Verkauf)"
                        + "|R.CKZAHLUNG RESERVEN AUS KAPITALEINLAGEN"
                        + "|FUSION"
                        + "|FRAKTIONS\\-ABRECHNUNG)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^.* B.rse (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Ihr (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(R.CKZAHLUNG RESERVEN AUS KAPITALEINLAGEN"
                                + "|FUSION"
                                + "|FRAKTIONS\\-ABRECHNUNG))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("RÜCKZAHLUNG RESERVEN AUS KAPITALEINLAGEN")
                                    || v.get("type").equals("FUSION")
                                    || v.get("type").equals("FRAKTIONS-ABRECHNUNG"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // USD 2'180 UBS (Lux) Fund Solutions - MSCI 21966836
                // Emerging Markets UCITS ETF LU0950674175
                .section("currency", "name", "wkn", "nameContinued", "isin").optional()
                .match("^(?<currency>[\\w]{3}) [\\.,'\\d\\s]+ (?<name>.*) (?<wkn>[0-9]{6,9})$")
                .match("^(?<nameContinued>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stückzahl Valor 1203204 ISIN CH0012032048 Kurs
                // 10 Genussscheine CHF 376.3
                // Roche Holding AG (ROG)
                // Kurswert in Handelswährung CHF 3 763.00
                .section("wkn", "isin", "name1", "currency", "name", "tickerSymbol").optional()
                .match("^St.ckzahl Valor (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .match("^(?<name1>.*) \\((?<tickerSymbol>.*)\\)$")
                .match("^Kurswert .*$")
                .assign((t, v) -> {
                    v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stückzahl Valor 1253020 ISIN CH0012530207 Kurs
                // 15 N-Akt -B- Bachem Holding AG CHF 146
                // Kurswert in Handelswährung CHF 2 190.00
                .section("wkn", "isin", "name", "currency", "name1").optional()
                .match("^St.ckzahl Valor (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .match("^(?<name1>.*) [\\w]{3} [\\.,'\\d\\s]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurswert"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STUECKZAHL VALOR 1057076 ISIN CH0010570767 ANSATZ
                // 1 PARTIZIPATIONSSCHEINE BRUTTO
                // CHOCOLADEFABRIKEN LINDT &
                // SPRUENGLI AG (LISP) CHF 36.90
                .section("wkn", "isin", "name3", "name", "name1", "tickerSymbol", "currency").optional()
                .match("^STUECKZAHL VALOR (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name3>.*) BRUTTO$")
                .match("^(?<name>.*)$")
                .match("^(?<name1>.*) \\((?<tickerSymbol>.*)\\) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurswert"))
                        v.put("name", v.get("name") + " " + v.get("name1") + " " + v.get("name3"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück Valor 58198423 ISIN US92556V1061 Preis
                // 0.167 N-AKT VIATRIS INC USD 18.21874
                // (VTRSV)
                // Zum Preis von USD 3.04
                .section("wkn", "isin", "name", "currency", "tickerSymbol").optional()
                .match("^St.ck Valor (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .match("^\\((?<tickerSymbol>.*)\\)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stück Valor 26729122 ISIN CH0267291224 Preis
                // 73 N-AKT SUNRISE COMMUNICATIONS CHF 110.00
                // GROUP AG
                // Zum Preis von CHF 8 030.00
                .section("wkn", "isin", "name", "currency", "name1").optional()
                .match("^St.ck Valor (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zum Preis"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                                section -> section
                                        .attributes("shares")
                                        .match("^.* Komptant (\\-)?(?<shares>[\\.,'\\d\\s]+) .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // 15 N-Akt -B- Bachem Holding AG CHF 146
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,'\\d\\s]+) .* [\\w]{3} [\\.,'\\d\\s]+$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // 1 PARTIZIPATIONSSCHEINE BRUTTO
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,'\\d\\s]+) PARTIZIPATIONSSCHEINE BRUTTO$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                .section("time").optional()
                .match("^(Abschluss|Abschlussdatum:) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Abschluss 08.03.2022  15:02:13 Börse Kauf Komptant 450 USD 10.868
                                // Abschlussdatum: 27.12.2017 Abschlussort SIX
                                section -> section
                                        .attributes("date")
                                        .match("^(Abschluss|Abschlussdatum:) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                                ,
                                // VERFALL 10.05.2021 EX-TAG 06.05.2021
                                section -> section
                                        .attributes("date")
                                        .match("^VERFALL (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Zugunsten Konto 292-123456.40R CHF Valuta 12.04.2021 CHF 8 030.00
                                section -> section
                                        .attributes("date")
                                        .match("^Zugunsten Konto .* Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,'\\d\\s]+$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Abrechnungsbetrag USD -4'919.95
                                // Abrechnungsbetrag USD 11'050.04
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Abrechnungsbetrag (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Zulasten Konto 0292 00123456.M1Z CHF Valuta 29.12.2017 CHF 2 213.15
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(Zulasten|Zugunsten) Konto .* Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // GUTSCHRIFT KONTO 292-123456.40R VALUTA 10.05.2021 CHF 36.90
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Buchung 10.03.2022 XXXXXXXXXXXXXXXX 0.9267
                                // 15:02:13 450 USD 10.87
                                // Abrechnungsdetails Bewertet in  CHF
                                // Transaktionswert USD 4'890.60 4'532
                                // @formatter:on
                                section -> section
                                        .attributes("exchangeRate", "baseCurrency", "termCurrency", "currency", "gross")
                                        .match("^Buchung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<exchangeRate>[\\.'\\d]+)$")
                                        .match("^Abrechnungsdetails Bewertet in .*(?<termCurrency>[\\w]{3})$")
                                        .match("^Transaktionswert (?<currency>[\\w]{3}) (?<gross>[\\.,'\\d\\s]+) [\\.,'\\d\\s]+$")
                                        .match("^Abrechnungsbetrag (?<baseCurrency>[\\w]{3}) (\\-)?[\\.,'\\d\\s]+$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Kurswert in Handelswährung USD 3 851.00
                                // USD / CHF zu 0.9987
                                // USD 3 851.00
                                // CHF 3 846.00
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "termCurrency", "baseCurrency", "exchangeRate", "currency", "gross")
                                        .match("^Kurswert in Handelsw.hrung (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+)$")
                                        .match("^(?<termCurrency>[\\w]{3}) \\/ (?<baseCurrency>[\\w]{3}) zu (?<exchangeRate>[\\.,'\\d\\s]+)$")
                                        .match("^[\\w]{3} [\\.,'\\d\\s]+$")
                                        .match("^(?<currency>[\\w]{3}) (?<gross>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Kurswert in Handelswährung USD 3 851.00
                                // USD / CHF zu 0.90231 USD 2 156.69
                                // CHF 1 946.00
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "termCurrency", "baseCurrency", "exchangeRate", "currency", "gross")
                                        .match("^Kurswert in Handelsw.hrung (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+)$")
                                        .match("^(?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) zu (?<exchangeRate>[\\.'\\d]+) [\\w]{3} [\\.,'\\d\\s]+$")
                                        .match("^(?<currency>[\\w]{3}) (?<gross>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Zum Preis von USD 3.04
                                // USD/CHF 0.86661 CHF 2.63
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency", "gross")
                                        .match("^Zum Preis von (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+)$")
                                        .match("^(?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+) (?<currency>[\\w]{3}) (?<gross>[\\.,'\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

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

        Block block = new Block("DIVIDENDENZAHLUNG$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // STUECKZAHL VALOR 957150 ISIN US6541061031 ANSATZ
                // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                // BRUTTO USD 6.10
                .section("wkn", "isin", "name", "tickerSymbol", "currency")
                .match("^STUECKZAHL VALOR (?<wkn>[0-9]{6,9}) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^[\\.,'\\d\\s]+ (?<name>.*) \\((?<tickerSymbol>.*)\\) BRUTTO$")
                .match("^BRUTTO (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 20 AKT -B- NIKE INC. (NKE) BRUTTO
                .section("shares")
                .match("^(?<shares>[\\.,'\\d\\s]+) .* \\(.*\\) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                .section("date")
                .match("^GUTSCHRIFT KONTO .* VALUTA (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,'\\d\\s]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // GUTSCHRIFT KONTO 292-614724.40R VALUTA 28.12.2021 CHF 3.85
                .section("currency", "amount")
                .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // BRUTTO USD 6.10
                // UMRECHNUNGSKURS USD/CHF 0.901639
                .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional()
                .match("^BRUTTO (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+)$")
                .match("^UMRECHNUNGSKURS (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d\\s]+)$")
                .match("^GUTSCHRIFT KONTO .* VALUTA [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) [\\.,'\\d\\s]+$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDepotAccountFeeTransaction()
    {
        DocumentType type = new DocumentType("Depotf.hrungspreis");
        this.addDocumentTyp(type);

        Block block = new Block("^Abrechnung vom .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        pdfTransaction
                // Position Basis Preis p.a. Betrag (CHF)
                // Depotführungspreis inkl. Steuern 101.82
                .section("currency", "amount")
                .match("^Position Basis Preis p\\.a\\. Betrag \\((?<currency>[\\w]{3})\\)$")
                .match("^Depotf.hrungspreis inkl\\. Steuern (?<amount>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Zu Lasten Konto 292-123456.40R, Valuta 30. Juni 2021
                .section("date")
                .match("^Zu Lasten Konto .* Valuta (?<date>[\\d]{1,2}.\\ .* [\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Abrechnung vom 01.04.2021 - 30.06.2021
                .section("note1", "note2").optional()
                .match("^(?<note1>Abrechnung) vom (?<note2>.*)$")
                .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // STEUERABZUG 30.00% USD -1.83
                .section("currency", "tax").optional()
                .match("^STEUERABZUG [\\.'\\d]+% (?<currency>[\\w]{3}) \\-(?<tax>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Eidgenössische Stempelabgabe CHF 1.65
                .section("currency", "tax").optional()
                .match("^Eidgen.ssische Stempelabgabe (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Courtage CHF 36.55
                // Rabatt e-banking CHF -6.55
                //
                // Courtage CHF -45.15
                // Rabatt e-banking CHF 5.15
                // @formatter:on
                .section("currency", "fee", "discountCurrency", "discount").optional()
                .match("^Courtage (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$")
                .match("^Rabatt e\\-banking (?<discountCurrency>[\\w]{3}) (\\-)?(?<discount>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> {
                    Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (fee.subtract(discount).isPositive())
                    {
                        fee = fee.subtract(discount);
                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }

                    type.getCurrentContext().put("noProvision", "X");
                })

                // Courtage USD -22.01
                // Courtage CHF 20.00
                .section("currency", "fee").optional()
                .match("^Courtage (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noProvision")))
                        processFeeEntries(t, v, type);
                })

                // Diverse USD -7.34
                .section("currency", "fee").optional()
                .match("^Diverse (?<currency>[\\w]{3}) \\-(?<fee>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Externe Gebühren CHF 1.50
                // Externe Gebühren USD -0.01
                .section("currency", "fee").optional()
                .match("^Externe Geb.hren (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }


    @Override
    protected long asShares(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
