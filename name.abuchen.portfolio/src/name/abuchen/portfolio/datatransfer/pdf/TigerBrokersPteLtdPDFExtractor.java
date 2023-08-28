package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;

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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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
            Pattern pCurrency = Pattern.compile("^Currency: (?<currency>[\\w]{3})$");
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

            // Extract security information using pSecurity pattern and add pSecurityCurrency pattern
            List<SecurityItem> securityItems = new ArrayList<>();

            for (String line : lines)
            {
                Matcher mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    SecurityItem securityItem = new SecurityItem();
                    securityItem.tickerSymbol = mSecurity.group("tickerSymbol");
                    securityItem.name = mSecurity.group("name");
                    securityItem.currency = asCurrencyCode(securityCurrency);
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
        buyBlock_Format01.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // @formatter:off
        // Settlement Fee: -0.14
        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
        // Platform Fee: -1.00
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format01 = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_Format01);
        firstRelevantLineForBuyBlock_Format01.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format01.set(buyBlock_Format01);

        buyBlock_Format01
                .section("tickerSymbol", "date", "time", "shares", "amount")
                .match("^Settlement Fee: \\-[\\.,\\d]+$")
                .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* "
                                + "(?<shares>[\\.,\\d]+) "
                                + "[\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+) "
                                + "Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$")
                .match("^Platform Fee: \\-[\\.,\\d]+$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                    Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                    if (securityItem.isPresent())
                    {
                        v.put("name", securityItem.get().name);
                        v.put("tickerSymbol", securityItem.get().tickerSymbol);
                        v.put("currency", securityItem.get().currency);
                    }

                    t.setDate(asDate(v.get("date"), v.get("time")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    type.getCurrentContext().removeType(SecurityItem.class);

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(buyBlock_Format01, type);

        Transaction<BuySellEntry> buyBlock_Format02 = new Transaction<>();
        buyBlock_Format02.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format02 = new Block("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format02);
        firstRelevantLineForBuyBlock_Format02.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format02.set(buyBlock_Format02);

        buyBlock_Format02
                .section("tickerSymbol", "date", "time", "shares", "amount").optional()
                .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* "
                                + "(?<shares>[\\.,\\d]+) "
                                + "[\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+) "
                                + "Commission: \\-[\\.,\\d]+(\\s)?Platform Fee: \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                    Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                    if (securityItem.isPresent())
                    {
                        v.put("name", securityItem.get().name);
                        v.put("tickerSymbol", securityItem.get().tickerSymbol);
                        v.put("currency", securityItem.get().currency);
                    }

                    t.setDate(asDate(v.get("date"), v.get("time")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    type.getCurrentContext().removeType(SecurityItem.class);

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(buyBlock_Format02, type);

        Transaction<BuySellEntry> buyBlock_Format03 = new Transaction<>();
        buyBlock_Format03.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        Block firstRelevantLineForBuyBlock_Format03 = new Block("^[A-Z0-9]{2,4} [A-Z]+ (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format03);
        firstRelevantLineForBuyBlock_Format03.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format03.set(buyBlock_Format03);

        buyBlock_Format03
                .section("tickerSymbol", "date", "time", "shares", "amount").optional()
                .match("^(?<tickerSymbol>[A-Z0-9]{2,4}) [A-Z]+ "
                                + "(?<shares>[\\.,\\d]+) "
                                + "[\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+) "
                                + "Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})(\\s)?Platform Fee: \\-[\\.,\\d]+ "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                    Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                    if (securityItem.isPresent())
                    {
                        v.put("name", securityItem.get().name);
                        v.put("tickerSymbol", securityItem.get().tickerSymbol);
                        v.put("currency", securityItem.get().currency);
                    }

                    t.setDate(asDate(v.get("date"), v.get("time")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    type.getCurrentContext().removeType(SecurityItem.class);

                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(buyBlock_Format03, type);

        Transaction<AccountTransaction> dividendBlock = new Transaction<>();
        dividendBlock.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        // @formatter:off
        // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
        // 2022-12-22 VT Cash Dividend 0.6381 USD per Share (Ordinary Dividend) 44.03 USD
        // @formatter:on
        Block firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]{2,4} Cash Dividend .* [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock
                .section("date", "tickerSymbol", "amountPerShare", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) "
                                + "(?<tickerSymbol>[A-Z0-9]{2,4}) "
                                + "Cash Dividend "
                                + "(?<amountPerShare>[\\.,\\d]+) "
                                + "[\\w]{3} per Share "
                                + "\\((?<note>.*)\\) "
                                + "(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                    Optional<SecurityItem> securityItem = securityListHelper.findItem(v.get("tickerSymbol"));

                    if (securityItem.isPresent())
                    {
                        v.put("name", securityItem.get().name);
                        v.put("tickerSymbol", securityItem.get().tickerSymbol);
                        v.put("currency", securityItem.get().currency);
                    }

                    t.setDateTime(asDate(v.get("date")));

                    // Calculation of dividend shares and rounding to whole shares
                    BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                    BigDecimal amount = BigDecimal.valueOf(asAmount(v.get("amount")));
                    BigDecimal shares = amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                    BigDecimal roundedShares = shares.setScale(0, RoundingMode.HALF_UP); // Rounding to whole shares
                    t.setShares(roundedShares.movePointRight(Values.Share.precision()).longValue());

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));

                    // Set dividend taxes, if available
                    DividendTaxesTransactionListHelper divdendTaxesTransactionListHelper = context.getType(DividendTaxesTransactionListHelper.class).orElseGet(DividendTaxesTransactionListHelper::new);
                    Optional<DividendTaxesTransactionItem> divdendTaxesTransactionItem = divdendTaxesTransactionListHelper.findItem(v.get("tickerSymbol"), LocalDate.parse(v.get("date"), DATEFORMAT));

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

        Transaction<AccountTransaction> depositBlock = new Transaction<>();
        depositBlock.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        });

        // @formatter:off
        // 2022-03-02 Deposit DR-3649942 30,000.00
        // @formatter:on
        Block firstRelevantLineForDepositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Deposit .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDepositBlock);
        firstRelevantLineForDepositBlock.set(depositBlock);

        depositBlock
                .section("date", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Deposit (?<note>.*) (?<amount>[\\.,\\d]+)$")
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
        transaction
                // @formatter:off
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: (?<fee>\\-[\\.,\\d]+) \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                // @formatter:on
                .section("fee").optional()
                .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: (?<fee>\\-[\\.,\\d]+) [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Settlement Fee: -0.14
                // @formatter:on
                .section("fee").optional()
                .match("^Settlement Fee: \\-(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Platform Fee: -1.00
                // @formatter:on
                .section("fee").optional()
                .match("^Platform Fee: \\-(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                });
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
