package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class PostfinancePDFExtractor extends SwissBasedPDFExtractor
{

    public PostfinancePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("PostFinance");

        addBuyTransaction();
        addSellTransaction();
        addDividendsTransaction();
        addCapitalGainTransaction();
        addFeeTransaction();
        addAccountTransferTransaction();
    }
    
    @SuppressWarnings("nls")
    /***
     * Postfinance offers three accounts with different currencies (CHF, EUR, USD)
     * There are two possibilities to buy shares of foreign currencies:
     * - Transfer money from CHF account to EUR/USD account and buy it in foreign currency
     * - Buy EUR/USD shares from CHF account directly (acutal exchange rate will be taken) 
     */
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Kauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })
                        
                        .section("name", "isin", "shares", "currency", "transactionCurrency", "amount")
                        .find("Titel Ort der Ausführung")
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .match("^Zu Ihren Lasten (?<transactionCurrency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("transactionCurrency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section("feecurrency", "fee").optional()
                        .match("^Kommission (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })
                        
                        .section("taxcurrency", "tax").optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })
                        
                        .section("stockfeecurrency", "stockfee").optional()
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")), asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })
                        
                        // buy shares directly from CHF account
                        .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount").optional()
                        .match("^Total (?<forexCurrency>\\w{3}+) (?<forexAmount>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^(?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forexAmount")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            if (forex.getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                            {
                                
                                t.getPortfolioTransaction()
                                                .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            }
                        })
                        
                        .section("date")
                        .match("^Betrag belastet auf Kontonummer (\\d+), Valutadatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Börsentransaktion: Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Börsentransaktion: Verkauf (.*)$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })
                        
                        .section("name", "isin", "shares", "currency")
                        .find("Titel Ort der Ausführung")
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*) (.*)$")
                        .match("^(?<shares>[\\d+',.]*) ([\\d+',.]*) (?<currency>\\w{3}+) ([\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        .section("feecurrency", "fee").optional()
                        .match("^Kommission (?<feecurrency>\\w{3}+) (?<fee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money fee = Money.of(asCurrencyCode(v.get("feecurrency")), asAmount(v.get("fee")));
                            if (fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fee));
                        })
                        
                        .section("taxcurrency", "tax").optional()
                        .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<taxcurrency>\\w{3}+) (?<tax>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("taxcurrency")), asAmount(v.get("tax")));
                            if (tax.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })
                        
                        .section( "stockfeecurrency", "stockfee").optional()
                        .match("^Börsengebühren (?<stockfeecurrency>\\w{3}+) (?<stockfee>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            Money stock_fee = Money.of(asCurrencyCode(v.get("stockfeecurrency")), asAmount(v.get("stockfee")));
                            if (stock_fee.getCurrencyCode().equals(t.getAccountTransaction().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, stock_fee));
                        })
                        
                        .section("amount")
                        .match("^Zu Ihren Gunsten (\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section("date")
                        .match("^Betrag gutgeschrieben auf Ihrer Kontonummer (\\d+), Valutadatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(BuySellEntryItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividende Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })
                        
                        // There are two kinds of dividend exports
                        // 1st:
                        // ISIN: <isin>
                        // <name> NKN: 2560588 <shares>
                        .section("name", "isin", "shares").optional()
                        .match("^ISIN: (?<isin>\\S*)$")
                        .match("^(?<name>.*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        // 2nd:
                        // <name> ISIN: <isin>NKN: 3291273 <shares>
                        .section("name", "isin", "shares").optional()
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        .section("date")
                        .match("^Ausführungsdatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                        })
                        
                        .section("gross", "grosscurrency", "amount", "currency")
                        .match("^Betrag (?<grosscurrency>\\w{3}+) (?<gross>[\\d+',.]*)$")
                        .match("^Total (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.getSecurity().setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            long gross = asAmount(v.get("gross"));
                            long tax = gross - t.getAmount();
                            Unit unit = new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax));
                            if (unit.getAmount().getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(unit);
                        })
                        
                        .wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addCapitalGainTransaction()
    {
        DocumentType type = new DocumentType("Kapitalgewinn");
        this.addDocumentTyp(type);

        Block block = new Block("^Kapitalgewinn Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })
                        
                        // There are two kinds of dividend exports
                        // 1st:
                        // ISIN: <isin>
                        // <name> NKN: 2560588 <shares>
                        .section("name", "isin", "shares").optional()
                        .match("^ISIN: (?<isin>\\S*)$")
                        .match("^(?<name>.*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        // 2nd:
                        // <name> ISIN: <isin>NKN: 3291273 <shares>
                        .section("name", "isin", "shares").optional()
                        .match("^(?<name>.*) ISIN: (?<isin>\\S*)NKN: [\\d+]* (?<shares>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        .section("date")
                        .match("^Ausführungsdatum (?<date>\\d+\\.\\d+\\.\\d{4})$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                        })
                        
                        .section("currency", "amount")
                        .match("^Total (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.getSecurity().setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        .wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Jahresgebühr");
        this.addDocumentTyp(type);

        Block block = new Block("^Jahresgebühr (.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        .section("date", "amount", "currency")
                        .find("^Jahresgebühr (.*)")
                        .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                        .match("^Betrag belastet (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        .wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addAccountTransferTransaction()
    {
        DocumentType type = new DocumentType("Zahlungsverkehr");
        
        this.addDocumentTyp(type);

        Block block = new Block("^Zahlungsverkehr (.*)$");
        type.addBlock(block);
        

        block.set(new Transaction<AccountTransferEntry>()
                        

                        .subject(() -> {
                            return new AccountTransferEntry();
                        })
                        
                        .section("date", "amountTo", "currencyTo", "exchangeRate", "amountFrom", "currencyFrom").optional()
                        .find("^Zahlungsverkehr (.*)")
                        .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                        .match("^Betrag belastet (?<currencyTo>\\w{3}+) (?<amountTo>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^Total (?<currencyFrom>\\w{3}+) (?<amountFrom>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.getSourceTransaction().setDateTime(asDate(v.get("date")));
                            t.getSourceTransaction().setCurrencyCode(asCurrencyCode(v.get("currencyFrom")));
                            t.getSourceTransaction().setAmount(asAmount(v.get("amountFrom")));
                            
                            Money forex = Money.of(asCurrencyCode(v.get("currencyTo")), asAmount(v.get("amountTo")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currencyFrom")), asAmount(v.get("amountFrom"))); 
                            t.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            
                            t.getTargetTransaction().setDateTime(asDate(v.get("date")));
                            t.getTargetTransaction().setCurrencyCode(asCurrencyCode(v.get("currencyTo")));
                            t.getTargetTransaction().setAmount(asAmount(v.get("amountTo")));
                        })
                        
                        .section("date", "amountFrom", "currencyFrom", "exchangeRate", "amountTo", "currencyTo").optional()
                        .find("^Zahlungsverkehr (.*)")
                        .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                        .match("^Gutgeschriebener Betrag (?<currencyFrom>\\w{3}+) (?<amountFrom>[\\d+',.]*)$")
                        .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                        .match("^Total (?<currencyTo>\\w{3}+) (?<amountTo>[\\d+',.]*)$")
                        .assign((t, v) -> {
                            t.getSourceTransaction().setDateTime(asDate(v.get("date")));
                            t.getSourceTransaction().setCurrencyCode(asCurrencyCode(v.get("currencyFrom")));
                            t.getSourceTransaction().setAmount(asAmount(v.get("amountFrom")));
                            
                            Money forex = Money.of(asCurrencyCode(v.get("currencyTo")), asAmount(v.get("amountTo")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money gross = Money.of(asCurrencyCode(v.get("currencyFrom")), asAmount(v.get("amountFrom"))); 
                            t.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                            
                            t.getTargetTransaction().setDateTime(asDate(v.get("date")));
                            t.getTargetTransaction().setCurrencyCode(asCurrencyCode(v.get("currencyTo")));
                            t.getTargetTransaction().setAmount(asAmount(v.get("amountTo")));
                        })
                        
                        .wrap(t -> new AccountTransferItem(t, true)));
    }

    @Override
    public String getLabel()
    {
        return "PostFinance";
    }

}
