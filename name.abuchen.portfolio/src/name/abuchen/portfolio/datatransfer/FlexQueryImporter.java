package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.datatransfer.CSVImporter.Column;

public class FlexQueryImporter
{

    private final Client client;
    private final File inputFile;

    private Column[] columns;
    private List<String[]> values;
    
    private HashMap<String,String> exchanges;

    // List of CSV Lines
    public List<String[]> getRawValues()
    {
        return values;
    }

    public Column[] getColumns()
    {
        return columns;
    }
    
    
    
    public FlexQueryImporter(Client client, File file)
    {
        this.client = client;
        this.inputFile = file;
        
        // Maps Interactive Broker Exchange to Yahoo
        this.exchanges = new HashMap<String,String>();
        this.exchanges.put("SWX","SW");
        this.exchanges.put("EBS","SW");     //ISLAND,BEX,
        this.exchanges.put("TSE","TO");
        this.exchanges.put("VENTURE","V");
    }
     
    
    protected Date convertDate(String date) throws ParseException
    {
        //20111215
        if (date.length() > 8) {
            DateFormat df = new SimpleDateFormat ("yyyy-MM-dd");
            return df.parse(date);
        } else {
            DateFormat df = new SimpleDateFormat ("yyyyMMdd");
            return df.parse(date);
        }
    }

