package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

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

        addBankIdentifier("justTRADE");
        addBankIdentifier("Sutor");
        addBankIdentifier("SUTOR BANK");

        addBuySellTransaction();
        addBuySellCryptoTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sutor Bank / justTRADE";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Transaktionsart:|Wertpapier Abrechnung) (Kauf|Verkauf)|F.lligkeit\\/Verfall)", "ABRECHNUNG KRYPTOHANDEL");
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
                // If type is "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Transaktionsart: |Wertpapier Abrechnung )?(?<type>(Kauf|Verkauf|F.lligkeit\\/Verfall))$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")) || "Fälligkeit/Verfall".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Produktbezeichnung - Vanguard FTSE All-World U.ETF Re
                                // Internationale Wertpapierkennnummer (ISIN): IE00BK5BQT80
                                // Währung: EUR
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^Produktbezeichnung \\- (?<name>.*)$")
                                        .match("^Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^W.hrung: (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (v.get("name").endsWith(":"))
                                                v.put("name", v.get("name").substring(0, v.get("name").length()-1));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                                // REGISTERED SHS USD (DIST) O.N.
                                // Ausführungskurs 25,295 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "wkn", "name1", "currency")
                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // Produktbezeichnung - Discount Zertifikat auf Münchener Rückversicherungs AG:
                                // Internationale Wertpapierkennnummer DE000VP63TQ3
                                // Rückzahlungskurs EUR 230,000000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^Produktbezeichnung \\- (?<name>.*)$")
                                        .match("^Internationale Wertpapierkennnummer (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^R.ckzahlungskurs (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            if (v.get("name").endsWith(":"))
                                                v.put("name", v.get("name").substring(0, v.get("name").length() - 1));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // Stück/Nominale: 29,00
                // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                // Anzahl/Nominale 9,00
                // @formatter:on
                .section("shares")
                .match("^(St.ck|Anzahl)(\\/Nominale(:)?)? (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                // @formatter:on
                .section("time").optional()
                .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // @formatter:off
                                // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                                // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^(Orderausf.hrung Datum\\/Zeit:|Schlusstag\\/\\-Zeit) (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{1,2} .* [\\d]{4})).*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär")));
                                        })
                                ,
                                // @formatter:off
                                // Orderausführung Datum/Zeit: 13. 06. 2023 11:10:52
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^(Orderausf.hrung Datum\\/Zeit:|Schlusstag\\/\\-Zeit) (?<date>[\\d]{2}\\.([\\s]+)?[\\d]{2}\\.([\\s]+)?[\\d]{4}).*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(stripBlanks(v.get("date")), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(stripBlanks(v.get("date"))));
                                        })
                                ,
                                // @formatter:off
                                // Valutadatum 25. Juni 2021
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date").replace("Mrz", "Mär"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Ausmachender Betrag: €2.083,94
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Ausmachender Betrag: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Ausmachender Betrag 1.340,64- EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Ausmachender Betrag EUR 2.070,00
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .wrap(t -> {
                    // If we have multiple entries in the document, with
                    // fee and fee refunds, then the "noProvision" flag
                    // must be removed.
                    type.getCurrentContext().remove("noProvision");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellCryptoTransaction()
    {
        DocumentType type = new DocumentType("ABRECHNUNG KRYPTOHANDEL");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^ABRECHNUNG KRYPTOHANDEL$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // If type is "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Transaktionsart: (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // Produktbezeichnung - Bitcoin
                // Kennung: BTC
                // Währung: EUR
                // @formatter:on
                .section("name", "tickerSymbol", "currency")
                .match("^Produktbezeichnung \\- (?<name>.*)$")
                .match("^Kennung: (?<tickerSymbol>[A-Z]*)$")
                .match("^W.hrung: (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateCryptoCurrency(v)))

                // @formatter:off
                // Stück / Nominale: 0,031
                // @formatter:on
                .section("shares")
                .match("^St.ck \\/ Nominale: (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Orderausführung Datum / Zeit: 13. Februar 2021 04:21:59
                // @formatter:on
                .section("time").optional()
                .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // @formatter:off
                                // Orderausführung Datum / Zeit: 13. Februar 2021 04:21:59
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Orderausf.hrung Datum \\/ Zeit: (?<date>[\\d]{1,2}\\. .* [\\d]{4}).*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date").replace("Mrz", "Mär")));
                                        })
                        )

                .oneOf(
                                // @formatter:off
                                // Ausmachender Betrag: €1.220,89
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Ausmachender Betrag: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // @formatter:off
                // Handelsreferenz: 61ccf74741c06f001232ead6
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Handelsreferenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

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
                // @formatter:off
                // Produktbezeichnung - Kellogg Co.:
                // Internationale Wertpapierkennnummer US4878361082
                // @formatter:on
                .section("name", "isin")
                .match("^Produktbezeichnung \\- (?<name>.*)$")
                .match("^Internationale Wertpapierkennnummer (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> {
                    if (v.get("name").endsWith(":"))
                        v.put("name", v.get("name").substring(0, v.get("name").length()-1));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Anzahl/Nominale 30,00
                // @formatter:on
                .section("shares")
                .match("^Anzahl\\/Nominale (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valutadatum 15. März 2021
                // @formatter:on
                .section("date")
                .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date").replace("Mrz", "Mär"))))

                // @formatter:off
                // Ausmachender Betrag EUR 12,15
                // @formatter:on
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Dividende - Vierteljährlich
                // Dividende - Interim
                // @formatter:on
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
                    if ("Verkauf".equals(v.get("type")) || "Gebührentilgung".equals(v.get("type")))
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
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\.,\\d]+ ([\\w]{2}\\p{Sc}|[\\w]{3})$")
                                    .assign((t, v) -> {
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(v.get("date"), v.get("time")));
                                        t.setShares(asShares(v.get("shares")));

                                        t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                        t.setAmount(asAmount(v.get("amount")));

                                        v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);
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
                                                    + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
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
                                                    + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
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

                .wrap((t, ctx) -> {
                    BuySellEntryItem item = new BuySellEntryItem(t);

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                    if (ctx.getString(FAILURE) != null)
                        item.setFailureMessage(ctx.getString(FAILURE));

                    return item;
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
                                    .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\w]{2}\\p{Sc}|[\\w]{3})$")
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
                // @formatter:off
                // Kapitalertragssteuer: €0,73
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Kapitalertragssteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kapitalertragssteuer EUR 1,75
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Kapitalertragssteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Solidaritätszuschlag: €0,04
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Solidarit.tszuschlag: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Solidaritätszuschlag EUR 0,09
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kirchensteuer: €1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Kirchensteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kirchensteuer EUR 1,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Einbehaltende Quellensteuer EUR 2,14
                // @formatter:on
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
                // @formatter:off
                // Orderprovision: €12,51
                // Rabatt (inkl. Rückvergütung): - €11,51
                // @formatter:on
                .section("currency", "fee", "discountCurrency", "discount").optional()
                .match("Orderprovision: (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)")
                .match("^Rabatt \\(inkl\\. R.ckverg.tung\\): \\- (?<discountCurrency>\\p{Sc})(?<discount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (fee.subtract(discount).isPositive())
                    {
                        fee = fee.subtract(discount);
                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }

                    type.getCurrentContext().putBoolean("noProvision", true);
                });

        transaction
                // @formatter:off
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                // @formatter:on
                .section("currency", "fee").optional()
                .match(".* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                // @formatter:on
                .section("currency", "fee").optional()
                .match(".* Tradinggebühren: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                // @formatter:on
                .section("currency", "fee").optional()
                .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Orderprovision: €12,51
                // @formatter:on
                .section("currency", "fee").optional()
                .match("Orderprovision: (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean("noProvision"))
                        processFeeEntries(t, v, type);
                });
    }
}
