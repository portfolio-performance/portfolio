package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

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
 * @implNote The PDF does not include the name of the security.
 *           The ticker symbol is used to identify the security.
 *           The currency of Stake is AUD (A$).
 */

@SuppressWarnings("nls")
public class StakePDFExtractor extends AbstractPDFExtractor
{
    public StakePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Stake");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Stake";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(BUY|SELL) CONFIRMATION");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^(BUY|SELL) CONFIRMATION$");
        type.addBlock(firstRelevantLine);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Sell" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(BUY|SELL)) CONFIRMATION$")
                .assign((t, v) -> {
                    if ("SELL".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // EFFECTIVE PRICE $20.43 TICKER FLT.ASX
                // @formatter:on
                .section("tickerSymbol", "currency")
                .match("^EFFECTIVE PRICE \\p{Sc}[\\.,\\d]+ TICKER (?<tickerSymbol>[\\w]{3,4})\\..*$")
                .match("^VALUE (?<currency>[A-Z]\\p{Sc}).*$")
                .assign((t, v) -> {
                    v.put("name", v.get("tickerSymbol"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // QUANTITY 512
                // @formatter:on
                .section("shares")
                .match("^QUANTITY (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // VALUE A$10,455.04 EXECUTION DATE 27-05-2022
                // @formatter:on
                .section("date")
                .match("^VALUE [A-Z]\\p{Sc}[\\.,\\d]+ EXECUTION DATE (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // AMOUNT DUE & PAYABLE A$10,458.04 Funds have already
                // been deducted from your buying power. No action
                // @formatter:on
                .section("currency", "amount")
                .match("^AMOUNT DUE & PAYABLE (?<currency>[A-Z]\\p{Sc})(?<amount>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // CONFIRMATION NUMBER 0000001 PID 3556
                // @formatter:on
                .section("note").optional()
                .match("^CONFIRMATION NUMBER (?<note>.*) PID .*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // BROKERAGE & GST A$3.00 SIDE BUY
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^BROKERAGE & GST (?<currency>[A-Z]\\p{Sc})(?<fee>[\\.,\\d]+) SIDE (BUY|SELL)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "AU");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "AU");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "AU");
    }
}
