package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
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
public class SBrokerPDFExtractor extends AbstractPDFExtractor
{
    public SBrokerPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("S Broker AG & Co. KG"); //$NON-NLS-1$
        addBankIdentifier("Sparkasse"); //$NON-NLS-1$
        addBankIdentifier("Stadtsparkasse"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "S Broker AG & Co. KG / Sparkasse"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Wertpapier Abrechnung )?(Ausgabe Investmentfonds|Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Wertpapier Abrechnung )?(Ausgabe Investmentfonds|Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Wertpapier Abrechnung )?(?<type>(Ausgabe Investmentfonds|Kauf|Verkauf)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Gattungsbezeichnung ISIN
                                // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                // STK 16,000 EUR 120,4000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("Gattungsbezeichnung ISIN")
                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Nominale Wertpapierbezeichnung ISIN (WKN)
                                // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                                // Ausführungskurs 71,253 EUR Auftragserteilung/ -ort Persönlich im Institut
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // STK 16,000 EUR 120,4000
                // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                // @formatter:on
                .section("shares")
                .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // @formatter:off
                                // Auftrag vom 27.02.2021 01:31:42 Uhr
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Auftrag vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Handelstag 05.05.2021 EUR 498,20-
                                // Handelszeit 09:04
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Schlusstag/-Zeit 14.10.2021 09:00:12 Auftraggeber XXXXXX XXXXXXX
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Schlusstag 05.11.2021
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Ausmachender Betrag 500,00- EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Wert Konto-Nr. Betrag zu Ihren Lasten
                                // 01.10.2014 10/0000/000 EUR 1.930,17
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .find("Wert Konto\\-Nr\\. Betrag zu Ihren (Gunsten|Lasten).*")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\/\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                // @formatter:off
                // Ausführungskurs 146,8444 USD Auftragserteilung/ -ort Persönlich im Institut
                // Devisenkurs (EUR/USD) 1,1762 vom 28.07.2021
                // Kurswert 15.605,81- EUR
                // @formatter:on
                .section("fxCurrency", "baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional()
                .match("^Ausf.hrungskurs [\\.,\\d]+ (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$")
                .match("^Kurswert (?<gross>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // Limit 189,40 EUR
                // @formatter:on
                .section("note").optional()
                .match("(?<note>Limit .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .conclude(ExtractorUtils.fixGrossValueBuySell())

                .wrap(t -> {
                    // If we have multiple entries in the document,
                    // then the "negative" flag must be removed.
                    type.getCurrentContext().remove("negative");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|"
                        + "Aussch.ttung|"
                        + "Gutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|"
                        + "Aussch.ttung (f.r|Investmentfonds)|"
                        + "Gutschrift)"
                        + "( [^\\.,\\d]+.*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Storno unserer Erträgnisgutschrift Nr. 81565205 vom 15.06.2016.
                .section("type").optional()
                .match("^(?<type>Storno) unserer Ertr.gnisgutschrift .*$")
                .assign((t, v) -> v.getTransactionContext().put(FAILURE,
                                Messages.MsgErrorOrderCancellationUnsupported))

                // @formatter:off
                // If we have a positive amount and a gross reinvestment,
                // there is a tax refund.
                // If the amount is negative, then it is taxes.
                // @formatter:on

                // @formatter:off
                // Ertragsthesaurierung
                // Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten
                // 15.01.2018 00/0000/000 EUR/USD 1,19265 EUR 0,65
                //
                // Storno - Ertragsthesaurierung
                // Wert Konto-Nr. Betrag zu Ihren Gunsten
                // 15.01.2018 00/0000/000 EUR 0,05
                // @formatter:on
                .section("type", "sign").optional()
                .match("^(Storno \\- )?(?<type>Ertragsthesaurierung)$")
                .match("^Wert Konto\\-Nr\\.( Devisenkurs)? Betrag zu Ihren (?<sign>(Gunsten|Lasten))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Ertragsthesaurierung") && v.get("sign").equals("Gunsten"))
                        t.setType(AccountTransaction.Type.TAX_REFUND);

                    if (v.get("type").equals("Ertragsthesaurierung") && v.get("sign").equals("Lasten"))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                .oneOf(
                                // @formatter:off
                                // Gattungsbezeichnung ISIN
                                // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("Gattungsbezeichnung ISIN")
                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^STK .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Nominale Wertpapierbezeichnung ISIN (WKN)
                                // Stück 250 GLADSTONE COMMERCIAL CORP. US3765361080 (260884)
                                // REGISTERED SHARES DL -,01
                                // Zahlbarkeitstag 31.12.2021 Ausschüttung pro St. 0,125275000 USD
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Aussch.ttung|Dividende|Auszahlung) pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // Stück 250 GLADSTONE COMMERCIAL CORP. US3765361080 (260884)
                // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                // @formatter:on
                .section("shares")
                .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // @formatter:off
                                // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Zahlbarkeitstag 31.12.2021 Ausschüttung pro St. 0,125275000 USD
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Wert Konto-Nr. Betrag zu Ihren Gunsten
                                // 17.11.2014 10/0000/000 EUR 12,70
                                //
                                // Wert Konto-Nr. Betrag zu Ihren Lasten
                                // 15.06.2016 00/0000/000 EUR 20,24
                                //
                                // Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten
                                // 15.12.2014 12/3456/789 EUR/USD 1,24495 EUR 52,36
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .find("Wert Konto\\-Nr\\.( Devisenkurs)? Betrag zu Ihren (Gunsten|Lasten)")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // @formatter:off
                                // Ausmachender Betrag 20,31+ EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs EUR / PLN 4,5044
                                // Ausschüttung 31,32 USD 27,48+ EUR
                                // @formatter:on
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$")
                                        .match("^(Dividendengutschrift|Aussch.ttung|Kurswert) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // 15.12.2014 12/3456/789 EUR/USD 1,24495 EUR 52,36
                                // ausländische Dividende EUR 70,32
                                // @formatter:on
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "currency", "gross")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$")
                                        .match("^ausl.ndische Dividende (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            v.put("fxCurrency", asCurrencyCode(v.get("termCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // 15.01.2018 00/0000/000 EUR/USD 1,19265 EUR 0,65
                                // ausl. Dividendenanteil (Thesaurierung) EUR 45,10
                                // @formatter:on
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "currency", "gross")
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$")
                                        .match("^ausl\\. Dividendenanteil \\(Thesaurierung\\) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("fxCurrency", asCurrencyCode(v.get("termCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                // @formatter:off
                // Ex-Tag 22.12.2021 Art der Dividende Quartalsdividende
                // @formatter:on
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // Ertrag für 2014/15 EUR 12,70
                // Ertrag für 2017 USD 54,16
                // @formatter:on
                .section("note1", "note2", "note3").optional()
                .match("^(?<note1>Ertrag f.r [\\d]{4}(\\/[\\d]{2})?) (?<note2>[\\w]{3}) (?<note3>[\\.,\\d]+)$")
                .assign((t, v) -> t.setNote(v.get("note1") + " (" + v.get("note3") + " " + v.get("note2") + ")"))

                // @formatter:off
                // Ertragsthesaurierung
                // Ertrag für 2017 USD 54,16
                // @formatter:on
                .section("note1", "note2", "note3", "note4").optional()
                .match("^(Storno \\- )?(?<note1>Ertragsthesaurierung)$")
                .match("^Ertrag (?<note2>f.r [\\d]{4}(\\/[\\d]{2})?) (?<note3>[\\w]{3}) (?<note4>[\\.,\\d]+)$")
                .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2") + " (" + v.get("note4") + " " + v.get("note3") + ")"))

                .wrap((t, ctx) -> {
                    // If we have multiple entries in the document, with
                    // taxes and tax refunds, then the "negative" flag
                    // must be removed.
                    type.getCurrentContext().remove("negative");

                    // If we have a gross reinvestment, then the "noTax"
                    // flag must be removed.
                    type.getCurrentContext().remove("noTax");

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                    {
                        TransactionItem item = new TransactionItem(t);
                        item.setFailureMessage(ctx.getString(FAILURE));
                        return item;
                    }
                    return null;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^(Kauf.*|Verkauf.*|Wertpapier Abrechnung Ausgabe Investmentfonds)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.TAX_REFUND);
                    return transaction;
                })

                .oneOf(
                                // @formatter:off
                                // Gattungsbezeichnung ISIN
                                // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                // STK 16,000 EUR 120,4000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("Gattungsbezeichnung ISIN")
                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3} [\\.,\\d]+)$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Nominale Wertpapierbezeichnung ISIN (WKN)
                                // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                                // Ausführungskurs 71,253 EUR Auftragserteilung/ -ort Persönlich im Institut
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // STK 16,000 EUR 120,4000
                // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                // @formatter:on
                .section("shares")
                .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                // 03.06.2015 10/3874/009 87966195 EUR 11,48
                // @formatter:on
                .section("date", "amount", "currency").optional()
                .find("Wert Konto\\-Nr\\. Abrechnungs\\-Nr\\. Betrag zu Ihren Gunsten")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\/\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a tax refunds, we set a flag and don't book tax below.
        transaction
                .section("n").optional()
                .match("^zu versteuern \\(negativ\\) (?<n>.*)$")
                .assign((t, v) -> type.getCurrentContext().put("negative", "X"));

        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.
        transaction
                .section("n").optional()
                .match("^(?<n>Ertragsthesaurierung)$")
                .assign((t, v) -> type.getCurrentContext().put("noTax", "X"));

        transaction
                // @formatter:off
                // einbehaltene Kapitalertragsteuer EUR 7,03
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^einbehaltene Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer 24,51 % auf 11,00 EUR 2,70- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kapitalertragsteuer EUR 70,16
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // einbehaltener Solidaritätszuschlag EUR 0,38
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^einbehaltener Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solidaritätszuschlag EUR 3,86
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solidaritätszuschlag 5,5 % auf 2,70 EUR 0,14- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // einbehaltener Kirchensteuer EUR 1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^einbehaltener Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer EUR 1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer 8 % auf 2,70 EUR 0,21- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Einbehaltene Quellensteuer 15 % auf 31,32 USD 4,12- EUR
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltene Quellensteuer .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                })

                // @formatter:off
                // Anrechenbare Quellensteuer pro Stück 0,01879125 USD 4,70 USD
                // @formatter:on
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // @formatter:off
                // davon anrechenbare Quellensteuer Fondseingangsseite EUR 0,04
                // @formatter:on
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^davon anrechenbare Quellensteuer Fondseingangsseite (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                })

                // @formatter:off
                // davon anrechenbare US-Quellensteuer 15% USD 13,13
                // @formatter:on
                .section("currency", "creditableWithHoldingTax").optional()
                .match("^davon anrechenbare US\\-Quellensteuer [\\.,\\d]+% (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) && !"X".equals(type.getCurrentContext().get("noTax")))
                        processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Handelszeit 09:02 Orderentgelt                EUR 10,90-
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Orderentgelt ([\\s]+)?(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Orderentgelt
                // EUR 0,71-
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Orderentgelt$")
                .match("^(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Börse Stuttgart Börsengebühr EUR 2,29-
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* B.rsengeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision 1,48- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Transaktionsentgelt Börse 0,60- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Übertragungs-/Liefergebühr 0,12- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision 2,5015 % vom Kurswert 1,25- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % vom Kurswert (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Kurswert 509,71- EUR
                // Kundenbonifikation 40 % vom Ausgabeaufschlag 9,71 EUR
                // Ausgabeaufschlag pro Anteil 5,00 %
                // @formatter:on
                .section("amount", "currency", "discount", "discountCurrency", "percentageFee").optional()
                .match("^Kurswert (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .match("^Kundenbonifikation [\\.,\\d]+ % vom Ausgabeaufschlag (?<discount>[\\.,\\d]+) (?<discountCurrency>[\\w]{3})$")
                .match("^Ausgabeaufschlag pro Anteil (?<percentageFee>[\\.,\\d]+) %$")
                .assign((t, v) -> {
                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                    {
                        // @formatter:off
                        // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        // @formatter:on
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        // @formatter:off
                        // fee = fee - discount
                        // @formatter:on
                        fee = fee.subtract(discount);

                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }
                })

                // @formatter:off
                // Kurswert
                // EUR 14,40-
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Kurswert$")
                .match("^(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)\\-$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
