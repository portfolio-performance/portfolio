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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class DegiroPDFExtractor extends AbstractPDFExtractor
{

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.GERMANY); //$NON-NLS-1$
    
    public DegiroPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("DEGIRO"); //$NON-NLS-1$
        addBankAccountTransactions();
        addPortfolioTransactions();
    }

    @SuppressWarnings("nls")
    private void addBankAccountTransactions()
    {
        DocumentType type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        // 02-08-2017 00:00 Einzahlung EUR 350,00 EUR 350,00
        // 01-02-2019 11:44 01-02-2019 Einzahlung EUR 0,01 EUR 0,01
        Block blockDeposit = new Block("^.* Einzahlung .*$");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?Einzahlung (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        // 17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 22,64 EUR 519,89
        Block blockDividends = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Dividende .*");
        type.addBlock(blockDividends);
        blockDividends.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount", "balance")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}+) .*Dividende (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) (\\w{3}) -?(?<balance>[\\d.]+,\\d{2}).*$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            type.getCurrentContext().put("balance", v.get("balance"));
                        })
                        
                        .section("currencyTax", "tax").optional()
                        .match("^(\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (\\w{12}+) .*Dividendensteuer\\s(?<currencyTax>\\w{3}) -?(?<tax>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currencyTax")), asAmount(v.get("tax")))));
                        })
                        
                        .wrap(t -> {
                            // (Dididendenzahlungen in Fremdwährung, die den Saldo nicht ändern, sind wohl eher nicht relevant...?)
                            // 17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
                            if (!CurrencyUnit.EUR.equalsIgnoreCase(t.getCurrencyCode()) && t.getAmount() == asAmount(type.getCurrentContext().get("balance")))
                                return new NonImportableItem("Währung der Dividende passt nicht zur Währung des Wertpapiers");
                            else
                                return new TransactionItem(t);
                        }));
        
        //31-07-2017 00:00 Zinsen EUR -0,07 EUR -84,16
        Block blockInterest = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Zinsen .*");
        type.addBlock(blockInterest);
        blockInterest.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Zinsen (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}).*$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
        
        // 30-06-2017 00:00 Einrichtung von EUR -2,50 EUR 1.116,79
        // Handelsmodalitäten
        // 2017
        Block blockTrademodalities = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Einrichtung von .*$");
        type.addBlock(blockTrademodalities);
        blockTrademodalities.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Einrichtung von (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}).*$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
        
        //22-03-2019 14:10 22-03-2019 ODX4 P11550.00 22MAR19 DE000C2ZNL25 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 2.204,26
        Block blockFeeStrike = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Gebühr.* (Ausübung|Zuteilung).*$");
        type.addBlock(blockFeeStrike);
        blockFeeStrike.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}+) .*Gebühr.* (Ausübung|Zuteilung).*(?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(t -> new TransactionItem(t)));
        
        // 05-03-2019 15:37 28-02-2019 Rabatt für 500 Euro Aktion EUR 18,00 EUR 1.960,64
        // 05-03-2019 15:37 28-02-2019 Gutschrift für die Neukundenaktion EUR 18,00 EUR 1.960,69
        Block blockFeeReturn = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?(Rabatt|Gutschrift).*$");
        type.addBlock(blockFeeReturn);
        blockFeeReturn.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES_REFUND);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(Rabatt|Gutschrift) .* (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));
    }
    
    
    @SuppressWarnings("nls")
    private void addPortfolioTransactions()
    {
        DocumentType type = new DocumentType("Transaktionsübersicht");
        this.addDocumentTyp(type);
        
        DocumentType typeNL = new DocumentType("Transacties");
        this.addDocumentTyp(typeNL);

        Block blockBuy = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ .* \\w{12}+ .* \\w{3}+ .*[.\\d]+,\\d{2}$");
        type.addBlock(blockBuy);
        typeNL.addBlock(blockBuy);

        blockBuy.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .oneOf(
                            // 07-02-2019 12:22 ODX2 C11200.00 08FEB19 DE000C25KFN9 ERX 1 EUR 30,00 EUR -150,00 EUR -150,00 EUR -0,90 EUR -150,90
                            // 01-04-2019 15:35 DEUTSCHE BANK AG NA O.N DE0005140008 XET -136 EUR 7,45 EUR 1.013,20 EUR 1.013,20 EUR -2,26 EUR 1.010,94
                            // 01-04-2019 12:20 DEUTSCHE BANK AG NA O.N DE0005140008 XET 18 EUR 7,353 EUR -132,35 EUR -132,35 EUR -0,03 EUR -132,38
                            // 08-02-2019 13:27 ODX2 C11000.00 08FEB19 DE000C25KFE8 ERX -3 EUR 0,00 EUR 0,00 EUR 0,00 EUR 0,00
                            section -> section.attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                            .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>\\w{12}+) \\w{3} (?<shares>[-]?[.\\d]+[,\\d]*) .* \\w{3} -?[.\\d]+,\\d{2} (?<currencyFee>\\w{3}) (?<fee>-?[.\\d]+,\\d{2}) (?<currency>\\w{3}) -?(?<amount>[.\\d]+,\\d{2})$")
                            .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    if (v.get("shares").startsWith("-")) 
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));  
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                        }),
                            //26-04-2019 17:52 TESLA MOTORS INC. - C US88160R1014 NDQ 2 USD 240,00 USD -480,00 EUR -430,69 1,1145 EUR -0,51 EUR -431,20
                            //29-04-2019 16:11 TESLA MOTORS INC. - C US88160R1014 NDQ -3 USD 240,00 USD 720,00 EUR 645,04 1,1162 EUR -0,51 EUR 644,53
                            section -> section.attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                            .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>\\w{12}+) \\w{3} (?<shares>[-]?[.\\d]+[,\\d]*) \\w{3} -?[.\\d]+,\\d{2} (?<currency>\\w{3}) -?(?<amountFx>[.\\d]+,\\d{2}).* \\w{3} -?[.\\d]+,\\d{2} (?<exchangeRate>[.\\d]+,\\d{1,6}) (?<currencyFee>\\w{3}) (?<fee>-?[.\\d]+,\\d{2}) (?<currencyAccount>\\w{3}) -?(?<amount>[.\\d]+,\\d{2})$")
                            .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    if (v.get("shares").startsWith("-")) 
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));  
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                                    
                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
    
                            })
                        )

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
