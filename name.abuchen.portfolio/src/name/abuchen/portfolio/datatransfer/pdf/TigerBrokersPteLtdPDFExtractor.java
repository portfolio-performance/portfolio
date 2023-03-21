package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@SuppressWarnings("nls")
public class TigerBrokersPteLtdPDFExtractor extends AbstractPDFExtractor
{
    public TigerBrokersPteLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Tiger Brokers (Singapore) PTE.LTD."); //$NON-NLS-1$

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Tiger Brokers (Singapore) Pte. Ltd."; //$NON-NLS-1$
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Activity Statement", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^Currency: (?<currency>[\\w]{3})$");
            Pattern pSecurityCurrency = Pattern.compile("^Stock Currency: (?<securityCurrency>[\\w]{3})$");
            Pattern pSecurity = Pattern.compile("^(?<tickerSymbol>[\\w]{2,3}) (?<name>.*) [\\d]$");
            Pattern pSecurityDividendTax = Pattern.compile("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<tickerSymbol>[\\w]{2,4}) Cash Dividend .* \\-(?<tax>[\\.,\\d]+).*$");
            Pattern pSecurityDividendShares = Pattern.compile("^(?<tickerSymbol>[\\w]{2,3}) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}.* (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+$");
            Pattern pSecurityBlockStart = Pattern.compile("^Stock$");
            Pattern pSecurityBlockEnd = Pattern.compile("^Base Currency Exchange Rate$");
            Pattern pSecurityDividendTaxStart = Pattern.compile("^Withholding Tax$");
            Pattern pSecurityDividendTaxEnd = Pattern.compile("^Change in Dividend Accruals$");
            Pattern pSecurityDividendSharesStart = Pattern.compile("^Change in Dividend Accruals$");
            Pattern pSecurityDividendSharesEnd = Pattern.compile("^GST$");

            // Set start and end line for the securities list
            int startBlockSecurityList = 0;
            int endBlockSecurityList = lines.length;

            // Set start and end line for the list of dividend taxes
            int startBlockDividendTaxList = 0;
            int endBlockDividendTaxList = lines.length;

            // Set start and end line for the list of shares for dividends
            int startBlockDividendSharesList = 0;
            int endBlockDividendSharesList = lines.length;

            String securityCurrency = CurrencyUnit.USD;
            String baseCurrency = CurrencyUnit.USD;

            for (int i = lines.length - 1; i >= 1; i--)
            {
                Matcher m = pCurrency.matcher(lines[i]);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pSecurityCurrency.matcher(lines[i]);
                if (m.matches())
                    securityCurrency = m.group("securityCurrency");

                m = pSecurityBlockStart.matcher(lines[i]);
                if (m.matches())
                    startBlockSecurityList = i;

                m = pSecurityBlockEnd.matcher(lines[i]);
                if (m.matches())
                    endBlockSecurityList = i;

                m = pSecurityDividendTaxStart.matcher(lines[i]);
                if (m.matches())
                    startBlockDividendTaxList = i;

                m = pSecurityDividendTaxEnd.matcher(lines[i]);
                if (m.matches())
                    endBlockDividendTaxList = i;   

                m = pSecurityDividendSharesStart.matcher(lines[i]);
                if (m.matches())
                    startBlockDividendSharesList = i;

                m = pSecurityDividendSharesEnd.matcher(lines[i]);
                if (m.matches())
                    endBlockDividendSharesList = i;
            }

            for (int i = endBlockSecurityList - 1; i >= startBlockSecurityList; i--)
            {
                Matcher m = pSecurity.matcher(lines[i]);
                if (m.matches())
                {
                    // @formatter:off
                    // Stringbuilder:
                    // security_(security name)_(security currency) = tickerSymbol
                    // 
                    // Example:
                    // Stock
                    // Symbol Issuer Description Multiplier Expiry Strike Right
                    // QQQ Invesco QQQ Trust 1
                    // @formatter:on
                    StringBuilder securityListKey = new StringBuilder("security_");
                    securityListKey.append(trim(m.group("name"))).append("_");
                    securityListKey.append(securityCurrency);
                    context.put(securityListKey.toString(), m.group("tickerSymbol"));
                }
            }

            for (int i = endBlockDividendSharesList - 1; i >= startBlockDividendSharesList; i--)
            {
                Matcher m = pSecurityDividendShares.matcher(lines[i]);
                if (m.matches())
                {
                    // @formatter:off
                    // Stringbuilder:
                    // pSecurityDividendShares_(shares) = tickerSymbol
                    // 
                    // Example:
                    // Change in Dividend Accruals
                    // Symbol Date Ex Date Pay Date Quantity Tax GST Fee(include ADR) Gross Rate Gross Amount Net Amount Code
                    // QQQ 2022-03-21 2022-03-21T00:00-04:00[US/Eastern] 2022-04-29T00:00-04:00[US/Eastern] 52 6.77 0.00 0.00 22.55 15.78
                    // @formatter:on
                    StringBuilder dividendSharesBySecurityKey = new StringBuilder("securityDividendShares_");
                    dividendSharesBySecurityKey.append(m.group("shares"));
                    context.put(dividendSharesBySecurityKey.toString(), m.group("tickerSymbol"));
                }
            }

            for (int i = endBlockDividendTaxList - 1; i >= startBlockDividendTaxList; i--)
            {
                Matcher m = pCurrency.matcher(lines[i]);
                if (m.matches())
                    baseCurrency = m.group("currency");
                
                m = pSecurityDividendTax.matcher(lines[i]);
                if (m.matches())
                {
                    // @formatter:off
                    // Stringbuilder:
                    // pSecurityDividendTax_(tax)_(security currency) = tickerSymbol
                    // 
                    // Example:
                    // Withholding Tax
                    // Date Description Amount
                    // 2022-03-24 VT Cash Dividend 0.2572 USD per Share - Tax -5.32
                    // @formatter:on
                    StringBuilder dividendTaxBySecurityKey = new StringBuilder("securityDividendTax_");
                    dividendTaxBySecurityKey.append(m.group("tax")).append("_");
                    dividendTaxBySecurityKey.append(baseCurrency);
                    context.put(dividendTaxBySecurityKey.toString(), m.group("tickerSymbol"));
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

        Block firstRelevantLineForBuyBlock_Format01 = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_Format01);
        firstRelevantLineForBuyBlock_Format01.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format01.set(buyBlock_Format01);

        buyBlock_Format01
                // @formatter:off
                // Settlement Fee: -0.14
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // Platform Fee: -1.00
                // @formatter:on
                .section("tickerSymbol", "date", "time", "shares", "amount")
                .match("^Settlement Fee: \\-[\\.,\\d]+$")
                .match("^(?<tickerSymbol>[\\w]{2,4}) "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* "
                                + "(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+) "
                                + "Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$")
                .match("^Platform Fee: \\-[\\.,\\d]+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.get("tickerSymbol"));
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("tickerSymbol", securityData.getTickerSymbol());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDate(asDate(v.get("date"), v.get("time")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
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

        Block firstRelevantLineForBuyBlock_Format02 = new Block("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format02);
        firstRelevantLineForBuyBlock_Format02.set(buyBlock_Format02);

        buyBlock_Format02
                // @formatter:off
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("tickerSymbol", "date", "time", "shares", "amount").optional()
                .match("^(?<tickerSymbol>[\\w]{2,4}) "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* "
                                + "(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+) "
                                + "Commission: \\-[\\.,\\d]+(\\s)?Platform Fee: \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.get("tickerSymbol"));
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("tickerSymbol", securityData.getTickerSymbol());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDate(asDate(v.get("date"), v.get("time")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(buyBlock_Format02, type);

        Transaction<AccountTransaction> dividendBlock = new Transaction<>();
        dividendBlock.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        Block firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\w]{2,4} Cash Dividend .* [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock
                // @formatter:off
                // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
                // 2022-12-22 VT Cash Dividend 0.6381 USD per Share (Ordinary Dividend) 44.03 USD
                // @formatter:on
                .section("date", "tickerSymbol", "perShare", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) "
                                + "(?<tickerSymbol>[\\w]{2,4}) "
                                + "Cash Dividend "
                                + "(?<perShare>[\\.,\\d]+) "
                                + "[\\w]{3} per Share "
                                + "\\((?<note>.*)\\) "
                                + "(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    Money tax = null;

                    Security securityData = getSecurity(context, v.get("tickerSymbol"));
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("tickerSymbol", securityData.getTickerSymbol());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(v.get("date")));

                    // Set dividend shares
                    SecurityDividendShares securityDividendeShares = getSecurityDividendeShares(context, v.get("tickerSymbol"));
                    if (securityDividendeShares != null)
                    {
                        t.setShares(asShares(securityDividendeShares.getShares()));
                    }

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));

                    // Set dividend tax
                    SecurityDividendTax securityDividendeTax = getSecurityDividendeTax(context, v.get("tickerSymbol"));
                    if (securityDividendeTax != null)
                    {
                        tax = Money.of(asCurrencyCode(securityDividendeTax.getCurrency()), asAmount(securityDividendeTax.getTax()));

                        checkAndSetTax(tax, t, type.getCurrentContext());
                    }

                    // Dividends are stated in gross.
                    // If taxes exist, then we subtract this amount.
                    if (tax != null)
                        t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));

                    t.setNote(trim(v.get("note")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0 && t.getShares() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        Transaction<AccountTransaction> depositBlock = new Transaction<>();
        depositBlock.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        });

        Block firstRelevantLineForDepositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Deposit .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDepositBlock);
        firstRelevantLineForDepositBlock.set(depositBlock);

        depositBlock
                // @formatter:off
                // 2022-03-02 Deposit DR-3649942 30,000.00
                // @formatter:on
                .section("date", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Deposit (?<note>.*) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(trim(v.get("note")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // @formatter:on
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                // @formatter:on
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$")
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

    private Security getSecurity(Map<String, String> context, String tickerSymbol)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("security")) //$NON-NLS-1$
            {
                if (context.get(key).equals(tickerSymbol))
                {
                    // returns security name, tickerSymbol, security currency
                    return new Security(parts[1], context.get(key), parts[2]);
                }
            }
        }
        return null;
    }

    private static class Security
    {
        public Security(String name, String tickerSymbol, String currency)
        {
            this.name = name;
            this.tickerSymbol = tickerSymbol;
            this.currency = currency;
        }

        private String name;
        private String tickerSymbol;
        private String currency;

        public String getName()
        {
            return name;
        }

        public String getTickerSymbol()
        {
            return tickerSymbol;
        }

        public String getCurrency()
        {
            return currency;
        }
    }

    private static class SecurityDividendShares
    {
        public SecurityDividendShares(String tickerSymbol, String shares)
        {
            this.shares = shares;
        }

        private String shares;

        public String getShares()
        {
            return shares;
        }
    }

    private SecurityDividendShares getSecurityDividendeShares(Map<String, String> context, String tickerSymbol)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("securityDividendShares")) //$NON-NLS-1$
            {
                if (context.get(key).equals(tickerSymbol))
                {
                    // returns tickerSymbol, shares
                    return new SecurityDividendShares(context.get(key), parts[1]);
                }
            }
        }
        return null;
    }

    private static class SecurityDividendTax
    {
        public SecurityDividendTax(String tickerSymbol, String tax, String currency)
        {
            this.tax = tax;
            this.currency = currency;
        }

        private String tax;
        private String currency;

        public String getTax()
        {
            return tax;
        }

        public String getCurrency()
        {
            return currency;
        }
    }

    private SecurityDividendTax getSecurityDividendeTax(Map<String, String> context, String tickerSymbol)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("securityDividendTax")) //$NON-NLS-1$
            {
                if (context.get(key).equals(tickerSymbol))
                {
                    // returns tickerSymbol, tax, currency
                    return new SecurityDividendTax(context.get(key), parts[1], parts[2]);
                }
            }
        }
        return null;
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
