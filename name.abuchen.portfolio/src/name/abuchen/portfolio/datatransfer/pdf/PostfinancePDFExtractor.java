package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
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
 * @implNote Postfinance offers three accounts with different currencies (CHF, EUR, USD).
 *           There are two possibilities to buy shares of foreign currencies:
 *
 *           - Transfer money from CHF account to EUR/USD account and buy it in foreign currency
 *           - Buy EUR/USD shares from CHF account directly (actual exchange rate will be taken)
 *
 * @implSpec User manual:
 *           https://isotest.postfinance.ch/corporates/help/PostFinance_Testplattform_BenHB.pdf
 * @formatter:on
 */

@SuppressWarnings("nls")
public class PostfinancePDFExtractor extends AbstractPDFExtractor
{
    public PostfinancePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("PostFinance");

        addBuySellTransaction();
        addSettlementTransaction();
        addDividendeTransaction();
        addPaymentTransaction();
        addAnnualFeesTransaction();
        addDepotFeesTransaction();
        addInterestTransaction();
        addTaxesTransaction();
        addFeesTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "PostFinance AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsentransaktion: (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^B.rsentransaktion: (Kauf|Verkauf) .*$");
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
                        .match("^B.rsentransaktion: (?<type>(Kauf|Verkauf)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // UNILEVER DUTCH CERT ISIN: NL0000009355 Amsterdam Euronext
                        // 60 47.29 EUR 2'837.40
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                        .match("^[\\.'\\d\\s]+ [\\.,'\\d\\s]+ (?<currency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 60 47.29 EUR 2'837.40
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.'\\d\\s]+) [\\.,'\\d\\s]+ [\\w]{3} [\\.'\\d\\s]+.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Gemäss Ihrem Kaufauftrag vom 25.09.2018 haben wir folgende Transaktionen vorgenommen:
                        // Gemäss Ihrem Verkaufsauftrag vom 20.09.2018 haben wirfolgende Transaktionen vorgenommen:
                        // @formatter:on
                        .section("date") //
                        .match("^.* (Kaufauftrag|Verkaufsauftrag) vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Ihren Lasten EUR 2'850.24
                        // Zu Ihren Gunsten CHF 7'467.50
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Zu Ihren (Lasten|Gunsten) (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // 55 49.76 EUR 2'736.80
                        // Wechselkurs 1.08279
                        // Zu Ihren Lasten CHF 2'968.50
                        // @formatter:on
                        .section("baseCurrency", "fxGross", "exchangeRate", "termCurrency").optional() //
                        .match("^[\\.'\\d\\s]+ [\\.'\\d\\s]+ (?<baseCurrency>[\\w]{3}) (?<fxGross>[\\.'\\d\\s]+).*$") //
                        .match("^Wechselkurs (?<exchangeRate>[\\.'\\d\\s]+).*$") //
                        .match("^Zu Ihren (Lasten|Gunsten) (?<termCurrency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Börsentransaktion: Kauf Unsere Referenz: 153557048
                        // @formatter:on
                        .section("note").optional() //
                        .match("^B.rsentransaktion: (Kauf|Verkauf) Unsere (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSettlementTransaction()
    {
        DocumentType type = new DocumentType("Transaktionsabrechnung: (Zeichnung|Fondssparplan)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Transaktionsabrechnung: (Zeichnung|Fondssparplan) Seite: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Position Anteile Währung Kurs
                                        // Pictet - Japan Index - I JPY 1.441 JPY 23 608.200
                                        // ISIN LU0188802960
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .find("Position Anteile W.hrung Kurs.*") //
                                                        .match("^(?<name>.*) [\\w]{3} [\\.'\\d\\s]+ (?<currency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Position Anteile Währung Kurs
                                        // PF - Global Fund A 1.216 CHF 162.830
                                        // ISIN CH0014933193
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .find("Position Anteile W.hrung Kurs.*") //
                                                        .match("^(?<name>.*) [\\.'\\d\\s]+ (?<currency>[\\w]{3}) [\\.'\\d\\s]+.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Position Anteile Währung Kurs
                                        // Pictet - Japan Index - I JPY 1.441 JPY 23 608.200
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Position Anteile W.hrung Kurs.*") //
                                                        .match("^.* [\\w]{3} (?<shares>[\\.'\\d\\s]+) [\\w]{3} [\\.'\\d\\s]+.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Position Anteile Währung Kurs
                                        // PF - Global Fund A 1.216 CHF 162.830
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("Position Anteile W.hrung Kurs.*") //
                                                        .match("^.* (?<shares>[\\.'\\d\\s]+) [\\w]{3} [\\.'\\d\\s]+.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // E-Vermögensverwaltung Datum: 20.12.2021
                        // Selfservice Fonds Datum: 31.07.2023
                        // @formatter:on
                        .section("date") //
                        .match("^(E\\-Verm.gensverwaltung|Selfservice Fonds) Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Der Totalbetrag von CHF 280.91 wurde Ihrem Konto CH11 0100 0000 1111 1111 1 mit Valuta 21.12.2021 belastet.
                        // Der Totalbetrag von  CHF 200.00 wurde Ihrem Konto  CH81 0900 1234 8952 2587 6 mit Valuta  02.08.2023 belastet.
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Der Totalbetrag von ([\\s]+)?(?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+) .*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Kurswert in Handelswährung JPY 34 019.00
                        // Total in Kontowährung zum Kurs von JPY/CHF 0.0082450 CHF 280.91
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^Kurswert in Handelsw.hrung [\\w]{3} (?<fxGross>[\\.'\\d\\s]+).*$") //
                        .match("^Total in Kontow.hrung zum Kurs von (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d\\s]+) [\\w]{3} [\\.'\\d\\s]+.*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getTermCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Auftrag 10111111
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrag .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividende|Kapitalgewinn)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividende|Kapitalgewinn) Unsere Referenz: .*$");
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
                                        // ISIN: NL0000009355
                                        // UNILEVER DUTCH CERT NKN: 2560588 60
                                        // Dividende 0.4104 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^(?<name>.*) NKN: [\\d]+ [\\.'\\d\\s]+.*$") //
                                                        .match("^(Dividende|Kapitalgewinn) [\\.'\\d\\s]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // UBS ETF CH - SLI CHF A ISIN: CH0032912732NKN: 3291273 34
                                        // Dividende 1.66 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])([\\s]+)?NKN: [\\d]+ [\\.'\\d\\s]+.*$") //
                                                        .match("^(Dividende|Kapitalgewinn) [\\.'\\d\\s]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Anzahl 60
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl (?<shares>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valutadatum 05.06.2019
                        // @formatter:on
                        .section("date") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Total EUR 20.93
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Dividende Unsere Referenz: 169933304
                        // Kapitalgewinn Unsere Referenz: 149619136
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Dividende|Kapitalgewinn) Unsere (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addPaymentTransaction()
    {
        DocumentType type = new DocumentType("Zahlungsverkehr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Zahlungsverkehr \\- (Gutschrift|Belastung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // Is type --> "Belastung" change from DEPOSIT to REMOVAL
                        .section("type").optional() //
                        .match("^Zahlungsverkehr \\- (?<type>(Gutschrift|Belastung)) .*$") //
                        .assign((t, v) -> {
                            if ("Belastung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);
                        })

                        // @formatter:off
                        // Valutadatum 02.05.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Total CHF 1'200.00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Zahlungsverkehr - Gutschrift Unsere Referenz: 391377700
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addAnnualFeesTransaction()
    {
        DocumentType type = new DocumentType("Jahresgeb.hr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Jahresgeb.hr Unsere Referenz: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Jahresgebühr Unsere Referenz: 161333839
                        // Valutadatum 03.01.2019
                        // Betrag belastet CHF 90.00
                        // @formatter:on
                        .section("date", "currency", "amount") //
                        .find("Jahresgeb.hr .*") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .match("^Betrag belastet (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Jahresgebühr Unsere Referenz: 161333839
                        // @formatter:on
                        .section("note1", "note2") //
                        .match("^(?<note1>Jahresgeb.hr) Unsere (?<note2>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(v.get("note1"), trim(v.get("note2")), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addDepotFeesTransaction()
    {
        DocumentType type = new DocumentType("Depotgeb.hr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Depotgeb.hr Unsere Referenz: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Depotgebühr Unsere Referenz: 464973467
                        // Valutadatum 01.01.2024
                        // Betrag belastet CHF 18.00
                        // @formatter:on
                        .section("date", "currency", "amount") //
                        .find("Depotgeb.hr .*") //
                        .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .match("^Betrag belastet (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Depotgebühr Unsere Referenz: 464973467
                        // @formatter:on
                        .section("note1", "note2") //
                        .match("^(?<note1>Depotgeb.hr) Unsere (?<note2>Referenz: .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(v.get("note1"), trim(v.get("note2")), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addInterestTransaction()
    {
        final DocumentType type = new DocumentType("Zinsabschluss", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // IBAN CH64 0900 0000 1111 2222 1 CHF
                                        // Kontonummer 11-22222-0 CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(Kontonummer|IBAN) .* (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // 31.12.2019 Kontostand nach Zinsabschluss 10 004.59
                                        // @formatter:on
                                        .section("year") //
                                        .match("^[\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) Kontostand nach Zinsabschluss .*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{6}) (\\-|\\–) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{6}) [\\.'\\d\\s]+(%| %) [\\.'\\d\\s]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("note", "date", "amount") //
                                                        .documentContext("currency", "year") //
                                                        .match("^(?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) " //
                                                                        + "[\\.'\\d\\s]+(%| %) " //
                                                                        + "(?<amount>[\\.'\\d\\s]+)" //
                                                                        + ".*$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(v.get("note"));
                                                        }),
                                        section -> section //
                                                        .attributes("date1", "date2", "amount") //
                                                        .documentContext("currency", "year") //
                                                        .match("^(?<date1>[\\d]{6}) (\\-|\\–) " //
                                                                        + "(?<date2>[\\d]{6}) [\\.'\\d\\s]+(%| %) " //
                                                                        + "(?<amount>[\\.'\\d\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            // Split date1 and date2
                                                            // 010117 - 311217 4.00 % 400.00
                                                            String day1 = v.get("date1").substring(0, 2);
                                                            String month1 = v.get("date1").substring(2, 4);
                                                            String date1 = day1 + "." + month1 + "." + v.get("year");

                                                            String day2 = v.get("date2").substring(0, 2);
                                                            String month2 = v.get("date2").substring(2, 4);
                                                            String date2 = day2 + "." + month2 + "." + v.get("year");

                                                            t.setDateTime(asDate(date2));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setNote(date1 + " - " + date2);
                                                        }))

                        .wrap(TransactionItem::new);
    }

    private void addFeesTransaction()
    {
        DocumentType type = new DocumentType("Zinsabschluss");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(?i:Geb.hrenausweis) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gebührenausweis 01.01.2015 - 31.12.2015
                        // Zusammenstellung der belasteten Kontoführungsgebühr:
                        // CHF 60.00
                        // @formatter:on
                        .section("note", "date", "currency", "amount") //
                        .match("^(?i:Geb.hrenausweis) " //
                                        + "(?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(\\-|\\–) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})).*$") //
                        .match("^Zusammenstellung der belasteten Kontof.hrungsgeb.hr: (?<currency>[\\w]{3}) (?<amount>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote("Gebührenausweis " + v.get("note"));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addTaxesTransaction()
    {
        final DocumentType type = new DocumentType("Zinsabschluss", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // IBAN CH64 0900 0000 1111 2222 1 CHF
                                        // Kontonummer 11-22222-0 CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(Kontonummer|IBAN) .* (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Zinsabschluss 01.01.2019 - 31.12.2019 Datum: 01.01.2020
                                        // @formatter:on
                                        .section("note") //
                                        .match("^Zinsabschluss (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                        .assign((ctx, v) -> ctx.put("note", v.get("note"))));

        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(?i:Verrechnungssteuer) [\\.'\\d\\s]+(%| %) [\\.'\\d\\s]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Verrechnungssteuer 35.00% 40.83
                        // 31.12.2019 Kontostand nach Zinsabschluss 10 075.83
                        // @formatter:on
                        .section("amount", "date") //
                        .documentContext("currency", "note") //
                        .match("^(?i:Verrechnungssteuer) [\\.'\\d\\s]+(%| %) (?<amount>[\\.'\\d\\s]+).*$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Kontostand nach Zinsabschluss .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote("Verrechnungssteuer " + v.get("note"));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // IBAN CH64 0900 0000 1111 2222 1 CHF
                                        // Kontonummer 11-22222-0 CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(Kontonummer|IBAN).* (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Kontoauszug 01.04.2018 - 30.04.2018 Datum: 01.05.2018
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Kontoauszug [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) .*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        Block removalBlock = new Block("^.* [\\.'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "(?<note>.BERTRAG) " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .match("^AUF KONTO .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            v.put("note", trim(v.get("note")));

                            if ("ÜBERTRAG".equals(v.get("note")))
                                v.put("note", "Übertrag auf Konto");

                            t.setNote(v.get("note"));
                        })

                        .section("note", "amount", "date").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "(?<note>(AUFTRAG .*LASTSCHRIFT" //
                                        + "|LASTSCHRIFT" //
                                        + "|ESR" //
                                        + "|KAUF\\/DIENSTLEISTUNG(.*\\.[\\d]{4})?" //
                                        + "|.BERTRAG (AUF|A UF) .*NTO" //
                                        + "|GIRO .*(OST|ANK|ONAL)( \\(SEPA\\))?" //
                                        + "|(KAUF\\/)?ONLINE( S.*|-S.*)(.*\\.[\\d]{4})?" //
                                        + "|BARGELDBEZUG( VOM)?(.*\\.[\\d]{4})?" //
                                        + "|TWINT .*(ENDEN|DIENSTLEISTUNG)" //
                                        + "|E\\-FINANCE .*\\-[\\d]+" //
                                        + "|AUFTRAG DEBIT DIRECT" //
                                        + "|.BERWEISUNG AUF KONTO)) " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            String note = trim(v.get("note"));

                            if (note.contains("AUFTRAG") && note.contains("LASTSCHRIFT"))
                            {
                                String[] parts = note.split("AUFTRAG");
                                note = concatenate("Auftrag", stripBlanks(parts[1]).replace("BASISLASTSCHRIFT", "Basislastschrift"), " ");
                            }
                            else if ("LASTSCHRIFT".equals(note))
                                note = "Lastschrift";
                            else if ("ÜBERWEISUNG AUF KONTO".equals(note))
                                note = "Überweisung";
                            else if ("AUFTRAG DEBIT DIRECT".equals(note))
                                note = "Auftrag DEBIT DIRECT";
                            else if ("ONLINE-SHOPPING".equals(note))
                                note = "Online-Shopping";
                            else if ("ESR".equals(note))
                                note = "Oranger Einzahlungsschein";
                            else if (note.contains("ÜBERTRAG") && note.matches("^.*NTO$"))
                                note = "Übertrag auf Konto";
                            else if (note.contains("GIRO"))
                            {
                                if (note.matches("^.* \\(SEPA\\)$"))
                                    note = "Giro Internation (SEPA)";
                                else if (note.matches("^.*OST$"))
                                    note = "Giro Post";
                                else if (note.matches("^.*ANK$"))
                                    note = "Giro Bank";
                                else
                                    note = "";
                            }
                            else if (note.contains("KAUF/ONLINE"))
                            {
                                if (note.matches("^.*\\.[\\d]{4}$"))
                                {
                                    String[] parts = note.split("OM");
                                    note = concatenate("Kauf/Online Shopping vom", stripBlanks(parts[1]), " ");
                                }
                                else
                                {
                                    note = "Kauf/Online Shopping";
                                }
                            }
                            else if (note.contains("BARGELDBEZUG"))
                            {
                                if (note.matches("^.*\\.[\\d]{4}$"))
                                {
                                    String[] parts = note.split("OM");
                                    note = concatenate("Bargeldbezug vom", stripBlanks(parts[1]), " ");
                                }
                                else
                                {
                                    note = "Bargeldbezug";
                                }
                            }
                            else if (note.contains("TWINT"))
                            {
                                if (note.matches("^.*ENDEN$"))
                                    note = "TWINT Geld senden";
                                else
                                    note = "TWINT Kauf/Dienstleistung";
                            }
                            else if (note.contains("KAUF/DIENSTLEISTUNG"))
                            {
                                if (note.matches("^.*\\.[\\d]{4}$"))
                                {
                                    String[] parts = note.split("OM");
                                    note = concatenate("Kauf/Dienstleistung vom", stripBlanks(parts[1]), " ");
                                }
                                else
                                {
                                    note = "Kauf/Dienstleistung";
                                }
                            }

                            t.setNote(note);
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block depositBlock = new Block("^.* [\\.'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "(?<note>.BERTRAG) " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .match("^AUS KONTO .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            String note = trim(v.get("note"));

                            if ("ÜBERTRAG".equals(note))
                                note = "Übertrag aus Konto";

                            t.setNote(note);
                        })

                        .section("note", "amount", "date").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "(?<note>(TWINT .*EMPFANGEN" //
                                        + "|GIRO (AUSLAND|AUS ONLINE-SIC [\\-\\d]+|AUS KONTO)" //
                                        + "|GUTSCHRIFT VON FREMDBANK [\\-\\d]+" //
                                        + "|GUTSCHRIFT( .*(BANK|PING))?" //
                                        + "|.* EINZAHLUNGSSCHEIN\\/QR\\-ZAHLTEIL" //
                                        + "|.BERTRAG (AUS|A US) .*NTO)) " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            String note = trim(v.get("note"));

                            if (note.contains("TWINT"))
                                note = "TWINT Geld empfangen";

                            if (note.contains("GIRO"))
                            {
                                if (note.matches("^.* AUSLAND$"))
                                    note = "Giro Ausland";
                                else if (note.matches("^.* AUS ONLINE-SIC .*$"))
                                    note = "Giro aus Online-SIC";
                                else if (note.matches("^.* AUS KONTO$"))
                                    note = "Giro aus Konto";
                                else
                                    note = "";
                            }

                            if (note.contains("GUTSCHRIFT"))
                            {
                                if (note.matches("^.*BANK( [\\-\\d]+)?$"))
                                    note = "Gutschrift von Fremdbank";
                                else if (note.matches("^.*PING$"))
                                    note = "Gutschrift Online Shopping";
                                else
                                    note = "Gutschrift";
                            }

                            if (note.contains("EINZAHLUNGSSCHEIN"))
                                note = "Einzahlschein/QR-Zahlteil";

                            if (note.contains("ÜBERTRAG") && note.matches("^.*NTO$"))
                                note = "Übertrag aus Konto";

                            t.setNote(note);
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block feesBlock = new Block("^.* [\\.'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note", "amount", "date").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "(?<note>(PREIS F.*" //
                                        + "|F.R DIE KONTOF.HRUNG" //
                                        + "|F.R GIRO INTERNATIONAL \\(SEPA\\)" //
                                        + "|GUTHABENGEB.HR F.R [\\d]{2}\\.[\\d]{4}" //
                                        + "|JAHRESPREIS LOGIN" //
                                        + "|.* KONTOAUSZUG PAPIER))" //
                                        + "([\\s]+)? " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            String note = trim(v.get("note"));

                            if ("JAHRESPREIS LOGIN".equals(note))
                                note = "Jahrespreis Login";

                            if ("FÜR GIRO INTERNATIONAL (SEPA)".equals(note))
                                note = "Überweisungsgebühr (SEPA)";

                            if (note.contains("KONTOAUSZUG PAPIER"))
                                note = "Kontoführungsgebühr (Papier)";

                            if (note.contains("PREIS"))
                            {
                                if (note.matches("^.*HRUNG$"))
                                    note = "Kontoführungsgebühr";
                                else if (note.matches("^.*SCHALTER$"))
                                    note = "Einzahlungen am Schalter";
                                else
                                    note = "";
                            }

                            if (note.contains("KONTOFÜHRUNG"))
                                note = "Kontoführungsgebühr";

                            if (note.contains("GUTHABENGEBÜHR"))
                            {
                                String[] parts = note.split("FÜR");
                                note = "Guthabengebühr für " + stripBlanks(parts[1]);
                            }

                            t.setNote(note);
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block interestBlock = new Block("^.* [\\.'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("note", "date", "amount").optional() //
                        .documentContext("currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "ZINSABSCHLUSS (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}))([\\s]+)? " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            t.setNote("Zinsabschluss " + trim(v.get("note")));
                        })

                        .section("date1", "date2", "amount").optional() //
                        .documentContext("year", "currency") //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?" //
                                        + "ZINSABSCHLUSS (?<date1>[\\d]{6}) (\\-|\\–) (?<date2>[\\d]{6})" //
                                        + "([\\s]+)? " //
                                        + "(?<amount>[\\.'\\d\\s]+) " //
                                        + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$") //
                        .assign((t, v) -> {
                            // Split date1 and date2
                            String day1 = v.get("date1").substring(0, 2);
                            String month1 = v.get("date1").substring(2, 4);
                            String date1 = day1 + "." + month1 + "." + v.get("year");
                            String day2 = v.get("date2").substring(0, 2);
                            String month2 = v.get("date2").substring(2, 4);
                            String date2 = day2 + "." + month2 + "." + v.get("year");

                            t.setDateTime(asDate(date2));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            t.setNote("Zinsabschluss " + date1 + " - " + date2);
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Abgabe (Eidg. Stempelsteuer) EUR 4.26
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Abgabe \\(Eidg\\. Stempelsteuer\\) (?<currency>[\\w]{3}) (?<tax>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer 15.00% (NL) EUR 3.69
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Quellensteuer [\\.'\\d\\s]+(%| %) \\(.*\\) (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Verrechnungssteuer 35% (CH) CHF 19.75
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer [\\.'\\d\\s]+(%| %) \\(.*\\) (?<currency>[\\w]{3}) (?<tax>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Umsatzabgabe JPY 51.00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Umsatzabgabe (?<currency>[\\w]{3}) (?<tax>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kommission EUR 8.58
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kommission (?<currency>[\\w]{3}) (?<fee>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühren CHF 1.50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hren (?<currency>[\\w]{3}) (?<fee>[\\.'\\d\\s]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühren und sonstige Spesen EUR 0.60
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hren und sonstige Spesen (?<currency>[\\w]{3}) (?<fee>[\\.'\\d\\s]+).*$") //
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
