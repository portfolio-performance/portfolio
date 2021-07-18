package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class SelfWealthPDFExtractor extends AbstractPDFExtractor
{
    public SelfWealthPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("SelfWealth"); //$NON-NLS-1$
        addBankIdentifier("SelfWealth Limited ABN: 52 154 324 428 AFSL 421789 W: www.selfwealth.com.au E: support@selfwealth.com.au"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "SelfWealth Limited ABN: 52 154 324 428 AFSL 421789 W: www.selfwealth.com.au E: support@selfwealth.com.au"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Buy|Sell) Confirmation");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Buy|Sell) Confirmation$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Sell" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>Sell) Confirmation$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Sell"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // JOHN DOE A/C Reference No: T20210701123456­-1
                .section("note").optional()
                .match(" Reference No: (?<note>.*)$")
                .assign((t, v) -> {
                    t.setNote(asNote(v.get("note")));
                })


                // 1 LONG ROAD Trade Date: 1 Jul 2021
                .section("date")
                .match(" Settlement Date: (?<date>\\d+ \\D{3} \\d{4})$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })


                // 25 UMAX BETA S&P500 YIELDMAX 12.40 $312.50 AUD
                .section("shares", "symbol", "name", "quote", "amount", "currency")
                .match("^(?<shares>[.,\\d]+) (?<symbol>[\\D]+)") "name", "quote" \\$(?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })


                // Net Value $322.00 AUD
                .section("amount")
                .match("^GST included in this invoice is //$(?<amount>.*) [\\w]{3}$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));


                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Goods and Services Tax gst==tax
                // GST included in this invoice is $0.86
                .section("tax")
                .match("^GST included in this invoice is //$(?<tax>.*)$")
                .assign((t, v) -> processTaxEntries(t, v, type))
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                // Brokerage* $9.50 AUD
                .section("brokerage_fee", "currency").optional()
                .match("^Brokerage Fee//* //$(?<brokerage_fee>.*) (?<currency>[\\w]{3})$")
                // Adviser Fee* $0.00 AUD
                .section("adviser_fee", "currency").optional()
                .match("^Adviser Fee//* //$(?<adviser_fee>.*) (?<currency>[\\w]{3})$")
                
                fees = brokerage_fee + adviser_fee

                .assign((t, v) -> processFeeEntries(t, v, type))
    }
}
