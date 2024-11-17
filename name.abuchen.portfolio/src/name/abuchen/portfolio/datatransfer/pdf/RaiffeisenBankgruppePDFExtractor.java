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
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RaiffeisenBankgruppePDFExtractor extends AbstractPDFExtractor
{
    public RaiffeisenBankgruppePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raiffeisenbank");
        addBankIdentifier("RB Augsburger Land West eG");
        addBankIdentifier("Raiffeisenlandesbank");
        addBankIdentifier("Freisinger Bank eG");
        addBankIdentifier("VR Bank");
        addBankIdentifier("Postfach 3069 · 55020 Mainz");

        addBuySellTransaction();
        addDividendeTransaction();
        addFeeTransaction();
        addDeliveryInOutBoundTransaction();
        addNonImportableTransaction();
        addAccountStatementTransactions();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Raiffeisenbank Bankgruppe";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Gesch.ftsart:|Wertpapier Abrechnung) (Kauf|Verkauf|R.cknahme Fonds).*" //
                        + "|B.rse \\- (Zeichnung|Kauf))");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Auftrags\\-Nr|Auftragsnummer|Referenz).*$", "^(Der Betrag wird Ihnen|Den Gegenwert buchen|Dieser Beleg (tr.gt|wurde|wird)).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Rücknahme Fonds" change from BUY to SELL
                        .section("type").optional()
                        .match("^(Gesch.ftsart:|Wertpapier Abrechnung) (?<type>(Kauf|Verkauf|R.cknahme Fonds)) .*$")
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Rücknahme Fonds".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: DE000BAY0017 Bayer AG
                                        // Namens-Aktien o.N.
                                        // Kurs: 53,47 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Kurs: [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Kurs:")  //
                                                                            || !v.get("name1").startsWith("Fondsgesellschaft:")) //
                                                                v.put("name", trim(v.get("name")) + " "+ trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                                        // REGISTERED SHARES DL -,0001
                                        // Ausführungskurs 143,68 EUR Auftragserteilung/ -ort Online-Banking
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 2.549 Raiffeisen Futura - Pension Invest Balanced -V-
                                        // Valoren-Nr.: 10229545, ISIN-Nr.: CH0102295455
                                        // Kurs CHF 155.66
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben für Sie .*") //
                                                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                                                        .match("^Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN\\-Nr\\.: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Kurs (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zugang: 2 Stk
                                        // Abgang: 4.500 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // 2.549 Raiffeisen Futura - Pension Invest Balanced -V-
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Wir haben für Sie .*") //
                                                        .match("^(?<shares>[\\.'\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "de", "CH"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelszeit: 03.05.2021 13:45:18
                                        // Schlusstag/-Zeit 09.11.2021 09:58:45 Auftraggeber Muster
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(Handelszeit:|Schlusstag\\/\\-Zeit) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handelszeit: 27.03.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelszeit: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Wir haben für Sie am 21.03.2022 gezeichnet:
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Wir haben für Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Lasten IBAN AT99 9999 9000 0011 1110 -107,26 EUR
                                        // Zu Gunsten IBAN AT27 3284 2000 0011 1111 36.115,76 EUR
                                        // Ausmachender Betrag 14.399,34- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(Zu (Lasten|Gunsten) .*|Ausmachender Betrag) (\\-)?(?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Belastung Valuta 23.03.2022 CHF 399.95
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Belastung Valuta .* (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        // @formatter:off
                        // Kurswert: -1.464,32 CAD
                        // Devisenkurs: 1,406 (20.01.2022) -1.093,40 EUR
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional() //
                        .match("^Kurswert: (\\-)?(?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3}).*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Geschäftsart: Rücknahme Fonds Auftrags-Nr.: 47199493 - 27.03.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Auftrags\\-Nr\\.: [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Auftragsnummer 464088/93.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Auftragsnummer .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Unsere Referenz: 17815533192 Gebenstorf, 14. November 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Referenz: (?<note>[\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note")))))
                        // @formatter:off
                        // Limit bestens
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ertrag|Dividendengutschrift|Kapitaltransaktion|Ertragsgutschrift nach .*)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Private Banking|Abrechnungsnr|Gesch.ftsart|Anlageverm.gen).*$", "^(Den Betrag buchen|Der Betrag wird|Dieser Beleg (tr.gt|wurde|wird)).*$");
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
                                        // Titel: AT0000A1TW21 RAIFF.-EMERGINGMARKETS-AKTIEN RZ(A)
                                        // MITEIGENTUMSANTEILE - Ausschüttung
                                        // Ertrag: 1,19 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*) \\- Aussch.ttung$") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Dividende:") //
                                                                            || !v.get("name1").startsWith("Ertrag:") //
                                                                            || !v.get("name1").startsWith("Fondsgesellschaft:")) //
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Titel: DE000BAY0017 Bayer AG
                                        // Namens-Aktien o.N.
                                        // Dividende: 2 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "name1", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Dividende:") //
                                                                            || !v.get("name1").startsWith("Ertrag:") //
                                                                            || !v.get("name1").startsWith("Fondsgesellschaft:")) //
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stück 100 QUALCOMM INC. US7475251036 (883121)
                                        // REGISTERED SHARES DL -,0001
                                        // Zahlbarkeitstag 16.12.2021 Dividende pro Stück 0,68 USD
                                        //
                                        // Stück 111 DEUTSCHE TELEKOM AG DE0005557508 (555750)
                                        // NAMENS-AKTIEN O.N.
                                        // Zahlbarkeitstag 12.04.2023 Ertrag  pro Stück 0,70 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Dividende|Ertrag) .* [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Depotposition
                                        // 254 UBS ETF (CH) - SPI (R) Mid
                                        // Valoren-Nr.: 13059512, ISIN CH0130595124 (SPMCHA)
                                        // Dividende CHF 2.38 pro Titel
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Depotposition.*") //
                                                        .match("^[\\.'\\d]+ (?<name>.*)$") //
                                                        .match("^Valoren\\-Nr\\.: (?<wkn>[A-Z0-9]{5,9}), ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^(Dividende|Ertrag) (?<currency>[\\w]{3}) [\\.'\\d]+.*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 90 Stk
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) Stk.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Stück 100 QUALCOMM INC. US7475251036 (883121)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Depotposition
                                        // 254 UBS ETF (CH) - SPI (R) Mid
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Depotposition.*") //
                                                        .match("^(?<shares>[\\.'\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                    // Valuta 30.04.2021
                                    // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Den Betrag buchen wir mit Wertstellung 20.12.2021 zu Gunsten des Kontos 123456789 (IBAN DE11 1111 1111 1111 123456), BLZ 720 692 74 (BIC GENODEF1ZUS).
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Gutschrift Valuta 11. September 2024 CHF 392.94
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Gutschrift Valuta (?<date>[\\d]{2}\\. .* [\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT99 9999 9000 0011 1111 110,02 EUR
                                        // Ausmachender Betrag 50,88+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(Zu Gunsten .*|Ausmachender Betrag) (?<amount>[\\.,\\d]+)(\\+)? (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Zu Lasten IBAN AT44 4649 0000 0110 3051 -156,32 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // If the amount is negative, then it is taxes.
                                                            // @formatter:on
                                                            t.setType(AccountTransaction.Type.TAXES);

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Gutschrift Valuta 11. September 2024 CHF 392.94
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gutschrift Valuta .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Devisenkurs EUR / USD  1,1360
                                        // Dividendengutschrift 68,00 USD 59,86+ EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^Dividendengutschrift (?<fxGross>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)\\+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag: 119,37 USD
                                        // Devisenkurs: 1,0856 (13.01.2023) 109,37 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "exchangeRate", "baseCurrency") //
                                                        .match("^Bruttoertrag: (?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3}).*$") //
                                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) [\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        //  Abrechnungsnr. 76560429680
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Abrechnungsnr. .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Unsere Referenz: 17518731738 Gebenstorf, 11. September 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Referenz: (?<note>[\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note")))))
                        // @formatter:off
                        // Ex-Tag 01.12.2021 Art der Dividende Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Art der Dividende (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap((t, ctx) -> {
                            // @formatter:off
                            // If we have multiple entries in the document, then
                            // the "noTax" flag must be removed.
                            // @formatter:on
                            type.getCurrentContext().remove("noTax");

                            TransactionItem item = new TransactionItem(t);

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Geb.hrenbelastung Depot");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Anlageverm.gen.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Belastung Valuta 30.09.2024 CHF 100.04
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .match("^Belastung Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+).*$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        //  Abrechnungsnr. 76560429680
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Abrechnungsnr. .*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Unsere Referenz: 17495598455 Gebenstorf, 17. September 2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Referenz: (?<note>[\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note")))))

                        // @formatter:off
                        // Für den Zeitraum vom 01.07.2024 bis 30.09.2024 haben wir Ihnen folgende Gebühr belastet:
                        // @formatter:on
                        .section("note").optional() //
                        .match("^F.r den Zeitraum vom (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: (Einbuchung|Ausbuchung)");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnungsnummer\\/\\-datum:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // Is type --> "Einbuchung" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                        .section("type").optional() //
                        .match("^Gesch.ftsart: (?<type>(Einbuchung|Ausbuchung))$") //
                        .assign((t, v) -> {
                            if ("Ausbuchung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        // @formatter:off
                        // Titel: LU0392496344 Lyxor MSCI Europe SmallCap ETF
                        // Inh.-An. I o.N.
                        // @formatter:on
                        .section("isin", "name", "nameContinued") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Abgang: 433 Stk
                        // Zugang: 433 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(Abgang|Zugang): (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 10.03.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Gunsten IBAN AT44 3400 0000 0123 4567 0,00 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abrechnungsnummer/-datum: 45664992 - 16.03.2023
                        // @formatter:on
                        .section("note1", "note2").optional()
                        .match("^(?<note1>Abrechnungsnummer)\\/\\-datum: (?<note2>[\\d]+) \\- .*$")
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        // @formatter:off
                        // im Verhältnis: 1 : 1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Verh.ltnis: .*)$") //
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                            t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("Gesch.ftsart: Freier Erhalt"); //
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gesch.ftsart: Freier Erhalt$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Titel: IE00BKX55T58 Vang.FTSE Develop.World U.ETF
                        // Registered Shares USD Dis.oN
                        // @formatter:on
                        .section("isin", "name", "nameContinued") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Menge: 550 Stk
                        // Zugang: 200 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(Menge|Zugang): (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag: 18.03.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT44 3400 0000 0123 4567 0,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Verrechnung: IBAN Do07 0286 0000 5451 0810 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency") //
                                                        .match("^Verrechnung: IBAN .* (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(0L);
                                                        }))

                        // @formatter:off
                        // Abrechnungsnummer/-datum: 45664992 - 16.03.2023
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Abrechnungsnummer)\\/\\-datum: (?<note2>[\\d]+) \\- .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                        // @formatter:off
                        // Die Änderung/Stornierung dieses Auftrages erfolgt mit Vorbehalt.
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Die (?<note>.nderung\\/Stornierung) dieses Auftrages .*$") //
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);
                            t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addAccountStatementTransactions()
    {
        final DocumentType type = new DocumentType("(Kontokorrent|Privatkonto|Tagesgeld Plus)", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // EUR-Konto Kontonummer 12364567
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[\\w]{3})\\-Konto Kontonummer .*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // 92586 Hof Kontoauszug Nr.  12/2020
                                        // @formatter:on
                                        .section("nr", "year") //
                                        .match("^.* Kontoauszug Nr\\. ([\\s]+)?(?<nr>[\\d]+)\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("nr", v.get("nr"));
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 01.12. 01.12. BASISLASTSCHRIFT PN:931                                             42,13 S
                        // 01.12. 01.12. DAUERAUFTRAG PN:900                                                  50,00 S
                        // 02.12. 03.12. EURO-UEBERWEISUNG PN:801                                            500,00 S
                        // 29.12. 29.12. Einnahmen PN:931                                                          1.097,00 H
                        // 02.08. 02.08. Kartenzahlung girocard PN:931                                       10,00 S
                        // 30.08. 30.08. LOHN/GEHALT PN:931                                                          1.200,00 H
                        // 27.08. 27.08. Auszahlung girocard PN:931                                           20,00 S
                        // 08.06. 08.06. Überweisung SEPA                                                      4,00 S
                        // @formatter:on
                        .section("day", "month", "note", "amount", "sign").optional() //
                        .documentContext("currency", "nr", "year") //
                        .match("^(?i)[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. " //
                                        + "(?<note>Einnahmen" //
                                        + "|BASISLASTSCHRIFT" //
                                        + "|DAUERAUFTRAG" //
                                        + "|EURO\\-UEBERWEISUNG" //
                                        + "|GUTSCHRIFT" //
                                        + "|Kartenzahlung" //
                                        + "|Auszahlung" //
                                        + "|LOHN\\/GEHALT" //
                                        + "|.berweisung SEPA" //
                                        + "|UEBERWEISUNG" //
                                        + "|RETOUREN" //
                                        + "|UEBERTRAG) " //
                                        + ".* " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<sign>[S|H])$") //
                        .match("^(?![\\s]+ [Dividende]).*$") //
                        .match("^(?![\\s]+ [Dividende]).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is sign --> "H" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("H".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.DEPOSIT);

                            dateTranactionHelper(t, v);

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));

                            // Formatting some notes
                            if ("LOHN/GEHALT".equals(v.get("note")))
                                v.put("note", "Lohn/Gehalt");

                            if ("EURO-UEBERWEISUNG".equals(v.get("note")))
                                v.put("note", "EURO-Überweisung");

                            if ("BASISLASTSCHRIFT".equals(v.get("note")))
                                v.put("note", "Basislastschrift");

                            if ("DAUERAUFTRAG".equals(v.get("note")))
                                v.put("note", "Dauerauftrag");

                            if ("GUTSCHRIFT".equals(v.get("note")))
                                v.put("note", "Gutschrift");

                            if ("UEBERWEISUNG".equals(v.get("note")))
                                v.put("note", "Überweisung");

                            if ("RETOUREN".equals(v.get("note")))
                                v.put("note", "Retouren");

                            if ("UEBERTRAG".equals(v.get("note")))
                                v.put("note", "Übertrag");

                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 23.04. 22.04. Mol*Heinrich Dittmar G PN:4444 63,39 S
                        //  DEU 495522500131 EUR 63,39
                        //  Umsatz vom 21.04.2024 MC Hauptkarte
                        // 25.04. 24.04. Ihr MastercardCashback PN:4444 5,00 H
                        //  DEU Cashback EUR 5,00
                        //  Umsatz vom 23.04.2024 MC Hauptkarte
                        // 25.04. 24.04. Mol*Heinrich Dittmar G PN:4444 5,52 H
                        //  DEU 495522500131 EUR 5,52
                        //  Umsatz vom 23.04.2024 MC Hauptkarte
                        // @formatter:on
                        .section("day", "month", "note", "note2", "amount", "sign").optional() //
                        .documentContext("currency", "nr", "year") //
                        .match("^(?i)[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. (?<note>.*) PN:4444 (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                        .match("^.*$") //
                        .match("^ (?<note2>.*)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is sign --> "H" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("H".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.DEPOSIT);

                            dateTranactionHelper(t, v);

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));

                            if (!v.get("note").contains("Cashback"))
                                t.setNote(v.get("note2"));
                            else
                                t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .optionalOneOf(
                                        // @formatter:off
                                        // 30.12. 31.12. Abschluss PN:905                                                      1,95 S
                                        //          9,60000% einger. Kontoüberziehung    3112       1,00S
                                        //          14,60000% einger. Kontoüberziehung    3112       1,00S
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "amount1", "amount2", "note") //
                                                        .documentContext("currency", "nr", "year") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ S$") //
                                                        .match("^.*[\\.,\\d]+% einger\\. Konto.berziehung .* (?<amount1>[\\.,\\d]+)S$") //
                                                        .match("^.*[\\.,\\d]+% einger\\. Konto.berziehung .* (?<amount2>[\\.,\\d]+)S$") //
                                                        .match("^.*(?<note>Entgelte vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> {
                                                            t.setType(Type.INTEREST_CHARGE);

                                                            dateTranactionHelper(t, v);

                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount1")) + asAmount(v.get("amount2")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 29.12. 31.12. Abschluss lt. Anlage 1 PN:905 534,59 H
                                        // 30.04. 30.04. Abschluss lt. Anlage 1 PN:905 0,11 S
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("day", "month", "amount", "sign") //
                                                        .documentContext("currency", "nr", "year") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). Abschluss lt\\. Anlage [\\d] .* (?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                                                        .assign((t, v) -> {
                                                            if ("S".equals(v.get("sign")))
                                                                t.setType(Type.INTEREST_CHARGE);

                                                            dateTranactionHelper(t, v);

                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))
                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 30.12. 31.12. Abschluss PN:905                                                      1,95 S
                        // 9,60000% einger. Kontoüberziehung    3112       1,00S
                        // 14,60000% einger. Kontoüberziehung    3112       1,00S
                        //Entgelte vom 01.12.2020 - 31.12.2020
                        //          Buchungen Online   St.    4 3112       0,00H
                        //          Buchungen automatisch    12 3112       0,00H
                        //          Kontoführungsentgelt        3112       1,95S
                        // Abschluss vom 01.10.2020 bis 31.12.2020
                        // @formatter:on
                        .section("day", "month", "sign", "amount1", "amount2", "amount3", "note").optional() //
                        .documentContext("currency", "nr", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ (?<sign>[S|H])$") //
                        .match("^[\\s]+ Buchungen Online .* (?<amount1>[\\.,\\d]+)[S|H]$") //
                        .match("^[\\s]+ Buchungen automatisch .* (?<amount2>[\\.,\\d]+)[S|H]$") //
                        .match("^[\\s]+ Kontof.hrungsentgelt .* (?<amount3>[\\.,\\d]+)[S|H]$") //
                        .match("^[\\s]+ (?<note>Abschluss vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "H" change from FEES to FEES_REFUND
                            // @formatter:on
                            if ("H".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            dateTranactionHelper(t, v);

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount1")) + asAmount(v.get("amount2")) + asAmount(v.get("amount3")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // 31.08. 31.08. Abschluss PN:905                                                      1,95 S
                        //          Buchungen automatisch    23 2345       0,00H
                        //          Kontoführungsentgelt        2345       1,95S
                        // Abschluss vom 30.07.2021 bis 31.08.2021
                        // @formatter:on
                        .section("day", "month", "sign", "amount1", "amount2", "note").optional() //
                        .documentContext("currency", "nr", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ (?<sign>[S|H])$") //
                        .match("^[\\s]+ Buchungen automatisch .* (?<amount1>[\\.,\\d]+)[S|H]$") //
                        .match("^[\\s]+ Kontof.hrungsentgelt .* (?<amount2>[\\.,\\d]+)[S|H]$") //
                        .match("^[\\s]+ (?<note>Abschluss vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "H" change from FEES to FEES_REFUND
                            // @formatter:on
                            if ("H".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            dateTranactionHelper(t, v);

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount1")) + asAmount(v.get("amount2")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. (Kapitalertragsteuer|Solid\\.-Zuschlag|Kirchensteuer) .* ([\\.,\\d]+) [S|H]");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 29.12. 31.12. Solid.-Zuschlag aus PN:905 0,46 S
                        // 29.12. 31.12. Kirchensteuer aus PN:905 0,76 S
                        // 29.12. 31.12. Kapitalertragsteuer aus PN:905 8,46 S
                        // Abschluss vom 30.07.2021 bis 31.08.2021
                        // @formatter:on
                        .section("day", "month", "sign", "amount").optional() //
                        .documentContext("currency", "nr", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. "
                                        + "(Kapitalertragsteuer"
                                        + "|Solid\\.-Zuschlag"
                                        + "|Kirchensteuer) .* "
                                        + "(?<amount>[\\.,\\d]+) (?<sign>[S|H])$") //
                        .assign((t, v) -> {
                        // @formatter:off
                            // Is type --> "H" change from TAXES to TAX_REFUND
                            // @formatter:on
                            if ("H".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            dateTranactionHelper(t, v);

                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Auszug .*[\\d]+\\/[\\d]+ vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Datum Buchungstext                                   Wert       Betrag EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Datum Buchungstext .* (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Auszug  13/001 vom 31.05.2023
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Auszug .*[\\d]+\\/[\\d]+ vom [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 08.05 Gutschrift                                     0805         5.000,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2} Gutschrift .* [\\d]{4} .* [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) (?<note>Gutschrift) .* [\\d]{4} .* (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.

        transaction //

                        .section("n").optional() //
                        .match("^Zu (?<n>Lasten) .* \\-[\\.,\\d]+ [\\w]{3}.*$") //
                        .match("^Belastung KESt bei aussch.ttungsgleichen Erträgen .*$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true));

        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 24,51 % auf 15,08 EUR 3,69- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag 5,5 % auf 3,69 EUR 0,20- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer 8 % auf 3,69 EUR 0,30- EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Quellensteuer: -47,48 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Quellensteuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Auslands-KESt: -22,50 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Auslands\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // KESt ausländische Dividende: -0,64 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt ausl.ndische Dividende: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // KESt: -10,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Umsatzsteuer: -0,29 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Umsatzsteuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kursgewinn-KESt: -696,65 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kursgewinn\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Verrechnungssteuer 35 % CHF -211.58
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Verrechnungssteuer [\\.,\\d]+([\\s]+)?% .*(?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Umsatzabgabe CHF -3.75
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Umsatzabgabe (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+).*$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Einbehaltene Quellensteuer 15 % auf 68,00 USD 8,98- EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltene Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // Anrechenbare Quellensteuer 15 % auf 59,86 EUR 8,98 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Serviceentgelt: -0,32 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Serviceentgelt: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 0,2000 % vom Kurswert 28,74- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision 1,50- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen 2,50- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Übertragungs-/Liefergebühr 0,10- EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Handelsortentgelt inkl. Fremdspesen: -4,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Handelsortentgelt inkl\\. Fremdspesen: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Gebühren: -25,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Geb.hren: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Orderleitgebühr: -3,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Orderleitgeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Inkassogebühr: -1,45 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Inkassogeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Zahlungsverkehr-Transaktionsgebühr: -3,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Zahlungsverkehr\\-Transaktionsgeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühr CHF -2.35
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hr (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage CHF -3.17
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            int lastDot = value.lastIndexOf(".");
            int lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de";
        String country = "DE";

        int apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            int lastDot = value.lastIndexOf(".");
            int lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }

    /**
     * Helper method to set the date of an AccountTransaction based on the provided ParsedData.
     *
     * This method checks if the transaction's "nr" field is "01" and if the month is less than 3 (January or February).
     * If both conditions are met, it assumes the transaction should be recorded in the next year.
     * Otherwise, it uses the year provided in the ParsedData.
     *
     * @param t The AccountTransaction object to set the date for.
     * @param v The ParsedData object containing the date information.
     */
    private void dateTranactionHelper(AccountTransaction t, ParsedData v)
    {
        final String SPECIAL_NR = "01";
        final int THRESHOLD_MONTH = 3;

        String nr = v.get("nr");
        int month = Integer.parseInt(v.get("month"));
        int year = Integer.parseInt(v.get("year"));

        if (SPECIAL_NR.equals(nr) && month < THRESHOLD_MONTH)
        {
            year++;
        }

        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + year));
    }
}
