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
 * @implNote Aviva Pension Trustees UK Limited is a £-based financial services company.
 *           The currency is GBP --> £.
 *
 * @implSpec All security currencies are GBP --> £.
 *           The SEDOL number is the WKN number with 7 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class AvivaPLCPDFExtractor extends AbstractPDFExtractor
{
    public AvivaPLCPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Aviva Pension Trustees UK Limited");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Aviva Pension Trustees UK Limited";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("You have (PURCHASED|SOLD)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^You have (PURCHASED|SOLD).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "SOLD" change from BUY to SELL
                        .section("type").optional() //
                        .match("^You have (?<type>(PURCHASED|SOLD)).*$") //
                        .assign((t, v) -> {
                            if ("SOLD".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Investment Name: Vanguard US Equity Index I£
                                        // ISIN: GB00B5B74S01
                                        // SEDOL: B5B74S0
                                        // Citicode: FPD4.LN
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "wkn", "tickerSymbol") //
                                                        .match("^Investment Name: (?<name>.*)$") //
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^SEDOL: (?<wkn>[A-Z0-9]{7})$") //
                                                        .match("^Citicode: (?<tickerSymbol>[\\w]{3,4})\\..*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode("GBP"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Investment Name: Av MyM My Future Growth
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name") //
                                                        .match("^Investment Name: (?<name>.*)$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode("GBP"));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Number of units: 1.5661
                        // @formatter:on
                        .section("shares")
                        .match("^Number of units: (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Execution date/time: 02 Nov 2023 21:00:00.000
                        // Execution date: 07 Mar 2023 23:59:00.000
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Execution date(\\/time)?: (?<date>[\\d]{2} .* [\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Total Consideration £999.99
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .match("^Total Consideration \\p{Sc}(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode("GBP"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Consideration: £1,777.41
                                        // Consideration £5.06
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .match("^Consideration([:])? \\p{Sc}(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode("GBP"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))
                        // @formatter:off
                        // Order reference: ############
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Order reference: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "UK");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "UK");
    }
}
