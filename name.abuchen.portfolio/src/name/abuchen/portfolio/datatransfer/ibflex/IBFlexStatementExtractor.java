package name.abuchen.portfolio.datatransfer.ibflex;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

@SuppressWarnings("nls")
public class IBFlexStatementExtractor implements Extractor
{
    /**
     * https://ibkrguides.com/reportingreference/reportguide/tradesfq.htm
     */

    private static final String TO_BE_DELETED = "to_be_deleted"; //$NON-NLS-1$

    private final Client client;
    private List<Security> allSecurities;

    private Map<String, String> exchanges;

    public Client getClient()
    {
        return client;
    }

    @Override
    public String getLabel()
    {
        return Messages.IBXML_Label;
    }

    public IBFlexStatementExtractor(Client client)
    {
        this.client = client;
        allSecurities = new ArrayList<>(client.getSecurities());

        // Maps Interactive Broker Exchange to Yahoo Exchanges, to be completed
        this.exchanges = new HashMap<>();

        this.exchanges.put("EBS", "SW");
        this.exchanges.put("LSE", "L"); // London Stock Exchange
        this.exchanges.put("SWX", "SW"); // Swiss Exchange (SWX)
        this.exchanges.put("TSE", "TO"); // TSX Venture
        this.exchanges.put("IBIS", "DE"); // XETRA
        this.exchanges.put("TGATE", "DE"); // TradeGate
        this.exchanges.put("SWB", "SG"); // Stuttgart Stock Exchange
        this.exchanges.put("FWB", "F"); // Frankfurt Stock Exchange
    }

    /**
     * Import an Interactive Broker ActivityStatement from an XML file. It
     * currently only imports Trades, Corporate Transactions and Cash
     * Transactions.
     */
    /* package */ IBFlexStatementExtractorResult importActivityStatement(InputStream f)
    {
        IBFlexStatementExtractorResult result = new IBFlexStatementExtractorResult();
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);

            doc.getDocumentElement().normalize();

