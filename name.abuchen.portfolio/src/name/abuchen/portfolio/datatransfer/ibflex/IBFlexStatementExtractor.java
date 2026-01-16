package name.abuchen.portfolio.datatransfer.ibflex;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.money.impl.FixedExchangeRateProvider;
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

    // Supported date formats (from IB Flex Query configuration):
    // - yyyyMMdd, yyyy-MM-dd, MM/dd/yyyy, MM/dd/yy, dd/MM/yyyy, dd/MM/yy,
    // dd-MMM-yy
    //
    // Supported time formats:
    // - HHmmss, HH:mm:ss, HHmmss zzz (with timezone), HH:mm:ss zzz (with
    // timezone)
    //
    // Date and time are separated by semicolon. Time and timezone are optional.
    private static final DateTimeFormatter[] DATE_TIME_FORMATTER = { //
                    // Compact format: yyyyMMdd with optional time and timezone
                    createFormatter("yyyyMMdd[;HHmmss][ z]"), //
                    createFormatter("yyyyMMdd[;HH:mm:ss][ z]"), //

                    // ISO format (comma+space separator for backward compat
                    // with CorporateAction)
                    createFormatter("yyyy-MM-dd[, HH:mm:ss][ z]"), //
                    createFormatter("yyyy-MM-dd[;HHmmss][ z]"), //
                    createFormatter("yyyy-MM-dd[;HH:mm:ss][ z]"), //

                    // US format MM/dd/yyyy (4-digit year first to avoid
                    // ambiguity)
                    createFormatter("MM/dd/yyyy[;HHmmss][ z]"), //
                    createFormatter("MM/dd/yyyy[;HH:mm:ss][ z]"), //

                    // US format MM/dd/yy (2-digit year)
                    createFormatter("MM/dd/yy[;HHmmss][ z]"), //
                    createFormatter("MM/dd/yy[;HH:mm:ss][ z]"), //

                    // European format dd/MM/yyyy (4-digit year first)
                    createFormatter("dd/MM/yyyy[;HHmmss][ z]"), //
                    createFormatter("dd/MM/yyyy[;HH:mm:ss][ z]"), //

                    // European format dd/MM/yy (2-digit year)
                    createFormatter("dd/MM/yy[;HHmmss][ z]"), //
                    createFormatter("dd/MM/yy[;HH:mm:ss][ z]"), //

                    // Month abbreviation format dd-MMM-yy (e.g., 15-Jan-24)
                    createFormatter("dd-MMM-yy[;HHmmss][ z]"), //
                    createFormatter("dd-MMM-yy[;HH:mm:ss][ z]"), //
    };

    private static DateTimeFormatter createFormatter(String pattern)
    {
        return new DateTimeFormatterBuilder() //
                        .appendPattern(pattern) //
                        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0) //
                        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0) //
                        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0) //
                        .toFormatter(Locale.US);
    }

    /**
     * Parses a date/time string using all supported formats.
     * <p/>
     * If a timezone is present, it is dropped (not converted).
     *
     * @param dateTime
     *            the date/time string to parse
     * @return the parsed LocalDateTime, or null if parsing fails
     */
    /* package */ static LocalDateTime parseDateTime(String dateTime)
    {
        if (dateTime == null || dateTime.isEmpty())
            return null;

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTER)
        {
            try
            {
                TemporalAccessor parsed = formatter.parseBest(dateTime, ZonedDateTime::from, LocalDateTime::from);
                if (parsed instanceof ZonedDateTime zdt)
                    return zdt.toLocalDateTime();
                return LocalDateTime.from(parsed);
            }
            catch (DateTimeParseException ignore)
            {
                // try next formatter
            }
        }

        return null;
    }


    /**
     * Constructs an IBFlexStatementExtractor with the given client.
     * Initializes the list of securities and exchange mappings.
     *
     * @param client The client for which the extractor is created.
     */
    public IBFlexStatementExtractor(Client client)
    {
        this.client = client;
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
        // --- US & Major Markets – No Suffix ---
        // AMEX, ARCA, BATS, NASDAQ, NYSE

        // --- Canada ---
        exchanges.put("TSE", "TO");      // Toronto Stock Exchange
        exchanges.put("VENTURE", "V");   // TSX Venture Exchange

        // --- United Kingdom & Ireland ---
        exchanges.put("LSE", "L");       // London Stock Exchange
        exchanges.put("LSEIOB1", "IL");  // LSE International Order Book
        exchanges.put("ISED", "IR");     // Euronext Dublin (Irish Stock Exchange)

        // --- Europe: Euronext Group ---
        exchanges.put("AEB", "AS");      // Euronext Amsterdam
        exchanges.put("ENEXT.BE", "BR"); // Euronext Brussels
        exchanges.put("BVL", "LS");      // Euronext Lisbon
        exchanges.put("SBF", "PA");      // Euronext Paris
        exchanges.put("BVME", "MI");     // Borsa Italiana (Milan)

        // --- Europe: DACH (Germany, Austria, Switzerland) ---
        exchanges.put("IBIS", "DE");     // XETRA (Germany)
        exchanges.put("TGATE", "DE");    // TradeGate (Germany)
        exchanges.put("FWB", "F");       // Frankfurt Stock Exchange
        exchanges.put("SWB", "SG");      // Stuttgart Stock Exchange
        exchanges.put("EBS", "SW");      // SIX Swiss Exchange
        exchanges.put("SWX", "SW");      // SIX Swiss Exchange
        exchanges.put("VSE", "VI");      // Vienna Stock Exchange

        // --- Europe: Nordic & Baltic ---
        exchanges.put("SFB", "ST");      // Nasdaq Stockholm
        exchanges.put("OSE", "OL");      // Oslo Stock Exchange
        exchanges.put("OMXNO", "OL");    // Nasdaq OMX Nordic (Usually maps to Oslo for Eq)
        exchanges.put("CPH", "CO");      // Nasdaq Copenhagen
        exchanges.put("HEX", "HE");      // Nasdaq Helsinki
        exchanges.put("N.RIGA", "RG");   // Nasdaq Riga
        exchanges.put("N.TALLINN", "TL");// Nasdaq Tallinn
        exchanges.put("N.VILNIUS", "VS");// Nasdaq Vilnius

        // --- Europe: Other ---
        exchanges.put("BM", "MC");       // Bolsas y Mercados Españoles (Madrid)
        exchanges.put("WSE", "WA");      // Warsaw Stock Exchange
        exchanges.put("PRA", "PR");      // Prague Stock Exchange
        exchanges.put("BUX", "BD");      // Budapest Stock Exchange

        // --- Asia: Greater China & Connect Programs ---
        exchanges.put("SEHK", "HK");     // Hong Kong Stock Exchange
        exchanges.put("SEHKNTL", "SS");  // Shanghai-HK Connect (Maps to Shanghai)
        exchanges.put("SEHKSTAR", "SS"); // Shanghai STAR Market (Maps to Shanghai)
        exchanges.put("SEHKSZSE", "SZ"); // Shenzhen-HK Connect (Maps to Shenzhen)
        exchanges.put("CHINEXT", "SZ");  // ChiNext (Maps to Shenzhen)
        exchanges.put("TWSE", "TW");     // Taiwan Stock Exchange

        // --- Asia: Other ---
        exchanges.put("TSEJ", "T");      // Tokyo Stock Exchange
        exchanges.put("SGX", "SI");      // Singapore Exchange
        exchanges.put("NSE", "NS");      // National Stock Exchange of India

        // --- Rest of World ---
        exchanges.put("ASX", "AX");      // Australian Securities Exchange
        exchanges.put("MEXI", "MX");     // Mexican Stock Exchange
        exchanges.put("BOVESPA", "SA");  // B3 (Brazil)
        exchanges.put("JSE", "JO");      // Johannesburg Stock Exchange (South Africa)
    }

    /**
     * Imports an Interactive Broker Activity Statement from an XML file.
     * Returns one result per FlexStatement (i.e., per account) in the file.
     *
     * @param f The input stream of the XML file.
     * @return The list of extraction results, one per account.
     */
    /* package */ List<IBFlexStatementExtractorResult> importActivityStatement(InputStream f)
    {
        List<IBFlexStatementExtractorResult> results = new ArrayList<>();

        try (InputStream fileInputStream = f)
        {
            // Setup XML document parsing with secure features
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileInputStream);

            doc.getDocumentElement().normalize();

            // Process each FlexStatement separately (one per account)
            NodeList statements = doc.getElementsByTagName("FlexStatement");
            for (int i = 0; i < statements.getLength(); i++)
            {
                Node node = statements.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    var result = new IBFlexStatementExtractorResult();
                    result.parseStatement((Element) node);
                    results.add(result);
                }
            }
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            // Handle exceptions and add errors to a new result
            var result = new IBFlexStatementExtractorResult();
            result.addError(e);
            results.add(result);
        }

        return results;
    }

    /**
     * @formatter:off
     * The IBFlexStatementExtractorResult class represents the result of importing
     * a single FlexStatement (i.e., one account) from an Interactive Broker
     * Activity Statement XML file.
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
        private static final String ASSETKEY_CASH = "CASH";
        private static final String ASSETKEY_FUND = "FUND";
        private static final String ASSETKEY_OPTION = "OPT";
        private static final String ASSETKEY_CERTIFICATE = "IOPT";
        private static final String ASSETKEY_FUTURE_OPTION = "FOP";
        private static final String ASSETKEY_WARRANTS = "WAR";

        private static final FixedExchangeRateProvider FIXED_RATE_PROVIDER = new FixedExchangeRateProvider();

        private Element statement;
        private List<Exception> errors = new ArrayList<>();
        private List<Item> results = new ArrayList<>();
        private String accountCurrency = null;
        private Map<String, Account> accounts = new HashMap<>();
        private Portfolio portfolio = null;

        // Map to store currency conversion rates by (date, "fromCurrency-toCurrency")
        private Map<Pair<String, String>, BigDecimal> conversionRates = new HashMap<>();

        /**
         * Processes ConversionRate elements to build currency conversion rate
         * mapping. Stores exchange rates from each currency to the account base
         * currency.
         */
        private Consumer<Element> buildConversionRates = element -> {
            String reportDate = element.getAttribute("reportDate");
            String toCurrency = element.getAttribute("toCurrency");
            String fromCurrency = element.getAttribute("fromCurrency");
            String rateStr = element.getAttribute("rate");

            if (!rateStr.equals("-1"))
            {
                BigDecimal rate = asExchangeRate(rateStr);
                Pair<String, String> key = new Pair<>(reportDate, fromCurrency + "-" + toCurrency);
                conversionRates.put(key, rate);
            }
        };

        /**
         * Builds account information based on the provided XML element. Extracts the currency
         * and acctAlias attributes from the element. The currency is converted to a currency
         * code and sets the corresponding currency unit for the IB account if valid. The
         * acctAlias is matched against client account names to find the corresponding account.
         *
         * @param element The XML element containing account information.
         */
        private Consumer<Element> buildAccountInformation = element -> {
            String currency = asCurrencyCode(element.getAttribute("currency"));
            if (currency != null && !currency.isEmpty())
            {
                CurrencyUnit currencyUnit = CurrencyUnit.getInstance(currency);
                if (currencyUnit != null)
                    accountCurrency = currency;
            }

            String acctAlias = element.getAttribute("acctAlias");
            String acctID = element.getAttribute("accountId");

            portfolio = client.getPortfolios().stream()
                            .filter(p -> p.getName() != null
                                            && (p.getName().equals(acctAlias) || p.getName().equals(acctID)))
                            .findFirst() //
                            .orElse(null);

            if (portfolio != null && portfolio.getReferenceAccount() != null)
            {
                Account reference = portfolio.getReferenceAccount();

                // Always add the reference account as a candidate.
                accounts.put(reference.getCurrencyCode(), reference);

                // Work around the fact that we can't have multiple
                // reference accounts at the moment by checking for accounts
                // with the same prefix as the reference account.
                client.getAccounts().stream().filter(a -> a.getName().startsWith(reference.getName()))
                                .forEach(a -> accounts.putIfAbsent(a.getCurrencyCode(), a));
            }
        };

        /**
         * Constructs an AccountTransactionItem based on the information provided in the XML element.
         */
        private Consumer<Element> buildAccountTransaction = element -> {
            AccountTransaction accountTransaction = new AccountTransaction();

            accountTransaction.setDateTime(extractDate(element));

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
                case "Management Fees":
                case "Advisor Fees":
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
                addItem(accountTransaction);
        };

        /**
         * Constructs a AccountTransferItem based on the information provided in the XML element.
         */
        private Consumer<Element> buildCashTransaction = element -> {
            AccountTransferEntry cashTransaction = new AccountTransferEntry();

            // Check if the asset category is supported
            if (!ASSETKEY_CASH.equals(element.getAttribute("assetCategory")))
                return;

            // Check if the level of detail is supported
            String lod = element.getAttribute("levelOfDetail");
            if (lod.contains("ASSET_SUMMARY")
                            || lod.contains("SYMBOL_SUMMARY")
                            || lod.contains("ORDER"))
                return;

            cashTransaction.setDate(extractDate(element));
            
            // get the transaction type
            String tType = element.getAttribute("buySell");
            boolean isSell;
            switch (tType)
            {
                case "SELL":
                    isSell = true;
                    break;
                case "BUY":
                    isSell = false;
                    break;
                case "BUY (Ca.)", "SELL (Ca.)":
                    // cancellations are not supported at the moment
                    return;
                default:
                    // unknown type. Do nothing.
                    return;
            }

            // Ensure all required attributes are present
            if (!element.hasAttribute("symbol") || 
                !element.hasAttribute("tradeMoney") || 
                !element.hasAttribute("quantity"))
            {
                return;
            }

            // Split the symbol into "source" and "target" currency (e.g. EUR.USD)
            // where USD is the currency paid and EUR is the currency bought. We
            // may need to swap what's source and what't target below.
            String symbol = element.getAttribute("symbol");
            String[] parts = symbol.split("\\.");
            if (parts.length != 2)
                return;

            String sourceCurrency = isSell ? parts[0] : parts[1];
            String targetCurrency = isSell ? parts[1] : parts[0];
            
            String sourceAmount = element.getAttribute(isSell ? "quantity" : "tradeMoney");
            String targetAmount = element.getAttribute(isSell ? "tradeMoney" : "quantity");
            
            BigDecimal exchangeRate = asExchangeRate(element.getAttribute("tradePrice"));
            if (isSell)
                exchangeRate = ExchangeRate.inverse(exchangeRate);
            
            // Set the amounts
            Money source = Money.of(sourceCurrency, asAmount(sourceAmount));
            Money target = Money.of(targetCurrency, asAmount(targetAmount));

            // Since this is a cash transaction, we don't need to convert the amounts currencies
            // therefore we skip setAmount()
            cashTransaction.getSourceTransaction().setMonetaryAmount(source);
            cashTransaction.getTargetTransaction().setMonetaryAmount(target);

            cashTransaction.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, source, target, exchangeRate));

            // set note
            cashTransaction.setNote(extractNote(element));

            addItem(cashTransaction);

            // check fees

            Money fees = Money.of(asCurrencyCode(element.getAttribute("ibCommissionCurrency")), //
                            asAmount(element.getAttribute("ibCommission")));
            if (!fees.isZero())
            {
                AccountTransaction feesTransaction = new AccountTransaction();
                feesTransaction.setType(AccountTransaction.Type.FEES);
                feesTransaction.setDateTime(cashTransaction.getSourceTransaction().getDateTime());
                feesTransaction.setMonetaryAmount(fees);
                feesTransaction.setNote(MessageFormat.format("Commission paid for {0}",
                                cashTransaction.getNote() != null ? cashTransaction.getNote()
                                                : Messages.LabelTransferAccount));

                addItem(feesTransaction);
            }
            
            // check taxes
            
            Money taxes = Money.of(asCurrencyCode(element.getAttribute("currency")), //
                            asAmount(element.getAttribute("taxes")));
            if (!taxes.isZero())
            {
                AccountTransaction taxesTransaction = new AccountTransaction();
                taxesTransaction.setType(AccountTransaction.Type.TAXES);
                taxesTransaction.setDateTime(cashTransaction.getSourceTransaction().getDateTime());
                taxesTransaction.setMonetaryAmount(taxes);
                taxesTransaction.setNote(MessageFormat.format("Taxes paid for {0}",
                                cashTransaction.getNote() != null ? cashTransaction.getNote()
                                                : Messages.LabelTransferAccount));

                addItem(taxesTransaction);
            }
        };

        /**
         * Constructs a BuySellEntryItem based on the information provided in the XML element.
         */
        private Consumer<Element> buildPortfolioTransaction = element -> {
            BuySellEntry portfolioTransaction = new BuySellEntry();

            // Check if the asset category is supported
            if (!Arrays.asList(ASSETKEY_STOCK, //
                            ASSETKEY_FUND, //
                            ASSETKEY_OPTION, //
                            ASSETKEY_CERTIFICATE, //
                            ASSETKEY_FUTURE_OPTION, //
                            ASSETKEY_WARRANTS).contains(element.getAttribute("assetCategory")))
                return;

            // Check if the level of detail is supported
            String lod = element.getAttribute("levelOfDetail");
            if (lod.contains("ASSET_SUMMARY")
                            || lod.contains("SYMBOL_SUMMARY")
                            || lod.contains("ORDER"))
                return;

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
            
            portfolioTransaction.setDate(extractDate(element));

            // Set security before amount so that setAmount can detect currency mismatches in all cases
            portfolioTransaction.setSecurity(this.getOrCreateSecurity(element, true));

            // @formatter:off
            // Set amount and check if the element contains the "netCash"
            // attribute. If the element contains only the "cost" attribute, the
            // amount will be set based on this attribute.
            // @formatter:on
            if (element.hasAttribute("netCash"))
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                                asAmount(element.getAttribute("netCash")));

                setAmount(element, portfolioTransaction.getPortfolioTransaction(), amount);
                // the account transaction must not carry the gross value unit
                // for currency conversion
                portfolioTransaction.getAccountTransaction().setMonetaryAmount(amount);
            }
            else
            {
                Money amount = Money.of(asCurrencyCode(element.getAttribute("currency")),
                                asAmount(element.getAttribute("cost")));

                setAmount(element, portfolioTransaction.getPortfolioTransaction(), amount);
                // the account transaction must not carry the gross value unit
                // for currency conversion
                portfolioTransaction.getAccountTransaction().setMonetaryAmount(amount);
            }

            // Set share quantity
            Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
            Double multiplier = Double.parseDouble(Optional.ofNullable(element.getAttribute("multiplier")).orElse("1"));
            portfolioTransaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor() * multiplier.doubleValue()));

            // Set fees
            Money fees = Money.of(asCurrencyCode(element.getAttribute("ibCommissionCurrency")), asAmount(element.getAttribute("ibCommission")));
            Unit feeUnit = new Unit(Unit.Type.FEE, fees);
            portfolioTransaction.getPortfolioTransaction().addUnit(feeUnit);

            // Set taxes
            Money taxes = Money.of(asCurrencyCode(element.getAttribute("currency")), asAmount(element.getAttribute("taxes")));
            Unit taxUnit = new Unit(Unit.Type.TAX, taxes);
            portfolioTransaction.getPortfolioTransaction().addUnit(taxUnit);

            // Set note
            if (portfolioTransaction.getNote() == null || !portfolioTransaction.getNote().equals(Messages.MsgErrorOrderCancellationUnsupported))
            {
                portfolioTransaction.setNote(extractNote(element));
            }

            ExtractorUtils.fixGrossValueBuySell().accept(portfolioTransaction);

            BuySellEntryItem item = addItem(portfolioTransaction);

            if (portfolioTransaction.getPortfolioTransaction().getCurrencyCode() != null && portfolioTransaction.getPortfolioTransaction().getAmount() == 0)
            {
                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
            }
            else if (Messages.MsgErrorOrderCancellationUnsupported.equals(portfolioTransaction.getPortfolioTransaction().getNote()))
            {
                item.setFailureMessage(Messages.MsgErrorOrderCancellationUnsupported);
            }
        };

        /**
         * Constructs a Corporate Transaction Item based on the information provided in the XML element.
         */
        private Consumer<Element> buildCorporateTransaction = element -> {
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

                addItem(portfolioTransaction);
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

                addItem(portfolioTransaction);
            }
        };

        /**
         * Constructs a Sales Tax Transaction Item based on the information provided in the XML element.
         */
        private Consumer<Element> buildSalesTaxTransaction = element -> {
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
                addItem(accountTransaction);
        };

        /**
         * Sets the specified monetary amount on the given transaction, with an
         * option to include in the portfolio transaction.
         *
         * @param element
         *            The XML element containing transaction details.
         * @param transaction
         *            The transaction object to update with the amount.
         * @param amount
         *            The monetary amount to set on the transaction.
         */
        private void setAmount(Element element, Transaction transaction, Money amount)
        {
            transaction.setMonetaryAmount(amount);

            if (transaction.getSecurity() != null
                            && !transaction.getSecurity().getCurrencyCode().equals(amount.getCurrencyCode()))
            {
                // Transaction currency differs from security currency - create
                // GROSS_VALUE unit
                BigDecimal exchangeRate = getExchangeRate(element, amount.getCurrencyCode(),
                                transaction.getSecurity().getCurrencyCode());

                // Some old testcases contain neither accountCurrency nor fx
                // rates.
                // Don't add a GROSS_VALUE in that case.
                if (exchangeRate != null)
                {
                    exchangeRate = ExchangeRate.inverse(exchangeRate);
                    Money fxAmount = Money.of(transaction.getSecurity().getCurrencyCode(),
                                    BigDecimal.valueOf(amount.getAmount()).divide(exchangeRate, Values.MC)
                                                    .setScale(0, RoundingMode.HALF_UP).longValue());

                    Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                    transaction.addUnit(grossValue);
                }
            }
        }

        /**
         * Extract the date from the transaction.
         * <p/>
         * If possible, set "tradeDate" with "tradeTime" as the correct trading
         * date of the transaction.
         * <p/>
         * If "tradeDate" is not present, use "dateTime" if possible.
         *
         * @return the extracted date or null if not available
         */
        private LocalDateTime extractDate(Element element)
        {
            String dateTime = "";

            // Prefer tradeDate over dateTime for <Trade> tags.
            if (element.hasAttribute("tradeDate"))
            {
                dateTime = element.getAttribute("tradeDate");

                if (element.hasAttribute("tradeTime"))
                {
                    dateTime += ";" + element.getAttribute("tradeTime");
                }
                else if (element.hasAttribute("dateTime")
                                && dateTime.equals(element.getAttribute("dateTime").substring(0, 8)))
                {
                    dateTime = element.getAttribute("dateTime");
                }
            }

            // Prefer reportDate over dateTime for <CashTransaction> tags.
            if (dateTime.isEmpty() && element.hasAttribute("reportDate"))
            {
                dateTime = element.getAttribute("reportDate");
            }

            // All other tags.
            if (dateTime.isEmpty() && element.hasAttribute("dateTime"))
            {
                dateTime = element.getAttribute("dateTime");
            }

            return parseDateTime(dateTime);
        }

        /**
         * Extract the trade and transaction ids as note.
         */
        private String extractNote(Element element)
        {
            StringBuilder note = new StringBuilder();

            // Add Trade-ID note if available
            if (!element.getAttribute("tradeID").isEmpty() && !"N/A".equals(element.getAttribute("tradeID")))
            {
                note.append("Trade-ID: ").append(element.getAttribute("tradeID"));
            }

            // Add Transaction-ID note if available
            if (!element.getAttribute("transactionID").isEmpty()
                            && !"N/A".equals(element.getAttribute("transactionID")))
            {
                if (note.length() > 0)
                    note.append(" | ");
                note.append("Transaction-ID: ").append(element.getAttribute("transactionID"));
            }

            return note.length() > 0 ? note.toString() : null;
        }

        /**
         * Gets the exchange rate to go from fromCurrency to toCurrency.
         *
         * @param element
         *            The XML element containing transaction details
         * @param fromCurrency
         *            The currency to convert from
         * @param toCurrency
         *            The currency to convert to
         * @return The exchange rate, or null if it cannot be determined
         */
        private BigDecimal getExchangeRate(Element element, String fromCurrency, String toCurrency)
        {
            if (fromCurrency.equals(toCurrency))
                return BigDecimal.ONE;

            LocalDateTime dateTime = extractDate(element);
            if (dateTime == null)
                return null;

            String dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // Attempt a direct lookup in the rates table.
            Pair<String, String> key = new Pair<>(dateStr, fromCurrency + "-" + toCurrency);
            BigDecimal rate = conversionRates.get(key);
            if (rate != null)
                return rate;

            key = new Pair<>(dateStr, toCurrency + "-" + fromCurrency);
            rate = conversionRates.get(key);
            if (rate != null)
                return ExchangeRate.inverse(rate);

            // Fall back to using accountCurrency as an intermediate.
            if (accountCurrency != null)
            {
                if (toCurrency.equals(accountCurrency) && element.hasAttribute("fxRateToBase"))
                {
                    // Avoid cross rate if possible by using fxRateToBase from
                    // the
                    // transaction element itself.
                    return asExchangeRate(element.getAttribute("fxRateToBase"));
                }

                // Attempt to calculate cross rate via accountCurrency. No use
                // in trying a different intermediate currency, it seems like
                // toCurrency is only ever the account's base.
                Pair<String, String> fromKey = new Pair<>(dateStr, fromCurrency + "-" + accountCurrency);
                Pair<String, String> toKey = new Pair<>(dateStr, toCurrency + "-" + accountCurrency);

                BigDecimal fromRate = conversionRates.get(fromKey);
                BigDecimal toRate = conversionRates.get(toKey);

                if (fromRate != null && toRate != null)
                    return fromRate.divide(toRate, 10, RoundingMode.HALF_DOWN);
            }

            // Check if there's a fixed exchange rate (e.g. GBP/GBX)
            BigDecimal fixedRate = getWellKnownFixedExchangeRate(fromCurrency, toCurrency);
            if (fixedRate != null)
                return fixedRate;

            return null;
        }

        /**
         * Returns the exchange rate for currency pairs with a fixed
         * relationship (e.g. GBX/GBP) using FixedExchangeRateProvider. Handles
         * both directions.
         *
         * @param fromCurrency
         *            The source currency
         * @param toCurrency
         *            The target currency
         * @return The exchange rate, or null if not a known fixed-rate pair
         */
        private BigDecimal getWellKnownFixedExchangeRate(String fromCurrency, String toCurrency)
        {
            for (var series : FIXED_RATE_PROVIDER.getAvailableTimeSeries(null))
            {
                if (series.getRates() == null || series.getRates().isEmpty())
                    continue;

                var rate = series.getRates().get(0).getValue();

                if (series.getBaseCurrency().equals(fromCurrency) && series.getTermCurrency().equals(toCurrency))
                    return rate;
                else if (series.getBaseCurrency().equals(toCurrency) && series.getTermCurrency().equals(fromCurrency))
                    return BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP);
            }

            return null;
        }

        /**
         * @formatter:off
         * Imports model objects from the statement based on the specified type using the provided handling function.
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
        private void importModelObjects(String type, Consumer<Element> handleNodeFunction)
        {
            NodeList nList = statement.getElementsByTagName(type);
            for (int temp = 0; temp < nList.getLength(); temp++)
            {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    try
                    {
                        handleNodeFunction.accept((Element) nNode);
                    }
                    catch (Exception e)
                    {
                        errors.add(e);
                    }
                }
            }
        }

        /**
         * Parses a single FlexStatement element and processes its model objects.
         *
         * @param statementElement The FlexStatement XML element to parse.
         */
        public void parseStatement(Element statementElement)
        {
            this.statement = statementElement;

            if (statement == null)
                return;

            // Process conversion rates first
            importModelObjects("ConversionRate", buildConversionRates);

            // Import AccountInformation
            importModelObjects("AccountInformation", buildAccountInformation);

            // Process all Trades
            importModelObjects("Trade", buildPortfolioTransaction);

            // Process all cash related trades
            importModelObjects("Trade", buildCashTransaction);

            // Process all CashTransaction
            importModelObjects("CashTransaction", buildAccountTransaction);

            // Process all CorporateTransactions
            importModelObjects("CorporateAction", buildCorporateTransaction);

            // Process all SalesTaxes
            importModelObjects("SalesTax", buildSalesTaxTransaction);

            // TODO: Process all FxTransactions
        }

        /**
         * Adds an AccountTransaction result and sets an account if possible.
         */
        private TransactionItem addItem(AccountTransaction transaction)
        { 
            TransactionItem item = new TransactionItem(transaction);
            item.setAccountPrimary(accounts.get(transaction.getCurrencyCode()));
            results.add(item);
            return item;
        }

        /**
         * Adds a PortfolioTransaction result and sets a portfolio if possible.
         */
        private TransactionItem addItem(PortfolioTransaction transaction)
        {
            TransactionItem item = new TransactionItem(transaction);
            item.setPortfolioPrimary(portfolio);
            results.add(item);
            return item;
        }

        /**
         * Adds a BuySellEntry result and sets both account and portfolio if
         * possible.
         */
        private BuySellEntryItem addItem(BuySellEntry entry)
        {
            BuySellEntryItem item = new BuySellEntryItem(entry);
            item.setAccountPrimary(accounts.get(entry.getAccountTransaction().getCurrencyCode()));
            item.setPortfolioPrimary(portfolio);
            results.add(item);
            return item;
        }

        /**
         * Adds an AccountTransferEntry result and sets both primary and
         * secondary account if possible.
         */
        private AccountTransferItem addItem(AccountTransferEntry entry)
        {
            AccountTransferItem item = new AccountTransferItem(entry, true);
            item.setAccountPrimary(accounts.get(entry.getSourceTransaction().getCurrencyCode()));
            item.setAccountSecondary(accounts.get(entry.getTargetTransaction().getCurrencyCode()));
            results.add(item);
            return item;
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
            results.add(new SecurityItem(security));

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
            List<Item> items = new ArrayList<>();
            for (var result : importActivityStatement(in))
            {
                errors.addAll(result.getErrors());
                items.addAll(result.getResults());
            }
            return items;
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
