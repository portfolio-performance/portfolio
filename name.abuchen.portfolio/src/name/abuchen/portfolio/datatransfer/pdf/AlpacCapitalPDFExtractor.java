package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Alpac Capital is a US-based financial services company.
 *           The currency is USD.
 *
 *           All security currencies are USD.
 *
 * @implSpec The CUSIP number is the WKN number.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class AlpacCapitalPDFExtractor extends AbstractPDFExtractor
{
    public AlpacCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Alpaca Securities LLC");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Alpac Capital";
    }

    private void addBuySellTransaction()
    {
        var securityRange = new Block("^SYMBOL: [A-Z]{3,4}$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // SYMBOL: SGOV
                                        // @formatter:on
                                        .attributes("tickerSymbol") //
                                        .match("^SYMBOL: (?<tickerSymbol>[A-Z]{3,4})$"));

        final DocumentType type = new DocumentType("BUY\\/SELL", securityRange);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^BUY\\/SELL.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(buy|sell)) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}) .* (\\-)?[\\.,\\d]+ .* [\\d]{2}, [\\d]{4}$") //
                        .assign((t, v) -> {
                            if ("sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // SYMBOL: SGOV
                        // BUY/SELL QTY CURRENCY PRICE GROSSAMOUNT FEES NET AMOUNT Trade Date
                        // buy 1.079762663 USD 100.448000 -108.46 -108.46 Jul 11, 2024
                        // TRADE TIME SETTLE DATE ASSET TYPE STATUS CAPACITY S/U/D CUSIP PRIMARYEXCHANGE NOTE
                        // 03:45:50 PM (ET) Jul 12, 2024 Equity executed principal 46436E718 NYSE Arca
                        //
                        // SYMBOL: SGOV
                        // BUY/SELL QTY CURRENCY PRICE GROSSAMOUNT FEES NET AMOUNT Trade Date
                        // sell -1.078967788 USD 100.522000 108.46 108.46 Jul 17, 2024
                        // TRADE TIME SETTLE DATE ASSET TYPE STATUS CAPACITY S/U/D CUSIP PRIMARYEXCHANGE NOTE
                        // 11:27:42 AM (ET) Jul 18, 2024 Equity executed principal 46436E718 NYSE Arca
                        // @formatter:on
                        .section("currency", "wkn") //
                        .documentRange("tickerSymbol") //
                        .match("^(buy|sell) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}) .* (\\-)?[\\.,\\d]+ .* [\\d]{2}, [\\d]{4}$") //
                        .match("^[\\d]{2}:[\\d]{2}:[\\d]{2} .*, [\\d]{4} .* (?<wkn>[\\w]{9}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // buy 1.079762663 USD 100.448000 -108.46 -108.46 Jul 11, 2024
                        // sell -1.078967788 USD 100.522000 108.46 108.46 Jul 17, 2024
                        // @formatter:on
                        .section("shares") //
                        .match("^(buy|sell) (\\-)?(?<shares>[\\.,\\d]+) [\\w]{3} .* (\\-)?[\\.,\\d]+ .* [\\d]{2}, [\\d]{4}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // buy 1.079762663 USD 100.448000 -108.46 -108.46 Jul 11, 2024
                        // 03:45:50 PM (ET) Jul 12, 2024 Equity executed principal 46436E718 NYSE Arca
                        //
                        // sell -1.078967788 USD 100.522000 108.46 108.46 Jul 17, 2024
                        // 11:27:42 AM (ET) Jul 18, 2024 Equity executed principal 46436E718 NYSE Arca
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(buy|sell) (\\-)?[\\.,\\d]+ [\\w]{3} .* (\\-)?[\\.,\\d]+ (?<date>.* [\\d]{2}, [\\d]{4})$") //
                        .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2} .*) \\(.*\\).*, [\\d]{4} .* [\\w]{9}.*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // buy 1.079762663 USD 100.448000 -108.46 -108.46 Jul 11, 2024
                        // sell -1.078967788 USD 100.522000 108.46 108.46 Jul 17, 2024
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(buy|sell) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}) .* (\\-)?(?<amount>[\\.,\\d]+) .* [\\d]{2}, [\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // buy 1 USD 512.941000 -512.94 commission: -1.00 -513.94 Jul 17, 2024
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^(buy|sell) (\\-)?[\\.,\\d]+ (?<currency>[\\w]{3}) .* commission: \\-(?<fee>[\\.,\\d]+) (\\-)?[\\.,\\d]+ .* [\\d]{2}, [\\d]{4}$") //
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
