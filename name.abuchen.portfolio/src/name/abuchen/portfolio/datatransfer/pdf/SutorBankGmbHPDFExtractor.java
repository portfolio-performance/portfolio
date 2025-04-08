package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
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
import name.abuchen.portfolio.money.Money;

/**
 * @formatter:off
 * @implNote JustTrade is a service provided by Sutor Bank,
 *           which it has outsourced to JT Technologies GmbH.
 *
 * @implSpec The account statement transactions are reported in EUR.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SutorBankGmbHPDFExtractor extends AbstractPDFExtractor
{
    public SutorBankGmbHPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("justTRADE");
        addBankIdentifier("Sutor");
        addBankIdentifier("SUTOR BANK");

        addBuySellTransaction();
        addBuySellCryptoTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sutor Bank GmbH / justTRADE";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Transaktionsart:|Wertpapier Abrechnung) (Kauf|Verkauf)|F.lligkeit\\/Verfall)", "ABRECHNUNG KRYPTOHANDEL");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(F.lligkeit\\/Verfall|Wertpapier Abrechnung|WERTPAPIERABRECHNUNG).*$", "^Ausmachender Betrag.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // If type is "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(Transaktionsart: |Wertpapier Abrechnung )?(?<type>(Kauf|Verkauf|F.lligkeit\\/Verfall))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Fälligkeit/Verfall".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Produktbezeichnung - Vanguard FTSE All-World U.ETF Re
                                        // Internationale Wertpapierkennnummer (ISIN): IE00BK5BQT80
                                        // Währung: EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Produktbezeichnung \\- (?<name>.*)$") //
                                                        .match("^Internationale Wertpapierkennnummer \\(ISIN\\): (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^W.hrung: (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (v.get("name").endsWith(":"))
                                                                v.put("name", v.get("name").substring(0, v.get("name").length() - 1));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                                        // REGISTERED SHS USD (DIST) O.N.
                                        // Ausführungskurs 25,295 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "name1", "currency") //
                                                        .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\)$") //
                                                        .match("^(?<name1>.*)$") //
                                                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Produktbezeichnung - Discount Zertifikat auf Münchener Rückversicherungs AG:
                                        // Internationale Wertpapierkennnummer DE000VP63TQ3
                                        // Rückzahlungskurs EUR 230,000000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Produktbezeichnung \\- (?<name>.*)$") //
                                                        .match("^Internationale Wertpapierkennnummer (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^R.ckzahlungskurs (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (v.get("name").endsWith(":"))
                                                                v.put("name", v.get("name").substring(0, v.get("name").length() - 1));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Stück/Nominale: 29,00
                        // Stück 53 ISHSII-DEV.MKTS PROP.YLD U.ETF IE00B1FZS350 (A0LEW8)
                        // Anzahl/Nominale 9,00
                        // @formatter:on
                        .section("shares") //
                        .match("^(St.ck|Anzahl)(\\/Nominale(:)?)? (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                        // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                        // @formatter:on
                        .section("time").optional() //
                        .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // Orderausführung Datum/Zeit: 31 Jul 2020 21:00:15
                                        // Schlusstag/-Zeit 02.01.2020 10:49:34 Auftragserteilung/ -ort sonstige
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Orderausf.hrung Datum\\/Zeit:|Schlusstag\\/\\-Zeit) (?<date>([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{1,2} .* [\\d]{4})).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // Orderausführung Datum/Zeit: 13. 06. 2023 11:10:52
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Orderausf.hrung Datum\\/Zeit:|Schlusstag\\/\\-Zeit) (?<date>[\\d]{2}\\.([\\s]+)?[\\d]{2}\\.([\\s]+)?[\\d]{4}).*$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(stripBlanks(v.get("date")), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(stripBlanks(v.get("date"))));
                                                        }),
                                        // @formatter:off
                                        // Valutadatum 25. Juni 2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausmachender Betrag: €2.083,94
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Ausmachender Betrag: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Ausmachender Betrag 1.340,64- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Ausmachender Betrag EUR 2.070,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Referenz 6709
                        // Handelsreferenz: 547l78QW0n57EA12598N8822
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Referenz|Handelsreferenz:) .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

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

        Block firstRelevantLine = new Block("^ABRECHNUNG KRYPTOHANDEL$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // If type is "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Transaktionsart: (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Produktbezeichnung - Bitcoin
                        // Kennung: BTC
                        // Währung: EUR
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency") //
                        .match("^Produktbezeichnung \\- (?<name>.*)$") //
                        .match("^Kennung: (?<tickerSymbol>[A-Z]*)$") //
                        .match("^W.hrung: (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateCryptoCurrency(v)))

                        // @formatter:off
                        // Stück / Nominale: 0,031
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck \\/ Nominale: (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Orderausführung Datum / Zeit: 13. Februar 2021 04:21:59
                        // @formatter:on
                        .section("time").optional() //
                        .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Orderausführung Datum / Zeit: 13. Februar 2021 04:21:59
                        // @formatter:on
                        .section("date") //
                        .match("^Orderausf.hrung Datum \\/ Zeit: (?<date>[\\d]{1,2}\\. .* [\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Ausmachender Betrag: €1.220,89
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Handelsreferenz: 61ccf74741c06f001232ead6
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Handelsreferenz: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividende [^pro]).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Produktbezeichnung - Kellogg Co.:
                        // Internationale Wertpapierkennnummer US4878361082
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Produktbezeichnung \\- (?<name>.*)$") //
                        .match("^Internationale Wertpapierkennnummer (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> {
                            if (v.get("name").endsWith(":"))
                                v.put("name", v.get("name").substring(0, v.get("name").length() - 1));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Anzahl/Nominale 30,00
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl\\/Nominale (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valutadatum 15. März 2021
                        // @formatter:on
                        .section("date") //
                        .match("^Valutadatum (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag EUR 12,15
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Dividende - Vierteljährlich
                        // Dividende - Interim
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Dividende \\- (?<note>(Viertelj.hrlich|J.hrlich|Monatlich|Interim))$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Abrechnung Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Abrechnung Vorabpauschale.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Name iShares MSCI World (Acc)
                        // ISIN IE00B4L5Y983
                        // Monat Anzahl Stücke Vorabpauschale in EUR Vorabpauschale in EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Name (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^.*Vorabpauschale in (?<currency>[\\w]{3})$$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Jahresnettobestand 150,0000 1,24 185,55
                        // @formatter:on
                        .section("shares") //
                        .match("^Jahresnettobestand (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Tag des Zuflusses 02 Januar 2024
                        // @formatter:on
                        .section("date") //
                        .match("^Tag des Zuflusses (?<date>[\\d]{1,2} .* [\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Aufgrund nachstehender Abrechnung buchen wir zu Ihren Lasten €34,25
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Aufgrund nachstehender Abrechnung buchen wir zu Ihren Lasten (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("(Sutor fairriester 2\\.0|Ums.tze) .*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> buySellBlock = new Transaction<>();

        Block firstRelevantLine = new Block("^.* (Kauf|Verkauf|Geb.hrentilgung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(buySellBlock);

        buySellBlock //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.* (?<type>(Kauf|Verkauf|Geb.hrentilgung)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Gebührentilgung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 23.06.2020 22.06.2020 Storno Kauf Storno Kauf Xtrackers MSCI World Min Vol ETF -3,9988 1,0000 130,57
                                        // 13:31 OTC IE00BL25JN58 32,6523 US$
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares", "time", "isin") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "Storno Kauf Storno Kauf " //
                                                                        + "(?<name>.*) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+)$") //
                                                        .match("^(?<time>[\\d]{2}:[\\d]{2}) " //
                                                                        + ".* " //
                                                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) " //
                                                                        + "[\\.,\\d]+ ([\\w]{2}\\p{Sc}|[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);
                                                        }),
                                        // @formatter:off
                                        // 25.03.2024 22.03.2024 Verkauf Verkauf X-trackers MSCI USA Index -8,2731 1,0823 1.139,97 1.173,40 -31,69
                                        // 10:41 Tradegate LU0274210672 153,5065 US$ -1,74
                                        //
                                        // 25.03.2024 22.03.2024 Verkauf Verkauf X-trackers MSCI EMU INDEX dis -3,3986 172,65 177,47 -4,57
                                        // 10:47 Tradegate LU0846194776 52,2200 EUR -0,25
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "shares", "amount", "tax1", "time", "isin", "tax2") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                                        + "Verkauf Verkauf " + "(?<name>.*) "
                                                                        + "\\-(?<shares>[\\.,\\d]+) "
                                                                        + "([\\.,\\d]+ )?"
                                                                        + "(?<amount>[\\.,\\d]+) "
                                                                        + "[\\.,\\d]+ "
                                                                        + "\\-(?<tax1>[\\.,\\d]+)$") //
                                                        .match("^(?<time>[\\d]{2}:[\\d]{2}) " + ".* "
                                                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                                        + "[\\.,\\d]+ ([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                                                        + "\\-(?<tax2>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            Money tax1 = Money.of(t.getPortfolioTransaction().getCurrencyCode(), asAmount(v.get("tax1")));
                                                            Money tax2 = Money.of(t.getPortfolioTransaction().getCurrencyCode(), asAmount(v.get("tax2")));
                                                            Money tax = tax1.add(tax2);

                                                            checkAndSetTax(tax, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 01.07.2020 01.07.2020 Kauf Kauf iShares Edge MSCI EM Min Vol ETF 1,9784 1,1200 -48,44
                                        // 12:56 OTC IE00B8KGV557 27,4228 US$
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares", "time", "isin") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(Kauf Kauf|Verkauf Verkauf) " //
                                                                        + "(?<name>.*) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+)$") //
                                                        .match("^(?<time>[\\d]{2}:[\\d]{2}) " //
                                                                        + ".* " //
                                                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 01.07.2020 01.07.2020 Kauf Kauf Vanguard EUR Eurozone Gov Bond ETF 26,7524 -719,05
                                        // 12:46 OTC IE00BH04GL39 26,8780 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares", "time", "isin") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(Kauf Kauf|Verkauf Verkauf) " //
                                                                        + "(?<name>.*) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+)$") //
                                                        .match("^(?<time>[\\d]{2}:[\\d]{2}) " //
                                                                        + ".* " //
                                                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 05.07.2019 04.07.2019 -8,35 Kauf iShares Core MSCI Emerging Markets 1,1288 28,50 US$ 0,3308
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                                                        + "(Kauf|Verkauf) " //
                                                                        + "(?<name>.*) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3}) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 04.07.2019 02.07.2019 -75,22 Kauf Dimensional European Value Fund 12,01 EUR 6,2631
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                                                        + "(Kauf|Verkauf) " //
                                                                        + "(?<name>.*) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3}) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 04.07.2019 03.07.2019 -242,09 Kauf Lyxor Core Stoxx Europe 600 acc 158,60 1,5264
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "name", "shares") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                                                        + "(Kauf|Verkauf) " //
                                                                        + "(?<name>.*) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 08.07.2019 05.07.2019 0,98 Gebührentilgung iShares Core MSCI Emerging Markets 1,1260 28,48 US$ -0,0387
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "note", "name", "shares") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                                                        + "(?<note>Geb.hrentilgung) (?<name>.*) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3}) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 09.07.2019 06.07.2019 4,78 Gebührentilgung Lyxor Core Stoxx Europe 600 acc 159,78 EUR -0,0299
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "note", "name", "shares") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                                                        + "(?<note>Geb.hrentilgung) (?<name>.*) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "([\\w]{2}\\p{Sc}|[\\w]{3}) " //
                                                                        + "(\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap((t, ctx) -> {
                            BuySellEntryItem item = new BuySellEntryItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        Transaction<AccountTransaction> dividendsBlock = new Transaction<>();

        firstRelevantLine = new Block("^.* Aussch.ttung Betrag der Aussch.ttung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(dividendsBlock);

        dividendsBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 13.03.2024 07.03.2024 Ausschüttung Betrag der Ausschüttung X-trackers MSCI EMU INDEX dis - 0,64 0,79 -0,14
                        // LU0846194776 EUR -0,01
                        // @formatter:on
                        .section("date", "name", "amount", "tax1", "tax2") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "Aussch.ttung Betrag der Aussch.ttung " //
                                        + "(?<name>.*) " //
                                        + "\\- " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ " //
                                        + "\\-(?<tax1>[\\.,\\d]+)$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) " //
                                        + "([\\w]{2}\\p{Sc}|[\\w]{3}) "
                                        + "\\-(?<tax2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(0L);

                            t.setCurrencyCode("EUR");
                            t.setAmount(asAmount(v.get("amount")));

                            Money tax1 = Money.of(t.getCurrencyCode(), asAmount(v.get("tax1")));
                            Money tax2 = Money.of(t.getCurrencyCode(), asAmount(v.get("tax2")));
                            Money tax = tax1.add(tax2);

                            checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);

        Transaction<AccountTransaction> depositRemovalBlock = new Transaction<>();

        firstRelevantLine = new Block("^.*([^staatlichen] Zulage|Einzahlung |Auszahlung |automatischer Lastschrifteinzug).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(depositRemovalBlock);

        depositRemovalBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 01.02.2019 01.02.2019 160,42 automatischer Lastschrifteinzug
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "note") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(?<amount>[\\.,\\d]+) " //
                                                                        + "(?<note>(Zulage [\\d]{4}|automatischer Lastschrifteinzug))" //
                                                                        + "(: .*)?$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }),
                                        // @formatter:off
                                        // 02.03.2020 02.03.2020 Einzahlung automatischer Lastschrifteinzug - 175,00
                                        // 25.03.2024 25.03.2024 Auszahlung Überweisung bei Kündigung - -2.858,95
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "type", "amount", "note") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) "
                                                                        + "(?<type>(Einzahlung|Auszahlung)) "
                                                                        + "(?<note>.*) \\- "
                                                                        + "(\\-)?(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            if ("Auszahlung".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }))

                        .wrap(TransactionItem::new);

        Transaction<AccountTransaction> taxesBlock = new Transaction<>();

        firstRelevantLine = new Block("^.* Steuerbuchung.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(taxesBlock);

        taxesBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 20.01.2024 20.01.2024 Steuerbuchung KapSt2023Vorabpauschale AIS-Amundi I.S.Stoxx - -0,26
                        // 20.01.2024 20.01.2024 Steuerbuchung KapSt2023Vorabpauschale Amundi Prime Japan - - -0,06
                        // @formatter:on
                        .section("date", "note", "amount") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "Steuerbuchung " //
                                        + ".*[\\d]{4}(?<note>Vorabpauschale .*) "
                                        + "\\- .*" //
                                        + "\\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));

                            t.setCurrencyCode("EUR");
                            t.setAmount(asAmount(v.get("amount")));

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new);

        Transaction<AccountTransaction> feesBlock = new Transaction<>();

        firstRelevantLine = new Block("^.* (Verwaltungsgeb.hr\\/Vertriebskosten" //
                        + "|anteil\\.Verwaltgeb.hr\\/Vertriebskosten" //
                        + "|Kontof.hrungs\\-u\\.Depotgeb.hren" //
                        + "|Geb.hr anteilige Depot\\- u. Verwaltgeb.hr" //
                        + "|Geb.hr anteilige Kontof.hrungsgeb.hr" //
                        + "|Geb.hr Servicegeb.hr" //
                        + "|Verm.gensverwaltungsgeb.hr" //
                        + "|Depot\\- u\\. Verwaltgeb.hr" //
                        + "|Kontof.hrungsgeb.hr" //
                        + "|Umbuchung Geld).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(feesBlock);

        feesBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("date", "amount", "note") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "\\-(?<amount>[\\.,\\d]+) " //
                                                                        + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?" //
                                                                        + "(?<note>(Verwaltungsgeb.hr\\/Vertriebskosten" //
                                                                        + "|Verwaltgeb.hr\\/Vertriebskosten" //
                                                                        + "|Kontof.hrungs\\-u\\.Depotgeb.hren" //
                                                                        + "|Depot\\- u\\. Verwaltgeb.hr" //
                                                                        + "|Kontof.hrungsgeb.hr))" //
                                                                        + ".*$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "amount", "note") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?" //
                                                                        + "(?<note>(Verwaltungsgeb.hr\\/Vertriebskosten" //
                                                                        + "|Kontof.hrungs\\-u\\.Depotgeb.hren" //
                                                                        + "|Servicegeb.hr" //
                                                                        + "|Verwaltgeb.hr\\/Vertriebskosten" //
                                                                        + "|Verm.gensverwaltungsgeb.hr" //
                                                                        + "|Kontof.hrungs\\-u.Depotgeb.hren" //
                                                                        + "|Depot\\- u\\. Verwaltgeb.hr" //
                                                                        + "|Kontof.hrungsgeb.hr)).* " //
                                                                        + "\\- \\-(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "amount", "note") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "Umbuchung " //
                                                                        + "(?<note>Umbuchung Geld) " //
                                                                        + "\\- \\-(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note"));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "stornoDate", "note1", "note2", "amount") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(?<stornoDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + "(?<note1>Storno) Storno " //
                                                                        + "(anteil\\.|Geb.hr (anteil\\.|anteilige )?|Geb.hren (anteil\\.|anteilige )?)?" //
                                                                        + "(?<note2>(Verwaltungsgeb.hr\\/Vertriebskosten" //
                                                                        + "|Kontof.hrungs\\-u\\.Depotgeb.hren" //
                                                                        + "|Servicegeb.hr" //
                                                                        + "|Verwaltgeb.hr\\/Vertriebskosten" //
                                                                        + "|Verm.gensverwaltungsgeb.hr" //
                                                                        + "|Kontof.hrungs\\-u.Depotgeb.hren" //
                                                                        + "|Depot\\- u\\. Verwaltgeb.hr" //
                                                                        + "|Kontof.hrungsgeb.hr)).* " //
                                                                        + "\\- (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.FEES_REFUND);

                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setCurrencyCode("EUR");
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            t.setNote(v.get("note1") + " " + v.get("note2") + " vom " + v.get("stornoDate"));
                                                        }))

                        .wrap(TransactionItem::new);

        Transaction<PortfolioTransaction> deliveryOutbound = new Transaction<>();

        firstRelevantLine = new Block("^.* .bertrag .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(deliveryOutbound);

        deliveryOutbound //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("date", "note", "name", "shares", "isin") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                                                        + ".bertrag " //
                                                                        + "(?<note>.bertrag) " //
                                                                        + "(?<name>.*) " //
                                                                        + "\\-(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\w]{2}\\p{Sc}|[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setAmount(0L);
                                                            t.setCurrencyCode("EUR");

                                                            t.setNote(v.get("note"));

                                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                                                        }))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void addNonImportableTransaction()
    {
        final DocumentType type = new DocumentType("(Kapitalver.nderung \\- Bezugsrechtsemission" //
                        + "|.bernahme \\- Vergleichsplan)");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Kapitalver.nderung \\- Bezugsrechtsemission"
                        + "|.bernahme - Vergleichsplan)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Produktbezeichnung - OXFORD SQUARE CAP. DL-,01
                        // Internationale Wertpapierkennnummer US69181V1070
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Produktbezeichnung \\- (?<name>.*)$") //
                        .match("^Internationale Wertpapierkennnummer (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> {
                            if (v.get("name").endsWith(":"))
                                v.put("name", v.get("name").substring(0, v.get("name").length() - 1));

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setAmount(0L);

                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                        })

                        // @formatter:off
                        // Anzahl/Nominale 500,00
                        // @formatter:on
                        .section("shares") //
                        .match("^Anzahl\\/Nominale (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ex Datum - Tag 25. Mai 2023
                        // @formatter:on
                        .section("date") //
                        .match("^Ex Datum \\- Tag (?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Verhältnis Neu/ Alt 1 : 3,00
                        // Verhältnis Neu/ Alt 1,00 : 1,00
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Verh.ltnis Neu\\/.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragssteuer: €0,73
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Kapitalertragssteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragssteuer EUR 1,75
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Kapitalertragssteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag: €0,04
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Solidarit.tszuschlag: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag EUR 0,09
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer: €1,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Kirchensteuer: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR 1,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Einbehaltende Quellensteuer EUR 2,14
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Einbehaltende Quellensteuer (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Anrechenbare Quellensteuer EUR 2,14
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Anrechenbare Quellensteuer (?<currency>[\\w]{3}) (?<creditableWithHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Orderprovision: €12,51
                        // Rabatt (inkl. Rückvergütung): - €11,51
                        // @formatter:on
                        .section("currency", "fee", "discountCurrency", "discount").optional() //
                        .match("Orderprovision: (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)") //
                        .match("^Rabatt \\(inkl\\. R.ckverg.tung\\): \\- (?<discountCurrency>\\p{Sc})(?<discount>[\\.,\\d]+)$") //
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

        transaction //

                        // @formatter:off
                        // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match(".* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match(".* Tradinggebühren: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match(".* WP-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Orderprovision: €12,51
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("Orderprovision: (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noProvision"))
                                processFeeEntries(t, v, type);
                        });
    }
}
