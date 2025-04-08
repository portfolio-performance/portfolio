package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
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

@SuppressWarnings("nls")
public class HargreavesLansdownPlcExtractor extends AbstractPDFExtractor
{
    // @formatter:off
    // Information:
    // The currency of Hargreaves Lansdown Asset Management Limited is always GBP.
    //
    // All security currencies are GPB.
    // However, the quote per share is in GPX.
    //
    // @formatter:off

    public HargreavesLansdownPlcExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hargreaves Lansdown Asset Management Limited");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Hargreaves Lansdown Asset Management Limited";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Contract Note");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^A\\/C Designation :.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "sold" change from BUY to SELL
                .section("type").optional()
                .match("^We have today on your instructions ([\\s]+)?\\*\\*([\\s]+)?"
                                + "(?<type>(B([\\s]+)?O([\\s]+)?U([\\s]+)?G([\\s]+)?H([\\s]+)?T|"
                                + "S([\\s]+)?O([\\s]+)?L([\\s]+)?D))"
                                + "([\\s]+)?\\*\\* ([\\s]+)?the security detailed below\\.$")
                .assign((t, v) -> {
                    if ("SOLD".equals(stripBlanks(v.get("type"))))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // IE00B4X9L533 STOCK CODE: HMWO
                // HSBC ETFs Plc
                // 2,539.00 MSCI World ETF GBP 2267.4179 57,569.74
                // @formatter:on
                .section("isin", "tickerSymbol", "name", "nameContinued").optional()
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) STOCK CODE: (?<tickerSymbol>.*)$")
                .match("^(?<name>.*)$")
                .match("^[\\.,\\d]+ (?<nameContinued>.*) [\\.,\\d]+ [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("currency", asCurrencyCode("GBP"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // GB00BG0QPJ30
                // Legal & General UK Index
                // 1,043.478 Class C - Accumulation (GBP) 287.500000 3,000.00
                // @formatter:on
                .section("isin", "name", "nameContinued").optional()
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(?<name>.*)$")
                .match("^[\\.,\\d]+ (?<nameContinued>.*) [\\.,\\d]+ [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("currency", asCurrencyCode("GBP"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 2,539.00 MSCI World ETF GBP 2267.4179 57,569.74
                .section("shares")
                .match("^(?<shares>[\\.,\\d]+) .* [\\.,\\d]+ [\\.,\\d]+$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Date 15/10/2021 Time 09:47 Contract Note No. B105372223-01000000
                // Date 25 /01/202 2                                      Time  09:35                       Contract Note No. S186039383-01000000
                // @formatter:on
                .section("date", "time")
                .match("^Date (?<date>[\\d\\s]+\\/[\\d\\s]+\\/[\\d\\s]+) ([\\s]+)?Time ([\\s]+)?(?<time>[\\d]{2}\\:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")), v.get("time"))))

                // HL SIPP Settlement Date: 19/10/2021 57,581.69
                .section("amount")
                .match("^.*: [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode("GBP"));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Date 15/10/2021 Time 09:47 Contract Note No. B105372223-01000000
                // Date 25 /01/202 2                                      Time  09:35                       Contract Note No. S186039383-01000000
                // @formatter:on
                .section("note").optional()
                .match("^Date .* Time .* Contract Note No\\. (?<note>.*)$")
                .assign((t, v) -> t.setNote("Ref.: " + trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Commission 11.95
                .section("fee").optional()
                .match("^Commission (?<fee>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    v.put("currency", asCurrencyCode("GBP"));
                    processFeeEntries(t, v, type);
                });
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "UK");
    }
}