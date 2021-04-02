package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class JustTradePDFExtractor extends AbstractPDFExtractor
{
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT = "^(\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}) -?(?<amount>[\\.\\d]+[,\\d]*).*$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_NEW_FORMAT = "^(\\d+\\.\\d+\\.\\d{4}) (\\d+\\.\\d+\\.\\d{4}).*(\\s|-)(?<amount>[\\.\\d]+,\\d{2})$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_AND_SHARES = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) (?<sign>[-])?(?<amount>[\\.\\d]+[,\\d]*) .* -?(?<shares>[\\.\\d]+[,\\d]*)$"; //$NON-NLS-1$
    private static final String REGEX_AMOUNT_AND_SHARES_NEW_FORMAT = "^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+)[^,]* -?(?<shares>[\\.\\d]+,\\d{4}) ([\\.\\d]+,\\d{4}\\s)?(?<sign>[-])?(?<amount>[\\.\\d]+,\\d{2})$"; //$NON-NLS-1$
    private static final String REGEX_DATE = "^(\\d+\\.\\d+\\.\\d{4}) (?<date>\\d+\\.\\d+\\.\\d{4}) .*"; //$NON-NLS-1$

    public JustTradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE"); //$NON-NLS-1$
        addBankIdentifier("Sutor"); //$NON-NLS-1$
        addBankIdentifier("SUTOR BANK"); //$NON-NLS-1$

        addBuyTransactionOld();
        addBuySellTransaction();
        addDividendeTransaction();
        addDeposit();
        addBuySellAccountTransaction();
        addFeePayment();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Sutor Bank / justTRADE"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransactionOld()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Stück .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // REGISTERED SHS USD (DIST) O.N.
                .section("shares", "name", "isin", "wkn", "name1")
                .match("^Stück (?<shares>[\\d.,]+) (?<name>.*) (?<isin>\\S*) \\((?<wkn>\\S*)\\)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-\\/Ausf.hrungsplatz"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                .section("date", "time")
                .match("^Schlusstag\\/\\-Zeit (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 1.340,64- EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[.,\\d]+)- (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Transaktionsart: (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Produktbezeichnung - .*", "Ausmachender Betrag:.*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // if type is "Verkauf" change from BUY to SELL
                .section("type")
                .match("Transaktionsart: (?<type>[\\w]+)")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Produktbezeichnung - Vanguard FTSE All-World U.ETF Re
                // Internationale Wertpapierkennnummer (ISIN): IE00BK5BQT80
                // Währung: EUR
                .section("name", "isin", "currency").optional()
                .match("Produktbezeichnung - (?<name>.*)").match(".*")
                .match("Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>[\\w]{12})")
                .match("Währung: (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    type.getCurrentContext().put("currency", v.get("currency"));
                })

                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                .section("date", "time")
                .match("Orderausführung Datum\\/Zeit: (?<date>\\d+ .* \\d{4}) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> {
                    // Formate the date from 05. Oktober 2009 to 05.10.2009
                    // Work-around DateTimeFormatter in Local.GERMAN looks like "Mär" not "Mrz" and in Local.ENGLISH like "Mar".
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date").replace("Mrz", "Mär"), DateTimeFormatter.ofPattern("d LLL yyyy", Locale.GERMANY))));

                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Stück/Nominale: 29,00
                .section("shares")
                .match("Stück\\/Nominale: (?<shares>[0-9.]+(\\,[0-9]{2})).*")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausmachender Betrag: €2.083,94
                .section("amount")
                .match("Ausmachender Betrag: (\\W{1})(?<amount>([0-9.]+)\\,([0-9]{2}))")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende [^pro]).*");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Produktbezeichnung - Kellogg Co.:
                // Internationale Wertpapierkennnummer US4878361082
                .section("name", "isin")
                .match("^Produktbezeichnung - (?<name>.*):")
                .match("^Internationale Wertpapierkennnummer (?<isin>.*)")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Anzahl/Nominale 30,00
                .section("shares")
                .match("^Anzahl\\/Nominale (?<shares>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex Datum - Tag 01. März 2021
                .section("date")
                .match("^Ex Datum - Tag (?<date>\\d+. .* \\d{4})")
                .assign((t, v) -> {
                    // Formate the date from 01. März 2021 to 01.03.2021
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date"), DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Ausmachender Betrag EUR 12,15
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addDeposit()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".*([^staatlichen] Zulage|automatischer Lastschrifteinzug|Einzahlung).*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( // check for old and new format
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }),
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }))
                        .wrap(TransactionItem::new);
    }

    @SuppressWarnings("nls")
    private void addBuySellAccountTransaction()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* (Kauf|Verkauf|Gebührentilgung) .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name").match("^.* (Kauf|Verkauf|Gebührentilgung) (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .oneOf( //
                                // check for old format, if amount is negative,
                                // we buy
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for old format - if amount is
                                        // positive (no sign), we sell
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }),
                                        // check for BUY with new format
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for SELL with new format
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeePayment()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(
                        ".* (Verwaltungsgebühr/Vertriebskosten|anteil.Verwaltgebühr/Vertriebskosten|Kontoführungs-u.Depotgebühren).*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( // check for old and new format
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }),
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // Sutor always
                                                            // provides the
                                                            // amount in EUR,
                                                            // column
                                                            // "Betrag in EUR"
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }))
                        .wrap(TransactionItem::new);
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragssteuer: €0,73
                .section("tax").optional()
                .match("Kapitalertragssteuer: (\\W{1})(?<tax>([0-9.]+)\\,([0-9]{2}))")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag: €0,04
                .section("tax").optional()
                .match("Solidaritätszuschlag: (\\W{1})(?<tax>([0-9.]+)\\,([0-9]{2}))")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltende Quellensteuer EUR 2,14
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltende Quellensteuer (?<currency>\\w{3}) (?<quellensteinbeh>[.,\\d]+)$")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer EUR 2,14
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer (?<currency>\\w{3}) (?<quellenstanr>[.,\\d]+)$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                .section("currency", "fee").optional()
                .match(".* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* Tradinggebühren: (?<currency>[\\w]{3}) (?<fee>[\\d.-]+,\\d+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\d.-]+,\\d+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @SuppressWarnings("nls")
    private void addTax(DocumentType type, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        if (checkWithholdingTax(type, taxtype))
        {
            ((name.abuchen.portfolio.model.Transaction) t)
                    .addUnit(new Unit(Unit.Type.TAX, 
                                    Money.of(asCurrencyCode(v.get("currency")), 
                                                    asAmount(v.get(taxtype)))));
        }
    }

    @SuppressWarnings("nls")
    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    @SuppressWarnings("nls")
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

    @SuppressWarnings("nls")
    private boolean checkWithholdingTax(DocumentType type, String taxtype)
    {
        if (Boolean.valueOf(type.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype))
            { 
                return false; 
            }
        }
        return true;
    }
}
