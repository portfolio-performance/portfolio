package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.RoundingMode;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Morgan Stanley Smith Barney LLC (Global Stock Plan Services) is a US-based financial services company.
 *           The currency is USD --> $.
 *
 * @implSpec All security currencies are USD --> $.
 *           The CUSIP number is the WKN number with 9 letters.
 *
 *           Dividend reinvestment confirmations contain the dividend and the purchase of the net dividend.
 *           Both transactions are created from the same document.
 *           The dividend is booked on the settlement date, the purchase on the trade date.
 *           The number of shares of the dividend is the share balance before the reinvestment.
 *
 *           Release detail reports (restricted stock units) are booked as delivery inbound
 *           of the net quantity at the fair market value (FMV) at vest.
 *           The residual balance is ignored.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class MorganStanleyPDFExtractor extends AbstractPDFExtractor
{
    public MorganStanleyPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Morgan Stanley Smith Barney LLC");

        addDividendReinvestmentTransaction();
        addReleaseTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Morgan Stanley Smith Barney LLC";
    }

    private void addDividendReinvestmentTransaction()
    {
        final var type = new DocumentType("Dividend Reinvestment", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // CUSIP: 775265677 Order Reference #: 0845006
                                        // @formatter:on
                                        .section("wkn").optional() //
                                        .match("^CUSIP:[\\s]+(?<wkn>[A-Z0-9]{9})([\\s].*)?$") //
                                        .assign((ctx, v) -> ctx.put("wkn", v.get("wkn"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // You Bought 0.267 shares at $136.1234 on Trade Date 13-Jun-2022
        // @formatter:on
        var dividendBlock = new Block("^You Bought [\\.,\\d]+ shares at \\p{Sc}[\\.,\\d]+ on Trade Date .*$");
        type.addBlock(dividendBlock);
        var dividendTransaction = new Transaction<AccountTransaction>();
        dividendBlock.set(dividendTransaction);

        dividendTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Security Name: ABC ABC ABC ABC Gross Proceeds: $47.85
                        // Trading Symbol: ABC Less Transaction Expense: $11.48
                        // @formatter:on
                        .section("name", "currency", "tickerSymbol") //
                        .documentContextOptionally("wkn") //
                        .match("^Security Name: (?<name>.*) Gross Proceeds: (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .match("^Trading Symbol: (?<tickerSymbol>[A-Za-z0-9]{1,6}(?:\\.[A-Za-z]{1,4})?) Less Transaction Expense: .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Current Share Balance: 29.000
                        // @formatter:on
                        .section("shares") //
                        .match("^Current Share Balance: (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Settlement Date: 15-Jun-2022
                        // @formatter:on
                        .section("date") //
                        .match("^Settlement Date: (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Net Proceeds: $36.37
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Net Proceeds: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(dividendTransaction, type);

        // @formatter:off
        // You Bought 0.267 shares at $136.1234 on Trade Date 13-Jun-2022
        // @formatter:on
        var buyBlock = new Block("^You Bought [\\.,\\d]+ shares at \\p{Sc}[\\.,\\d]+ on Trade Date .*$");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Security Name: ABC ABC ABC ABC Gross Proceeds: $47.85
                        // Trading Symbol: ABC Less Transaction Expense: $11.48
                        // @formatter:on
                        .section("name", "currency", "tickerSymbol") //
                        .documentContextOptionally("wkn") //
                        .match("^Security Name: (?<name>.*) Gross Proceeds: (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .match("^Trading Symbol: (?<tickerSymbol>[A-Za-z0-9]{1,6}(?:\\.[A-Za-z]{1,4})?) Less Transaction Expense: .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // You Bought 0.267 shares at $136.1234 on Trade Date 13-Jun-2022
                        // @formatter:on
                        .section("shares") //
                        .match("^You Bought (?<shares>[\\.,\\d]+) shares at \\p{Sc}[\\.,\\d]+ on Trade Date .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // You Bought 0.267 shares at $136.1234 on Trade Date 13-Jun-2022
                        // @formatter:on
                        .section("date") //
                        .match("^You Bought [\\.,\\d]+ shares at \\p{Sc}[\\.,\\d]+ on Trade Date (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Net Proceeds: $36.37
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Net Proceeds: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // CUSIP: 775265677 Order Reference #: 0845006
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Order Reference #: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new));
    }

    private void addReleaseTransaction()
    {
        final var type = new DocumentType("Release Detail Report", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // CUSIP: 728031340
                                        // @formatter:on
                                        .section("wkn").optional() //
                                        .match("^CUSIP:[\\s]+(?<wkn>[A-Z0-9]{9})([\\s].*)?$") //
                                        .assign((ctx, v) -> ctx.put("wkn", v.get("wkn"))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^Summary for Release$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND))

                        // @formatter:off
                        // Security Name: BQzW twIsSBkn AwakjMDU lMih Withheld Quantity: 1.0000
                        // Trading Symbol: ygl x Withheld Quantity Value Per Share: $133.77
                        // *FMV @ Vest: $133.7650
                        //
                        // Security Name: NspX UnMmjcPt GRmgNwoa WccH Withheld Quantity: 3.0000
                        // Trading Symbol: TSf
                        // *FMV @ Vest / FMV Date: $140.5250 / 01-Dec-2015
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency") //
                        .documentContextOptionally("wkn") //
                        .match("^Security Name: (?<name>.*) Withheld Quantity: [\\.,\\d]+$") //
                        .match("^Trading Symbol: (?<tickerSymbol>[A-Za-z0-9]{1,6}(?:\\.[A-Za-z]{1,4})?)( .*)?$") //
                        .match("^\\*FMV @ Vest.*: (?<currency>\\p{Sc})[\\.,\\d]+.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Net Quantity: 1.0000
                        // @formatter:on
                        .section("shares") //
                        .match("^Net Quantity: (?<shares>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Release Date: 01-May-2022 **Total Tax Amount Due: $133.77
                        // Release Date: 01-Dec-2015 Federal Tax 42.8574 % $421.58
                        // @formatter:on
                        .section("date") //
                        .match("^Release Date: (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})( .*)?$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Net Quantity: 1.0000
                        // *FMV @ Vest: $133.7650
                        //
                        // Net Quantity: 4.000000
                        // *FMV @ Vest / FMV Date: $140.5250 / 01-Dec-2015
                        // @formatter:on
                        .section("shares", "currency", "fmv") //
                        .match("^Net Quantity: (?<shares>[\\.,\\d]+)[\\s]*$") //
                        .match("^\\*FMV @ Vest.*: (?<currency>\\p{Sc})(?<fmv>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            // Value of the delivery = net quantity x FMV at vest
                            var shares = ExtractorUtils.convertToNumberBigDecimal(v.get("shares"), Values.Share, "en", "US");
                            var fmv = ExtractorUtils.convertToNumberBigDecimal(v.get("fmv"), Values.Share, "en", "US");
                            var amount = shares.multiply(fmv).setScale(Values.Amount.precision(), RoundingMode.HALF_UP);

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(amount.multiply(Values.Amount.getBigDecimalFactor()).longValue());
                        })

                        // @formatter:off
                        // Award ID: 80824152 Tax % Tax Paid
                        // Award ID: 88382462
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award ID: [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // IRS Backup Withholding: $11.48
                        // IRS Nonresident Alien Withholding: $7.52
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^IRS .*Withholding: (?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
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