            result.parseDocument(doc);
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            result.addError(e);
        }
        return result;
    }

    /**
     * @formatter:off
     * Information on the different asset categorie
     * --------------------------------------------
     * STK  --> Stock
     * FUND --> Fonds
     * IND  --> Indices
     * OPT  --> Options
     * IOPT --> Certificate
     * FOP  --> Future Options
     * WAR  --> Warrants
     * @formatter:on
     */
    private class IBFlexStatementExtractorResult
    {
        private static final String ASSETKEY_STOCK = "STK";
        private static final String ASSETKEY_FUND = "FUND";
        private static final String ASSETKEY_OPTION = "OPT";
        private static final String ASSETKEY_CERTIFICATE = "IOPT";
        private static final String ASSETKEY_FUTURE_OPTION = "FOP";
        private static final String ASSETKEY_WARRANTS = "WAR";

        private Document document;
        private List<Exception> errors = new ArrayList<>();
        private List<Item> results = new ArrayList<>();
        private String ibAccountCurrency = null;

        private Function<Element, Item> importAccountInformation = element -> {
            String currency = asCurrencyCode(element.getAttribute("currency"));
            if (currency != null && !currency.isEmpty())
            {
                CurrencyUnit currencyUnit = CurrencyUnit.getInstance(currency);
                if (currencyUnit != null)
                    ibAccountCurrency = currency;
            }
            return null;
        };

        private Function<Element, Item> buildAccountTransaction = element -> {
            AccountTransaction transaction = new AccountTransaction();

            // @formatter:off
            // New Format dateTime has now also Time [YYYYMMDD;HHMMSS], I cut
            // Date from string [YYYYMMDD]
            //
            // Checks for old format [YYYY-MM-DD, HH:MM:SS], too. Quapla 11.1.20
            // Changed from dateTime to reportDate + Check for old Data-Formats,
            // Quapla 14.2.20
            // @formatter:on
            String dateTime = !element.getAttribute("reportDate").isEmpty() //
                            ? element.getAttribute("reportDate") //
                            : element.getAttribute("dateTime");

            dateTime = dateTime.length() == 15 ? dateTime.substring(0, 8) : dateTime;
            transaction.setDateTime(ExtractorUtils.asDate(dateTime));

            // Set amount
            Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                            asAmount(element.getAttribute("amount")));

            // Set transaction type
            switch (element.getAttribute("type"))
            {
                case "Deposits":
                case "Deposits & Withdrawals":
                case "Deposits/Withdrawals":
                    // Positive amount are a deposit
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        transaction.setType(AccountTransaction.Type.REMOVAL);
                    else
                        transaction.setType(AccountTransaction.Type.DEPOSIT);
                    break;
                case "Dividends":
                case "Payment In Lieu Of Dividends":
                    // Set the Symbol
                    if (element.getAttribute("symbol").length() > 0)
                        transaction.setSecurity(this.getOrCreateSecurity(element, true));

                    transaction.setType(AccountTransaction.Type.DIVIDENDS);
                    this.calculateShares(transaction, element);
                    break;
                case "Withholding Tax":
                    // Set the Symbol
                    if (element.getAttribute("symbol").length() > 0)
                        transaction.setSecurity(this.getOrCreateSecurity(element, true));

                    // Positive amount are a tax refund
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        transaction.setType(AccountTransaction.Type.TAXES);
                    else
                        transaction.setType(AccountTransaction.Type.TAX_REFUND);
                    break;
                case "Broker Interest Received":
                    transaction.setType(AccountTransaction.Type.INTEREST);
                    break;
                case "Broker Interest Paid":
                    transaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    break;
                case "Other Fees":
                    // Positive amount are a fee refund
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        transaction.setType(AccountTransaction.Type.FEES);
                    else
                        transaction.setType(AccountTransaction.Type.FEES_REFUND);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            if (AccountTransaction.Type.DIVIDENDS.equals(transaction.getType())
                            || AccountTransaction.Type.TAXES.equals(transaction.getType()))
            {
                // if the account currency differs from transaction currency
                // convert currency, if there is a matching security with the
                // account currency
                if (this.ibAccountCurrency != null && !this.ibAccountCurrency.equals(amount.getCurrencyCode())) // NOSONAR
                {
                    // matching isin or wkn & base currency
                    boolean foundIsinBase = false;
                    // matching isin & transaction currency
                    boolean foundIsinTransaction = false;
                    // matching conid (wkn) & transaction currency
                    boolean foundWknTransaction = false;

                    for (Security s : allSecurities)
                    {
                        // Find security with same isin or conid (wkn) and
                        // currency
                        if (element.getAttribute("isin").length() > 0 && element.getAttribute("isin").equals(s.getIsin()))
                        {
                            if (amount.getCurrencyCode().equals(s.getCurrencyCode()))
                                foundIsinTransaction = true;
                            else if (this.ibAccountCurrency.equals(s.getCurrencyCode()))
                                foundIsinBase = true;
                        }
                        else if (element.getAttribute("conid").length() > 0 && element.getAttribute("conid").equals(s.getWkn()))
                        {
                            if (amount.getCurrencyCode().equals(s.getCurrencyCode()))
                                foundWknTransaction = true;
                            else if (this.ibAccountCurrency.equals(s.getCurrencyCode()))
                                foundIsinBase = true;
                        }
                    }

                    if ((!foundIsinTransaction || !foundWknTransaction) && foundIsinBase
                                    && element.getAttribute("fxRateToBase").length() > 0)
                    {
                        BigDecimal fxRateToBase = asExchangeRate(element.getAttribute("fxRateToBase"));

                        amount = Money.of(ibAccountCurrency, BigDecimal.valueOf(amount.getAmount())
                                        .multiply(fxRateToBase).setScale(0, RoundingMode.HALF_UP).longValue());
                    }
                }
            }
            setAmount(element, transaction, amount);

            // Set note
            if (!AccountTransaction.Type.DIVIDENDS.equals(transaction.getType()))
                transaction.setNote(element.getAttribute("description"));

            // Add Trade-ID note if available
            if (transaction.getNote() == null)
            {
                if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
                    transaction.setNote("Trade-ID: " + element.getAttribute("tradeID"));
            }
            else
            {
                if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
                    transaction.setNote("Trade-ID: " + element.getAttribute("tradeID") + " | " + transaction.getNote());
            }

            // Add Transaction-ID note if available
            if (transaction.getNote() == null)
            {
                if (!element.getAttribute("transactionID").isEmpty()
                                && !"N/A".equals(element.getAttribute("transactionID")))
                    transaction.setNote("Transaction-ID: " + element.getAttribute("transactionID"));
            }
            else
            {
                if (!element.getAttribute("transactionID").isEmpty()
                                && !"N/A".equals(element.getAttribute("transactionID")))
                    transaction.setNote("Transaction-ID: " + element.getAttribute("transactionID") + " | "
                                    + transaction.getNote());
            }

            // Transactions which do not have an account-id will not be
            // imported.
            if (!"-".equals(element.getAttribute("accountId")))
                return new TransactionItem(transaction);
            else
                return null;
        };

        /**
         * Construct a BuySellEntry based on Trade object defined in element
         */
        private Function<Element, Item> buildPortfolioTransaction = element -> {
            if (!Arrays.asList(ASSETKEY_STOCK, //
                            ASSETKEY_FUND, //
                            ASSETKEY_OPTION, //
                            ASSETKEY_CERTIFICATE, //
                            ASSETKEY_FUTURE_OPTION, //
                            ASSETKEY_WARRANTS).contains(element.getAttribute("assetCategory")))
                return null;

            if (element.getAttribute("levelOfDetail").contains("ASSET_SUMMARY")
                            || element.getAttribute("levelOfDetail").contains("SYMBOL_SUMMARY")
                            || element.getAttribute("levelOfDetail").contains("ORDER"))
                return null;

            BuySellEntry transaction = new BuySellEntry();

            // Set transaction type
            switch (element.getAttribute("buySell"))
            {
                case "BUY":
                    transaction.setType(PortfolioTransaction.Type.BUY);
                    break;
                case "BUY (Ca.)":
                    transaction.setNote(Messages.MsgErrorOrderCancellationUnsupported);
                    transaction.setType(PortfolioTransaction.Type.SELL);
                    break;
                case "SELL":
                    transaction.setType(PortfolioTransaction.Type.SELL);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            // @formatter:off
            // If possible, set "tradeDate" with "tradeTime" as the correct
            // trading date of the transaction.
            //
            // If "tradeTime" is not present, then check if "tradeDate" and "dateTime" are the same date, 
            // then set "dateTime" as the trading day.
            // @formatter:on
            if (element.hasAttribute("tradeDate"))
            {
                if (element.hasAttribute("tradeTime"))
                {
                    transaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate"),
                                    element.getAttribute("tradeTime")));
                }
                else if (element.hasAttribute("dateTime"))
                {
                    if (element.getAttribute("tradeDate").equals(element.getAttribute("dateTime").substring(0, 8)))
                    {
                        transaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate"),
                                        element.getAttribute("dateTime").substring(9, 15)));
                    }
                    else
                    {
                        transaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate")));
                    }
                }
                else
                {
                    transaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate")));
                }
            }

            // @formatter:off
            // Set amount and check if the element contains the "netCash"
            // attribute. If the element contains only the "cost" attribute, the
            // amount will be set based on this attribute.
            // @formatter:on
            if (element.hasAttribute("netCash"))
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                                asAmount(element.getAttribute("netCash")));

                setAmount(element, transaction.getPortfolioTransaction(), amount);
                setAmount(element, transaction.getAccountTransaction(), amount, false);
            }
            else
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                                asAmount(element.getAttribute("cost")));

                setAmount(element, transaction.getPortfolioTransaction(), amount);
                setAmount(element, transaction.getAccountTransaction(), amount, false);
            }

            // Set share quantity
            Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
            Double multiplier = Double.parseDouble(Optional.ofNullable(element.getAttribute("multiplier")).orElse("1"));
            transaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor() * multiplier.doubleValue()));

            // Set fees
            Money fees = Money.of(asCurrencyCode(element.getAttribute("ibCommissionCurrency")),
                            asAmount(element.getAttribute("ibCommission")));
            Unit feeUnit = createUnit(element, Unit.Type.FEE, fees);
            transaction.getPortfolioTransaction().addUnit(feeUnit);

            // Set taxes
            Money taxes = Money.of(asCurrencyCode(element.getAttribute("currency")),
                            asAmount(element.getAttribute("taxes")));
            Unit taxUnit = createUnit(element, Unit.Type.TAX, taxes);
            transaction.getPortfolioTransaction().addUnit(taxUnit);

            transaction.setSecurity(this.getOrCreateSecurity(element, true));

            // Set note
            if (transaction.getNote() == null
                            || !transaction.getNote().equals(Messages.MsgErrorOrderCancellationUnsupported))
            {
                // Add Trade-ID note if available
                if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
                    transaction.setNote("Trade-ID: " + element.getAttribute("tradeID"));

                // Add Transaction-ID note if available
                if (!element.getAttribute("transactionID").isEmpty()
                                && !"N/A".equals(element.getAttribute("transactionID")))
                {
                    if (transaction.getNote() != null)
                        transaction.setNote(transaction.getNote() + " | Transaction-ID: " //
                                        + element.getAttribute("transactionID"));
                    else
                        transaction.setNote("Transaction-ID: " + element.getAttribute("transactionID"));
                }
            }

            ExtractorUtils.fixGrossValueBuySell().accept(transaction);

            BuySellEntryItem item = new BuySellEntryItem(transaction);

            if (transaction.getPortfolioTransaction().getCurrencyCode() != null
                            && transaction.getPortfolioTransaction().getAmount() == 0)
            {
                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                return item;
            }
            else if (Messages.MsgErrorOrderCancellationUnsupported
                            .equals(transaction.getPortfolioTransaction().getNote()))
            {
                item.setFailureMessage(Messages.MsgErrorOrderCancellationUnsupported);
                return item;
            }

            return item;
        };

        /**
         * Constructs a Transaction object for a Corporate Transaction defined
         * in element.
         */
        private Function<Element, Item> buildCorporateTransaction = element -> {
            Money proceeds = Money.of(asCurrencyCode(element.getAttribute("currency")),
                            Values.Amount.factorize(Double.parseDouble(element.getAttribute("proceeds"))));

            if (!proceeds.isZero())
            {
                BuySellEntry transaction = new BuySellEntry();

                if (Double.parseDouble(element.getAttribute("quantity")) >= 0)
                    transaction.setType(PortfolioTransaction.Type.BUY);
                else
                    transaction.setType(PortfolioTransaction.Type.SELL);

                transaction.setDate(ExtractorUtils.asDate(element.getAttribute("reportDate")));

                // Set share quantity
                double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
                transaction.setShares(Values.Share.factorize(qty));

                transaction.setSecurity(this.getOrCreateSecurity(element, true));

                transaction.setMonetaryAmount(proceeds);

                return new BuySellEntryItem(transaction);

            }
            else
            {
                // Set transaction type
                PortfolioTransaction transaction = new PortfolioTransaction();
                if (Double.parseDouble(element.getAttribute("quantity")) >= 0)
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                else
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                transaction.setDateTime(ExtractorUtils.asDate(element.getAttribute("reportDate")));

                // Set share quantity
                Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
                transaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor()));

                transaction.setSecurity(this.getOrCreateSecurity(element, true));
                transaction.setNote(element.getAttribute("description"));

                transaction.setMonetaryAmount(proceeds);

                return new TransactionItem(transaction);
            }
        };

        /**
         * Constructs a Transaction object for a SalesTax Transaction defined in
         * element.
         */
        private Function<Element, Item> buildSalesTaxTransaction = element -> {
            AccountTransaction transaction = new AccountTransaction();

            // Set transaction type
            transaction.setType(AccountTransaction.Type.TAXES);

            // Set date
            transaction.setDateTime(ExtractorUtils.asDate(element.getAttribute("date")));

            // Set amount
            Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                            asAmount(element.getAttribute("salesTax")));
            setAmount(element, transaction, amount);

            // Set note
            transaction.setNote(element.getAttribute("taxableDescription"));

            // Add Tax-Transaction-ID note if available
            if (!element.getAttribute("taxableTransactionID").isEmpty()
                            && !"N/A".equals(element.getAttribute("taxableTransactionID")))
                transaction.setNote("Tax-Transaction-ID: " + element.getAttribute("taxableTransactionID") + " | "
                                + transaction.getNote());

            // Add Transaction-ID note if available
            if (!element.getAttribute("transactionID").isEmpty()
                            && !"N/A".equals(element.getAttribute("transactionID")))
                transaction.setNote("Transaction-ID: " + element.getAttribute("transactionID") + " | "
                                + transaction.getNote());

            // Transactions which do not have an account-id will not be
            // imported.
            if (!"-".equals(element.getAttribute("accountId")))
                return new TransactionItem(transaction);
            else
                return null;
        };

        private Unit createUnit(Element element, Unit.Type unitType, Money amount)
        {
            Unit unit;
            if (ibAccountCurrency == null || ibAccountCurrency.equals(amount.getCurrencyCode()))
            {
                unit = new Unit(unitType, amount);
            }
            else
            {
                BigDecimal fxRateToBase;

                if (element.getAttribute("fxRateToBase") != null && !element.getAttribute("fxRateToBase").isEmpty())
                    fxRateToBase = asExchangeRate(element.getAttribute("fxRateToBase"));
                else
                    fxRateToBase = BigDecimal.ONE;

                BigDecimal inverseRate = BigDecimal.ONE.divide(fxRateToBase, 10, RoundingMode.HALF_DOWN);

                Money fxAmount = Money.of(ibAccountCurrency, BigDecimal.valueOf(amount.getAmount())
                                .divide(inverseRate, Values.MC).setScale(0, RoundingMode.HALF_UP).longValue());

                unit = new Unit(unitType, fxAmount, amount, fxRateToBase);
            }
            return unit;
        }

        private void setAmount(Element element, Transaction transaction, Money amount)
        {
            setAmount(element, transaction, amount, true);
        }

        private void setAmount(Element element, Transaction transaction, Money amount, boolean addUnit)
        {
            if (ibAccountCurrency != null && !ibAccountCurrency.equals(amount.getCurrencyCode()))
            {
                BigDecimal fxRateToBase;

                if (element.getAttribute("fxRateToBase") != null && !element.getAttribute("fxRateToBase").isEmpty())
                    fxRateToBase = asExchangeRate(element.getAttribute("fxRateToBase"));
                else
                    fxRateToBase = BigDecimal.ONE;

                Money fxAmount = Money.of(ibAccountCurrency, BigDecimal.valueOf(amount.getAmount())
                                .multiply(fxRateToBase).setScale(0, RoundingMode.HALF_UP).longValue());

                transaction.setMonetaryAmount(fxAmount);

                if (addUnit)
                {
                    Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, fxAmount, amount, fxRateToBase);
                    transaction.addUnit(grossValue);
                }
            }
            else
            {
                transaction.setMonetaryAmount(amount);

                if (addUnit && transaction.getSecurity() != null
                                && !transaction.getSecurity().getCurrencyCode().equals(amount.getCurrencyCode()))
                {
                    // @formatter:off
                    // If the transaction currency is different from the
                    // security currency (as stored in PP) we need to supply the
                    // gross value in the security currency.
                    //
                    // We assume that the security currency is the same that IB
                    // thinks of as base currency for this transaction (fxRateToBase).
                    // @formatter:on
                    BigDecimal fxRateToBase;

                    if (element.getAttribute("fxRateToBase") != null && !element.getAttribute("fxRateToBase").isEmpty())
                        fxRateToBase = asExchangeRate(element.getAttribute("fxRateToBase"));
                    else
                        fxRateToBase = BigDecimal.ONE;

                    BigDecimal inverseRate = BigDecimal.ONE.divide(fxRateToBase, 10, RoundingMode.HALF_DOWN);

                    Money fxAmount = Money.of(transaction.getSecurity().getCurrencyCode(),
                                    BigDecimal.valueOf(amount.getAmount()).divide(inverseRate, Values.MC)
                                                    .setScale(0, RoundingMode.HALF_UP).longValue());

                    transaction.setMonetaryAmount(amount);

                    Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                    transaction.addUnit(grossValue);
                }
            }
        }

        /**
         * @formatter:off
         * Imports from Document
         * - AccountInformation
         * - Trades
         * - CashTransaction
         * - CorporateAction
         * - SalesTaxes
         * @formatter:on
         */
        private void importModelObjects(String type, Function<Element, Item> handleNodeFunction)
        {
            NodeList nList = document.getElementsByTagName(type);
            for (int temp = 0; temp < nList.getLength(); temp++)
            {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    try
                    {
                        Item item = handleNodeFunction.apply((Element) nNode);
                        if (item != null)
                            results.add(item);
                    }
                    catch (Exception e)
                    {
                        errors.add(e);
                    }
                }
            }
        }

        public void parseDocument(Document doc)
        {
            this.document = doc;

            if (document == null)
                return;

            importModelObjects("AccountInformation", importAccountInformation);

            // Process all Trades
            importModelObjects("Trade", buildPortfolioTransaction);

            // Process all CashTransaction
            importModelObjects("CashTransaction", buildAccountTransaction);

            // Process all CorporateTransactions
            importModelObjects("CorporateAction", buildCorporateTransaction);

            // Process all SalesTaxes
            importModelObjects("SalesTax", buildSalesTaxTransaction);

            // TODO: Process all FxTransactions and ConversionRates
        }

        public void addError(Exception e)
        {
            errors.add(e);
        }

        /**
         * Lookup a Security in the Model or create a new one if it does not yet
         * exist It uses IB ContractID (conID) for the WKN, tries to degrade if
         * conID or ISIN are not available
         */
        private Security getOrCreateSecurity(Element element, boolean doCreate)
        {
            // Lookup the Exchange Suffix for Yahoo
            Optional<String> tickerSymbol = Optional.ofNullable(element.getAttribute("symbol"));
            String quoteFeed = QuoteFeed.MANUAL;

            // yahoo uses '-' instead of ' '
            String currency = asCurrencyCode(element.getAttribute("currency"));
            String isin = element.getAttribute("isin");
            String cusip = element.getAttribute("cusip");
            Optional<String> computedTickerSymbol = tickerSymbol.map(t -> t.replace(' ', '-'));

            // Store cusip in isin if isin is not available
            if (isin.length() == 0 && cusip.length() > 0)
                isin = cusip;

            String conID = element.getAttribute("conid");
            String description = element.getAttribute("description");

            if (Arrays.asList(ASSETKEY_OPTION, ASSETKEY_FUTURE_OPTION).contains(element.getAttribute("assetCategory")))
            {
                computedTickerSymbol = tickerSymbol.map(t -> t.replaceAll("\\s+", ""));
                // e.g a put option for oracle: ORCL 171117C00050000
                if (computedTickerSymbol.filter(p -> p.matches(".*\\d{6}[CP]\\d{8}")).isPresent())
                    quoteFeed = YahooFinanceQuoteFeed.ID;
            }

            if (Arrays.asList(ASSETKEY_STOCK, ASSETKEY_FUND, ASSETKEY_CERTIFICATE).contains(element.getAttribute("assetCategory")))
            {
                computedTickerSymbol = tickerSymbol;
                if (!CurrencyUnit.USD.equals(currency))
                {
                    // @formatter:off
                    // Some symbols in IB included the exchange as lower key
                    // without "." at the end, e.g. BMWd for BMW trading at d
                    // (Xetra, DE), so we'll get rid of this.
                    // @formatter:on
                    computedTickerSymbol = computedTickerSymbol.map(t -> t.replaceAll("[a-z]*$", ""));

                    // @formatter:off
                    // Another curiosity, sometimes the ticker symbol has EUR
                    // appended, e.g. Deutsche Bank is DBKEUR, BMW is BMWEUR.
                    // Also notices this during cash transactions (dividend payments).
                    // @formatter:on
                    if (CurrencyUnit.EUR.equals(currency))
                        computedTickerSymbol = computedTickerSymbol.map(t -> t.replaceAll("EUR$", ""));

                    // @formatter:off
                    // At last, lets add the exchange to the ticker symbol.
                    // Since all european stock have ISIN set, this should not
                    // produce duplicate security (later on).
                    // @formatter:on
                    if (tickerSymbol.isPresent() && exchanges.containsKey(element.getAttribute("exchange")))
                        computedTickerSymbol = computedTickerSymbol.map(t -> t + '.' + exchanges.get(element.getAttribute("exchange")));

                }

                // for Stock and Fund, use Yahoo as default (AlphaVantage has no
                // meaningful free tier)
                quoteFeed = YahooFinanceQuoteFeed.ID;
            }

            Security s2 = null;

            for (Security s : allSecurities)
            {
                // Find security with same conID or isin & currency or
                // yahooSymbol
                if (conID != null && conID.length() > 0 && conID.equals(s.getWkn()))
                    return s;
                if (isin.length() > 0 && isin.equals(s.getIsin()))
                    if (currency.equals(s.getCurrencyCode()))
                        return s;
                    else
                        s2 = s;
                if (computedTickerSymbol.isPresent() && computedTickerSymbol.get().equals(s.getTickerSymbol()))
                    return s;
            }

            if (s2 != null)
                return s2;

            if (!doCreate)
                return null;

            Security security = new Security(description, isin, computedTickerSymbol.orElse(null), quoteFeed);

            // We use the Wkn to store the IB conID as a unique identifier
            security.setWkn(conID);
            security.setCurrencyCode(currency);
            security.setNote(description);

            // Store
            allSecurities.add(security);

            // add to result
            SecurityItem item = new SecurityItem(security);
            results.add(item);

            return security;
        }

        /**
         * Figure out how many shares a dividend payment is related to. Extracts
         * the information from the description string given by IB
         */
        private void calculateShares(Transaction transaction, Element element)
        {
            // Figure out how many shares were holding related to this Dividend
            // Payment
            long numShares = 0;
            double amount = Double.parseDouble(element.getAttribute("amount"));

            // Regex Pattern matches the Dividend per Share and calculate number
            // of shares
            Pattern pDividendPerShares = Pattern.compile(
                            ".*DIVIDEND( [\\w]{3})? (?<dividendPerShares>[\\.,\\d]+)( [\\w]{3})? PER SHARE .*");
            Matcher mDividendPerShares = pDividendPerShares.matcher(element.getAttribute("description"));
            if (mDividendPerShares.find())
            {
                double dividendPerShares = Double.parseDouble(mDividendPerShares.group("dividendPerShares"));
                numShares = Math.round(amount / dividendPerShares) * Values.Share.factor();
            }

            transaction.setShares(numShares);
        }

        public List<Exception> getErrors()
        {
            return errors;
        }

        public List<Item> getResults()
        {
            return results;
        }
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, Extractor.InputFile inputFile, List<Exception> errors)
    {
        try (FileInputStream in = new FileInputStream(inputFile.getFile()))
        {
            IBFlexStatementExtractorResult result = importActivityStatement(in);
            errors.addAll(result.getErrors());
            return result.getResults();
        }
        catch (IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Item> postProcessing(List<Item> items)
    {
        // Group dividend and taxes transactions together and group by date and
        // security
        Map<LocalDateTime, Map<Security, List<Item>>> dividendTaxTransactions = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .map(TransactionItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals(((AccountTransaction) i.getSubject()).getType()) || //
                                        AccountTransaction.Type.TAXES //
                                                        .equals(((AccountTransaction) i.getSubject()).getType())) //
                        .filter(i -> i.getSecurity() != null)
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        dividendTaxTransactions.forEach((k, v) -> {
            v.forEach((security, transactions) -> {
                AccountTransaction dividendTransaction = (AccountTransaction) transactions.get(0).getSubject();

                // @formatter:off
                // It is possible that several dividend transactions exist on
                // the same day without one or with several taxes transactions.
                //
                // We simplify here only one dividend transaction with one
                // related taxes transaction.
                // @formatter:on

                if (transactions.size() == 2)
                {
                    AccountTransaction taxTransaction = (AccountTransaction) transactions.get(1).getSubject();

                    // Which transaction is the taxes and which the dividend?
                    if (!AccountTransaction.Type.TAXES.equals(taxTransaction.getType()))
                    {
                        dividendTransaction = (AccountTransaction) transactions.get(1).getSubject();
                        taxTransaction = (AccountTransaction) transactions.get(0).getSubject();
                    }

                    // Check if there is a dividend transaction and a tax
                    // transaction.
                    if (AccountTransaction.Type.TAXES.equals(taxTransaction.getType())
                                    && AccountTransaction.Type.DIVIDENDS.equals(dividendTransaction.getType()))
                    {
                        // Subtract the taxes from the tax transaction from the
                        // total amount
                        dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount()
                                        .subtract(taxTransaction.getMonetaryAmount()));

                        // Add taxes as tax unit
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxTransaction.getMonetaryAmount()));

                        // Set note that the tax transaction will be deleted
                        taxTransaction.setNote(TO_BE_DELETED);
                    }
                }
                else
                {
                    // do nothing
                }
            });
        });

        // iterate list and remove items that are marked TO_BE_DELETED
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext())
        {
            Object o = iter.next().getSubject();
            if (o instanceof AccountTransaction a && TO_BE_DELETED.equals(a.getNote()))
                iter.remove();
        }

        return items;
    }

    protected String asCurrencyCode(String currency)
    {
        if (currency == null)
            return client.getBaseCurrency();

        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
    }

    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
