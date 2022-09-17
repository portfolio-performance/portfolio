package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class DADATBankenhausPDFExtractor extends AbstractPDFExtractor
{
    public DADATBankenhausPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DADAT - Bankhaus"); //$NON-NLS-1$
        addBankIdentifier("DADAT-Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addBuySellAccountStatementTransaction();
        addDividendeTransaction();
        addDividendeAccountStatementTransaction();
        addDividendeTaxesAccountStatementTransaction();
        addInterestAccountStatementTransaction();
        addFeesAccountStatementTransaction();
        addDepositRemovalAccountStatementTransaction();
        addTaxLossAdjustmentAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DADAT / Bankhaus Schelhammer & Schattera AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Gesch.ftsart: Kauf$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Titel: US09247X1019 B L A C K R O C K I NC. 
                // Reg. Shares Class A DL -,01
                // Kurswert: -1.800,-- EUR
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert: [\\-\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Zugang: 3 Stk
                .section("shares")
                .match("^Zugang: (?<shares>[\\.,\\d]+) Stk$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeit: 17.2.2021 um 20:49:54 Uhr
                .section("date", "time")
                .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Zu Lasten IBAN IBAN-NR -1.800,-- EUR
                .section("currency", "amount")
                .match("^Zu (Lasten|Gunsten) .* (\\-)?(?<amount>[\\-\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Kauf|Kauf aus Dauerauftrag|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^[\\d]{1,2}\\.[\\d]{1,2} (?<type>(Kauf|Kauf aus Dauerauftrag|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // 16.12 Kauf aus Dauerauftrag            Depot    7800000000/20191216-45514943 18.12 99,68-
                // ISIN LU0378449770 COMST.-NASDAQ-100 U.ETF I               1,22000 STK
                // Kurs                     80,340000  KURSWERT               -98,01 EUR
                .section("date", "year", "isin", "name", "shares", "currency")
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (Kauf|Kauf aus Dauerauftrag|Verkauf) ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+(\\-)?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) [\\s]+(?<shares>[\\.,\\d]+) STK$")
                .match("^.* KURSWERT [\\s]+(\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDate(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 30.07 Kauf                             Depot    780680000/20200730-45125411 31.07 1.250,01-
                .section("amount")
                .match("^[\\d]{1,2}\\.[\\d]{1,2} (Kauf|Kauf aus Dauerauftrag|Verkauf) ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/[\\d]{4}[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Kurs                    282,740000  KURSWERT              1.979,18 USD
                // DevKurs        1,187100/3.9.2020    DADAT Handelsspesen      -7,87 EUR
                // Kurs                    206,940000  KURSWERT             -1.448,58 USD
                // Handelsspesen            -5,06 USD  DevKurs        1,170500/30.7.2020
                .section("fxGross", "fxCurrency", "exchangeRate").optional()
                .match("^.* KURSWERT ([\\s]+)?(\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^.*DevKurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)\\/.*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(type.getCurrentContext().get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(type.getCurrentContext().get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("^Gesch.ftsart: Ertrag$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Titel: US09247X1019 B L A C K R O C K I NC. 
                // Reg. Shares Class A DL -,01
                // Dividende: 4,13 USD 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) ([\\s]+)?(?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("Dividende: [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 3 Stk
                .section("shares")
                .match("^(?<shares>[\\.,\\d]+) Stk$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 23.3.2021
                .section("date")
                .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Zu Gunsten IBAN IBAN-NR 7,51 EUR
                .section("amount", "currency")
                .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // ZINSERTRAG: 12,39 USD
                // Devisenkurs: 1,197 (22.3.2021) 7,51 EUR
                // Ertrag: 10,35 EUR
                .section("fxGross", "fxCurrency", "exchangeRate", "baseCurrency", "gross", "currency").optional()
                .match("^ZINSERTRAG: (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}.\\d{4}\\) [\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$")
                .match("^Ertrag: (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDividendeAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} Ertrag .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // 31.07 Ertrag                           Depot    7800000000/20200730-45756156 30.07 8,16
                // ISIN AT0000969985 AT+S AUST. TECH.SYS.O.N.               45,00000 STK
                // Kurs                      0,250000  ZINSERTRAG               11,25 EUR                
                .section("date", "year", "amount", "isin", "name", "shares", "currency").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Ertrag ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) ([\\s]+)?(?<shares>[\\.,\\d]+) STK$")
                .match("^.* ZINSERTRAG ([\\s]+)?(\\-)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setSecurity(getOrCreateSecurity(v));

                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // 03.08 Ertrag                           Depot    0123456700/20210802-1234567 02.08 63,05
                // ISIN US00206R1023 AT + T INC.          DL 1             200,00000 STK
                // Kurs                      0,520000  ZINSERTRAG              104,00 USD
                // DevKurs        1,195900/30.7.2021
                .section("date", "year", "amount", "isin", "name", "shares", "fxGross", "fxCurrency", "exchangeRate").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Ertrag ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) ([\\s]+)?(?<shares>[\\.,\\d]+) STK$")
                .match("^.* ZINSERTRAG ([\\s]+)?(\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^.*DevKurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)\\/.*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(type.getCurrentContext().get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setSecurity(getOrCreateSecurity(v));

                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(type.getCurrentContext().get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDividendeTaxesAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Steuern aussch.ttungsgl. Ertr.ge|Steuerdividende) .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.TAXES);
                return entry;
            });

        pdfTransaction
                // 08.01 Steuern ausschüttungsgl. Erträge Depot    7800000000/20200107-45942704 03.01 1,34-
                // ISIN LU0378449770 COMST.-NASDAQ-100 U.ETF I               1,22000 STK
                // Kurs                      0,000000  KEST                     -1,51 USD
                // DevKurs        1,123200/2.1.2020
                .section("date", "year", "gross", "isin", "name", "shares", "fxCurrency", "exchangeRate")
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (Steuern aussch.ttungsgl. Ertr.ge|Steuerdividende) ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<gross>[\\.,\\d]+)(\\-)?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) ([\\s]+)?(?<shares>[\\.,\\d]+) STK$")
                .match("^.*KEST ([\\s]+)?\\-[\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .match("^.*DevKurs ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)\\/.*")
                .assign((t, v) -> {
                    v.put("currency", v.get("fxCurrency"));
                    v.put("baseCurrency", asCurrencyCode(type.getCurrentContext().get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setSecurity(getOrCreateSecurity(v));

                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("gross")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(type.getCurrentContext().get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addTaxLossAdjustmentAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("KONTOAUSZUG");
        this.addDocumentTyp(type);

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} KESt\\-Verlustausgleich .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.TAX_REFUND);
                return entry;
            });

        pdfTransaction
                // 15.10 KESt-Verlustausgleich            Depot    7806000200/20211014-41350584 18.10 159,57
                // ISIN US92556H2067 VIACOMCBS INC. BDL-,001                
                // KEST                    159,57 EUR
                .section("date", "note", "year", "amount", "isin", "name", "currency")
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>KESt\\-Verlustausgleich) ([\\s]+)?Depot ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)(\\-)?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^KEST ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(0L);

                    t.setSecurity(getOrCreateSecurity(v));

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addInterestAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");
            Pattern pYear = Pattern.compile("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}\\.(?<year>[\\d]{4}) .*$");

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

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} Abschluss .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.INTEREST);
                return entry;
            });

        pdfTransaction
                // 31.03 Abschluss 31.03 7,26-
                // Sollzinsen
                // AB 2021-01-01          3,9000%               4,76-
                .section("date", "year", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$")
                .find("^(?<note>Sollzinsen)$")
                .match("^AB (?<year>[\\d]{4})\\-[\\d]{1,2}\\-[\\d]{1,2} ([\\s]+)?[\\.,\\d]+% ([\\s]+)?(?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 30.09 Abschluss 30.09 2,54-
                // Sollzinsen                                   0,01-
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$")
                .match("^(?<note>Sollzinsen) ([\\s]+)?(?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addFeesAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");
            Pattern pYear = Pattern.compile("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}.(?<year>[\\d]{4}) .*$");

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

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} (Abschluss|Depotgeb.hrenabrechnung) .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.FEES);
                return entry;
            });

        pdfTransaction
                // 31.03 Abschluss 31.03 7,26-
                // Kontoführungsgebühr                          2,50-
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$")
                .match("^(?<note>Kontof.hrungsgeb.hr) ([\\s]+)?(?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 30.06 Abschluss 30.06 2,53-
                // Spesen                                       2,53-
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+\\-$")
                .match("^(?<note>Spesen) ([\\s]+)?(?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 07.01 Depotgebührenabrechnung per 31.12.2020  20210106  12345678 31.12 63,68-
                .section("date", "note", "year", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>Depotgeb.hrenabrechnung per [\\d]{1,2}\\.[\\d]{1,2}.[\\d]{4}) ([\\s]+)?(?<year>[\\d]{4})[\\d]+ ([\\s]+)?[\\d]+ [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)\\-$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private void addDepositRemovalAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+(\\-)?$");
            Pattern pYear = Pattern.compile("^Alter Saldo per [\\d]{1,2}\\.[\\d]{1,2}.(?<year>[\\d]{4}) .*$");

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

        Block block = new Block("^[\\d]{1,2}\\.[\\d]{1,2} .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DEPOSIT);
                return entry;
            });

        pdfTransaction
                // 18.06 Max Muster 19.06 100,00
                // IBAN: DE17 1234 1234 1234 1234 12
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>(?!(Sollzins ab|Information gem.ß)).*) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$")
                .match("^(IBAN: .*|Transfer)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 31.10 Werbebonus 31.10 75,00
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>Werbebonus) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                // 18.06 Max Muster 19.06 100,00-
                // IBAN: DE17 1234 1234 1234 1234 12
                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>(?!(Sollzins ab|Information gem.ß)).*) [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)\\-$")
                .match("^(IBAN: .*|Transfer)$")
                .assign((t, v) -> {
                    // change from DEPOSIT to REMOVAL
                    t.setType(AccountTransaction.Type.REMOVAL);

                    t.setDateTime(asDate(v.get("date") + "." + type.getCurrentContext().get("year")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // QUELLENSTEUER: -1,86 USD
                .section("withHoldingTax", "currency").optional()
                .match("^QUELLENSTEUER: ([\\s]+)?\\-(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // QUELLENSTEUER           -15,60 USD  Auslands-KESt           -13,00 USD
                .section("withHoldingTax", "currency").optional()
                .match("^QUELLENSTEUER ([\\s]+)?\\-(?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Auslands-KESt: -1,54 USD
                .section("tax", "currency").optional()
                .match("^Auslands\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // QUELLENSTEUER            -3,77 USD  Auslands-KESt            -3,13 USD
                .section("tax", "currency").optional()
                .match("^.* Auslands\\-KESt ([\\s]+)?\\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KEST                   -140,27 USD  Handelsspesen            -5,07 USD
                .section("tax", "currency").optional()
                .match("^KEST ([\\s]+)?\\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelsspesen            -3,66 EUR  DADAT Handelsspesen      -6,36 EUR
                .section("fee", "currency").optional()
                .match("^.*  DADAT Handelsspesen ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // DADAT Handelsspesen      -1,67 EUR
                .section("fee", "currency").optional()
                .match("^DADAT Handelsspesen ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // KEST                   -140,27 USD  Handelsspesen            -5,07 USD
                .section("fee", "currency").optional()
                .match("^.*  Handelsspesen ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsspesen            -3,66 EUR  DADAT Handelsspesen      -6,36 EUR
                .section("fee", "currency").optional()
                .match("^Handelsspesen ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Clearing Gebühr          -1,00 EUR
                .section("fee", "currency").optional()
                .match("^Clearing Geb.hr ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // DADAT Handelsspesen      -7,12 EUR  Clearing Gebühr          -1,00 EUR
                .section("fee", "currency").optional()
                .match("^.*  Clearing Geb.hr ([\\s]+)?\\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
