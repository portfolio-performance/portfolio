package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Robinhood Financial LLC is a US-based broker.
 *           The account is carried by Robinhood Securities, LLC.
 *           The currency is US$.
 *
 * @implSpec All security currencies are USD.
 *           The CUSIP number is the WKN number.
 *
 *           The monthly account statement does not distinguish the debit and credit column in the text output.
 *           The direction of each transaction is therefore determined by the transaction type (Buy, Sell, ACH, ...).
 *
 *           Internal transfers (ITRF) are classified by their description:
 *           "Transfer from Brokerage to ..." is a removal, any other transfer (e.g. "Transfer from ... to Brokerage") is a deposit.
 *           This has only been verified with the statement of the individual (brokerage) account.
 *
 *           Dividend lines only contain the ticker symbol, therefore the ticker symbol is also used as name.
 *           The dividend lines contain the record date (R/D) but no ex-date.
 *
 *           Foreign withholding taxes (DTAX) are listed as separate lines.
 *           In postProcessing, they are merged into the dividend with the same statement, date and security,
 *           but only if the assignment is unique. Otherwise the tax is kept as separate transaction.
 *
 *           Trades listed in "Executed Trades Pending Settlement" are not imported,
 *           because they are reported in the "Account Activity" of the following statement.
 *           The "Deposit Sweep Activity" only contains internal cash movements and is not imported.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class RobinhoodPDFExtractor extends AbstractPDFExtractor
{
    private static final String USD = "USD";

    private static record DividendTaxKey(String source, LocalDate date, Security security)
    {
    }

    public RobinhoodPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Robinhood");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Robinhood Financial LLC";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Account Activity");
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Description | Symbol | Acct Type | Transaction Date | Qty | Price | Debit | Credit
        // -------------------------------------
        // Vanguard FTSE All-World ex-US ETF VEU Cash Buy 12/31/2025 10 $73.66500 $736.65
        // CUSIP: 922042775
        //
        // AMD
        // AMD Cash Sell 01/05/2026 1 $232.72000 $232.72
        // CUSIP: 007903107
        // @formatter:on
        //
        // The name of the security is either in the same line as the
        // ticker symbol or in the line above it. In the second case, the
        // block starts one line earlier, so that the name can be read.
        var startsWith = Pattern.compile("^(.* )?[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? Cash (Buy|Sell) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\.,\\d]+ \\$[\\.,\\d]+ \\$[\\.,\\d]+$");
        var tickerOnly = Pattern.compile("^[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? Cash (Buy|Sell) .*$");

        var splittingStrategy = (SplittingStrategy) lines -> {
            var spans = new ArrayList<LineSpan>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (!startsWith.matcher(lines[ii]).matches())
                    continue;

                var startLine = ii > 0 && tickerOnly.matcher(lines[ii]).matches() ? ii - 1 : ii;
                var endLine = Math.min(ii + 1, lines.length - 1);

                spans.add(new LineSpan(startLine, endLine));
            }

            return spans;
        };

        var buySellBlock = new Block(splittingStrategy);
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .oneOf( //
                                        // @formatter:off
                                        // Vanguard FTSE All-World ex-US ETF VEU Cash Buy 12/31/2025 10 $73.66500 $736.65
                                        // CUSIP: 922042775
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "wkn") //
                                                        .match("^(?<name>.*) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash (Buy|Sell) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\.,\\d]+ \\$[\\.,\\d]+ \\$[\\.,\\d]+$") //
                                                        .match("^CUSIP: (?<wkn>[A-Z0-9]{9})$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode(USD));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // AMD
                                        // AMD Cash Sell 01/05/2026 1 $232.72000 $232.72
                                        // CUSIP: 007903107
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "tickerSymbol", "wkn") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash (Buy|Sell) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\.,\\d]+ \\$[\\.,\\d]+ \\$[\\.,\\d]+$") //
                                                        .match("^CUSIP: (?<wkn>[A-Z0-9]{9})$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode(USD));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Vanguard FTSE All-World ex-US ETF VEU Cash Buy 12/31/2025 10 $73.66500 $736.65
                        // AMD Cash Sell 01/05/2026 1 $232.72000 $232.72
                        // @formatter:on
                        .section("type", "date", "shares", "amount") //
                        .match("^(.* )?[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? Cash (?<type>(Buy|Sell)) (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<shares>[\\.,\\d]+) \\$[\\.,\\d]+ \\$(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "Sell" change from BUY to SELL
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            t.setDate(asDate(v.get("date"), Locale.US));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(USD));
                        })

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // Cash Div: R/D 2025-12-18 P/D 2026-01-02 - 43 shares at 0.05 PSKY Cash CDIV 01/02/2026 $2.15
        // Cash Div: R/D 2025-12-11 P/D 2026-01-08 - 115 shares at 0.79542 TSM Cash CDIV 01/08/2026 $91.47
        // @formatter:on
        var dividendBlock = new Block("^Cash Div: R\\/D .* Cash CDIV [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} \\$[\\.,\\d]+$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "shares", "tickerSymbol", "amount") //
                        .match("^Cash Div: R\\/D [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} P\\/D (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) \\- (?<shares>[\\.,\\d]+) shares at [\\.,\\d]+ (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash CDIV [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} \\$(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Dividend lines only contain the ticker symbol
                            v.put("name", v.get("tickerSymbol"));
                            v.put("currency", asCurrencyCode(USD));

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Foreign Tax Witholding at $19.21 TSM Cash DTAX 01/08/2026 $19.21
        // @formatter:on
        var taxesBlock = new Block("^Foreign Tax Withh?olding .* Cash DTAX [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} \\$[\\.,\\d]+$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("tickerSymbol", "date", "amount") //
                        .match("^Foreign Tax Withh?olding .* (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash DTAX (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) \\$(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Tax lines only contain the ticker symbol
                            v.put("name", v.get("tickerSymbol"));
                            v.put("currency", asCurrencyCode(USD));

                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // ACH Deposit Cash ACH 01/02/2026 $7,500.00
        // ACH Withdrawal Cash ACH 01/13/2026 $10,000.00
        // Transfer from Brokerage to Traditional IRA Cash ITRF 01/02/2026 $7,500.00
        // Cash back from Robinhood Credit Card Cash XENT_CC 01/12/2026 $70.00
        // @formatter:on
        var depositRemovalBlock = new Block("^(ACH Deposit|ACH Withdrawal|Transfer from .*|Cash back from .*) Cash (ACH|ITRF|XENT_CC) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} \\$[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("note", "type", "date", "amount") //
                        .match("^(?<note>(ACH Deposit|ACH Withdrawal|Transfer from .*|Cash back from .*)) Cash (?<type>(ACH|ITRF|XENT_CC)) (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) \\$(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "ACH Withdrawal" change from DEPOSIT to REMOVAL
                            if ("ACH Withdrawal".equals(v.get("note")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // Is type --> "ITRF" out of the brokerage account change from DEPOSIT to REMOVAL
                            if ("ITRF".equals(v.get("type")) && v.get("note").startsWith("Transfer from Brokerage "))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(USD));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Interest Payment Sweep INT 01/20/2026 $102.83
        // @formatter:on
        var interestBlock = new Block("^Interest Payment Sweep INT [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} \\$[\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "amount") //
                        .match("^Interest Payment Sweep INT (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) \\$(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(USD));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    public void postProcessing(List<Item> items)
    {
        // Group dividends and taxes by statement (source), date and security
        var dividendsByKey = groupByKey(items, AccountTransaction.Type.DIVIDENDS);
        var taxesByKey = groupByKey(items, AccountTransaction.Type.TAXES);

        // @formatter:off
        // This loop iterates through the taxes grouped by statement, date and security.
        //
        // A tax is only merged into a dividend if the assignment is unique, i.e. exactly one dividend
        // and exactly one tax exist for the key. The DTAX line does not reference the gross amount of
        // the dividend, so ambiguous taxes are kept as separate tax transactions.
        //
        // When merged, the tax amount is subtracted from the dividend amount and added as tax unit,
        // the source and note are combined and the tax transaction is removed from the 'items' list.
        // @formatter:on
        for (var entry : taxesByKey.entrySet())
        {
            var dividends = dividendsByKey.get(entry.getKey());
            var taxes = entry.getValue();

            if (dividends == null || dividends.size() != 1 || taxes.size() != 1)
                continue;

            var dividendTransaction = (AccountTransaction) dividends.get(0).getSubject();
            var taxesTransaction = (AccountTransaction) taxes.get(0).getSubject();

            dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount() //
                            .subtract(taxesTransaction.getMonetaryAmount()));

            dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

            dividendTransaction.setSource(
                            concatenate(dividendTransaction.getSource(), taxesTransaction.getSource(), "; "));

            dividendTransaction.setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

            items.remove(taxes.get(0));
        }
    }

    /**
     * Groups the account transactions of the given type by statement (source),
     * date and security. The source is part of the key, because postProcessing
     * receives the items of all statements of this extractor at once. A tax
     * must only be merged into a dividend of the same statement. Multiple
     * transactions per key are retained, so that ambiguous assignments can be
     * detected.
     *
     * @param items
     *            The list of all extracted items.
     * @param type
     *            The account transaction type to be grouped.
     * @return A map of keys to the list of matching items.
     */
    private Map<DividendTaxKey, List<Item>> groupByKey(List<Item> items, AccountTransaction.Type type)
    {
        Map<DividendTaxKey, List<Item>> groups = new HashMap<>();

        items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> type.equals(((AccountTransaction) i.getSubject()).getType())) //
                        .filter(i -> i.getSecurity() != null) //
                        .forEach(i -> {
                            var transaction = (AccountTransaction) i.getSubject();
                            var key = new DividendTaxKey(transaction.getSource(), i.getDate().toLocalDate(),
                                            i.getSecurity());

                            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
                        });

        return groups;
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
