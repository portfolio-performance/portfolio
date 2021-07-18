package name.abuchen.portfolio.datatransfer.pdf;

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

@SuppressWarnings("nls")
public class RaiffeisenBankgruppePDFExtractor extends AbstractPDFExtractor
{
    public RaiffeisenBankgruppePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raiffeisenbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransactions();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Raiffeisenbank Bankgruppe"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Kauf|Verkauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Gesch.ftsart: (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("Gesch.ftsart: (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Titel: DE000BAY0017 Bayer AG
                // Namens-Aktien o.N.
                // Kurs: 53,47 EUR
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurs: ([.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Zugang: 2 Stk   
                // Abgang: 4.500 Stk   
                .section("shares")
                .match("^(Zugang|Abgang): (?<shares>[.,\\d]+)(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeit: 03.05.2021 13:45:18 
                .section("date", "time")
                .match("^Handelszeit: (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Zu Lasten IBAN AT99 9999 9000 0011 1110 -107,26 EUR  
                // Zu Gunsten IBAN AT27 3284 2000 0011 1111 36.115,76 EUR 
                .section("amount", "currency")
                .match("^Zu (Lasten|Gunsten) .* ([-])?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("Gesch.ftsart: Ertrag");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Titel: DE000BAY0017 Bayer AG
                // Namens-Aktien o.N.
                // Dividende: 2 EUR
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Dividende: ([.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Dividende:"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Geschäftsart: Ertrag
                // 90 Stk    
                .section("shares")
                .match("^Gesch.ftsart: .*$")
                .match("^(?<shares>[.,\\d]+) Stk(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Extag: 28.04.2021
                .section("date")
                .match("^Extag: (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Zu Gunsten IBAN AT99 9999 9000 0011 1111 110,02 EUR 
                .section("amount", "currency")
                .match("^Zu Gunsten .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransactions()
    {
        DocumentType type1 = new DocumentType("Kontokorrent", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^([\\w]{3})(-Konto Kontonummer)(.*)$");
            Pattern pYear = Pattern.compile("^[\\d]+ .* Kontoauszug Nr. ([\\s]+)?(?<nr>[\\d]+)\\/(?<year>[\\d]{4})");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
            
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("nr", m.group(2));
                    // Read year
                    context.put("year", m.group(3));
                }
            }
        });
        this.addDocumentTyp(type1);
        
        DocumentType type = new DocumentType("Kontokorrent");
        this.addDocumentTyp(type);

        Block depositremoval = new Block("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [S|H]$");
        type.addBlock(depositremoval);
        Transaction<AccountTransaction> pdfTransactionDepositRemoval = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.REMOVAL);
                return entry;
            });

        pdfTransactionDepositRemoval
                // Is type --> "H" change from DEPOSIT to REMOVAL
                .section("type").optional()
                .match("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                    {
                        t.setType(AccountTransaction.Type.DEPOSIT);
                    }
                })

                .section("day", "month", "amount", "note").optional()
                .match("^\\d{2}\\.\\d{2}\\. (?<day>\\d{2})\\.(?<month>\\d{2})\\. (?<note>Einnahmen|BASISLASTSCHRIFT|DAUERAUFTRAG|EURO-UEBERWEISUNG|GUTSCHRIFT) .* (?<amount>[.,\\d]+) [S|H]$")
                .match("^(?![\\s]+ [Dividende]).*$")
                .match("^(?![\\s]+ [Dividende]).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type1.getCurrentContext();

                    if (context.get("nr").compareTo("01") == 0  && Integer.parseInt(v.get("month")) < 3)
                    {
                        Integer year = Integer.parseInt(context.get("year")) + 1;
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + year.toString()));
                    }
                    else
                    {
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + context.get("year")));
                    }

                    t.setCurrencyCode(context.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() > 0)
                        return new TransactionItem(t);
                    else
                        return null;
                });

        depositremoval.set(pdfTransactionDepositRemoval);

        Block interest = new Block("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [S|H]$");
        type.addBlock(interest);
        Transaction<AccountTransaction> pdfTransactionInterest = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.INTEREST);
                return entry;
            });

        pdfTransactionInterest
                // Is type --> "S" change from INTEREST to INTEREST_CHARGE
                .section("type").optional()
                .match("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("S"))
                    {
                        t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    }
                })

                .section("day", "month", "amount1", "amount2", "note").optional()
                .match("^\\d{2}\\.\\d{2}\\. (?<day>\\d{2}).(?<month>\\d{2}). (Abschluss) .* [.,\\d]+ [S|H]$")
                .match("^[\\s]+ [.,\\d]+% einger. Konto.berziehung .* (?<amount1>[.,\\d]+)[S|H]$")
                .match("^[\\s]+ [.,\\d]+% einger. Konto.berziehung .* (?<amount2>[.,\\d]+)[S|H]$")
                .match("^[\\s]+ (?<note>Entgelte vom \\d{2}\\.\\d{2}\\.\\d{4} .* \\d{2}\\.\\d{2}\\.\\d{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type1.getCurrentContext();

                    if (context.get("nr").compareTo("01") == 0  && Integer.parseInt(v.get("month")) < 3)
                    {
                        Integer year = Integer.parseInt(context.get("year")) + 1;
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + year.toString()));
                    }
                    else
                    {
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + context.get("year")));
                    }
                    t.setCurrencyCode(context.get("currency"));
                    t.setAmount(asAmount(v.get("amount1")) + asAmount(v.get("amount2")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() > 0)
                        return new TransactionItem(t);
                    else
                        return null;
                });

        interest.set(pdfTransactionInterest);

        Block fees = new Block("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [S|H]$");
        type.addBlock(fees);
        Transaction<AccountTransaction> pdfTransactionFees = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.FEES);
                return entry;
            });

        pdfTransactionFees
                // Is type --> "H" change from FEES to FEES_REFUND
                .section("type").optional()
                .match("^\\d{2}\\.\\d{2}\\. \\d{2}\\.\\d{2}\\. .* [.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                    {
                        t.setType(AccountTransaction.Type.FEES_REFUND);
                    }
                })

                .section("day", "month", "amount1", "amount2", "amount3", "note").optional()
                .match("^\\d{2}\\.\\d{2}\\. (?<day>\\d{2}).(?<month>\\d{2}). (Abschluss) .* [.,\\d]+ [S|H]$")
                .match("^[\\s]+ Buchungen Online .* (?<amount1>[.,\\d]+)[S|H]$")
                .match("^[\\s]+ Buchungen automatisch .* (?<amount2>[.,\\d]+)[S|H]$")
                .match("^[\\s]+ Kontof.hrungsentgelt .* (?<amount3>[.,\\d]+)[S|H]$")
                .match("^[\\s]+ (?<note>Abschluss vom \\d{2}\\.\\d{2}\\.\\d{4} .* \\d{2}\\.\\d{2}\\.\\d{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type1.getCurrentContext();

                    if (context.get("nr").compareTo("01") == 0  && Integer.parseInt(v.get("month")) < 3)
                    {
                        Integer year = Integer.parseInt(context.get("year")) + 1;
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + year.toString()));
                    }
                    else
                    {
                        t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + context.get("year")));
                    }
                    t.setCurrencyCode(context.get("currency"));
                    t.setAmount(asAmount(v.get("amount1")) + asAmount(v.get("amount2")) + asAmount(v.get("amount3")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() > 0)
                        return new TransactionItem(t);
                    else
                        return null;
                });

        fees.set(pdfTransactionFees);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Quellensteuer: -47,48 EUR 
                .section("tax", "currency").optional()
                .match("^Quellensteuer: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Auslands-KESt: -22,50 EUR 
                .section("tax", "currency").optional()
                .match("^Auslands-KESt: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kursgewinn-KESt: -696,65 EUR 
                .section("tax", "currency").optional()
                .match("^Kursgewinn-KESt: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Serviceentgelt: -0,32 EUR 
                .section("fee", "currency").optional()
                .match("^Serviceentgelt: -(?<fee>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
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
