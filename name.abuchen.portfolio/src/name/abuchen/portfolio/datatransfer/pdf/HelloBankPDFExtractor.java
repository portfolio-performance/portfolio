package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

public class HelloBankPDFExtractor extends AbstractPDFExtractor
{
    public HelloBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hellobank"); //$NON-NLS-1$
        addBankIdentifier("Hello bank!"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
        addInboundDelivery();
    }

    @Override
    public String getLabel()
    {
        return "Hello Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: (Kauf|Verkauf|Kauf aus Dauerauftrag)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Gesch.ftsart: (Kauf|Verkauf|Kauf aus Dauerauftrag)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Gesch.ftsart: (?<type>(Kauf|Verkauf|Kauf aus Dauerauftrag))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Titel: NO0003054108  M a r i n e  H a r v est ASA   
                // Kurs: 140 NOK 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurs: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelszeit: 11.5.2021
                // Handelszeit: 30.6.2017 um 09:00:10 Uhr
                .section("time").optional()
                .match("^Handelszeit: .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Handelszeit: 11.5.2021
                .section("date")
                .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Zugang: 74 Stk Teilausführung
                .section("shares")
                .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Zu Lasten IBAN AT44 1925 0654 0668 9002 -1.118,80 EUR 
                .section("amount", "currency")
                .match("^(Zu Lasten|Zu Gunsten) .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Kurswert: -10.360,-- NOK 
                // -10.360,-- NOK 
                // Devisenkurs: 9,486 (3.7.2017) -1.092,14 EUR 
                .section("termCurrency", "fxGross", "fxCurrency", "exchangeRate", "baseCurrency").optional()
                .match("^Kurs: [\\-\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$")
                .match("^Kurswert: (?<fxGross>[\\-\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("^Gesch.ftsart: Ertrag$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Titel: NO0003054108  M a r i n e  H a r v est ASA                 
                // Navne-Aksjer NK 7,50               
                // Dividende: 3,2 NOK 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^(Dividende|Ertrag): (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 200 Stk
                .section("shares")
                .match("^(Abgang: )?(?<shares>[\\.,\\d]+) Stk.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 6.9.2017
                .section("date")
                .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Zu Gunsten IBAN AT44 1925 0654 0668 9002 48,71 EUR 
                .section("amount", "currency")
                .match("^Zu Gunsten .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Dividende: 3,2 NOK 
                // Bruttoertrag: 640,-- NOK 
                // Devisenkurs: 9,308 (5.9.2017) 49,85 EUR 
                .section("termCurrency","fxGross", "fxCurrency", "exchangeRate", "baseCurrency").optional()
                .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$")
                .match("^Bruttoertrag: (?<fxGross>[\\-\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addInboundDelivery()
    {
        DocumentType type = new DocumentType("Freier Erhalt");
        this.addDocumentTyp(type);

        Block block = new Block("^Gesch.ftsart: Freier Erhalt$");
        type.addBlock(block);
        block.set(new Transaction<PortfolioTransaction>()

                .subject(() -> {
                    PortfolioTransaction transaction = new PortfolioTransaction();
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                    return transaction;
                })

                // Titel: DK0060534915  N o v o - N o r d i s k AS                    
                // Navne-Aktier B DK -,20             
                // Lieferpflichtiger: Depotnummer: 99147 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>\\S*) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^steuerlicher Anschaffungswert: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs") || !v.get("name1").startsWith("Verwahrart"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Zugang: 80 Stk
                .section("shares")
                .match("^Zugang: (?<shares>[\\.,\\d]+) Stk.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Kassatag: 29.3.2017
                .section("date")
                .match("^Kassatag: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // steuerlicher Anschaffungswert: 3.225,37 EUR
                .section("amount", "currency")
                .match("^steuerlicher Anschaffungswert: (?<amount>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer: -254,10 AUD 
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt Ausländische Dividende: -176,01 NOK 
                .section("tax", "currency").optional()
                .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer US-Emittent: -3,05 USD 
                .section("withHoldingTax", "currency").optional()
                .match("^Quellensteuer US\\-Emittent: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Quellensteuer: -8,81 EUR 
                .section("withHoldingTax", "currency").optional()
                .match("^Quellensteuer: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Umsatzsteuer: -0,19 EUR 
                .section("tax", "currency").optional()
                .match("^Umsatzsteuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Fremde Spesen: -25,20 EUR 
                .section("fee", "currency").optional()
                .match("^Fremde Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen: -6,42 EUR 
                .section("fee", "currency").optional()
                .match("^Eigene Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Inkassoprovision: -0,95 EUR 
                .section("fee", "currency").optional()
                .match("^Inkassoprovision: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Grundgebühr: -1,46 EUR 
                .section("fee", "currency").optional()
                .match("^Grundgeb.hr: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
