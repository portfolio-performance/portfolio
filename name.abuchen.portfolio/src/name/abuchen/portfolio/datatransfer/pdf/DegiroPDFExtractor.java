package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.GERMANY);  //$NON-NLS-1$
    
    public DegiroPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("DEGIRO");  //$NON-NLS-1$
        addBankAccountTransactions();
        addPortfolioTransactions();
    }
    
    private static class FxChange
    {

        public FxChange(Money money, String exchangeRate, String amountBase)
        {
            this.money = money;
            this.exchangeRate = exchangeRate;
            this.amountBase = amountBase;
        }

        private Money money;
        private String exchangeRate;
        private String amountBase;

        public Money getMoney()
        {
            return money;
        }

        public String getExchangeRate()
        {
            return exchangeRate;
        }

        public String getAmountBase()
        {
            return amountBase;
        }
    }
    
    @SuppressWarnings("nls")
    private void addBankAccountTransactions()
    {   
        final DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {  
            
            // (Einb.)  19-07-2017 00:00 Währungswechsel 1,1528 EUR 0,75 EUR 1.783,89
            //          03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
            Pattern pCurrencyFx = Pattern.compile("(?<date>\\d+-\\d+-\\d{4}) (?:\\d+:\\d+) (?<valuta>\\d+-\\d+-\\d{4} )?Währungswechsel.* (?<fxRate>[.\\d]+,\\d+) (?<currency>\\w{3}) -?(?<amount>[.\\d]+,\\d+) (\\w{3}) .*"); 
            
            // (Ausb.)  19-07-2017 00:00 Währungswechsel USD -0,86 USD 0,00
            //          03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
            Pattern pCurrencyBase = Pattern.compile("(\\d+-\\d+-\\d{4}) (?:\\d+:\\d+) (?:\\d+-\\d+-\\d{4} )?Währungswechsel.* (?<currency>\\w{3}) -?(?<amount>[.\\d]+,\\d+) (\\w{3}).*"); 
            
            
            // read the current context here
            for (int i = 10; i < lines.length; i++)
            {
                Matcher mFx = pCurrencyFx.matcher(lines[i]);
                if (mFx.matches())
                {
                    StringBuilder contextEntryKey = new StringBuilder("exchange_");
                    contextEntryKey.append(mFx.group("date")).append("_");
                    // Date + (Valuta) + currencyFx + fxRate
                    if (mFx.group("valuta") != null)
                    {
                        contextEntryKey.append(mFx.group("valuta").trim()).append("_");
                    }
                    
                    if (!mFx.group("currency").equalsIgnoreCase(getClient().getBaseCurrency()))
                    {
                        contextEntryKey.append(mFx.group("currency")).append("_");
                        contextEntryKey.append(mFx.group("fxRate")).append("_");

                        Matcher mBase = pCurrencyBase.matcher(lines[i - 1]);
                        if (mBase.matches() && mBase.group("currency").equalsIgnoreCase(getClient().getBaseCurrency()))
                        {
                            contextEntryKey.append(mBase.group("amount"));
                            context.put(contextEntryKey.toString(), mFx.group("amount"));
                        }
                    }
                    else
                    {
                        Matcher mBase = pCurrencyBase.matcher(lines[i - 2]);
                        if (mBase.matches() && !mBase.group("currency").equalsIgnoreCase(getClient().getBaseCurrency()))
                        {
                            contextEntryKey.append(mBase.group("currency")).append("_");
                            contextEntryKey.append(mFx.group("fxRate")).append("_");
                            contextEntryKey.append(mFx.group("amount"));
                            context.put(contextEntryKey.toString(), mBase.group("amount")); 
                        }
                    }
                }
            }
        });
        this.addDocumentTyp(type);

        // 02-08-2017 00:00 Einzahlung EUR 350,00 EUR 350,00
        // 01-02-2019 11:44 01-02-2019 Einzahlung EUR 0,01 EUR 0,01
        // 22-02-2019 18:40 22-02-2019 SOFORT Einzahlung EUR 27,00 EUR 44,89
        // 26-10-2020 15:00 26-10-2020 flatex Einzahlung EUR 500,00 EUR 512,88
        Block blockDeposit = new Block("^.* Einzahlung .*$"); 
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("date", "currency", "amount")   
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(SOFORT |flatex )?Einzahlung (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*") 
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                t.setDateTime(asDate(v.get("date"))); 
                                t.setAmount(asAmount(v.get("amount"))); 
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        // 05-08-2019 00:09 05-08-2019 Auszahlung EUR -1.000,00 EUR 1.445,06
        Block blockRemoval = new Block("^.* Auszahlung .*$"); 
        type.addBlock(blockRemoval);
        blockRemoval.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.REMOVAL);
            return t;
        })

                        .section("date", "currency", "amount")   
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?Auszahlung (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*") 
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                t.setDateTime(asDate(v.get("date"))); 
                                t.setAmount(asAmount(v.get("amount"))); 
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 22,64 EUR 519,89
        
        // nicht  25-06-2019 06:53 24-06-2019 Währungswechsel (Einbuchung) 1,1387 USD 0,31 USD -0,00
        
        //        15-06-2019 06:44 14-06-2019 Währungswechsel (Einbuchung) EUR 0,30 EUR 23,09
        //        15-06-2019 06:44 14-06-2019 Währungswechsel (Ausbuchung) 1,1219 USD -0,34 USD 0,00
        //        ....
        //        14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividende USD 0,40 USD 0,34
        //        14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
        
        //        05-07-2019 06:49 04-07-2019 Währungswechsel (Einbuchung) EUR 10,12 EUR 2.119,03
        //        05-07-2019 06:49 04-07-2019 Währungswechsel (Ausbuchung) 1,1297 USD -11,44 USD -0,00
        //        04-07-2019 10:22 30-06-2019 MORGAN STANLEY USD LIQUIDITY FUND LU0904783114 Fondsausschüttung USD 11,44 USD 11,44
        
        //        19-07-2017 00:00 Währungswechsel USD -0,86 USD 0,00
        //        (Ausbuchung)
        //        19-07-2017 00:00 Währungswechsel 1,1528 EUR 0,75 EUR 1.783,89
        //        (Einbuchung)
        //        ....
        //        17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
        
        //        17-05-2019 06:53 16-05-2019 Währungswechsel (Einbuchung) EUR 0,58 EUR 11,49
        //        17-05-2019 06:53 16-05-2019 Währungswechsel (Ausbuchung) 1,1186 USD -0,65 USD 0,00
        //        16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividende USD 0,77 USD 0,65
        //        16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividendensteuer USD -0,12 USD -0,12
        
        //        06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,04
        
        //        03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        //        03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        
        //        02-08-2019 07:38 02-08-2019 VODAFONE GROUP PLC GB00BH4HKS39 Dividende GBP 3,73 GBP 3,73
        Block blockDividends = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Dividende .*"); 
        type.addBlock(blockDividends);
        blockDividends.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount")    //$NON-NLS-4$ //$NON-NLS-5$
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}) (Dividende|Fondsausschüttung) (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) (\\w{3}).*$") 
                        .assign((t, v) -> {
                            
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"))); 
                            
                            Map<String, String> context = type.getCurrentContext();
                            FxChange fxChange = getFxChangeFromContext(context, v.get("date"), v.get("currency"), v.get("amount"));
                            if (!v.get("currency").equalsIgnoreCase(getClient().getBaseCurrency()) && fxChange != null)
                            {
                                String currencyCodeFx = asCurrencyCode(fxChange.getMoney().getCurrencyCode()); 

                                t.setAmount(asAmount(fxChange.getAmountBase()));
                                t.setCurrencyCode(getClient().getBaseCurrency()); 

                                BigDecimal exchangeRate = asExchangeRate(fxChange.getExchangeRate()); 
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                long partialAmountDividend = inverseRate
                                                .multiply(BigDecimal.valueOf(asAmount(v.get("amount")))) 
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();
                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                                Money.of(getClient().getBaseCurrency(), partialAmountDividend),
                                                Money.of(currencyCodeFx, asAmount(v.get("amount"))), inverseRate)); 
                                
                                context.put("FX_RATE_FOR_TAX_FEES", fxChange.getExchangeRate());
                            }
                            else if (v.get("currency").equalsIgnoreCase(getClient().getBaseCurrency()))
                            {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                t.setAmount(asAmount(v.get("amount")));                                 
                            }
                        })
                        
                        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
                        // nicht  17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
                        .section("isin", "currencyTax", "tax").optional()  
                        .match("^(\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (?<isin>\\w{12}) .*Dividendensteuer\\s(?<currencyTax>\\w{3}) -?(?<tax>[\\d.]+,\\d{2}) (\\w{3}).*$") 
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            if (!v.get("currencyTax").equalsIgnoreCase(getClient().getBaseCurrency())
                                            && context.get("FX_RATE_FOR_TAX_FEES") != null
                                            && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                BigDecimal exchangeRate = asExchangeRate(context.get("FX_RATE_FOR_TAX_FEES")); 
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                String currencyCodeFx = asCurrencyCode(v.get("currencyTax")); 

                                Money mTaxesFx = Money.of(currencyCodeFx, asAmount(v.get("tax"))); 

                                long taxesFxInEUR = BigDecimal.valueOf(mTaxesFx.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), taxesFxInEUR), mTaxesFx,
                                                inverseRate));
                            } 
                            else if (v.get("currencyTax").equalsIgnoreCase(getClient().getBaseCurrency()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currencyTax")), asAmount(v.get("tax")))));                            
                            }
                            
                        })
                        
                        //  06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,040
                        .section("isin", "currencyFee", "feeFx").optional()      
                        .match("^(\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (?<isin>\\w{12}) ADR/GDR Weitergabegebühr (?<currencyFee>\\w{3}) -?(?<feeFx>[\\d.]+,\\d{2}) .*$") 
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            if (!v.get("currencyFee").equalsIgnoreCase(getClient().getBaseCurrency())
                                            && context.get("FX_RATE_FOR_TAX_FEES") != null
                                            && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {

                                BigDecimal exchangeRate = asExchangeRate(context.get("FX_RATE_FOR_TAX_FEES")); 
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                String currencyCodeFx = asCurrencyCode(v.get("currencyFee")); 

                                Money mFeesFx = Money.of(currencyCodeFx, asAmount(v.get("feeFx"))); 

                                long feesFxInEUR = BigDecimal.valueOf(mFeesFx.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                t.addUnit(new Unit(Unit.Type.FEE, Money.of(t.getCurrencyCode(), feesFxInEUR), mFeesFx,
                                                inverseRate));
                            } 
                            else if (v.get("currencyFee").equalsIgnoreCase(getClient().getBaseCurrency()))
                            {
                                t.addUnit(new Unit(Unit.Type.FEE, Money.of(asCurrencyCode(v.get("currencyFee")), 
                                                asAmount(v.get("feeFx")))));
                            }
                        })
                        
                        .wrap(t -> {
                            
                            type.getCurrentContext().remove("FX_RATE_FOR_TAX_FEES");
                            
                              // check if there is a delta between the gross
                              // amount and the sum of fees and taxs
    
                              Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
                              Optional<Unit> feesAndTaxesValue = t.getUnits().filter(u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE).findAny();
                              if (grossValue.isPresent() && feesAndTaxesValue.isPresent())
                              {
                                  long net = t.getAmount();
                                  long gross = grossValue.get().getAmount().getAmount();
    
                                  long feesAndTaxes = t.getUnits().filter(
                                                  u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                                                  .mapToLong(u -> u.getAmount().getAmount()).sum();
    
                                  long delta = gross - feesAndTaxes - net;
    
                                  if (delta == 1 || delta == -1 )
                                  {
                                      // pick the first unit and make it fit;see
                                      // discussion
                                      // https://github.com/buchen/portfolio/pull/1198
    
                                      Unit unit = t.getUnits().filter(
                                                      u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                                                      .filter(u -> u.getExchangeRate() != null)
                                                      .findFirst().orElseThrow(IllegalArgumentException::new);
    
                                      t.removeUnit(unit);
    
                                      long amountPlusDelta = unit.getAmount().getAmount() + delta;
                                      long forexPlusDelta = BigDecimal.ONE
                                                      .divide(unit.getExchangeRate(), 10, RoundingMode.HALF_DOWN)
                                                      .multiply(BigDecimal.valueOf(amountPlusDelta))
                                                      .setScale(0, RoundingMode.HALF_DOWN).longValue();
    
                                      Unit newUnit = new Unit(unit.getType(),
                                                      Money.of(unit.getAmount().getCurrencyCode(),
                                                                      amountPlusDelta),
                                                      Money.of(unit.getForex().getCurrencyCode(),
                                                                      forexPlusDelta),
                                                      unit.getExchangeRate());
    
                                      t.addUnit(newUnit);
                                  }
                              }
                            if (t.getCurrencyCode() != null && t.getAmount() != 0L)
                            { 
                                return new TransactionItem(t); 
                            }
                            return null;
                        }));
        
        //31-07-2017 00:00 Zinsen EUR -0,07 EUR -84,16
        //05-12-2018 16:07 30-11-2018 Zinsen für Leerverkauf EUR -0,04 EUR 220,63
        Block blockInterest = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Zinsen .*"); 
        type.addBlock(blockInterest);
        blockInterest.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return t;
        })

                        .section("date", "currency", "amount")   
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Zinsen (für Leerverkauf )?(?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}).*$") 
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                            t.setDateTime(asDate(v.get("date"))); 
                            t.setAmount(asAmount(v.get("amount"))); 
                        })

                        .wrap(t -> new TransactionItem(t)));
        
        //04-01-2019 20:14 04-01-2019 SOFORT Zahlungsgebühr EUR -1,00 EUR 150,00
        Block blockDepositFee = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?SOFORT Zahlungsgeb.*hr.*$"); 
        type.addBlock(blockDepositFee);
        blockDepositFee.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "currency", "amount")   
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?SOFORT Zahlungsgeb.*hr (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*$") 
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                            t.setDateTime(asDate(v.get("date"))); 
                            t.setAmount(asAmount(v.get("amount"))); 
                        })
                        
                        .wrap(t -> new TransactionItem(t)));
        
        
        
        // 30-06-2017 00:00 Einrichtung von EUR -2,50 EUR 1.116,79
        // Handelsmodalitäten
        // 2017
        //03-07-2019 11:47 30-06-2019 Einrichtung von Handelsmodalitäten 2019 (New York Stock EUR -0,54 EUR 22,16
        //01-02-2019 16:31 31-01-2019 Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ) EUR 2,50 EUR 108,21
        Block blockTrademodalities = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Einrichtung von .*$"); 
        type.addBlock(blockTrademodalities);
        blockTrademodalities.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })
                        
                        .oneOf(
                                        section -> section.attributes("date", "currency", "type", "amount").match(    //$NON-NLS-4$
                                        "^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Einrichtung von (?<currency>\\w{3})(?<type>\\s-?)(?<amount>[\\d.]+,\\d{2}) (\\w{3}) .*$") 
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                            t.setDateTime(asDate(v.get("date"))); 
                                            t.setAmount(asAmount(v.get("amount"))); 
                                                            if (" ".equalsIgnoreCase(v.get("type")))
                                                            {
                                                t.setType(AccountTransaction.Type.FEES_REFUND);
                                            }
                                        }),
                                        section -> section.attributes("date", "currency", "type", "amount").match(    //$NON-NLS-4$
                                                        "^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Einrichtung von Handelsmodalit.* (?<currency>\\w{3})(?<type>\\s-?)(?<amount>[\\d.]+,\\d{2}) (\\w{3}) .*$") 
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                                            t.setDateTime(asDate(v.get("date"))); 
                                                            t.setAmount(asAmount(v.get("amount"))); 
                                                            if (" ".equalsIgnoreCase(v.get("type")))
                                                            {
                                                                t.setType(AccountTransaction.Type.FEES_REFUND);
                                                            }
                                        })
                              )

                        .wrap(t -> new TransactionItem(t)));
        
        //22-03-2019 14:10 22-03-2019 ODX4 P11550.00 22MAR19 DE000C2ZNL25 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 2.204,26
        Block blockFeeStrike = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Gebühr.* (Ausübung|Zuteilung).*$"); 
        type.addBlock(blockFeeStrike);
        blockFeeStrike.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount")    //$NON-NLS-4$ //$NON-NLS-5$
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}) .*Gebühr.* (Ausübung|Zuteilung).*(?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*$") 
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
    private FxChange getFxChangeFromContext(Map<String, String> fxContextMap, String date, String currencyCode, String amountFx)
    {
        String dateOnly = date.substring(0, date.indexOf(" ")); 
        for (String key : fxContextMap.keySet())
        {
            String[] parts = key.split("_"); 
            if (parts[0].equalsIgnoreCase("exchange")) 
            {
                // entry without valuta information
                if (parts[2].equalsIgnoreCase(currencyCode) && (parts[1].equalsIgnoreCase(dateOnly)
                                || fxContextMap.get(key).equalsIgnoreCase(amountFx)))
                {
                    return new FxChange(Money.of(currencyCode, asAmount(fxContextMap.get(key))), parts[3],
                                    parts[4]);
                }
                // entry with valuta
                else if (parts[3].equalsIgnoreCase(currencyCode)
                                && (parts[1].equalsIgnoreCase(dateOnly) || parts[2].equalsIgnoreCase(dateOnly)))
                {
                    return new FxChange(Money.of(currencyCode, asAmount(fxContextMap.get(key))), parts[4], parts[5]);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("nls")
    private void addPortfolioTransactions()
    {
        // -------------------------------------
        // Information:
        //
        // If you make changes to the regEx, the examples will help you to adjust it correctly.
        // Always check where and on which side the currency, amount, exchange rate and fees are!
        // Please do not delete these examples!
        // -------------------------------------

        DocumentType type = new DocumentType("Transaktionsübersicht|Transacciones|Transacties|Transactions|Transakcje");
        this.addDocumentTyp(type);

        Block blockBuy = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ .* \\w{12} .* (\\w{3}|\\w{3} \\w{4}) .*([.\\d]+,\\d{2}|\\w{3})$");
        type.addBlock(blockBuy);

        blockBuy.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .oneOf(

                            // -------------------------------------
                            // without exchange rate
                            // without fee
                            // without stock exchange place
                            // Money        --> \d,\d OR \d.\d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Total amount
                            // -------------------------------------          
                            // 08-02-2019 13:27 ODX2 C11000.00 08FEB19 DE000C25KFE8 ERX -3 EUR 0,00 EUR 0,00 EUR 0,00 EUR 0,00
                            // 09-07-2019 14:08 VANGUARD FTSE AW IE00B3RBWM25 EAM 3 EUR 77,10 EUR -231,30 EUR -231,30 EUR -231,30
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*)" 
                                                + " \\w{3} [-.,\\d]*" 
                                                + " \\w{3} [-.,\\d]*" 
                                                + " \\w{3} [-.,\\d]*" 
                                                + " (?<currency>\\w{3}) -?(?<amount>[.,\\d]*)$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // -------------------------------------
                            // without exchange rate
                            // without fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR  \d\s \d\s,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Total amount
                            // -------------------------------------     
                            // 13-03-2018 00:00 BAUMOT GROUP AG O.N DE000A2DAM11 XET -350 1,84 EUR 644,00 EUR 644,00 EUR 644,00 EUR
                            // 22-07-2020 00:00 PHARMA MAR SA ES0169501022 MAD 21 114,66 EUR -2 407,86 EUR -2 407,86 EUR -2 407,86 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*) "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "-?(?<amount>[.,\\d\\s]*) (?<currency>\\w{3})$") 
                                .assign((t, v) -> {
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // -------------------------------------
                            // without exchange rate
                            // with fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Fee | Total amount
                            // -------------------------------------  
                            // 07-02-2019 12:22 ODX2 C11200.00 08FEB19 DE000C25KFN9 ERX 1 EUR 30,00 EUR -150,00 EUR -150,00 EUR -0,90 EUR -150,90
                            // 01-04-2019 15:35 DEUTSCHE BANK AG NA O.N DE0005140008 XET -136 EUR 7,45 EUR 1.013,20 EUR 1.013,20 EUR -2,26 EUR 1.010,94
                            // 01-04-2019 12:20 DEUTSCHE BANK AG NA O.N DE0005140008 XET 18 EUR 7,353 EUR -132,35 EUR -132,35 EUR -0,03 EUR -132,38
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*)"
                                                + " \\w{3} [-.,\\d]*"
                                                + " \\w{3} [-.,\\d]*"
                                                + " \\w{3} [-.,\\d]*"
                                                + " (?<currencyFee>\\w{3}) -?(?<fee>[.,\\d]*)"
                                                + " (?<currency>\\w{3}) -?(?<amount>[.,\\d]*)$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // -------------------------------------
                            // without exchange rate 
                            // with fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR     \d\s \d\s,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Fee | Total amount
                            // -------------------------------------
                            // 06-10-2020 12:47 ALMIRALL ES0157097017 MAD 128 10,45 EUR -1 337,60 EUR -1 337,60 EUR -4,67 EUR -1 342,27 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*) "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "-?(?<fee>[.,\\d\\s]*) (?<currencyFee>\\w{3}) "
                                                + "-?(?<amount>[.,\\d\\s]*) (?<currency>\\w{3})$") 
                                .assign((t, v) -> {
                                    v.put("fee", stripBlanks(v.get("fee")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // with fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d
                            // exchangeRate --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Market value | Local market value | Fee | Total amount
                            // -------------------------------------
                            // 26-04-2019 17:52 TESLA MOTORS INC. - C US88160R1014 NDQ 2 USD 240,00 USD -480,00 EUR -430,69 1,1145 EUR -0,51 EUR -431,20
                            // 29-04-2019 16:11 TESLA MOTORS INC. - C US88160R1014 NDQ -3 USD 240,00 USD 720,00 EUR 645,04 1,1162 EUR -0,51 EUR 644,53
                            // 06-08-2019 20:20 WILLIAMS-SONOMA INC. US9699041011 NSY 1 USD 64,695 USD -64,70 EUR -57,85 1,1184 EUR -0,50 EUR -58,35
                            // 23-07-2019 15:30 RIO TINTO PLC COMMON S US7672041008 NSY -3 USD 61,04 USD 183,12 EUR 163,83 1,1177 EUR -0,51 EUR 163,32
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*)"
                                                + " \\w{3} -?[.\\d]+,\\d{2,4}" 
                                                + " (?<currency>\\w{3}) -?(?<amountFx>[.\\d]+,\\d{2}).*"
                                                + " \\w{3} -?[.\\d]+,\\d{2}"
                                                + " (?<exchangeRate>[.\\d]+,\\d{1,6})"
                                                + " (?<currencyFee>\\w{3}) (?<fee>-?[.\\d]+,\\d{2})"
                                                + " (?<currencyAccount>\\w{3}) -?(?<amount>[.\\d]+,\\d{2})$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

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
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // with fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR  \d\s \d\s,\d
                            // exchangeRate --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Market value | Local market value | Fee | Total amount
                            // -------------------------------------
                            // 22-10-2020 16:35 THE KRAFT HEINZ COMPAN  US5007541064 NDQ -40 31,21 USD 1.248,40 USD 1.054,33 EUR 1,1829 -0,64 EUR 1.053,69 EUR
                            // 22-12-2020 04:41 TCL ELECTRONICS KYG8701T1388 HKS -8000 5,78 HKD 46 240,00 HKD 4 868,15 EUR 9,489 -2,93 EUR 4 865,22 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*) "
                                                + "-?[.\\d\\s]+,\\d{2,4} \\w{3} "
                                                + "-?(?<amountFx>[.\\d\\s]+,\\d{2}) (?<currency>\\w{3}).* "
                                                + "-?[.\\d]+,\\d{2} \\w{3} "
                                                + "(?<exchangeRate>[.\\d]+,\\d{1,6}) "
                                                + "(?<fee>-?[.\\d\\s]+,\\d{2}) (?<currencyFee>\\w{3}) "
                                                + "-?(?<amount>[.\\d\\s]+,\\d{2}) (?<currencyAccount>\\w{3})$") 
                                .assign((t, v) -> {
                                    v.put("fee", stripBlanks(v.get("fee")));
                                    v.put("amountFx", stripBlanks(v.get("amountFx")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

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
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // without fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR  \d\s \d\s,\d
                            // exchangeRate --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 22-07-2019 19:16 LPL FINANCIAL HOLDINGS US50212V1008 NDQ -1 USD 85,73 USD 85,73 EUR 76,42 1,1218 EUR 76,42
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*)"
                                                + " \\w{3} -?[.\\d]+,\\d{2,3}.*" 
                                                + " (?<currency>\\w{3}) -?(?<amountFx>[.\\d]+,\\d{2}).*"
                                                + " \\w{3} -?[.\\d]+,\\d{2}"
                                                + " (?<exchangeRate>[.\\d]+,\\d{1,6})"
                                                + " (?<currencyAccount>\\w{3}) -?(?<amount>[.\\d]+,\\d{2})$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // without fee
                            // without stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR  \d\s \d\s,\d
                            // exchangeRage --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 14-01-2021 15:36 ALTO INGREDIENTS INC US0215131063 NDQ 2048 7,30 USD -14 950,40 USD -12 245,87 EUR 1,2209 -12 245,87 EUR
                            // 14-12-2020 00:00 ASTON MARTIN GB00BN7CG237 LSE 51 1 417,00 GBX -72 267,00 GBX -791,47 EUR 91,3075 -791,47 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>\\w{12}) "
                                                + "\\w{3} "
                                                + "(?<shares>[-]?[.\\d]+[,\\d]*) "
                                                + "-?[.\\d\\s]+,\\d{2,3} \\w{3} "
                                                + "-?(?<amountFx>[.\\d\\s]+,\\d{2}) (?<currency>\\w{3}) "
                                                + "-?[.\\d\\s]+,\\d{2} \\w{3} "
                                                + "(?<exchangeRate>[.\\d]+,\\d{1,6}) "
                                                + "-?(?<amount>[.\\d\\s]+,\\d{2}) (?<currencyAccount>\\w{3})$") 
                                .assign((t, v) -> {
                                    v.put("amountFx", stripBlanks(v.get("amountFx")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // with fee
                            // with stock exchange place
                            // Money        --> \d,\d   OR  \d\d,\d     OR  \d\s \d\s,\d
                            // exchangeRage --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Fee | Total amount
                            // -------------------------------------
                            // 27-01-2021 20:55 APPLE INC. - COMMON ST US0378331005 NDQ XNAS 9 141,70 USD -1.275,30 USD -1.053,79 EUR 1,209 -0,53 EUR -1.054,32 EUR
                            // 27-01-2021 20:54 NIKOLA CORP US6541101050 NDQ XNAS -48 28,00 USD 1.344,00 USD 1.108,34 EUR 1,2114 -0,66 EUR 1.107,68 EUR
                            // 23-02-2021 15:30 ALTO INGREDIENTS INC US0215131063 NDQ SOHO -224 6,30 USD 1 411,20 USD 1 159,26 EUR 1,2161 -0,74 EUR 1 158,52 EUR
                            // 23-02-2021 09:42 ASTON MARTIN GB00BN7CG237 LSE MESI -128 2 105,00 GBX 269 440,00 GBX 3 113,71 EUR 86,4469 -5,56 EUR 3 108,15 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[,\\d]*) "
                                                + "-?[.\\d\\s]+,\\d{2,4} [\\w]{3} "
                                                + "-?(?<amountFx>([.\\d\\s]+[,\\d]*)) (?<currency>[\\w]{3}) "
                                                + "-?[.\\d\\s]+[,\\d]* [\\w]{3} "
                                                + "(?<exchangeRate>[.\\d]+[,\\d]*) "
                                                + "(?<fee>-?[.\\d\\s]+[,\\d]*) (?<currencyFee>[\\w]{3}) "
                                                + "(?<amount>-?[.\\d\\s]+[,\\d]*) (?<currencyAccount>[\\w]{3})$")
                                .assign((t, v) -> {
                                    v.put("fee", stripBlanks(v.get("fee")));
                                    v.put("amountFx", stripBlanks(v.get("amountFx")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

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
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // with fee
                            // with stock exchange place
                            // Money        --> \d.\d   OR  \d,\d.\d
                            // exchangeRage --> \d.\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Fee | Total amount
                            // -------------------------------------
                            // 02-02-2021 17:39 NRG ENERGY INC. COMMO US6293775085 NSY XNAS 25 43.30 USD -1,082.50 USD -972.53 CHF 1.112 -0.63 CHF -973.16 CHF
                            // 02-02-2021 16:14 AMERICAN STATES WATER US0298991011 NSY XNAS 10 80.35 USD -803.50 USD -721.95 CHF 1.1118 -0.04 CHF -721.99 CHF
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[.\\d]*) "
                                                + "-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+) [\\w]{3} "
                                                + "(?<amountFx>-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)) "
                                                + "(?<currency>[\\w]{3}) "
                                                + "-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+) [\\w]{3} "
                                                + "(?<exchangeRate>[.\\d]+[.\\d]*) "
                                                + "(?<fee>-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)) "
                                                + "(?<currencyFee>[\\w]{3}) "
                                                + "(?<amount>-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)) "
                                                + "(?<currencyAccount>[\\w]{3})$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(convertAmount(v.get("amount"))));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(convertAmount(v.get("fee"))));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    long amountFx = asAmount(convertAmount(v.get("amountFx")));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(convertAmount(v.get("amount"))));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(convertExchangeRate(v.get("exchangeRate"))), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // without exchange rate
                            // with fee
                            // with stock exchange place
                            // Money        --> .\d,\d  OR  \d\s \d\s,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Market value | Local Market value | Fee | Total amount
                            // -------------------------------------
                            // 07-01-2021 09:00 K&S AG DE000KSAG888 XET XETA 20 9,748 EUR -194,96 EUR -194,96 EUR -2,04 EUR -197,00 EUR
                            // 21-12-2020 10:33 VARTA AG DE000A0TGJ55 XET XETA -11 111,60 EUR 1.227,60 EUR 1.227,60 EUR -2,22 EUR 1.225,38 EUR
                            // 23-02-2021 15:56 INVESCO EQQQ NASDAQ-100 IE0032077012 XET XETA 8 260,00 EUR -2 080,00 EUR -2 080,00 EUR -2,62 EUR -2 082,62 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[,\\d]*) "  
                                                + "[-.,\\d\\s]* [\\w]{3} " 
                                                + "[-.,\\d\\s]* [\\w]{3} " 
                                                + "[-.,\\d\\s]* [\\w]{3} " 
                                                + "-?(?<fee>[.\\d\\s]+,[\\d]*) (?<currencyFee>[\\w]{3}) "
                                                + "-?(?<amount>[.\\d\\s]+,[\\d]*) (?<currency>[\\w]{3})$")
                                .assign((t, v) -> {
                                    v.put("fee", stripBlanks(v.get("fee")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency"))); 
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                            }),

                            // -------------------------------------
                            // without exchange rate
                            // with fee
                            // with stock exchange place
                            // Money        --> \d.\d   OR  \d,\d.\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Market value | Local Market value | Fee | Total amount
                            // -------------------------------------
                            // 07-01-2021 09:00 K&S AG DE000KSAG888 XET XETA 25 9.748 EUR -1,082.50 EUR -972.53 EUR -0.63 EUR -973.16 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[,\\d]*) "
                                                + "[-.,\\d]* [\\w]{3} "
                                                + "[-.,\\d]* [\\w]{3} "
                                                + "[-.,\\d]* [\\w]{3} "
                                                + "-?(?<fee>([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)*) (?<currencyFee>[\\w]{3}) "
                                                + "-?(?<amount>([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)*) (?<currency>[\\w]{3})$")
                                .assign((t, v) -> {
                                    v.put("fee", stripBlanks(v.get("fee")));
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(convertAmount(v.get("amount"))));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(convertAmount(v.get("fee"))));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // without fee
                            // with stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d
                            // exchangeRage --> \d,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 03-02-2021 21:18 UPWORK INC. CMN US91688F1049 NDQ SOHO -1 47,00 USD 47,00 USD 39,00 EUR 1,2038 39,00 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[,\\d]*) "
                                                + "-?[.\\d]+,\\d{2,3} [\\w]{3}.* "
                                                + "-?(?<amountFx>[.\\d]+,\\d{2}) (?<currency>[\\w]{3}).* "
                                                + "-?[.\\d]+,\\d{2} [\\w]{3} "
                                                + "(?<exchangeRate>[.\\d]+,\\d{1,6}) "
                                                + "-?(?<amount>[.\\d]+,\\d{2}) (?<currencyAccount>[\\w]{3})$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // with exchange rate
                            // without fee
                            // with stock exchange place
                            // Money        --> \d.\d   OR  \d,\d.\d
                            // exchangeRage --> \d.\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 03-08-2020 09:30 VANGUARD FTSE AW IE00B3RBWM25 EAM XAMS 15 77.09 EUR -1,156.35 EUR -1,248.40 CHF 0.9253 -1,248.40 CHF
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[\\w]{12}) "
                                                + "[\\w]{3} [\\w]{4} "
                                                + "(?<shares>-?[.\\d]+[.\\d]*) "
                                                + "-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+) [\\w]{3}.* "
                                                + "-?(?<amountFx>([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)) (?<currency>[\\w]{3}).* "
                                                + "-?([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+) [\\w]{3} "
                                                + "(?<exchangeRate>[.\\d]+[.\\d]*) "
                                                + "-?(?<amount>([\\d]+,[\\d]{3}.\\d+|\\d+.\\d+)) (?<currencyAccount>[\\w]{3})$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(convertAmount(v.get("amount"))));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(convertAmount(v.get("amountFx")));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(convertAmount(v.get("amount"))));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(convertExchangeRate(v.get("exchangeRate"))), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // -------------------------------------
                            // without exchange rate
                            // without fee
                            // with stock exchange place
                            // Money        --> \d,\d   OR  \d.\d,\d    OR  \d\s \d\s,\d
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange + Place | Shares | Quote | Local Market value | Total amount
                            // -------------------------------------
                            // 28-02-2020 09:00 TOMTOM NL0013332471 EAM XAMS 1 8,933 EUR -8,93 EUR -8,93 EUR -8,93 EUR
                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) "
                                                + "(?<name>.*) (?<isin>\\w{12}) "
                                                + "\\w{3} \\w{4} "
                                                + "(?<shares>[-]?[.\\d\\s]+[,\\d]*) "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "[-.,\\d\\s]* \\w{3} "
                                                + "-?(?<amount>[.,\\d\\s]*) (?<currency>\\w{3})$") 
                                .assign((t, v) -> {
                                    v.put("amount", stripBlanks(v.get("amount")));

                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(
                                                        v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            })
                        )

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private String convertAmount(String inputAmount)
    {
        // 43.30        --> 43,30
        // -1,082.50    --> 1082,50
        String amount1 = inputAmount.replace(",", "");
        return amount1.replace(".", ",");
    }
    
    @SuppressWarnings("nls") 
    private String convertExchangeRate(String inputExchangeRate)
    {
        // 1.112    --> 1,112
        return inputExchangeRate.replace(".", ",");
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
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
