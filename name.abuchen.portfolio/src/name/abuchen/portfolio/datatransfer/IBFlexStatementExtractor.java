package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Client client;
    private final List<Item> results;
    private List<Security> allSecurities;

    private Map<String, String> exchanges;

    public IBFlexStatementExtractor(Client client)
    {
        this.client = client;
        this.results = new ArrayList<>();
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
     * Lookup a Security in the Model or create a new one if it does not yet
     * exist It uses IB ContractID (conID) for the WKN, tries to degrade if
     * conID or ISIN are not available
     */
    private Security getOrCreateSecurity(Client client, Element eElement, boolean doCreate)
    {
        // Lookup the Exchange Suffix for Yahoo
        String tickerSymbol = eElement.getAttribute("symbol");
        // yahoo uses '-' instead of ' '
        String yahooSymbol = tickerSymbol == null ? tickerSymbol : tickerSymbol.replaceAll(" ", "-");
        String exchange = eElement.getAttribute("exchange");
        String currency = asCurrencyUnit(eElement.getAttribute("currency"));
        String isin = eElement.getAttribute("isin");
        String cusip = eElement.getAttribute("cusip");
        // Store cusip in isin if isin is not available
        if (isin.length() == 0 && cusip.length() > 0)
            isin = cusip;

        String conID = eElement.getAttribute("conid");
        String description = eElement.getAttribute("description");

        if (tickerSymbol != null)
        {
            String exch = this.exchanges.get(exchange);
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
     * Construct a BuySellEntry based on Trade object defined in eElement
     */
    private void buildPortfolioTransaction(Client client, Element eElement) throws ParseException
    {
        // Unused Information from Flexstatement Trades, to be used in the
        // future: tradeTime, transactionID, ibOrderID

        BuySellEntry transaction = new BuySellEntry();

        // Set Transaction Type
        if (eElement.getAttribute("buySell").equals("BUY"))
        {
            transaction.setType(PortfolioTransaction.Type.BUY);
        }
        else if (eElement.getAttribute("buySell").equals("SELL"))
        {
            transaction.setType(PortfolioTransaction.Type.SELL);
        }
        else
        {
            throw new IllegalArgumentException();
        }

        String d = eElement.getAttribute("tradeDate");
        if (d == null || d.length() == 0)
        {
            // use reportDate for CorporateActions
            d = eElement.getAttribute("reportDate");
        }
        transaction.setDate(convertDate(d));

        // transaction currency
        String currency = asCurrencyUnit(eElement.getAttribute("currency"));

        // Set the Amount which is "cost"
        transaction.setCurrencyCode(currency);
        transaction.setAmount(
                        Values.Amount.factorize(Math.abs(Double.parseDouble(eElement.getAttribute("cost")))));

        // Share Quantity
        Double qty = Math.abs(Double.parseDouble(eElement.getAttribute("quantity")));
        transaction.setShares(Math.round(qty.doubleValue() * Values.Share.factor()));

        // fees
        double fees = Math.abs(Double.parseDouble(eElement.getAttribute("ibCommission")));
        String feesCurrency = asCurrencyUnit(eElement.getAttribute("ibCommissionCurrency"));
        Unit unit = new Unit(Unit.Type.FEE, Money.of(feesCurrency, Values.Amount.factorize(fees)));
        transaction.getPortfolioTransaction().addUnit(unit);

        // taxes
        double taxes = Math.abs(Double.parseDouble(eElement.getAttribute("taxes")));
        unit = new Unit(Unit.Type.TAX, Money.of(currency, Values.Amount.factorize(taxes)));
        transaction.getPortfolioTransaction().addUnit(unit);

        transaction.setSecurity(this.getOrCreateSecurity(client, eElement, true));

        transaction.setNote(eElement.getAttribute("description"));

        results.add(new BuySellEntryItem(transaction));

    }

    /**
     * Constructs a Transaction object for a Corporate Transaction defined in
     * eElement.
     */
    private void buildCorporateTransaction(Client client, Element eElement) throws ParseException
    {
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

            transaction.setSecurity(this.getOrCreateSecurity(client, eElement, true));
            transaction.setNote(eElement.getAttribute("description"));

            transaction.setMonetaryAmount(proceeds);

            results.add(new BuySellEntryItem(transaction));

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

            transaction.setSecurity(this.getOrCreateSecurity(client, eElement, true));
            transaction.setNote(eElement.getAttribute("description"));

            transaction.setMonetaryAmount(proceeds);

            results.add(new TransactionItem(transaction));
        }

    }

    /**
     * Figure out how many shares a dividend payment is related to. Extracts the
     * information from the description string given by IB
     */
    private void calculateShares(Transaction transaction, Element eElement)
    {
        // Figure out how many shares were holding related to this Dividend
        // Payment
        long numShares = 0;
        String desc = eElement.getAttribute("description");
        double amount = Double.parseDouble(eElement.getAttribute("amount"));

        // Regex Pattern matches the Dividend per Share and calculate number of
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

    private void buildAccountTransaction(Client client, Element eElement) throws ParseException
    {
        AccountTransaction transaction = new AccountTransaction();

        transaction.setDate(convertDate(eElement.getAttribute("dateTime")));
        Double amount = Double.parseDouble(eElement.getAttribute("amount"));
        String currency = asCurrencyUnit(eElement.getAttribute("currency"));

        // Set Transaction Type
        String type = eElement.getAttribute("type");
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
            if (eElement.getAttribute("symbol").length() > 0)
                transaction.setSecurity(this.getOrCreateSecurity(client, eElement, true));

            this.calculateShares(transaction, eElement);
        }
        else if (type.equals("Withholding Tax"))
        {
            // Set the Symbol
            if (eElement.getAttribute("symbol").length() > 0)
                transaction.setSecurity(this.getOrCreateSecurity(client, eElement, true));

            if(amount > 0) {
                transaction.setType(AccountTransaction.Type.TAXES);
            } else {
                // Negative taxes are a tax refund: see #310
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
            transaction.setType(AccountTransaction.Type.FEES);
        }
        else
        {
            throw new IllegalArgumentException();
        }

        amount = Math.abs(amount);
        transaction.setAmount(Math.round(amount.doubleValue() * Values.Amount.factor()));
        transaction.setCurrencyCode(currency);

        transaction.setNote(eElement.getAttribute("description"));

        results.add(new TransactionItem(transaction));
    }

    /**
     * Imports Trades, CorporateActions and CashTransactions from Document
     */
    private void importModelObjects(Document doc, String type, List<Exception> errors)
    {
        NodeList nList = doc.getElementsByTagName(type);
        for (int temp = 0; temp < nList.getLength(); temp++)
        {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE)
            {
                try
                {
                    if (type.equals("Trade"))
                    {
                        String assetCategory = ((Element) nNode).getAttribute("assetCategory");
                        if ("STK".equals(assetCategory))
                        {
                            this.buildPortfolioTransaction(client, (Element) nNode);
                        }
                    }
                    else if (type.equals("CorporateAction"))
                    {
                        this.buildCorporateTransaction(client, (Element) nNode);
                    }
                    else if (type.equals("CashTransaction"))
                    {
                        this.buildAccountTransaction(client, (Element) nNode);
                    }
                }
                catch (ParseException e)
                {
                    errors.add(e);
                }
            }
        }
    }

    /**
     * Import an Interactive Broker ActivityStatement from an XML file. It
     * currently only imports Trades, Corporate Transactions and Cash
     * Transactions.
     */
    /* package */void importActivityStatement(InputStream f, List<Exception> errors)
    {

        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);

            doc.getDocumentElement().normalize();

            // Process all Trades
            importModelObjects(doc, "Trade", errors);

            // Process all CashTransaction
            importModelObjects(doc, "CashTransaction", errors);

            // Process all CorporateTransactions
            importModelObjects(doc, "CorporateAction", errors);

            // TODO: Process all FxTransactions and ConversionRates
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            errors.add(e);
        }
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

    /* package */List<Item> getResults()
    {
        return results;
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
    public List<Item> extract(List<File> files, List<Exception> errors)
    {
        results.clear();
        for (File f : files)
        {
            try
            {
                importActivityStatement(new FileInputStream(f), errors);
            }
            catch (FileNotFoundException e)
            {
                errors.add(e);
            }
        }
        return results;
    }

}
