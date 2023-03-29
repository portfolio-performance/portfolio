package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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
        addBankIdentifier("RB Augsburger Land West eG"); //$NON-NLS-1$
        addBankIdentifier("Raiffeisenlandesbank"); //$NON-NLS-1$
        addBankIdentifier("Freisinger Bank eG"); //$NON-NLS-1$
        addBankIdentifier("VR Bank"); //$NON-NLS-1$

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
        DocumentType type = new DocumentType("(Kauf|Verkauf|R.cknahme Fonds)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Gesch.ftsart:|Wertpapier Abrechnung) (Kauf|Verkauf|R.cknahme Fonds).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Gesch.ftsart:|Wertpapier Abrechnung) (?<type>(Kauf|Verkauf|R.cknahme Fonds)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Rücknahme Fonds"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Titel: DE000BAY0017 Bayer AG
                                // Namens-Aktien o.N.
                                // Kurs: 53,47 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "name1", "currency")
                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Kurs: [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Kurs:") || !v.get("name1").startsWith("Fondsgesellschaft:"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                                // REGISTERED SHARES DL -,0001        
                                // Ausführungskurs 143,68 EUR Auftragserteilung/ -ort Online-Banking
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                .oneOf(
                                // @formatter:off
                                // Zugang: 2 Stk   
                                // Abgang: 4.500 Stk
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+).*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // Stück 100 QUALCOMM INC.                      US7475251036 (883121)
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Handelszeit: 03.05.2021 13:45:18
                                // Schlusstag/-Zeit 09.11.2021 09:58:45 Auftraggeber Muster
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^(Handelszeit:|Schlusstag\\/\\-Zeit) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Handelszeit: 27.03.2023 
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Handelszeit: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // @formatter:off
                // Zu Lasten IBAN AT99 9999 9000 0011 1110 -107,26 EUR  
                // Zu Gunsten IBAN AT27 3284 2000 0011 1111 36.115,76 EUR
                // Ausmachender Betrag 14.399,34- EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^(Zu (Lasten|Gunsten) .*|Ausmachender Betrag) (\\-)?(?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Kurswert: -1.464,32 CAD 
                // Devisenkurs: 1,406 (20.01.2022) -1.093,40 EUR
                // @formatter:on
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^Kurswert: (\\-)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // Geschäftsart: Rücknahme Fonds Auftrags-Nr.: 47199493 - 27.03.2023
                // @formatter:on
                .section("note").optional()
                .match("^.*(?<note>Auftrags\\-Nr\\.: [\\d]+).*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // Limit bestens
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Limit .*)$")
                .assign((t, v) -> {
                    if (t.getNote() == null)
                        t.setNote(trim(v.get("note")));
                    else
                        t.setNote(t.getNote() + " | " + trim(v.get("note")));
                })

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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                // Titel: DE000BAY0017 Bayer AG
                                // Namens-Aktien o.N.
                                // Dividende: 2 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "name1", "currency")
                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Dividende:") 
                                                            || !v.get("name1").startsWith("Ertrag:") 
                                                            || !v.get("name1").startsWith("Fondsgesellschaft:"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // Stück 100 QUALCOMM INC. US7475251036 (883121)
                                // REGISTERED SHARES DL -,0001
                                // Zahlbarkeitstag 16.12.2021 Dividende pro Stück 0,68 USD
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Zahlbarkeitstag [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Dividende pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                .oneOf(
                                // @formatter:off
                                // 90 Stk  
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) Stk.*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // Stück 100 QUALCOMM INC. US7475251036 (883121)
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [\\w]{12} \\(.*\\)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Valuta 30.04.2021
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Den Betrag buchen wir mit Wertstellung 20.12.2021 zu Gunsten des Kontos 123456789 (IBAN DE11 1111 1111 1111 123456), BLZ 720 692 74 (BIC GENODEF1ZUS).
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // @formatter:off
                // Zu Gunsten IBAN AT99 9999 9000 0011 1111 110,02 EUR 
                // Ausmachender Betrag 50,88+ EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^(Zu Gunsten .*|Ausmachender Betrag) (?<amount>[\\.,\\d]+)(\\+)? (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .optionalOneOf(
                                // @formatter:off
                                // Devisenkurs EUR / USD  1,1360
                                // Dividendengutschrift 68,00 USD 59,86+ EUR
                                // @formatter:on
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$")
                                        .match("^Dividendengutschrift (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Bruttoertrag: 119,37 USD 
                                // Devisenkurs: 1,0856 (13.01.2023) 109,37 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("fxGross", "fxCurrency", "exchangeRate", "currency")
                                        .match("^Bruttoertrag: (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}\\) [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                // @formatter:off
                // Ex-Tag 01.12.2021 Art der Dividende Quartalsdividende
                // @formatter:on
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

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
                    context.put("currency", m.group("currency"));

                m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("nr", m.group("nr"));
                    context.put("year", m.group("year"));
                }
            }
        });
        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.REMOVAL);
                    return entry;
                })

                // Is type --> "H" change from DEPOSIT to REMOVAL
                .section("type").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [\\.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                        t.setType(AccountTransaction.Type.DEPOSIT);
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
                                + "|LOHN\\/GEHALT"
                                + "|.berweisung SEPA)"
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

                    if ("EURO-UEBERWEISUNG".equals(v.get("note")))
                        v.put("note", "EURO-Überweisung");

                    if ("BASISLASTSCHRIFT".equals(v.get("note")))
                        v.put("note", "Basislastschrift");

                    if ("DAUERAUFTRAG".equals(v.get("note")))
                        v.put("note", "Dauerauftrag");

                    if ("GUTSCHRIFT".equals(v.get("note")))
                        v.put("note", "Gutschrift");

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
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.INTEREST);
                    return entry;
                })

                // Is type --> "S" change from INTEREST to INTEREST_CHARGE
                .section("type").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("S"))
                        t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                })

                // @formatter:off
                // 30.12. 31.12. Abschluss PN:905                                                      1,95 S
                //          9,60000% einger. Kontoüberziehung    3112       1,00S
                //          14,60000% einger. Kontoüberziehung    3112       1,00S
                // @formatter:on
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
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [S|H]$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                // Is type --> "H" change from FEES to FEES_REFUND
                .section("type").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [\\.,\\d]+ (?<type>[S|H])$")
                .assign((t, v) -> {
                    if (v.get("type").equals("H"))
                        t.setType(AccountTransaction.Type.FEES_REFUND);
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

                // @formatter:off
                // 31.08. 31.08. Abschluss PN:905                                                      1,95 S
                //          Buchungen automatisch    23 2345       0,00H
                //          Kontoführungsentgelt        2345       1,95S
                // Abschluss vom 30.07.2021 bis 31.08.2021
                // @formatter:on
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
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Kapitalertragsteuer 24,51 % auf 15,08 EUR 3,69- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Solidaritätszuschlag 5,5 % auf 3,69 EUR 0,20- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kirchensteuer 8 % auf 3,69 EUR 0,30- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\d.]+,\\d+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Quellensteuer: -47,48 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Quellensteuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Auslands-KESt: -22,50 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Auslands\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // KESt ausländische Dividende: -0,64 USD
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KESt ausl.ndische Dividende: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // KESt: -10,00 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Umsatzsteuer: -0,29 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Umsatzsteuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kursgewinn-KESt: -696,65 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kursgewinn\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Einbehaltene Quellensteuer 15 % auf 68,00 USD 8,98- EUR
                // @formatter:on
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltene Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Anrechenbare Quellensteuer 15 % auf 59,86 EUR 8,98 EUR
                // @formatter:on
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+ % .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Serviceentgelt: -0,32 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Serviceentgelt: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Provision 0,2000 % vom Kurswert 28,74- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Eigene Spesen 2,50- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Übertragungs-/Liefergebühr 0,10- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Handelsortentgelt inkl. Fremdspesen: -4,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Handelsortentgelt inkl\\. Fremdspesen: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Gebühren: -25,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Geb.hren: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Orderleitgebühr: -3,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Orderleitgeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Inkassogebühr: -1,45 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Inkassogeb.hr: \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
