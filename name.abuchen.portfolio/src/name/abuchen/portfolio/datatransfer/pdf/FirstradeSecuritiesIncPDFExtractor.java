package name.abuchen.portfolio.datatransfer.pdf;

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
 * @implNote Firstrade Securities Inc. is a US-based financial services company.
 *           The currency is USD --> $.
 *
 * @implSpec All security currencies are USD --> $.
  *          The CUSIP number is the WKN number with 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class FirstradeSecuritiesIncPDFExtractor extends AbstractPDFExtractor
{
    public FirstradeSecuritiesIncPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIRSTRADE HOUSE");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Firstrade Securities Inc.";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("TRADE SETTLEMENT ACCT");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^YOU (BOUGHT|SOLD) .*$");
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
                        .match("^YOU (?<type>(BOUGHT|SOLD)).*$") //
                        .assign((t, v) -> {
                            if ("SOLD".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf(
                                        // @formatter:off
                                        // YOU SOLD TSLA 88160R101 11/22/21 11/24/21 SHORT 1 $1,156.03000
                                        // TESLA INC COMMON STOCK PRINCIPAL $1,156.03
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "wkn", "name", "currency") //
                                                        .match("^YOU (BOUGHT|SOLD) (?<tickerSymbol>[A-Z0-9]{3,4}) (?<wkn>[A-Z0-9]{9}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} .* [\\.,\\d] (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .match("^(?<name>.*) PRINCIPAL \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // YOU SOLD 8SFXJX7 11/23/21 11/24/21 MARGIN 1 $4.40000
                                        // PUT PYPL 11/26/21 190 PAYPAL HOLDINGS INC PRINCIPAL $440.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "tickerSymbol", "name", "currency") //
                                                        .match("^YOU (BOUGHT|SOLD) (?<wkn>[A-Z0-9]{7}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} .* (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .match("^.* (?<tickerSymbol>[A-Z0-9]{3,4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{1,} (?<name>.*) PRINCIPAL \\p{Sc}[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // YOU SOLD TSLA 88160R101 11/22/21 11/24/21 SHORT 1 $1,156.03000
                        // YOU SOLD 8SFXJX7 11/23/21 11/24/21 MARGIN 1 $4.40000
                        // @formatter:on
                        .section("shares") //
                        .match("^YOU (BOUGHT|SOLD) .* (?<shares>[\\.,\\d]) \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // YOU SOLD TSLA 88160R101 11/22/21 11/24/21 SHORT 1 $1,156.03000
                        // YOU SOLD 8SFXJX7 11/23/21 11/24/21 MARGIN 1 $4.40000
                        // @formatter:on
                        .section("date") //
                        .match("^YOU (BOUGHT|SOLD) .* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) .* [\\.,\\d] \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // SHORT. NET AMOUNT $1,156.02
                        // NET AMOUNT $439.97
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^.*NET AMOUNT (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
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
                        // UNSOLICITED FEES $0.01
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^UNSOLICITED FEES (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // UNSOLICITED OPTION FEE $0.02
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^UNSOLICITED OPTION FEE (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // CLOSING CONTRACT FEES $0.01
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^CLOSING CONTRACT FEES (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)$") //
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
