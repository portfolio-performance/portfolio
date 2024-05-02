package name.abuchen.portfolio.datatransfer.ibflex;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote https://ibkrguides.com/reportingreference/reportguide/tradesfq.htm
 *
 * @implSpec The IBFlexStatementExtractor class implements the Extractor interface
 *           for importing Interactive Broker Activity Statements from XML files.
 *
 *           Therefore, we use the transaction based on their function and merge both, if possible, as one transaction.
 *           {@code
 *              matchTransactionPair(List<Item> transactionList,List<Item> taxesTreatmentList)
 *           }
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class IBFlexStatementExtractor implements Extractor
{

    // The client associated with this extractor
    private final Client client;

    // List to store all securities associated with the client
    private List<Security> allSecurities = new ArrayList<>();

    // Map to store exchange mappings from Interactive Broker to Yahoo
    private Map<String, String> exchanges = new HashMap<>();

    /**
     * Constructs an IBFlexStatementExtractor with the given client.
     * Initializes the list of securities and exchange mappings.
     *
     * @param client The client for which the extractor is created.
     */
    public IBFlexStatementExtractor(Client client)
    {
        this.client = new Client();
        allSecurities.addAll(client.getSecurities());

        initializeExchangeMappings();
    }

    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public String getLabel()
    {
        return Messages.IBXML_Label;
    }

    /**
     * Initializes the exchange mappings from Interactive Broker to Yahoo.
     * This method can be extended as needed for additional mappings.
     */
    private void initializeExchangeMappings()
    {
        exchanges.put("EBS", "SW");
        exchanges.put("LSE", "L"); // London Stock Exchange
        exchanges.put("SWX", "SW"); // Swiss Exchange (SWX)
        exchanges.put("TSE", "TO"); // TSX Venture
        exchanges.put("IBIS", "DE"); // XETRA
        exchanges.put("TGATE", "DE"); // TradeGate
        exchanges.put("SWB", "SG"); // Stuttgart Stock Exchange
        exchanges.put("FWB", "F"); // Frankfurt Stock Exchange
    }

    /**
     * Imports an Interactive Broker Activity Statement from an XML file.
     *
     * @param f The input stream of the XML file.
     * @return The result of the extraction process.
     */
    /* package */ IBFlexStatementExtractorResult importActivityStatement(InputStream f)
    {
        IBFlexStatementExtractorResult result = new IBFlexStatementExtractorResult();

        try (InputStream fileInputStream = f)
        {
            // Setup XML document parsing with secure features
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileInputStream);

            doc.getDocumentElement().normalize();

            // Parse the document and populate the result
            result.parseDocument(doc);
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            // Handle exceptions and add errors to the result
            result.addError(e);
        }

        return result;
    }

    /**
     * @formatter:off
     * The IBFlexStatementExtractorResult class represents the result of importing
     * an Interactive Broker Activity Statement from an XML file.
     *
     * Information on the different asset categories
     * --------------------------------------------
     * STK      --> Stock
     * FUND     --> Fonds
     * IND      --> Indices
     * OPT      --> Options
     * IOPT     --> Certificate
     * FUT      --> Future
     * FOP      --> Future Options
     * WAR      --> Warrants
     * CMDTY    --> Metal
     * @formatter:on
     */
    private class IBFlexStatementExtractorResult
    {
        // Asset category keys
        private static final String ASSETKEY_STOCK = "STK";
        private static final String ASSETKEY_FUND = "FUND";
        private static final String ASSETKEY_OPTION = "OPT";
        private static final String ASSETKEY_CERTIFICATE = "IOPT";
        private static final String ASSETKEY_FUTURE_OPTION = "FOP";
        private static final String ASSETKEY_WARRANTS = "WAR";

        private Document document;
        private List<Exception> errors = new ArrayList<>();
        private List<Item> results = new ArrayList<>();
        private String accountCurrency = null;

        /**
         * Builds account information based on the provided XML element. Extracts the currency
         * attribute from the element, converts it to a currency code, and sets the corresponding
         * currency unit for the IB account if valid. The resulting information is not returned,
         * as the primary purpose is to update the 'accountCurrency' field.
         *
         * @param element The XML element containing account information.
         * @return Always returns null, as the focus is on updating the currency information.
         */
        private Function<Element, Item> buildAccountInformation = element -> {
            String currency = asCurrencyCode(element.getAttribute("currency"));
            if (currency != null && !currency.isEmpty())
            {
                CurrencyUnit currencyUnit = CurrencyUnit.getInstance(currency);
                if (currencyUnit != null)
                    accountCurrency = currency;
            }
            return null;
        };

        /**
         * Constructs an AccountTransactionItem based on the information provided in the XML element.
         */
        private Function<Element, Item> buildAccountTransaction = element -> {
            AccountTransaction accountTransaction = new AccountTransaction();

            // @formatter:off
            // New Format dateTime has now also Time [YYYYMMDD;HHMMSS], I cut
            // Date from string [YYYYMMDD]
            //
            // Checks for old format [YYYY-MM-DD, HH:MM:SS], too. Quapla 11.1.20
            // Changed from dateTime to reportDate + Check for old Data-Formats,
            // Quapla 14.2.20
            // @formatter:on
            String dateTime = !element.getAttribute("reportDate").isEmpty() ? element.getAttribute("reportDate")
                            : element.getAttribute("dateTime");

            dateTime = dateTime.length() == 15 ? dateTime.substring(0, 8) : dateTime;
            accountTransaction.setDateTime(ExtractorUtils.asDate(dateTime));

            // Set amount
            Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                            asAmount(element.getAttribute("amount")));

            // Set transaction type based on the attribute "type"
            String type = element.getAttribute("type");
            switch (type)
            {
                case "Deposits":
                case "Deposits & Withdrawals":
                case "Deposits/Withdrawals":
                    // Positive amount are a deposit
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                    else
                        accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                    break;
                case "Dividends":
                case "Payment In Lieu Of Dividends":
                    // Set the Symbol
                    if (element.getAttribute("symbol").length() > 0)
                        accountTransaction.setSecurity(this.getOrCreateSecurity(element, true));

                    accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                    this.calculateShares(accountTransaction, element);
                    break;
                case "Withholding Tax":
                    // Set the Symbol
                    if (element.getAttribute("symbol").length() > 0)
                        accountTransaction.setSecurity(this.getOrCreateSecurity(element, true));

                    // Positive amount are a tax refund
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        accountTransaction.setType(AccountTransaction.Type.TAXES);
                    else
                        accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                    break;
                case "Broker Interest Received":
                    accountTransaction.setType(AccountTransaction.Type.INTEREST);
                    break;
                case "Broker Interest Paid":
                    accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    break;
                case "Other Fees":
                    // Positive amount are a fee refund
                    if (Math.signum(Double.parseDouble(element.getAttribute("amount"))) == -1)
                        accountTransaction.setType(AccountTransaction.Type.FEES);
                    else
                        accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported type '" + type + "'");
            }

            // @formatter:off
            // If the account currency differs from transaction currency
            // convert currency, if there is a matching security with the account currency
            // @formatter:on
            if ((AccountTransaction.Type.DIVIDENDS.equals(accountTransaction.getType())
                            || AccountTransaction.Type.TAXES.equals(accountTransaction.getType()))
                            && (this.accountCurrency != null && !this.accountCurrency.equals(amount.getCurrencyCode())))
            {
                // matching isin or wkn & base currency
                boolean foundIsinBase = false;

                // matching isin & transaction currency
                boolean foundIsinTransaction = false;

                // matching conid (wkn) & transaction currency
                boolean foundWknTransaction = false;

                for (Security s : allSecurities)
                {
                    // Find security with same isin or conid (wkn) and currency
                    if (element.getAttribute("isin").length() > 0 && element.getAttribute("isin").equals(s.getIsin()))
                    {
                        if (amount.getCurrencyCode().equals(s.getCurrencyCode()))
                            foundIsinTransaction = true;
                        else if (this.accountCurrency.equals(s.getCurrencyCode()))
                            foundIsinBase = true;
                    }
                    else if (element.getAttribute("conid").length() > 0 && element.getAttribute("conid").equals(s.getWkn()))
                    {
                        if (amount.getCurrencyCode().equals(s.getCurrencyCode()))
                            foundWknTransaction = true;
                        else if (this.accountCurrency.equals(s.getCurrencyCode()))
                            foundIsinBase = true;
                    }
                }

                // If the transaction currency is not found but the base
                // currency is found, and there is an exchange rate, convert the
                // amount.
                if ((!foundIsinTransaction || !foundWknTransaction) && foundIsinBase && element.getAttribute("fxRateToBase").length() > 0)
                {
                    BigDecimal fxRateToBase = asExchangeRate(element.getAttribute("fxRateToBase"));

                    amount = Money.of(accountCurrency, BigDecimal.valueOf(amount.getAmount()).multiply(fxRateToBase)
                                    .setScale(0, RoundingMode.HALF_UP).longValue());
                }
            }

            // Set the amount in the transaction
            setAmount(element, accountTransaction, amount);

            // Set note
            if (!AccountTransaction.Type.DIVIDENDS.equals(accountTransaction.getType()))
                accountTransaction.setNote(element.getAttribute("description"));

            // Add Trade-ID note if available
            if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
            {
                accountTransaction.setNote(concatenate("Trade-ID: " + element.getAttribute("tradeID"), accountTransaction.getNote(), " | "));
            }

            // Add Transaction-ID note if available
            if (!element.getAttribute("transactionID").isEmpty() && !"N/A".equals(element.getAttribute("transactionID")))
            {
                accountTransaction.setNote(concatenate("Transaction-ID: " + element.getAttribute("transactionID"), accountTransaction.getNote(), " | "));
            }

            // Transactions without an account-id will not be imported.
            if (!"-".equals(element.getAttribute("accountId")))
                return new TransactionItem(accountTransaction);
            else
                return null;
        };

        /**
         * Constructs a BuySellEntryItem based on the information provided in the XML element.
         */
        private Function<Element, Item> buildPortfolioTransaction = element -> {
            BuySellEntry portfolioTransaction = new BuySellEntry();

            // Check if the asset category is supported
            if (!Arrays.asList(ASSETKEY_STOCK, //
                            ASSETKEY_FUND, //
                            ASSETKEY_OPTION, //
                            ASSETKEY_CERTIFICATE, //
                            ASSETKEY_FUTURE_OPTION, //
                            ASSETKEY_WARRANTS).contains(element.getAttribute("assetCategory")))
                return null;

            // Check if the level of detail is supported
            String lod = element.getAttribute("levelOfDetail");
            if (lod.contains("ASSET_SUMMARY")
                            || lod.contains("SYMBOL_SUMMARY")
                            || lod.contains("ORDER"))
                return null;

            // Set transaction type
            String tType = element.getAttribute("buySell");
            switch (tType)
            {
                case "BUY":
                    portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                    break;
                case "BUY (Ca.)":
                    portfolioTransaction.setNote(Messages.MsgErrorOrderCancellationUnsupported);
                    portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                    break;
                case "SELL":
                    portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                    break;
                case "SELL (Ca.)":
                    portfolioTransaction.setNote(Messages.MsgErrorOrderCancellationUnsupported);
                    portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                    break;
                default:
                    throw new IllegalArgumentException("unsupported transaction type '" + tType + "'");
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
                    portfolioTransaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate"), element.getAttribute("tradeTime")));
                }
                else if (element.hasAttribute("dateTime"))
                {
                    if (element.getAttribute("tradeDate").equals(element.getAttribute("dateTime").substring(0, 8)))
                    {
                        portfolioTransaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate"), element.getAttribute("dateTime").substring(9, 15)));
                    }
                    else
                    {
                        portfolioTransaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate")));
                    }
                }
                else
                {
                    portfolioTransaction.setDate(ExtractorUtils.asDate(element.getAttribute("tradeDate")));
                }
            }

            // @formatter:off
            // Set amount and check if the element contains the "netCash"
            // attribute. If the element contains only the "cost" attribute, the
            // amount will be set based on this attribute.
            // @formatter:on
            if (element.hasAttribute("netCash"))
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")), asAmount(element.getAttribute("netCash")));

                setAmount(element, portfolioTransaction.getPortfolioTransaction(), amount);
                setAmount(element, portfolioTransaction.getAccountTransaction(), amount, false);
            }
            else
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")), asAmount(element.getAttribute("cost")));

                setAmount(element, portfolioTransaction.getPortfolioTransaction(), amount);
                setAmount(element, portfolioTransaction.getAccountTransaction(), amount, false);
            }

            // Set share quantity
            Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
            Double multiplier = Double.parseDouble(Optional.ofNullable(element.getAttribute("multiplier")).orElse("1"));
            portfolioTransaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor() * multiplier.doubleValue()));

            // Set fees
            Money fees = Money.of(asCurrencyCode(element.getAttribute("ibCommissionCurrency")), asAmount(element.getAttribute("ibCommission")));
            Unit feeUnit = createUnit(element, Unit.Type.FEE, fees);
            portfolioTransaction.getPortfolioTransaction().addUnit(feeUnit);

            // Set taxes
            Money taxes = Money.of(asCurrencyCode(element.getAttribute("currency")), asAmount(element.getAttribute("taxes")));
            Unit taxUnit = createUnit(element, Unit.Type.TAX, taxes);
            portfolioTransaction.getPortfolioTransaction().addUnit(taxUnit);

            portfolioTransaction.setSecurity(this.getOrCreateSecurity(element, true));

            // Set note
            if (portfolioTransaction.getNote() == null || !portfolioTransaction.getNote().equals(Messages.MsgErrorOrderCancellationUnsupported))
            {
                // Add Trade-ID note if available
                if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
                {
                    portfolioTransaction.setNote(concatenate("Trade-ID: ", element.getAttribute("tradeID"), ""));
                }

                // Add Transaction-ID note if available
                if (!element.getAttribute("transactionID").isEmpty() && !"N/A".equals(element.getAttribute("transactionID")))
                {
                    portfolioTransaction.setNote(concatenate(portfolioTransaction.getNote(), "Transaction-ID: " + element.getAttribute("transactionID"), " | "));
                }
            }

            ExtractorUtils.fixGrossValueBuySell().accept(portfolioTransaction);

            BuySellEntryItem item = new BuySellEntryItem(portfolioTransaction);

            if (portfolioTransaction.getPortfolioTransaction().getCurrencyCode() != null && portfolioTransaction.getPortfolioTransaction().getAmount() == 0)
            {
                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                return item;
            }
            else if (Messages.MsgErrorOrderCancellationUnsupported.equals(portfolioTransaction.getPortfolioTransaction().getNote()))
            {
                item.setFailureMessage(Messages.MsgErrorOrderCancellationUnsupported);
                return item;
            }

            return item;
        };

        /**
         * Constructs a Corporate Transaction Item based on the information provided in the XML element.
         */
        private Function<Element, Item> buildCorporateTransaction = element -> {
            Money proceeds = Money.of(asCurrencyCode(element.getAttribute("currency")), Values.Amount.factorize(Double.parseDouble(element.getAttribute("proceeds"))));

            if (!proceeds.isZero())
            {
                // Set transaction type
                BuySellEntry portfolioTransaction = new BuySellEntry();

                if (Double.parseDouble(element.getAttribute("quantity")) >= 0)
                    portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                else
                    portfolioTransaction.setType(PortfolioTransaction.Type.SELL);

                portfolioTransaction.setDate(ExtractorUtils.asDate(element.getAttribute("reportDate")));

                // Set share quantity
                double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
                portfolioTransaction.setShares(Values.Share.factorize(qty));

                portfolioTransaction.setSecurity(this.getOrCreateSecurity(element, true));

                portfolioTransaction.setMonetaryAmount(proceeds);

                return new BuySellEntryItem(portfolioTransaction);

            }
            else
            {
                // Set transaction type
                PortfolioTransaction portfolioTransaction = new PortfolioTransaction();

                if (Double.parseDouble(element.getAttribute("quantity")) >= 0)
                    portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                else
                    portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                portfolioTransaction.setDateTime(ExtractorUtils.asDate(element.getAttribute("reportDate")));

                // Set share quantity
                Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
                portfolioTransaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor()));

                portfolioTransaction.setSecurity(this.getOrCreateSecurity(element, true));
                portfolioTransaction.setNote(element.getAttribute("description"));

                portfolioTransaction.setMonetaryAmount(proceeds);

                return new TransactionItem(portfolioTransaction);
            }
        };

        /**
         * Constructs a Sales Tax Transaction Item based on the information provided in the XML element.
         */
        private Function<Element, Item> buildSalesTaxTransaction = element -> {
            AccountTransaction accountTransaction = new AccountTransaction();

            // Set transaction type
            accountTransaction.setType(AccountTransaction.Type.TAXES);

            // Set date
            accountTransaction.setDateTime(ExtractorUtils.asDate(element.getAttribute("date")));

            // Set amount
            Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")), asAmount(element.getAttribute("salesTax")));
            setAmount(element, accountTransaction, amount);

            // Set note
            accountTransaction.setNote(element.getAttribute("taxableDescription"));

            // Add Tax-Transaction-ID note if available
            if (!element.getAttribute("taxableTransactionID").isEmpty() && !"N/A".equals(element.getAttribute("taxableTransactionID")))
            {
                accountTransaction.setNote("Tax-Transaction-ID: " + element.getAttribute("taxableTransactionID") + " | " + accountTransaction.getNote());
            }

            // Add Transaction-ID note if available
            if (!element.getAttribute("transactionID").isEmpty() && !"N/A".equals(element.getAttribute("transactionID")))
            {
                accountTransaction.setNote("Transaction-ID: " + element.getAttribute("transactionID") + " | " + accountTransaction.getNote());
            }

            // Transactions without an account-id will not be imported.
            if (!"-".equals(element.getAttribute("accountId")))
                return new TransactionItem(accountTransaction);
            else
                return null;
        };

        /**
         * Creates a Unit based on the specified element, unit type, and monetary amount.
         *
         * @param element The element containing FX rate information.
         * @param unitType The type of the Unit to be created.
         * @param amount The original monetary amount.
         * @return The created Unit.
         */
        private Unit createUnit(Element element, Unit.Type unitType, Money amount)
        {
            Unit unit;

            // Check if the IB account currency is null or matches the amount
            // currency
            if (accountCurrency == null || accountCurrency.equals(amount.getCurrencyCode()))
            {
                unit = new Unit(unitType, amount);
            }
            else
            {
                // Calculate the FX rate to the base currency
                BigDecimal fxRateToBase = element.getAttribute("fxRateToBase").isEmpty() ? BigDecimal.ONE
                                : asExchangeRate(element.getAttribute("fxRateToBase"));

                // Calculate the inverse rate
                BigDecimal inverseRate = BigDecimal.ONE.divide(fxRateToBase, 10, RoundingMode.HALF_DOWN);

                // Convert the amount to the IB account currency using the
                // inverse rate
                Money fxAmount = Money.of(accountCurrency, BigDecimal.valueOf(amount.getAmount())
                                .divide(inverseRate, Values.MC).setScale(0, RoundingMode.HALF_UP).longValue());

                // Create a Unit with FX amount, original amount, and FX rate
                unit = new Unit(unitType, fxAmount, amount, fxRateToBase);
            }

            return unit;
        }

        /**
         * Sets the specified monetary amount on the given transaction, with an option to include in the portfolio transaction.
         *
         * @param element The XML element containing transaction details.
         * @param transaction The transaction object to update with the amount.
         * @param amount The monetary amount to set on the transaction.
         */
        private void setAmount(Element element, Transaction transaction, Money amount)
        {
            setAmount(element, transaction, amount, true);
        }

        /**
         * Sets the specified monetary amount on the given transaction, with an option to include in the portfolio transaction.
         *
         * @param element      The XML element containing transaction details.
         * @param transaction  The transaction object to update with the amount.
         * @param amount       The monetary amount to set on the transaction.
         * @param addUnit      A flag indicating whether to add a Unit to the transaction.
         */
        private void setAmount(Element element, Transaction transaction, Money amount, boolean addUnit)
        {
            if (accountCurrency != null && !accountCurrency.equals(amount.getCurrencyCode()))
            {
                BigDecimal fxRateToBase = element.getAttribute("fxRateToBase").isEmpty() ? BigDecimal.ONE
                                : asExchangeRate(element.getAttribute("fxRateToBase"));

                Money fxAmount = Money.of(accountCurrency, BigDecimal.valueOf(amount.getAmount())
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

                if (addUnit && transaction.getSecurity() != null && !transaction.getSecurity().getCurrencyCode().equals(amount.getCurrencyCode()))
                {
                    // @formatter:off
                    // If the transaction currency is different from the security currency (as stored in PP)
                    // we need to supply the gross value in the security currency.
                    //
                    // We assume that the security currency is the same that IB
                    // thinks of as base currency for this transaction (fxRateToBase).
                    // @formatter:on
                    BigDecimal fxRateToBase = element.getAttribute("fxRateToBase").isEmpty() ? BigDecimal.ONE
                                    : asExchangeRate(element.getAttribute("fxRateToBase"));

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
         * Imports model objects from the document based on the specified type using the provided handling function.
         *
         * Supported types:
         * - AccountInformation
         * - Trades
         * - CashTransaction
         * - CorporateAction
         * - SalesTaxes
         *
         * @param type                 The type of model objects to import.
         * @param handleNodeFunction  The function to handle each XML element node and convert it to an Item.
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
                        {
                            results.add(item);
                        }
                    }
                    catch (Exception e)
                    {
                        errors.add(e);
                    }
                }
            }
        }

        /**
         * Parses the given XML document and processes various model objects.
         *
         * @param doc The XML document to parse.
         */
        public void parseDocument(Document doc)
        {
            this.document = doc;

            if (document == null)
                return;

            // Import AccountInformation
            importModelObjects("AccountInformation", buildAccountInformation);

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
         * Looks up a Security in the model or creates a new one if it does not yet exist.
         * It uses the IB ContractID (conID) for the WKN, tries to degrade if conID or ISIN are not available.
         *
         * @param element   The XML element containing information about the security.
         * @param doCreate  A flag indicating whether to create a new Security if not found.
         * @return          The found or created Security object.
         */
        private Security getOrCreateSecurity(Element element, boolean doCreate)
        {
            // Lookup the Exchange Suffix for Yahoo
            Optional<String> tickerSymbol = Optional.ofNullable(element.getAttribute("symbol"));
            String quoteFeed = QuoteFeed.MANUAL;

            // Yahoo uses '-' instead of ' '
            String currency = asCurrencyCode(element.getAttribute("currency"));
            String isin = element.getAttribute("isin");
            String cusip = element.getAttribute("cusip");
            String conid = element.getAttribute("conid");
            Optional<String> computedTickerSymbol = tickerSymbol.map(t -> t.replace(' ', '-'));

            // Store CUSIP in ISIN if ISIN is not available
            if (isin.isEmpty() && !cusip.isEmpty())
                isin = cusip;

            String description = element.getAttribute("description");

            if (Arrays.asList(ASSETKEY_OPTION, ASSETKEY_FUTURE_OPTION).contains(element.getAttribute("assetCategory")))
            {
                computedTickerSymbol = tickerSymbol.map(t -> t.replaceAll("\\s+", ""));
                // e.g a put option for oracle: ORCL 171117C00050000
                if (computedTickerSymbol.filter(p -> p.matches(".*\\d{6}[CP]\\d{8}")).isPresent())
                    quoteFeed = YahooFinanceQuoteFeed.ID;
            }

            if (Arrays.asList(ASSETKEY_STOCK, ASSETKEY_FUND, ASSETKEY_CERTIFICATE)
                            .contains(element.getAttribute("assetCategory")))
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

                // @formatter:off
                // For Stock and Fund, use Yahoo as default (AlphaVantage has no meaningful free tier)
                // @formatter:on
                quoteFeed = YahooFinanceQuoteFeed.ID;
            }

            Security matchingSecurity = null;

            for (Security security : allSecurities)
            {
                // Find security with same CONID or ISIN & currency or yahooSymbol
                if (conid != null && conid.length() > 0 && conid.equals(security.getWkn()))
                    return security;

                if (!isin.isEmpty() && isin.equals(security.getIsin()))
                    if (currency.equals(security.getCurrencyCode()))
                        return security;
                    else
                        matchingSecurity = security;

                if (computedTickerSymbol.isPresent() && computedTickerSymbol.get().equals(security.getTickerSymbol()))
                    return security;
            }

            if (matchingSecurity != null)
                return matchingSecurity;

            if (!doCreate)
                return null;

            Security security = new Security(description, isin, computedTickerSymbol.orElse(null), quoteFeed);

            // We use the WKN to store the IB CONID as a unique identifier
            security.setWkn(conid);
            security.setCurrencyCode(currency);
            security.setNote(description);

            // Store
            allSecurities.add(security);

            // Add to result
            SecurityItem item = new SecurityItem(security);
            results.add(item);

            return security;
        }

        /**
         * Calculates the number of shares related to a dividend payment by extracting
         * information from the provided description string by IB (Interactive Brokers).
         */
        private void calculateShares(Transaction transaction, Element element)
        {
            long numShares = 0;
            double amount = Double.parseDouble(element.getAttribute("amount"));

            // Regular expression pattern to match the Dividend per Share and
            // calculate the number of shares
            Pattern pDividendShare = Pattern.compile("^.*DIVIDEND( [\\w]{3})? (?<dividendPerShares>[\\.,\\d]+)( [\\w]{3})? PER SHARE .*$");
            Matcher mDividendShare = pDividendShare.matcher(element.getAttribute("description"));

            if (mDividendShare.find())
            {
                double dividendPerShares = Double.parseDouble(mDividendShare.group("dividendPerShares"));
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

    /**
     * @formatter:off
     * This method performs post-processing on a list of transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions and dividend transactions.
     * 2. Matches dividend transactions with their corresponding taxes treatment.
     * 3. Adjusts dividend transactions by updating the gross amount if necessary, subtracting tax amounts, adding tax units,
     *    combining source information, appending taxes treatment notes, and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in dividend transactions, making the transactions more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        List<Item> taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance)
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> {
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by dividend transactions
        List<Item> dividendTransactionList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .toList();

        var dividendTaxPairs = matchTransactionPair(dividendTransactionList, taxesTreatmentList);

        // @formatter:off
        // This loop iterates through a list of dividend and tax pairs and processes them.
        //
        // For each pair, it subtracts the tax amount from the dividend transaction's total amount,
        // adds the tax as a tax unit to the dividend transaction, combines source information if needed,
        // appends taxes treatment notes to the dividend transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax treatment transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : dividendTaxPairs)
        {
            var dividendTransaction = (AccountTransaction) pair.transaction().getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null)
            {
                Money taxesAmount = taxesTransaction.getMonetaryAmount();

                dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount().subtract(taxesAmount));

                dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxesAmount));

                dividendTransaction.setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

                items.remove(pair.tax());
            }
        }
    }

    /**
     * @formatter:off
     * Matches transactions and taxes treatment's, ensuring unique pairs based on date and security.
     *
     * This method matches transactions and taxes treatment's by creating a Pair consisting of the transaction's
     * date and security. It uses a Set called 'keys' to prevent duplicates based on these Pair keys,
     * ensuring that the same combination of date and security is not processed multiple times.
     * Duplicate transactions for the same security on the same day are avoided.
     *
     * @param transactionList      A list of transactions to be matched.
     * @param taxesTreatmentList   A list of taxes treatment's to be considered for matching.
     * @return A collection of TransactionTaxesPair objects representing matched transactions and taxes treatment's.
     * @formatter:on
     */
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList, List<Item> taxesTreatmentList)
    {
        // Use a Set to prevent duplicates
        Set<Pair<LocalDate, Security>> keys = new HashSet<>();
        Map<Pair<LocalDate, Security>, TransactionTaxesPair> pairs = new HashMap<>();

        // Match identified transactions and taxes treatment's
        transactionList.forEach( //
                        transaction -> {
                            var key = new Pair<>(transaction.getDate().toLocalDate(), transaction.getSecurity());

                            // Prevent duplicates
                            if (keys.add(key))
                                pairs.put(key, new TransactionTaxesPair(transaction, null));
                        } //
        );

        // Iterate through the list of taxes treatment's to match them with transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
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
