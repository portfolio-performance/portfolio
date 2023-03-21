package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.DocumentContext;
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
public class PostbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String isJointAccount = "isJointAccount"; //$NON-NLS-1$

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("Anteilige Berechnungsgrundlage .* \\(50,00 %\\).*"); //$NON-NLS-1$
        Boolean bJointAccount = false;

        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(isJointAccount, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(isJointAccount, Boolean.FALSE.toString());

    };

    private static class PeriodicHelper
    {
        private List<PeriodicItem> items = new ArrayList<>();

        public Optional<PeriodicItem> findItem(int lineNumber)
        {
            // search backwards for the first items _before_ the given line
            // number

            for (int ii = items.size() - 1; ii >= 0; ii--) // NOSONAR
            {
                PeriodicItem item = items.get(ii);
                if (item.periodicStartLine > lineNumber)
                    continue;
                else
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    /**
     * Represents the period on the account statement for the year and the currency
     * 
     * <pre>
     *  year per period = year of transaction date
     *  currency per period = base currency of transaction
     * </pre>
     */
    private static class PeriodicItem
    {
        int periodicStartLine;
        int year;

        String baseCurrency;

        @Override
        public String toString()
        {
            return "PeriodicItem [periodicStartLine=" + periodicStartLine + ", year=" + year + ", baseCurrency=" + baseCurrency + "]";
        }
    }

    public PostbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Postbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Postbank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf"
                        + "|Verkauf"
                        + "|Verkauf\\-Festpreisgesch.ft"
                        + "|Ausgabe Investmentfonds"
                        + "|R.cknahme Investmentfonds)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung "
                        + "(Kauf"
                        + "|Verkauf"
                        + "|Verkauf\\-Festpreisgesch.ft"
                        + "|Ausgabe Investmentfonds"
                        + "|R.cknahme Investmentfonds)"
                        + ".*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf|Ausgabe Investmentfonds|R.cknahme Investmentfonds)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Rücknahme Investmentfonds"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Stück 158 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                // REGISTERED SHARES 1C O.N.
                // Ausführungskurs 62,821 EUR
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("^(?<name1>.*)$")
                .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 158 XTR.(IE) - MSCI WORLD IE00BJ0KDQ92 (A1XB5U)
                .section("notation", "shares")
                .match("^(?<notation>(St.ck|[\\w]{3})) (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("Stück"))
                    {
                        BigDecimal shares = asBigDecimal(v.get("shares"));
                        t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
                })

                .oneOf(
                                // Schlusstag 05.02.2020
                                section -> section
                                        .attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Schlusstag/-Zeit 04.02.2020 08:00:04
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Den Gegenwert buchen wir mit Valuta 14.01.2020 zu Gunsten des Kontos 012345678
                                section -> section
                                        .attributes("date")
                                        .match("^Den Gegenwert buchen wir mit Valuta ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // Ausmachender Betrag 9.978,18- EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Abrech.-Preis 196,00 USD
                // Devisenkurs (EUR/USD) 1,06386 vom 12.12.2016
                // Kurswert 1.289,64 EUR
                .section( "fxCurrency", "baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional()
                .match("^(Ausf.hrungskurs|Abrech\\.\\-Preis) [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+) .*$")
                .match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // Limit 43,00 EUR 
                .section("note").optional()
                .match("^(?<note>Limit .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // Barabfindung wegen Fusion
                .section("note").optional()
                .match("^(?<note>Barabfindung wegen .*)$")
                .assign((t, v) -> {
                    if (t.getNote() == null)
                        t.setNote(trim(v.get("note")));
                    else
                        t.setNote(trim(v.get("note") + " | " + t.getNote())); //$NON-NLS-1$
                })

                .conclude(ExtractorUtils.fixGrossValueBuySell())

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift"
                        + "|Aussch.ttung Investmentfonds"
                        + "|Gutschrift von Investmentertr.gen"
                        + "|Ertragsgutschrift"
                        + "|Zinsgutschrift"
                        + "|Kupongutschrift)", jointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift"
                        + "|Aussch.ttung Investmentfonds"
                        + "|Gutschrift von Investmentertr.gen"
                        + "|Ertragsgutschrift .*"
                        + "|Zinsgutschrift"
                        + "|Kupongutschrift)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 12 JOHNSON & JOHNSON SHARES US4781601046 (853260)
                // REGISTERED SHARES DL 1
                // Zahlbarkeitstag 14.01.2022 Ausschüttung pro St. 1,390000000 USD
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("(?<name1>.*)")
                .match("^.* (Aussch.ttung|Dividende|Ertrag) ([\\s]+)?pro (St\\.|St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // EUR 15.000,00 ENEL FINANCE INTL N.V. XS0177089298 (908043)
                // EO-MEDIUM-TERM NOTES 2003(23)
                .section("currency", "name", "isin", "wkn", "name1").optional()
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 16.000,000000 EUR A0JCCZ XS1014610254
                // 2,625% VOLKSWAGEN LEASING MTN.V.14 15.1. 24
                .section("currency", "wkn", "isin", "name").optional()
                .match("^[\\.,\\d]+ (?<currency>[\\w]{3}) (?<wkn>[A-Z0-9]{6}) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(?<name>.*)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // Stück 12 JOHNSON & JOHNSON  SHARES US4781601046 (853260)
                                section -> section
                                        .attributes("shares")
                                        .match("^St.ck (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // EUR 15.000,00 ENEL FINANCE INTL N.V. XS0177089298 (908043)
                                section -> section
                                        .attributes("shares")
                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) .* [A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\)$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                        })
                                ,
                                // 16.000,000000 EUR A0JCCZ XS1014610254
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) [\\w]{3} [A-Z0-9]{6} [A-Z]{2}[A-Z0-9]{9}[0-9]$")
                                        .assign((t, v) -> {
                                            // Percentage quotation, workaround for bonds
                                            BigDecimal shares = asBigDecimal(v.get("shares"));
                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                        })
                        )

                .oneOf(
                                // Zahlbarkeitstag 08.04.2021 Ertrag  pro Stück 0,60 EUR
                                section -> section
                                        .attributes("date")
                                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Gutschrift mit Wert 16.01.2023 309,23 EUR
                                section -> section
                                        .attributes("date")
                                        .match("^Gutschrift mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Ausmachender Betrag 8,64+ EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>\\w{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // Wir überweisen den Betrag von 309,23 EUR auf Ihr Konto 2222222 00.
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Wir überweisen den Betrag von (?<amount>[\\.,\\d]+) (?<currency>\\w{3}) .*$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                .optionalOneOf(
                                // Devisenkurs EUR / USD 1,1920
                                // Dividendengutschrift 12,12 USD 10,17+ EUR
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // Devisenkurs EUR / USD  0,9997  
                                // Zinsertrag 166,25 USD 166,30+ EUR
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+).*$")
                                        .match("^Zinsertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // Bruttoertrag 312,50 USD 285,83 EUR
                                // Umrechnungskurs USD zu EUR 1,0933000000
                                section -> section
                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency")
                                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .match("^Umrechnungskurs (?<termCurrency>[\\w]{3}) zu (?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // Bestandsstichtag 13.09.2022 Laufzeit Zinsschein 180 Tag(e)     
                .section("note").optional()
                .match("^.* (?<note>Zinsschein .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pTransactionPeriod = Pattern.compile("^[\\d]{3} (?<year>[\\d]{4}) [\\d] [\\d]+ [A-Z]{2}(?:[\\s]?[0-9]){18,20} (?<baseCurrency>[\\w]{3}) ([\\-|\\+])? [\\.,\\d]+$");

            PeriodicHelper periodicHelper = new PeriodicHelper();
            context.putType(periodicHelper);

            for (int i = 0; i < lines.length; i++)
            {
                Matcher m = pTransactionPeriod.matcher(lines[i]);
                if (m.matches())
                {
                    PeriodicItem item = new PeriodicItem();
                    item.periodicStartLine = i;
                    item.year = Integer.parseInt(m.group("year"));
                    item.baseCurrency = asCurrencyCode(m.group("baseCurrency"));

                    periodicHelper.items.add(item);
                }
            }
        });
        this.addDocumentTyp(type);

        // 01.08./01.08. D Gut SEPA + 2,70
        // 01.08./01.08. Gutschr.SEPA + 600,00
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.\\/[\\d]{2}\\.[\\d]{2}\\. (D Gut SEPA|Gutschr\\.SEPA) \\+ [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)\\/[\\d]{2}\\.[\\d]{2}\\. (?<note>(D Gut SEPA|Gutschr\\.SEPA)) \\+ (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    PeriodicHelper periodicHelper = context.getType(PeriodicHelper.class)
                                    .orElseGet(PeriodicHelper::new);

                    Optional<PeriodicItem> item = periodicHelper.findItem(v.getStartLineNumber());

                    if (item.isPresent())
                    {
                        t.setDateTime(asDate(v.get("date") + item.get().year));
                        t.setCurrencyCode(asCurrencyCode(item.get().baseCurrency));
                        t.setAmount(asAmount(v.get("amount")));

                        // Formatting some notes
                        if (v.get("note").equals("D Gut SEPA"))
                            v.put("note", "Dauerauftrag");

                        // Formatting some notes
                        if (v.get("note").equals("Gutschr.SEPA"))
                            v.put("note", "SEPA Überweisungsgutschrift");
                        
                        t.setNote(v.get("note"));
                    }
                })

            .wrap(t -> {
                type.getCurrentContext().removeType(PeriodicItem.class);

                if (t.getCurrencyCode() != null && t.getAmount() != 0)
                    return new TransactionItem(t);
                return null;
            }));

        // 06.08./07.08. SEPA Überw. Einzel - 250,00
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.\\/[\\d]{2}\\.[\\d]{2}\\. SEPA Überw\\. Einzel \\- [\\.,\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                .section("date", "note", "amount").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.)\\/[\\d]{2}\\.[\\d]{2}\\. (?<note>SEPA Überw\\. Einzel) \\- (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    PeriodicHelper periodicHelper = context.getType(PeriodicHelper.class)
                                    .orElseGet(PeriodicHelper::new);

                    Optional<PeriodicItem> item = periodicHelper.findItem(v.getStartLineNumber());

                    if (item.isPresent())
                    {
                        t.setDateTime(asDate(v.get("date") + item.get().year));
                        t.setCurrencyCode(asCurrencyCode(item.get().baseCurrency));
                        t.setAmount(asAmount(v.get("amount")));

                        // Formatting some notes
                        if (v.get("note").equals("SEPA Überw. Einzel"))
                            v.put("note", "SEPA Überweisungslastschrift");
                        
                        t.setNote(v.get("note"));
                    }
                })

                .wrap(t -> {
                    type.getCurrentContext().removeType(PeriodicItem.class);

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Einbehaltene Quellensteuer 15 % auf 12,12 USD 1,53- EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 10,17 EUR 1,53 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // Anrechenbare Quellensteuer pro Stück 0,0144878 EUR 0,29 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer pro St.ck [\\.,\\d]+ [\\w]{3} (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // Kapitalertragsteuer (Account)
                // Kapitalertragsteuer 24,51% auf 0,71 EUR 0,17- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kapitalerstragsteuer (Joint Account)
                // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                // Kapitalertragsteuer 25 % auf 50,51 EUR 12,63- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer (KESt) - 105,00 EUR
                // Kapitalertragsteuer (KESt) - 78,13 USD - 71,46 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer \\(KESt\\).* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag (Account)
                // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Solitaritätszuschlag (Joint Account)
                // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                // Solidaritätszuschlag 5,5 % auf 12,63 EUR 0,69- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag auf KESt - 5,77 EUR
                // Solidaritätszuschlag auf KESt - 4,30 USD - 3,93 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf KESt.* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer (Account)
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer (Joint Account)
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                // Kirchensteuer 8,00% auf 42,45 EUR 3,39- EUR
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax1>[\\.,\\d]+)\\- (?<currency1>[\\w]{3})$")
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% auf [\\.,\\d]+ [\\w]{3} (?<tax2>[\\.,\\d]+)\\- (?<currency2>[\\w]{3})$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer auf KESt - 5,77 EUR
                // Kirchensteuer auf KESt - 4,30 USD - 3,93 EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer auf KESt.* \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 39,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Abwicklungskosten Börse 0,04- EUR
                .section("fee", "currency").optional()
                .match("^Abwicklungskosten B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 11,82- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,65- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs\\-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Auslagen 16,86- EUR
                .section("fee", "currency").optional()
                .match("^Fremde Auslagen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
