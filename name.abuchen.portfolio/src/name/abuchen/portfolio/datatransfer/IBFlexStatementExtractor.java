package name.abuchen.portfolio.datatransfer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import name.abuchen.portfolio.Messages;
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

@SuppressWarnings("nls")
public class IBFlexStatementExtractor implements Extractor
{
    private List<Security> allSecurities;

    private Map<String, String> exchanges;

    public IBFlexStatementExtractor(Client client)
    {
        allSecurities = new ArrayList<>(client.getSecurities());

        // Maps Interactive Broker Exchange to Yahoo Exchanges, to be completed
        this.exchanges = new HashMap<>();

        this.exchanges.put("EBS", "SW");
        this.exchanges.put("LSE", "L");
        this.exchanges.put("SWX", "SW");
        this.exchanges.put("TSE", "TO");
        this.exchanges.put("VENTURE", "V");
    }

    private LocalDate convertDate(String date) throws DateTimeParseException
    {

        if (date.length() > 8)
        {
            return LocalDate.parse(date);
        }
        else
        {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

    /**
     * Import an Interactive Broker ActivityStatement from an XML file. It
     * currently only imports Trades, Corporate Transactions and Cash
     * Transactions.
     */
    /* package */IBFlexStatementExtractorResult importActivityStatement(InputStream f)
    {
        IBFlexStatementExtractorResult result = new IBFlexStatementExtractorResult();
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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
     * Return currency as valid currency code (in the sense that PP is
     * supporting this currency code)
     */
    private String asCurrencyUnit(String currency)
    {
        if (currency == null)
            return CurrencyUnit.EUR;

        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        return unit == null ? CurrencyUnit.EUR : unit.getCurrencyCode();
    }

    @Override
    public String getLabel()
    {
        return Messages.IBXML_Label;
    }

    @Override
    public String getFilterExtension()
    {
        return "*.xml"; //$NON-NLS-1$
    }

    @Override
    public List<Item> extract(List<Extractor.InputFile> files, List<Exception> errors)
    {
        List<Item> results = new ArrayList<>();
        for (Extractor.InputFile f : files)
        {
            try (FileInputStream in = new FileInputStream(f.getFile()))
            {
                IBFlexStatementExtractorResult result = importActivityStatement(in);
                errors.addAll(result.getErrors());
                results.addAll(result.getResults());
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }
        return results;
    }

    private class IBFlexStatementExtractorResult
    {
        private Document document;
        private List<Exception> errors = new ArrayList<>();
        private List<Item> results = new ArrayList<>();
        private String ibAccountCurrency = null;

        private Function<Element, Item> importAccountInformation = element -> {
            String currency = asCurrencyUnit(element.getAttribute("currency"));
            if (currency != null && !currency.isEmpty())
            {
                CurrencyUnit currencyUnit = CurrencyUnit.getInstance(currency);
                if (currencyUnit != null)
                {
                    ibAccountCurrency = currency;
                }
            }
            return null;
        };

        private Function<Element, Item> buildAccountTransaction = element -> {
            AccountTransaction transaction = new AccountTransaction();

            transaction.setDate(convertDate(element.getAttribute("dateTime")));
            Double amount = Double.parseDouble(element.getAttribute("amount"));
            String currency = asCurrencyUnit(element.getAttribute("currency"));

            // Set Transaction Type
            String type = element.getAttribute("type");
            if (type.equals("Deposits") || type.equals("Deposits & Withdrawals") || type.equals("Deposits/Withdrawals"))
            {
                if (amount >= 0)
                {
                    transaction.setType(AccountTransaction.Type.DEPOSIT);
                }
                else
                {
                    transaction.setType(AccountTransaction.Type.REMOVAL);
                }
            }
            else if (type.equals("Dividends") || type.equals("Payment In Lieu Of Dividends"))
            {
                transaction.setType(AccountTransaction.Type.DIVIDENDS);

                // Set the Symbol
                if (element.getAttribute("symbol").length() > 0)
                    transaction.setSecurity(this.getOrCreateSecurity(element, true));

                this.calculateShares(transaction, element);
            }
            else if (type.equals("Withholding Tax"))
            {
                // Set the Symbol
                if (element.getAttribute("symbol").length() > 0)
                    transaction.setSecurity(this.getOrCreateSecurity(element, true));

                if(amount <= 0)
                {
                    transaction.setType(AccountTransaction.Type.TAXES);
                }
                else
                {
                    // Positive taxes are a tax refund: see #310
                    transaction.setType(AccountTransaction.Type.TAX_REFUND);
                }
            }
            else if (type.equals("Broker Interest Received"))
            {
                transaction.setType(AccountTransaction.Type.INTEREST);
            }
            else if (type.equals("Broker Interest Paid"))
            {
                // Paid interests are of type interest charge: see #310
                transaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
            }
            else if (type.equals("Other Fees"))
            {
                if (amount <= 0)
                {
                    transaction.setType(AccountTransaction.Type.FEES);
                }
                else
                {
                    // Positive values are a fee refund
                    transaction.setType(AccountTransaction.Type.FEES_REFUND);
                }
            }
            else
            {
                throw new IllegalArgumentException();
            }

            amount = Math.abs(amount);
            setAmount(element, transaction, amount, currency);

            transaction.setNote(element.getAttribute("description"));

            return new TransactionItem(transaction);
        };

        /**
         * Construct a BuySellEntry based on Trade object defined in eElement
         */
        private Function<Element, Item> buildPortfolioTransaction = element -> {
            String assetCategory = element.getAttribute("assetCategory");
            if (!"STK".equals(assetCategory))
                return null;

            // Unused Information from Flexstatement Trades, to be used in the
            // future: tradeTime, transactionID, ibOrderID
            BuySellEntry transaction = new BuySellEntry();

            // Set Transaction Type
            if (element.getAttribute("buySell").equals("BUY"))
            {
                transaction.setType(PortfolioTransaction.Type.BUY);
            }
            else if (element.getAttribute("buySell").equals("SELL"))
            {
                transaction.setType(PortfolioTransaction.Type.SELL);
            }
            else
            {
                throw new IllegalArgumentException();
            }

            String d = element.getAttribute("tradeDate");
            if (d == null || d.length() == 0)
            {
                // use reportDate for CorporateActions
                d = element.getAttribute("reportDate");
            }
            transaction.setDate(convertDate(d));

            // transaction currency
            String currency = asCurrencyUnit(element.getAttribute("currency"));

            // Set the Amount which is "cost"
            Double amount = Math.abs(Double.parseDouble(element.getAttribute("cost")));
            setAmount(element, transaction.getPortfolioTransaction(), amount, currency);
            setAmount(element, transaction.getAccountTransaction(), amount, currency, false);

            // Share Quantity
            Double qty = Math.abs(Double.parseDouble(element.getAttribute("quantity")));
            transaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor()));

            // fees
            double fees = Math.abs(Double.parseDouble(element.getAttribute("ibCommission")));
            String feesCurrency = asCurrencyUnit(element.getAttribute("ibCommissionCurrency"));
            Unit feeUnit = createUnit(element, Unit.Type.FEE, fees, feesCurrency);
            transaction.getPortfolioTransaction().addUnit(feeUnit);

            // taxes
            double taxes = Math.abs(Double.parseDouble(element.getAttribute("taxes")));
            Unit taxUnit = createUnit(element, Unit.Type.TAX, taxes, currency);
            transaction.getPortfolioTransaction().addUnit(taxUnit);

            transaction.setSecurity(this.getOrCreateSecurity(element, true));

            transaction.setNote(element.getAttribute("description"));

            return new BuySellEntryItem(transaction);
        };

        /**
         * Constructs a Transaction object for a Corporate Transaction defined
         * in eElement.
         */
        private Function<Element, Item> buildCorporateTransaction = eElement -> {
            Money proceeds = Money.of(asCurrencyUnit(eElement.getAttribute("currency")),
                            Values.Amount.factorize(Double.parseDouble(eElement.getAttribute("proceeds"))));

            if (!proceeds.isZero())
            {
                BuySellEntry transaction = new BuySellEntry();

                if (Double.parseDouble(eElement.getAttribute("quantity")) >= 0)
                {
                    transaction.setType(PortfolioTransaction.Type.BUY);
                }
                else
                {
                    transaction.setType(PortfolioTransaction.Type.SELL);
                }
                transaction.setDate(convertDate(eElement.getAttribute("reportDate")));
                // Share Quantity
                double qty = Math.abs(Double.parseDouble(eElement.getAttribute("quantity")));
                transaction.setShares(Values.Share.factorize(qty));

                transaction.setSecurity(this.getOrCreateSecurity(eElement, true));
                transaction.setNote(eElement.getAttribute("description"));

                transaction.setMonetaryAmount(proceeds);

                return new BuySellEntryItem(transaction);

            }
            else
            {
                // Set Transaction Type
                PortfolioTransaction transaction = new PortfolioTransaction();
                if (Double.parseDouble(eElement.getAttribute("quantity")) >= 0)
                {
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                }
                else
                {
                    transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                }
                transaction.setDate(convertDate(eElement.getAttribute("reportDate")));
                // Share Quantity
                Double qty = Math.abs(Double.parseDouble(eElement.getAttribute("quantity")));
                transaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor()));

                transaction.setSecurity(this.getOrCreateSecurity(eElement, true));
                transaction.setNote(eElement.getAttribute("description"));

                transaction.setMonetaryAmount(proceeds);

                return new TransactionItem(transaction);
            }
        };

        private Unit createUnit(Element element, Unit.Type unitType, Double amount, String currency)
        {
            Unit unit;
            if (ibAccountCurrency == null || ibAccountCurrency.equals(currency))
            {
                unit = new Unit(unitType, Money.of(currency, Values.Amount.factorize(amount)));
            }
            else
            {
                // only required when a account currency is available
                String fxRateToBaseString = element.getAttribute("fxRateToBase");
                BigDecimal fxRateToBase;
                if (fxRateToBaseString != null && !fxRateToBaseString.isEmpty())
                {
                    fxRateToBase = new BigDecimal(Double.parseDouble(fxRateToBaseString));
                }
                else
                {
                    fxRateToBase = new BigDecimal(1);
                }
                BigDecimal inverseRate = BigDecimal.ONE.divide(fxRateToBase, 10, BigDecimal.ROUND_HALF_DOWN);

                BigDecimal baseCurrencyMoney = BigDecimal.valueOf(amount.doubleValue()).divide(inverseRate,
                                RoundingMode.HALF_DOWN);

                unit = new Unit(unitType,
                                Money.of(ibAccountCurrency,
                                                Math.round(baseCurrencyMoney.doubleValue() * Values.Amount.factor())),
                                Money.of(currency, Values.Amount.factorize(amount)), fxRateToBase);
            }
            return unit;
        }

        private void setAmount(Element element, Transaction transaction, Double amount, String currency)
        {
            setAmount(element, transaction, amount, currency, true);
        }

        private void setAmount(Element element, Transaction transaction, Double amount, String currency,
                        boolean addUnit)
        {
            if (ibAccountCurrency != null && !ibAccountCurrency.equals(currency))
            {
                // only required when a account currency is available
                String fxRateToBaseString = element.getAttribute("fxRateToBase");
                BigDecimal fxRateToBase;
                if (fxRateToBaseString != null && !fxRateToBaseString.isEmpty())
                {
                    fxRateToBase = new BigDecimal(Double.parseDouble(fxRateToBaseString));
                }
                else
                {
                    fxRateToBase = new BigDecimal(1);
                }
                BigDecimal inverseRate = BigDecimal.ONE.divide(fxRateToBase, 10, BigDecimal.ROUND_HALF_DOWN);

                BigDecimal baseCurrencyMoney = BigDecimal.valueOf(amount.doubleValue() * Values.Amount.factor())
                                .divide(inverseRate, RoundingMode.HALF_DOWN);
                transaction.setAmount(Math.round(baseCurrencyMoney.doubleValue()));
                transaction.setCurrencyCode(ibAccountCurrency);
                if (addUnit)
                {
                    Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, transaction.getMonetaryAmount(),
                                    Money.of(currency, Math.round(amount.doubleValue() * Values.Amount.factor())),
                                    fxRateToBase);

                    transaction.addUnit(grossValue);
                }
            }
            else

            {
                transaction.setAmount(Math.round(amount.doubleValue() * Values.Amount.factor()));
                transaction.setCurrencyCode(currency);
            }
        }

