package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

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
public class VBankAGPDFExtractor extends AbstractPDFExtractor
{
    public VBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("V-Bank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "V-Bank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)( \\((Zeichnung|R.cknahme)\\))?");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Kauf|Verkauf)( \\((Zeichnung|R.cknahme)\\))?$", "^Diese Mitteilung wurde maschinell .*$");
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
                        .match("^(?<type>(Kauf|Verkauf))( \\((Zeichnung|Rücknahme)\\))?$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wertpapierbezeichnung Deut. Börse Commodities GmbH Xetra-Gold IHS 2007(09/Und)
                        // ISIN DE000A0S9GB0
                        // WKN A0S9GB
                        // Kurs EUR 36,906
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^Kurs (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal / Stück 300 ST
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal \\/ St.ck (?<shares>[\\.,\\d]+) ST$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Handelstag / Zeit 08.05.2019 09:36:23
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelstag \\/ Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Ausmachender Betrag EUR - 11.116,97
                        // Ausmachender Betrag EUR 26.199,03
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (\\-\\s)?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Kurswert USD - 16.136,00
                        // Devisenkurs EUR/USD 1,116670
                        // @formatter:on
                        .section("fxGross", "baseCurrency", "termCurrency", "exchangeRate").optional() //
                        .match("^Kurswert [\\w]{3} \\- (?<fxGross>[\\.,\\d]+)$") //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertr.gnisabrechnung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Ertr.gnisabrechnung$", "^Der Abrechnungsbetrag wird mit Valuta .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapierbezeichnung OptoFlex Inhaber-Ant. P o.N.
                        // ISIN LU0834815366
                        // WKN A1J4YZ
                        // Nominal/Stück 16 ST
                        // Währung EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^WKN (?<wkn>[A-Z0-9]{6})$") //
                        .match("^W.hrung (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nominal/Stück 16 ST
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) ST$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlungstag 11.12.2019
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlungstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag EUR 48,54
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer EUR - 386,08
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag EUR - 21,23
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR - 11,11
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer (?<currency>[\\w]{3}) \\-([\\s])?(?<tax>[\\.,\\d]+)") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Bank-Provision EUR - 30,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Bank\\-Provision (\\*\\s])?(?<currency>[\\w]{3}) \\-([\\s])?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Abwicklungsgebühren * EUR - 2,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Abwicklungsgeb.hren (\\*\\s)?(?<currency>[\\w]{3}) \\-([\\s])?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Spesen * EUR - 1,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Spesen (\\*\\s)?(?<currency>[\\w]{3}) \\-([\\s])?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Gebühren USD - 1,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Geb.hren (\\*\\s)?(?<currency>[\\w]{3}) \\-([\\s])?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage * EUR - 13,17
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Courtage (\\*\\s)?(?<currency>[\\w]{3}) \\-([\\s])?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
