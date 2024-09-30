package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Tiger Brokers (Singapore) PTE.LTD. is a US-based financial services company.
 *           The currency is USD.
 *
 *           All security currencies are USD.
 *           When there is no trade in progress, the securities currency is not issued.
 *           We then set the securities currency to USD.
 *           @see Test file --> AccountStatement06.txt
 *
 * @implSpec In case of purchase and sale, the amount is given in gross.
 *           To get the correct net amount, we need to add the fees.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class TigerBrokersPteLtdPDFExtractor extends AbstractPDFExtractor
{
    private static final DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TigerBrokersPteLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Tiger Brokers (Singapore) PTE.LTD.");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Tiger Brokers (Singapore) Pte. Ltd.";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Activity Statement", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* Base Currency : (?<currency>[\\w]{3})$");
            Pattern pSecurity = Pattern.compile("^(?<tickerSymbol>(?!(GST|Net))[A-Z0-9]{2,4}) (?<name>.*) [\\d]$");
            Pattern pSecurityCurrency = Pattern.compile("^Stock Currency: (?<securityCurrency>[\\w]{3})$");
            Pattern pDividendTaxes = Pattern.compile("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<tickerSymbol>[A-Z0-9]{2,4}) Cash Dividend .* \\-(?<tax>[\\.,\\d]+).*$");

            String securityCurrency = null;

            for (String line : lines)
            {
                Matcher mCurrency = pCurrency.matcher(line);
                if (mCurrency.matches())
                    context.put("currency", mCurrency.group("currency"));

                Matcher mSecurityCurrency = pSecurityCurrency.matcher(line);
                if (mSecurityCurrency.matches())
                    securityCurrency = mSecurityCurrency.group("securityCurrency");
            }

            // Create a helper to store the list of security items found in the document
            SecurityListHelper securityListHelper = new SecurityListHelper();
            context.putType(securityListHelper);

            // Extract security information using pSecurity pattern and add
            // pSecurityCurrency pattern
            List<SecurityItem> securityItems = new ArrayList<>();

            for (String line : lines)
            {
                Matcher mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    SecurityItem securityItem = new SecurityItem();
                    securityItem.tickerSymbol = mSecurity.group("tickerSymbol");
                    securityItem.name = mSecurity.group("name");
                    securityItem.currency = (securityCurrency == null) ? CurrencyUnit.USD : asCurrencyCode(securityCurrency);
                    securityItems.add(securityItem);
                    securityListHelper.items.add(securityItem);
                }
            }

            // Create a helper to store the list of dividend taxes items
            DividendTaxesTransactionListHelper dividendTaxesTransactionListHelper = new DividendTaxesTransactionListHelper();
            context.putType(dividendTaxesTransactionListHelper);

            // Extract dividend taxes using pDividendTaxes pattern
            List<DividendTaxesTransactionItem> dividendTaxesTransactionItems = new ArrayList<>();

            for (String line : lines)
            {
                Matcher mDividendTaxes = pDividendTaxes.matcher(line);
                if (mDividendTaxes.matches())
                {
                    DividendTaxesTransactionItem dividendTaxesTransactionItem = new DividendTaxesTransactionItem();
                    dividendTaxesTransactionItem.tickerSymbol = mDividendTaxes.group("tickerSymbol");
                    dividendTaxesTransactionItem.date = LocalDate.parse(mDividendTaxes.group("date"), DATEFORMAT);
                    dividendTaxesTransactionItem.taxes = asAmount(mDividendTaxes.group("tax"));
                    dividendTaxesTransactionItems.add(dividendTaxesTransactionItem);
                    dividendTaxesTransactionListHelper.items.add(dividendTaxesTransactionItem);
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> buyBlock_Format01 = new Transaction<>();

        // @formatter:off
        // Settlement Fee: -0.14
        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
        // Platform Fee: -1.00
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format01 = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_Format01);
        firstRelevantLineForBuyBlock_Format01.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format01.set(buyBlock_Format01);

        buyBlock_Format01 //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3") //
                        .match("^Settlement Fee: \\-[\\.,\\d]+$") //
                        .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+) " //
                                        + "\\-(?<fee2>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$") //
                        .match("^Platform Fee: \\-(?<fee3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();

                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                            .orElseGet(SecurityListHelper::new);
                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                            if (securityItem.isPresent())
                            {
                                v.put("name", securityItem.get().name);
                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                v.put("currency", securityItem.get().currency);
                            }

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                                return new BuySellEntryItem(t);
                            return null;
                        });

        addFeesSectionsTransaction(buyBlock_Format01, type);

        Transaction<BuySellEntry> buyBlock_Format02 = new Transaction<>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format02 = new Block("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format02);
        firstRelevantLineForBuyBlock_Format02.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format02.set(buyBlock_Format02);

        buyBlock_Format02 //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+)" //
                                        + "(\\s)?Platform Fee: \\-(?<fee2>[\\.,\\d]+) " //
                                        + "\\-(?<fee3>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();

                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                            .orElseGet(SecurityListHelper::new);
                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                            if (securityItem.isPresent())
                            {
                                v.put("name", securityItem.get().name);
                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                v.put("currency", securityItem.get().currency);
                            }

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                                return new BuySellEntryItem(t);
                            return null;
                        });

        addFeesSectionsTransaction(buyBlock_Format02, type);

        Transaction<BuySellEntry> buyBlock_Format03 = new Transaction<>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format03 = new Block("^[A-Z0-9]{2,4} [A-Z]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format03);
        firstRelevantLineForBuyBlock_Format03.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format03.set(buyBlock_Format03);

        buyBlock_Format03 //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "shares", "gross", "fee1", "fee2", "date", "fee3", "time").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) [A-Z]+ " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+) " //
                                        + "\\-(?<fee2>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})" //
                                        + "(\\s)?Platform Fee: \\-(?<fee3>[\\.,\\d]+) " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();

                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                            .orElseGet(SecurityListHelper::new);
                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                            if (securityItem.isPresent())
                            {
                                v.put("name", securityItem.get().name);
                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                v.put("currency", securityItem.get().currency);
                            }

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                                return new BuySellEntryItem(t);
                            return null;
                        });

        addFeesSectionsTransaction(buyBlock_Format03, type);

        Transaction<BuySellEntry> buyBlock_Format04 = new Transaction<>();

        Block firstRelevantLineForBuyBlock_Format04 = new Block("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$");
        type.addBlock(firstRelevantLineForBuyBlock_Format04);
        firstRelevantLineForBuyBlock_Format04.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format04.set(buyBlock_Format04);

        buyBlock_Format04 //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Commission: 2024-04-08
                                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                                        // -1.00 GMT+8 10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tickerSymbol", "shares", "gross", "fee1", "fee2", "time", "fee3") //
                                                        .match("^Commission: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) .* " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<gross>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "\\-(?<fee1>[\\.,\\d]+) .*Platform Fee: " //
                                                                        + "(\\-)?(?<fee2>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                                                        .match("^\\-(?<fee3>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();

                                                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                                                            .orElseGet(SecurityListHelper::new);
                                                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                                                            if (securityItem.isPresent())
                                                            {
                                                                v.put("name", securityItem.get().name);
                                                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                                                v.put("currency", securityItem.get().currency);
                                                            }

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross
                                                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                                                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Commission: 2023-01-06
                                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                                        // -1.00 GMT+8
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tickerSymbol", "shares", "gross", "fee1", "fee2", "time", "fee3") //
                                                        .match("^Commission: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) .* " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<gross>[\\.,\\d]+) " //
                                                                        + "\\-(?<fee1>[\\.,\\d]+)Platform Fee: " //
                                                                        + "(\\-)?(?<fee2>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ (\\-)[\\.,\\d]+ " //
                                                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                                                        .match("^\\-(?<fee3>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            DocumentContext context = type.getCurrentContext();

                                                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                                                            .orElseGet(SecurityListHelper::new);
                                                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                                                            if (securityItem.isPresent())
                                                            {
                                                                v.put("name", securityItem.get().name);
                                                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                                                v.put("currency", securityItem.get().currency);
                                                            }

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross
                                                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                                                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                                        }))

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                                return new BuySellEntryItem(t);
                            return null;
                        });

        addFeesSectionsTransaction(buyBlock_Format04, type);

        Transaction<AccountTransaction> dividendBlock = new Transaction<>();

        // @formatter:off
        // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
        // 2022-12-22 VT Cash Dividend 0.6381 USD per Share (Ordinary Dividend) 44.03 USD
        // @formatter:on
        Block firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]{2,4} Cash Dividend .* [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .section("date", "tickerSymbol", "amountPerShare", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "(?<tickerSymbol>[A-Z0-9]{2,4}) " //
                                        + "Cash Dividend " //
                                        + "(?<amountPerShare>[\\.,\\d]+) " //
                                        + "[\\w]{3} per Share " //
                                        + "\\((?<note>.*)\\) " //
                                        + "(?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();

                            SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class)
                                            .orElseGet(SecurityListHelper::new);
                            Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                            if (securityItem.isPresent())
                            {
                                v.put("name", securityItem.get().name);
                                v.put("tickerSymbol", securityItem.get().tickerSymbol);
                                v.put("currency", securityItem.get().currency);
                            }

                            t.setDateTime(asDate(v.get("date")));

                            // Calculation of dividend shares and rounding to
                            // whole shares
                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            BigDecimal amount = BigDecimal.valueOf(asAmount(v.get("amount")));
                            t.setShares(amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP) //
                                            .setScale(0, RoundingMode.HALF_UP) //
                                            .movePointRight(Values.Share.precision()) //
                                            .longValue());

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setSecurity(getOrCreateSecurity(v));

                            // Set dividend taxes, if available
                            DividendTaxesTransactionListHelper divdendTaxesTransactionListHelper = context
                                            .getType(DividendTaxesTransactionListHelper.class)
                                            .orElseGet(DividendTaxesTransactionListHelper::new);
                            Optional<DividendTaxesTransactionItem> divdendTaxesTransactionItem = divdendTaxesTransactionListHelper
                                            .findItem(v.get("tickerSymbol"), LocalDate.parse(v.get("date"), DATEFORMAT));

                            if (divdendTaxesTransactionItem.isPresent())
                            {
                                Money tax = Money.of(asCurrencyCode(context.get("currency")), divdendTaxesTransactionItem.get().taxes);
                                checkAndSetTax(tax, t, type.getCurrentContext());

                                // Dividend are stated in gross.
                                // If taxes exist, then we subtract this amount.
                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            }

                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);
                            type.getCurrentContext().removeType(DividendTaxesTransactionItem.class);

                            return new TransactionItem(t);
                        });

        Transaction<AccountTransaction> taxRefundBlock = new Transaction<>();

        // @formatter:off
        // 2023-09-21 Cash Dividend USD per Share - Tax 12.29 USD
        // @formatter:on
        Block firstRelevantLineForTaxRefundBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Cash Dividend [\\w]{3} per Share \\- Tax [\\.,\\d]+ [\\w]{3}$");
        type.addBlock(firstRelevantLineForTaxRefundBlock);
        firstRelevantLineForTaxRefundBlock.set(taxRefundBlock);

        taxRefundBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("date", "amount", "currency") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "Cash Dividend [\\w]{3} per Share \\- Tax " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        Transaction<AccountTransaction> depositBlock = new Transaction<>();

        // @formatter:off
        // 2022-03-02 Deposit DR-3649942 30,000.00
        // @formatter:on
        Block firstRelevantLineForDepositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Deposit .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDepositBlock);
        firstRelevantLineForDepositBlock.set(depositBlock);

        depositBlock //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Deposit " //
                                        + "(?<note>.*) " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: (?<fee>\\-[\\.,\\d]+) \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: (?<fee>\\-[\\.,\\d]+) [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<gross>[\\.,\\d]+) [\\.,\\d]+ \\-[\\.,\\d]+ .*Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) .*Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+ .*Platform Fee: (\\-)?(?<fee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-(?<fee>[\\.,\\d]+)Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+Platform Fee: (\\-)?(?<fee>[\\.,\\d]+) [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Settlement Fee: -0.14
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Settlement Fee: \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Platform Fee: -1.00
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Platform Fee: \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private static class SecurityItem
    {
        String tickerSymbol;
        String name;
        String currency;

        @Override
        public String toString()
        {
            return "SecurityItem [tickerSymbol=" + tickerSymbol + ", name=" + name + ", currency=" + currency + "]";
        }
    }

    private static class SecurityListHelper
    {
        private List<SecurityItem> items = new ArrayList<>();

        // Finds a SecurityItem in the list
        public Optional<SecurityItem> findItem(String tickerSymbol)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (SecurityItem item : items) // NOSONAR
            {
                if (!item.tickerSymbol.equals(tickerSymbol))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class DividendTaxesTransactionItem
    {
        String tickerSymbol;
        LocalDate date;
        Long taxes;

        @Override
        public String toString()
        {
            return "DividendTaxesTransactionItem [tickerSymbol=" + tickerSymbol + ", date=" + date + ", taxes=" + taxes
                            + "]";
        }
    }

    private static class DividendTaxesTransactionListHelper
    {
        private List<DividendTaxesTransactionItem> items = new ArrayList<>();

        public Optional<DividendTaxesTransactionItem> findItem(String tickerSymbol, LocalDate date)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (DividendTaxesTransactionItem item : items) // NOSONAR
            {
                if (!item.tickerSymbol.equals(tickerSymbol))
                    continue;

                if (!item.date.equals(date))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
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