    /**
     * Verify if a Transaction t2 already exists in the portfolio 
     * @param portfolio
     * @param t2
     * @return true or false if transaction exists
     */
    public boolean ptransactionExists(Portfolio portfolio, PortfolioTransaction t2) {
        
        for (PortfolioTransaction t : portfolio.getTransactions()) {
            
            
            if( t.getDate().equals(t2.getDate()) && 
                t.getType().equals(t2.getType()) ) {
                return true;
            } else {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Lookup an active Portfolio for accountId
     * Portfolioname in PP has to be equal to the IB Account
     * @param accountId
     * @return
     */
    public Portfolio getPortfolio(String accountId) {      
        List<Portfolio> portfolios = client.getActivePortfolios();
        for (Portfolio temp : portfolios) {
            if ( temp.getName().equals(accountId) )
                return temp;
        }
        
        throw new IllegalArgumentException();
    }
    
    /**
     * Lookup the Account with the Name accountId
     * Accountname in PP has to be equal to the IB Account
     * @param accountId
     * @return
     */
    public Account getAccount(String accountId) {
        List<Account> accounts = client.getActiveAccounts();
        for (Account temp : accounts) {
            if ( temp.getName().equals(accountId) )
                return temp;
        }
        
        throw new IllegalArgumentException();        
    }
    
    /**
     * Lookup a Security in the Model or create a new one if it does not yet exist
     * It uses IB ContractID (conID) for the WKN, tries to degrade if conID or ISIN are not available
     * @param client
     * @param eElement
     * @param doCreate
     * @return
     */
    protected Security lookupSecurity(Client client, Element eElement, boolean doCreate)
    {
        // Lookup the Exchange Suffix for Yahoo
        String tickerSymbol = eElement.getAttribute("symbol");
        String yahooSymbol = tickerSymbol;
        String exchange = eElement.getAttribute("exchange");
        String isin  = eElement.getAttribute("isin");
        String cusip = eElement.getAttribute("cusip");
        // Store cusip in isin if isin is not available
        if (isin.length() == 0 && cusip.length()>0)
            isin = cusip;
        
        String conID = eElement.getAttribute("conid");
        String description = eElement.getAttribute("description");
        
        if (tickerSymbol != null) {
            String exch = this.exchanges.get(exchange);
            if ( exch != null && exch.length() > 0 )
                yahooSymbol = tickerSymbol + '.' +  exch;            
        }
        
        
        for (Security s : client.getSecurities())
        {
            //Find security with same conID or isin or yahooSymbol
            if (conID != null && conID.length() > 0 && conID.equals(s.getWkn())) {
                return s;
            }
            if (isin != null &&  isin.length() > 0 && isin.equals(s.getIsin())) {
                return s;
            }
            if (yahooSymbol != null &&  yahooSymbol.length() > 0 && yahooSymbol.equals(s.getTickerSymbol())) {
                return s;
            }
        }

        if (!doCreate)
            return null;
        
        Security security = new Security(MessageFormat.format(Messages.CSVImportedSecurityLabel, tickerSymbol), "", yahooSymbol, QuoteFeed.MANUAL);
        security.setIsin(isin);
        // We use the Wkn to store the IB conID as a unique identifier
        security.setWkn(conID);
        
        security.setNote(description);
        client.addSecurity(security);

        return security;
    }    
    
    /**
     * Construct a BuySellEntry based on Trade object defined in eElement
     * @param client
     * @param eElement
     * @throws ParseException
     */
    void buildPortfolioTransaction(Client client, Element eElement) throws ParseException
    { 
        
        //Unused Information from Flexstatement Trades, to used in the future
        //System.out.println("currency : " + eElement.getAttribute("currency"));
        //System.out.println("tradeTime : " + eElement.getAttribute("tradeTime"));
        //System.out.println("transactionID : " + eElement.getAttribute("transactionID"));
        //System.out.println("ibOrderID : " + eElement.getAttribute("ibOrderID"));
        
        Portfolio portfolio = this.getPortfolio(eElement.getAttribute("accountId"));        
        
        //PortfolioTransaction transaction = new PortfolioTransaction(); 
        BuySellEntry transaction = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
        
        // Set Transaction Type
        if (eElement.getAttribute("buySell").equals("BUY") ) {
            transaction.setType(PortfolioTransaction.Type.BUY);
        } else if (eElement.getAttribute("buySell").equals("SELL") ) {
            transaction.setType(PortfolioTransaction.Type.SELL);
        } else throw new IllegalArgumentException();
                
        String d = eElement.getAttribute("tradeDate");
        if (d == null || d.length() == 0 ) {
            d = eElement.getAttribute("reportDate");     // use reportDate for CorporateActions
        }
        transaction.setDate(convertDate(d));

        // Share Quantity
        Double qty = Math.abs( Double.parseDouble(eElement.getAttribute("quantity")));
        transaction.setShares(  Long.valueOf((long) Math.round(qty.doubleValue() * Values.Share.factor())) );       
                
        Double fees = Math.abs( Double.parseDouble(eElement.getAttribute("ibCommission")) );
        transaction.setFees(  Long.valueOf((long) Math.round(fees.doubleValue() * Values.Amount.factor())) );
        
        Double taxes = Math.abs( Double.parseDouble(eElement.getAttribute("taxes")) );
        transaction.setTaxes( Long.valueOf((long) Math.round(taxes.doubleValue() * Values.Amount.factor())) );
        
        //Set the Amount which is ( tradePrice * qty ) + Fees + Taxes
        Double amount = Double.parseDouble(eElement.getAttribute("tradePrice")) * qty + fees + taxes;
        transaction.setAmount( Math.abs( Long.valueOf((long) Math.round(amount.doubleValue() * Values.Amount.factor() )) ));        
        
        transaction.setSecurity(this.lookupSecurity(client, eElement, true));
        
        transaction.setNote(eElement.getAttribute("description"));

        // create transaction only if it does not yet exist
        if ( ! ptransactionExists( portfolio, transaction.getPortfolioTransaction()) ) {
            transaction.insert();
        }
    }
    
    /**
     * Constructs a Transaction object for a Corporate Transaction defined in eElement.
     * @param client
     * @param eElement
     * @throws ParseException
     */
    void buildCorporateTransaction(Client client, Element eElement) throws ParseException
    { 
        Portfolio portfolio = this.getPortfolio(eElement.getAttribute("accountId"));         
        //Transaction transaction;     
        System.out.println(eElement.getNodeName());
        
        Double amount = Double.parseDouble(eElement.getAttribute("proceeds")) ;
        if (amount != 0) {
            BuySellEntry transaction = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
            
            if ( Double.parseDouble(eElement.getAttribute("quantity")) >= 0 ) {
                transaction.setType(PortfolioTransaction.Type.BUY);
            } else {
                transaction.setType(PortfolioTransaction.Type.SELL);
            }
            transaction.setDate(convertDate(eElement.getAttribute("reportDate")));
            // Share Quantity
            Double qty = Math.abs( Double.parseDouble(eElement.getAttribute("quantity")));
            transaction.setShares(  Long.valueOf((long) Math.round(qty.doubleValue() * Values.Share.factor())) );
            
            transaction.setSecurity(this.lookupSecurity(client, eElement, true));
            transaction.setNote(eElement.getAttribute("description"));
            
            transaction.setAmount( Math.abs( Long.valueOf((long) Math.round(amount.doubleValue() * Values.Amount.factor() )) ));
            
            // create transaction only if it does not yet exist
            if ( ! ptransactionExists( portfolio, transaction.getPortfolioTransaction()) ) {
                transaction.insert();
            }
            
        } else {
            // Set Transaction Type
            PortfolioTransaction transaction = new PortfolioTransaction();
            if ( Double.parseDouble(eElement.getAttribute("quantity")) >= 0 ) {
                transaction.setType(PortfolioTransaction.Type.TRANSFER_IN);
            } else {
                transaction.setType(PortfolioTransaction.Type.TRANSFER_OUT);
            }
            transaction.setDate(convertDate(eElement.getAttribute("reportDate")));
            // Share Quantity
            Double qty = Math.abs( Double.parseDouble(eElement.getAttribute("quantity")));
            transaction.setShares(  Long.valueOf((long) Math.round(qty.doubleValue() * Values.Share.factor())) );
            
            transaction.setSecurity(this.lookupSecurity(client, eElement, true));
            transaction.setNote(eElement.getAttribute("description"));
            // create transaction only if it does not yet exist
            if ( ! ptransactionExists( portfolio, transaction) ) {
                portfolio.addTransaction(transaction);
            }
            
        }
                
    }    
    
    /**
     * Figure out how many shares a dividend payment is related to.
     * Extracts the information from the description string given by IB
     * @param transaction
     * @param eElement
     */
    public void calculateShares(Transaction transaction, Element eElement) {
        Portfolio p = this.getPortfolio(eElement.getAttribute("accountId"));
        
        //Figure out how many shares were holding related to this Dividend Payment
        long numShares = 0;
        String desc = eElement.getAttribute("description");
        double amount = Double.parseDouble(eElement.getAttribute("amount"));
        
        //Regex Patternmatch the Dividend per Share and calculate number of shares
        Pattern dividendPattern = Pattern.compile("DIVIDEND ([0-9]*\\.[0-9]*) .*");
        Matcher tagmatch = dividendPattern.matcher(desc);
        if (tagmatch.find()) {
            double dividend = Double.parseDouble(tagmatch.group(1));
            numShares = Math.round(amount /  dividend ) * Values.Share.factor();
        }
        
        transaction.setShares(numShares);
    }
    
    public void buildAccountTransaction(Client client, Element eElement) throws ParseException {

        
        Account account = this.getAccount(eElement.getAttribute("accountId"));  
        AccountTransaction transaction = new AccountTransaction();
        
        transaction.setDate(convertDate(eElement.getAttribute("dateTime")));
        Double amount = Double.parseDouble(eElement.getAttribute("amount"));
        // Set the Symbol
        if (eElement.getAttribute("symbol").length() > 0 )
            transaction.setSecurity(this.lookupSecurity(client, eElement, true));
        
        // Set Transaction Type
        if (eElement.getAttribute("type").equals("Deposits") || eElement.getAttribute("type").equals("Deposits & Withdrawals")) {
            if ( amount >= 0 ) {
                transaction.setType(AccountTransaction.Type.DEPOSIT);
            } else {
                transaction.setType(AccountTransaction.Type.REMOVAL);
            }
        } else if (eElement.getAttribute("type").equals("Dividends") ) {
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            this.calculateShares(transaction,eElement);
        } else if (eElement.getAttribute("type").equals("Payment In Lieu Of Dividends") ) {
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            this.calculateShares(transaction,eElement);
        } else if (eElement.getAttribute("type").equals("Withholding Tax") ) {
            transaction.setType(AccountTransaction.Type.TAXES);              
        } else if (eElement.getAttribute("type").equals("Broker Interest Paid") ) {
            transaction.setType(AccountTransaction.Type.INTEREST);    
        } else if (eElement.getAttribute("type").equals("Other Fees") ) {
            transaction.setType(AccountTransaction.Type.FEES);
        } else {
            System.out.println("Unkown Transaction Type :"+eElement.getAttribute("type"));
            throw new IllegalArgumentException();
        }
        
        // For interest payments we should not use absolute values
        if ( transaction.getType() != AccountTransaction.Type.INTEREST) {
            amount = Math.abs(amount);
        }
        transaction.setAmount( Long.valueOf((long) Math.round(amount.doubleValue() * Values.Amount.factor() )) );
                
        transaction.setNote(eElement.getAttribute("description"));

        account.addTransaction(transaction);
    }        
    
    public void importModelObjects(Document doc, String type) {
        
        NodeList nList = doc.getElementsByTagName(type);
        for (int temp = 0; temp < nList.getLength(); temp++) {
            
            Node nNode = nList.item(temp);
            
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                try
                {
                    if ( type.equals("Trade") ) {
                        this.buildPortfolioTransaction(client,(Element) nNode );
                    } else if ( type.equals("CorporateAction")) {
                        this.buildCorporateTransaction(client,(Element) nNode );
                    } else if ( type.equals("CashTransaction")) {
                        this.buildAccountTransaction(client, (Element) nNode);
                    }
                }
                catch (ParseException e)
                {
                    e.printStackTrace();
                }
            }                
        }        
    }
    
    /**
     * Import an Interactive Broker Activitystatement from XML File
     * It currently only imports Trades, Corporate Transactions and Cash Transactions
     * @param errors
     */
    public void importActivityStatement(List<Exception> errors)
    {        

        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(this.inputFile);

            doc.getDocumentElement().normalize();
            
            // Process all Trades
            importModelObjects(doc,"Trade");
            
            // Process all CashTransaction            
            importModelObjects(doc,"CashTransaction");
            
            // Process all CorporateTransactions
            importModelObjects(doc,"CorporateAction");
           
            // Process all FxTransactions and ConversionRates
            
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }
        catch (SAXException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
        }        
    }    
}
