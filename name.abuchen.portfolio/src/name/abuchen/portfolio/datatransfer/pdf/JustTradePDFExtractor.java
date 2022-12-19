package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class JustTradePDFExtractor extends AbstractPDFExtractor
{
    /***
     * Information: 
     * Sutor always provides the amount in EUR, column "Betrag in EUR"
     */

    public JustTradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE"); //$NON-NLS-1$
        addBankIdentifier("Sutor"); //$NON-NLS-1$
        addBankIdentifier("SUTOR BANK"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sutor Bank / justTRADE"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Transaktionsart:|Wertpapier Abrechnung) (Kauf|Verkauf)|F.lligkeit\\/Verfall)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(F.lligkeit\\/Verfall|Wertpapier Abrechnung|WERTPAPIERABRECHNUNG).*$", "^Ausmachender Betrag.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // if type is "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Transaktionsart: |Wertpapier Abrechnung )?(?<type>(Kauf|Verkauf|F.lligkeit\\/Verfall))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Fälligkeit/Verfall"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Produktbezeichnung - Vanguard FTSE All-World U.ETF Re
                // Internationale Wertpapierkennnummer (ISIN): IE00BK5BQT80
                // Währung: EUR
                .section("name", "isin", "currency").optional()
                .match("^Produktbezeichnung \\- (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>[\\w]{12})$")
                .match("^W.hrung: (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // REGISTERED SHS USD (DIST) O.N.
                // Ausführungskurs 25,295 EUR
                .section("name", "isin", "wkn", "name1", "currency").optional()
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Produktbezeichnung - Discount Zertifikat auf Münchener Rückversicherungs AG:
                // Internationale Wertpapierkennnummer DE000VP63TQ3
                // Rückzahlungskurs EUR 230,000000
                .section("name", "isin", "currency").optional()
                .match("^Produktbezeichnung \\- (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer (?<isin>[\\w]{12})$")
                .match("^R.ckzahlungskurs (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück/Nominale: 29,00
                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // Anzahl/Nominale 9,00
                .section("shares")
                .match("^(St.ck|Anzahl)(\\/Nominale(:)?)? (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                .section("time").optional()
                .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})( .*)?$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                                section -> section
                                        .attributes("date")
                                        .match("^(Orderausf.hrung Datum\\/Zeit:|Schlusstag\\/\\-Zeit) (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{1,2} .* [\\d]{4})) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär")));
                                        })
                                ,
                                // Valutadatum 25. Juni 2021
                                section -> section
                                        .attributes("date")
                                        .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date").replace("Mrz", "Mär"))))
                        )

                .oneOf(
                                // Ausmachender Betrag: €2.083,94
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Ausmachender Betrag: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            if (v.get("currency").equals("€"))
                                                v.put("currency", CurrencyUnit.EUR);
                                            else
                                                v.put("currency", CurrencyUnit.USD);
                                                
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Ausmachender Betrag 1.340,64- EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Ausmachender Betrag EUR 2.070,00
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende [^pro]).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Produktbezeichnung - Kellogg Co.:
                // Internationale Wertpapierkennnummer US4878361082
                .section("name", "isin")
                .match("^Produktbezeichnung \\- (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer (?<isin>.*)$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Anzahl/Nominale 30,00
                .section("shares")
                .match("^Anzahl\\/Nominale (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valutadatum 15. März 2021
                .section("date")
                .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date").replace("Mrz", "Mär"))))

                // Ausmachender Betrag EUR 12,15
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Dividende - Vierteljährlich
                // Dividende - Interim
                .section("note").optional()
                .match("^Dividende \\- (?<note>(Viertelj.hrlich|J.hrlich|Monatlich|Interim))$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("(Sutor fairriester 2\\.0|Ums.tze) .*");
        this.addDocumentTyp(type);

        Block buySellBlock = new Block("^.* (Kauf|Verkauf|Geb.hrentilgung) .*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                .subject(() -> {
                    BuySellEntry entry = new BuySellEntry();
                    entry.setType(PortfolioTransaction.Type.BUY);
                    return entry;
                })

                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^.* (?<type>(Kauf|Verkauf|Geb.hrentilgung)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Gebührentilgung"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                            section -> section
                                .attributes("date", "amount", "name", "shares", "time", "isin")
                                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                + "Storno Kauf Storno Kauf "
                                                + "(?<name>.*) "
                                                + "(\\-)?(?<shares>[\\.,\\d]+) "
                                                + "[\\.,\\d]+ "
                                                + "(\\-)?(?<amount>[\\.,\\d]+)$")
                                .match("^(?<time>[\\d]{2}:[\\d]{2}) "
                                                + ".* "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\.,\\d]+ ([\\w]{2}\\p{Sc}|[\\w]{3})$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date"), v.get("time")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(Messages.MsgErrorOrderCancellationUnsupported);
                                    })
                            ,
                            // Storno ohne Wechselkurs
                            section -> section
                                    .attributes("date", "amount", "name", "shares", "time", "isin")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(Kauf Kauf|Verkauf Verkauf) "
                                                    + "(?<name>.*) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+ "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+)$")
                                    .match("^(?<time>[\\d]{2}:[\\d]{2}) "
                                                    + ".* "
                                                    + "(?<isin>[\\w]{12}) "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));

                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                            ,
                            section -> section
                                    .attributes("date", "amount", "name", "shares", "time", "isin")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(Kauf Kauf|Verkauf Verkauf) "
                                                    + "(?<name>.*) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+)$")
                                    .match("^(?<time>[\\d]{2}:[\\d]{2}) "
                                                    + ".* "
                                                    + "(?<isin>[\\w]{12}) "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setSecurity(getOrCreateSecurity(v));

                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                            ,
                            section -> section
                                    .attributes("date", "amount", "name", "shares")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+) "
                                                    + "(Kauf|Verkauf) "
                                                    + "(?<name>.*) "
                                                    + "[\\.,\\d]+ "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "name", "shares")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+) "
                                                    + "(Kauf|Verkauf) "
                                                    + "(?<name>.*) "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "name", "shares")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+) "
                                                    + "(Kauf|Verkauf) "
                                                    + "(?<name>.*) "
                                                    + "[\\.,\\d]+ "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "note", "name", "shares")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+) "
                                                    + "(?<note>Geb.hrentilgung) (?<name>.*) "
                                                    + "[\\.,\\d]+ "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "note", "name", "shares")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(\\-)?(?<amount>[\\.,\\d]+) "
                                                    + "(?<note>Geb.hrentilgung) (?<name>.*) "
                                                    + "[\\.,\\d]+ "
                                                    + "([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                                    + "(\\-)?(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                        )

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        if (t.getPortfolioTransaction().getNote() == null || !t.getPortfolioTransaction().getNote().equals(Messages.MsgErrorOrderCancellationUnsupported))
                            return new BuySellEntryItem(t);
                        else
                            return new NonImportableItem(Messages.MsgErrorOrderCancellationUnsupported);
                    return null;
                }));

        Block depositBlock = new Block("^.*([^staatlichen] Zulage|(Einzahlung )?automatischer Lastschrifteinzug).*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DEPOSIT);
                    return entry;
                })

                .oneOf(
                            section -> section
                                    .attributes("date", "amount", "note")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "(?<note>(Zulage [\\d]{4}|automatischer Lastschrifteinzug))"
                                                    + "(: .*)?$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "note")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(?<note>Einzahlung automatischer Lastschrifteinzug) "
                                                    + "\\- (?<amount>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                        )

                .wrap(TransactionItem::new));

        Block feeBlock = new Block(
                        ".* (Verwaltungsgeb.hr\\/Vertriebskosten"
                        + "|anteil\\.Verwaltgeb.hr\\/Vertriebskosten"
                        + "|Kontof.hrungs\\-u\\.Depotgeb.hren"
                        + "|Geb.hr anteilige Depot\\- u. Verwaltgeb.hr"
                        + "|Geb.hr anteilige Kontof.hrungsgeb.hr"
                        + "|Depot\\- u\\. Verwaltgeb.hr"
                        + "|Kontof.hrungsgeb.hr"
                        + "|Umbuchung Geld).*");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                .oneOf(
                            section -> section
                                    .attributes("date", "amount", "note")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "\\-(?<amount>[\\.,\\d]+) "
                                                    + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?"
                                                    + "(?<note>(Verwaltungsgeb.hr\\/Vertriebskosten"
                                                    + "|Verwaltgeb.hr\\/Vertriebskosten"
                                                    + "|Kontof.hrungs\\-u\\.Depotgeb.hren"
                                                    + "|Depot\\- u\\. Verwaltgeb.hr"
                                                    + "|Kontof.hrungsgeb.hr))"
                                                    + ".*$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "note")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?"
                                                    + "(?<note>(Verwaltungsgeb.hr\\/Vertriebskosten"
                                                    + "|Kontof.hrungs\\-u\\.Depotgeb.hren"
                                                    + "|Verwaltgeb.hr\\/Vertriebskosten"
                                                    + "|Kontof.hrungs\\-u.Depotgeb.hren"
                                                    + "|Depot\\- u\\. Verwaltgeb.hr"
                                                    + "|Kontof.hrungsgeb.hr)).* "
                                                    + "\\- \\-(?<amount>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "note")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "Umbuchung "
                                                    + "(?<note>Umbuchung Geld) "
                                                    + "\\- \\-(?<amount>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "stornoDate", "note1", "note2", "amount")
                                    .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(?<stornoDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + "(?<note1>Storno) Storno "
                                                    + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?"
                                                    + "(?<note2>(Verwaltungsgeb.hr\\/Vertriebskosten"
                                                    + "|Kontof.hrungs\\-u\\.Depotgeb.hren"
                                                    + "|Verwaltgeb.hr\\/Vertriebskosten"
                                                    + "|Kontof.hrungs\\-u.Depotgeb.hren"
                                                    + "|Depot\\- u\\. Verwaltgeb.hr"
                                                    + "|Kontof.hrungsgeb.hr)).* "
                                                    + "\\- (?<amount>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        t.setType(AccountTransaction.Type.FEES_REFUND);

                                        t.setDateTime(asDate(v.get("date")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        t.setNote(v.get("note1") + " " + v.get("note2") + " vom " + v.get("stornoDate"));
                                    })
                        )

                .wrap(TransactionItem::new));

        Block deliveryInOutbondblock = new Block("^.* .bertrag .*$");
        type.addBlock(deliveryInOutbondblock);
        deliveryInOutbondblock.set(new Transaction<PortfolioTransaction>()

                .subject(() -> {
                    PortfolioTransaction transaction = new PortfolioTransaction();
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    return transaction;
                })

                .oneOf(
                            section -> section
                                    .attributes("date", "note", "name", "shares", "isin")
                                    .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                    + ".bertrag "
                                                    + "(?<note>.bertrag) "
                                                    + "(?<name>.*) "
                                                    + "\\-(?<shares>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+$")
                                    .match("^(?<isin>[\\w]{12}) ([\\w]{2}\\p{Sc}|[\\w]{3})$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDateTime(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setAmount(0L);
                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));

                                        t.setNote(v.get("note"));
                                    })
                        )

                .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragssteuer: €0,73
                .section("currency", "tax").optional()
                .match("Kapitalertragssteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> {
                    if (v.get("currency").equals("€"))
                        v.put("currency", CurrencyUnit.EUR);
                    else
                        v.put("currency", CurrencyUnit.USD);

                    processTaxEntries(t, v, type);
                })

                // Kapitalertragssteuer EUR 1,75
                .section("currency", "tax").optional()
                .match("Kapitalertragssteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag: €0,04
                .section("currency", "tax").optional()
                .match("Solidarit.tszuschlag: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> {
                    if (v.get("currency").equals("€"))
                        v.put("currency", CurrencyUnit.EUR);
                    else
                        v.put("currency", CurrencyUnit.USD);

                    processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag EUR 0,09
                .section("currency", "tax").optional()
                .match("Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer: €1,00
                .section("currency", "tax").optional()
                .match("Kirchensteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> {
                    if (v.get("currency").equals("€"))
                        v.put("currency", CurrencyUnit.EUR);
                    else
                        v.put("currency", CurrencyUnit.USD);

                    processTaxEntries(t, v, type);
                })

                // Kirchensteuer EUR 1,00
                .section("currency", "tax").optional()
                .match("Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltende Quellensteuer EUR 2,14
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltende Quellensteuer (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer EUR 2,14
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                .section("currency", "fee").optional()
                .match(".* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* Tradinggebühren: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                .section("currency", "fee").optional()
                .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
