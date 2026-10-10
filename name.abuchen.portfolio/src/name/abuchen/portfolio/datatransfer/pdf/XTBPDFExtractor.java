package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote XTB S.A. only provides a combined account report as PDF. It always contains the sections
 *           CLOSED POSITION HISTORY, OPEN POSITION HISTORY, PENDING ORDERS HISTORY and CASH OPERATION HISTORY.
 *           Only the CASH OPERATION HISTORY is imported, the other sections are holdings snapshots.
 *
 *           The report does not contain the name of the broker. It is identified by its header line.
 *           All amounts are formatted in US number format and are booked in the account currency.
 *           The report contains neither ISIN nor security names, only the XTB ticker symbol.
 *           Foreign currency instruments (e.g. DFNS.UK) are booked in the account currency without
 *           any exchange rate, therefore all securities are created in the account currency.
 *
 *           The report combines the main account and its sub-accounts (investment plans).
 *           The transfers between those accounts are skipped as not supported.
 *
 *           A sale consists of two lines with the same time stamp: "close trade" (profit) and "Stock sale" (purchase value).
 *           The proceeds of the sale are the sum of both amounts.
 *
 *           Dividends and the withholding tax as well as interest and the interest tax are separate lines.
 *           In postProcessing, they are merged into one transaction.
 *           {@code
 *              mergeDividendsWithWithholdingTax(List<Item> items)
 *              mergeInterestWithInterestTax(List<Item> items)
 *           }
 * @formatter:on
 */
@SuppressWarnings("nls")
public class XTBPDFExtractor extends AbstractPDFExtractor
{
    private static final String WITHHOLDING_TAX = "Withholding Tax";
    private static final String INTEREST_TAX = "Free-funds Interest Tax";
    private static final Pattern INTEREST_PERIOD = Pattern.compile("^.* (?<period>[\\d]{4}\\-[\\d]{2})$");