        /**
         * Imports Trades, CorporateActions and CashTransactions from Document
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
            String tickerSymbol = element.getAttribute("symbol");
            // yahoo uses '-' instead of ' '
            String yahooSymbol = tickerSymbol == null ? tickerSymbol : tickerSymbol.replaceAll(" ", "-");
            String exchange = element.getAttribute("exchange");
            String currency = asCurrencyUnit(element.getAttribute("currency"));
            String isin = element.getAttribute("isin");
            String cusip = element.getAttribute("cusip");
            // Store cusip in isin if isin is not available
            if (isin.length() == 0 && cusip.length() > 0)
                isin = cusip;

            String conID = element.getAttribute("conid");
            String description = element.getAttribute("description");

            if (tickerSymbol != null)
            {
                String exch = exchanges.get(exchange);
                if (exch != null && exch.length() > 0)
                    yahooSymbol = tickerSymbol + '.' + exch;
            }

            for (Security s : allSecurities)
            {
                // Find security with same conID or isin or yahooSymbol
                if (conID != null && conID.length() > 0 && conID.equals(s.getWkn()))
                    return s;
                if (isin.length() > 0 && isin.equals(s.getIsin()))
                    return s;
                if (yahooSymbol != null && yahooSymbol.length() > 0 && yahooSymbol.equals(s.getTickerSymbol()))
                    return s;
            }

            if (!doCreate)
                return null;

            Security security = new Security(description, isin, yahooSymbol, QuoteFeed.MANUAL);
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
            String desc = element.getAttribute("description");
            double amount = Double.parseDouble(element.getAttribute("amount"));

            // Regex Pattern matches the Dividend per Share and calculate number
            // of
            // shares
            Pattern dividendPattern = Pattern.compile("DIVIDEND ([0-9]*\\.[0-9]*) .*");
            Matcher tagmatch = dividendPattern.matcher(desc);
            if (tagmatch.find())
            {
                double dividend = Double.parseDouble(tagmatch.group(1));
                numShares = Math.round(amount / dividend) * Values.Share.factor();
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
}
