package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

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
public class DreiBankenEDVPDFExtractor extends AbstractPDFExtractor
{
    public DreiBankenEDVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("91810s/Klagenfurt");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "3BankenEDV";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapier\\-.* (Kauf|Verkauf)$");
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
                        .match("Wertpapier\\-.* (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk .               2,00
                        // Inhaber-Anteile I o.N.
                        // Kurs 102,64 EUR Kurswert EUR              205,28
                        // @formatter:on
                        .section("isin", "name", "nameContinued", "currency") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) .*$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk .               2,00
                        // @formatter:on
                        .section("shares") //
                        .match("^.* (Zugang|Abgang) Stk([\\s]+)?\\. ([\\s]+)?(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Handelszeitpunkt: 04.01.2021 12:05:55
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelszeitpunkt: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Wertpapierrechnung Wert 06.01.2021 EUR              205,30
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Wertpapierrechnung Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$") //
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
        final DocumentType type = new DocumentType("(Aussch.ttung|Dividende)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapier\\-.* (Aussch.ttung|Dividende)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk .               4,00
                        // Registered Shares EUR (Dist)oN
                        // Ertrag 0,0806 EUR Kurswert EUR                0,32KESt-Neu EUR               -0,02
                        // @formatter:on
                        .section("isin", "name", "nameContinued", "currency") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) Stk([\\s]+)?\\. .*$") //
                        .match("(?<nameContinued>.*)") //
                        .match("^Ertrag [\\.,\\d]+ (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk .               4,00
                        // @formatter:on
                        .section("shares") //
                        .match("^.* Stk([\\s]+)?\\. ([\\s]+)?(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wertpapierrechnung Wert 23.12.2020 EUR                0,30
                        // @formatter:on
                        .section("date") //
                        .match("^Wertpapierrechnung Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Wertpapierrechnung Wert 23.12.2020 EUR                0,30
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Wertpapierrechnung Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) ([\\s]+)?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Ertrag 0,68 USD Kurswert USD                2,04Quellensteuer USD               -0,31
                        // 15 % QUSt a 1,224 v. 28.12.2020 EUR 1,21
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional() //
                        .match("^.* Kurswert (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+).*$") //
                        .match("^.* (?<exchangeRate>[\\.,\\d]+) v\\. [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<baseCurrency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // Auslands-KESt USD               -0,26
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Auslands\\-KESt (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 0,0204 EUR KESt
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) KESt$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Ertrag 0,0806 EUR Kurswert EUR                0,32KESt-Neu EUR               -0,02
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*KESt\\-Neu (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Ertrag 0,68 USD Kurswert USD                2,04Quellensteuer USD               -0,31
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^.*Quellensteuer (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Kursgewinn-KESt EUR                -3,37
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kursgewinn\\-KESt (?<currency>[\\w]{3}) ([\\s]+)?\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // Dritt- und Börsengebühr EUR                0,02
                        // Dritt- und Börsengebühr EUR                -0,08
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Dritt\\- und B.rsengeb.hr (?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
