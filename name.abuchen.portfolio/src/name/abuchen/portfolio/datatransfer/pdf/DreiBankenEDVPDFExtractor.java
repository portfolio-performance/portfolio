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
public class DreiBankenEDVPDFExtractor extends AbstractPDFExtractor
{
    public DreiBankenEDVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("91810s/Klagenfurt"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "3BankenEDV"; //$NON-NLS-1$
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

        Block firstRelevantLine = new Block("^Wertpapier\\-.* (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional().match("Wertpapier\\-.* (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk .               2,00
                // Inhaber-Anteile I o.N.
                // Kurs 102,64 EUR Kurswert EUR              205,28
                .section("isin", "name", "nameContinued", "currency")
                .match("^(?<isin>[\\w]{12}) (?<name>.*) (Zugang|Abgang) .*$")
                .match("^(?<nameContinued>.*)$")
                .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk .               2,00
                .section("shares")
                .match("^.* (Zugang|Abgang) Stk([\\s]+)?\\. ([\\s]+)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeitpunkt: 04.01.2021 12:05:55
                .section("date", "time")
                .match("^Handelszeitpunkt: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Wertpapierrechnung Wert 06.01.2021 EUR              205,30
                .section("currency", "amount")
                .match("^Wertpapierrechnung Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Aussch.ttung|Dividende)");
        this.addDocumentTyp(type);

        Block block = new Block("^Wertpapier\\-.* (Ausschüttung|Dividende)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk .               4,00
                // Registered Shares EUR (Dist)oN
                // Ertrag 0,0806 EUR Kurswert EUR                0,32KESt-Neu EUR               -0,02
                .section("isin", "name", "nameContinued", "currency")
                .match("^(?<isin>[\\w]{12}) (?<name>.*) Stk([\\s]+)?\\. .*$")
                .match("(?<nameContinued>.*)")
                .match("^Ertrag [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk .               4,00
                .section("shares")
                .match("^.* Stk([\\s]+)?\\. ([\\s]+)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Wertpapierrechnung Wert 23.12.2020 EUR                0,30
                .section("date")
                .match("^Wertpapierrechnung Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Wertpapierrechnung Wert 23.12.2020 EUR                0,30
                .section("currency", "amount")
                .match("^Wertpapierrechnung Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ertrag 0,68 USD Kurswert USD                2,04Quellensteuer USD               -0,31
                // 15 % QUSt a 1,224 v. 28.12.2020 EUR 1,21
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^.* Kurswert (?<fxCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$")
                .match("^.* (?<exchangeRate>[\\.,\\d]+) v\\. [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueA())

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Auslands-KESt USD               -0,26
                .section("tax", "currency").optional()
                .match("^Auslands\\-KESt (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // 0,0204 EUR KESt
                .section("tax", "currency").optional()
                .match("^(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) KESt$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ertrag 0,0806 EUR Kurswert EUR                0,32KESt-Neu EUR               -0,02
                .section("tax", "currency").optional()
                .match("^.*KESt\\-Neu (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ertrag 0,68 USD Kurswert USD                2,04Quellensteuer USD               -0,31
                .section("withHoldingTax", "currency").optional()
                .match("^.*Quellensteuer (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Kursgewinn-KESt EUR                -3,37
                .section("tax", "currency").optional()
                .match("^Kursgewinn\\-KESt (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Dritt- und Börsengebühr EUR                0,02
                // Dritt- und Börsengebühr EUR                -0,08
                .section("fee", "currency").optional()
                .match("^Dritt\\- und B.rsengeb.hr (?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
