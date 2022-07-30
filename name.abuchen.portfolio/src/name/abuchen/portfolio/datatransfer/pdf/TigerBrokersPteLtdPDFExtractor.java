package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Pattern pSecurityDividendTax = Pattern.compile("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<tickerSymbol>[\\w]{2,4}) Cash Dividend .* \\-(?<tax>[\\.,\\d]+)$");
            Pattern pSecurityBlockStart = Pattern.compile("^Stock$");
            Pattern pSecurityBlockEnd = Pattern.compile("^Base Currency Exchange Rate$");
            Pattern pSecurityDividendTaxStart = Pattern.compile("^Withholding Tax$");
            Pattern pSecurityDividendTaxEnd = Pattern.compile("^Total$");

            // Set start and end line for security list
            int startBlockSecurityList = 0;
            int endBlockSecurityList = lines.length;

            // Set start and end line for security list
            int startBlockDividendTaxList = 0;
            int endBlockDividendTaxList = lines.length;

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
                    securityListKey.append(securityCurrency).append("_");
                    context.put(securityListKey.toString(), m.group("tickerSymbol"));
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
                    dividendTaxBySecurityKey.append(baseCurrency).append("_");
                    context.put(dividendTaxBySecurityKey.toString(), m.group("tickerSymbol"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> buyBlock = new Transaction<>();
        buyBlock.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLineForBuyBlock = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock);
        firstRelevantLineForBuyBlock.set(buyBlock);

        buyBlock
                // Settlement Fee: -0.14
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                // Platform Fee: -1.00
                .section("tickerSymbol", "date", "time", "shares", "amount")
                .match("^Settlement Fee: \\-[\\.,\\d]+$")
                .match("^(?<tickerSymbol>[\\w]{2,4}) "
                                + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), "
                                + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* "
                                + "(?<shares>[\\.,\\d]+) "
                                + "[\\.,\\d]+ [\\.,\\d]+ "
                                + "(?<amount>[\\.,\\d]+).*$")
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
                    if (t.getPortfolioTransaction().getCurrencyCode() != null)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(buyBlock, type);

        Transaction<AccountTransaction> dividendBlock = new Transaction<>();
        dividendBlock.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        Block firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\w]{2,4} Cash Dividend .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock
                // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
                .section("date", "tickerSymbol", "perShare", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) "
                                + "(?<tickerSymbol>[\\w]{2,4}) "
                                + "Cash Dividend "
                                + "(?<perShare>[\\.,\\d]+) "
                                + "[\\w]{3} per Share "
                                + "\\((?<note>.*)\\) "
                                + "(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.get("tickerSymbol"));
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("tickerSymbol", securityData.getTickerSymbol());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(v.get("date")));

                    // Calculate shares
                    BigDecimal shares = asBigDecimal(v.get("perShare"));
                    t.setShares(Values.Share.factorize(asAmount(v.get("amount")) / shares.doubleValue()) * 100);

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setSecurity(getOrCreateSecurity(v));

                    // Set dividend tax
                    SecurityDividendTax securityDividendeTax = getSecurityDividendeTax(context, v.get("tickerSymbol"));
                    if (securityDividendeTax != null)
                    {
                        v.put("tickerSymbol", securityDividendeTax.getTickerSymbol());
                        v.put("tax", securityDividendeTax.getTax());
                        v.put("currency", asCurrencyCode(securityDividendeTax.getCurrency()));

                        Money tax = null;
                        tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                        checkAndSetTax(tax, t, type);
                    }
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
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
                // 2022-03-02 Deposit DR-3649942 30,000.00
                .section("date", "note", "amount")
                .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Deposit (?<note>.*) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote(v.get("note"));
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
                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-(?<fee>[\\.,\\d]+) \\-[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                .section("fee").optional()
                .match("^[\\w]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // Settlement Fee: -0.14
                .section("fee").optional()
                .match("^Settlement Fee: \\-(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("currency")));

                    processFeeEntries(t, v, type);
                })

                // Platform Fee: -1.00
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

    private static class SecurityDividendTax
    {
        public SecurityDividendTax(String tickerSymbol, String tax, String currency)
        {
            this.tickerSymbol = tickerSymbol;
            this.tax = tax;
            this.currency = currency;
        }

        private String tickerSymbol;
        private String tax;
        private String currency;

        public String getTickerSymbol()
        {
            return tickerSymbol;
        }

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
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
