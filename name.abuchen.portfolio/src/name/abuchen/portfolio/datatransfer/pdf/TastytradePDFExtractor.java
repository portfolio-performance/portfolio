package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.OccOsiSymbology;

@SuppressWarnings("nls")
public class TastytradePDFExtractor extends AbstractPDFExtractor
{
    private List<BuySellEntry> m_optionTradesInImport = new ArrayList<>();

    public TastytradePDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("TASTYWORKS");
        addBankIdentifier("tastytrade");
        addBankIdentifier("Apex Clearing Corporation");

        addSummaryStatementBuySellTransaction(); // Purchase and sale ( multiple settlements )
        addDepotStatementTransaction(); // Securities account transactions ( Settlement account )
        addDividendTransaction(); // Dividends
    }

    @Override
    public String getLabel()
    {
        return "tastytrade, Inc.";
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("A[\\s]+C[\\s]+C[\\s]+O[\\s]+U[\\s]+N[\\s]+T[\\s]+"
                        + "S[\\s]+T[\\s]+A[\\s]+T[\\s]+E[\\s]+M[\\s]+E[\\s]+N[\\s]+T");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^DIVIDEND[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[A-Z][\\s]+.*$");
        type.addBlock(firstRelevantLine);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            entry.setCurrencyCode("USD");
            return entry;
        });
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            // DIVIDEND 06/28/23 M PROSHARES ULTRAPRO S&P500
            // 0.145499 29.10
            .section("date", "name", "price", "amount")
            .match("DIVIDEND[\\s]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+(?<name>[\\/\\&\\s\\w]+)[\\s]+"
                            + "(\\$)?(?<price>[\\.,\\d]+)[\\s]+(\\$)?(?<amount>[\\.,\\d]+)")
            .assign((t, v) -> {
                v.put("currency", "USD");
                t.setDateTime(asDate(v.get("date")));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
            })
            .oneOf(
                // CASH DIV ON 200 SHS WH 8.73
                section -> section
                .attributes("tax", "shares")
                .match("CASH[\\s]+DIV[\\s]+ON[\\s]+(?<shares>[\\.,\\d]+)[\\s]+SHS[\\s]+WH[\\s]+(?<tax>[\\.,\\d]+)")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares"), "en", "US"));
                    v.put("currency", "USD");
                    Money tax = Money.of("USD", asAmount(v.get("tax"), "en", "US"));
                    ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                }),

                // ULTRAPRO QQQ WH 0.38
                // CASH DIV ON 10 SHS
                section -> section
                .attributes("tax", "shares")
                .match("[\\s\\w\\d]+WH[\\s]+(?<tax>[\\.,\\d]+)")
                .match("CASH[\\s]+DIV[\\s]+ON[\\s]+(?<shares>[\\.,\\d]+)[\\s]+SHS$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares"), "en", "US"));
                    Money tax = Money.of("USD", asAmount(v.get("tax"), "en", "US"));
                    ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                })
            )

            // CUSIP: 74347X864
            .section("cusip")
            .match("^(Security Number|CUSIP):[\\s]+(?<cusip>[\\w]+)$")
            .assign((t, v) -> {
                v.put("wkn", v.get("cusip"));
                v.put("currency", "USD");
                t.setSecurity(getOrCreateSecurity(v));
            })
            .wrap(TransactionItem::new);
    }

    /**
     * This function imports a Trade Confirmation document.
     * It summarizes the trades of one day and is issued as soon as the trades are settled, i.e. T+2.
     * The Trade Confirmation document only contains trades, i.e. no general account transactions. 
     */
    private void addSummaryStatementBuySellTransaction()
    {
        // The document does not contain any sort of identifation markings such as "Trade Confirmation"
        // However, the name TASTYWORKS, INC appears on the first page.
        DocumentType type = new DocumentType("TASTYWORKS, INC");
        this.addDocumentTyp(type);

        // A block is a stock trade, if it contains a ticker symbol after the settlement date.
        // 2 B 07/17/23 07/19/23 8 VUG 290.5298000 2,324.24 0.00 0.00 0.00 D2851 2,324.24 TUA0719 6 1
        Block stockTrade = new Block("^[\\d][\\s]+.[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[,\\d]+[\\s]+[\\w]+[\\s]+[\\.,\\d]+.*");
        type.addBlock(stockTrade);
        stockTrade.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setCurrencyCode("USD");
            return entry;
        })
            .section("addlfee", "commfee", "currency", "cusip", "name", "amount", "shares",
                            "tickerSymbol", "tradedate", "tranfee", "type").optional()
            .match("^[\\d][\\s]+(?<type>.)"
                            + "[\\s]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}"
                            + "[\\s]+(?<shares>[,\\d]+)"
                            + "[\\s]+(?<tickerSymbol>[\\w]+)[\\s]+[\\.,\\d]+[\\s]+[\\.,\\d]+"
                            + "[\\s]+(?<commfee>[\\.,\\d]+)"
                            + "[\\s]+(?<tranfee>[\\.,\\d]+)"
                            + "[\\s]+(?<addlfee>[\\.,\\d]+)[\\s]+[\\w]+"
                            + "[\\s]+(?<amount>[\\.,\\d]+)[\\s]+[\\w\\d]+ [\\d] [\\d] *$")
            // Desc: VANGUARD INDEX FUNDS VANGUARD GROWTH ETF Interest/STTax: 0.00 CUSIP: 922908736
            .match("^Desc:[\\s]+(?<name>.+)[\\s]+Interest/STTax:[\\s]+[\\.,\\d]+[\\s]+CUSIP:[\\s]+(?<cusip>[\\w]+)$")
            .match("^Currency:[\\s]+(?<currency>[\\w]{3})[\\s]+ReportedPX: .*$")
            .assign((t, v) -> {
                if (v.get("type").equals("S"))
                    t.setType(PortfolioTransaction.Type.SELL);
                v.put("wkn", v.get("cusip"));
                t.setSecurity(getOrCreateSecurity(v));
                t.setDate(asDate(v.get("tradedate")));
                t.setShares(asShares(v.get("shares"), "en", "US"));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                Money fee = Money.of("USD", asAmount(v.get("commfee"), "en", "US")
                                + asAmount(v.get("tranfee"), "en", "US")
                                + asAmount(v.get("addlfee"), "en", "US"));
                checkAndSetFee(fee, t, type.getCurrentContext());
            })

            .wrap(BuySellEntryItem::new));

        // The first line of an option trade looks a bit different.
        // 2 S 03/30/23 03/31/23 1 0.5600000 56.00 1.00 0.05 0.10 U1140 54.85 TUA0331 3 1  
        Block optionTrade = new Block("^[\\d][\\s].[\\s][\\d]{2}/[\\d]{2}/[\\d]{2}[\\s][\\d]{2}/[\\d]{2}/[\\d]{2}[\\s][,\\d]+[\\s][\\.,\\d]+.*");
        type.addBlock(optionTrade);
        optionTrade.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setCurrencyCode("USD");
            return entry;
        })

            // Option Trade, subtly different from the normal stock trade
            .section("addlfee", "commfee", "amount", "shares", "tradedate", "tranfee", "type").optional()
            .match("^[\\d] (?<type>.) (?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2}) [\\d]{2}/[\\d]{2}/[\\d]{2}"
                            + " (?<shares>[,\\d]+) [\\.,\\d]+ [\\.,\\d]+"
                            + " (?<commfee>[\\.,\\d]+)"
                            + " (?<tranfee>[\\.,\\d]+)"
                            + " (?<addlfee>[\\.,\\d]+) [\\w]+"
                            + " (?<amount>[\\.,\\d]+) [\\w\\d]+ [\\d] [\\d] *$")
            .assign((t, v) -> {
                if (v.get("type").equals("S"))
                    t.setType(PortfolioTransaction.Type.SELL);
                t.setDate(asDate(v.get("tradedate")));
                t.setShares(asShares(v.get("shares"), "en", "US") * 100);
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                Money fee = Money.of("USD", asAmount(v.get("commfee"), "en", "US")
                                + asAmount(v.get("tranfee"), "en", "US")
                                + asAmount(v.get("addlfee"), "en", "US"));
                checkAndSetFee(fee, t, type.getCurrentContext());
            })

            // Desc: PUT  TQQQ   04/06/23    26 PROSHARES ULTRAPRO QQQ Interest/STTax: 0.00 CUSIP: 9LVCHS1
            .section("option", "tickerSymbol", "expiration", "strike", "underlaying", "cusip").optional()
            .match("^Desc:[\\s]+(?<option>[PUTCAL]{3,4})[\\s]+(?<tickerSymbol>[\\w]+)[\\s]+"
                            + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+"
                            + "(?<strike>[,\\.\\d]+)[\\s]+"
                            + "(?<underlaying>[/\\& \\w]+)[\\s]+Interest/STTax: [\\.,\\d]+[\\s]+"
                            + "CUSIP:[\\s]+(?<cusip>[\\w]+)$")
            .assign((t, v) -> {
                OccOsiSymbology o = new OccOsiSymbology(v.get("tickerSymbol"), v.get("expiration"),
                                v.get("option"), Double.parseDouble(v.get("strike").toString()));
                v.put("tickerSymbol", o.getOccKey());
                v.put("name", o.getName());
                v.put("wkn", v.get("cusip"));
                v.put("currency", "USD");
                t.setSecurity(getOrCreateSecurity(v));
            })

            .wrap(BuySellEntryItem::new));
    }

    /**
     * This function imports an Account Statement document.
     * This is the same as a Kontoauszug issued monthly. It contains all account-related transactions.
     * These are
     *  - trades (of cause),
     *  - expired options,
     *  - dividends,
     *  - interest,
     *  - deposits and withdrawals,
     *  - expired options,
     *  - options’ assignment and so on.
     */
    private void addDepotStatementTransaction()
    {
        DocumentType type = new DocumentType("A[\\s]+C[\\s]+C[\\s]+O[\\s]+U[\\s]+N[\\s]+T[\\s]+"
                        + "S[\\s]+T[\\s]+A[\\s]+T[\\s]+E[\\s]+M[\\s]+E[\\s]+N[\\s]+T");
        this.addDocumentTyp(type);

        // Buy and Sell Transactions
        Block buySell = new Block("(BOUGHT|SOLD)[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[A-Z][\\s]+.*");
        type.addBlock(buySell);
        buySell.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setCurrencyCode("USD");
            return entry;
        })
        .oneOf(
            // Option Trade
            //    BOUGHT 05/23/23 M PUT  TQQQ   06/16/23    27 1 0.51 51.13
            //    PROSHARES ULTRAPRO QQQ
            //    UNSOLICITED
            //    CLOSING CONTRACT
            //    Security Number: 9NJWKB0
            section -> section
            .attributes("type", "tradedate", "option", "underlaying", "expiration", "strike", "shares",
                                        "price", "amount", "cusip")
            .match("(?<type>(BOUGHT|SOLD))[\\s]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+"
                            + "(?<option>(CALL|PUT))[\\s]+" + "(?<underlaying>[a-zA-Z0-9]+)[\\s]+"
                            + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+"
                            + "(?<strike>[,\\.\\d]+)[\\s]+" + "(?<shares>[\\.,\\d]+)[\\s]+"
                            + "(\\$)?(?<price>[\\.,\\d]+)[\\s]+" + "(\\$)?(?<amount>[\\.,\\d]+)[\\s]*$")
            .match("Security Number:[\\s]+(?<cusip>[A-Z0-9]+)")
            .assign((t, v) -> {
                t.setNote(v.get("option") + " " + v.get("underlaying") + " " + v.get("expiration") + " " + v.get("strike"));
                if (v.get("type").equals("SOLD"))
                    t.setType(PortfolioTransaction.Type.SELL);
                OccOsiSymbology o = new OccOsiSymbology(v.get("underlaying"), v.get("expiration"),
                                v.get("option"), Double.parseDouble(v.get("strike").toString()));
                v.put("tickerSymbol", o.getOccKey());
                v.put("name", o.getName());
                v.put("wkn", v.get("cusip"));
                v.put("currency", "USD");
                v.put("shares", v.get("shares") + "00");
                t.setSecurity(getOrCreateSecurity(v));
                t.setDate(asDate(v.get("tradedate")));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                t.setShares(asShares(v.get("shares"), "en", "US"));
                checkAndSetFee(calculateFee(v), t, type.getCurrentContext());
                // save to determine type of expiration (BTC or STC), if applicable.
                m_optionTradesInImport.add(t);
            }),
            
            // Stock Split
            //    BOUGHT 01/18/22 C PROSHARES TRUST 30  
            //    ULTRAPRO QQQ
            //    PRODUCT DESCRIPTION UNDER
            //    SEPARATE COVER
            //    STK SPLIT ON      30 SHS
            //    REC 01/11/22 PAY 01/12/22
            //    CUSIP: 74347X831
            section -> section
            .attributes("type", "tradedate", "name", "shares", "description", "cusip")
            .match("(?<type>(BOUGHT|SOLD))[\\s]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+"
                            + "(?<name>[\\/\\& \\w]+)[\\s]+"
                            + "(?<shares>[\\.,\\d]+)[\\s]+$")
            .match("STK[\\s]+SPLIT[\\s]+ON.*")
            .match("(?<description>REC[\\/\\& \\w]+)")
            .match("CUSIP:[\\s]+(?<cusip>[A-Z0-9]+)")
            .assign((t, v) -> {
                v.put("price", "0.0");
                v.put("amount", "0.0");
                v.put("wkn", v.get("cusip"));
                v.put("currency", "USD");
                t.setShares(asShares(v.get("shares"), "en", "US"));
                t.setSecurity(getOrCreateSecurity(v));
                if (v.get("type").equals("SOLD"))
                    t.setType(PortfolioTransaction.Type.SELL);
                t.setNote("Splitbuy: " + v.get("description"));
                t.setDate(asDate(v.get("tradedate")));
            }),

            // Regular Stock Transaction
            //    BOUGHT 01/20/22 C PROSHARES TRUST 6 68.6499 411.90
            //    ULTRAPRO QQQ
            //    UNSOLICITED
            //    PRODUCT DESCRIPTION UNDER
            //    SEPARATE COVER
            //    CUSIP: 74347X831
            section -> section.optional()
            .attributes("type", "tradedate", "name", "shares", "price", "amount", "cusip")
            .match("(?<type>(BOUGHT|SOLD))[\\s]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+"
                            + "(?<name>[\\/\\& \\w]+)[\\s]+"
                            + "(?<shares>[\\.,\\d]+)[\\s]+"
                            + "(\\$)?(?<price>[\\.,\\d]+)[\\s]+"
                            + "(\\$)?(?<amount>[\\.,\\d]+)[\\s]*$")
            .match("^CUSIP:[\\s]+(?<cusip>[A-Z0-9]+)")
            .assign((t, v) -> {
                t.setNote("");
                if (v.get("type").equals("SOLD"))
                    t.setType(PortfolioTransaction.Type.SELL);
                v.put("wkn", v.get("cusip"));
                t.setCurrencyCode("USD");
                v.put("currency", "USD");
                t.setSecurity(getOrCreateSecurity(v));
                t.setDate(asDate(v.get("tradedate")));
                t.setShares(asShares(v.get("shares"), "en", "US"));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                checkAndSetFee(calculateFee(v), t, type.getCurrentContext());
            }))

            .wrap(BuySellEntryItem::new));

        // Expired Option
        //    EXPIRED 04/06/23 M PUT  TQQQ   04/06/23    27 1  
        //    PROSHARES ULTRAPRO QQQ
        //    OPTION EXPIRATION - EXPIRED
        //    Security Number: 9LVCJB6
        // Option Assignment
        //    ASG 06/09/23 C CALL UPRO   06/09/23    40 1  
        //    PROSHARES ULTRAPRO S&P 500
        //    A/E 9SMCXK2 1 ASSIGNED
        //    Security Number: 9SMCXK2
        Block optionExpiration = new Block("^(EXPIRED|ASG).*$");
        type.addBlock(optionExpiration);
        optionExpiration.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setMonetaryAmount(Money.of("USD", 0));
            return entry;
        })        
            .section("date", "type",  "otype", "underlaying", "expiration", "strike", "quantity", "description", "cusip")
            .match("^(?<type>(EXPIRED|ASG))[\\s]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+(?<otype>[PUTCAL]{3,4})[\\s]+"
                            + "(?<underlaying>[/\\&\\w]+)[\\s]+"
                            + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+"
                            + "(?<strike>[,\\.\\d]+)[\\s]+"
                            + "(?<quantity>[\\-]?[\\d]+)[\\s]*.*$")
            .match("^(?<description>.*).*$")
            .match("(^OPTION EXPIRATION.*$|^.*[\\s]+[A-Z0-9]+[\\s]+[\\d]+[\\s]+ASSIGNED.*)")
            .match("Security Number:[\\s]+(?<cusip>[A-Z0-9]+)")
            .assign((t, v) -> {                v.put("currency", "USD");
                v.put("wkn", v.get("cusip"));
                t.setDate(asDate(v.get("date")));
                OccOsiSymbology o = new OccOsiSymbology(v.get("underlaying"),
                                v.get("expiration"), v.get("type"),
                                Double.parseDouble(v.get("strike").toString()));
                v.put("tickerSymbol", o.getOccKey());
                v.put("name", o.getName());
                t.setShares(asShares(v.get("quantity").replace("-", ""), "en", "US") * 100);
                t.setSecurity(getOrCreateSecurity(v));
                if (v.get("type").equals("EXPIRED")) {
                    setTypeAndExpirationNote(t, v); // TODO
                } else {
                    t.setNote("Assigned: Buy To Close");
                    t.setType(PortfolioTransaction.Type.BUY); // Assignment means this was a short position
                }
            })
            
            .wrap(BuySellEntryItem::new));

        // INTEREST 01/18/22 C INTEREST ON CREDIT BALANCE $0.03
        // AT  0.010% 01/01 THRU 01/15
        Block interestBlock = new Block( "^INTEREST[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[A-Z][\\s]+"
                        + "INTEREST[\\s]+ON[\\s]+CREDIT[\\s]+BALANCE[\\s]+(\\$)?[\\.,\\d]+.*$");
        type.addBlock(interestBlock);
        Transaction<AccountTransaction> interestTransaction = new Transaction<>();
        interestTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        }).wrap(TransactionItem::new);
        addInterestsSectionTransaction(interestTransaction, type);
        interestBlock.set(interestTransaction);
        
        // DE BI T  I N T ER EST
        // INTEREST 04/18/22 C FROM 03/16 THRU 04/15 @ 8    % $0.67
        // BAL   19,619-  AVBAL       97
        Block debitInterestBlock = new Block("D[\\s]?E[\\s]?B[\\s]?I[\\s]?T[\\s]+I[\\s]?N[\\s]?T[\\s]?E[\\s]?R[\\s]?E[\\s]?S[\\s]?T");
        type.addBlock(debitInterestBlock);
        
        Transaction<AccountTransaction> debitInterestTransaction = new Transaction<>();
        debitInterestTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return entry;
        }).wrap(TransactionItem::new);
        addInterestsSectionTransaction(debitInterestTransaction, type);
        debitInterestBlock.set(debitInterestTransaction);
        
        // FUNDS PAID AND RECEIVED
        // WIRE 04/28/22 C Wire Funds Received $3,114.90
        Block depositBlock = new Block("^WIRE[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[A-Z][\\s]+.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            entry.setMonetaryAmount(Money.of("USD", 0));
            entry.setCurrencyCode("USD");
            return entry;
        })
            // WIRE 03/21/20 C Wire Funds Received $1,234.56
            // FedRef 20220428MMQFMPEC001925
            // SEN(20200321028392)
            .section("date", "description", "amount", "fedref", "sen").optional()
            .match("WIRE[\\s]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]"
                            + "+(?<description>[\\/\\& \\w]+)[\\s]+(\\$)?(?<amount>[\\.,\\d]+)")
            .match("(?<fedref>FedRef.*)")
            .match("(?<sen>SEN.*)")
            .assign((t, v) -> {
                t.setDateTime(asDate(v.get("date")));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                t.setNote(v.get("description") + " " + v.get("fedref") + " " + v.get("sen"));
            })
            
            .wrap(TransactionItem::new));

        // CSG 09/11/22 M Journal to account 1AB28390 $1,000.00
        // CSG 09/11/22 M Journal from account 1AB28390 $1,000.00
        // SEN(20220911023817)                        
        Block journalBlock = new Block("^CSG[\\s]+[\\d]{2}/[\\d]{2}/[\\d]{2}[\\s]+[A-Z][\\s]+.*$");
        type.addBlock(journalBlock);
        journalBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setMonetaryAmount(Money.of("USD", 0));
            entry.setCurrencyCode("USD");
            return entry;
        })
            .section("date", "type", "account", "amount", "sen")
            .match("^CSG[\\s]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+"
                            + "Journal (?<type>\\w+) account[\\s]+"
                            + "(?<account>.*)[\\s]+(\\$)?(?<amount>[\\.,\\d]+)")
    
            .match("(?<sen>SEN.*)")
            .assign((t, v) -> {
                if (v.get("type").equals("from"))
                    t.setType(AccountTransaction.Type.DEPOSIT);
                else if (v.get("type").equals("to"))
                    t.setType(AccountTransaction.Type.REMOVAL); 
                t.setDateTime(asDate(v.get("date")));
                t.setAmount(asAmount(v.get("amount"), "en", "US"));
                t.setNote("Journal " + v.get("type") + " account " + v.get("account") + " " + v.get("sen"));
            })
            
            .wrap(TransactionItem::new));
    }
    
    private Money calculateFee(final ParsedData v)
    {
      BigDecimal amount = ExtractorUtils.convertToNumberBigDecimal(v.get("amount"), Values.Amount, "en", "US");
      BigDecimal shares = ExtractorUtils.convertToNumberBigDecimal(v.get("shares"), Values.Amount, "en", "US");
      BigDecimal price = ExtractorUtils.convertToNumberBigDecimal(v.get("price"), Values.Amount, "en", "US");
      BigDecimal gross = price.multiply(shares);
      BigDecimal fee = v.get("type").equals("SOLD") ? gross.subtract(amount) : amount.subtract(gross);
      return Money.of("USD", asAmount(fee.toString(), "en", "US"));
    }
    
    private <T extends Transaction<?>> void addInterestsSectionTransaction(T transaction, DocumentType type)
    {
        transaction
            .section("date", "amount", "description", "details")
            .match("^INTEREST[\\s]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[\\s]+[A-Z][\\s]+(?<description>.*)[\\s]+(\\$)?(?<amount>[\\.,\\d]+).*$")
            .match("^(?<details>.*$)")
            .assign((t, v) -> {
                ((name.abuchen.portfolio.model.Transaction) t).setDateTime(asDate(v.get("date")));
                ((name.abuchen.portfolio.model.Transaction) t).setMonetaryAmount(Money.of("USD", asAmount(v.get("amount"), "en", "US")));
                ((name.abuchen.portfolio.model.Transaction) t).setNote(v.get("description") + " " + v.get("details"));
            });
    }
    
    private void setTypeAndExpirationNote(BuySellEntry t, ParsedData v)
    {        BigDecimal quantity = ExtractorUtils.convertToNumberBigDecimal(v.get("quantity"), Values.Amount, "en", "US");

        if (quantity.signum() == 1)
        {
            t.setType(PortfolioTransaction.Type.BUY);
            t.setNote("Expired: Buy To Close");
        }
        else if (quantity.signum() == -1)
        {
            t.setType(PortfolioTransaction.Type.SELL);
            t.setNote("Expired: Sell To Close");
        }
        else
        {
            throw new IllegalArgumentException(quantity.toString());
        }
    }

    /**
     * Convert date using US locale.
     * 
     * @param dateString
     *            date with pattern M/d/yy
     * @return converted date
     */
    private LocalDateTime asDate(String dateString)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy").withLocale(Locale.US);
        return LocalDate.parse(dateString, formatter).atStartOfDay();
    }
}
