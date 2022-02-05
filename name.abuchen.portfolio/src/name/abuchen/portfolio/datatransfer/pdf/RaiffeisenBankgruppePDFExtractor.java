package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.strip;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class RaiffeisenBankgruppePDFExtractor extends AbstractPDFExtractor
{
    public RaiffeisenBankgruppePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raiffeisenbank"); //$NON-NLS-1$
        addBankIdentifier("RB Augsburger Land West eG"); //$NON-NLS-1$
        addBankIdentifier("Raiffeisenlandesbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransactions();
    }

    @Override
    public String getLabel()
    {
        return "Raiffeisenbank Bankgruppe"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Gesch.ftsart:|Wertpapier Abrechnung) (Kauf|Verkauf)(.*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Gesch.ftsart:|Wertpapier Abrechnung) (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Titel: DE000BAY0017 Bayer AG
                // Namens-Aktien o.N.
                // Kurs: 53,47 EUR
                .section("isin", "name", "name1", "currency").optional()
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurs: ([\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:") || !v.get("name1").startsWith("Fondsgesellschaft:"))
                        v.put("name", strip(v.get("name")) + " " + strip(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                // REGISTERED SHARES DL -,0001        
                // Ausführungskurs 143,68 EUR Auftragserteilung/ -ort Online-Banking
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", strip(v.get("name")) + " " + strip(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // Zugang: 2 Stk   
                                // Abgang: 4.500 Stk  
                                section -> section
                                        .attributes("shares")
                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+)(.*)?$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [\\w]{12} \\(.*\\)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // Handelszeit: 03.05.2021 13:45:18
                // Schlusstag/-Zeit 09.11.2021 09:58:45 Auftraggeber Muster 
                .section("date", "time")
                .match("^(Handelszeit:|Schlusstag\\/\\-Zeit) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})(.*)?$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Zu Lasten IBAN AT99 9999 9000 0011 1110 -107,26 EUR  
                // Zu Gunsten IBAN AT27 3284 2000 0011 1111 36.115,76 EUR
                // Ausmachender Betrag 14.399,34- EUR
                .section("amount", "currency")
                .match("^(Zu (Lasten|Gunsten) .*|Ausmachender Betrag) (\\-)?(?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Devisenkurs: 1,406 (20.01.2022) -1.093,40 EUR 
                .section("exchangeRate").optional()
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ [\\w]{3}(.*)?$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // -1.537,32 CAD 
                // Devisenkurs: 1,406 (20.01.2022) -1.093,40 EUR 
                .section("fxCurrency", "fxAmount", "exchangeRate").optional()
                .match("^(\\-)?(?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})(.*)?$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ [\\w]{3}(.*)?$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // Limit bestens
                .section("note").optional()
                .match("^(?<note>Limit .*)$")
                .assign((t, v) -> t.setNote(strip(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ertrag|Dividendengutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Gesch.ftsart: Ertrag|Dividendengutschrift)$");
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
                .section("isin", "name", "name1", "currency").optional()
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Dividende: [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Dividende:"))
                        v.put("name", strip(v.get("name")) + " " + strip(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 100 QUALCOMM INC. US7475251036 (883121)
                // REGISTERED SHARES DL -,0001
                // Zahlbarkeitstag 16.12.2021 Dividende pro Stück 0,68 USD
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Dividende pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", strip(v.get("name")) + " " + strip(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // 90 Stk  
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) Stk(.*)?$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // Stück 100 QUALCOMM INC. US7475251036 (883121)
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [\\w]{12} \\(.*\\)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // Valuta 30.04.2021
                                section -> section
                                        .attributes("date")
                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Den Betrag buchen wir mit Wertstellung 20.12.2021 zu Gunsten des Kontos 123456789 (IBAN DE11 1111 1111 1111 123456), BLZ 720 692 74 (BIC GENODEF1ZUS). 
                                section -> section
                                        .attributes("date")
                                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // Zu Gunsten IBAN AT99 9999 9000 0011 1111 110,02 EUR 
                // Ausmachender Betrag 50,88+ EUR
                .section("amount", "currency")
                .match("^(Zu Gunsten .*|Ausmachender Betrag) (?<amount>[\\.,\\d]+)(\\+)? (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / USD  1,1360
                // Dividendengutschrift 68,00 USD 59,86+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs [\\w]{3} \\/ [\\w]{3} ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$")
                .match("^Dividendengutschrift (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), 
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), 
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                // Ex-Tag 01.12.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(strip(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransactions()
    {
        DocumentType type = new DocumentType("(Kontokorrent|Privatkonto)", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(?<currency>[\\w]{3})\\-Konto Kontonummer .*$");
            Pattern pYear = Pattern.compile("^.* Kontoauszug Nr\\. ([\\s]+)?(?<nr>[\\d]+)\\/(?<year>[\\d]{4})$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }

                m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("nr", m.group("nr"));
                    context.put("year", m.group("year"));
                }
            }
        });
        this.addDocumentTyp(type);

        Block depositremoval = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
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
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [\\.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                    {
                        t.setType(AccountTransaction.Type.DEPOSIT);
                    }
                })

                .section("day", "month", "amount", "note").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. "
                                + "(?i:"
                                + "(?<note>Einnahmen"
                                + "|BASISLASTSCHRIFT"
                                + "|DAUERAUFTRAG"
                                + "|EURO\\-UEBERWEISUNG"
                                + "|GUTSCHRIFT"
                                + "|Kartenzahlung"
                                + "|Auszahlung"
                                + "|LOHN\\/GEHALT)"
                                + ") .* "
                                + "(?<amount>[\\.,\\d]+) [S|H]$")
                .match("^(?![\\s]+ [Dividende]).*$")
                .match("^(?![\\s]+ [Dividende]).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

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
                    
                    // Formatting some notes
                    if ("LOHN/GEHALT".equals(v.get("note")))
                        v.put("note", "Lohn/Gehalt");
                    else if ("EURO-UEBERWEISUNG".equals(v.get("note")))
                        v.put("note", "EURO-Überweisung");
                    else
                        v.put("note", v.get("note").substring(0, 1).toUpperCase() + v.get("note").substring(1, v.get("note").length()).toLowerCase());

                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getAmount() > 0)
                        return new TransactionItem(t);
                    else
                        return null;
                });

        depositremoval.set(pdfTransactionDepositRemoval);

        Block interest = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
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
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("S"))
                    {
                        t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    }
                })

                // 30.12. 31.12. Abschluss PN:905                                                      1,95 S
                //          9,60000% einger. Kontoüberziehung    3112       1,00S
                //          14,60000% einger. Kontoüberziehung    3112       1,00S
                .section("day", "month", "amount1", "amount2", "note").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ [S|H]$")
                .match("^[\\s]+ [\\.,\\d]+% einger. Konto.berziehung .* (?<amount1>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ [\\.,\\d]+% einger. Konto.berziehung .* (?<amount2>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ (?<note>Entgelte vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

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

        Block fees = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
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
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [\\.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                    {
                        t.setType(AccountTransaction.Type.FEES_REFUND);
                    }
                })

                // 30.12. 31.12. Abschluss PN:905                                                      1,95 S
                // 9,60000% einger. Kontoüberziehung    3112       1,00S
                // 14,60000% einger. Kontoüberziehung    3112       1,00S
                //Entgelte vom 01.12.2020 - 31.12.2020
                //          Buchungen Online   St.    4 3112       0,00H
                //          Buchungen automatisch    12 3112       0,00H
                //          Kontoführungsentgelt        3112       1,95S
                // Abschluss vom 01.10.2020 bis 31.12.2020
                .section("day", "month", "amount1", "amount2", "amount3", "note").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ [S|H]$")
                .match("^[\\s]+ Buchungen Online .* (?<amount1>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ Buchungen automatisch .* (?<amount2>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ Kontof.hrungsentgelt .* (?<amount3>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ (?<note>Abschluss vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

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

                // 31.08. 31.08. Abschluss PN:905                                                      1,95 S
                //          Buchungen automatisch    23 2345       0,00H
                //          Kontoführungsentgelt        2345       1,95S
                // Abschluss vom 30.07.2021 bis 31.08.2021
                .section("day", "month", "amount1", "amount2", "note").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<day>[\\d]{2}).(?<month>[\\d]{2}). (Abschluss) .* [\\.,\\d]+ [S|H]$")
                .match("^[\\s]+ Buchungen automatisch .* (?<amount1>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ Kontof.hrungsentgelt .* (?<amount2>[\\.,\\d]+)[S|H]$")
                .match("^[\\s]+ (?<note>Abschluss vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

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

        fees.set(pdfTransactionFees);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Quellensteuer: -47,48 EUR 
                .section("tax", "currency").optional()
                .match("^Quellensteuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Auslands-KESt: -22,50 EUR 
                .section("tax", "currency").optional()
                .match("^Auslands\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kursgewinn-KESt: -696,65 EUR 
                .section("tax", "currency").optional()
                .match("^Kursgewinn\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 15 % auf 68,00 USD 8,98- EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltende Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 59,86 EUR 8,98 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Serviceentgelt: -0,32 EUR 
                .section("fee", "currency").optional()
                .match("^Serviceentgelt: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision 0,2000 % vom Kurswert 28,74- EUR
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen 2,50- EUR
                .section("fee", "currency").optional()
                .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,10- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsortentgelt inkl. Fremdspesen: -4,00 EUR 
                .section("fee", "currency").optional()
                .match("^Handelsortentgelt inkl\\. Fremdspesen: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Gebühren: -25,00 EUR 
                .section("fee", "currency").optional()
                .match("^Geb.hren: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Orderleitgebühr: -3,00 EUR 
                .section("fee", "currency").optional()
                .match("^Orderleitgeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
