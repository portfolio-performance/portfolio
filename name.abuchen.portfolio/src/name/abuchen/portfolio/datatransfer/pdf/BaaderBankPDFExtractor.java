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
public class BaaderBankPDFExtractor extends AbstractPDFExtractor
{
    public BaaderBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Baader Bank Aktiengesellschaft");
        addBankIdentifier("Baader Bank AG");
        addBankIdentifier("Verrechnungskonto bei der Baader Bank");

        addBuySellTransaction();
        addBuySellCryptoTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addTaxAdjustmentTransaction();
        addDepotStatementTransaction();
        addFeesAssetManagerTransaction();
        addInterestTransaction();
        addDeliveryInOutBoundTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Baader Bank AG / Scalable Capital Vermögensverwaltung GmbH / Traders Place GmbH & Co. KGaA";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("((Wertpapierabrechnung|Transaction Statement): " //
                        + "(Kauf" //
                        + "|Verkauf" //
                        + "|Purchase" //
                        + "|Sale)" //
                        + "|Zeichnung" //
                        + "|Spitzenregulierung" //
                        + "|Gesamtr.ckzahlung" //
                        + "|Ablauf der Optionsfrist" //
                        + "|Obligatorische Barabfindung)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Vorgangs\\-Nr|Transaction No)\\.: .*$");
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
                        .match("^(Wertpapierabrechnung|Transaction Statement): " //
                                        + "(?<type>(Kauf" //
                                        + "|Verkauf" //
                                        + "|Purchase" //
                                        + "|Sale)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Sale".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Is type --> "Spitzenregulierung" change from BUY to SELL
                        // Is type --> "Gesamtrückzahlung" change from BUY to SELL
                        // Is type --> "Ablauf der Optionsfrist" change from BUY to SELL
                        // Is type --> "Obligatorische Barabfindung" change from BUY to SELL
                        // @formatter:off
                        .section("type").optional() //
                        .match("^(?<type>(Spitzenregulierung" //
                                        + "|Gesamtr.ckzahlung" //
                                        + "|Ablauf der Optionsfrist" //
                                        + "|Obligatorische Barabfindung)).*$") //
                        .assign((t, v) -> {
                            if ("Spitzenregulierung".equals(v.get("type")) //
                                            || "Gesamtrückzahlung".equals(v.get("type")) //
                                            || "Ablauf der Optionsfrist".equals(v.get("type")) //
                                            || "Obligatorische Barabfindung".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale ISIN: IE0032895942 WKN: 911950 Kurs
                                        // STK 2 iShs DL Corp Bond UCITS ETF EUR 104,37
                                        // Registered Shares o.N.
                                        // Kurswert EUR 208,74
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "nameContinued", "currency") //
                                                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}) (Kurs|Bezugspreis|Barabfindung|Price|R.ckzahlung).*$") //
                                                        .match("^(STK|Units) [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) .*$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            if (v.get("nameContinued").endsWith("p.STK"))
                                                                v.put("nameContinued", v.get("nameContinued").replace("p.STK", ""));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nominale ISIN: DE000VU9QLB0 WKN: VU9QLB Kurs
                                        // EUR   1.000,00 15,00 % 97,28 %
                                        // Vontobel Financial Products
                                        // Aktienanleihe v.23(24)ZAL
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "currency", "name", "nameContinued") //
                                                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}) (Kurs|Bezugspreis|Barabfindung|Price|R.ckzahlung).*$") //
                                                        .match("^(?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+ [\\.,\\d]+ % [\\.,\\d]+ %$") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Nominale ISIN: DE0001104875 WKN: 110487 Rückzahlung
                                        // EUR   208.000,00 Bundesrep.Deutschland 100,00 %
                                        // Bundesschatzanw. v.22(24)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "currency", "name", "nameContinued") //
                                                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}) (Kurs|Bezugspreis|Barabfindung|Price|R.ckzahlung).*$") //
                                                        .match("^(?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+ (?<name>.*) [\\.,\\d]+ %$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 2 iShs DL Corp Bond UCITS ETF EUR 104,37
                                        // Units 2.734 iShsIII-Core MSCI World U.ETF EUR 73.128
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("local", "shares") //
                                                        .match("^(?<local>(STK|Units)) (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> {
                                                            if ("Units".equals(v.get("local")))
                                                                t.setShares(asShares(v.get("shares"), "en", "US"));
                                                            else
                                                                t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // EUR   1.000,00 15,00 % 97,28 %
                                        // EUR   208.000,00 Bundesrep.Deutschland 100,00 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3}[\\s]{1,}(?<shares>[\\.,\\d]+) .* [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            // Percentage quotation, workaround for bonds
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelsdatum Handelsuhrzeit
                                        // 20.03.2017 15:31:10:00
                                        //
                                        // Handelsdatum Handelsuhrzeit
                                        // 11.04.2023 12:31:19:07
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find(".*Handelsdatum Handelsuhrzeit.*") //
                                                        .match("^.*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}):.*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handels- Handels-
                                        // STK   70 EUR 14,045 GETTEX - MM Munich 24.02.2021 14:49:46:04
                                        //
                                        // Handels- Handels-
                                        // STK   50 EUR 30,79 GETTEX - MM Munich 12.04.2023 09:00:06:185
                                        //
                                        // Details zur Ausführung: Handels- Handels-
                                        // STK   6 EUR 146,34 GETTEX - MM Munich 27.01.2022 16:25:24:39
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find(".*Handels\\- Handels\\-.*") //
                                                        .match("^.*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}):.*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                                        // Ausbuchung aus Depot 11 per 05.05.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Einbuchung in|Ausbuchung aus) Depot .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Quantity Price Execution Venue Trade Date Trade Time
                                        // Units   11 EUR 8.55 GETTEX - MM Munich 2022-04-29 09:32:39:47
                                        //
                                        // Trade Date Trade Time
                                        // 2022-02-28 13:48:52:44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find(".*Trade Date Trade Time.*") //
                                                        .match("^.*(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}):.*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Zu Gunsten Konto 7777777704 Valuta: 22.12.2023 EUR 2.088,13
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu (Gunsten|Lasten) Konto .* Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Lasten Konto 12345004 Valuta: 22.03.2017 EUR 208,95
                                        // Zu Gunsten Konto 12345004 Valuta: 12.05.2017 EUR 75,92
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu (Gunsten|Lasten) Konto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Amount debited to account 1960017000 Value: 2022-03-02 EUR 199.93
                                        // Amount credited to account 1209625007 Value: 2022-07-13 EUR 329.36
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Amount (debited|credited) to account [\\d]+ Value: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Kurswert Umrechnungskurs CAD/EUR: 1,4595 EUR 8,85
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^Kurswert Umrechnungskurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}): (?<exchangeRate>[\\.,\\d]+) [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Umrechnungskurs: EUR/USD 1,09084
                                        // Bruttobetrag USD 232,00
                                        // Bruttobetrag EUR 212,68
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^(Umrechnungskurs|Exchange Rate): (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^(Bruttobetrag|Gross Amount) [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                                                        .match("^(Bruttobetrag|Gross Amount) [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Vorgangs-Nr.: 184714818
                        // Transaction No.: 204751222
                        // dfXMYlRrT Vorgangs-Nr.: 002052907
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Verhältnis: 1 : 1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Verh.ltnis: .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        // @formatter:off
                        // Spitzenregulierung KOPIE
                        // Ablauf der Optionsfrist
                        // Obligatorische Barabfindung
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Spitzenregulierung|Ablauf der Optionsfrist|Obligatorische Barabfindung)).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        // @formatter:off
                        // Stückzinsen für 54 Tag(e) nächster Zinstermin: 27.09.2023 EUR 9,36
                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^(?<note1>St.ckzinsen .* [\\d]+ Tag\\(e\\)).* (?<note3>[\\w]{3}) (?<note2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), trim(v.get("note1")), " | "));
                            t.setNote(concatenate(t.getNote(), trim(v.get("note2")), ": "));
                            t.setNote(concatenate(t.getNote(), trim(v.get("note3")), " "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellCryptoTransaction()
    {
        final DocumentType type = new DocumentType("Abrechnung über den Kauf von Kryptowerten");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Vorgangs\\-Nr|Transaction No)\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Nominale Kennung: BTC Kurs
                        // STK 0,0024 Bitcoin EUR 41.981,240
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency") //
                        .match("^Nominale Kennung: (?<tickerSymbol>[A-Z]+) Kurs$") //
                        .match("^STK [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateCryptoCurrency(v)))

                        // @formatter:off
                        // STK 0,0024 Bitcoin EUR 41.981,240
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+) .* [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // STK 0,0024 EUR 41.981,240 B4C Markets BV 02.01.2024 11:04:16:95
                        // @formatter:on
                        .section("date", "time") //
                        .match("^STK [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Zu Lasten Konto 1234567890 Valuta: 04.01.2024 EUR 101,75
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Lasten Konto .* Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Vorgangs-Nr.: KRY 000000123456
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Fondsaussch.ttung" //
                        + "|Ertragsthesaurierung" //
                        + "|Dividendenabrechnung"
                        + "|Aussch.ttung" //
                        + "|Aussch.ttung aus" //
                        + "|Wahldividende" //
                        + "|Fund Distribution" //
                        + "|Dividend Statement" //
                        + "|Dividende" //
                        + "|Reklassifizierung" //
                        + "|Zinsabrechnung)", //
                        "(Kontoauszug" //
                        + "|Account Statement" //
                        + "|Transaction Statement"
                        + "|Steuerausgleichsrechnung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Seite|Page) 1\\/[\\d]$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // If we have a positive amount and a gross reinvestment,
                        // there is a tax refund.
                        // If the amount is negative, then it is taxes.
                        // @formatter:on
                        .section("type", "sign").optional() //
                        .match("^Nominale ISIN: .* (?<type>(Aussch.ttung|Thesaurierung brutto))$") //
                        .match("^Zu (?<sign>(Gunsten|Lasten)) Konto [\\d]+ Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if ("Thesaurierung brutto".equals(v.get("type")) && "Gunsten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            if ("Thesaurierung brutto".equals(v.get("type")) && "Lasten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        // @formatter:off
                        // Dividendenabrechnung STORNO
                        // Fondsausschüttung STORNO
                        // Dividende § 27 STORNO
                        //
                        // Fondsausschüttung KOPIE
                        // STORNO
                        //
                        // Reklassifizierung
                        // @formatter:on
                        .section("type").optional() //
                        .match("^((Dividendenabrechnung" //
                                        + "|Fondsaussch.ttung" //
                                        + "|Dividende . 27) )?"
                                        + "(?<type>(STORNO|Reklassifizierung))$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // Nominale ISIN: FR0000130577 WKN: 859386 Ausschüttung
                                        // STK 57 Publicis Groupe S.A. EUR 2,00 p.STK
                                        // Zahlungszeitraum: 17.06.2021 - 30.06.2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "name1", "currency") //
                                                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(STK|Units) [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) .*$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Zahlungszeitraum") && !v.get("name1").startsWith("Payment")
                                                                            && !v.get("name1").startsWith("Bruttobetrag"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Nominale ISIN: US698299AW45 WKN: A0GMJZ Zinsertrag
                                        // USD   1.000,00 6,70 % 3,35 %
                                        // Panama, Republik
                                        // DL-Bonds 2006(34-36)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "currency", "name", "nameContinued") //
                                                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^(?<currency>[\\w]{3})[\\s]{1,}[\\.,\\d]+ [\\.,\\d]+ % [\\.,\\d]+ %$") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // STK 57 Publicis Groupe S.A. EUR 2,00 p.STK
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("local", "shares") //
                                                        .match("^(?<local>(STK|Units)) (?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> {
                                                            if ("Units".equals(v.get("local")))
                                                                t.setShares(asShares(v.get("shares"), "en", "US"));
                                                            else
                                                                t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // USD   1.000,00 6,70 % 3,35 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3}[\\s]{1,}(?<shares>[\\.,\\d]+) [\\.,\\d]+ % [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> {
                                                            // Percentage quotation, workaround for bonds
                                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten Konto 1111111111 Valuta: 06.07.2021 EUR 68,22
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zu Gunsten Konto [\\d]+ Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Amount credited to account 1209625007 Value: 2022-02-25 EUR 0.20
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Amount credited to account [\\d]+ Value: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten Konto 1111111111 Valuta: 06.07.2021 EUR 68,22
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu Gunsten Konto [\\d]+ Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Amount credited to account 1209625007 Value: 2022-02-25 EUR 0.20
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Amount credited to account [\\d]+ Value: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Umrechnungskurs: EUR/USD 1,1452
                                        // Bruttobetrag USD 3,94
                                        // Bruttobetrag EUR 3,44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross") //
                                                        .match("^(Umrechnungskurs|Exchange Rate): (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^(Bruttobetrag|Gross Amount) [\\w]{3} (?<fxGross>[\\.,\\d]+)$") //
                                                        .match("^(Bruttobetrag|Gross Amount) [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Umrechnungskurs: EUR/USD 1,0878000
                                        // Bruttobetrag USD 14,40
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^(Umrechnungskurs|Exchange Rate): (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .match("^(Bruttobetrag|Gross Amount) [\\w]{3} (?<gross>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Vorgangs-Nr.: 184714818
                        // Transaction No.: 204751222
                        // dfXMYlRrT Vorgangs-Nr.: 002052907
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            // If we have multiple entries in the document, then
                            // the "noTax" flag must be removed.
                            type.getCurrentContext().remove("noTax");

                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        final DocumentType type = new DocumentType("(Vorabpauschale|Advance Lump Sum)", //
                        "(Wertpapierabrechnung" //
                        + "|Steuerausgleichsrechnung" //
                        + "|Transaction Statement" //
                        + "|Kontoauszug" //
                        + "|Account Statement)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Vorgangs\\-Nr|Transaction No)\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Nominale ISIN: IE00BWBXM385 WKN: A14QBZ
                        // STK 112 SPDR S+P US Con.Sta.Sel.S.UETF
                        //
                        // Quantity ISIN: IE00BKM4GZ66 WKN: A111X9
                        // Units 418 iShs Core MSCI EM IMI U.ETF
                        // Registered Shares o.N.
                        // @formatter:on
                        .section("isin", "wkn", "name", "name1") //
                        .match("^(Nominale|Quantity) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6})$") //
                        .match("^(STK|Units) [\\.,\\d]+ (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Zahlungszeitraum"))
                                v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 112 SPDR S+P US Con.Sta.Sel.S.UETF
                        // Units 418 iShs Core MSCI EM IMI U.ETF
                        // @formatter:on
                        .section("shares") //
                        .match("^(STK|Units) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            if ("Units".equals(v.get("local")))
                                t.setShares(asShares(v.get("shares"), "en", "US"));
                            else
                                t.setShares(asShares(v.get("shares")));
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Zahltag: 04.01.2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zahltag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Payday: 2024-01-02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Payday: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Lasten Konto 1247201005 Valuta: 04.01.2021 EUR 0,04
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu Lasten Konto [\\d]+ Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Amount debited to account 4345818561 Value: 2024-01-02 EUR 3.12
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Amount debited to account [\\d]+ Value: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Bemessungsgrundlage zur Kapitalertragsteuer EUR 24,17 EUR 0,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Bemessungsgrundlage zur Kapitalertragsteuer [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Vorgangs-Nr.: yyyyyyy
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Zahlungszeitraum: 01.01.2020 - 31.12.2020
                        // Payment Period: 2023-01-01 - 2023-12-31
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Zahlungszeitraum|Payment Period): .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addTaxAdjustmentTransaction()
    {
        final DocumentType type = new DocumentType("Steuerausgleichsrechnung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Seite 1\\/[\\d]$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        // Is type --> "Nachbelastung" change from TAX_REFUND to TAXES
                        .section("type").optional() //
                        .match("^(?<type>(Erstattung|Nachbelastung)) [\\w]{3} [\\.,\\d]+( \\-)?$") //
                        .assign((t, v) -> {
                            if ("Nachbelastung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        // @formatter:off
                        // Unterschleißheim, 22.06.2017
                        // 26.06.2020
                        // @formatter:on
                        .section("date") //
                        .match("^.*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Erstattung EUR 9,01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Erstattung (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Nachbelastung EUR 59,38 -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Nachbelastung (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+) \\-$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Vorgangs-Nr.: 184714818
                        // Transaction No.: 204751222
                        // dfXMYlRrT Vorgangs-Nr.: 002052907
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Vorgangs-Nr.: 48132680 Fondsfusion
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .* Fondsfusion)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        // @formatter:off
                        // Nominale ISIN: LU1861134382
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Nominale ISIN: (?<note>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Perioden\\-Kontoauszug" //
                        + "|Tageskontoauszug" //
                        + "|Periodic Account Statement)", //
                        "Rechnungsabschluss:", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Perioden-Kontoauszug: EUR-Konto KOPIE
                                        // Tageskontoauszug: EUR-Konto
                                        // Periodic Account Statement: EUR Account
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(Perioden\\-Kontoauszug|Tageskontoauszug|Periodic Account Statement): (?<currency>[\\w]{3})(\\-Konto| Account).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 12.04.2018 Lastschrift aktiv 12.04.2018 10.000,00
        // 11.12.2020 Gutschrift 11.12.2020 20,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Lastschrift aktiv|Gutschrift) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Lastschrift aktiv|Gutschrift)) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2022-02-02 Credit SEPA 2022-02-02 1,000.00
        // 2022-02-03 Direct Debit 2022-02-03 1.00
        // @formatter:on
        Block depositEnglishBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (Credit SEPA|Direct Debit) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositEnglishBlock);
        depositEnglishBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<note>(Credit SEPA|Direct Debit)) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 12.04.2018 Lastschrift aktiv 12.04.2018 10.000,00
        // 22.08.2018 SEPA-Ueberweisung 22.08.2018 2.000,00 -
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Lastschrift aktiv|SEPA\\-Ueberweisung) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ \\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Lastschrift aktiv|SEPA\\-Ueberweisung)) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Transaktionskostenpauschale o\\. MwSt\\.|Ordergeb.hr) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ \\-$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(3);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 06.07.2018 Transaktionskostenpauschale o. MwSt. 10.07.2018 2,56 -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Transaktionskostenpauschale o\\. MwSt\\.) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) \\-$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 15.07.2024 Ordergebühr 15.07.2024 0,99 -
                                        // REF:SCALANDnkDU5Lx8
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "date", "amount", "note2") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note1>Ordergeb.hr) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) \\-$") //
                                                        .match("^REF:(?<note2>.*)$$")
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(concatenate(v.get("note1"), trim(v.get("note2")), " | Ref.-Nr.: "));
                                                        }))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2022-05-03 Ordergebühr 2022-05-03 0.99 -
        // ORDERGEBUEHR
        // REF:SCAL4Pm2ZTfDF4c
        // @formatter:on
        Block feesEnglishBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Ordergeb.hr [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\.,\\d]+ \\-$");
        type.addBlock(feesEnglishBlock);
        feesBlock.setMaxSize(3);
        feesEnglishBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note1", "date", "amount", "note2") //
                        .documentContext("currency") //
                        .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<note1>Ordergeb.hr) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<amount>[\\.,\\d]+) \\-$") //
                        .match("^REF:(?<note2>.*)$$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(concatenate(v.get("note1"), trim(v.get("note2")), " | Ref.-Nr.: "));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addFeesAssetManagerTransaction()
    {
        final DocumentType type = new DocumentType("Verg.tung des Verm.gensverwalters");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Rechnung f.r .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Leistungen Beträge (EUR)
                        // Rechnungsbetrag 6,48
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Leistungen Betr.ge \\((?<currency>[\\w]{3})\\)$") //
                        .match("^Rechnungsbetrag *(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abbuchungsdatum: 02.08.2017
                        // @formatter:on
                        .section("date") //
                        .match("^Abbuchungsdatum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Rechnungsnr.: 2017071234567
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Rechnungsnr\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Abrechnungszeitraum 01.07.2017 - 31.07.2017
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Abrechnungszeitraum .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addInterestTransaction()
    {
        final DocumentType type = new DocumentType("Zinsberechnung: Betrag in [\\w]{3}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Zinsberechnung: Betrag in EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Zinsberechnung: Betrag in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Vorgangs\\-Nr|Transaction No)\\.: .*$", "^Die Buchung erfolgt über Konto.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Zinsberechnung: Betrag in EUR
                        // Gesamtsumme: 1,46
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zinsberechnung: Betrag in (?<currency>[\\w]{3})$") //
                        .match("^Gesamtsumme: (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Abschlussbuchung vom 31.10.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Abschlussbuchung vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Vorgangs-Nr.: 12345678
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Vorgangs\\-Nr|Transaction No)\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // für den Zeitraum 30.09.2023 bis 31.10.2023 ergeben sich für das Konto DE12 3456 7891 2345 6789 12
                        // @formatter:on
                        .section("note").optional() //
                        .match("^f.r den Zeitraum (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        final DocumentType type = new DocumentType("Kapitalerh.hung gegen Bareinzahlung");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Einbuchung in Depot|Ausbuchung aus Depot) .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Einbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>(Einbuchung|Ausbuchung)) (in|aus) Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> {
                            if ("Ausbuchung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Ausbuchung aus Depot yyyyyyyyyy per 09.03.2021
                                        // Nominale ISIN: DE000A3H3MF2 WKN: A3H3MF
                                        // STK 96 Enapter AG
                                        // Inhaber-Bezugsrechte
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "nameContinued") //
                                                        .find(".*Ausbuchung aus Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*") //
                                                        .match("^Nominale ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^STK [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                                        // Nominale ISIN: DE000A3H3MG0 WKN: A3H3MG Bezugspreis:
                                        // STK 6 Enapter AG EUR 22,00 p.STK
                                        // junge Inhaber-Aktien o.N.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "wkn", "name", "currency", "nameContinued") //
                                                        .find(".*Einbuchung in Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*") //
                                                        .match("^Nominale ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^STK [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,\\d]+ .*$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // STK 96 Enapter AG
                        // @formatter:on
                        .section("shares") //
                        .match("^STK (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                        // Ausbuchung aus Depot yyyyyyyyyy per 09.03.2021
                        // @formatter:on
                        .section("date") //
                        .match("^(Einbuchung|Ausbuchung) .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Lasten Konto yyyyyyyyyy   Valuta: 09.03.2021 EUR 132,00
                        // @formatter:on
                        .section("currency", "amount").optional()
                        .match("^Zu (Gunsten|Lasten) Konto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bezugsverhältnis: 16 : 1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Bezugsverh.ltnis: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(Fusion \\/ Zusammenlegung" //
                        + "|Depoteinlieferung" //
                        + "|Reverse Split"
                        + "|Obligatorischer Umtausch)", //
                        documentContext -> documentContext //
                                        .section("transaction") //
                                        .match("^(?<transaction>(Fusion \\/ Zusammenlegung" //
                                                        + "|Depoteinlieferung" //
                                                        + "|Reverse Split"
                                                        + "|Obligatorischer Umtausch))$") //
                                        .assign((ctx, v) -> ctx.put("transaction", v.get("transaction"))));
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*(Vorgangs\\-Nr|Transaction No)\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Ausbuchung aus Depot 1686005753 per 18.01.2024
                                        // Nominale ISIN: IE000Y77LGG9 WKN: ETF143
                                        // STK 277,691 Am.ETF-MSCI W.SRI CL.N.Z.AM.P.
                                        // Bear.Shs EUR Acc. oN
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "isin", "wkn", "shares", "name", "nameContinued") //
                                                        .documentContext("transaction") //
                                                        .find(".*Ausbuchung aus Depot .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*") //
                                                        .match("^Nominale ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^STK (?<shares>[\\.,\\d]+) (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            if ("Reverse Split".equals(v.get("transaction")) || "Obligatorischer Umtausch".equals(v.get("transaction")))
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                                                            else
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }),
                                        // @formatter:off
                                        // Nominale ISIN: IE00BL25JL35 WKN: A1103D
                                        // STK 1 Xtr.(IE) - MSCI World Quality
                                        // Registered Shares 1C USD o.N.
                                        // Verwahrart: Wertpapierrechnung Handelstag: 20.01.2023
                                        // Lagerstelle: 1419 Valuta: 25.01.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "isin", "wkn", "shares", "name", "nameContinued") //
                                                        .documentContext("transaction") //
                                                        .match("^Nominale ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) WKN: (?<wkn>[A-Z0-9]{6}).*$") //
                                                        .match("^STK (?<shares>[\\.,\\d]+) (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .find(".*Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*") //
                                                        .assign((t, v) -> {

                                                            if ("Reverse Split".equals(v.get("transaction")) || "Obligatorischer Umtausch".equals(v.get("transaction")))
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorSplitTransactionsNotSupported);
                                                            else
                                                                v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                                                            t.setAmount(0L);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.
        transaction //

                        .section("n").optional() //
                        .match("^Ertragsthesaurierung .*$") //
                        .match("Steuerliquidit.t (?<n>.*)") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true));

        transaction //

                        // @formatter:off
                        // Span. Finanztransaktionssteuer EUR 1,97
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.* Finanztransaktionssteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // German flat rate tax EUR 0.03 -
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^German flat rate tax (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer EUR 127,73 -
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kapitalertragsteuer EUR 127,73 -
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^Kapitalertrags(s)?teuer (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR 11,49 -
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer 0,03 -
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^Kirchensteuer (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 0,02 -
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag 0,02 -
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^Solidarit.tszuschlag (?<tax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer EUR 30,21 -
                        // US-Quellensteuer EUR 0,17 -
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^(US\\-)?Quellensteuer (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // US withholding tax EUR 0.14 -
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^US withholding tax (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+) \\-$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision EUR 0,21
                        // Provision EUR 0,08 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Provision Baader EUR 5,21
                        // Provision Smartbroker EUR 4,00
                        // Provision TradersPlace EUR 4,00
                        // @formatter:on
                        .section("currency", "fee").multipleTimes().optional() //
                        .match("^Provision .* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühren EUR 13,13
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hren (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Gebühren extern ADR EUR 2,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Geb.hren extern ADR (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Mindermengenzuschlag Finanzen.net EUR 1,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Mindermengenzuschlag .* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Stamp HongKong EUR 0,12
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Stamp HongKong (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
