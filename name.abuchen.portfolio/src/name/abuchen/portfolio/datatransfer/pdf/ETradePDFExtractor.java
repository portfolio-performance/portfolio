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

@SuppressWarnings("nls")
public class ETradePDFExtractor extends AbstractPDFExtractor
{
    public ETradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("E*TRADE Securities LLC");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "E*TRADE Securities LLC";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Purchase Summary");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^EMPLOYEE STOCK PLAN PURCHASE CONFIRMATION$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Company Name (Symbol) NXP SEMICONDUCTORS, Beginning Balance 0.0000
                        // N.V.(NXPI) Shares Purchased 5.2350
                        // Grant Date Market Value $215.590000
                        // @formatter:on
                        .section("name", "nameContinued", "tickerSymbol", "currency") //
                        .match("^Company Name \\(Symbol\\) (?<name>.*),.*$") //
                        .match("^(?<nameContinued>.*)\\((?<tickerSymbol>[\\w]{3,4})\\) Shares Purchased [\\.,\\d]+$") //
                        .match("^Grant Date Market Value (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // N.V.(NXPI) Shares Purchased 5.2350
                        // @formatter:on
                        .section("shares") //
                        .match("^.*\\([\\w]{3,4}\\) Shares Purchased (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Purchase Date 02-28-2025
                        // @formatter:on
                        .section("date") //
                        .match("^Purchase Date (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Total Price ($959.31)
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total Price \\((?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Taxable Gain $169.30
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Taxable Gain .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
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
