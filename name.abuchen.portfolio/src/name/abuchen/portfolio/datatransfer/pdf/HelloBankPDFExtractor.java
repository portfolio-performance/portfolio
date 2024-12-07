package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

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
public class HelloBankPDFExtractor extends AbstractPDFExtractor
{
    public HelloBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hellobank");
        addBankIdentifier("Hello bank!");

        addBuySellTransaction();
        addDividendTransaction();
        addInboundDelivery();
    }

    @Override
    public String getLabel()
    {
        return "Hello Bank";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Gesch.ftsart: (Kauf|Verkauf|Kauf aus Dauerauftrag)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gesch.ftsart: (Kauf|Verkauf|Kauf aus Dauerauftrag)$");
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
                        .match("^Gesch.ftsart: (?<type>(Kauf|Verkauf|Kauf aus Dauerauftrag))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Titel: NO0003054108  M a r i n e  H a r v est ASA
                        // Kurs: 140 NOK
                        // @formatter:on
                        .section("isin", "name", "nameContinued", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^Kurs: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Handelszeit: 11.5.2021
                        // Handelszeit: 30.6.2017 um 09:00:10 Uhr
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelszeit: .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Handelszeit: 11.5.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Zugang: 74 Stk Teilausführung
                        // @formatter:on
                        .section("shares") //
                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zu Lasten IBAN AT44 1925 0654 0668 9002 -1.118,80 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Zu Lasten|Zu Gunsten) .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Kurswert: -10.360,-- NOK
                        // Devisenkurs: 9,486 (3.7.2017) -1.092,14 EUR
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional() //
                        .match("^Kurswert: (?<fxGross>[\\-\\.,\\d]+) (?<termCurrency>[\\w]{3}).*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$") //
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

    private void addDividendTransaction()
    {
        final DocumentType type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gesch.ftsart: Ertrag$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Titel: NO0003054108  M a r i n e  H a r v est ASA
                        // Navne-Aksjer NK 7,50
                        // Dividende: 3,2 NOK
                        // @formatter:on
                        .section("isin", "name", "nameContinued", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^(Dividende|Ertrag): (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 200 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(Abgang: )?(?<shares>[\\.,\\d]+) Stk.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 6.9.2017
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Gunsten IBAN AT44 1925 0654 0668 9002 48,71 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu Gunsten .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Bruttoertrag: 640,-- NOK
                        // Devisenkurs: 9,308 (5.9.2017) 49,85 EUR
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional() //
                        .match("^Bruttoertrag: (?<fxGross>[\\-\\.,\\d]+) (?<termCurrency>[\\w]{3}).*$") //
                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addInboundDelivery()
    {
        final DocumentType type = new DocumentType("Freier Erhalt");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Gesch.ftsart: Freier Erhalt$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            PortfolioTransaction  portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Titel: DK0060534915  N o v o - N o r d i s k AS
                        // Navne-Aktier B DK -,20
                        // Lieferpflichtiger: Depotnummer: 99147
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^steuerlicher Anschaffungswert: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Kurs") || !v.get("name1").startsWith("Verwahrart"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Zugang: 80 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^Zugang: (?<shares>[\\.,\\d]+) Stk.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Kassatag: 29.3.2017
                        // @formatter:on
                        .section("date") //
                        .match("^Kassatag: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // steuerlicher Anschaffungswert: 3.225,37 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^steuerlicher Anschaffungswert: (?<amount>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // Kapitalertragsteuer: -254,10 AUD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // KESt Ausländische Dividende: -176,01 NOK
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer US-Emittent: -3,05 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer US\\-Emittent: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Quellensteuer: -8,81 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Quellensteuer: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Umsatzsteuer: -0,19 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Umsatzsteuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // Fremde Spesen: -25,20 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fremde Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Eigene Spesen: -6,42 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Eigene Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Inkassoprovision: -0,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Inkassoprovision: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Grundgebühr: -1,46 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Grundgeb.hr: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
