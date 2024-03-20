package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

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
 * @implNote Stakeshop Pty Ltd, trading as Stake, is a Australian dollar-based financial services company.
 *           The currency is AUD --> A$.
 *
 * @implSpec The PDF does not include the name of the security.
 *           If it is created, identify it at least by the ticker.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class StakeshopPtyLtdPDFExtractor extends AbstractPDFExtractor
{
    public StakeshopPtyLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Stakeshop Pty Ltd");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Stakeshop Pty Ltd (Stake)";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(BUY|SELL) CONFIRMATION");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(BUY|SELL) CONFIRMATION$");
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
                        .match("^(?<type>(BUY|SELL)) CONFIRMATION$") //
                        .assign((t, v) -> {
                            if ("SELL".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // EFFECTIVE PRICE $20.43 TICKER FLT.ASX
                        // @formatter:on
                        .section("tickerSymbol", "currency") //
                        .match("^EFFECTIVE PRICE \\p{Sc}[\\.,\\d]+ TICKER (?<tickerSymbol>[\\w]{3,4})\\..*$") //
                        .match("^VALUE (?<currency>[\\w]{1}\\p{Sc}).*") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // QUANTITY 512
                        // @formatter:on
                        .section("shares") //
                        .match("^QUANTITY (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // VALUE A$10,455.04 EXECUTION DATE 27-05-2022
                        // @formatter:on
                        .section("date") //
                        .match("^VALUE A\\p{Sc}[\\.,\\d]+ EXECUTION DATE (?<date>[\\d]{1,2}\\-[\\d]{1,2}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // AMOUNT DUE & PAYABLE A$10,458.04 Funds have already
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^AMOUNT DUE & PAYABLE (?<currency>A\\p{Sc})(?<amount>[\\.,\\d]+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // CONFIRMATION NUMBER 0000001 PID 3556
                        // @formatter:on
                        .section("note").optional()//
                        .match("^CONFIRMATION NUMBER (?<note>.*) PID [\\d]+$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // BROKERAGE & GST A$3.00 SIDE BUY
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^BROKERAGE & GST (?<currency>A\\p{Sc})(?<fee>[\\.,\\d]+) SIDE (BUY|SELL)$")
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
}
