package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Importer for "Transaktionshistorie" reports produced by BSDEX.
 *
 * @implSpec There is no “real” bank/broker identification in the document,
 *           so we use the column headings
 * @formatter:on
 */

@SuppressWarnings("nls")
public class BSDEXPDFExtractor extends AbstractPDFExtractor
{
    public BSDEXPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("ID Typ Ausführung eingehender eingehendes ausgehender ausgehendes");
        addBankIdentifier("ID Typ Ausführung eingehender Betrag eingehendes Asset ausgehender");

        addBuyCryptoTransaction();
        addSellCryptoTransaction();
        addDepositAndRemovalTransaction();
    }

    @Override
    public String getLabel()
    {
        return "BSDEX (Börse Stuttgart Digital Exchange)";
    }

    private void addBuyCryptoTransaction()
    {
        final var type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[a-f0-9\\-]+ Kauf [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // d54eb916-79c9-4eb2-b0ed- Kauf 18.12.2024 0.001 BTC 99.0 EUR 0.2 EUR
                                        // 041dd43be6cc 04:14:42
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "shares", "tickerSymbol", "amount", "currency", "time", "fee", "feeCurrency") //
                                                        .match("^([a-f0-9\\-]+) Kauf (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<shares>[\\.,\\d]+) (?<tickerSymbol>[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+) (?<feeCurrency>[\\w]{3})$") //
                                                        .match("^([a-f0-9\\-]+)? ?(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})") //
                                                        .assign((t, v) -> {
                                                            var fee = Money.of(v.get("feeCurrency"), asAmount(v.get("fee")));
                                                            var amount = Money.of(v.get("currency"), asAmount(v.get("amount")));

                                                            t.setSecurity(getOrCreateCryptoCurrency(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setMonetaryAmount(amount.add(fee));
                                                        }),
                                        // @formatter:off
                                        // 529801f1-b186-4c4c-a43d-91d340da2b5d Kauf 02.08.2023 01:25:55 0.00553256 BTC 150.65 EUR 0.53 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time", "shares", "tickerSymbol", "amount", "currency", "fee", "feeCurrency") //
                                                        .match("^([a-f0-9\\-]+) Kauf (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) (?<shares>[\\.,\\d]+) (?<tickerSymbol>[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+) (?<feeCurrency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            var fee = Money.of(v.get("feeCurrency"), asAmount(v.get("fee")));
                                                            var amount = Money.of(v.get("currency"), asAmount(v.get("amount")));

                                                            t.setSecurity(getOrCreateCryptoCurrency(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setMonetaryAmount(amount.add(fee));
                                                        }))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellCryptoTransaction()
    {
        final var type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[a-f0-9\\-]+ Verkauf [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // d54eb916-79c9-4eb2-b0ed- Kauf 18.12.2024 0.001 BTC 99.0 EUR 0.2 EUR
                                        // 041dd43be6cc 04:14:42
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "amount", "currency", "shares", "tickerSymbol", "fee", "feeCurrency", "time") //
                                                        .match("^([a-f0-9\\-]+) Verkauf (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<shares>[\\.,\\d]+) (?<tickerSymbol>[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?) (?<fee>[\\.,\\d]+) (?<feeCurrency>[\\w]{3})$") //
                                                        .match("^([a-f0-9\\-\\s]+)?(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}).*") //
                                                        .assign((t, v) -> {
                                                            var fee = Money.of(v.get("feeCurrency"), asAmount(v.get("fee")));
                                                            var amount = Money.of(v.get("currency"), asAmount(v.get("amount")));

                                                            t.setSecurity(getOrCreateCryptoCurrency(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setMonetaryAmount(amount.subtract(fee));
                                                        }),
                                        // @formatter:off
                                        // 97ad9a75-5298-4bef-97c7-a7fe3adeed50 Verkauf 11.12.2023 02:13:12 211.62 EUR 0.00553256 BTC 0.74 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time", "amount", "currency", "shares", "tickerSymbol", "fee", "feeCurrency") //
                                                        .match("^([a-f0-9\\-]+) Verkauf (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<shares>[\\.,\\d]+) (?<tickerSymbol>[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?) (?<fee>[\\.,\\d]+) (?<feeCurrency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            var fee = Money.of(v.get("feeCurrency"), asAmount(v.get("fee")));
                                                            var amount = Money.of(v.get("currency"), asAmount(v.get("amount")));

                                                            t.setSecurity(getOrCreateCryptoCurrency(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            t.setMonetaryAmount(amount.subtract(fee));
                                                        }))


                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepositAndRemovalTransaction()
    {
        var type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[a-f0-9\\-]+ (Einzahlung|Auszahlung) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(2);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // f5f27cba-6e3a-4120-8d96-c6be8859ce6f Einzahlung 14.06.2024 25.5 EUR IBAN-A IBAN-B
                        // 06:00:26 4 6
                        //
                        // c2c3eea6-cbee-4352-b87a- Auszahlung 17.12.2024 113.66 EUR IBAN-A IBAN-B
                        // 87557f1886c3 06:30:23 6 4
                        // @formatter:on
                        .section("type", "date", "amount", "currency", "time") //
                        .match("^([a-f0-9\\-]+) (?<type>Einzahlung|Auszahlung) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .match("^([a-f0-9\\-\\s]+)?(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}).*") //
                        .assign((t, v) -> {
                            // Is type --> "Auszahlung" change from DEPOSIT to REMOVAL
                            if ("Auszahlung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // d54eb916-79c9-4eb2-b0ed- Kauf 18.12.2024 0.001 BTC 99.0 EUR 0.2 EUR
                        // 750968db-6059-481b-9dd4- Verkauf 17.12.2024 37.8 EUR 15.0 XRP 0.08 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^([a-f0-9\\-]+) (Kauf|Verkauf) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }
}
