package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
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
        addDividendTaxesAccountStatementTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
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

        Block firstRelevantLine = new Block("^Geschäftsart: Kauf");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Titel: US09247X1019  B L A C K R O C K  I NC.
                // Reg. Shares Class A DL -,01      
                .section("isin", "name", "name1")
                .match("^Titel: (?<isin>[\\w]{12}) [\\s+]?(?<name>.*)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1"));
                    else
                        v.put("name", v.get("name").trim());
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Zugang: 3 Stk
                .section("shares")
                .match("^Zugang: (?<shares>[.,\\d]+) Stk$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Handelszeit: 17.2.2021 um 20:49:54 Uhr
                .section("date", "time")
                .match("^Handelszeit: (?<date>\\d+.\\d+.\\d{4}+) .* (?<time>\\d+:\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Zu Lasten IBAN IBAN-NR -1.800,-- EUR 
                .section("currency", "amount")
                .match("^Zu Lasten .* -(?<amount>[.\\d]+(,[\\d]{2})?).* (?<currency>[\\w]{3}).*$")
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
            Pattern pCurrency = Pattern.compile(".* (?<currency>[\\w]{3}) [.,\\d]+([-])?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^\\d+.\\d+ (Kauf|Kauf aus Dauerauftrag|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^\\d+.\\d+ (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 16.12 Kauf aus Dauerauftrag            Depot    7800000000/20191216-45514943 18.12 99,68-
                // ISIN LU0378449770 COMST.-NASDAQ-100 U.ETF I               1,22000 STK
                // Kurs                     80,340000  KURSWERT               -98,01 EUR
                .section("date", "year", "amount", "isin", "name", "shares", "currency")
                .match("^(?<date>\\d+.\\d+) (Kauf|Kauf aus Dauerauftrag|Verkauf) [\\s]+Depot [\\s]+[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]+.[\\d]+ (?<amount>[.,\\d]+)([-])?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) [\\s]+(?<shares>[.,\\d]+) STK$")
                .match("^.* KURSWERT [\\s]+([-])?[.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDate(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Kurs                    282,740000  KURSWERT              1.979,18 USD
                // DevKurs        1,187100/3.9.2020    DADAT Handelsspesen      -7,87 EUR
                .section("fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^.* KURSWERT [\\s]+(?<fxamount>[.,\\d]+) (?<fxcurrency>[\\w]{3})[-]?$")
                .match("^DevKurs [\\s]+(?<exchangeRate>[.,\\d]+)\\/.*")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxcurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxamount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // DevKurs        1,187100/3.9.2020    DADAT Handelsspesen      -7,87 EUR
                .section("exchangeRate").optional()
                .match("^DevKurs [\\s]+(?<exchangeRate>[.,\\d]+)\\/.*")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("^Geschäftsart: Ertrag");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // 3 Stk
                // Titel: US09247X1019  B L A C K R O C K  I NC.                     
                // Reg. Shares Class A DL -,01     
                .section("shares", "isin","name", "name1", "currency")
                .match("(?<shares>[.,\\d]+) Stk$")
                .match("^Titel: (?<isin>[\\w]{12}) [\\s+]?(?<name>.*)$")
                .match("(?<name1>.*)")
                .match("Dividende: [.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1"));
                    else
                        v.put("name", v.get("name").trim());
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Extag: 4.3.2021
                .section("date")
                .match("^Extag: (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // Zu Gunsten IBAN IBAN-NR 7,51 EUR 
                .section("currency", "amount").optional()
                .match("^Zu Gunsten .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // ZINSERTRAG: 12,39 USD 
                // Devisenkurs: 1,197 (22.3.2021) 7,51 EUR 
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^ZINSERTRAG: (?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[.,\\d]+) \\(\\d+.\\d+.\\d{4}\\) (?<amount>[.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long localAmount = exchangeRate.multiply(BigDecimal.valueOf(fxAmount.getAmount()))
                                            .longValue();
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), localAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long forexAmount = exchangeRate.multiply(BigDecimal.valueOf(amount.getAmount()))
                                            .longValue();
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), forexAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        // remove existing unit to replace with new one
                        Optional<Unit> grossUnit = t.getUnit(Unit.Type.GROSS_VALUE);
                        if (grossUnit.isPresent())
                        {
                            t.removeUnit(grossUnit.get());
                        }
                        t.addUnit(grossValue);
                    }
                })

                // Devisenkurs: 1,197 (22.3.2021)
                .section("exchangeRate").optional()
                .match("^Devisenkurs: (?<exchangeRate>[.,\\d]+) \\(\\d+.\\d+.\\d{4}\\)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        
        block.set(pdfTransaction);
    }

    private void addDividendeAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile(".* (?<currency>[\\w]{3}) [.,\\d]+([-])?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("^\\d+.\\d+ Ertrag .*$");
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
                .section("date", "year", "amount", "isin", "name", "shares", "currency")
                .match("^(?<date>\\d+.\\d+) Ertrag [\\s]+Depot [\\s]+[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]+.[\\d]+ (?<amount>[.,\\d]+)([-])?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) [\\s]+(?<shares>[.,\\d]+) STK$")
                .match("^.* ZINSERTRAG [\\s]+([-])?[.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        
        block.set(pdfTransaction);
    }

    private void addDividendTaxesAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile(".* (?<currency>[\\w]{3}) [.,\\d]+([-])?$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^\\d+.\\d+ Steuern aussch.ttungsgl. Ertr.ge .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAXES);
                    return t;
                })

                // 08.01 Steuern ausschüttungsgl. Erträge Depot    7800000000/20200107-45942704 03.01 1,34-
                // ISIN LU0378449770 COMST.-NASDAQ-100 U.ETF I               1,22000 STK
                // Kurs                      0,000000  KEST                     -1,51 USD
                // DevKurs        1,123200/2.1.2020
                .section("date", "year", "amount", "isin", "name", "shares", "fxAmount", "fxCurrency", "exchangeRate")
                .match("^(?<date>\\d+.\\d+) Steuern aussch.ttungsgl. Ertr.ge ([\\s]+)?Depot [\\s]+[\\d]+\\/(?<year>[\\d]{4})[\\d]+\\-[\\d]+ [\\d]+.[\\d]+ (?<amount>[.,\\d]+)([-])?$")
                .match("^ISIN (?<isin>[\\w]{12}) (?<name>.*) [\\s]+(?<shares>[.,\\d]+) STK$")
                .match("^.* KEST [\\s]+\\-(?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^DevKurs [\\s]+(?<exchangeRate>[.,\\d]+)\\/.*")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                    t.setShares(asShares(v.get("shares")));
                    v.put("currency", v.get("fxCurrency"));
                    t.setSecurity(getOrCreateSecurity(v));

                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));

                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.addUnit(grossValue);
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // QUELLENSTEUER: -1,86 USD
                .section("tax", "currency").optional()
                .match("^QUELLENSTEUER: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // Auslands-KESt: -1,54 USD
                .section("tax", "currency").optional()
                .match("^Auslands-KESt: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // KEST                   -140,27 USD  Handelsspesen            -5,07 USD
                .section("tax", "currency").optional()
                .match("^KEST [\\s]+-(?<tax>[.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelsspesen            -3,66 EUR  DADAT Handelsspesen      -6,36 EUR
                .section("fee", "currency").optional()
                .match("^(.*)?  DADAT Handelsspesen [\\s]+-(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // DADAT Handelsspesen      -1,67 EUR
                .section("fee", "currency").optional()
                .match("^DADAT Handelsspesen [\\s]+-(?<fee>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // KEST                   -140,27 USD  Handelsspesen            -5,07 USD
                .section("fee", "currency").optional()
                .match("^(.*)?  Handelsspesen [\\s]+-(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsspesen            -3,66 EUR  DADAT Handelsspesen      -6,36 EUR
                .section("fee", "currency").optional()
                .match("^Handelsspesen [\\s]+-(?<fee>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Clearing Gebühr          -1,00 EUR
                .section("fee", "currency").optional()
                .match("^Clearing Geb.hr [\\s]+-(?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}
