package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * @formatter:off
 * @implNote Importer for "Info Reports" produced by the Bison App.
 *
 * @implSpec Bison only supports EUR as currency. Therefore the extractor is always
 *           defaulting to EUR.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class BisonPDFExtractor extends AbstractPDFExtractor
{
    public BisonPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BISON");

        addBuyCryptoTransaction();
        addDeliveryInboundTransaction();
        addDepositRemovalTransaction();
        addAdvanceTaxTransaction();
    }

    @Override
    public String getLabel()
    {
        return "BISON";
    }

    private void addBuyCryptoTransaction()
    {
        final var type = new DocumentType("Info-Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(Kauf|Verkauf)([\\*])? [\\w]{3} [\\.,\\d]+$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Verkauf))([\\*])? [\\w]{3} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Kauf BTC 0,00028505
                        // 28.12.2021 09:00 43.850,74 €/BTC - 12,50 €
                        //
                        // Kauf* BTC 0,01282436
                        // 16.01.2020 11:19 7.797,66 €/BTC - 100,00 €
                        //
                        // Verkauf* ETH 0,03396843
                        // 12.06.2022 21:19 1.401,94 €/ETH + 47,62 €
                        // @formatter:on
                        .section("tickerSymbol", "shares", "date", "time", "amount", "currency") //
                        .match("^(Kauf|Verkauf)([\\*])? (?<tickerSymbol>[\\w]{3}) (?<shares>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date"), v.get("time")));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDeliveryInboundTransaction()
    {
        var type = new DocumentType("Info-Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^(Gutschein)\\*? [\\w]{3} [\\.,\\d]+$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Gutschein BTC 0,00130130
                        // 16.01.2020 11:19 7.684,63 €/BTC + 10,00 €
                        // @formatter:on
                        .section("tickerSymbol", "shares", "date", "time", "amount", "currency") //
                        .match("^Gutschein([\\*])? (?<tickerSymbol>[\\w]{3}) (?<shares>[\\.,\\d]+)$") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) .* (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            t.setShares(asShares(v.get("shares")));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addDepositRemovalTransaction()
    {
        var type = new DocumentType("Info-Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(Einzahlung|Auszahlung)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // Is type --> "Auszahlung" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Einzahlung|Auszahlung))$") //
                        .assign((t, v) -> {
                            if ("Auszahlung".equals(v.get("type"))) //
                                t.setType(AccountTransaction.Type.REMOVAL);
                        })

                        // @formatter:off
                        // Einzahlung
                        // 16.01.2020 10:30 + 100,00 €
                        //
                        // Auszahlung
                        // 22.11.2020 09:51 - 100,00 €
                        // @formatter:on
                        .section("date", "time", "amount", "currency") //
                        .find("(Einzahlung|Auszahlung)") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) [\\-|\\+] (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAdvanceTaxTransaction()
    {
        final var type = new DocumentType("Abrechnung Vorabpauschale");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Abrechnung Vorabpauschale.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Name LYX ETF EUR CASH
                        // ISIN FR0010510800
                        // Monat Anzahl Stücke Vorabpauschale in EUR Vorabpauschale in EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Name (?<name>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^.*Vorabpauschale in (?<currency>[\\w]{3})$$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück
                        // Dezember 24,0000 0,14 3,41
                        // @formatter:on
                        .section("shares") //
                        .find("St.ck") //
                        .match("^.* (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Tag des Zuflusses 02 Januar 2025
                        // @formatter:on
                        .section("date") //
                        .match("^Tag des Zuflusses (?<date>[\\d]{1,2} .* [\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Aufgrund nachstehender Abrechnung buchen wir zu Ihren Lasten €0,89
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Aufgrund nachstehender Abrechnung buchen wir zu Ihren Lasten (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }
}