    public XTBPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Name and surname Account Currency");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "XTB S.A.";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("CASH OPERATION HISTORY", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Name and surname Account Currency 26/08/2025 16:03:10
                                        // tctp oVMKS 13132377 EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Name and surname Account Currency .*") //
                                        .match("^.* [\\d]+ (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 783364057 deposit 15/04/2025 20:45:14 eWallet OT|DIR:B2I|BA:1790104| 3110.00
        // IA:52090475|BR:PLCZ|CID:9c2147cd-1a6d-
        // 4e7c-aca8-860728bad87f
        // @formatter:on
        var depositBlock = new Block("^[\\d]+ deposit [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* [\\.\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "time", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ deposit (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .* (?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 783369158 transfer 15/04/2025 20:55:01 Transfer from 52090475 to 52092041 -150.00
        // 783369159 transfer 15/04/2025 20:55:01 Transfer from 52090475 to 52092041 150.00
        // @formatter:on
        var transferBlock = new Block("^[\\d]+ transfer [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} Transfer from .* (\\-)?[\\.\\d]+$");
        type.addBlock(transferBlock);
        transferBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "time", "note", "type", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ transfer (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) " //
                                        + "(?<note>Transfer from [\\d]+ to [\\d]+) (?<type>(\\-)?)(?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(t -> new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired)));

        // @formatter:off
        // 783672698 Stock purchase 16/04/2025 09:00:01 OPEN BUY 11/11.4233 @ 43.400 P911.DE -477.40
        // 784829322 Stock purchase 17/04/2025 09:00:30 OPEN BUY 0.6185 @ 484.55 MC.FR -299.69
        // @formatter:on
        var buyBlock = new Block("^[\\d]+ Stock purchase [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} OPEN BUY .* \\-[\\.\\d]+$");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("date", "time", "shares", "tickerSymbol", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ Stock purchase (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) " //
                                        + "OPEN BUY (?<shares>[\\.\\d]+)(\\/[\\.\\d]+)? @ [\\.\\d]+ " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) \\-(?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // 899612209 close trade 11/08/2025 09:14:43 Profit of position #1887765361 VT0P.DE 70.16
        // 899612210 Stock sale 11/08/2025 09:14:43 CLOSE BUY 20 @ 11.3730 VT0P.DE 157.30
        // @formatter:on
        var sellBlock = new Block("^[\\d]+ close trade [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* (\\-)?[\\.\\d]+$");
        type.addBlock(sellBlock);
        sellBlock.setMaxSize(2);
        sellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // 899612210 Stock sale 11/08/2025 09:14:43 CLOSE BUY 20 @ 11.3730 VT0P.DE 157.30
                        // @formatter:on
                        .section("date", "time", "shares", "tickerSymbol", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ Stock sale (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) " //
                                        + "CLOSE BUY (?<shares>[\\.\\d]+)(\\/[\\.\\d]+)? @ [\\.\\d]+ " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // The "Stock sale" line only contains the purchase value of the position.
                        // The proceeds are the purchase value plus the profit (or minus the loss) of the "close trade" line.
                        //
                        // 899612209 close trade 11/08/2025 09:14:43 Profit of position #1887765361 VT0P.DE 70.16
                        // @formatter:on
                        .section("sign", "profit") //
                        .documentContext("currency") //
                        .match("^[\\d]+ close trade [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* " //
                                        + "[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? (?<sign>(\\-)?)(?<profit>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            var profit = Money.of(v.get("currency"), asAmount(v.get("profit")));

                            if ("-".equals(trim(v.get("sign"))))
                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().subtract(profit));
                            else
                                t.setMonetaryAmount(t.getPortfolioTransaction().getMonetaryAmount().add(profit));
                        })

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // 793133926 DIVIDENT 28/04/2025 11:46:04 MC.FR EUR 7.5000/ SHR MC.FR 4.64
        // 822727454 DIVIDENT 27/05/2025 16:02:05 P911.DE EUR 1.4932/ SHR P911.DE 0.63
        // @formatter:on
        var dividendBlock = new Block("^[\\d]+ DIVIDENT [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* [\\.\\d]+$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "time", "tickerSymbol", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ DIVIDENT (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .* " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 793133927 Withholding Tax 28/04/2025 11:46:04 MC.FR EUR WHT 25% MC.FR -1.16
        // @formatter:on
        var withholdingTaxBlock = new Block("^[\\d]+ Withholding Tax [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* \\-[\\.\\d]+$");
        type.addBlock(withholdingTaxBlock);
        withholdingTaxBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("date", "time", "tickerSymbol", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ Withholding Tax (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) .* " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) \\-(?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(WITHHOLDING_TAX);
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 785289247 tax IFTT 17/04/2025 15:27:58 FTT France adj MC.FR 20250416 MC.FR -1.46
        // @formatter:on
        var financialTransactionTaxBlock = new Block("^[\\d]+ tax IFTT [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* \\-[\\.\\d]+$");
        type.addBlock(financialTransactionTaxBlock);
        financialTransactionTaxBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("date", "time", "note", "tickerSymbol", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ tax IFTT (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) (?<note>.*) " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) \\-(?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("tickerSymbol"));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 799547280 Free-funds Interest 04/05/2025 16:43:00 Free-funds Interest 2025-04 0.62
        // @formatter:on
        var interestBlock = new Block("^[\\d]+ Free\\-funds Interest [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* [\\.\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "time", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ Free\\-funds Interest (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) " //
                                        + "(?<note>Free\\-funds Interest [\\d]{4}\\-[\\d]{2}) (?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 799547304 Free-funds Interest Tax 04/05/2025 16:43:04 Free-funds Interest Tax 2025-04 -0.09
        // @formatter:on
        var interestTaxBlock = new Block("^[\\d]+ Free\\-funds Interest Tax [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} .* \\-[\\.\\d]+$");
        type.addBlock(interestTaxBlock);
        interestTaxBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("date", "time", "note", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ Free\\-funds Interest Tax (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) " //
                                        + "(?<note>Free\\-funds Interest Tax [\\d]{4}\\-[\\d]{2}) \\-(?<amount>[\\.\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    public void postProcessing(List<Item> items)
    {
        mergeDividendsWithWithholdingTax(items);
        mergeInterestWithInterestTax(items);
    }

    /**
     * Merges the "Withholding Tax" line into the "DIVIDENT" line with the
     * same security, the same time stamp and the same source file. The
     * dividend amount is the gross amount, therefore the tax is subtracted.
     * Only taxes marked as withholding tax are considered, so that other
     * security related taxes (e.g. the French financial transaction tax) are
     * never merged into a dividend.
     */
    private void mergeDividendsWithWithholdingTax(List<Item> items)
    {
        var dividendItems = filterAccountTransactions(items, AccountTransaction.Type.DIVIDENDS);

        var withholdingTaxItems = new ArrayList<>(filterAccountTransactions(items, AccountTransaction.Type.TAXES) //
                        .stream() //
                        .filter(i -> WITHHOLDING_TAX.equals(i.getSubject().getNote())) //
                        .toList());

        for (var dividendItem : dividendItems)
        {
            var dividend = (AccountTransaction) dividendItem.getSubject();

            var withholdingTaxItem = withholdingTaxItems.stream() //
                            .filter(i -> {
                                var tax = (AccountTransaction) i.getSubject();
                                return dividend.getSecurity().equals(tax.getSecurity()) //
                                                && dividend.getDateTime().equals(tax.getDateTime()) //
                                                && Objects.equals(dividend.getSource(), tax.getSource());
                            }) //
                            .findFirst();

            if (withholdingTaxItem.isEmpty())
                continue;

            var tax = ((AccountTransaction) withholdingTaxItem.get().getSubject()).getMonetaryAmount();

            dividend.setMonetaryAmount(dividend.getMonetaryAmount().subtract(tax));
            dividend.addUnit(new Unit(Unit.Type.TAX, tax));

            withholdingTaxItems.remove(withholdingTaxItem.get());
            items.remove(withholdingTaxItem.get());
        }
    }

    /**
     * Merges the "Free-funds Interest Tax" line into the "Free-funds Interest"
     * line of the same period (e.g. 2025-04) and the same source file. The
     * time stamps of both lines can differ, therefore the period is used.
     */
    private void mergeInterestWithInterestTax(List<Item> items)
    {
        var interestItems = filterAccountTransactions(items, AccountTransaction.Type.INTEREST);

        var interestTaxItems = new ArrayList<>(filterAccountTransactions(items, AccountTransaction.Type.TAXES) //
                        .stream() //
                        .filter(i -> ((AccountTransaction) i.getSubject()).getSecurity() == null) //
                        .filter(i -> i.getSubject().getNote() != null && i.getSubject().getNote().startsWith(INTEREST_TAX)) //
                        .toList());

        for (var interestItem : interestItems)
        {
            var interest = (AccountTransaction) interestItem.getSubject();
            var interestPeriod = getInterestPeriod(interest.getNote());

            if (interestPeriod == null)
                continue;

            var interestTaxItem = interestTaxItems.stream() //
                            .filter(i -> {
                                var tax = (AccountTransaction) i.getSubject();
                                return interestPeriod.equals(getInterestPeriod(tax.getNote())) //
                                                && Objects.equals(interest.getSource(), tax.getSource());
                            }) //
                            .findFirst();

            if (interestTaxItem.isEmpty())
                continue;

            var tax = ((AccountTransaction) interestTaxItem.get().getSubject()).getMonetaryAmount();

            interest.setMonetaryAmount(interest.getMonetaryAmount().subtract(tax));
            interest.addUnit(new Unit(Unit.Type.TAX, tax));

            interestTaxItems.remove(interestTaxItem.get());
            items.remove(interestTaxItem.get());
        }
    }

    private List<Item> filterAccountTransactions(List<Item> items, AccountTransaction.Type type)
    {
        return items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == type) //
                        .toList();
    }

    private String getInterestPeriod(String note)
    {
        if (note == null)
            return null;

        var matcher = INTEREST_PERIOD.matcher(note);
        return matcher.matches() ? matcher.group("period") : null;
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
