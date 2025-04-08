package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.Messages;
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

@SuppressWarnings("nls")
public class TradegateAGPDFExtractor extends AbstractPDFExtractor
{
    public TradegateAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Tradegate AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addTaxAdjustmentTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Tradegate AG";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Orderart (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // ISIN IE00BM67HN09 Gültigkeit 31.07.2024
                        // WKN A113FG
                        // Wertpapier Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N.
                        // Ausführungskurs 43,2500 EUR
                        // @formatter:on
                        .section("isin", "wkn", "name", "currency") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^Ausf.hrungskurs [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück ausgeführt 3
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck .* (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Handelstag/-zeit 04.06.2024 11:04:53
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelstag\\/\\-zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag 129,75 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 12345 Investcity Order-/Ref.nr. 9876543
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-\\/Ref\\.nr\\. .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Limit 43,2500 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Limit [\\.,\\d]+ [\\w]{3})$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("Ertragsgutschrift");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapier Vanguard EUR Corp.Bond U.ETF Registered Shares EUR Dis.oN
                        // ISIN IE00BZ163G84
                        // WKN A143JK
                        // Ausschüttung 0,127847 EUR pro Stück
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Aussch.ttung [\\.,\\d]+ (?<currency>[\\w]{3}) pro St.ck$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal/Stück 76 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Der Abrechnungsbetrag wird Ihrem o. g. Verrechnungskonto mit Valuta 26.06.2024 gutgeschrieben.
                        // @formatter:on
                        .section("date") //
                        .match("^.* Verrechnungskonto mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) gutgeschrieben\\.$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 7,16 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Bruttobetrag 10,96 USD
                        // Devisenkurs 1,11856 EUR/USD
                        // @formatter:on
                        .section("fxGross", "exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^Bruttobetrag (?<fxGross>[\\.,\\d]+) [\\w]{3}$") //
                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 12345 Investcity Order-/Ref.nr. 9876543
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-\\/Ref\\.nr\\. .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addAdvanceTaxTransaction()
    {
        final DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapier PFI ETFs-EO Sh.Mat.UC.ETF Registered Shares EUR Acc.o.N.
                        // ISIN IE00BVZ6SP04
                        // WKN A14PHG
                        // Vorabpauschale 1,5871303 EUR pro Stück
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Vorabpauschale [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal/Stück 220 Stück
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) St.ck$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ex-Datum 02.01.2025
                        // @formatter:on
                        .section("date") //
                        .match("^Ex\\-Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag 0,00 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // xtmJChhUdfJIJPk 9 d Order-/Ref.nr. 11522032
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-\\/Ref\\.nr\\. .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addTaxAdjustmentTransaction()
    {
        final DocumentType type = new DocumentType("Durch steuerliche Verrechnungen");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Kundennummer.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        // Is type --> "Belastung" change from TAX_REFUND to TAXES
                        .section("type").optional() //
                        .match("^(?<type>(Gutgeschriebener Betrag|Belastung)) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if ("Belastung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        // @formatter:off
                        // Durch steuerlicheVerrechnungen für IhrDepot 00400731.020wurde für denZeitraumvom13.01.2025bis 14.01.2025
                        // @formatter:on
                        .section("date") //
                        .match("^Durch steuerliche.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutgeschriebener Betrag 0,31
                        // Belastung -0,44
                        // @formatter:on
                        .section("amount") //
                        .match("^(Gutgeschriebener Betrag|Belastung) (\\-)?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode("EUR");
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // xtmJChhUdfJIJPk 9 d Order-/Ref.nr. 11522032
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Order\\-\\/Ref\\.nr\\. .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Durch steuerlicheVerrechnungen für IhrDepot 00400731.020wurde für denZeitraumvom13.01.2025bis 14.01.2025
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^.*Zeitraum([\\s])?vom([\\s])?(?<note1>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})([\\s])?bis (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + " - " + v.get("note2"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Abgeführte Kapitalertragsteuer -0,22 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgef.hrte Kapitalertrags(s)?teuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Abgeführter Solidaritätszuschlag -0,01 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgef.hrter Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Abgeführte Kirchensteuer -0,63 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgef.hrte Kirchensteuer \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
