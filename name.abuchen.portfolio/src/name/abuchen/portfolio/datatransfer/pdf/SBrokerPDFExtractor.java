package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;

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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SBrokerPDFExtractor extends AbstractPDFExtractor
{
    public SBrokerPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("S Broker AG & Co. KG");
        addBankIdentifier("Sparkasse");
        addBankIdentifier("Stadtsparkasse");
        addBankIdentifier("Kreissparkasse");

        addBuySellTransaction();
        addDividendTransaction();
        addAdvanceTaxTransaction();
        addBuyTransactionFundsSavingsPlan();
        addAccountStatementTransaction();
        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "S Broker AG & Co. KG / Sparkasse / StarMoney";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(?<![\\d]{4} )" //
                        + "(Wertpapier Abrechnung" //
                        + "|Wertpapierabrechnung)", //
                        "Kontoauszug [\\d]+\\/[\\d]{4} .*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Wertpapier Abrechnung|Wertpapierabrechnung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional()
                        .match("^(Wertpapier Abrechnung )?(?<type>(Ausgabe Investmentfonds|Kauf|Verkauf)).*$")
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                        // STK 16,000 EUR 120,4000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                                        // Ausführungskurs 71,253 EUR Auftragserteilung/ -ort Persönlich im Institut
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // STK 16,000 EUR 120,4000
                        // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                        // @formatter:on
                        .section("shares") //
                        .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Auftrag vom 27.02.2021 01:31:42 Uhr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Auftrag vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handelstag 05.05.2021 EUR 498,20-
                                        // Handelszeit 09:04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .match("^Handelszeit (?<time>[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag/-Zeit 14.10.2021 09:00:12 Auftraggeber XXXXXX XXXXXXX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 05.11.2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag 500,00- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                                        // 01.10.2014 10/0000/000 EUR 1.930,17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Wert Konto\\-Nr\\. Betrag zu Ihren (Gunsten|Lasten).*") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\/\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Wert Betrag zu Ihren Lasten
                                        // 07.05.2020 EUR 460,91
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Wert Betrag zu Ihren (Gunsten|Lasten).*") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Devisenkurs (EUR/USD) 1,1762 vom 28.07.2021
                        // Kurswert 15.605,81- EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "gross").optional() //
                        .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .match("^Kurswert (?<gross>[\\.,\\d]+)\\- [\\w]{3}$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Herrn        Depot-Nr. Abrechnungs-Nr. ADRESSZEILE4=PLZ Stadt
                                        // Vorname Name 100/0000/000 10000000 ADRESSZEILE5=
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "note1") //
                                                        .match("^.* (?<note>Abrechnungs\\-Nr\\.) .*$") //
                                                        .match("^.*[\\d]+\\/[\\d]+\\/[\\d]+ (?<note1>.*) .*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " "
                                                                        + trim(v.get("note1")))),
                                        // @formatter:off
                                        // Depot-Nr. Abrechnungs-Nr.
                                        // 111/2222/002 65091167
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "note1") //
                                                        .match("^.* (?<note>Abrechnungs\\-Nr\\.)$") //
                                                        .match("^[\\d]+\\/[\\d]+\\/[\\d]+ (?<note1>.*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " "
                                                                        + trim(v.get("note1")))),
                                        // @formatter:off
                                        // Abrechnungsnr. 12345678
                                        //  XXXX XXXAuftragsnummer XXXXXX/XX.XX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>(Abrechnungsnr\\.|Auftragsnummer) .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        // @formatter:off
                        // Limit 189,40 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

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
        DocumentType type = new DocumentType("(Dividendengutschrift|" //
                        + "Aussch.ttung|" //
                        + "Kapitalr.ckzahlung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividendengutschrift" //
                        + "|Aussch.ttung (f.r|Investmentfonds)" //
                        + "|Gutschrift)" //
                        + "( [^\\.,\\d]+.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Storno unserer Erträgnisgutschrift Nr. 81565205 vom 15.06.2016.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Storno) unserer Ertr.gnisgutschrift .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

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
                        //
                        // Ertragsthesaurierung
                        // Wert Betrag zu Ihren Gunsten
                        // 15.01.2018 EUR 4,69
                        // @formatter:on
                        .section("type", "sign").optional() //
                        .match("^(Storno \\- )?(?<type>Ertragsthesaurierung)$") //
                        .match("^Wert( Konto\\-Nr\\.)?( Devisenkurs)? Betrag zu Ihren (?<sign>(Gunsten|Lasten))$") //
                        .assign((t, v) -> {
                            if ("Ertragsthesaurierung".equals(v.get("type")) && "Gunsten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            if ("Ertragsthesaurierung".equals(v.get("type")) && "Lasten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Gattungsbezeichnung ISIN
                                        // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                        // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK .* (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 250 GLADSTONE COMMERCIAL CORP. US3765361080 (260884)
                                        // REGISTERED SHARES DL -,01
                                        // Zahlbarkeitstag 31.12.2021 Ausschüttung pro St. 0,125275000 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Aussch.ttung|Dividende|Auszahlung) pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Stück 250 GLADSTONE COMMERCIAL CORP. US3765361080 (260884)
                        // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                        // @formatter:on
                        .section("shares") //
                        .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 16,000 17.11.2014 17.11.2014 EUR 0,793806
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^STK [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zahlbarkeitstag 31.12.2021 Ausschüttung pro St. 0,125275000 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
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
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Wert Konto\\-Nr\\.( Devisenkurs)? Betrag zu Ihren (Gunsten|Lasten)") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Wert Betrag zu Ihren Gunsten
                                        // 15.11.2016 EUR 41,44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Wert Betrag zu Ihren (Gunsten|Lasten)") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Wert Devisenkurs Betrag zu Ihren Gunsten
                                        // 07.09.2021 EUR/USD 1,19025 EUR 5,30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Wert Devisenkurs Betrag zu Ihren (Gunsten|Lasten)") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Ausmachender Betrag 20,31+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Thesaurierung brutto EUR 0,52
                                        // zu versteuern EUR 0,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type", "amount", "currency") //
                                                        .match("^(Storno \\- )?(?<type>Ertragsthesaurierung)$") //
                                                        .match("^zu versteuern (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            if (asAmount(v.get("amount")) == 0)
                                                            {
                                                                t.setType(AccountTransaction.Type.TAXES);

                                                                t.setAmount(asAmount(v.get("amount")));
                                                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                                Money fxAmount = Money.of(asCurrencyCode(t.getSecurity().getCurrencyCode()), t.getMonetaryAmount().getAmount());
                                                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), fxAmount, BigDecimal.ONE));

                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                                                            }
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs EUR / PLN 4,5044
                                        // Ausschüttung 31,32 USD 27,48+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^(Dividendengutschrift|Aussch.ttung|Kurswert) (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 15.12.2014 12/3456/789 EUR/USD 1,24495 EUR 52,36
                                        // ausländische Dividende EUR 70,32
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^ausl.ndische Dividende [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 07.09.2021 EUR/USD 1,19025 EUR 5,30
                                        // ausländische Dividende EUR 70,32
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^ausl.ndische Dividende [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            v.put("fxCurrency", asCurrencyCode(v.get("termCurrency")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 15.01.2018 00/0000/000 EUR/USD 1,19265 EUR 0,65
                                        // ausl. Dividendenanteil (Thesaurierung) EUR 45,10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$") //
                                                        .match("^ausl\\. Dividendenanteil \\(Thesaurierung\\) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            v.put("fxCurrency", asCurrencyCode(v.get("termCurrency")));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Depot-Nr. Abrechnungs-Nr.
                                        // 000/0000/000 70314707
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^.* (?<note1>Abrechnungs\\-Nr\\.) .*$") //
                                                        .match("^[\\d]+\\/[\\d]+\\/[\\d]+ (?<note2>[\\d]+) .*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2")))),
                                        // @formatter:off
                                        // Abrechnungsnr. 12345678
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Abrechnungsnr\\. .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Ex-Tag 22.12.2021 Art der Dividende Quartalsdividende
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.* Art der Dividende (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "))),
                                        // @formatter:off
                                        // Ertragsthesaurierung
                                        // Ertrag für 2017 USD 54,16
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3", "note4") //
                                                        .match("^(Storno \\- )?(?<note1>Ertragsthesaurierung)$") //
                                                        .match("^Ertrag (?<note2>f.r [\\d]{4}(\\/[\\d]{2})?) (?<note3>[\\w]{3}) (?<note4>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + " " + v.get("note2") + " (" + v.get("note4") + " " + v.get("note3") + ")", " | "))),
                                        // @formatter:off
                                        // Ertrag für 2014/15 EUR 12,70
                                        // Ertrag für 2017 USD 54,16
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3") //
                                                        .match("^(?<note1>Ertrag f.r [\\d]{4}(\\/[\\d]{2})?) (?<note2>[\\w]{3}) (?<note3>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + " (" + v.get("note3") + " " + v.get("note2") + ")", " | "))),
                                        // @formatter:off
                                        // Thesaurierung brutto EUR 0,52
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2", "note3") //
                                                        .match("^(?<note1>Thesaurierung) .* (?<note2>[\\w]{3}) (?<note3>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + " (" + v.get("note3") + " " + v.get("note2") + ")", " | "))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            // If we have multiple entries in the document, with
                            // taxes and tax refunds, then the "negative" flag
                            // must be removed.
                            type.getCurrentContext().remove("negative");

                            // If we have a gross reinvestment, then the "noTax"
                            // flag must be removed.
                            type.getCurrentContext().remove("noTax");

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Vorabpauschale.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stück 155 ISHSIV-AUTOMATION&ROBOT.U.ETF IE00BYZK4552 (A2ANH0)
                        // REGISTERED SHARES O.N.
                        // Zahlbarkeitstag 02.01.2024 Vorabpauschale pro St. 0,160196610 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "name1", "currency") //
                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Vorabpauschale pro St\\. [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Stück 155 ISHSIV-AUTOMATION&ROBOT.U.ETF IE00BYZK4552 (A2ANH0)
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbarkeitstag 02.01.2024 Vorabpauschale pro St. 0,160196610 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag 0,08- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Berechnungsgrundlage für die Kapitalertragsteuer 0,00+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Berechnungsgrundlage f.r die Kapitalertrags(s)?teuer (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Abrechnungsnr. 51274930950
                        // @formatter:on
                        .section("note") //
                        .match("^(?<note>Abrechnungsnr\\. .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addBuyTransactionFundsSavingsPlan()
    {
        final DocumentType type = new DocumentType("Halbjahresabrechnung Sparplan", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // DEKA-GLOBALCHAMPIONS DE000DK0ECU8 (DK0ECU)
                                        // @formatter:on
                                        .section("name", "isin", "wkn") //
                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("name", trim(v.get("name")));
                                            ctx.put("isin", v.get("isin"));
                                            ctx.put("wkn", v.get("wkn"));
                                        })

                                        // @formatter:off
                                        // Im Abrechnungszeitraum angelegter Betrag EUR 595,04
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Im Abrechnungszeitraum angelegter Betrag (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Kauf [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Kauf 145,00 172520/34.00 282,3348 1,0000 0,5269 02.07.2021 06.07.2021 0,00 0,00
                        // + Anlagerabatt 3,76 Summe 148,76
                        // @formatter:on
                        .section("note", "amount", "shares", "date") //
                        .documentContext("name", "isin", "wkn", "currency") //
                        .match("^Kauf [\\.,\\d]+ (?<note>[\\d]+\\/[\\.\\d]+).* (?<shares>[\\.,\\d]+) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .*$") //
                        .match("^\\+ Anlagerabatt [\\.,\\d]+ Summe (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));
                            t.setNote("Auftragsnummer " + trim(v.get("note")));

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        // Datum Erläuterung Betrag Soll EUR Betrag Haben EUR
                                                        // Datum Wert Erläuterung Betrag Soll EUR Betrag Haben EUR
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Datum( Wert)? Erl.uterung Betrag Soll [\\w]{3} Betrag Haben (?<currency>[\\w]{3})$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))),
                                                        // @formatter:off
                                                        // Buch.-Tag Wert Verwendungszweck/Erläuterungen Umsatz (EUR)
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^Buch\\.-Tag Wert Verwendungszweck\\/Erl.uterungen Umsatz \\((?<currency>[\\w]{3})\\)$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))));

        this.addDocumentTyp(type);

        // @formatter:off
        //              -600,00
        // 02.11.2021 Dauerauftrag / Wert: 01.11.2021
        //
        //              -34,50
        // 02.11.2021 Lastschrift
        // @formatter:on
        Block depositRemovalBlock_Format01 = new Block("^.* [\\-|\\+|\\s][\\.,\\d]+$");
        depositRemovalBlock_Format01.setMaxSize(2);
        type.addBlock(depositRemovalBlock_Format01);
        depositRemovalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date", "note").optional() //
                        .documentContext("currency") //
                        .match("^.* (?<type>[\\-|\\+|\\s])(?<amount>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>(Lastschrift" //
                                        + "|.berweisung online" //
                                        + "|.berweisung" //
                                        + "|.berweisung Vordruck" //
                                        + "|Kartenzahlung" //
                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                        + "|Rechnung" //
                                        + "|Gutschrift.berweisung" //
                                        + "|Gutschrift .berw\\." //
                                        + "|SEPA GUTSCHRIFT" //
                                        + "|Dauerauftrag" //
                                        + "|Scheckeinzug" //
                                        + "|Lohn, Gehalt, Rente" //
                                        + "|Basis\\-Lastschrift" //
                                        + "|Basislastschrift" //
                                        + "|Zahlungseingang" //
                                        + "|Bargeldeinzahlung" //
                                        + "|Bargeldeinzahlung SB" //
                                        + "|Geldautomat" //
                                        + "|Bargeldauszahlung" //
                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                        + "|BargAuszDebitFremdGA" //
                                        + "|Barumsatz" //
                                        + "|sonstige Buchung" //
                                        + "|sonstige Entgelte" //
                                        + "|entgeltfreie Buchung))" //
                                        + ".*$") //
                        .assign((t, v) -> {
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // @formatter:off
                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                            // @formatter:on
                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                v.put("note", "Gutschrift (Überweisung)");

                            if ("Gutschrift Überw.".equals(v.get("note")))
                                v.put("note", "Gutschrift (Überweisung)");

                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                v.put("note", "Eigene Kreditkartenabrechnung");

                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "SEPA Gutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            if (t.getDateTime() == null && t.getNote() == null)
                                return null;

                            return item;
                        }));

        // @formatter:off
        // 02.03.2020 02.03.2020 Lastschrift              2,00-
        // 02.03.2020 02.03.2020 Überweisung online              1,00-
        // 01.03.2016 01.03.2016 Basis-Lastschrift              119,00-
        // 01.03.2016 01.03.2016 Zahlungseingang               130,00+
        // 06.04.2017 06.04.2017 Überweisung            3.000,00-
        // 02.05.2018 02.05.2018 Basislastschrift              260,00-
        // 01.04.2019 30.03.2019 Bargeldeinzahlung SB              500,00+
        // 13.06.2019 13.06.2019 Überweisung Vordruck              800,00-
        // 22.03.2019 21.03.2019 BargAuszDebitFremdGA              500,00-
        //
        // . 16.08.2019 16.08.2019 Lastschrift               12,37-
        //  1 3.11.2017 13.11.2017 Zahlungseingang              289,00+
        // . 1.TAN 00023613.06.2016 13.06.2016 Überweisung              161,00-
        // @formatter:on
        Block depositRemovalBlock_Format02 = new Block("^.*\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(Lastschrift" //
                                        + "|.berweisung online" //
                                        + "|.berweisung" //
                                        + "|.berweisung Vordruck" //
                                        + "|Kartenzahlung" //
                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                        + "|Rechnung" //
                                        + "|Gutschrift.berweisung" //
                                        + "|SEPA GUTSCHRIFT" //
                                        + "|Dauerauftrag" //
                                        + "|Scheckeinzug" //
                                        + "|Lohn, Gehalt, Rente" //
                                        + "|Basis\\-Lastschrift" //
                                        + "|Basislastschrift" //
                                        + "|Zahlungseingang" //
                                        + "|Bargeldeinzahlung" //
                                        + "|Bargeldeinzahlung SB" //
                                        + "|Geldautomat" //
                                        + "|Bargeldauszahlung" //
                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                        + "|BargAuszDebitFremdGA" //
                                        + "|Barumsatz" //
                                        + "|sonstige Buchung" //
                                        + "|sonstige Entgelte" //
                                        + "|entgeltfreie Buchung) " //
                                        + ".*[\\.,\\d][\\-|\\+].*$");
        depositRemovalBlock_Format02.setMaxSize(1);
        type.addBlock(depositRemovalBlock_Format02);
        depositRemovalBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount") //
                        .documentContext("currency") //
                        .match("^.*.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>(Lastschrift" //
                                        + "|.berweisung online" //
                                        + "|.berweisung" //
                                        + "|.berweisung Vordruck" //
                                        + "|Kartenzahlung" //
                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                        + "|Rechnung" //
                                        + "|Gutschrift.berweisung" //
                                        + "|SEPA GUTSCHRIFT" //
                                        + "|Dauerauftrag" //
                                        + "|Scheckeinzug" //
                                        + "|Lohn, Gehalt, Rente" //
                                        + "|Basis\\-Lastschrift" //
                                        + "|Basislastschrift" //
                                        + "|Zahlungseingang" //
                                        + "|Bargeldeinzahlung" //
                                        + "|Bargeldeinzahlung SB" //
                                        + "|Geldautomat" //
                                        + "|Bargeldauszahlung" //
                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                        + "|BargAuszDebitFremdGA" //
                                        + "|Barumsatz" //
                                        + "|sonstige Buchung" //
                                        + "|sonstige Entgelte" //
                                        + "|entgeltfreie Buchung)) " //
                                        + "[\\s]+" //
                                        + "(?<amount>[\\.,\\d]+)" //
                                        + "(?<type>[\\-|\\+]).*$") //
                        .assign((t, v) -> {
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // @formatter:off
                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                            // @formatter:on
                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                v.put("note", "Gutschrift (Überweisung)");

                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                v.put("note", "Eigene Kreditkartenabrechnung");

                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "SEPA Gutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        }));

        Block depositRemovalBlock_Format03 = new Block("^.*[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        depositRemovalBlock_Format03.setMaxSize(3);
        type.addBlock(depositRemovalBlock_Format03);
        depositRemovalBlock_Format03.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 25.09.2020 25.09.2020
                                        // Lastschrift
                                        //                9,75-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^.*[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .match("^(?<note>(Lastschrift" //
                                                                        + "|.berweisung online" //
                                                                        + "|.berweisung" //
                                                                        + "|.berweisung Vordruck" //
                                                                        + "|Kartenzahlung" //
                                                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                                                        + "|Rechnung" //
                                                                        + "|Gutschrift.berweisung" //
                                                                        + "|SEPA GUTSCHRIFT" //
                                                                        + "|Dauerauftrag" //
                                                                        + "|Scheckeinzug" //
                                                                        + "|Lohn, Gehalt, Rente" //
                                                                        + "|Basis\\-Lastschrift" //
                                                                        + "|Basislastschrift" //
                                                                        + "|Zahlungseingang" //
                                                                        + "|Bargeldeinzahlung" //
                                                                        + "|Bargeldeinzahlung SB" //
                                                                        + "|Geldautomat" //
                                                                        + "|Bargeldauszahlung" //
                                                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                                                        + "|BargAuszDebitFremdGA" //
                                                                        + "|Barumsatz" //
                                                                        + "|sonstige Buchung" //
                                                                        + "|sonstige Entgelte" //
                                                                        + "|entgeltfreie Buchung))$") //
                                                        .match("^[\\s]+ (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+]).*$") //
                                                        .assign((t, v) -> {
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            // @formatter:off
                                                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                                                            // @formatter:off
                                                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // Formatting some notes
                                                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                                                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                                                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                                                v.put("note", "Gutschrift (Überweisung)");

                                                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                                                v.put("note", "Eigene Kreditkartenabrechnung");

                                                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                                                v.put("note", "SEPA Gutschrift");

                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 08.06.2020 08.06.2020
                                        // Überweisung online               25,00-
                                        //
                                        // 10.01.2020 10.01.2020
                                        // . GutschriftÜberweisung               15,00+
                                        //
                                        // 08.07.2020 08.07.2020
                                        //   Kartenzahlung               24,41-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^.*[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .match("^(.* )?(?<note>(Lastschrift" //
                                                                        + "|.berweisung online" //
                                                                        + "|.berweisung" //
                                                                        + "|.berweisung Vordruck" //
                                                                        + "|Kartenzahlung" //
                                                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                                                        + "|Rechnung" //
                                                                        + "|Gutschrift.berweisung" //
                                                                        + "|SEPA GUTSCHRIFT" //
                                                                        + "|Dauerauftrag" //
                                                                        + "|Scheckeinzug" //
                                                                        + "|Lohn, Gehalt, Rente" //
                                                                        + "|Basis\\-Lastschrift" //
                                                                        + "|Basislastschrift" //
                                                                        + "|Zahlungseingang" //
                                                                        + "|Bargeldeinzahlung" //
                                                                        + "|Bargeldeinzahlung SB" //
                                                                        + "|Geldautomat" //
                                                                        + "|Bargeldauszahlung" //
                                                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                                                        + "|BargAuszDebitFremdGA" //
                                                                        + "|Barumsatz" //
                                                                        + "|sonstige Buchung" //
                                                                        + "|sonstige Entgelte" //
                                                                        + "|entgeltfreie Buchung))" //
                                                                        + "[\\s]+ (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+]).*$") //
                                                        .assign((t, v) -> {
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            // @formatter:off
                                                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                                                            // @formatter:off
                                                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // Formatting some notes
                                                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                                                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                                                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                                                v.put("note", "Gutschrift (Überweisung)");

                                                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                                                v.put("note", "Eigene Kreditkartenabrechnung");

                                                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                                                v.put("note", "SEPA Gutschrift");

                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;

                            return null;
                        }));

        // @formatter:off
        // 30.06 30.06.14 Überweisung -400,00
        // 07.07 07.07.14 Geldautomat -500,00
        // 01.07 01.07.14 Lastschrift -28,50
        // 18.09 18.09.14 Basis-Lastschrift -40,00
        // 26.09 26.09.14 Lohn, Gehalt, Rente 1.835,19
        // 29.09 29.09.14 Zahlungseingang 435,54
        // 22.06 22.06.15 Kartenzahlung -60,82
        // 23.06 23.06.15 Barumsatz 500,00
        // @formatter:on
        Block depositRemovalBlock_Format04 = new Block("^.*[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} " //
                        + "(Lastschrift" //
                        + "|.berweisung online" //
                        + "|.berweisung" //
                        + "|Kartenzahlung" //
                        + "|EIGENE KREDITKARTENABRECHN\\." //
                        + "|Rechnung" //
                        + "|Gutschrift.berweisung" //
                        + "|SEPA GUTSCHRIFT" //
                        + "|Dauerauftrag" //
                        + "|Scheckeinzug" //
                        + "|Lohn, Gehalt, Rente" //
                        + "|Basis\\-Lastschrift" //
                        + "|Basislastschrift" //
                        + "|Zahlungseingang" //
                        + "|Bargeldeinzahlung" //
                        + "|Geldautomat" //
                        + "|Bargeldauszahlung" //
                        + "|Bargeldausz\\.Debitk\\.GA" //
                        + "|BargAuszDebitFremdGA" //
                        + "|Barumsatz" //
                        + "|sonstige Buchung" //
                        + "|sonstige Entgelte" //
                        + "|entgeltfreie Buchung) " //
                        + "(\\-)?[\\.,\\d]+$"); //
        depositRemovalBlock_Format04.setMaxSize(1);
        type.addBlock(depositRemovalBlock_Format04);
        depositRemovalBlock_Format04.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                        + "(?<note>(Lastschrift" //
                                        + "|.berweisung online" //
                                        + "|.berweisung" //
                                        + "|Kartenzahlung" //
                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                        + "|Rechnung" //
                                        + "|Gutschrift.berweisung" //
                                        + "|SEPA GUTSCHRIFT" //
                                        + "|Dauerauftrag" //
                                        + "|Scheckeinzug" //
                                        + "|Lohn, Gehalt, Rente" //
                                        + "|Basis\\-Lastschrift" //
                                        + "|Basislastschrift" //
                                        + "|Zahlungseingang" //
                                        + "|Bargeldeinzahlung" //
                                        + "|Geldautomat" //
                                        + "|Bargeldauszahlung" //
                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                        + "|BargAuszDebitFremdGA" //
                                        + "|Barumsatz" //
                                        + "|sonstige Buchung" //
                                        + "|sonstige Entgelte" //
                                        + "|entgeltfreie Buchung))" //
                                        + "(?<type>[\\s|\\-]+)(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // @formatter:off
                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                            // @formatter:on
                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                v.put("note", "Gutschrift (Überweisung)");

                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                v.put("note", "Eigene Kreditkartenabrechnung");

                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "SEPA Gutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;

                            return null;
                        }));

        // @formatter:off
        // 27.12.2019 27.12.2019 Lastschrift
        // .                6,60-Lotto24 AG
        // @formatter:on
        Block depositRemovalBlock_Format05 = new Block("^.*[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                        + "(Lastschrift" //
                        + "|.berweisung online" //
                        + "|.berweisung" //
                        + "|Kartenzahlung" //
                        + "|EIGENE KREDITKARTENABRECHN\\." //
                        + "|Rechnung" //
                        + "|Gutschrift.berweisung" //
                        + "|SEPA GUTSCHRIFT" //
                        + "|Dauerauftrag" //
                        + "|Scheckeinzug" //
                        + "|Lohn, Gehalt, Rente" //
                        + "|Basis\\-Lastschrift" //
                        + "|Basislastschrift" //
                        + "|Zahlungseingang" //
                        + "|Bargeldeinzahlung" //
                        + "|Geldautomat" //
                        + "|Bargeldauszahlung" //
                        + "|Bargeldausz\\.Debitk\\.GA" //
                        + "|BargAuszDebitFremdGA" //
                        + "|Barumsatz" //
                        + "|sonstige Buchung" //
                        + "|sonstige Entgelte" //
                        + "|entgeltfreie Buchung)$"); //
        depositRemovalBlock_Format05.setMaxSize(2);
        type.addBlock(depositRemovalBlock_Format05);
        depositRemovalBlock_Format05.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .match("^.*[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>(Lastschrift" //
                                        + "|.berweisung online" //
                                        + "|.berweisung" //
                                        + "|Kartenzahlung" //
                                        + "|EIGENE KREDITKARTENABRECHN\\." //
                                        + "|Rechnung" //
                                        + "|Gutschrift.berweisung" //
                                        + "|SEPA GUTSCHRIFT" //
                                        + "|Dauerauftrag" //
                                        + "|Scheckeinzug" //
                                        + "|Lohn, Gehalt, Rente" //
                                        + "|Basis\\-Lastschrift" //
                                        + "|Basislastschrift" //
                                        + "|Zahlungseingang" //
                                        + "|Bargeldeinzahlung" //
                                        + "|Geldautomat" //
                                        + "|Bargeldauszahlung" //
                                        + "|Bargeldausz\\.Debitk\\.GA" //
                                        + "|BargAuszDebitFremdGA" //
                                        + "|Barumsatz" //
                                        + "|sonstige Buchung" //
                                        + "|sonstige Entgelte" //
                                        + "|entgeltfreie Buchung))$") //
                        .match("^[\\.\\s]+ (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+]).*$") //
                        .assign((t, v) -> {
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // @formatter:off
                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                            // @formatter:off
                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Bargeldausz.Debitk.GA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte)");

                            if ("BargAuszDebitFremdGA".equals(v.get("note")))
                                v.put("note", "Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)");

                            if ("GutschriftÜberweisung".equals(v.get("note")))
                                v.put("note", "Gutschrift (Überweisung)");

                            if ("EIGENE KREDITKARTENABRECHN.".equals(v.get("note")))
                                v.put("note", "Eigene Kreditkartenabrechnung");

                            if ("SEPA GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "SEPA Gutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return item;

                            return null;
                        }));

        Block feesBlock = new Block("^Entgelte vom .*$", "^Abrechnung .*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Entgelte vom 29.02.2020 bis 31.03.2020                               4,70-
                                        //
                                        // Grundpreis (Kontoführung)                              2,00-
                                        // Zahlungsverkehr                                        2,70-
                                        //                                                             --------------
                                        // Abrechnung 31.03.2020                                                4,70-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^(?<note>Entgelte vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .match("^Abrechnung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<amount>[\\.,\\d]+)(?<type>([\\-|\\+])?)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("-".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            // @formatter:off
                                                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                                                            // @formatter:on
                                                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // Entgelte vom 01.11.2012 bis 30.11.2012                               4,30S
                                        // Entgelte / Auslagen                                    4,30S
                                        // abzüglich 100% Nachlass auf        1,80S               1,80H         1,80H
                                        // --------------
                                        // Abrechnung 30.11.2012                                                2,50S
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^(?<note>Entgelte vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .match("^Abrechnung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<amount>[\\.,\\d]+)(?<type>([S|H])?)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from FEES_REFUND to FEES
                                                            // @formatter:on
                                                            if ("S".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            // @formatter:off
                                                            // Is note equal "sonstige Entgelte" change from DEPOSIT to FEE
                                                            // @formatter:on
                                                            if ("sonstige Entgelte".equals(trim(v.get("note"))))
                                                                t.setType(AccountTransaction.Type.FEES);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        }));

        Block interestBlock = new Block("^Abrechnungszeitraum vom .*$", "^Abrechnung .*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Abrechnungszeitraum vom 01.01.2020 bis 31.03.2020
                                        // Zinsen für eingeräumte Kontoüberziehung                              0,66-
                                        // 10,0500 v.H. Kred-Zins  bis 29.03.2020
                                        //
                                        //                                                             --------------
                                        // Abrechnung 31.03.2020                                                0,66-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^(?<note>Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .match("^Abrechnung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<amount>[\\.,\\d]+)(?<type>[\\-|\\+])$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                                                            // @formatter:on
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // Abrechnungszeitraum vom 01.07.2008 bis 30.09.2008
                                        // Zinsen für Konto-/Kreditüberziehungen                                0,15S
                                        // --------------
                                        // Abrechnung 30.09.2008                                                0,15S
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^(?<note>Abrechnungszeitraum vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .match("^Abrechnung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<amount>[\\.,\\d]+)(?<type>[S|H])$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from INTEREST to INTEREST_CHARGE
                                                            // @formatter:on
                                                            if ("S".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        }));

        Block taxReturnBlock_Format01 = new Block("^.* [\\-|\\+|\\s][\\.,\\d]+$");
        taxReturnBlock_Format01.setMaxSize(3);
        type.addBlock(taxReturnBlock_Format01);
        taxReturnBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("type", "amount", "date", "note").optional() //
                        .documentContext("currency") //
                        .match("^.* (?<type>[\\-|\\+|\\s])(?<amount>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>Buchung beleglos).*$") //
                        .match("[\\d]+ Steuerausgleich Kapitalertragsteuer") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from TAX_REFUND to TAXES
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.TAXES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private void addCreditcardStatementTransaction()
    {
        final DocumentType type = new DocumentType("Ihre Abrechnung vom ", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Beleg BuchungVerwendungszweck EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Beleg BuchungVerwendungszweck (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} .* [\\.,\\d]+([\\s])?([\\-|\\+])$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 23.01.21 25.01.21 EVERDRIVE.ME, KRAKOW USD 181,00 1,2192 148,46 -
                                        // 10.09.19 05.11.19 WWW.ALIEXPRESS.COM, LONDON USD 31,49 1,10279 28,55+
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                                                        + "(?<note>.*) " //
                                                                        + "[\\w]{3} [\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<amount>[\\.,\\d]+)" //
                                                                        + "([\\s])?(?<type>[\\-|\\+])$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // Formatting some notes
                                                            if (trim(v.get("note")).endsWith(","))
                                                                v.put("note", trim(v.get("note")).substring(0, trim(v.get("note")).length() - 1));

                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 02.09.22 03.09.22 Lastschrift 1.345,61+
                                        // 14.09.22 03.09.22 PAYPAL *EBAY DE, 38888899999 20,29+
                                        //
                                        // 04.09.22 06.09.22 PAYPAL *ANNA.JAEGER97, 12,90 -
                                        // 22.09.22 23.09.22 PAYPAL *BRITTAWENDLAND, 10,00 -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})" //
                                                                        + "(?<note>.*) " //
                                                                        + "(?<amount>[\\.,\\d]+)" //
                                                                        + "([\\s])?(?<type>[\\-|\\+])$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(v.get("type")))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));

                                                            // Formatting some notes
                                                            if (trim(v.get("note")).endsWith(","))
                                                                v.put("note", trim(v.get("note")).substring(0, trim(v.get("note")).length() - 1));

                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} .* [\\w]{3} [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+([\\s])?([\\-|\\+])$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 23.01.21 25.01.21 EVERDRIVE.ME, KRAKOW USD 181,00 1,2192 148,46 -
                        // 2% für Währungsumrechnung 2,97 -
                        //
                        // 10.09.19 05.11.19 WWW.ALIEXPRESS.COM, LONDON USD 31,49 1,10279 28,55+
                        // 1,75% für Einsatz der Karte im Ausland 0,50+
                        // @formatter:on
                        .section("date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) " //
                                        + ".* " //
                                        + "[\\w]{3} [\\.,\\d]+ [\\.,\\d]+ " //
                                        + "[\\.,\\d]+" //
                                        + "([\\s])?[\\-|\\+]$") //
                        .match("^(?<note>[\\.,\\d]+% .*) (?<amount>[\\.,\\d]+)([\\s])?(?<type>[\\-|\\+])$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from FEES_REFUND to FEES
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Wertpapier Abrechnung|Wertpapierabrechnung).*$");
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
                                        // iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile DE000A0H0785
                                        // STK 16,000 EUR 120,4000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Gattungsbezeichnung ISIN") //
                                                        .match("^(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^STK [\\.,\\d]+ (?<currency>[\\w]{3} [\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale Wertpapierbezeichnung ISIN (WKN)
                                        // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                                        // Ausführungskurs 71,253 EUR Auftragserteilung/ -ort Persönlich im Institut
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .find("Nominale Wertpapierbezeichnung ISIN \\(WKN\\)") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // STK 16,000 EUR 120,4000
                        // Stück 7,1535 BGF - WORLD TECHNOLOGY FUND LU0171310443 (A0BMAN)
                        // @formatter:on
                        .section("shares") //
                        .match("^(STK|St.ck) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten
                        // 03.06.2015 10/3874/009 87966195 EUR 11,48
                        // @formatter:on
                        .section("date", "amount", "currency").optional() //
                        .find("Wert Konto\\-Nr\\. Abrechnungs\\-Nr\\. Betrag zu Ihren Gunsten") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\/\\d]+ [\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Herrn        Depot-Nr. Abrechnungs-Nr. ADRESSZEILE4=PLZ Stadt
                                        // Vorname Name 100/0000/000 10000000 ADRESSZEILE5=
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "note1") //
                                                        .match("^.* (?<note>Abrechnungs\\-Nr\\.) .*$") //
                                                        .match("^.*[\\d]+\\/[\\d]+\\/[\\d]+ (?<note1>.*) .*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1")))),
                                        // @formatter:off
                                        // Depot-Nr. Abrechnungs-Nr.
                                        // 111/2222/002 65091167
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "note1") //
                                                        .match("^.* (?<note>Abrechnungs\\-Nr\\.)$") //
                                                        .match("^[\\d]+\\/[\\d]+\\/[\\d]+ (?<note1>.*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")) + " " + trim(v.get("note1")))),
                                        // @formatter:off
                                        // Abrechnungsnr. 12345678
                                        //  XXXX XXXAuftragsnummer XXXXXX/XX.XX
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>(Abrechnungsnr\\.|Auftragsnummer) .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

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

                        .section("n").optional() //
                        .match("^zu versteuern \\(negativ\\) (?<n>.*)$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("negative", true));

        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.
        transaction //

                        .section("n").optional() //
                        .match("^(?<n>Ertragsthesaurierung)$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true));

        transaction //

                        // @formatter:off
                        // einbehaltene Kapitalertragsteuer EUR 7,03
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^einbehaltene Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer 24,51 % auf 11,00 EUR 2,70- EUR
                        // Kapitalertragsteuer 25,00% auf 768,37 EUR 192,09- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer [\\.,\\d]+([\\s])?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer EUR 70,16
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltener Solidaritätszuschlag EUR 0,38
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^einbehaltener Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag EUR 3,86
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 2,70 EUR 0,14- EUR
                        // Solidaritätszuschlag 5,50% auf 192,09 EUR 10,56- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s])?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // einbehaltene Kirchensteuer EUR 1,27
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^einbehaltene Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer EUR 1,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer 8 % auf 2,70 EUR 0,21- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s])?% .* [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 31,32 USD 4,12- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // Anrechenbare Quellensteuer pro Stück 0,01879125 USD 4,70 USD
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // davon anrechenbare Quellensteuer Fondseingangsseite EUR 0,04
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^davon anrechenbare Quellensteuer Fondseingangsseite (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        })

                        // @formatter:off
                        // davon anrechenbare US-Quellensteuer 15% USD 13,13
                        // @formatter:on
                        .section("currency", "creditableWithHoldingTax").optional() //
                        .match("^davon anrechenbare US\\-Quellensteuer [\\.,\\d]+([\\s])?% (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("negative")
                                            && !type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Handelszeit 09:02 Orderentgelt                EUR 10,90-
                        // Handelszeit 09:04 Orderentgelt EUR 9,97 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* Orderentgelt[\\s]{1,}(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)([\\s]+)?\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Orderentgelt
                        // EUR 0,71-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Orderentgelt$") //
                        .match("^(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)([\\s]+)?\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börse Stuttgart Börsengebühr EUR 2,29-
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* B.rsengeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)([\\s]+)?\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 1,48- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)([\\s]+)?\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Transaktionsentgelt Börse 0,60- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)([\\s]+)?\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,12- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)([\\s]+)?\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 2,5015 % vom Kurswert 1,25- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision [\\.,\\d]+([\\s])?% vom Kurswert (?<fee>[\\.,\\d]+)([\\s]+)?\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // + Provision 0,49 Summe 200,49
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^\\+ Anlagerabatt (?<fee>[\\.,\\d]+) Summe [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            v.put("currency", context.get("currency"));

                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kurswert 509,71- EUR
                        // Kundenbonifikation 40 % vom Ausgabeaufschlag 9,71 EUR
                        // Ausgabeaufschlag pro Anteil 5,00 %
                        //
                        // Kurswert 155,93- EUR
                        // Kundenbonifikation 3,8 % vom Kurswert 5,93 EUR
                        // Ausgabeaufschlag pro Anteil 5,00 %
                        // @formatter:on
                        .section("amount", "currency", "discount", "discountCurrency", "percentageFee").optional() //
                        .match("^Kurswert (?<amount>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .match("^Kundenbonifikation [\\.,\\d]+([\\s])?% vom (Ausgabeaufschlag|Kurswert) (?<discount>[\\.,\\d]+) (?<discountCurrency>[\\w]{3})$") //
                        .match("^Ausgabeaufschlag pro Anteil (?<percentageFee>[\\.,\\d]+) %$") //
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                            BigDecimal amount = asBigDecimal(v.get("amount"));
                            Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                BigDecimal fxFee = amount.divide(percentageFee //
                                                .divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE), Values.MC)
                                                .multiply(percentageFee, Values.MC);

                                Money fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

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
                        .section("currency", "fee").optional() //
                        .match("^Kurswert$") //
                        .match("^(?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)([\\s]+)?\\-$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}

