package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

public class JustTradePDFExtractor extends AbstractPDFExtractor
{
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

        addBuySellTransaction();
        addDividendeTransaction();
        addDepositAccountTransaction();
        addBuySellAccountTransaction();
        addFeePaymentAccountTransaction();
        addRemoveAccountTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sutor Bank / justTRADE"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Transaktionsart:|Wertpapier Abrechnung) (Kauf|Verkauf)|Fälligkeit/Verfall)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(F.lligkeit\\/Verfall|Wertpapier Abrechnung|WERTPAPIERABRECHNUNG)(.*)?$", "^Ausmachender Betrag.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // if type is "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Transaktionsart: |Wertpapier Abrechnung )?(?<type>(Kauf|Verkauf|F.lligkeit\\/Verfall))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf")
                            || v.get("type").equals("Fälligkeit/Verfall"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Produktbezeichnung - Vanguard FTSE All-World U.ETF Re
                // Internationale Wertpapierkennnummer (ISIN): IE00BK5BQT80
                // Währung: EUR
                .section("name", "isin", "currency").optional()
                .match("^Produktbezeichnung - (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>[\\w]{12})$")
                .match("^W.hrung: (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));

                    t.setSecurity(getOrCreateSecurity(v));
                    type.getCurrentContext().put("currency", v.get("currency"));
                })

                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // REGISTERED SHS USD (DIST) O.N.
                // Ausführungskurs 25,295 EUR
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Ausf.hrungskurs [.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Produktbezeichnung - Discount Zertifikat auf Münchener Rückversicherungs AG:
                // Internationale Wertpapierkennnummer DE000VP63TQ3
                // Rückzahlungskurs EUR 230,000000
                .section("name", "isin", "currency").optional()
                .match("^Produktbezeichnung - (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer (?<isin>[\\w]{12})$")
                .match("^R.ckzahlungskurs (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück/Nominale: 29,00
                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // Anzahl/Nominale 9,00
                .section("shares")
                .match("^(St.ck|Anzahl)(\\/Nominale(:)?)? (?<shares>[.,\\d]+)(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                .section("date", "time").optional()
                .match("Orderausführung Datum\\/Zeit: (?<date>\\d+ .* \\d{4}) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> t.setDate(asDate(v.get("date").replace("Mrz", "Mär"), v.get("time"))))

                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                .section("date", "time").optional()
                .match("^Schlusstag\\/\\-Zeit (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Valutadatum 25. Juni 2021
                .section("date").optional()
                .match("^Valutadatum (?<date>\\d+\\. .* \\d{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Ausmachender Betrag: €2.083,94
                .section("amount").optional()
                .match("Ausmachender Betrag: (\\W{1})(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ausmachender Betrag 1.340,64- EUR
                .section("amount", "currency").optional()
                .match("Ausmachender Betrag (?<amount>[.,\\d]+)([-])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ausmachender Betrag EUR 2.070,00
                .section("amount", "currency").optional()
                .match("Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
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
                .match("^Produktbezeichnung - (?<name>.*)")
                .match("^Internationale Wertpapierkennnummer (?<isin>.*)")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Anzahl/Nominale 30,00
                .section("shares")
                .match("^Anzahl\\/Nominale (?<shares>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })

                // Valutadatum 15. März 2021
                .section("date")
                .match("^Valutadatum (?<date>\\d+. .* \\d{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag EUR 12,15
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Dividende - Vierteljährlich
                // Dividende - Interim
                .section("note").optional()
                .match("^Dividende - (?<note>(Vierteljährlich|Jährlich|Monatlich|Interim))$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addDepositAccountTransaction()
    {
        DocumentType type = new DocumentType("(Sutor fairriester 2.0|Ums.tze) .*");
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
        DocumentType type = new DocumentType("(Sutor fairriester 2.0|Ums.tze) .*");
        this.addDocumentTyp(type);

        Block block = new Block(".* (Kauf|Verkauf|Geb.hrentilgung) .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name").match("^.* (Kauf"
                                        + "|Verkauf"
                                        + "|Geb.hrentilgung)"
                                        + " (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        /***
                         * Sutor always provides the amount in EUR,
                         * column "Betrag in EUR"
                         */

                        .oneOf( // check for old format, if amount is negative,
                                        // we BUY
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for old format - if amount is
                                        // positive (no sign), we SELL
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }),
                                        // check for BUY with new format
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for SELL with new format
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setType(PortfolioTransaction.Type.SELL);
                                                        }))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addRemoveAccountTransaction()
    {
        DocumentType type = new DocumentType("(Sutor fairriester 2.0|Ums.tze) .*");
        this.addDocumentTyp(type);

        Block block = new Block(".* (.bertrag) .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            entry.getAccountTransaction().setType(AccountTransaction.Type.TRANSFER_IN);
                            return entry;
                        })

                        .section("name").match("^.* (.bertrag) (?<name>[^,]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match(REGEX_DATE).assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        /***
                         * Sutor always provides the amount in EUR,
                         * column "Betrag in EUR"
                         */

                        .oneOf(
                                        // check for DELIVERY_INBOUND + TRANSFER_IN with new format
                                        section -> section.attributes("amount", "shares", "sign") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // check for DELIVERY_OUTBOUND + TRANSFER_OUT with new format
                                        section -> section.attributes("amount", "shares") //
                                                        .match(REGEX_AMOUNT_AND_SHARES_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                                                            t.getAccountTransaction().setType(AccountTransaction.Type.TRANSFER_OUT);
                                                        }))

                                .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeePaymentAccountTransaction()
    {
        DocumentType type = new DocumentType("(Sutor fairriester 2.0|Ums.tze) .*");
        this.addDocumentTyp(type);

        Block block = new Block(
                        ".* (Verwaltungsgebühr/Vertriebskosten"
                        + "|anteil.Verwaltgeb.hr/Vertriebskosten"
                        + "|Kontof.hrungs-u.Depotgeb.hren"
                        + "|Geb.hr anteilige Depot- u. Verwaltgeb.hr"
                        + "|Geb.hr anteilige Kontof.hrungsgebühr"
                        + "|Umbuchung Geld).*");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").match(REGEX_DATE).assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        /***
                         * Sutor always provides the amount in EUR,
                         * column "Betrag in EUR"
                         */

                        .oneOf( // check for old and new format
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                                        }),
                                        section -> section.attributes("amount") //
                                                        .match(REGEX_AMOUNT_NEW_FORMAT).assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
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
                .match("Kapitalertragssteuer: (\\W{1})(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragssteuer EUR 1,75
                .section("currency", "tax").optional()
                .match("Kapitalertragssteuer (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag: €0,04
                .section("tax").optional()
                .match("Solidarit.tszuschlag: (\\W{1})(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag EUR 0,09
                .section("currency", "tax").optional()
                .match("Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer: €1,00
                .section("tax").optional()
                .match("Kirchensteuer: (\\W{1})(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer EUR 1,00
                .section("currency", "tax").optional()
                .match("Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltende Quellensteuer EUR 2,14
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltende Quellensteuer (?<currency>\\w{3}) (?<withHoldingTax>[.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer EUR 2,14
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer (?<currency>\\w{3}) (?<creditableWithHoldingTax>[.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
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
                .match(".* Tradinggebühren: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)[-]?")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
