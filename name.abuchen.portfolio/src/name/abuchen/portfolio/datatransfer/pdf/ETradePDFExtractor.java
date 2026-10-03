package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Locale;

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
        addBankIdentifier("E*TRADE from Morgan Stanley");

        addBuySellTransaction();
        addTradeConfirmationTransaction();
        addEmployeeStockPlanReleaseTransaction();
    }

    @Override
    public String getLabel()
    {
        return "E*TRADE Securities LLC";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Purchase Summary");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^EMPLOYEE STOCK PLAN PURCHASE CONFIRMATION$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Company Name (Symbol) NXP SEMICONDUCTORS, Beginning Balance 0.0000
                        // N.V.(NXPI) Shares Purchased 5.2350
                        // Grant Date Market Value $215.590000
                        // @formatter:on
                        .section("name", "nameContinued", "tickerSymbol", "currency") //
                        .match("^Company Name \\(Symbol\\) (?<name>.*),.*$") //
                        .match("^(?<nameContinued>.*)\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Shares Purchased [\\.,\\d]+$") //
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
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

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

    private void addTradeConfirmationTransaction()
    {
        final var type = new DocumentType("Transaction Type: Sold");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Trade Date Settlement Date Quantity Price Settlement Amount$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // Net Amount $24,902.94
                        // Transaction Type: Sold
                        // Description: INTEL CORP
                        // Symbol / CUSIP / ISIN: lFPl / 700282723 / US4581401001
                        // @formatter:on
                        .section("currency", "name", "isin") //
                        .match("^Net Amount (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .match("^Description: (?<name>.*)$") //
                        .match("^Symbol \\/ CUSIP \\/ ISIN: .* \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Trade Date Settlement Date Quantity Price Settlement Amount
                        // 09/18/2025 09/19/2025 796 31.2851 Principal $24,902.94
                        // @formatter:on
                        .section("date", "shares") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<shares>[\\.,\\d]+) [\\.,\\d]+ Principal \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date"), Locale.US));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Net Amount $24,902.94
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Net Amount (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addEmployeeStockPlanReleaseTransaction()
    {
        final var type = new DocumentType("EMPLOYEE STOCK PLAN RELEASE CONFIRMATION");
        this.addDocumentTyp(type);

        // The release confirmation repeats its header on the second page,
        // which only contains the tax details in local currency. Therefore the
        // block must end with the cash distribution summary of the first page.

        // @formatter:off
        // Award Shares 34.0000 Cash Distribution
        // Shares Traded (17.5190) Trade Value $1,230.10
        // Shares Issued 16.4810 Total Tax ($1,230.10)
        // Total Due Participant $0.00
        // @formatter:on

        // Release of the awarded shares at market value
        var buyBlock = new Block("^EMPLOYEE STOCK PLAN RELEASE CONFIRMATION$", "^Total Due Participant .*$");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Company Name (Symbol) noaQU ppixWWH, INC. - NEW Market Value Per Share $70.215000
                        // (XCWE) Award Price Per Share $0.000000
                        // @formatter:on
                        .section("name", "currency", "tickerSymbol") //
                        .match("^Company Name \\(Symbol\\) (?<name>.*) Market Value Per Share (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Award Price Per Share \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Tax Payment Method Withhold Shares Shares Released 34.0000
                        // @formatter:on
                        .section("shares") //
                        .match("^.* Shares Released (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Account Number 305637505 Release Date 08-10-2025
                        // @formatter:on
                        .section("date") //
                        .match("^.* Release Date (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Market Value $2,387.31 Taxable Gain $ Rate % Amount $
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Market Value (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Award Number 4414241
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award Number [\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new));

        // Sale of the withheld shares to pay the taxes
        var sellBlock = new Block("^EMPLOYEE STOCK PLAN RELEASE CONFIRMATION$", "^Total Due Participant .*$");
        type.addBlock(sellBlock);
        sellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // Company Name (Symbol) noaQU ppixWWH, INC. - NEW Market Value Per Share $70.215000
                        // (XCWE) Award Price Per Share $0.000000
                        // @formatter:on
                        .section("name", "currency", "tickerSymbol") //
                        .match("^Company Name \\(Symbol\\) (?<name>.*) Market Value Per Share (?<currency>\\p{Sc})[\\.,\\d]+$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) Award Price Per Share \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Shares Traded (17.5190) Trade Value $1,230.10
                        // @formatter:on
                        .section("shares") //
                        .match("^Shares Traded \\((?<shares>[\\.,\\d]+)\\) Trade Value \\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Account Number 305637505 Release Date 08-10-2025
                        // @formatter:on
                        .section("date") //
                        .match("^.* Release Date (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Total Due Participant $0.00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total Due Participant (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Shares Issued 16.4810 Total Tax ($1,230.10)
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Shares Issued [\\.,\\d]+ Total Tax \\((?<currency>\\p{Sc})(?<tax>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Award Number 4414241
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award Number [\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new));
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
