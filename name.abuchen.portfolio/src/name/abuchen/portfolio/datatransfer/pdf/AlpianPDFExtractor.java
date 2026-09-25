package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

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
 *           - the fee advice follows the wording "debited" / "credited".
 * @formatter:on
 */

@SuppressWarnings("nls")
public class AlpianPDFExtractor extends AbstractPDFExtractor
{
    public AlpianPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Alpian SA");

        addBuySellTransaction();
        addDividendTransaction();
        addFeesTransaction();
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
                        .match("^Quantity: (?<shares>[\\.,\\d]+)$") //
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
                        .match("^Total net amount (debited|credited): (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,\\d]+)$") //
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
                                                        .match("^Stamp tax duty: (?<taxCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<tax>[\\.,\\d]+)$") //
                                                        .match("^(Total net amount paid|Net amount): (?<termCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<fxAmount>[\\.,\\d]+)$") //
                                                        .match("^Total net amount (debited|credited): (?<baseCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,\\d]+)$") //
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
                                                        .match("^(Total net amount paid|Net amount): (?<termCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<fxAmount>[\\.,\\d]+)$") //
                                                        .match("^Total net amount (debited|credited): (?<baseCurrency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,\\d]+)$") //
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
                        .match("^Qualifying nominal: (?<shares>[\\.,\\d]+)$") //
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
                        .match("^Net amount: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,\\d]+)$") //
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
                        .match("^Amount: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<amount>[\\.,\\d]+)$") //
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
                        .match("^Stamp tax duty: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Witholding tax: CHF 0.61
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Withh?olding tax: (?<currency>[A-Z]{3}) (\\-[\\s]*)?(?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
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
