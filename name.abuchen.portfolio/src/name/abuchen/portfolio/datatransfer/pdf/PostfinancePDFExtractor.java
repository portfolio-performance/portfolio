package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class PostfinancePDFExtractor extends AbstractPDFExtractor
{
    /***
     * Postfinance offers three accounts with different currencies (CHF,
     * EUR, USD) There are two possibilities to buy shares of foreign
     * currencies: 
     * - Transfer money from CHF account to EUR/USD account and buy it in foreign currency 
     * - Buy EUR/USD shares from CHF account directly (actual exchange rate will be taken) 
     * 
     * User manual:
     * https://isotest.postfinance.ch/corporates/help/PostFinance_Testplattform_BenHB.pdf
     */
    public PostfinancePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("PostFinance"); //$NON-NLS-1$

        addBuySellTransaction();
        addSettlementTransaction();
        addDividendeTransaction();
        addAnnualFeesTransaction();
        addInterestTransaction();
        addTaxesTransaction();
        addFeesTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "PostFinance AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsentransaktion: (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^B.rsentransaktion: (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^B.rsentransaktion: (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // UNILEVER DUTCH CERT ISIN: NL0000009355 Amsterdam Euronext
                // 60 47.29 EUR 2'837.40
                .section("name", "isin", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12}) .*$")
                .match("^[\\.,'\\d\\s]+ [\\.,'\\d\\s]+ (?<currency>[\\w]{3}) [\\.,'\\d\\s]+.*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 60 47.29 EUR 2'837.40
                .section("shares")
                .match("^(?<shares>[\\.,'\\d\\s]+) [\\.,'\\d\\s]+ [\\w]{3} [\\.,'\\d\\s]+.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Gemäss Ihrem Kaufauftrag vom 25.09.2018 haben wir folgende Transaktionen vorgenommen:
                // Gemäss Ihrem Verkaufsauftrag vom 20.09.2018 haben wirfolgende Transaktionen vorgenommen:
                .section("date")
                .match("^.* (Kaufauftrag|Verkaufsauftrag) vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Zu Ihren Lasten EUR 2'850.24
                // Zu Ihren Gunsten CHF 7'467.50
                .section("currency", "amount")
                .match("^Zu Ihren (Lasten|Gunsten) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // 55 49.76 EUR 2'736.80
                // Wechselkurs 1.08279
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^[\\.,'\\d\\s]+ [\\.,'\\d\\s]+ (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+).*$")
                .match("^Wechselkurs (?<exchangeRate>[\\.,'\\d\\s]+).*$")
                .match("^Zu Ihren (Lasten|Gunsten) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+.*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));
                    v.put("termCurrency", asCurrencyCode(v.get("currency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Börsentransaktion: Kauf Unsere Referenz: 153557048
                .section("note").optional()
                .match("^B.rsentransaktion: (Kauf|Verkauf) Unsere (?<note>Referenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .conclude(PDFExtractorUtils.fixGrossValueBuySell())

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSettlementTransaction()
    {
        DocumentType type = new DocumentType("Transaktionsabrechnung: Zeichnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Transaktionsabrechnung: Zeichnung Seite: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Pictet - Japan Index - I JPY 1.441 JPY 23 608.200 
                // ISIN LU0188802960 
                .section("name", "currency", "isin")
                .match("^(?<name>.*) [\\w]{3} [\\.,'\\d\\s]+ (?<currency>[\\w]{3}) [\\.,'\\d\\s]+.*$")
                .match("^ISIN (?<isin>[\\w]{12}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Pictet - Japan Index - I JPY 1.441 JPY 23 608.200 
                .section("shares")
                .match("^(?<name>.*) [\\w]{3} (?<shares>[\\.,'\\d\\s]+) [\\w]{3} [\\.,'\\d\\s]+.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // E-Vermögensverwaltung Datum: 20.12.2021 
                .section("date")
                .match("^E\\-Verm.gensverwaltung Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Der Totalbetrag von CHF 280.91 wurde Ihrem Konto CH11 0100 0000 1111 1111 1 mit Valuta 21.12.2021 belastet. 
                .section("currency", "amount")
                .match("^Der Totalbetrag von (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+) .*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kurswert in Handelswährung JPY 34 019.00 
                // Total in Kontowährung zum Kurs von JPY/CHF 0.0082450 CHF 280.91 
                .section("fxCurrency", "fxGross", "termCurrency", "baseCurrency", "exchangeRate", "currency").optional()
                .match("^Kurswert in Handelsw.hrung (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,'\\d\\s]+).*$")
                .match("^Total in Kontow.hrung zum Kurs von (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d\\s]+) (?<currency>[\\w]{3}) [\\.,'\\d\\s]+.*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Auftrag 10111111 
                .section("note").optional()
                .match("^(?<note>Auftrag .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividende|Kapitalgewinn)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende|Kapitalgewinn) Unsere Referenz: .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // ISIN: NL0000009355
                // UNILEVER DUTCH CERT NKN: 2560588 60
                // Dividende 0.4104 EUR
                .section("isin", "name", "currency").optional()
                .match("^ISIN: (?<isin>[\\w]{12}).*$")
                .match("^(?<name>.*) NKN: [\\d]+ [\\.,'\\d\\s]+.*$")
                .match("^(Dividende|Kapitalgewinn) [\\.,'\\d\\s]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // UBS ETF CH - SLI CHF A ISIN: CH0032912732NKN: 3291273 34
                // Dividende 1.66 CHF
                .section("name", "isin", "currency").optional()
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12})([\\s]+)?NKN: [\\d]+ [\\.,'\\d\\s]+.*$")
                .match("^(Dividende|Kapitalgewinn) [\\.,'\\d\\s]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Anzahl 60
                .section("shares")
                .match("^Anzahl (?<shares>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valutadatum 05.06.2019
                .section("date")
                .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Total EUR 20.93
                .section("currency", "amount")
                .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Dividende Unsere Referenz: 169933304
                // Kapitalgewinn Unsere Referenz: 149619136
                .section("note").optional()
                .match("^(Dividende|Kapitalgewinn) Unsere (?<note>Referenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAnnualFeesTransaction()
    {
        DocumentType type = new DocumentType("Jahresgeb.hr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        Block firstRelevantLine = new Block("^Jahresgeb.hr Unsere Referenz: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Jahresgebühr Unsere Referenz: 161333839
                // Valutadatum 03.01.2019
                // Betrag belastet CHF 90.00
                .section("date", "currency", "amount")
                .find("Jahresgeb.hr .*")
                .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .match("^Betrag belastet (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Jahresgebühr Unsere Referenz: 161333839
                .section("note1", "note2")
                .match("^(?<note1>Jahresgeb.hr) Unsere (?<note2>Referenz: .*)$")
                .assign((t, v) -> t.setNote(v.get("note1") + " - " + trim(v.get("note2"))))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addInterestTransaction()
    {
        final DocumentType type = new DocumentType("Zinsabschluss", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Kontonummer|IBAN) .* (?<currency>[A-Z]{3}).*$");
            Pattern pYear = Pattern.compile("^[\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) Kontostand nach Zinsabschluss .*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        Block firstRelevantLine = new Block("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{6}) (\\-|\\–) ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{6}) [\\.,'\\d\\s]+(%| %) [\\.,'\\d\\s]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .oneOf(
                            section -> section
                                    .attributes("note", "date", "amount")
                                    .match("^(?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) "
                                                    + "[\\.,'\\d\\s]+(%| %) "
                                                    + "(?<amount>[\\.,'\\d\\s]+)"
                                                    + ".*$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        t.setDateTime(asDate(v.get("date")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                        t.setNote(v.get("note"));
                                    }),
                            section -> section
                                    .attributes("date1", "date2", "amount")
                                    .match("^(?<date1>[\\d]{6}) (\\-|\\–) "
                                                    + "(?<date2>[\\d]{6}) [\\.,'\\d\\s]+(%| %) "
                                                    + "(?<amount>[\\.,'\\d\\s]+).*$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        // Split date1 and date2
                                        // 010117 - 311217 4.00 % 400.00
                                        String day1 = v.get("date1").substring(0, 2);
                                        String month1 = v.get("date1").substring(2, 4);
                                        String date1 = day1 + "." + month1 + "." + context.get("year");
                                        String day2 = v.get("date2").substring(0, 2);
                                        String month2 = v.get("date2").substring(2, 4);
                                        String date2 = day2 + "." + month2 + "." + context.get("year");

                                        t.setDateTime(asDate(date2));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                        t.setNote(date1 + " - " + date2);
                                    }))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addFeesTransaction()
    {
        DocumentType type = new DocumentType("Zinsabschluss");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        Block firstRelevantLine = new Block("^(?i:Geb.hrenausweis) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gebührenausweis 01.01.2015 - 31.12.2015
                // Zusammenstellung der belasteten Kontoführungsgebühr:
                // CHF 60.00
                .section("note", "date", "currency", "amount")
                .match("^(?i:Geb.hrenausweis) "
                                + "(?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                + "(\\-|\\–) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})).*$")
                .match("^Zusammenstellung der belasteten Kontof.hrungsgeb.hr: "
                                + "(?<currency>[\\w]{3}) (?<amount>[\\.,'\\d\\s]+)(.*)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setNote("Gebührenausweis " + v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addTaxesTransaction()
    {
        final DocumentType type = new DocumentType("Zinsabschluss", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Kontonummer|IBAN) .* (?<currency>[A-Z]{3}).*$");
            Pattern pNote = Pattern.compile("^Zinsabschluss (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pNote.matcher(line);
                if (m.matches())
                    context.put("note", m.group("note"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        Block firstRelevantLine = new Block("^(?i:Verrechnungssteuer) [\\.,'\\d\\s]+(%| %) [\\.,'\\d\\s]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Verrechnungssteuer 35.00% 40.83
                // 31.12.2019 Kontostand nach Zinsabschluss 10 075.83
                .section("amount", "date")
                .match("^(?i:Verrechnungssteuer) [\\.,'\\d\\s]+(%| %) (?<amount>[\\.,'\\d\\s]+).*$")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Kontostand nach Zinsabschluss .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote("Verrechnungssteuer " + context.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Kontonummer|IBAN){1} (.*) (?<currency>[A-Z]{3}).*$");
            Pattern pYear = Pattern.compile("^Kontoauszug [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) .*$");
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                    break;
                }

                m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));
            }

        });
        this.addDocumentTyp(type);

        Block removalBlock = new Block("^.* [\\.,'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.REMOVAL);
                    return entry;
                })

                .section("note", "amount", "date").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "(?<note>.BERTRAG) "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})"
                                + ".*$")
                .match("^AUF KONTO .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    v.put("note", trim(v.get("note")));

                    if ("ÜBERTRAG".equals(v.get("note")))
                        v.put("note", "Übertrag auf Konto");

                    t.setNote(v.get("note"));
                })

                .section("note", "amount", "date").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "(?<note>"
                                + "(AUFTRAG .*LASTSCHRIFT"
                                + "|LASTSCHRIFT"
                                + "|ESR"
                                + "|KAUF\\/DIENSTLEISTUNG(.*\\.[\\d]{4})?"
                                + "|.BERTRAG (AUF|A UF) .*NTO"
                                + "|GIRO .*(OST|ANK|ONAL)( \\(SEPA\\))?"
                                + "|(KAUF\\/)?ONLINE( S.*|-S.*)(.*\\.[\\d]{4})?"
                                + "|BARGELDBEZUG( VOM)?(.*\\.[\\d]{4})?"
                                + "|TWINT .*(ENDEN|DIENSTLEISTUNG)"
                                + "|E\\-FINANCE .*\\-[\\d]+"
                                + "|AUFTRAG DEBIT DIRECT)"
                                + ") "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})"
                                + ".*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    v.put("note", trim(v.get("note")));

                    if (v.get("note").contains("AUFTRAG") && v.get("note").contains("LASTSCHRIFT"))
                    {
                        String splitNote = v.get("note");
                        String[] parts = splitNote.split("AUFTRAG");
                        v.put("note", "Auftrag " + stripBlanks(parts[1]).replace("BASISLASTSCHRIFT", "Basislastschrift"));
                    }

                    if ("LASTSCHRIFT".equals(v.get("note")))
                        v.put("note", "Lastschrift");

                    if ("AUFTRAG DEBIT DIRECT".equals(v.get("note")))
                        v.put("note", "Auftrag DEBIT DIRECT");

                    if ("ONLINE-SHOPPING".equals(v.get("note")))
                        v.put("note", "Online-Shopping");

                    if ("ESR".equals(v.get("note")))
                        v.put("note", "Oranger Einzahlungsschein");

                    if (v.get("note").contains("ÜBERTRAG") && v.get("note").matches("^.*NTO$"))
                        v.put("note", "Übertrag auf Konto");

                    if (v.get("note").contains("GIRO"))
                    {
                        if (v.get("note").matches("^.* \\(SEPA\\)$"))
                            v.put("note", "Giro Internation (SEPA)");
                        else if (v.get("note").matches("^.*OST$"))
                            v.put("note", "Giro Post");
                        else if (v.get("note").matches("^.*ANK$"))
                            v.put("note", "Giro Bank");
                        else
                            v.put("note", "");
                    }

                    if (v.get("note").contains("KAUF/ONLINE"))
                    {
                        if (v.get("note").matches("^.*\\.[\\d]{4}$"))
                        {
                            String splitNote = v.get("note");
                            String[] parts = splitNote.split("OM");
                            v.put("note", "Kauf/Online Shopping vom " + stripBlanks(parts[1]));
                        }
                        else
                        {
                            v.put("note", "Kauf/Online Shopping");
                        }
                    }

                    if (v.get("note").contains("BARGELDBEZUG"))
                    {
                        if (v.get("note").matches("^.*\\.[\\d]{4}$"))
                        {
                            String splitNote = v.get("note");
                            String[] parts = splitNote.split("OM");
                            v.put("note", "Bargeldbezug vom " + stripBlanks(parts[1]));
                        }
                        else
                        {
                            v.put("note", "Bargeldbezug");
                        }
                    }

                    if (v.get("note").contains("TWINT"))
                    {
                        if (v.get("note").matches("^.*ENDEN$"))
                            v.put("note", "TWINT Geld senden");
                        else
                            v.put("note", "TWINT Kauf/Dienstleistung");
                    }

                    if (v.get("note").contains("KAUF/DIENSTLEISTUNG"))
                    {
                        if (v.get("note").matches("^.*\\.[\\d]{4}$"))
                        {
                            String splitNote = v.get("note");
                            String[] parts = splitNote.split("OM");
                            v.put("note", "Kauf/Dienstleistung vom " + stripBlanks(parts[1]));
                        }
                        else
                        {
                            v.put("note", "Kauf/Dienstleistung");
                        }
                    }

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block depositBlock = new Block("^.* [\\.,'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DEPOSIT);
                    return entry;
                })

                .section("note", "amount", "date").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "(?<note>.BERTRAG) "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})"
                                + ".*$")
                .match("^AUS KONTO .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    v.put("note", trim(v.get("note")));

                    if ("ÜBERTRAG".equals(v.get("note")))
                        v.put("note", "Übertrag aus Konto");

                    t.setNote(v.get("note"));
                })

                .section("note", "amount", "date").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "(?<note>"
                                + "(TWINT .*EMPFANGEN"
                                + "|GIRO (AUSLAND|AUS ONLINE-SIC [\\-\\d]+|AUS KONTO)"
                                + "|GUTSCHRIFT VON FREMDBANK [\\-\\d]+"
                                + "|GUTSCHRIFT( .*(BANK|PING))?"
                                + "|.* EINZAHLUNGSSCHEIN\\/QR\\-ZAHLTEIL"
                                + "|.BERTRAG (AUS|A US) .*NTO)"
                                + ") "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})"
                                + ".*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    v.put("note", trim(v.get("note")));

                    if (v.get("note").contains("TWINT"))
                        v.put("note", "TWINT Geld empfangen");

                    if (v.get("note").contains("GIRO"))
                    {
                        if (v.get("note").matches("^.* AUSLAND$"))
                            v.put("note", "Giro Ausland");
                        else if (v.get("note").matches("^.* AUS ONLINE-SIC .*$"))
                            v.put("note", "Giro aus Online-SIC");
                        else if (v.get("note").matches("^.* AUS KONTO$"))
                            v.put("note", "Giro aus Konto");
                        else
                            v.put("note", "");
                    }

                    if (v.get("note").contains("GUTSCHRIFT"))
                    {
                        if (v.get("note").matches("^.*BANK( [\\-\\d]+)?$"))
                            v.put("note", "Gutschrift von Fremdbank");
                        else if (v.get("note").matches("^.*PING$"))
                            v.put("note", "Gutschrift Online Shopping");
                        else
                            v.put("note", "Gutschrift");
                    }

                    if (v.get("note").contains("EINZAHLUNGSSCHEIN"))
                        v.put("note", "Einzahlschein/QR-Zahlteil");

                    if (v.get("note").contains("ÜBERTRAG") && v.get("note").matches("^.*NTO$"))
                        v.put("note", "Übertrag aus Konto");

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feesBlock = new Block("^.* [\\.,'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                .section("note", "amount", "date").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "(?<note>"
                                + "(PREIS F.*"
                                + "|F.R DIE KONTOF.HRUNG"
                                + "|F.R GIRO INTERNATIONAL \\(SEPA\\)"
                                + "|GUTHABENGEB.HR F.R [\\d]{2}\\.[\\d]{4}"
                                + "|JAHRESPREIS LOGIN"
                                + "|.* KONTOAUSZUG PAPIER)"
                                + ")([\\s]+)? "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})"
                                + ".*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    v.put("note", trim(v.get("note")));

                    if ("JAHRESPREIS LOGIN".equals(v.get("note")))
                        v.put("note", "Jahrespreis Login");

                    if ("FÜR GIRO INTERNATIONAL (SEPA)".equals(v.get("note")))
                        v.put("note", "Überweisungsgebühr (SEPA)");

                    if (v.get("note").contains("KONTOAUSZUG PAPIER"))
                        v.put("note", "Kontoführungsgebühr (Papier)");

                    if (v.get("note").contains("PREIS"))
                    {
                        if (v.get("note").matches("^.*HRUNG$"))
                            v.put("note", "Kontoführungsgebühr");
                        else if (v.get("note").matches("^.*SCHALTER$"))
                            v.put("note", "Einzahlungen am Schalter");
                        else
                            v.put("note", "");
                    }

                    if (v.get("note").contains("KONTOFÜHRUNG"))
                        v.put("note", "Kontoführungsgebühr");

                    if (v.get("note").contains("GUTHABENGEBÜHR"))
                    {
                        String splitNote = v.get("note");
                        String[] parts = splitNote.split("FÜR");
                        v.put("note", "Guthabengebühr für " + stripBlanks(parts[1]));
                    }

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block interestBlock = new Block("^.* [\\.,'\\d\\s]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{2}.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.INTEREST);
                    return entry;
                })

                .section("note", "date", "amount").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "ZINSABSCHLUSS (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (\\-|\\–) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})"
                                + ")([\\s]+)? "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}"
                                + ".*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(context.get("currency"));

                    // Formatting some notes
                    t.setNote("Zinsabschluss " + trim(v.get("note")));
                })

                .section("date1", "date2", "amount").optional()
                .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{2} )?"
                                + "ZINSABSCHLUSS (?<date1>[\\d]{6}) (\\-|\\–) (?<date2>[\\d]{6})"
                                + "([\\s]+)? "
                                + "(?<amount>[\\.,'\\d\\s]+) "
                                + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}"
                                + ".*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Split date1 and date2
                    String day1 = v.get("date1").substring(0, 2);
                    String month1 = v.get("date1").substring(2, 4);
                    String date1 = day1 + "." + month1 + "." + context.get("year");
                    String day2 = v.get("date2").substring(0, 2);
                    String month2 = v.get("date2").substring(2, 4);
                    String date2 = day2 + "." + month2 + "." + context.get("year");

                    t.setDateTime(asDate(date2));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));

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
        transaction
                // Abgabe (Eidg. Stempelsteuer) EUR 4.26
                .section("currency", "tax").optional()
                .match("^Abgabe \\(Eidg\\. Stempelsteuer\\) (?<currency>[\\w]{3}) (?<tax>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer 15.00% (NL) EUR 3.69
                .section("currency", "withHoldingTax").optional()
                .match("^Quellensteuer [\\.,'\\d\\s]+(%| %) \\(.*\\) (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Verrechnungssteuer 35% (CH) CHF 19.75
                .section("currency", "tax").optional()
                .match("^Verrechnungssteuer [\\.,'\\d\\s]+(%| %) \\(.*\\) (?<currency>[\\w]{3}) (?<tax>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kommission EUR 8.58
                .section("currency", "fee").optional()
                .match("^Kommission (?<currency>[\\w]{3}) (?<fee>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühren CHF 1.50
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hren (?<currency>[\\w]{3}) (?<fee>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühren und sonstige Spesen EUR 0.60
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hren und sonstige Spesen (?<currency>[\\w]{3}) (?<fee>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Umsatzabgabe JPY 51.00 
                .section("currency", "fee").optional()
                .match("^Umsatzabgabe (?<currency>[\\w]{3}) (?<fee>[\\.,'\\d\\s]+).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        value = value.trim().replaceAll("\\s", "");
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
