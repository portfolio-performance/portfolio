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
import java.util.regex.Pattern;

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
 * @implNote Tiger Brokers (Singapore) PTE.LTD. is a US-based financial services company.
 *           The currency is USD.
 *
 *           All security currencies are USD.
 *           When there is no trade in progress, the securities currency is not issued.
 *           We then set the securities currency to USD.
 *           @see Test file --> AccountStatement06.txt
 *
 *           Late 2024 Format
 *           ================
 *           Apparently, the format has changed in late 2024.
 *           https://forum.portfolio-performance.info/t/pdf-import-from-tiger-brokers/20484/37
 *           https://forum.portfolio-performance.info/t/pdf-import-from-tiger-brokers/20484/41
 *           The PDF contains a table with each cell containing a multi-line text with different line wrapping (2-3 lines) which is centered.
 *           We do not use the currency from the context (anymore), but parse it as part of each transaction.
 *           Examples only contain purchases and dividend payments. Therefore we do not yet know if other transaction types (tax refund, deposit) still work.
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
        final var type = new DocumentType("Activity Statement", (context, lines) -> {
            var pCurrency = Pattern.compile("^.* (Base Currency :|Cash) (?<currency>[A-Z]{3})$");
            var pSecurity = Pattern.compile("^(?<tickerSymbol>(?!(GST|Net))[A-Z0-9]{1,6}(?:\\\\.[A-Z]{1,4})?) (?<name>.*) [\\d]$");
            var pSecurityCurrency = Pattern.compile("^Stock Currency: (?<securityCurrency>[A-Z]{3})$");
            var pDividendTaxes = Pattern.compile("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash Dividend .* \\-(?<tax>[\\.,\\d]+).*$");

            String securityCurrency = null;

            for (String line : lines)
            {
                var mCurrency = pCurrency.matcher(line);
                if (mCurrency.matches())
                    context.put("currency", mCurrency.group("currency"));

                var mSecurityCurrency = pSecurityCurrency.matcher(line);
                if (mSecurityCurrency.matches())
                    securityCurrency = mSecurityCurrency.group("securityCurrency");
            }

            // Create a helper to store the list of security items found in the document
            var securityListHelper = new SecurityListHelper();
            context.putType(securityListHelper);

            // Extract security information using pSecurity pattern and add
            // pSecurityCurrency pattern
            List<SecurityItem> securityItems = new ArrayList<>();

            for (String line : lines)
            {
                var mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    var securityItem = new SecurityItem();
                    securityItem.tickerSymbol = mSecurity.group("tickerSymbol");
                    securityItem.name = mSecurity.group("name");
                    securityItem.currency = (securityCurrency == null) ? "USD" : asCurrencyCode(securityCurrency);
                    securityItems.add(securityItem);
                    securityListHelper.items.add(securityItem);
                }
            }

            // Create a helper to store the list of dividend taxes items
            var dividendTaxesTransactionListHelper = new DividendTaxesTransactionListHelper();
            context.putType(dividendTaxesTransactionListHelper);

            // Extract dividend taxes using pDividendTaxes pattern
            List<DividendTaxesTransactionItem> dividendTaxesTransactionItems = new ArrayList<>();

            for (String line : lines)
            {
                var mDividendTaxes = pDividendTaxes.matcher(line);
                if (mDividendTaxes.matches())
                {
                    var dividendTaxesTransactionItem = new DividendTaxesTransactionItem();
                    dividendTaxesTransactionItem.tickerSymbol = mDividendTaxes.group("tickerSymbol");
                    dividendTaxesTransactionItem.date = LocalDate.parse(mDividendTaxes.group("date"), DATEFORMAT);
                    dividendTaxesTransactionItem.taxes = asAmount(mDividendTaxes.group("tax"));
                    dividendTaxesTransactionItems.add(dividendTaxesTransactionItem);
                    dividendTaxesTransactionListHelper.items.add(dividendTaxesTransactionItem);
                }
            }
        });
        this.addDocumentTyp(type);

        var buyBlock_Format01 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Settlement Fee: -0.14
        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
        // Platform Fee: -1.00
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format01 = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_Format01);
        firstRelevantLineForBuyBlock_Format01.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format01.set(buyBlock_Format01);

        buyBlock_Format01 //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3") //
                        .find("Settlement Fee: \\-[\\.,\\d]+") //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
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
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

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

        var buyBlock_Format02 = new Transaction<BuySellEntry>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format02 = new Block("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format02);
        firstRelevantLineForBuyBlock_Format02.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format02.set(buyBlock_Format02);

        buyBlock_Format02 //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
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
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

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

        var buyBlock_Format03 = new Transaction<BuySellEntry>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format03 = new Block("^[A-Z0-9]{2,4} [A-Z]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format03);
        firstRelevantLineForBuyBlock_Format03.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format03.set(buyBlock_Format03);

        buyBlock_Format03 //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("tickerSymbol", "shares", "gross", "fee1", "fee2", "date", "fee3", "time").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) [A-Z]+ " //
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
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

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

        var buyBlock_Format04 = new Transaction<BuySellEntry>();

        var firstRelevantLineForBuyBlock_Format04 = new Block("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$");
        type.addBlock(firstRelevantLineForBuyBlock_Format04);
        firstRelevantLineForBuyBlock_Format04.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format04.set(buyBlock_Format04);

        buyBlock_Format04 //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
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
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* " //
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
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

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
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<gross>[\\.,\\d]+) " //
                                                                        + "\\-(?<fee1>[\\.,\\d]+)Platform Fee: " //
                                                                        + "(\\-)?(?<fee2>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ (\\-)[\\.,\\d]+ " //
                                                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                                                        .match("^\\-(?<fee3>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

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

        var buyBlock_Format05 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Settlement Fee:
        // -0.01 2024-12-19
        // Vanguard Total World Stock ETF
        // US ARCA Open 3 118.29960 354.90 0.00 Commission: 2024-
        // -0.99 0.00 0.00 13:01:49, US USD
        // (VT) 12-20
        // Platform Fee: /Eastern
        // -1.00
        // @formatter:on
        var firstRelevantLineForBuyBlock_late24 = new Block("^Settlement Fee:\\s*$", "^\\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_late24);
        firstRelevantLineForBuyBlock_late24.setMaxSize(9);
        firstRelevantLineForBuyBlock_late24.set(buyBlock_Format05);

        buyBlock_Format05 //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })
                        .section("fee1", "date", "name", "shares", "gross", "fee2", "time", "tickerSymbol", "fee3") //
                        .find("Settlement Fee:[\\s]*") //
                        .match("^\\-(?<fee1>[\\.,\\d]+) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                        .match("^(?<name>.*)$") //
                        .match("^.*(?<shares>[\\.,\\d]+) [\\.,\\d]+ (?<gross>[\\.,\\d]+) [\\.,\\d]+ Commission: [\\d]{4}\\-$") //
                        .match("^\\-(?<fee2>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* [A-Z]{3}$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) [\\d]{2}\\-[\\d]{2}$") //
                        .find("Platform Fee:.*") //
                        .match("^\\-(?<fee3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

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
        
        addFeesSectionsTransaction(buyBlock_Format05, type);

        var dividendBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
        // 2022-12-22 VT Cash Dividend 0.6381 USD per Share (Ordinary Dividend) 44.03 USD
        // @formatter:on
        var firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]{2,4} Cash Dividend .* [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .section("date", "tickerSymbol", "amountPerShare", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
                                        + "Cash Dividend " //
                                        + "(?<amountPerShare>[\\.,\\d]+) " //
                                        + "[A-Z]{3} per Share " //
                                        + "\\((?<note>.*)\\) " //
                                        + "(?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date")));

                            // Calculation of dividend shares and rounding to
                            // whole shares
                            var amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            var amount = BigDecimal.valueOf(asAmount(v.get("amount")));
                            t.setShares(amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP) //
                                            .setScale(0, RoundingMode.HALF_UP) //
                                            .movePointRight(Values.Share.precision()) //
                                            .longValue());

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));

                            // Set dividend taxes, if available
                            var divdendTaxesTransactionListHelper = context
                                            .getType(DividendTaxesTransactionListHelper.class)
                                            .orElseGet(DividendTaxesTransactionListHelper::new);
                            var divdendTaxesTransactionItem = divdendTaxesTransactionListHelper
                                            .findItem(v.get("tickerSymbol"), LocalDate.parse(v.get("date"), DATEFORMAT));

                            if (divdendTaxesTransactionItem.isPresent())
                            {
                                var tax = Money.of(asCurrencyCode(context.get("currency")), divdendTaxesTransactionItem.get().taxes);
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

        var dividendBlock_late24 = new Transaction<AccountTransaction>();

        var firstRelevantLineForDividendBlock_late24 = new Block("^.*Quantity: [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDividendBlock_late24);
        firstRelevantLineForDividendBlock_late24.setMaxSize(6);
        firstRelevantLineForDividendBlock_late24.set(dividendBlock_late24);

        dividendBlock_late24 //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .optionalOneOf(
                                        // Pattern 1: three-line instrument
                                        //
                                        // @formatter:off
                                        // Vanguard Total World Stock Quantity: 72
                                        // 2024-12-
                                        // Stock ETF Gross Rate: 0.88 Paid 63.17 0 Dividend tax: 9.48 53.69 USD
                                        // 26
                                        // (VT) /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes", "amount", "dateDay", "tickerSymbol", "currency") //
                                                        .match("^.* Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\-$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2})$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\/Share$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            var date = v.get("dateYear") + "-" + v.get("dateDay");
                                                            t.setDateTime(asDate(date));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());
                                                        }),
                                        // Pattern 2: two-line instrument
                                        //
                                        // @formatter:off
                                        // Quantity: 25
                                        // 2024-12- Vanguard S&P 500 ETF
                                        // Stock Gross Rate: 1.74 Paid 43.46 0 Dividend tax: 6.52 36.94 USD
                                        // 30 (VOO)
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes",
                                                                        "amount", "dateDay", "tickerSymbol", "currency") //
                                                        .match("^Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\)$") //
                                                        .match("^\\/Share$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            var date = v.get("dateYear") + "-" + v.get("dateDay");
                                                            t.setDateTime(asDate(date));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());
                                                        }),

                                        // Pattern 3: two-line Quantity/Gross
                                        // Rate
                                        //
                                        // @formatter:off
                                        // Invesco QQQ Quantity: 54
                                        // 2025-01-02 Stock Paid 45.07 0 Dividend tax: 6.76 38.31 USD
                                        // (QQQ) Gross Rate: 0.83/Share
                                        // @formatter:on

                                        section -> section //
                                                        .attributes("shares", "date", "grossAmount", "taxes", "amount",
                                                                        "tickerSymbol", "currency") //
                                                        .match("^.* Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) .* Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) .*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());

                                                        })

                        )

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getDateTime() == null)
                                return null;
                            else
                                return new TransactionItem(t);
                        });

        var taxRefundBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2023-09-21 Cash Dividend USD per Share - Tax 12.29 USD
        // @formatter:on
        var firstRelevantLineForTaxRefundBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Cash Dividend [A-Z]{3} per Share \\- Tax [\\.,\\d]+ [A-Z]{3}$");
        type.addBlock(firstRelevantLineForTaxRefundBlock);
        firstRelevantLineForTaxRefundBlock.set(taxRefundBlock);

        taxRefundBlock //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("date", "amount", "currency") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "Cash Dividend [A-Z]{3} per Share \\- Tax " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        var depositBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2022-03-02 Deposit DR-3649942 30,000.00
        // @formatter:on
        var firstRelevantLineForDepositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Deposit .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDepositBlock);
        firstRelevantLineForDepositBlock.set(depositBlock);

        depositBlock //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
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
                        // Settlement Fee:
                        // -0.01 2024-12-19
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Platform Fee:.*") //
                        .match("^\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // US ARCA Open 3 118.29960 354.90 0.00 Commission: 2024-
                        // -0.99 0.00 0.00 13:01:49, US USD
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("^.*[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: [\\d]{4}\\-$") //
                        .match("^\\-(?<fee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Settlement Fee:
                        // -0.01 2024-12-19
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Settlement Fee:[\\s]*") //
                        .match("^\\-(?<fee>[\\.,\\d]+) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
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
        private final List<SecurityItem> items = new ArrayList<>();

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
        private final List<DividendTaxesTransactionItem> items = new ArrayList<>();

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
