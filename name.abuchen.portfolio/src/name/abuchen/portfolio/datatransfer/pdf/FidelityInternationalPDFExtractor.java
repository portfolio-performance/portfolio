package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

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
 * @implNote Fidelity International is a $-based financial services company.
 *           The currency is USD --> $.
 *
 * @implSpec All security currencies are USD --> $.
 *           The CUSIP number is the WKN number.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class FidelityInternationalPDFExtractor extends AbstractPDFExtractor
{
    public FidelityInternationalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIDELITY BROKERAGE SERVICES LLC"); //$NON-NLS-1$
        addBankIdentifier("Fidelity Stock Plan Services"); //$NON-NLS-1$

        addBuySellTransaction();
        addSummaryStatementBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Fidelity International Ltd."; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("YOU (PURCHASED|SOLD)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^PARTICIPANT NO\\.$");
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
                        .match("^YOU (?<type>(PURCHASED|SOLD)) [\\.,\\d]+.*$") //
                        .assign((t, v) -> {
                            if ("SOLD".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // U98299884 1 000 12-08-23 12-12-23 0D6SVL 31620M106
                                        // YOU PURCHASED 7.5146 AT $58.9290 PURCHASE PRICE Gain²          $0.00
                                        // SECURITY DESCRIPTION SYMBOL: FIS EXPLANATION OF PROCEEDS
                                        // FIDELITY NATL
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "tickerSymbol", "currency", "name") //
                                                        .match("^.* [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\w]+ (?<wkn>[A-Z0-9]+)$") //
                                                        .match("^YOU PURCHASED [\\.,\\d]+ AT (?<currency>\\p{Sc})[\\.,\\d]+.*$") //
                                                        .match("^SECURITY DESCRIPTION SYMBOL: (?<tickerSymbol>[A-Z]{2,}) .*$") //
                                                        .match("^(?<name>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // I00123456 1 WI# 12-12-23 12-14-23 K9T1Q9 11135F101
                                        // YOU SOLD 14 AT 173.1100
                                        // SECURITY DESCRIPTION SYMBOL: AAPL Sale Proceeds     $2,423.54
                                        // APPLE INC
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "tickerSymbol", "currency", "name") //
                                                        .match("^.* [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\w]+ (?<wkn>[A-Z0-9]+)$") //
                                                        .match("^YOU SOLD [\\.,\\d]+.*$") //
                                                        .match("^SECURITY DESCRIPTION SYMBOL: (?<tickerSymbol>[A-Z]{2,}) .* (?<currency>\\p{Sc})[\\.,\\d]+$") //
                                                        .match("^(?<name>.*)$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Sale Date: DEC/12/2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Sale Date: (?<date>[\\w]{3}\\/[\\d]{2}\\/[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))),
                                        // @formatter:off
                                        // FIS ESPP on DEC/08/2023. PURCHASE INFORMATION
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.* (?<date>[\\w]{3}\\/[\\d]{2}\\/[\\d]{4})\\. PURCHASE INFORMATION$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // YOU SOLD 14 AT 173.1100
                        // YOU PURCHASED 7.5146 AT $58.9290 PURCHASE PRICE Gain²          $0.00
                        // @formatter:on
                        .section("shares") //
                        .match("^YOU (PURCHASED|SOLD) (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Net Cash Proceeds¹     -$2,423.41
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Net Cash Proceeds.* \\-(?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Accumulated Contributions*        $442.83
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Accumulated Contributions.* (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // REF # 23346-K9T1Q9
                        // @formatter:on
                        .section("note").optional() //
                        .match("^REF # (?<note>[\\w]+\\-[\\w]+)$") //
                        .assign((t, v) -> t.setNote("Ref. No. " + v.get("note")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSummaryStatementBuySellTransaction()
    {
        DocumentType type = new DocumentType("Transaction Confirmation");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^REFERENCE NO\\. .*$");
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
                        .match("^You (?<type>(Bought|Sold)) .*$") //
                        .assign((t, v) -> {
                            if ("Sold".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 12234-8OPOP 1* WO# 12-14-23 12-18-23 113004105 12345-I8ZZZ
                                        // You Sold BROOKFIELD ASSET MANAGEMENT LTD CLASS Principal Amount           1.25
                                        //       .032 A LTD VOTING SHS ISIN #CA1130041058 Settlement Amount           1.25
                                        // Symbol: WE HAVE ACTED AS AGENT.
                                        // BAM LOTS WITHOUT SPECIFIC SHARES
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "name", "isin", "tickerSymbol") //
                                                        .match("^[A-Z0-9]+\\-[A-Z0-9]+ .* [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} (?<wkn>[A-Z0-9]+) [A-Z0-9]+\\-[A-Z0-9]+$") //
                                                        .match("^You (Bought|Sold) (?<name>.*) Principal Amount([\\s]{1,})[\\.,\\d]+$") //
                                                        .match("^.* ISIN #(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                                                        .find("Symbol:.*") //
                                                        .match("^(?<tickerSymbol>[A-Z]{2,5}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode("USD"));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 12234-8OPOP 1* WO# 12-14-23 12-18-23 113004105 12345-I8ZZZ
                                        // You Bought NUTRIEN LTD COM NPV ISIN #CA67077M1086 Principal Amount       4,427.59
                                        //        80 SEDOL #BDRJLN0 Settlement Amount       4,427.59
                                        // Symbol:
                                        // NTR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "name", "isin", "tickerSymbol") //
                                                        .match("^[A-Z0-9]+\\-[A-Z0-9]+ .* [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} (?<wkn>[A-Z0-9]+) [A-Z0-9]+\\-[A-Z0-9]+$") //
                                                        .match("^You (Bought|Sold) (?<name>.*) ISIN #(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Principal Amount([\\s]{1,})[\\.,\\d]+$") //
                                                        .find("Symbol:.*") //
                                                        .match("^(?<tickerSymbol>[A-Z]{2,5}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode("USD"));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // 20123-1XXXXX 1* WK# 01-04-21 01-06-21 46428Q109 20123-XXXXX
                                        // You Bought ISHARES SILVER TR ISHARES Principal Amount       1,011.60
                                        // Symbol: exclusive, long-term marketing program that includes the promotion of this security and
                                        // SLV other iShares funds, and the inclusion of iShares funds in certain platforms and investment
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("wkn", "name", "tickerSymbol") //
                                                        .match("^[A-Z0-9]+\\-[A-Z0-9]+ .* [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} (?<wkn>[A-Z0-9]+) [A-Z0-9]+\\-[A-Z0-9]+$") //
                                                        .match("^You (Bought|Sold) (?<name>.*) Principal Amount([\\s]{1,})[\\.,\\d]+$") //
                                                        .find("Symbol:.*") //
                                                        .match("^(?<tickerSymbol>[A-Z]{2,5}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode("USD"));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // 20123-1XXXXX 1* WK# 01-04-21 01-06-21 46428Q109 20123-XXXXX
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z0-9]+\\-[A-Z0-9]+ .* (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{2}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ [A-Z0-9]+\\-[A-Z0-9]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // You Bought ISHARES SILVER TR ISHARES Principal Amount       1,011.60
                        //        40 WE HAVE ACTED AS AGENT. Settlement Amount       1,011.60
                        // @formatter:on
                        .section("shares") //
                        .find("You (Bought|Sold) .*") //
                        .match("^[\\s]+ (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // You Bought ISHARES SILVER TR ISHARES Principal Amount       1,011.60
                        //        40 WE HAVE ACTED AS AGENT. Settlement Amount       1,011.60
                        // @formatter:on
                        .section("amount") //
                        .find("You (Bought|Sold) .*") //
                        .match("^.* Settlement Amount([\\s]{1,})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode("USD"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 20123-1XXXXX 1* WK# 01-04-21 01-06-21 46428Q109 20123-XXXXX
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\w]+\\-[\\w]+) .*$") //
                        .assign((t, v) -> t.setNote("Ref. No. " + v.get("note")))

                        // @formatter:off
                        // 20123-1XXXXX 1* WK# 01-04-21 01-06-21 46428Q109 20123-XXXXX
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\w]+\\-[\\w]+ .* (?<note>[\\w]+\\-[\\w]+)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | Ord. No. ")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // You Sold BROOKFIELD ASSET MANAGEMENT LTD CLASS Principal Amount       7,935.27
                        //       203 A LTD VOTING SHS ISIN #CA1130041058 Activity Assessmen t  F e e     0.07
                        // @formatter:on
                        .section("fee").optional() //
                        .find("You (Bought|Sold) .*") //
                        .match("^.* Activity .*([\\s]{1,})(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode("USD"));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // DETAILS: Total Fees          $0.13
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^DETAILS: Total Fees([\\s]{1,})(?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)$") //
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
