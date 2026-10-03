package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Pair;

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
 *           Release detail reports (restricted stock units) create three transactions:
 *           - delivery inbound of the released quantity at the fair market value (FMV) at vest,
 *           - sale of the quantity withheld to pay the taxes (sell-to-cover, withhold to cover),
 *           - removal of the tax amount.
 *           This keeps the cost basis at FMV and the net holding at the net quantity.
 *           A residual balance (sale proceeds minus taxes) remains on the account.
 *
 *           Quarterly statements only import the bookings that are not contained in a single document:
 *           - cash dividends (without reinvestment), booked on the payment date. The number of shares is
 *             the opening balance plus all shares released or reinvested before the payment date.
 *             The withholding tax on the same date belongs to the dividend. The ex-date is not contained.
 *           - disbursement of the proceeds (removal),
 *           - correction of the withholding tax of a previous dividend (Withholding Tax without dividend credit
 *             on the same date) as taxes or tax refund depending on the sign,
 *           - cancellation of a withholding tax (Cancel Withholding Tax), marked as unsupported cancellation.
 *           A dividend is reinvested if a reinvestment with the same gross amount follows within 10 days.
 *           Reinvested dividends and their reinvestment are imported from the dividend reinvestment
 *           confirmation and skipped in the statement. Releases are imported from the release detail report.
 *           The withholding tax line of a dividend credit is skipped, it is booked with the dividend.
 *           The statement neither contains the CUSIP nor the ticker symbol, only the issuer name.
 *           Known limitations: a dividend reinvested in the following quarter is imported as cash dividend,
 *           a release between record date and payment date increases the number of shares.
 *
 *           The account summary (StockPlan Connect report) is not supported. Its dividends are marked
 *           as failure with a reference to the other documents.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class MorganStanleyPDFExtractor extends AbstractPDFExtractor
{
    public MorganStanleyPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Morgan Stanley Smith Barney LLC");
        addBankIdentifier("StockPlan Connect");

        addDividendReinvestmentTransaction();
        addReleaseTransaction();
        addQuarterlyStatementTransaction();
        addAccountSummaryTransaction();
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

        // @formatter:off
        // The release is booked as delivery inbound of the released quantity at FMV,
        // followed by the sale of the withheld quantity (sell-to-cover) and the removal of the tax amount.
        // @formatter:on

        var deliveryBlock = new Block("^Summary for Release$");
        type.addBlock(deliveryBlock);
        deliveryBlock.set(new Transaction<PortfolioTransaction>()

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
                        // Quantity Released: 2.0000
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantity Released: (?<shares>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Release Date: 01-May-2022 **Total Tax Amount Due: $133.77
                        // Release Date: 01-Dec-2015 Federal Tax 42.8574 % $421.58
                        // @formatter:on
                        .section("date") //
                        .match("^Release Date: (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})( .*)?$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Total Gain (FMV x Quantity Released): $267.53
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total Gain \\(FMV x Quantity Released\\): (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Award ID: 80824152 Tax % Tax Paid
                        // Award ID: 88382462
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award ID: [A-Z0-9]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        var saleBlock = new Block("^Summary for Release$");
        type.addBlock(saleBlock);
        saleBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        // Security Name: BQzW twIsSBkn AwakjMDU lMih Withheld Quantity: 1.0000
                        // Trading Symbol: ygl x Withheld Quantity Value Per Share: $133.77
                        // *FMV @ Vest: $133.7650
                        // @formatter:on
                        .section("name", "tickerSymbol", "currency") //
                        .documentContextOptionally("wkn") //
                        .match("^Security Name: (?<name>.*) Withheld Quantity: [\\.,\\d]+$") //
                        .match("^Trading Symbol: (?<tickerSymbol>[A-Za-z0-9]{1,6}(?:\\.[A-Za-z]{1,4})?)( .*)?$") //
                        .match("^\\*FMV @ Vest.*: (?<currency>\\p{Sc})[\\.,\\d]+.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Quantity Withheld: 1.0000
                        // Quantity Withheld: (3.0000)
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantity Withheld: \\(?(?<shares>[\\.,\\d]+)\\)?[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Release Date: 01-May-2022 **Total Tax Amount Due: $133.77
                        // @formatter:on
                        .section("date") //
                        .match("^Release Date: (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})( .*)?$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Plan Name: 3900 Withheld Quantity Value: $133.77
                        // Award Date: 16-Jun-2011 Withheld Quantity Value: $421.58
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^.* Withheld Quantity Value: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Award ID: 80824152 Tax % Tax Paid
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award ID: [A-Z0-9]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        }));

        var removalBlock = new Block("^Summary for Release$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        // @formatter:off
                        // Release Date: 01-May-2022 **Total Tax Amount Due: $133.77
                        // @formatter:on
                        .section("date") //
                        .match("^Release Date: (?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4})( .*)?$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // Total Tax Amount Due: $133.77
                        // Total Tax Amount: $354.12
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total Tax Amount( Due)?: (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+)[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Tax withheld to cover");
                        })

                        // @formatter:off
                        // Award ID: 80824152 Tax % Tax Paid
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Award ID: [A-Z0-9]+).*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        }));
    }

    private void addQuarterlyStatementTransaction()
    {
        final var type = new DocumentType("SHARE PURCHASE AND HOLDINGS", (context, lines) -> {
            // @formatter:off
            // Issuer Description: INTL BUSINESS MACHINES CORP P.O. Box 182616 1-800-367-4777; 1-801-617-7414
            // Share Price $141.1900 $118.8100
            // Number of Shares 62.055 93.055
            // 7/10/25 Release 31.000 $285.5550
            // 9/10/22 Dividend Credit $48.29 48.29
            // 9/13/22 Dividend Reinvested 0.318 $129.0342 (48.29) (41.05)
            // 6/13/22 Dividend Reinvested 0.267 136.1234 47.85 (36.37)
            // @formatter:on
            var pName = Pattern.compile("^Issuer Description: (?<name>.*) P\\.O\\. Box .*$");
            var pCurrency = Pattern.compile("^Share Price (?<currency>\\p{Sc})[\\.,\\d]+ .*$");
            var pOpening = Pattern.compile("^Number of Shares (?<shares>[\\.,\\d]+) [\\.,\\d]+$");
            var pShareChange = Pattern.compile("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) (Release|Dividend Reinvested) (?<shares>[\\.,\\d]+) .*$");
            var pDividend = Pattern.compile("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Dividend Credit \\p{Sc}?(?<gross>[\\.,\\d]+) .*$");
            var pReinvest = Pattern.compile("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Dividend Reinvested [\\.,\\d]+ \\p{Sc}?[\\.,\\d]+ \\p{Sc}?\\(?(?<gross>[\\.,\\d]+)\\)? .*$");

            var openingShares = BigDecimal.ZERO;
            var shareChanges = new ArrayList<Pair<LocalDate, BigDecimal>>();
            var dividends = new ArrayList<Pair<String, BigDecimal>>();
            var reinvestments = new ArrayList<Pair<LocalDate, BigDecimal>>();

            for (String line : lines)
            {
                var m = pName.matcher(line);
                if (m.matches() && !context.containsKey("name"))
                    context.put("name", trim(m.group("name")));

                m = pCurrency.matcher(line);
                if (m.matches() && !context.containsKey("currency"))
                    context.put("currency", asCurrencyCode(m.group("currency")));

                m = pOpening.matcher(line);
                if (m.matches())
                    openingShares = asBigDecimalUS(m.group("shares"));

                m = pShareChange.matcher(line);
                if (m.matches())
                    shareChanges.add(new Pair<>(asStatementDate(m.group("date")).toLocalDate(), asBigDecimalUS(m.group("shares"))));

                // Remember the dates of the dividends. The withholding tax
                // on the same date belongs to the dividend.
                m = pDividend.matcher(line);
                if (m.matches())
                {
                    context.put(asDividendKey(m.group("date")), m.group("date"));
                    dividends.add(new Pair<>(m.group("date"), asBigDecimalUS(m.group("gross"))));
                }

                m = pReinvest.matcher(line);
                if (m.matches())
                    reinvestments.add(new Pair<>(asStatementDate(m.group("date")).toLocalDate(), asBigDecimalUS(m.group("gross"))));
            }

            for (var dividend : dividends)
            {
                var dividendDate = asStatementDate(dividend.getLeft()).toLocalDate();

                // A dividend is reinvested if a reinvestment with the same
                // gross amount follows within 10 days. It is imported with the
                // dividend reinvestment confirmation.
                var isReinvested = reinvestments.stream().anyMatch(r -> r.getRight().compareTo(dividend.getRight()) == 0 //
                                && !r.getLeft().isBefore(dividendDate) //
                                && ChronoUnit.DAYS.between(dividendDate, r.getLeft()) <= 10);

                if (isReinvested)
                {
                    context.put(asReinvestedKey(dividend.getLeft()), dividend.getLeft());
                }
                else
                {
                    // Shares of a cash dividend = opening balance plus all
                    // shares released or reinvested before the dividend date
                    var shares = openingShares;
                    for (var change : shareChanges)
                    {
                        if (change.getLeft().isBefore(dividendDate))
                            shares = shares.add(change.getRight());
                    }

                    context.put(asSharesKey(dividend.getLeft()), shares.toPlainString());
                }
            }
        });

        this.addDocumentTyp(type);

        // @formatter:off
        // 9/10/25 Dividend Credit $156.33 $156.33
        // 9/10/25 Withholding Tax (23.45)
        // @formatter:on
        var dividendBlock = new Block("^[\\d]{1,2}/[\\d]{1,2}/[\\d]{2} Dividend Credit .*$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "amount") //
                        .documentContext("name", "currency") //
                        .match("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Dividend Credit \\p{Sc}?(?<amount>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            t.setDateTime(asStatementDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));

                            // A reinvested dividend is imported with the
                            // dividend reinvestment confirmation and is skipped
                            if (!context.containsKey(asReinvestedKey(v.get("date"))))
                            {
                                t.setSecurity(getOrCreateSecurity(v));
                                t.setShares(asShares(context.get(asSharesKey(v.get("date")))));
                                t.setAmount(asAmount(v.get("amount")));
                            }
                        })

                        .section("date", "tax").multipleTimes().optional() //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Withholding Tax \\p{Sc}?\\((?<tax>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            // Only the withholding tax on the dividend date
                            // belongs to the dividend
                            if (t.getAmount() != 0 && asStatementDate(v.get("date")).equals(t.getDateTime()))
                            {
                                t.setAmount(t.getAmount() - asAmount(v.get("tax")));
                                processTaxEntries(t, v, type);
                            }
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        }));

        // @formatter:off
        // 9/11/25 Proceeds Disbursement (132.88)
        // @formatter:on
        var removalBlock = new Block("^[\\d]{1,2}/[\\d]{1,2}/[\\d]{2} Proceeds Disbursement .*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Proceeds Disbursement \\p{Sc}?\\((?<amount>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asStatementDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Correction of the withholding tax of a previous dividend (e.g. backup withholding replaced by treaty rate)
        // 8/23/22 Withholding Tax $(7.18)
        //
        // Withholding tax of a dividend credit on the same date (imported with the dividend)
        // 9/10/22 Withholding Tax (7.24)
        // @formatter:on
        var taxesBlock = new Block("^[\\d]{1,2}/[\\d]{1,2}/[\\d]{2} Withholding Tax .*$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("date", "type", "amount") //
                        .documentContext("name", "currency") //
                        .match("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Withholding Tax (?<type>[\\p{Sc}\\(]*)(?<amount>[\\.,\\d]+)\\)?$") //
                        .assign((t, v) -> {
                            // Is type --> without "(" change from TAXES to TAX_REFUND
                            if (!v.get("type").contains("("))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            t.setDateTime(asStatementDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));

                            // The withholding tax of a dividend credit on the
                            // same date is part of the dividend and is skipped
                            if (!type.getCurrentContext().containsKey(asDividendKey(v.get("date"))))
                            {
                                t.setSecurity(getOrCreateSecurity(v));
                                t.setAmount(asAmount(v.get("amount")));
                            }
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        }));

        // @formatter:off
        // 8/23/22 Cancel Withholding Tax 11.48
        // @formatter:on
        var cancelBlock = new Block("^[\\d]{1,2}/[\\d]{1,2}/[\\d]{2} Cancel Withholding Tax .*$");
        type.addBlock(cancelBlock);
        cancelBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        .section("date", "type", "amount") //
                        .documentContext("name", "currency") //
                        .match("^(?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}) Cancel Withholding Tax (?<type>[\\p{Sc}\\(]*)(?<amount>[\\.,\\d]+)\\)?$") //
                        .assign((t, v) -> {
                            // Is type --> "(" change from TAX_REFUND to TAXES
                            if (v.get("type").contains("("))
                                t.setType(AccountTransaction.Type.TAXES);

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asStatementDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));

                            v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported);
                        })

                        .wrap(TransactionItem::new));
    }

    private void addAccountSummaryTransaction()
    {
        final var type = new DocumentType("Summary Period: [\\d]{2}\\-[\\w]{3}\\-[\\d]{4}");
        this.addDocumentTyp(type);

        // @formatter:off
        // The account summary (StockPlan Connect report) is not supported.
        // The bookings are imported from the quarterly statement,
        // the dividend reinvestment confirmation and the release detail report.
        //
        // 10-Mar-2025​ Dividend (Cash)​ Participant​ $103.63​  ​  ​  ​  ​
        // 10-Mar-2025​ IRS Nonresident Alien​ Participant​ $-15.54​  ​  ​  ​  ​
        // Withholding​
        // @formatter:on
        var dividendBlock = new Block("^[\\d]{2}\\-[\\w]{3}\\-[\\d]{4}.? Dividend \\(Cash\\).*$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\-[\\w]{3}\\-[\\d]{4}).? Dividend \\(Cash\\).? Participant.? (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));

                            v.markAsFailure(Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .section("currency", "tax").optional() //
                        .match("^[\\d]{2}\\-[\\w]{3}\\-[\\d]{4}.? IRS .* Participant.? (?<currency>\\p{Sc})\\-(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() - asAmount(v.get("tax")));
                            processTaxEntries(t, v, type);
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * Dates in the quarterly statement have the format M/d/yy (e.g. 3/10/26 or
     * 09/10/22). Month and day are padded to two digits for the US date
     * formatters.
     */
    private LocalDateTime asStatementDate(String date)
    {
        return asDate(date.replaceAll("(?<![\\d])([\\d])(?=/)", "0$1"), Locale.US);
    }

    /**
     * The key of a dividend date in the document context. The date is
     * normalized, so that e.g. 9/10/22 and 09/10/22 result in the same key.
     */
    private String asDividendKey(String date)
    {
        return "dividend_" + asStatementDate(date).toLocalDate();
    }

    /**
     * The key of a reinvested dividend in the document context.
     */
    private String asReinvestedKey(String date)
    {
        return "reinvested_" + asStatementDate(date).toLocalDate();
    }

    /**
     * The key of the number of shares of a cash dividend in the document
     * context.
     */
    private String asSharesKey(String date)
    {
        return "shares_" + asStatementDate(date).toLocalDate();
    }

    private BigDecimal asBigDecimalUS(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // IRS Backup Withholding: $11.48
                        // IRS Nonresident Alien Withholding: $7.52
                        // @formatter:on
                        .section("currency", "tax").multipleTimes().optional() //
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
