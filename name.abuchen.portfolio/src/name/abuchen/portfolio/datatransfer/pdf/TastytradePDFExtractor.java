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

import name.abuchen.portfolio.util.OccOsiSymbology;
import name.abuchen.portfolio.datatransfer.DocumentContext;
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

public class TastytradePDFExtractor extends AbstractPDFExtractor
{
    private List<BuySellEntry> m_optionTradesInImport =  new ArrayList<>();

    public TastytradePDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("TASTYWORKS");
        addBankIdentifier("tastytrade");
        addBankIdentifier("Apex Clearing Corporation");

        addTransactionConfirmation();
        addAccountStatement();
        addDividendTransaction();
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("A[ ]+C[ ]+C[ ]+O[ ]+U[ ]+N[ ]+T[ ]+S[ ]+T[ ]+A[ ]+T[ ]+E[ ]+M[ ]+E[ ]+N[ ]+T");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^DIVIDEND[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[A-Z][ ]+.*$");
        type.addBlock(firstRelevantLine);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            entry.setDateTime(asDate("01/01/99"));
            entry.setMonetaryAmount(Money.of("USD", 0));
            entry.setCurrencyCode("USD");
            return entry;
        });
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
        .section("tradedate", "name", "price", "netamount")
        .match("DIVIDEND[ ]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+(?<name>[\\/\\& \\w]+)[ ]+"
                        + "(\\$)?(?<price>[\\.,\\d]+)[ ]+(\\$)?(?<netamount>[\\.,\\d]+)")
        .assign((t, v) -> {
            v.put("currency", "USD");
            t.setDateTime(asDate(v.get("tradedate")));
            t.setMonetaryAmount(Money.of("USD", asAmount(v.get("netamount"))));
        })
        .oneOf(
                        section -> section
                        .attributes("tax", "shares")
                        .match("CASH[ ]+DIV[ ]+ON[ ]+(?<shares>[\\.,\\d]+)[ ]+SHS[ ]+WH[ ]+(?<tax>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.setShares(this.asShares(v.get("shares")));
                            Money tax = Money.of("USD", asAmount(v.get("tax")));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                        }),
                        section -> section
                        .attributes("tax", "shares")
                        .match("[ \\w\\d]+WH[ ]+(?<tax>[\\.,\\d]+)")
                        .match("CASH[ ]+DIV[ ]+ON[ ]+(?<shares>[\\.,\\d]+)[ ]+SHS$")
                        .assign((t, v) -> {
                            t.setShares(this.asShares(v.get("shares")));
                            Money tax = Money.of("USD", asAmount(v.get("tax")));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                        })
                        )
        .section("cusip")
        .match("^(Security Number|CUSIP):[ ]+(?<cusip>[\\w]+)$")
        .assign((t, v) -> {
            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
            v.put("currency", "USD");
            t.setSecurity(getOrCreateSecurity(v));
        })
        .conclude(ExtractorUtils.fixGrossValueA())
        .wrap(TransactionItem::new);
    }

    @Override
    public String getLabel()
    {
        return "tastytrade, Inc.";
    }

    private void addTransactionConfirmation()
    {
        DocumentType type = new DocumentType("TASTYWORKS, INC");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setDate(asDate("01/01/99"));
            entry.setMonetaryAmount(Money.of("USD", 0));
            return entry;
        });

        Block stockTrade = new Block("^[\\d][ ]+[B|S][ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}.*$");
        type.addBlock(stockTrade);
        stockTrade.set(pdfTransaction);
        pdfTransaction
        .section("addlfee", "commfee", "currency", "cusip", "intereststtax", "name", "netamount", "price",
                        "principal", "shares", "tag", "tickerSymbol", "tradedate", "tranfee", "type").optional()
        .match("^[\\d][ ]+(?<type>.)[ ]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}"
                        + "[ ]+(?<shares>[,\\d]+)"
                        + "[ ]+(?<tickerSymbol>[\\w]+)"
                        + "[ ]+(?<price>[\\.,\\d]+)"
                        + "[ ]+(?<principal>[\\.,\\d]+)"
                        + "[ ]+(?<commfee>[\\.,\\d]+)"
                        + "[ ]+(?<tranfee>[\\.,\\d]+)"
                        + "[ ]+(?<addlfee>[\\.,\\d]+)"
                        + "[ ]+(?<tag>[\\w]+)"
                        + "[ ]+(?<netamount>[\\.,\\d]+)"
                        + "[ ]+[\\w\\d]+ [\\d] [\\d] *$")
        .match("^Desc:[ ]+(?<name>.+)[ ]+Interest/STTax:[ ]+(?<intereststtax>[\\.,\\d]+)[ ]+CUSIP:[ ]+(?<cusip>[\\w]+)$")
        .match("^Currency:[ ]+(?<currency>[\\w]{3})[ ]+ReportedPX: .*$")
        .assign((t, v) -> {
            // "S" means SELL
            if (v.get("type").equals("S"))
                t.setType(PortfolioTransaction.Type.SELL);
            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
            t.setSecurity(getOrCreateSecurity(v));
            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
            t.setDate(asDate(v.get("tradedate")));
            t.setShares(this.asShares(v.get("shares")));
            t.setAmount(asAmount(v.get("netamount")));
            t.setMonetaryAmount(Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("netamount"))));
            Money commfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("commfee")));
            Money tranfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tranfee")));
            Money addlfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("addlfee")));
            checkAndSetFee(commfee.add(tranfee).add(addlfee), t, type.getCurrentContext());
        })

        // Option Trade
        .section("addlfee", "commfee", "currency", "cusip", "expiration", "intereststtax", "netamount", "option",
                        "price", "principal", "shares", "strike", "tag", "tickerSymbol", "tradedate", "tranfee",
                        "type", "underlaying").optional() 
        .match("^[\\d] (?<type>.) (?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})"
                        + " [\\d]{2}/[\\d]{2}/[\\d]{2}"
                        + " (?<shares>[,\\d]+)"
                        + " (?<price>[\\.,\\d]+)"
                        + " (?<principal>[\\.,\\d]+)"
                        + " (?<commfee>[\\.,\\d]+)"
                        + " (?<tranfee>[\\.,\\d]+)"
                        + " (?<addlfee>[\\.,\\d]+)"
                        + " (?<tag>[\\w]+)"
                        + " (?<netamount>[\\.,\\d]+)"
                        + " [\\w\\d]+ [\\d] [\\d] *$")
        .match("^Desc:[ ]+(?<option>[PUTCAL]{3,4})[ ]+(?<tickerSymbol>[\\w]+)[ ]+"
                        + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+"
                        + "(?<strike>[,\\.\\d]+)[ ]+"
                        + "(?<underlaying>[/\\& \\w]+)[ ]+"
                        + "Interest/STTax: (?<intereststtax>[\\.,\\d]+)[ ]+"
                        + "CUSIP:[ ]+(?<cusip>[\\w]+)$")
        .match("^Currency:[ ]+(?<currency>[\\w]{3})[ ]+ReportedPX: .*$")
        .assign((t, v) -> {
            // "S" means SELL
            if (v.get("type").equals("S"))
                t.setType(PortfolioTransaction.Type.SELL);

            if (v.get("option") != null) {
                OccOsiSymbology o = new OccOsiSymbology(v.get("tickerSymbol"), v.get("expiration"), v.get("option"), Double.parseDouble(v.get("strike").toString()));
                v.put("tickerSymbol", o.getOccKey());
                v.put("name", o.getName());
            }
            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
            t.setSecurity(getOrCreateSecurity(v));
            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
            t.setDate(asDate(v.get("tradedate")));
            t.setShares(this.asSharesFromOption(v.get("shares"))); // one contract equals 100 shares
            t.setAmount(asAmount(v.get("netamount")));
            t.setMonetaryAmount(Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("netamount"))));
            Money commfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("commfee")));
            Money tranfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tranfee")));
            Money addlfee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("addlfee")));
            checkAndSetFee(commfee.add(tranfee).add(addlfee), t, type.getCurrentContext());
        })
        .wrap(BuySellEntryItem::new);
    }


    private void addAccountStatement()
    {
        DocumentType type = new DocumentType("A[ ]+C[ ]+C[ ]+O[ ]+U[ ]+N[ ]+T[ ]+S[ ]+T[ ]+A[ ]+T[ ]+E[ ]+M[ ]+E[ ]+N[ ]+T");
        this.addDocumentTyp(type);

        Block buySell = new Block("(BOUGHT|SOLD)[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[A-Z][ ]+.*");
        type.addBlock(buySell);
        buySell.set(new Transaction<BuySellEntry>()
                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            entry.setDate(asDate("01/01/99"));
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            entry.setCurrencyCode("USD");
                            return entry;
                        })
                        .oneOf(
                                        section -> section
                                        .attributes("type", "tradedate", "option", "underlaying", "expiration", "strike",
                                                        "contracts", "price", "netamount", "cusip")
                                        .match("(?<type>(BOUGHT|SOLD))[ ]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+"
                                                        + "(?<option>(CALL|PUT))[ ]+"
                                                        + "(?<underlaying>[a-zA-Z0-9]+)[ ]+"
                                                        + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+"
                                                        + "(?<strike>[,\\.\\d]+)[ ]+"
                                                        + "(?<contracts>[\\.,\\d]+)[ ]+"
                                                        + "(\\$)?(?<price>[\\.,\\d]+)[ ]+"
                                                        + "(\\$)?(?<netamount>[\\.,\\d]+)[ ]*$")
                                        .match("Security Number:[ ]+(?<cusip>[A-Z0-9]+)")
                                        .assign((t, v) -> {
                                            t.setNote(v.get("option") + " " + v.get("underlaying") + " " + v.get("expiration") + " " + v.get("strike"));
                                            if (v.get("type").equals("SOLD"))
                                                t.setType(PortfolioTransaction.Type.SELL);
                                            if (v.get("option") != null) {
                                                OccOsiSymbology o = new OccOsiSymbology(v.get("underlaying"), v.get("expiration"),
                                                                v.get("option"), Double.parseDouble(v.get("strike").toString()));
                                                v.put("tickerSymbol", o.getOccKey());
                                                v.put("name", o.getName());
                                            }
                                            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
                                            v.put("currency", "USD");
                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDate(asDate(v.get("tradedate")));
                                            NumberFormat nf = NumberFormat.getInstance(Locale.US);
                                            BigDecimal netamount = null, price = null, shares = null;
                                            try
                                            {
                                                netamount = new BigDecimal(nf.parse(v.get("netamount")).toString());
                                                price = new BigDecimal(nf.parse(v.get("price")).toString());                    
                                                shares = new BigDecimal(nf.parse(v.get("contracts")).toString()).multiply(new BigDecimal(100.));
                                            }
                                            catch (ParseException e)
                                            {
                                                e.printStackTrace();
                                            }
                                            BigDecimal gross = price.multiply(shares);
                                            BigDecimal fee = v.get("type").equals("SOLD") ? gross.subtract(netamount) : netamount.subtract(gross);                    
                                            t.setMonetaryAmount(Money.of("USD", asAmount(v.get("netamount"))));
                                            t.setShares(this.asSharesFromOption(v.get("contracts")));                      
                                            checkAndSetFee(Money.of("USD", asAmount(fee.toString())), t, type.getCurrentContext());
                                            // save to determine type of expiration (BTC or STC), if applicable.
                                            m_optionTradesInImport.add(t);
                                        })
                                        ,
                                        section -> section
                                        .attributes("type", "tradedate", "name", "shares", "description", "cusip")
                                        .match("(?<type>(BOUGHT|SOLD))[ ]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+"
                                                        + "(?<name>[\\/\\& \\w]+)[ ]+"
                                                        + "(?<shares>[\\.,\\d]+)[ ]+$")
                                        .match("STK[ ]+SPLIT[ ]+ON.*")
                                        .match("(?<description>REC[\\/\\& \\w]+)")
                                        .match("CUSIP:[ ]+(?<cusip>[A-Z0-9]+)")
                                        .assign((t, v) -> {
                                            v.put("price", "0.0");
                                            v.put("netamount", "0.0");
                                            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
                                            v.put("currency", "USD");
                                            t.setShares(this.asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
                                            if (v.get("type").equals("SOLD"))
                                                t.setType(PortfolioTransaction.Type.SELL);
                                            t.setNote("Splitbuy: " + v.get("description"));
                                            t.setDate(asDate(v.get("tradedate")));
                                        })
                                        ,
                                        section -> section.optional()
                                        .attributes("type", "tradedate", "name", "shares", "price", "netamount", "cusip")
                                        .match("(?<type>(BOUGHT|SOLD))[ ]+(?<tradedate>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+"
                                                        + "(?<name>[\\/\\& \\w]+)[ ]+"
                                                        + "(?<shares>[\\.,\\d]+)[ ]+"
                                                        + "(\\$)?(?<price>[\\.,\\d]+)[ ]+"
                                                        + "(\\$)?(?<netamount>[\\.,\\d]+)[ ]*$")
                                        .match("^CUSIP:[ ]+(?<cusip>[A-Z0-9]+)")
                                        .assign((t, v) -> {
                                            setBuySellEntry(t, v, type.getCurrentContext());
                                        })
                                        )
                        .wrap(BuySellEntryItem::new));

        // Expired Option Contract
        Block optionExpiration = new Block("^(EXPIRED|ASG).*$");
        type.addBlock(optionExpiration);
        optionExpiration.set(new Transaction<BuySellEntry>()
                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            entry.setDate(asDate("01/01/99"));
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            entry.setCurrencyCode("USD");
                            return entry;
                        })
                        .oneOf(
                                        section -> section
                                        .attributes("date", "option", "underlaying", "expiration", "strike", "quantity", "description", "cusip")
                                        .match("^EXPIRED[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+(?<option>[PUTCAL]{3,4})[ ]+"
                                                        + "(?<underlaying>[/\\&\\w]+)[ ]+"
                                                        + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+"
                                                        + "(?<strike>[,\\.\\d]+)[ ]+"
                                                        + "(?<quantity>[\\-]?[\\d]+)[ ]*.*$")
                                        .match("^(?<description>.*)$")
                                        .match("^OPTION EXPIRATION.*$")
                                        .match("Security Number:[ ]+(?<cusip>[A-Z0-9]+)")
                                        .assign((t, v) -> {
                                            v.put("currency", "USD");
                                            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
                                            t.setDate(asDate(v.get("date")));
                                            if (v.get("option") != null) {
                                                OccOsiSymbology o = new OccOsiSymbology(v.get("underlaying"), v.get("expiration"), v.get("option"), Double.parseDouble(v.get("strike").toString()));
                                                v.put("tickerSymbol", o.getOccKey());
                                                v.put("name", o.getName());
                                            }
                                            t.setShares(this.asSharesAbsFromOption(v.get("quantity")));
                                            t.setNote("Expired: ");
                                            t.setSecurity(getOrCreateSecurity(v));
                                            setTypeAndNote(t, v);
                                        }),
                                        section -> section
                                        .attributes("date", "option", "underlaying", "expiration", "strike", "quantity", "description", "cusip")
                                        .match("^ASG[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+(?<option>[PUTCAL]{3,4})[ ]+"
                                                        + "(?<underlaying>[/\\&\\w]+)[ ]+"
                                                        + "(?<expiration>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+"
                                                        + "(?<strike>[,\\.\\d]+)[ ]+"
                                                        + "(?<quantity>[\\d]+)[ ]*.*$")
                                        .match("^(?<description>.*).*$")
                                        .match("^.*[ ]+[A-Z0-9]+[ ]+[\\d]+[ ]+ASSIGNED.*")
                                        .match("Security Number:[ ]+(?<cusip>[A-Z0-9]+)")
                                        .assign((t, v) -> {
                                            v.put("currency", "USD");
                                            v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
                                            t.setDate(asDate(v.get("date")));
                                            if (v.get("option") != null) {
                                                OccOsiSymbology o = new OccOsiSymbology(v.get("underlaying"), v.get("expiration"), v.get("option"), Double.parseDouble(v.get("strike").toString()));
                                                v.put("tickerSymbol", o.getOccKey());
                                                v.put("name", o.getName());
                                            }
                                            t.setShares(this.asSharesAbsFromOption(v.get("quantity")));
                                            t.setNote("Assigned: ");
                                            t.setSecurity(getOrCreateSecurity(v));
                                            // Assignment means this was a short position
                                            t.setType(PortfolioTransaction.Type.BUY);
                                            t.setNote(t.getNote() + " Buy To Close");
                                        })
                                        )
                        .wrap(BuySellEntryItem::new));

        // DIVIDENDS AND INTEREST
        Block interestBlock = new Block("^INTEREST[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[A-Z][ ]+INTEREST[ ]+ON[ ]+CREDIT[ ]+BALANCE[ ]+(\\$)?[\\.,\\d]+.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST);
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            return entry;
                        })
                        .section("date", "interest", "description").optional()
                        .match("^INTEREST[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+[ A-Z]*(\\$)?(?<interest>[\\.,\\d]+).*$")
                        .match("^AT[ ]+(?<description>.*$)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setMonetaryAmount(Money.of("USD", asAmount(v.get("interest"))));
                            t.setNote(v.get("description"));
                        })
                        .wrap(TransactionItem::new));

        Block debitInterestBlock = new Block("D[ ]?E[ ]?B[ ]?I[ ]?T[ ]+I[ ]?N[ ]?T[ ]?E[ ]?R[ ]?E[ ]?S[ ]?T");
        type.addBlock(debitInterestBlock);
        debitInterestBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            return entry;
                        })
                        .section("date", "interest", "description", "details").optional()
                        .match("^INTEREST[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+(?<description>.*)[ ]+(\\$)?(?<interest>[\\.,\\d]+).*$")
                        .match("^(?<details>.*$)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setMonetaryAmount(Money.of("USD", asAmount(v.get("interest"))));
                            t.setNote(v.get("description") + " " + v.get("details"));
                        })
                        .wrap(TransactionItem::new));

        // FUNDS PAID AND RECEIVED
        Block depositBlock = new Block("^WIRE[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[A-Z][ ]+.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            entry.setCurrencyCode("USD");
                            return entry;
                        })

                        .section("date", "description", "amount", "fedref", "sen").optional()
                        .match("WIRE[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+(?<description>[\\/\\& \\w]+)[ ]+(\\$)?(?<amount>[\\.,\\d]+)")
                        .match("(?<fedref>FedRef.*)")
                        .match("(?<sen>SEN.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("description") + " " + v.get("fedref") + " " + v.get("sen"));
                        })

                        .wrap(TransactionItem::new));

        Block journalBlock = new Block("^CSG[ ]+[\\d]{2}/[\\d]{2}/[\\d]{2}[ ]+[A-Z][ ]+.*$");
        type.addBlock(journalBlock);
        journalBlock.set(new Transaction<AccountTransaction>()
                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setMonetaryAmount(Money.of("USD", 0));
                            entry.setCurrencyCode("USD");
                            return entry;
                        })
                        .oneOf(
                                        section -> section
                                        .attributes("date", "account", "amount", "sen")
                                        .match("^CSG[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+Journal from account[ ]+"
                                                        + "(?<account>.*)[ ]+(\\$)?(?<amount>[\\.,\\d]+)")
                                        .match("(?<sen>SEN.*)")
                                        .assign((t, v) -> {
                                            t.setType(AccountTransaction.Type.DEPOSIT);
                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setNote("Journal from account " + v.get("account") + " " + v.get("sen"));
                                        }),
                                        section -> section
                                        .attributes("date", "account", "amount", "sen")
                                        .match("^CSG[ ]+(?<date>[\\d]{2}/[\\d]{2}/[\\d]{2})[ ]+[A-Z][ ]+Journal to account[ ]+"
                                                        + "(?<account>.*)[ ]+(\\$)?(?<amount>[\\.,\\d]+)")
                                        .match("(?<sen>SEN.*)")
                                        .assign((t, v) -> {
                                            t.setType(AccountTransaction.Type.REMOVAL);
                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setNote("Journal to account " + v.get("account") + " " + v.get("sen"));
                                        })
                                        )
                        .wrap(TransactionItem::new));
    }


    private void setTypeAndNote(BuySellEntry t, ParsedData v)
    {
        BigDecimal quantity = null;
        try
        {
            quantity = new BigDecimal(NumberFormat.getInstance(Locale.US).parse(v.get("quantity")).toString());
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        if (quantity.signum() == 1) {
            t.setType(PortfolioTransaction.Type.BUY);
            t.setNote(t.getNote() + " Buy To Close");
        } else if (quantity.signum() == -1) {
            t.setType(PortfolioTransaction.Type.SELL);
            t.setNote(t.getNote() + " Sell To Close");
        } else {
            throw new IllegalArgumentException(quantity.toString());
        }
    }

    private void setBuySellEntry(BuySellEntry t, ParsedData v, DocumentContext ctx)
    {
        t.setNote("");
        if (v.get("type").equals("SOLD"))
            t.setType(PortfolioTransaction.Type.SELL);
        v.put("wkn", v.get("cusip")); // use the CUSIP for identification in WKN field
        t.setCurrencyCode("USD");
        v.put("currency", "USD");
        t.setSecurity(getOrCreateSecurity(v));
        t.setDate(asDate(v.get("tradedate")));
        t.setShares(this.asShares(v.get("shares")));
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        BigDecimal netamount = null, price = null, shares = null;
        try
        {
            netamount = new BigDecimal(nf.parse(v.get("netamount")).toString());
            price = new BigDecimal(nf.parse(v.get("price")).toString());                    
            shares = new BigDecimal(nf.parse(v.get("shares")).toString());
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        BigDecimal gross = price.multiply(shares);
        BigDecimal fee = v.get("type").equals("SOLD") ? gross.subtract(netamount) : netamount.subtract(gross);                    
        t.setAmount(asAmount(v.get("netamount")));
        t.setMonetaryAmount(Money.of("USD", asAmount(v.get("netamount"))));
        t.setShares(this.asShares(v.get("shares")));
        checkAndSetFee(Money.of("USD", asAmount(fee.toString())), t, ctx);
    }

    /**
     * Convert date using US locale.
     * 
     * @param dateString date with pattern M/d/yy
     * @return converted date
     */
    private LocalDateTime asDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy").withLocale(Locale.US);
        return LocalDate.parse(dateString , formatter ).atStartOfDay();
    }

    @Override
    protected long asAmount(String value)
    {
        try
        {
            return Math.abs(Math.round(NumberFormat.getInstance(Locale.US).parse(value).doubleValue() * Values.Amount.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Absolute value of shares.
     * @param value
     * @return
     */
    protected long asSharesAbs(String value)
    {
        try
        {
            return Math.round(NumberFormat.getInstance(Locale.US).parse(value.replace("-", "")).doubleValue() * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    protected long asSharesAbsFromOption(String value)
    {
        try
        {
            return Math.round(NumberFormat.getInstance(Locale.US).parse(value.replace("-", "")).doubleValue() * 100. * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    protected long asSharesFromOption(String value)
    {
        try
        {
            return Math.round(NumberFormat.getInstance(Locale.US).parse(value).doubleValue() * 100. * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    protected long asShares(String value)
    {
        try
        {
            return Math.round(NumberFormat.getInstance(Locale.US).parse(value).doubleValue() * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}