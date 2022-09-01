package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AJBellSecuritiesLimitedPDFExtractor extends AbstractPDFExtractor
{
    // @formatter:off
    // Information:
    // The securities are identified by Sedol number. 
    // https://en.wikipedia.org/wiki/SEDOL
    //
    // @formatter:off

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
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Account No\\. .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "sold" change from BUY to SELL
                .section("type").optional()
                .match("^We have (?<type>(bought|sold)) for you as agent$")
                .assign((t, v) -> {
                    if (v.get("type").equals("sold"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                // LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I
                // ACC
                // XOFF            17,940.965 0.5573         9,998.50 GBP
                // @formatter:on
                .section("sedol", "name", "name1", "currency")
                .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\.[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold).* (?<sedol>[\\w]+) [\\w]+$")
                .find("We have (bought|sold) for you as agent")
                .match("^(?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^.* [\\s]+[\\.,\\d]+ [\\.,\\d]+ [\\s]+[\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Venue"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // XOFF            17,940.965 0.5573         9,998.50 GBP
                // XLON                   470 67.3483        31,653.70 GBP
                // @formatter:on
                .section("shares")
                .match("^.* [\\s]+(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\s]+[\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                .section("date", "time")
                .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{2}) (?<time>[\\d]{2}\\.[\\d]{2}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // @formatter:off
                // Total debit     10,000.00 GBP
                // Total credit     15,783.20 GBP
                // @formatter:on
                .section("currency", "amount")
                .match("^Total (debit|credit) [\\s]+(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // 05/12/19 13.15 11/12/19 Bought BF41Q72 C5L6DQ
                .section("note").optional()
                .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{2} [\\d]{2}\\.[\\d]{2} [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} (Bought|Sold).* [\\w]{7} (?<note>.*)$")
                .assign((t, v) -> t.setNote("Ref.: " + trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Dealing charge          1.50 GBP
                .section("fee", "currency").optional()
                .match("^Dealing charge [\\s]+(?<fee>[\\.,'\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "UK");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "en", "UK");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "UK");
    }
}
