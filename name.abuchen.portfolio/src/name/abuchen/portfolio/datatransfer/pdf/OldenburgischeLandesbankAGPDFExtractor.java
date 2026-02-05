package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
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

@SuppressWarnings("nls")
public class OldenburgischeLandesbankAGPDFExtractor extends AbstractPDFExtractor
{
    public OldenburgischeLandesbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Oldenburgische Landesbank AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addAccountStatementTransaction();
        addNonImportableTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Oldenburgische Landesbank AG";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("WERTPAPIERABRECHNUNG");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*Depotnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Verkauf))( \\-|:).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Stornierung Kauf - VanEck Sust.World EQ.UC.ETF Aandelen oop naam o.N.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Stornierung) (Kauf|Verkauf)( \\-|:) .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        // @formatter:off
                        // Kauf - iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile
                        // DE000A0H0785 (A0H078) 0,033037 10,0350 EUR 1,47 EUR
                        //
                        // Kauf: AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N.
                        // LU1861134382 (A2JSDA) 9,727757 93,4642 EUR 982,07 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^(Stornierung )?(Kauf|Verkauf)( \\-|:) (?<name>.*)$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\) [\\.,\\d]+ [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // DE000A0H0785 (A0H078) 0,033037 10,0350 EUR 1,47 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) (?<shares>[\\.,\\d]+) [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Depotnummer 0195923225 Ausführung 17.10.2023 18:11:56
                        // @formatter:on
                        .section("time").optional() //
                        .match("^.* Ausf.hrung .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Depotnummer 50000000 Ausführung 17.05.2023
                        // @formatter:on
                        .section("date") //
                        .match("^.* Ausf.hrung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Ausmachender Betrag: 1,48 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag: (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // IE00B0M62Q58 (A0HGV0) 5,214577 66,4800 USD 317,67 EUR
                        // Devisenkurs EUR/ USD 1,09130 vom 18.12.2023
                        // @formatter:on
                        .section("gross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) [\\.,\\d]+ [\\.,\\d]+ [A-Z]{3} (?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3})\\/ (?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Orderreferenz 100000
                        // hZiVsq-ayrsK-DSa. 98 Ausführung 15.11.2024Orderreferenz 4359234
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Orderreferenz (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ord.-Ref.: " + v.get("note")))

                        // @formatter:off
                        // 23986 dUmJRYi Handelsreferenz 4237898
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Handelsreferenz (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | Handels.-Ref.: ")))

                        .wrap((t, ctx) -> {
                            var item = new BuySellEntryItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("(ERTRAGSAUSSCH.TTUNG|Stornierung Dividendengutschrift)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Depotnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Stornierung Dividendengutschrift – VanEck Sust.World EQ.UC.ETF Aandelen oop naam o.N.
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Stornierung) (Aussch.ttung|Dividendengutschrift) \\– .*$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        // @formatter:off
                        // Ausschüttung – iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile
                        // DE000A0H0785 (A0H078) 71,851808 8,895903 EUR
                        //
                        // Dividendengutschrift – iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN
                        // IE00B0M62Q58 (A0HGV0) 5,200029 0,169600 USD
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^(Stornierung )?(Aussch.ttung|Dividendengutschrift) \\– (?<name>.*)$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\) [\\.,\\d]+ [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // DE000A0H0785 (A0H078) 71,851808 8,895903 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) (?<shares>[\\.,\\d]+) [\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbarkeitstag 15.05.2023
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag + 7,44 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag \\+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Devisenkurs EUR/ USD 1,05550
                        // Umrechnung in EUR 0,88 USD 0,84 EUR
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[A-Z]{3})\\/ (?<termCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Umrechnung in [A-Z]{3} (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        var type = new DocumentType("VORABPAUSCHALE");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Depotnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // DE000A0H0785 (A0H078) iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile 26,634225
                        // Vorabpauschale pro Stück 1,257654000 EUR
                        // @formatter:on
                        .section("isin", "wkn", "name", "currency") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\) (?<name>.*) [\\.,\\d]+$") //
                        .match("^Vorabpauschale pro St.ck [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                        })

                        // @formatter:off
                        // DE000A0H0785 (A0H078) iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile 26,634225
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) (?<name>.*) (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Die angefallenen Steuern werden am 05.03.2024 von Ihrem Konto DE1234567890 eingezogen.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Die angefallenen Steuern werden am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Zahltag 02.01.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Zahltag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // Summe Steuern 8,81 EUR
                        // @formatter:on
                        .section("amount", "currency").optional() //
                        .match("^Summe Steuern (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            var item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Saldo                                       EUR                  0,00+
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Alter Saldo.* (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Rechnungsabschluss per 30.09.2024
                                        // @formatter:on
                                        .section("year").optional() //
                                        .match("^Rechnungsabschluss per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.([\\d]{2})? [\\d]{2}\\.[\\d]{2}\\. "
                        + "(.* EINGANG VORBEHALTEN"
                        + "|DA\\-GUTSCHR"
                        + "|GUTSCHRIFT)"
                        + "[\\s]{1,}[\\.,\\d]+[\\+|\\-].*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 03.08.23 03.08. CORE-LA-EV   EINGANG VORBEHALTEN                      10,00+
                                        // 02.09.24 02.09. DA-GUTSCHR                                           100,00+
                                        // 30.09.24 30.09. GUTSCHRIFT                                           200,00+
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("year", "date", "amount", "type") //
                                                        .documentContext("currency") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{2}) (?<date>[\\d]{2}\\.[\\d]{2}\\.) "
                                                                        + "(.* EINGANG VORBEHALTEN"
                                                                        + "|DA\\-GUTSCHR"
                                                                        + "|GUTSCHRIFT)"
                                                                        + "[\\s]{1,}(?<amount>[\\.,\\d]+)(?<type>[\\+|\\-]).*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 01.10. 01.10. DA-GUTSCHR                                             200,00+
                                        // 04.10. 04.10. CORE-LA-EV   EINGANG VORBEHALTEN                        25,00+
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "type") //
                                                        .documentContext("currency", "year") //
                                                        .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) "
                                                                        + "(.* EINGANG VORBEHALTEN" + "|DA\\-GUTSCHR"
                                                                        + "|GUTSCHRIFT)"
                                                                        + "[\\s]{1,}(?<amount>[\\.,\\d]+)(?<type>[\\+|\\-]).*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "-" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("-".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(TransactionItem::new));
    }

    private void addNonImportableTransaction()
    {
        final var type = new DocumentType("Steuerpflichtige Fondsfusion", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Vom 02.02.2024
                                        // @formatter:on
                                        .section("date") //
                                        .match("^Vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^Steuerpflichtige Fondsfusion$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Steuerpflichtige Fondsfusion
                        // AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N.
                        // ISIN LU1861134382
                        // Anzahl/Nominale 255,212216
                        // @formatter:on
                        .section("name", "isin", "shares") //
                        .documentContext("date") //
                        .find("Steuerpflichtige Fondsfusion")
                        .match("^(?<name>.*)$")
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Anzahl\\/Nominale (?<shares>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setAmount(0L);
                        })

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 1,40 EUR
                        // Kapitalertragsteuer: 251,13 EUR
                        // @formatter:on
                        .section("tax", "currency").optional()
                        .match("^Kapitalertrags(s)?teuer(:)? (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 0,07 EUR
                        // Solidaritätszuschlag: 13,81 EUR
                        // @formatter:on
                        .section("tax", "currency").optional()
                        .match("^Solidarit.tszuschlag(:)? (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 0,00 EUR
                        // Kirchensteuer: 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional()
                        .match("^Kirchensteuer(:)? (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Orderentgelt: 0,01 EUR
                        // Orderentgelt: -0,05 EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Orderentgelt: (\\-)?(?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
