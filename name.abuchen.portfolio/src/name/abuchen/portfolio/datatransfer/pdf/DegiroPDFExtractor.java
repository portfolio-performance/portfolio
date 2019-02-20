package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class DegiroPDFExtractor extends AbstractPDFExtractor
{

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.GERMANY); //$NON-NLS-1$
    
    public DegiroPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("DEGIRO"); //$NON-NLS-1$
        addBankAccountTransactions();    
    }

    @SuppressWarnings("nls")
    private void addBankAccountTransactions()
    {
        DocumentType type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        // 02-08-2017 00:00 Einzahlung EUR 350,00 EUR 350,00
        
        Block blockDeposit = new Block(".* Einzahlung .*");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("(?<date>\\d+-\\d+-\\d{4}) \\d+:\\d+ Einzahlung (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));

        // 03-08-2017 15:32 BERKSHIRE HATHAWAY INC US0846707026 Währungswechsel USD 177,50 USD 0,00
        // (Einbuchung)
        // 03-08-2017 15:32 BERKSHIRE HATHAWAY INC US0846707026 Währungswechsel 1,1851 EUR -149,92 EUR 36,52
        // (Ausbuchung)
        // 03-08-2017 15:32 BERKSHIRE HATHAWAY INC US0846707026 Transaktionsgebühr EUR -0,50 EUR 186,44
        // 03-08-2017 15:32 BERKSHIRE HATHAWAY INC US0846707026 Kauf 1 zu je 177,5 USD -177,50 USD -177,50

        Block blockBuyForex = new Block(".*Einbuchung.*");
        type.addBlock(blockBuyForex);
        blockBuyForex.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })
                        
                        .section("date", "isin", "name", "shares", "currency", "amount")
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>[^ ]+) Kauf (?<shares>[.\\d]+[,\\d]*) zu je [.\\d]+[,\\d]* (?<currency>\\w{3}) -(?<amount>[.\\d]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                t.setSecurity(getOrCreateSecurity(v));
                                t.setDate(asDate(v.get("date")));
                                t.setShares(asShares(v.get("shares")));
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setAmount(asAmount(v.get("amount")));

                        })
                        
                        .section("exchangeRate", "currency", "amount").optional()
                        .match(".* Währungswechsel (?<exchangeRate>[.\\d]+,\\d+) (?<currency>\\w{3}) -(?<amount>[.\\d]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                Money currentMonetaryAmount = t.getAccountTransaction().getMonetaryAmount(); // in fx
                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")); // exchange rate in fx/EUR
                                BigDecimal accountMoneyValue = BigDecimal.valueOf(t.getAccountTransaction().getAmount())
                                                .divide(exchangeRate,RoundingMode.HALF_DOWN); // in EUR
                                String currencyCode = asCurrencyCode(v.get("currency")); // "EUR"
                                Money accountMoney = Money.of(currencyCode, Math.round(accountMoneyValue.doubleValue())); // in EUR
                                
                                // change total amount to amount in EUR
                                t.setCurrencyCode(currencyCode); // "EUR"
                                t.setMonetaryAmount(accountMoney); // in EUR 
                                
                                // replace BRUTTO (which is in foreign currency) with the value in transaction currency
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, accountMoney, currentMonetaryAmount, inverseRate);
                                t.getPortfolioTransaction().addUnit(grossValue);
                            
                                
                                // the difference between the accountMoney and the amount after changing the currency
                                // are the fees and taxes which had to be paid in Fx
                                Money feesAndTaxes = Money.of(currencyCode, asAmount(v.get("amount"))).subtract(accountMoney);        // in EUR
                                BigDecimal feesAndTaxesFxValue = BigDecimal.valueOf(feesAndTaxes.getAmount())
                                                                           .multiply(exchangeRate);            // in USD
                                Money feesAndTaxesFx = Money.of(t.getPortfolioTransaction().getSecurity().getCurrencyCode(),
                                                                            Math.round(feesAndTaxesFxValue.doubleValue())); // in USD
                                
                                // add fee in fx currency and to total amount
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feesAndTaxes, feesAndTaxesFx, inverseRate));
                                t.setMonetaryAmount(accountMoney.add(feesAndTaxes));
                                        
                        })

                                                
                        // transaction fee
                        .section("fee", "currency").optional()
                        .match(".* Transaktionsgebühr (?<currency>\\w{3}) -(?<fee>[.\\d]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                // set fee    
                                Money feeAmount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                                
                                // add fee to total amount
                                Money currentMonetaryAmount = t.getAccountTransaction().getMonetaryAmount();
                                t.setMonetaryAmount(currentMonetaryAmount.add(feeAmount));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
        
    }
    
    @Override
    LocalDateTime asDate(String value)
    {
        return LocalDateTime.parse(value, DATE_FORMAT);
    }

    @Override
    public String getLabel()
    {
        return "DEGIRO"; //$NON-NLS-1$
    }
}
