package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Alpian SA
 *
 * @implSpec The documents are in English and use the number format 1,234.56.
 *           The contract notes do not contain an exchange rate. If the security
 *           is traded in a currency other than that of the account, the
 *           exchange rate is calculated from the amount in the trading currency
 *           and the amount debited or credited to the account.
 *
 *           All amounts may carry a minus sign. The sign is only accepted,
 *           the direction of a booking is never derived from it:
 *           - stamp duty and withholding tax are always a tax,
 *           - buy/sell and dividend amounts follow the document type,
 *           - the fee advice follows the wording "debited" / "credited",
 *           - the account statement follows the booking text.
 *
 *           Newer documents (portfolio statement) use the Swiss thousands
 *           separator ’ (U+2019) or ' instead of the comma: 51’068.47
 *
 *           The portfolio statement of the managed portfolio does not contain
 *           any booking that is not also provided by a separate document
 *           (contract note, dividend advice, fee advice). All bookings are
 *           therefore marked as failure with a reference to the other document.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class AlpianPDFExtractor extends AbstractPDFExtractor
{
    // @formatter:off
    // CASH - 4/2.56-iShares Swiss Dom Govt Bd 2026-07-24 2026-07-23 0.90 0.00
    // Booking date: 2026-07-24 | Value date (= pay date): 2026-07-23
    // @formatter:on
    private static final String CASH_LINE = "^CASH \\- (?<shares>[\\.,'’\\d]+)\\/[\\.,'’\\d]+\\-(?<name>.*) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (\\-[\\s]*)?(?<debit>[\\.,'’\\d]+) (\\-[\\s]*)?(?<credit>[\\.,'’\\d]+)$";

    // Continuation of a security name on the next line, e.g. "3-7" or "Bond",
    // but neither the next booking nor the page footer.
    private static final String NAME_CONTINUATION = "^(?!(Securities (Purchase|Sale)|Corporate Action|Tax Amount Due|Management Fees|[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} ))(?<nameContinued>.+)$";

    public AlpianPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Alpian SA");
        addBankIdentifier("Alpian Bank");

        addBuySellTransaction();
        addDividendTransaction();
        addFeesTransaction();
        addAccountStatementTransaction();
        addPortfolioStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Alpian SA";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Contract Note (Buy|Sell)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Contract Note (Buy|Sell) \\- .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Contract Note (?<type>(Buy|Sell)) \\- .*$") //
                        .assign((t, v) -> {
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Security name: iShares Core MSCI Europe E
                        // ISIN: IE00B4K48X80
                        // Currency: EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Security name: (?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Currency: (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Quantity: 20
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantity: (?<shares>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Trade date: 19.10.2023
                                        // Time of execution: 2023-10-19-08:12:34
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Trade date: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .match("^Time of execution: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}\\-(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Trade date: 19.10.2023
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Trade date: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Total net amount debited: CHF 67.82
                        // Total net amount credited: CHF 1,253.18
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Total net amount (debited|credited): (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // The exchange rate is not stated in the document.
                        // It is calculated from the amount in the trading currency
                        // and the amount debited or credited to the account.
                        // @formatter:on
                        .optionalOneOf( //
                                        // @formatter:off
                                        // Stamp tax duty: EUR 0.11
                                        // Total net amount paid: EUR 71.48
                                        // Total net amount debited: CHF 67.82
                                        //
                                        // Stamp tax duty: EUR 1.99
                                        // Net amount: EUR 1,324.71
                                        // Total net amount credited: CHF 1,253.18
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("taxCurrency", "tax", "termCurrency", "fxAmount", "baseCurrency", "amount") //
                                                        .match("^Stamp tax duty: (?<taxCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<tax>[\\.,'’\\d]+)$") //
                                                        .match("^(Total net amount paid|Net amount): (?<termCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<fxAmount>[\\.,'’\\d]+)$") //
                                                        .match("^Total net amount (debited|credited): (?<baseCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxAmount")));
                                                            var tax = Money.of(asCurrencyCode(v.get("taxCurrency")), asAmount(v.get("tax")));

                                                            // The net amount in the trading currency contains
                                                            // the stamp duty. For the gross value it is removed
                                                            // again: purchase = net - tax, sale = net + tax
                                                            if (tax.getCurrencyCode().equals(fxGross.getCurrencyCode()))
                                                            {
                                                                if (t.getPortfolioTransaction().getType().isPurchase())
                                                                    fxGross = fxGross.subtract(tax);
                                                                else
                                                                    fxGross = fxGross.add(tax);
                                                            }

                                                            processExchangeRate(t, v, fxGross, type);
                                                        }),
                                        // @formatter:off
                                        // Total net amount paid: EUR 71.48
                                        // Total net amount debited: CHF 67.82
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxAmount", "baseCurrency", "amount") //
                                                        .match("^(Total net amount paid|Net amount): (?<termCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<fxAmount>[\\.,'’\\d]+)$") //
                                                        .match("^Total net amount (debited|credited): (?<baseCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxAmount")));

                                                            processExchangeRate(t, v, fxGross, type);
                                                        }))

                        // @formatter:off
                        // Contract Note Buy - SCTRSC23346K8NG6
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Contract Note (Buy|Sell) \\- (?<note>[\\w]+)$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("Payment dividend advice");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Payment dividend advice$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Security ISIN & name: 800000-056   iShares Swiss Dom Govt BdCH0016999846
                        // Currency: CHF
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Security ISIN \\& name: ([\\d]+\\-[\\d]+[\\s]+)?(?<name>.*)(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Currency: (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Qualifying nominal: 3
                        // @formatter:on
                        .section("shares") //
                        .match("^Qualifying nominal: (?<shares>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Trade date: 21.01.2025
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Trade date: (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Value date: 23.01.2025
                        // @formatter:on
                        .section("date") //
                        .match("^Value date: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Net amount: CHF 1.13
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Net amount: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addFeesTransaction()
    {
        final var type = new DocumentType("Managed by Alpian fee advice");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Managed by Alpian fee advice.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        // Is type --> "credited" change from FEES to FEES_REFUND
                        // The direction is taken from the wording only. A minus
                        // sign in front of the amount does not change it.
                        .section("type").optional() //
                        .match("^We would like to inform you that your account has been (?<type>(debited|credited)) on .*$") //
                        .assign((t, v) -> {
                            if ("credited".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.FEES_REFUND);
                        })

                        // @formatter:off
                        // We would like to inform you that your account has been debited on 01.04.2025 .
                        // @formatter:on
                        .section("date") //
                        .match("^We would like to inform you that your account has been (debited|credited) on (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]*\\.$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Amount: CHF 76.37
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Amount: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Period: JAN 2025 - MAR 2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Period: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Account statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Account statement - May 2023 09.05.2023 - 31.05.2023
                                        // @formatter:on
                                        .section("periodStart", "periodEnd") //
                                        .match("^Account statement \\- .* (?<periodStart>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\- (?<periodEnd>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("periodStart", v.get("periodStart"));
                                            ctx.put("periodEnd", v.get("periodEnd"));
                                        }));

        this.addDocumentTyp(type);

        // @formatter:off
        // The columns "Debit" and "Credit" are lost in the text conversion.
        // The direction is therefore taken from the booking text:
        // Domestic Clearing       --> DEPOSIT
        // Debit Internal Transfer --> REMOVAL
        // @formatter:on

        // @formatter:off
        // Domestic Clearing (DD) 15 May 15 May CHF 1.00
        // Domestic Clearing (DD) 23 May 23 May CHF 29,999.00
        // @formatter:on
        var depositBlock = new Block("^Domestic Clearing.* [\\d]{1,2} [\\p{L}]{3,4} [\\d]{1,2} [\\p{L}]{3,4} [A-Z]{3} (\\-[\\s]*)?[\\.,'’\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("note", "date", "currency", "amount") //
                        .documentContext("periodStart", "periodEnd") //
                        .match("^(?<note>Domestic Clearing.*) [\\d]{1,2} [\\p{L}]{3,4} (?<date>[\\d]{1,2} [\\p{L}]{3,4}) (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDateWithinPeriod(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Debit Internal Transfer 23 May 23 May CHF 30,000.00
        // @formatter:on
        var removalBlock = new Block("^Debit Internal Transfer.* [\\d]{1,2} [\\p{L}]{3,4} [\\d]{1,2} [\\p{L}]{3,4} [A-Z]{3} (\\-[\\s]*)?[\\.,'’\\d]+$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("note", "date", "currency", "amount") //
                        .documentContext("periodStart", "periodEnd") //
                        .match("^(?<note>Debit Internal Transfer) [\\d]{1,2} [\\p{L}]{3,4} (?<date>[\\d]{1,2} [\\p{L}]{3,4}) (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDateWithinPeriod(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addPortfolioStatementTransaction()
    {
        final var type = new DocumentType("Portfolio statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Name ISIN Currency Quantity Market Value Weight
                                        // (CHF)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Name ISIN Currency Quantity Market Value Weight$") //
                                        .match("^\\((?<currency>[A-Z]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // All bookings of the portfolio statement are also provided by a
        // separate document (contract note, dividend advice, fee advice).
        // They are marked as failure so that they are not imported twice.
        //
        // The columns are: Booking date | Value date | Debit | Credit
        // @formatter:on

        // @formatter:off
        // Securities Purchase
        // 31 - 21Shares Crypto Basket Index-ETP 2026-08-04 2026-08-06 293.16 0.00
        // Securities Sale
        // 88 - iShares USD Corp Bond 2026-08-04 2026-08-06 0.00 381.74
        // @formatter:on
        var buySellBlock = new Block("^Securities (Purchase|Sale)$");
        type.addBlock(buySellBlock);
        buySellBlock.setMaxSize(3);
        buySellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Sale" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Securities (?<type>(Purchase|Sale))$") //
                        .assign((t, v) -> {
                            if ("Sale".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .section("shares", "name", "date", "debit", "credit") //
                        .documentContext("currency") //
                        .match("^(?<shares>[\\.,'’\\d]+) \\- (?<name>.*) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (\\-[\\s]*)?(?<debit>[\\.,'’\\d]+) (\\-[\\s]*)?(?<credit>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asDebitOrCreditAmount(v));
                            t.setNote(trim(v.get("name")));

                            v.markAsFailure(Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        // The name of the security may continue on the next line
                        .section("name", "nameContinued").optional() //
                        .match("^[\\.,'’\\d]+ \\- (?<name>.*) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} .*$") //
                        .match(NAME_CONTINUATION) //
                        .assign((t, v) -> t.setNote(trim(v.get("name")) + " " + trim(v.get("nameContinued"))))

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // Corporate Action Cr
        // CASH - 4/2.56-iShares Swiss Dom Govt Bd 2026-07-24 2026-07-23 0.00 2.56
        // 3-7
        // @formatter:on
        var dividendBlock = new Block("^Corporate Action (Cr|Dr)$");
        type.addBlock(dividendBlock);
        dividendBlock.setMaxSize(3);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("shares", "name", "date", "debit", "credit") //
                        .documentContext("currency") //
                        .match(CASH_LINE) //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asDebitOrCreditAmount(v));
                            t.setNote(trim(v.get("name")));

                            v.markAsFailure(Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .section("name", "nameContinued").optional() //
                        .match(CASH_LINE) //
                        .match(NAME_CONTINUATION) //
                        .assign((t, v) -> t.setNote(trim(v.get("name")) + " " + trim(v.get("nameContinued"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Tax Amount Due
        // CASH - 4/2.56-iShares Swiss Dom Govt Bd 2026-07-24 2026-07-23 0.90 0.00
        // 3-7
        // @formatter:on
        var taxesBlock = new Block("^Tax Amount Due$");
        type.addBlock(taxesBlock);
        taxesBlock.setMaxSize(3);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("shares", "name", "date", "debit", "credit") //
                        .documentContext("currency") //
                        .match(CASH_LINE) //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asDebitOrCreditAmount(v));
                            t.setNote(trim(v.get("name")));

                            v.markAsFailure(Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .section("name", "nameContinued").optional() //
                        .match(CASH_LINE) //
                        .match(NAME_CONTINUATION) //
                        .assign((t, v) -> t.setNote(trim(v.get("name")) + " " + trim(v.get("nameContinued"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Management Fees Dr
        // Fees posted from 202604 to 202606 2026-07-01 2026-07-01 91.72 0.00
        // @formatter:on
        var feesBlock = new Block("^Management Fees (Dr|Cr)$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(2);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("note", "date", "debit", "credit") //
                        .documentContext("currency") //
                        .match("^(?<note>Fees posted from [\\d]{6} to [\\d]{6}) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (\\-[\\s]*)?(?<debit>[\\.,'’\\d]+) (\\-[\\s]*)?(?<credit>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> {
                            // Is credit --> change from FEES to FEES_REFUND
                            if (asAmount(v.get("debit")) == 0 && asAmount(v.get("credit")) != 0)
                                t.setType(AccountTransaction.Type.FEES_REFUND);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asDebitOrCreditAmount(v));
                            t.setNote(trim(v.get("note")));

                            v.markAsFailure(Messages.MsgErrorTransactionAlternativeDocumentRequired);
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * The dates of the account statement have no year (15 May). The year is
     * taken from the statement period. If the period spans a year end (e.g.
     * 01.12.2023 - 31.01.2024), the date is built with the start year and the
     * end year and the one closer to the period is used. This also covers
     * value dates shortly before the start of the period.
     *
     * @formatter:off
     * 15 Jan --> 15.01.2023 (11 months before) or 15.01.2024 (within) --> 15.01.2024
     * 30 Nov --> 30.11.2023 (1 day before) or 30.11.2024 (10 months after) --> 30.11.2023
     * @formatter:on
     */
    private LocalDateTime asDateWithinPeriod(Map<String, String> v)
    {
        var periodStart = asDate(v.get("periodStart"));
        var periodEnd = asDate(v.get("periodEnd"));

        var dateInStartYear = asDate(v.get("date") + " " + periodStart.getYear());

        if (periodStart.getYear() == periodEnd.getYear())
            return dateInStartYear;

        var dateInEndYear = asDate(v.get("date") + " " + periodEnd.getYear());

        if (distanceToPeriod(dateInEndYear, periodStart, periodEnd) < distanceToPeriod(dateInStartYear, periodStart, periodEnd))
            return dateInEndYear;

        return dateInStartYear;
    }

    private static long distanceToPeriod(LocalDateTime date, LocalDateTime periodStart, LocalDateTime periodEnd)
    {
        if (date.isBefore(periodStart))
            return ChronoUnit.DAYS.between(date, periodStart);

        if (date.isAfter(periodEnd))
            return ChronoUnit.DAYS.between(periodEnd, date);

        return 0;
    }

    /**
     * Returns the amount of the column "Debit" or, if it is zero, of the
     * column "Credit" of the portfolio statement.
     */
    private long asDebitOrCreditAmount(Map<String, String> v)
    {
        var debit = asAmount(v.get("debit"));
        return debit != 0 ? debit : asAmount(v.get("credit"));
    }

    /**
     * Calculates the exchange rate from the net amount in the trading currency
     * (fxAmount) and the net amount in the account currency (amount), stores
     * it in the document context (required to convert taxes) and sets the
     * gross value unit. Nothing happens if both currencies are the same.
     */
    private void processExchangeRate(BuySellEntry t, Map<String, String> v, Money fxGross, DocumentType type)
    {
        var baseCurrency = asCurrencyCode(v.get("baseCurrency"));
        var termCurrency = fxGross.getCurrencyCode();

        if (baseCurrency.equals(termCurrency))
            return;

        var exchangeRate = BigDecimal.valueOf(asAmount(v.get("fxAmount"))) //
                        .divide(BigDecimal.valueOf(asAmount(v.get("amount"))), 10, RoundingMode.HALF_DOWN);

        var rate = new ExtrExchangeRate(exchangeRate, baseCurrency, termCurrency);
        type.getCurrentContext().putType(rate);

        var gross = rate.convert(baseCurrency, fxGross);

        checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // Stamp duty and withholding tax are by definition always a charge.
        // An optional minus sign is accepted so that the line is not skipped,
        // but it does not turn the tax into a tax refund.
        transaction //

                        // @formatter:off
                        // Stamp tax duty: EUR 0.11
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Stamp tax duty: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<tax>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Witholding tax: CHF 0.61
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Withh?olding tax: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<withHoldingTax>[\\.,'’\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(removeSwissGroupingSeparator(value), Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(removeSwissGroupingSeparator(value), Values.Share, "en", "US");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(removeSwissGroupingSeparator(value), Values.Share, "en", "US");
    }

    /**
     * The portfolio statement uses the Swiss thousands separator ’ (U+2019)
     * or ', e.g. 51’068.47. With the number format en_US, DecimalFormat stops
     * parsing at this character and would silently return 51.00 instead of
     * 51,068.47. The separator is therefore removed before parsing.
     */
    private static String removeSwissGroupingSeparator(String value)
    {
        return value.replace("’", "").replace("'", "");
    }
}
