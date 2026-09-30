package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Importer for "Transaction history report" statements produced by Relai AG.
 *           Relai is a Swiss Bitcoin-only broker.
 *
 * @implSpec There is no "real" bank/broker identification in the document,
 *           so we use the column headings of the transaction history.
 *
 *           The amounts use a blank as thousands separator and a dot as decimal separator.
 *           85 374.42 CHF --> 85374.42 CHF
 * @formatter:on
 */

@SuppressWarnings("nls")
public class RelaiPDFExtractor extends AbstractPDFExtractor
{
    public RelaiPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Date (DD.MM.YYYY) Type BTC amount BTC price Fiat amount excl. fee Fee Counterparty");

        addBuyCryptoTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Relai AG";
    }

    private void addBuyCryptoTransaction()
    {
        final var type = new DocumentType("Transaction history report for", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Date (DD.MM.YYYY) Type BTC amount BTC price Fiat amount excl. fee Fee Counterparty
                                        // @formatter:on
                                        .section("tickerSymbol") //
                                        .match("^Date \\(DD\\.MM\\.YYYY\\) Type (?<tickerSymbol>[A-Z0-9]{1,5}) amount .*$") //
                                        .assign((ctx, v) -> ctx.put("tickerSymbol", v.get("tickerSymbol"))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} Buy [\\.\\d]+ .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // 17.10.2025 20:09:05 Buy 0.00116054 85 374.42 CHF 99.10 CHF 0.90 CHF Relai Switzerland
                        // @formatter:on
                        .section("date", "time", "shares", "amount", "currency", "fee", "feeCurrency") //
                        .documentContext("tickerSymbol") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) Buy (?<shares>[\\.\\d]+) [\\.\\d\\s]+ [A-Z]{3} (?<amount>[\\.\\d\\s]+) (?<currency>[A-Z]{3}) (\\-)?(?<fee>[\\.\\d\\s]+) (?<feeCurrency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("feeCurrency")), asAmount(v.get("fee")));
                            var amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            t.setMonetaryAmount(amount.add(fee));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // 17.10.2025 20:09:05 Buy 0.00116054 85 374.42 CHF 99.10 CHF 0.90 CHF Relai Switzerland
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} Buy [\\.\\d]+ [\\.\\d\\s]+ [A-Z]{3} [\\.\\d\\s]+ [A-Z]{3} (\\-)?(?<fee>[\\.\\d\\s]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(stripBlanks(value), Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(stripBlanks(value), Values.Share, "en", "US");
    }
}
