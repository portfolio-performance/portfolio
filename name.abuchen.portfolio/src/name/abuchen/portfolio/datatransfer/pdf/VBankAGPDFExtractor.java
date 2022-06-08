package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;

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

        addBankIdentifier("V-Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "V-Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf|Verkauf)([\\s]+\\(Zeichnung\\))?$", "^Diese Mitteilung wurde maschinell .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>Verkauf)$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Wertpapierbezeichnung Deut. Börse Commodities GmbH Xetra-Gold IHS 2007(09/Und)
                // ISIN DE000A0S9GB0
                // WKN A0S9GB
                // Kurs EUR 36,906
                .section("name", "isin", "wkn", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^ISIN (?<isin>[\\w]{12})$")
                .match("^WKN (?<wkn>.*)$")
                .match("^Kurs (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Nominal / Stück 300 ST
                .section("shares")
                .match("^Nominal \\/ St.ck (?<shares>[\\.,\\d]+) ST$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelstag / Zeit 08.05.2019 09:36:23
                .section("date", "time")
                .match("^Handelstag \\/ Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Ausmachender Betrag EUR - 11.116,97
                // Ausmachender Betrag EUR 26.199,03
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) ([\\-\\s]+)?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kurswert USD - 16.136,00
                // Devisenkurs EUR/USD 1,116670
                // Ausmachender Betrag EUR - 14.480,11
                .section("fxCurrency", "fxGross", "baseCurrency", "termCurrency", "exchangeRate", "currency").optional()
                .match("^Kurswert (?<fxCurrency>[\\w]{3}) \\- (?<fxGross>[\\.,\\d]+)$")
                .match("^Devisenkurs (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertr.gnisabrechnung");
        this.addDocumentTyp(type);

        Block block = new Block("^Ertr.gnisabrechnung$", "^Der Abrechnungsbetrag wird mit Valuta .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Wertpapierbezeichnung OptoFlex Inhaber-Ant. P o.N.
                // ISIN LU0834815366
                // WKN A1J4YZ
                // Nominal/Stück 16 ST
                // Währung EUR
                .section("name", "isin", "wkn", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^ISIN (?<isin>[\\w]{12})$")
                .match("^WKN (?<wkn>.*)$")
                .match("^W.hrung (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Nominal/Stück 16 ST
                .section("shares")
                .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) ST$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Zahlungstag 11.12.2019
                .section("date")
                .match("^Zahlungstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag EUR 48,54
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer EUR - 386,08
                .section("currency", "tax").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) ([\\-\\s]+)?(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag EUR - 21,23
                .section("currency", "tax").optional()
                .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) ([\\-\\s]+)?(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer EUR - 11,11
                .section("currency", "tax").optional()
                .match("^Kirchensteuer (?<currency>[\\w]{3}) ([\\-\\s]+)?(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Bank-Provision EUR - 30,00
                .section("currency", "fee").optional()
                .match("^Bank\\-Provision ([\\*\\s]+)?(?<currency>[\\w]{3}) ([\\-\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Abwicklungsgebühren * EUR - 2,00
                .section("currency", "fee").optional()
                .match("^Abwicklungsgeb.hren ([\\*\\s]+)?(?<currency>[\\w]{3}) ([\\-\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Spesen * EUR - 1,00
                .section("currency", "fee").optional()
                .match("^Spesen ([\\*\\s]+)?(?<currency>[\\w]{3}) ([\\-\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Gebühren USD - 1,00
                .section("currency", "fee").optional()
                .match("^Geb.hren ([\\*\\s]+)?(?<currency>[\\w]{3}) ([\\-\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Courtage * EUR - 13,17
                .section("currency", "fee").optional()
                .match("^Courtage ([\\*\\s]+)?(?<currency>[\\w]{3}) ([\\-\\s]+)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
