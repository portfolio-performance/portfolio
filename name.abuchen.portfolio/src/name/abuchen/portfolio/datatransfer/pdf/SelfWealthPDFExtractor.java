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
 * @implNote SelfWealth Ltd. is a Australian dollar-based financial services company.
 *           The currency of SelfWealth Ltd is always AUD.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SelfWealthPDFExtractor extends AbstractPDFExtractor
{
    public SelfWealthPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("SelfWealth");
        addBankIdentifier("Selfwealth");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "SelfWealth Ltd";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Buy|Sell) Confirmation");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Buy|Sell) Confirmation$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Buy|Sell)) Confirmation$") //
                        .assign((t, v) -> {
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 25 UMAX BETA S&P500 YIELDMAX 12.40 $312.50 AUD
                        // @formatter:on
                        .section("tickerSymbol", "name", "currency") //
                        .match("^[\\.,\\d]+ (?<tickerSymbol>[\\w]{3,4}) (?<name>.*) [\\.,\\d]+ \\p{Sc}[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 25 UMAX BETA S&P500 YIELDMAX 12.40 $312.50 AUD
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) [\\w]{3,4} .* [\\.,\\d]+ \\p{Sc}[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 1 LONG ROAD Trade Date: 1 Jul 2021
                        // @formatter:on
                        .section("date") //
                        .match("^.* Trade Date: (?<date>[\\d]+ .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Net Value $322.00 AUD
                        // Total Amount Payable $692.60 AUD
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Net Value|Total Amount Payable) \\p{Sc}(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // JOHN DOE A/C Reference No: T20210701123456­-1
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* Reference No: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Brokerage* $9.50 AUD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Brokerage\\* \\p{Sc}(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Misc Fees & Charges $0.00 AUD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Misc Fees & Charges\\* \\p{Sc}(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Adviser Fee* $0.00 AUD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Adviser Fee\\* \\p{Sc}(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
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
