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

@SuppressWarnings("nls")
public class CommSecPDFExtractor extends AbstractPDFExtractor
{
    public CommSecPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Commonwealth Securities Limited");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Commonwealth Securities Limited";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("WE HAVE (SOLD|BOUGHT)", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // CONSIDERATION (AUD): $999.97 CONTRACT COMMENTS:
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^WE HAVE (SOLD|BOUGHT) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "SOLD" change from BUY to SELL
                        .section("type").optional() //
                        .match("^WE HAVE (?<type>SOLD) .*$") //
                        .assign((t, v) -> {
                            if ("SOLD".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // COMPANY: QANTAS AIRWAYS LIMITED
                        // QAN
                        // CONSIDERATION (AUD): $999.97 CONTRACT COMMENTS:
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency").optional() //
                        .match("^COMPANY: (?<name>.*)$") //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                        .match("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // COMPANY WISETECH GLOBAL LIMITED
                        // SECURITY ORDINARY FULLY PAID WTC
                        // CONSIDERATION (AUD): $28,060.00 PID XXXX HIN XXXXXXX
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency").optional() //
                        .match("^COMPANY (?<name>.*)$") //
                        .match("^SECURITY ORDINARY FULLY PAID (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)$") //
                        .match("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // AS AT DATE: 20/04/2020 277 3.610000
                        // @formatter:on
                        .section("date") //
                        .match("^AS AT DATE: (?<date>[\\d]+\\/[\\d]+\\/[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // AS AT DATE: 20/04/2020 277 3.610000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^AS AT DATE: .* (?<shares>[\\.,\\d]+) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // CONFIRMATION NO: XXXXXXX 1,000 28.060000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^CONFIRMATION NO: .* (?<shares>[\\.,\\d]+) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // TOTAL COST: $1,092.92
                        // NET PROCEEDS: $28,031.94
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^(TOTAL COST|NET PROCEEDS): \\p{Sc}(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // ORDER NO: N118818020
                        // @formatter:on
                        .section("note").optional() //
                        .match("^ORDER NO: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Order No: " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        // @formatter:off
        // The "TOTAL GST" is already included in the brokerage
        // (BROKERAGE & COSTS INCL GST) and is therefore not imported as tax.
        // @formatter:on
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // BROKERAGE & COSTS INCL GST: $29.95 55685147 0404181685
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^BROKERAGE & COSTS INCL GST: \\p{Sc}(?<fee>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // APPLICATION MONEY: $0.00
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^APPLICATION MONEY: \\p{Sc}(?<fee>[\\.,\\d]+)$") //
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
