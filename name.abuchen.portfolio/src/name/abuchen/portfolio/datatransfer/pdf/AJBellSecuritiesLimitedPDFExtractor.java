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
 * @formatter:off
 * @implNote AJ Bell Securities Limited is a £-based financial services company.
 *           The currency is GBP --> £.
 *
 * @implSpec All security currencies are GBP --> £.
 *           The SEDOL number is the WKN number with 7 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class AJBellSecuritiesLimitedPDFExtractor extends AbstractPDFExtractor
{
    public AJBellSecuritiesLimitedPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("AJ Bell Securities Limited"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "AJ Bell Securities Limited / AJ Bell Youinvest"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("CONTRACT NOTE");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Account No\\. .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "sold" change from BUY to SELL
                        .section("type").optional() //
                        .match("^We have (?<type>(bought|sold)) for you as agent$") //
                        .assign((t, v) -> {
                            if ("sold".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                        // LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I
                        // ACC
                        // XOFF            17,940.965 0.5573         9,998.50 GBP
                        // @formatter:on
                        .section("wkn", "name", "name1", "currency") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\.[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold).* (?<wkn>[A-Z0-9]{7}) [\\w]+$") //
                        .find("We have (bought|sold) for you as agent") //
                        .match("^(?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^.*([\\s]{1,})[\\.,\\d]+ [\\.,\\d]+([\\s]{1,})[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("Venue"))
                                v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // XOFF            17,940.965 0.5573         9,998.50 GBP
                        // XLON                   470 67.3483        31,653.70 GBP
                        // @formatter:on
                        .section("shares") //
                        .match("^.*([\\s]{1,})(?<shares>[\\.,\\d]+) [\\.,\\d]+([\\s]{1,})[\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) (?<time>[\\d]{2}\\.[\\d]{2}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total debit     10,000.00 GBP
                        // Total credit     15,783.20 GBP
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total (debit|credit)([\\s]{1,})(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\.[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold)([\\s]{1,})[A-Z0-9]{7} (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ref. No. " + trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Dealing charge 1.50 GBP
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Dealing charge([\\s]{1,})(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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
